package com.zsj.meetingagent.agent.tool;

/**
 * Agent 工具执行结果。
 * observation 是工具返回给 Agent 的观察结果，后续会进入最终回答生成链路。
 */
public record AgentToolResult(
        String toolName,
        String observation
) {
}
