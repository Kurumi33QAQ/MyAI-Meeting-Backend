package com.zsj.meetingagent.limit;

import com.zsj.meetingagent.limit.config.AiGuardProperties;
import com.zsj.meetingagent.limit.enums.AiCallOperation;
import com.zsj.meetingagent.limit.model.AiGuardRequest;
import com.zsj.meetingagent.limit.model.AiGuardResult;
import com.zsj.meetingagent.limit.service.impl.InMemoryAiGuardMetricService;
import com.zsj.meetingagent.limit.service.impl.RedisAiCallGuardService;
import com.zsj.meetingagent.limit.service.impl.RedisAiRateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisAiCallGuardServiceTest {

    @Test
    void ownerRequestCallsModelOnceAndRecordsMetric() {
        GuardFixture fixture = redisFixture();
        AtomicInteger modelCalls = new AtomicInteger();

        AiGuardResult result = fixture.guardService.executeText(
                guardRequest(),
                () -> {
                    modelCalls.incrementAndGet();
                    return "真实模型回答";
                }
        );

        assertThat(result.answer()).isEqualTo("真实模型回答");
        assertThat(result.ownerCall()).isTrue();
        assertThat(result.replayed()).isFalse();
        assertThat(modelCalls).hasValue(1);
        assertThat(fixture.metricService.snapshot().ownerCalls()).isEqualTo(1);
    }

    @Test
    void cachedResultIsReplayedWithoutCallingModel() {
        GuardFixture fixture = redisFixture();
        when(fixture.valueOperations.get(anyString())).thenReturn("缓存中的模型回答");

        AiGuardResult result = fixture.guardService.executeText(
                guardRequest(),
                () -> "这段不应该被执行"
        );

        assertThat(result.answer()).isEqualTo("缓存中的模型回答");
        assertThat(result.replayed()).isTrue();
        assertThat(fixture.metricService.snapshot().replayedCalls()).isEqualTo(1);
    }

    @Test
    void redisUnavailableFallsBackToDirectCallWhenRedisIsNotRequired() {
        AiGuardProperties properties = properties();
        InMemoryAiGuardMetricService metricService = new InMemoryAiGuardMetricService();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.getConnectionFactory()).thenReturn(null);
        RedisAiRateLimitService rateLimitService = new RedisAiRateLimitService(redisTemplate, properties, metricService);
        RedisAiCallGuardService guardService = new RedisAiCallGuardService(redisTemplate, properties, rateLimitService, metricService);

        AiGuardResult result = guardService.executeText(guardRequest(), () -> "Redis 不可用时直接调用模型");

        assertThat(result.answer()).isEqualTo("Redis 不可用时直接调用模型");
        assertThat(result.ownerCall()).isTrue();
        assertThat(metricService.snapshot().directCalls()).isEqualTo(1);
        assertThat(metricService.snapshot().redisBypassCalls()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void modelExceptionReturnsChineseFallbackAnswer() {
        GuardFixture fixture = redisFixture();

        AiGuardResult result = fixture.guardService.executeText(
                guardRequest(),
                () -> {
                    throw new IllegalStateException("model error");
                }
        );

        assertThat(result.fallback()).isTrue();
        assertThat(result.answer()).contains("降级回答");
        assertThat(result.answer()).contains("AI 服务暂时不稳定");
        assertThat(fixture.metricService.snapshot().fallbackCalls()).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    private GuardFixture redisFixture() {
        AiGuardProperties properties = properties();
        InMemoryAiGuardMetricService metricService = new InMemoryAiGuardMetricService();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);

        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        RedisAiRateLimitService rateLimitService = new RedisAiRateLimitService(redisTemplate, properties, metricService);
        RedisAiCallGuardService guardService = new RedisAiCallGuardService(redisTemplate, properties, rateLimitService, metricService);
        return new GuardFixture(guardService, metricService, valueOperations);
    }

    private AiGuardProperties properties() {
        AiGuardProperties properties = new AiGuardProperties();
        properties.setEnabled(true);
        properties.setRedisRequired(false);
        properties.setNamespace("meetingagent:test:ai");
        properties.setLockTtl(Duration.ofSeconds(30));
        properties.setResultTtl(Duration.ofMinutes(10));
        properties.setWaitTimeout(Duration.ofMillis(100));
        properties.setPollInterval(Duration.ofMillis(10));
        properties.setCallTimeout(Duration.ofSeconds(2));
        properties.setUserLimitPerMinute(20);
        properties.setFallbackAnswer("AI 服务暂时不稳定，请稍后重试。");
        return properties;
    }

    private AiGuardRequest guardRequest() {
        return AiGuardRequest.of("tester", AiCallOperation.CHAT_SYNC, "gpt-test", "请解释 Spring AI");
    }

    private record GuardFixture(
            RedisAiCallGuardService guardService,
            InMemoryAiGuardMetricService metricService,
            ValueOperations<String, String> valueOperations
    ) {
    }
}
