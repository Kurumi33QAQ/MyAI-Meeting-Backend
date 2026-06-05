package com.zsj.meetingagent.evaluation.model;

/**
 * 单个评测方案的聚合指标。
 * 所有指标都由实际 case 结果统计出来，不能提前写死到 README 或简历中。
 */
public record EvaluationMetrics(
        int totalCases,
        int hallucinatedCount,
        int answerHitCount,
        int citationCorrectCount,
        double hallucinationRate,
        double answerHitRate,
        double citationAccuracy,
        long avgLatencyMs
) {
}
