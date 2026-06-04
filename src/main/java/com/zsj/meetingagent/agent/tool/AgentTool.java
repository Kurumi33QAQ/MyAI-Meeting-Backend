package com.zsj.meetingagent.agent.tool;

/**
 * Agent 可调用工具接口。
 * 当前先使用 Java 后端显式工具抽象，后续可升级为 Spring AI Tool Calling 或 LLM 自动选工具。
 */
public interface AgentTool {

    String name();

    String description();

    boolean supports(AgentToolContext context);

    AgentToolResult execute(AgentToolContext context);
}
