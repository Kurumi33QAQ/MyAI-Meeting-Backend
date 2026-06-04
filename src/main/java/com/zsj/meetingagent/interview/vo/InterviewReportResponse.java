package com.zsj.meetingagent.interview.vo;

import com.zsj.meetingagent.interview.enums.InterviewSessionStatus;

import java.util.List;

/**
 * 面试报告响应。
 * 汇总总分、完成情况、改进建议和每道题的评分明细。
 */
public record InterviewReportResponse(
        String sessionId,
        InterviewSessionStatus status,
        Integer totalScore,
        int answeredCount,
        int questionCount,
        String reportSummary,
        List<InterviewQuestionResponse> questions
) {
}
