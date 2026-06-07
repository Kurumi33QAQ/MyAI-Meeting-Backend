package com.zsj.meetingagent.interview.adaptive;

/**
 * 自适应面试进度决策。
 * 决定当前是否结束面试，以及下一阶段需要开放多少道主问题。
 */
public record InterviewProgressDecision(
        boolean complete,
        int targetQuestionCount,
        String reason
) {
}
