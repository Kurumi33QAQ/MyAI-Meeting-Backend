package com.zsj.meetingagent.evaluation.vo;

/**
 * 单个评测方案的指标汇总响应。
 * 指标名称和 README/简历中计划使用的名称保持一致，方便后续引用真实报告。
 */
public record EvaluationStrategySummaryResponse(
        String strategy,
        String description,
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
