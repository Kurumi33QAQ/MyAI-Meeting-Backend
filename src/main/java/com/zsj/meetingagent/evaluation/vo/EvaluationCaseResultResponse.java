package com.zsj.meetingagent.evaluation.vo;

import java.util.List;

/**
 * 单条评测结果响应。
 * 前端或接口测试可以直接看到回答、引用证据和三个核心判断。
 */
public record EvaluationCaseResultResponse(
        String caseId,
        String strategy,
        String category,
        String question,
        String answer,
        String groundTruth,
        List<String> citedEvidenceIds,
        boolean answerCorrect,
        boolean hallucinated,
        boolean citationCorrect,
        long latencyMs
) {
}
