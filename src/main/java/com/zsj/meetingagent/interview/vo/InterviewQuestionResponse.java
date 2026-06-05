package com.zsj.meetingagent.interview.vo;

import java.time.Instant;
import java.util.List;

/**
 * 面试题目响应。
 * 返回题目、参考考察点、用户回答和评分结果，便于前端展示完整面试过程。
 */
public record InterviewQuestionResponse(
        String questionId,
        int questionOrder,
        String question,
        String referenceAnswer,
        String evaluationPoints,
        String followUpDirection,
        List<String> evidenceIds,
        String evidenceSummary,
        String agentRunId,
        String userAnswer,
        Integer score,
        String feedback,
        String followUpQuestion,
        String followUpRuleTrace,
        Instant createdAt,
        Instant answeredAt
) {
}
