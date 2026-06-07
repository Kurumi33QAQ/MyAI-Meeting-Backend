package com.zsj.meetingagent.interview.vo;

import java.time.Instant;

/**
 * 追问轮次响应。
 * 前端报告页可根据 round 和 questionId 把 F1/F2/F3 追问链归并到同一个主问题下。
 */
public record InterviewFollowUpResponse(
        int round,
        String questionId,
        String question,
        String answer,
        Integer score,
        String feedback,
        String ruleTrace,
        Instant createdAt,
        Instant answeredAt
) {
}
