package com.zsj.meetingagent.interview.adaptive;

import org.springframework.stereotype.Component;

/**
 * 默认自适应面试进度策略。
 * 默认先完成 8 道基础考察，标准面试为 12 道，能力判断仍不稳定时最多扩展到 15 道。
 */
@Component
public class DefaultInterviewProgressPolicy implements InterviewProgressPolicy {

    private static final int MIN_QUESTION_COUNT = 8;
    private static final int STANDARD_QUESTION_COUNT = 12;
    private static final int MAX_QUESTION_COUNT = 15;

    @Override
    public int initialQuestionCount() {
        return MIN_QUESTION_COUNT;
    }

    @Override
    public int questionPoolSize() {
        return MAX_QUESTION_COUNT;
    }

    @Override
    public InterviewProgressDecision decide(
            boolean adaptive,
            int answeredCount,
            int averageScore,
            int currentQuestionCount,
            int maxQuestionCount,
            boolean pendingFollowUp
    ) {
        if (pendingFollowUp) {
            return new InterviewProgressDecision(false, currentQuestionCount, "当前主问题已触发追问，需要先完成追问。");
        }
        if (!adaptive) {
            boolean complete = answeredCount >= currentQuestionCount;
            return new InterviewProgressDecision(
                    complete,
                    currentQuestionCount,
                    complete ? "已完成用户指定的固定题量。" : "继续完成用户指定的固定题量。"
            );
        }
        if (answeredCount < MIN_QUESTION_COUNT) {
            return new InterviewProgressDecision(false, MIN_QUESTION_COUNT, "至少完成 8 道主问题，覆盖项目、基础、场景和问题排查后再判断是否结束。");
        }
        if (answeredCount == MIN_QUESTION_COUNT) {
            if (averageScore >= 85) {
                return new InterviewProgressDecision(true, MIN_QUESTION_COUNT, "前八题表现稳定且优秀，基础考察已经充分，可以结束面试。");
            }
            int target = averageScore < 60 ? maxQuestionCount : Math.min(STANDARD_QUESTION_COUNT, maxQuestionCount);
            return new InterviewProgressDecision(false, target, averageScore < 60
                    ? "前八题暴露较多薄弱点，扩展到最多 15 道继续考察。"
                    : "前八题表现中等，继续完成标准 12 道题。");
        }
        if (answeredCount >= STANDARD_QUESTION_COUNT && averageScore >= 65) {
            return new InterviewProgressDecision(true, currentQuestionCount, "已完成标准题量，能力判断已基本稳定。");
        }
        if (answeredCount >= maxQuestionCount) {
            return new InterviewProgressDecision(true, maxQuestionCount, "已达到自适应面试最大题量。");
        }
        return new InterviewProgressDecision(false, maxQuestionCount, "当前得分仍不稳定，继续开放补充问题。");
    }
}
