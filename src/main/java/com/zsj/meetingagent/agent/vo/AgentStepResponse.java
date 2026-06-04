package com.zsj.meetingagent.agent.vo;

import com.zsj.meetingagent.agent.enums.AgentStepType;

import java.time.Instant;

/**
 * Agent 步骤响应。
 * 前端或调试工具可以用它展示 Agent 每一步在想什么、调了什么工具、观察到了什么结果。
 */
public record AgentStepResponse(
        AgentStepType stepType,
        int stepOrder,
        String toolName,
        String content,
        Instant createdAt
) {
}
