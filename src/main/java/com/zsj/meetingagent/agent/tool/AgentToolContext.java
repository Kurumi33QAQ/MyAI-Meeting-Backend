package com.zsj.meetingagent.agent.tool;

/**
 * Agent 工具执行上下文。
 * 工具不直接接触 Controller，而是从上下文中拿用户名、用户输入和会话 ID 等必要信息。
 */
public record AgentToolContext(
        String username,
        String input,
        String sessionId,
        String model
) {
}
