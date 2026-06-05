package com.zsj.meetingagent.evaluation.entity;

import java.time.Instant;

/**
 * 单条评测样本结果。
 * 每个 case 会按多个方案各写一行，便于后续对比 baseline 和 RAG 改进效果。
 */
public record EvaluationCaseResult(
        Long id,
        String runId,
        String caseId,
        String strategy,
        String category,
        String question,
        String answer,
        String groundTruth,
        String citedEvidenceIds,
        int answerCorrect,
        int hallucinated,
        int citationCorrect,
        long latencyMs,
        Instant createdAt
) {
}
