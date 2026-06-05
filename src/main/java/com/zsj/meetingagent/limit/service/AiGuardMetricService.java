package com.zsj.meetingagent.limit.service;

import com.zsj.meetingagent.limit.enums.AiCallFailureType;
import com.zsj.meetingagent.limit.model.AiGuardMetricSnapshot;

/**
 * AI Guard 指标服务。
 * 用内存计数记录本次应用启动后的调用治理效果，后续可迁移到 Redis 或 MySQL 做长期统计。
 */
public interface AiGuardMetricService {

    void recordOwnerCall(long latencyMs);

    void recordReplayedCall(long latencyMs);

    void recordDirectCall(long latencyMs);

    void recordFallbackCall(AiCallFailureType failureType, long latencyMs);

    void recordRateLimitedCall();

    void recordRedisBypassCall();

    AiGuardMetricSnapshot snapshot();
}
