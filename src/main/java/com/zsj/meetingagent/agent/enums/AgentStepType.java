package com.zsj.meetingagent.agent.enums;

/**
 * Agent 执行步骤类型。
 * 当前固定使用 Thought-Action-Observation-Final Answer，先保证执行链路清晰可追踪。
 */
public enum AgentStepType {
    THOUGHT,
    ACTION,
    OBSERVATION,
    FINAL_ANSWER
}
