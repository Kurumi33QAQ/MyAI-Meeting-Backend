package com.zsj.meetingagent.interview.entity;

import java.time.Instant;

/**
 * 面试记录领域对象。
 * 对应 MySQL interview_record 表，保存面试会话的结构化摘要和最终报告。
 */
public record InterviewRecord(
        Long id,
        String sessionId,
        String username,
        String resumeId,
        String jobTitle,
        String status,
        Integer questionCount,
        Integer answeredCount,
        Integer totalScore,
        String reportSummary,
        Instant createdAt,
        Instant updatedAt,
        int deleted
) {
}
