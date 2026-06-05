package com.zsj.meetingagent.interview.vo;

import com.zsj.meetingagent.interview.enums.InterviewSessionStatus;

/**
 * 面试回答评分响应。
 * 提交单题回答后返回分数、反馈、追问问题和当前会话状态。
 */
public record InterviewAnswerResponse(
        String sessionId,
        String questionId,
        int score,
        String feedback,
        String followUpQuestion,
        String followUpRuleTrace,
        InterviewSessionStatus status,
        int answeredCount,
        int questionCount
) {
}
