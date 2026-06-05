package com.zsj.meetingagent.interview.vo;

import com.zsj.meetingagent.interview.enums.InterviewSessionStatus;
import com.zsj.meetingagent.interview.runtime.InterviewRuntimeRestoreSource;

import java.time.Instant;

/**
 * 面试运行态响应对象。
 * 前端或调试工具可以通过它判断面试恢复到了哪个题号，以及恢复来源是什么。
 */
public record InterviewRuntimeStateResponse(
        String sessionId,
        InterviewSessionStatus status,
        int currentQuestionIndex,
        int answeredCount,
        int questionCount,
        Integer totalScore,
        long version,
        InterviewRuntimeRestoreSource restoreSource,
        Instant updatedAt
) {
}
