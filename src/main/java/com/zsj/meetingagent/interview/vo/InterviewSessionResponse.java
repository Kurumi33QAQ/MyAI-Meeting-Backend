package com.zsj.meetingagent.interview.vo;

import com.zsj.meetingagent.interview.enums.InterviewSessionStatus;

import java.time.Instant;
import java.util.List;

/**
 * 模拟面试会话响应。
 * 聚合会话状态、岗位信息、分数摘要和题目列表。
 */
public record InterviewSessionResponse(
        String sessionId,
        String resumeId,
        String jobTitle,
        String companyName,
        String jobDescription,
        InterviewSessionStatus status,
        int questionCount,
        int answeredCount,
        Integer totalScore,
        String reportSummary,
        Instant createdAt,
        Instant updatedAt,
        List<InterviewQuestionResponse> questions
) {
}
