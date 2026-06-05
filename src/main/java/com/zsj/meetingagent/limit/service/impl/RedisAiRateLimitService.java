package com.zsj.meetingagent.limit.service.impl;

import com.zsj.meetingagent.limit.config.AiGuardProperties;
import com.zsj.meetingagent.limit.model.AiGuardRequest;
import com.zsj.meetingagent.limit.service.AiGuardMetricService;
import com.zsj.meetingagent.limit.service.AiRateLimitService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Redis AI 调用限流服务。
 * 使用用户 + 业务类型 + 分钟窗口做计数，避免前端重复点击或脚本短时间刷爆模型接口。
 */
@Service
@EnableConfigurationProperties(AiGuardProperties.class)
public class RedisAiRateLimitService implements AiRateLimitService {

    private static final DateTimeFormatter MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final StringRedisTemplate redisTemplate;
    private final AiGuardProperties properties;
    private final AiGuardMetricService metricService;

    public RedisAiRateLimitService(
            StringRedisTemplate redisTemplate,
            AiGuardProperties properties,
            AiGuardMetricService metricService
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.metricService = metricService;
    }

    @Override
    public boolean tryAcquire(AiGuardRequest request) {
        if (!properties.isEnabled() || properties.getUserLimitPerMinute() <= 0) {
            return true;
        }
        String key = "%s:rate:%s:%s:%s".formatted(
                properties.getNamespace(),
                request.username(),
                request.operation().name(),
                LocalDateTime.now().format(MINUTE_FORMATTER)
        );
        try {
            Long current = redisTemplate.opsForValue().increment(key);
            if (current != null && current == 1L) {
                redisTemplate.expire(key, Duration.ofMinutes(2));
            }
            return current == null || current <= properties.getUserLimitPerMinute();
        } catch (Exception ex) {
            // Redis 限流不可用时不阻断主业务，记录旁路后交给 Single-flight 或普通调用继续处理。
            metricService.recordRedisBypassCall();
            return true;
        }
    }
}
