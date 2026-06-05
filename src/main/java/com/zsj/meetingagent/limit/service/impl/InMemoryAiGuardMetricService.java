package com.zsj.meetingagent.limit.service.impl;

import com.zsj.meetingagent.limit.enums.AiCallFailureType;
import com.zsj.meetingagent.limit.model.AiGuardMetricSnapshot;
import com.zsj.meetingagent.limit.service.AiGuardMetricService;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存版 AI Guard 指标服务。
 * 指标只用于本地验收和压测观察，应用重启后会清零，阶段 11 前如需长期报表再迁移到数据库。
 */
@Service
public class InMemoryAiGuardMetricService implements AiGuardMetricService {

    private final AtomicLong totalCalls = new AtomicLong();
    private final AtomicLong ownerCalls = new AtomicLong();
    private final AtomicLong replayedCalls = new AtomicLong();
    private final AtomicLong directCalls = new AtomicLong();
    private final AtomicLong fallbackCalls = new AtomicLong();
    private final AtomicLong rateLimitedCalls = new AtomicLong();
    private final AtomicLong failedCalls = new AtomicLong();
    private final AtomicLong redisBypassCalls = new AtomicLong();
    private final AtomicLong totalLatencyMs = new AtomicLong();

    @Override
    public void recordOwnerCall(long latencyMs) {
        totalCalls.incrementAndGet();
        ownerCalls.incrementAndGet();
        totalLatencyMs.addAndGet(Math.max(0, latencyMs));
    }

    @Override
    public void recordReplayedCall(long latencyMs) {
        totalCalls.incrementAndGet();
        replayedCalls.incrementAndGet();
        totalLatencyMs.addAndGet(Math.max(0, latencyMs));
    }

    @Override
    public void recordDirectCall(long latencyMs) {
        totalCalls.incrementAndGet();
        directCalls.incrementAndGet();
        totalLatencyMs.addAndGet(Math.max(0, latencyMs));
    }

    @Override
    public void recordFallbackCall(AiCallFailureType failureType, long latencyMs) {
        totalCalls.incrementAndGet();
        fallbackCalls.incrementAndGet();
        if (failureType != AiCallFailureType.NONE) {
            failedCalls.incrementAndGet();
        }
        totalLatencyMs.addAndGet(Math.max(0, latencyMs));
    }

    @Override
    public void recordRateLimitedCall() {
        totalCalls.incrementAndGet();
        rateLimitedCalls.incrementAndGet();
        failedCalls.incrementAndGet();
    }

    @Override
    public void recordRedisBypassCall() {
        redisBypassCalls.incrementAndGet();
    }

    @Override
    public AiGuardMetricSnapshot snapshot() {
        long total = totalCalls.get();
        long avgLatency = total == 0 ? 0 : totalLatencyMs.get() / total;
        return new AiGuardMetricSnapshot(
                total,
                ownerCalls.get(),
                replayedCalls.get(),
                directCalls.get(),
                fallbackCalls.get(),
                rateLimitedCalls.get(),
                failedCalls.get(),
                redisBypassCalls.get(),
                avgLatency
        );
    }
}
