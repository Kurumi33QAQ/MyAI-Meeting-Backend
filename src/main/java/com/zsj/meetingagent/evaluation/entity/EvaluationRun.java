package com.zsj.meetingagent.evaluation.entity;

import java.time.Instant;

/**
 * 评测任务主记录。
 * MySQL 保存结构化汇总，详细报告仍输出到 reports/evaluation 目录方便查看。
 */
public record EvaluationRun(
        Long id,
        String runId,
        String username,
        String datasetName,
        int totalCases,
        String baselineSummaryJson,
        String ragWithoutRerankSummaryJson,
        String ragWithRerankSummaryJson,
        String selfCheckRagSummaryJson,
        String reportJsonPath,
        String reportMarkdownPath,
        Instant createdAt,
        Instant completedAt,
        int deleted
) {
}
