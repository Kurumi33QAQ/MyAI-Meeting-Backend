package com.zsj.meetingagent.interview.adaptive;

import com.zsj.meetingagent.interview.config.InterviewProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 默认自适应面试进度策略。
 * 默认先完成 8 道基础考察，标准面试为 12 道，能力判断仍不稳定时最多扩展到 15 道。
 */
@Component
public class DefaultInterviewProgressPolicy implements InterviewProgressPolicy {

    private final InterviewProperties properties;

    public DefaultInterviewProgressPolicy() {
        this(new InterviewProperties());
    }

    @Autowired
    public DefaultInterviewProgressPolicy(InterviewProperties properties) {
        this.properties = properties;
    }

    @Override
    public int initialQuestionCount() {
        return properties.getInitialQuestionCount();
    }

    @Override
    public int questionPoolSize() {
        return properties.getMaxQuestionCount();
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
        int initialQuestionCount = properties.getInitialQuestionCount();
        int standardQuestionCount = Math.min(properties.getStandardQuestionCount(), maxQuestionCount);
        if (answeredCount < initialQuestionCount) {
            return new InterviewProgressDecision(false, initialQuestionCount, "至少完成 " + initialQuestionCount + " 道主问题，覆盖项目、基础、场景和问题排查后再判断是否结束。");
        }
        if (answeredCount == initialQuestionCount) {
            if (averageScore >= 85) {
                return new InterviewProgressDecision(true, initialQuestionCount, "前 " + initialQuestionCount + " 题表现稳定且优秀，基础考察已经充分，可以结束面试。");
            }
            int target = averageScore < 60 ? maxQuestionCount : standardQuestionCount;
            return new InterviewProgressDecision(false, target, averageScore < 60
                    ? "前 " + initialQuestionCount + " 题暴露较多薄弱点，扩展到最多 " + maxQuestionCount + " 道继续考察。"
                    : "前 " + initialQuestionCount + " 题表现中等，继续完成标准 " + standardQuestionCount + " 道题。");
        }
        if (answeredCount < currentQuestionCount) {
            return new InterviewProgressDecision(false, currentQuestionCount, "继续完成当前计划的 " + currentQuestionCount + " 道主问题，再判断是否需要扩题或结束。");
        }
        if (answeredCount >= standardQuestionCount && averageScore >= 65) {
            return new InterviewProgressDecision(true, currentQuestionCount, "已完成标准题量，能力判断已基本稳定。");
        }
        if (answeredCount >= maxQuestionCount) {
            return new InterviewProgressDecision(true, maxQuestionCount, "已达到自适应面试最大题量。");
        }
        return new InterviewProgressDecision(false, maxQuestionCount, "当前得分仍不稳定，继续开放补充问题。");
    }
}
