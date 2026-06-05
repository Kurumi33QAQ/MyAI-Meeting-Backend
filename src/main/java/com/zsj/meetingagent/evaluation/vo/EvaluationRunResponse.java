package com.zsj.meetingagent.evaluation.vo;

import java.time.Instant;
import java.util.List;

/**
 * 评测任务响应。
 * 包含四种方案的汇总指标、每条 case 明细以及报告文件路径。
 */
public record EvaluationRunResponse(
        String runId,
        String datasetName,
        int totalCases,
        List<EvaluationStrategySummaryResponse> summaries,
        List<EvaluationCaseResultResponse> caseResults,
        String reportJsonPath,
        String reportMarkdownPath,
        Instant createdAt,
        Instant completedAt
) {
}
