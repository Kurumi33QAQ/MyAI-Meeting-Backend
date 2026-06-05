package com.zsj.meetingagent.interview.runtime;

import com.zsj.meetingagent.interview.enums.InterviewSessionStatus;

import java.time.Instant;

/**
 * 面试运行态模型。
 * 这是阶段 8.6 长会话恢复的核心对象，描述用户当前进行到哪一题、已经回答多少题以及状态版本。
 */
public record InterviewRuntimeState(
        String sessionId,
        String username,
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
