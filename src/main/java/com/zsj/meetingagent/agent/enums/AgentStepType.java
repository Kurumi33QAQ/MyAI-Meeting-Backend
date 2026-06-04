package com.zsj.meetingagent.agent.enums;

/**
 * Agent 执行步骤类型。
 * 阶段 5 固定使用 Thought-Action-Observation-Final Answer，先让执行链路清晰可追踪。
 */
public enum AgentStepType {
    THOUGHT,
    ACTION,
    OBSERVATION,
    FINAL_ANSWER
}
