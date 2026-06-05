package com.zsj.meetingagent.limit.vo;

/**
 * AI Guard 健康状态返回结果。
 * 用于本地验收 Redis Single-flight 是否开启，以及 Redis 当前是否可访问。
 */
public record AiGuardHealthResponse(
        boolean enabled,
        boolean redisAvailable,
        String mode
) {
}
