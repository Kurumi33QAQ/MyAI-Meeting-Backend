package com.zsj.meetingagent.interview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 模拟面试流程配置。
 * 用于控制基础题量、标准题量、最大题量和单个主问题的最大追问轮数，避免关键策略散落在业务代码中。
 */
@ConfigurationProperties(prefix = "app.interview")
public class InterviewProperties {

    private int initialQuestionCount = 8;

    private int standardQuestionCount = 12;

    private int maxQuestionCount = 15;

    private int maxFollowUpCount = 3;

    public int getInitialQuestionCount() {
        return initialQuestionCount;
    }

    public void setInitialQuestionCount(int initialQuestionCount) {
        this.initialQuestionCount = normalizePositive(initialQuestionCount, 8);
    }

    public int getStandardQuestionCount() {
        return standardQuestionCount;
    }

    public void setStandardQuestionCount(int standardQuestionCount) {
        this.standardQuestionCount = normalizePositive(standardQuestionCount, 12);
    }

    public int getMaxQuestionCount() {
        return maxQuestionCount;
    }

    public void setMaxQuestionCount(int maxQuestionCount) {
        this.maxQuestionCount = normalizePositive(maxQuestionCount, 15);
    }

    public int getMaxFollowUpCount() {
        return maxFollowUpCount;
    }

    public void setMaxFollowUpCount(int maxFollowUpCount) {
        this.maxFollowUpCount = normalizePositive(maxFollowUpCount, 3);
    }

    private int normalizePositive(int value, int defaultValue) {
        return value > 0 ? value : defaultValue;
    }
}
