package com.zsj.meetingagent.limit.model;

import com.zsj.meetingagent.limit.enums.AiCallFailureType;

/**
 * AI 调用守卫执行结果。
 * 除回答文本外保留 owner/replay/fallback 信息，方便统计和后续排查。
 */
public record AiGuardResult(
        String answer,
        boolean ownerCall,
        boolean replayed,
        boolean fallback,
        AiCallFailureType failureType,
        long latencyMs
) {
}
