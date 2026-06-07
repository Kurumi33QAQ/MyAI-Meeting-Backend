package com.zsj.meetingagent.interview.adaptive;

/**
 * 自适应面试进度策略。
 * 根据已回答数量和平均得分决定提前结束、继续标准题量或扩展考察。
 */
public interface InterviewProgressPolicy {

    int initialQuestionCount();

    int questionPoolSize();

    InterviewProgressDecision decide(
            boolean adaptive,
            int answeredCount,
            int averageScore,
            int currentQuestionCount,
            int maxQuestionCount,
            boolean pendingFollowUp
    );
}
