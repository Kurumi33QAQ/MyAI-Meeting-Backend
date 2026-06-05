package com.zsj.meetingagent.limit.service.impl;

import com.zsj.meetingagent.common.exception.BusinessException;
import com.zsj.meetingagent.limit.config.AiGuardProperties;
import com.zsj.meetingagent.limit.enums.AiCallFailureType;
import com.zsj.meetingagent.limit.model.AiGuardRequest;
import com.zsj.meetingagent.limit.model.AiGuardResult;
import com.zsj.meetingagent.limit.service.AiCallGuardService;
import com.zsj.meetingagent.limit.service.AiGuardMetricService;
import com.zsj.meetingagent.limit.service.AiRateLimitService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Redis 版 AI 调用守卫服务。
 * 通过 Redis 锁实现 Single-flight：同一个 requestKey 同时进来时，只有抢到锁的 owner 会真正调用模型，其他请求等待并复用结果。
 */
@Service
@EnableConfigurationProperties(AiGuardProperties.class)
public class RedisAiCallGuardService implements AiCallGuardService {

    private static final String RATE_LIMIT_ERROR_CODE = "AI_GUARD_RATE_LIMITED";

    private final StringRedisTemplate redisTemplate;
    private final AiGuardProperties properties;
    private final AiRateLimitService rateLimitService;
    private final AiGuardMetricService metricService;

    public RedisAiCallGuardService(
            StringRedisTemplate redisTemplate,
            AiGuardProperties properties,
            AiRateLimitService rateLimitService,
            AiGuardMetricService metricService
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.rateLimitService = rateLimitService;
        this.metricService = metricService;
    }

    @Override
    public AiGuardResult executeText(AiGuardRequest request, Supplier<String> ownerCall) {
        Instant startedAt = Instant.now();
        if (!properties.isEnabled()) {
            return directCall(ownerCall, startedAt);
        }
        if (!rateLimitService.tryAcquire(request)) {
            metricService.recordRateLimitedCall();
            throw new BusinessException(RATE_LIMIT_ERROR_CODE, "AI 调用过于频繁，请稍后再试");
        }
        if (!redisAvailable()) {
            return handleRedisUnavailable(ownerCall, startedAt);
        }

        String resultKey = resultKey(request);
        Optional<String> cachedResult = readResult(resultKey);
        if (cachedResult.isPresent()) {
            long latencyMs = elapsedMillis(startedAt);
            metricService.recordReplayedCall(latencyMs);
            return new AiGuardResult(cachedResult.get(), false, true, false, AiCallFailureType.NONE, latencyMs);
        }

        String lockKey = lockKey(request);
        String ownerId = UUID.randomUUID().toString();
        if (tryBecomeOwner(lockKey, ownerId)) {
            return ownerCallAndPublishResult(ownerCall, lockKey, resultKey, ownerId, startedAt);
        }
        return waitForOwnerResult(resultKey, startedAt);
    }

    @Override
    public boolean redisAvailable() {
        try {
            RedisConnection connection = redisTemplate.getConnectionFactory() == null
                    ? null
                    : redisTemplate.getConnectionFactory().getConnection();
            if (connection == null) {
                return false;
            }
            connection.ping();
            connection.close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public boolean enabled() {
        return properties.isEnabled();
    }

    private AiGuardResult directCall(Supplier<String> ownerCall, Instant startedAt) {
        try {
            String answer = ownerCall.get();
            long latencyMs = elapsedMillis(startedAt);
            metricService.recordDirectCall(latencyMs);
            return new AiGuardResult(answer, true, false, false, AiCallFailureType.NONE, latencyMs);
        } catch (Exception ex) {
            return fallback(AiCallFailureType.MODEL_ERROR, startedAt);
        }
    }

    private AiGuardResult handleRedisUnavailable(Supplier<String> ownerCall, Instant startedAt) {
        metricService.recordRedisBypassCall();
        if (properties.isRedisRequired()) {
            return fallback(AiCallFailureType.REDIS_UNAVAILABLE, startedAt);
        }
        return directCall(ownerCall, startedAt);
    }

    private AiGuardResult ownerCallAndPublishResult(
            Supplier<String> ownerCall,
            String lockKey,
            String resultKey,
            String ownerId,
            Instant startedAt
    ) {
        try {
            String answer = callWithTimeout(ownerCall);
            writeResult(resultKey, answer);
            long latencyMs = elapsedMillis(startedAt);
            metricService.recordOwnerCall(latencyMs);
            return new AiGuardResult(answer, true, false, false, AiCallFailureType.NONE, latencyMs);
        } catch (AiGuardTimeoutException ex) {
            AiGuardResult fallback = fallback(AiCallFailureType.MODEL_TIMEOUT, startedAt);
            writeResult(resultKey, fallback.answer());
            return fallback;
        } catch (BusinessException ex) {
            AiGuardResult fallback = fallback(AiCallFailureType.MODEL_ERROR, startedAt);
            writeResult(resultKey, fallback.answer());
            return fallback;
        } catch (Exception ex) {
            AiGuardResult fallback = fallback(AiCallFailureType.UNKNOWN, startedAt);
            writeResult(resultKey, fallback.answer());
            return fallback;
        } finally {
            releaseLock(lockKey, ownerId);
        }
    }

    private AiGuardResult waitForOwnerResult(String resultKey, Instant startedAt) {
        Instant deadline = startedAt.plus(properties.getWaitTimeout());
        while (Instant.now().isBefore(deadline)) {
            Optional<String> result = readResult(resultKey);
            if (result.isPresent()) {
                long latencyMs = elapsedMillis(startedAt);
                metricService.recordReplayedCall(latencyMs);
                return new AiGuardResult(result.get(), false, true, false, AiCallFailureType.NONE, latencyMs);
            }
            sleep(properties.getPollInterval());
        }
        return fallback(AiCallFailureType.WAIT_TIMEOUT, startedAt);
    }

    private String callWithTimeout(Supplier<String> ownerCall) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(ownerCall);
        try {
            return future.get(properties.getCallTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new AiGuardTimeoutException();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiGuardTimeoutException();
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new IllegalStateException(cause);
        }
    }

    private Optional<String> readResult(String resultKey) {
        try {
            String result = redisTemplate.opsForValue().get(resultKey);
            return StringUtils.hasText(result) ? Optional.of(result) : Optional.empty();
        } catch (DataAccessException ex) {
            metricService.recordRedisBypassCall();
            return Optional.empty();
        }
    }

    private void writeResult(String resultKey, String answer) {
        try {
            redisTemplate.opsForValue().set(resultKey, answer, properties.getResultTtl());
        } catch (DataAccessException ex) {
            metricService.recordRedisBypassCall();
        }
    }

    private boolean tryBecomeOwner(String lockKey, String ownerId) {
        try {
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, ownerId, properties.getLockTtl());
            return Boolean.TRUE.equals(locked);
        } catch (RedisConnectionFailureException ex) {
            metricService.recordRedisBypassCall();
            return false;
        }
    }

    private void releaseLock(String lockKey, String ownerId) {
        try {
            String currentOwner = redisTemplate.opsForValue().get(lockKey);
            // 只释放自己持有的锁，避免误删其他请求刚刚抢到的新锁。
            if (ownerId.equals(currentOwner)) {
                redisTemplate.delete(lockKey);
            }
        } catch (DataAccessException ex) {
            metricService.recordRedisBypassCall();
        }
    }

    private AiGuardResult fallback(AiCallFailureType failureType, Instant startedAt) {
        long latencyMs = elapsedMillis(startedAt);
        metricService.recordFallbackCall(failureType, latencyMs);
        String answer = "【降级回答】%s（原因：%s）".formatted(properties.getFallbackAnswer(), failureType.description());
        return new AiGuardResult(answer, false, false, true, failureType, latencyMs);
    }

    private String lockKey(AiGuardRequest request) {
        return "%s:lock:%s:%s".formatted(properties.getNamespace(), request.operation().name(), request.requestKey());
    }

    private String resultKey(AiGuardRequest request) {
        return "%s:result:%s:%s".formatted(properties.getNamespace(), request.operation().name(), request.requestKey());
    }

    private long elapsedMillis(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(Math.max(10L, duration.toMillis()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static class AiGuardTimeoutException extends RuntimeException {
    }
}
