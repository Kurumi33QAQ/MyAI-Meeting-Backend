package com.zsj.meetingagent.limit.enums;

/**
 * AI 调用业务类型。
 * 通过业务类型区分聊天、面试出题和评估等高成本调用，后续可以按场景设置不同限流和降级策略。
 */
public enum AiCallOperation {

    CHAT_SYNC("同步 AI 对话"),
    INTERVIEW_QUESTION_GENERATION("模拟面试出题"),
    INTERVIEW_ANSWER_REVIEW("模拟面试答题评估"),
    AGENT_ORCHESTRATION("Agent 编排"),
    EVALUATION_RUN("评测任务");

    private final String description;

    AiCallOperation(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
