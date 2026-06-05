package com.zsj.meetingagent.limit.model;

/**
 * AI Guard 指标快照。
 * 这些指标用于验收 Single-flight 是否真的减少了重复模型调用。
 */
public record AiGuardMetricSnapshot(
        long totalCalls,
        long ownerCalls,
        long replayedCalls,
        long directCalls,
        long fallbackCalls,
        long rateLimitedCalls,
        long failedCalls,
        long redisBypassCalls,
        long avgLatencyMs
) {
}
