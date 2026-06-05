package com.zsj.meetingagent.evaluation.model;

import java.util.List;

/**
 * 单个方案在单条 case 上的预测结果。
 * 这里同时保存回答、引用证据、置信度和耗时，方便后续统一计算指标。
 */
public record EvaluationPrediction(
        String answer,
        List<String> citedEvidenceIds,
        double confidence,
        boolean refused,
        long latencyMs
) {
}
