package com.zsj.meetingagent.agent.tool;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 当前时间工具。
 * 这是阶段 5 的稳定演示工具，用于验证 Agent Action 和 Observation 链路可以真实调用后端方法。
 */
@Component
public class CurrentTimeTool implements AgentTool {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String name() {
        return "current_time";
    }

    @Override
    public String description() {
        return "获取后端服务器当前时间";
    }

    @Override
    public boolean supports(AgentToolContext context) {
        String input = context.input();
        return input == null
                || input.contains("时间")
                || input.contains("日期")
                || input.contains("今天")
                || input.contains("现在");
    }

    @Override
    public AgentToolResult execute(AgentToolContext context) {
        return new AgentToolResult(name(), "后端服务器当前时间：" + LocalDateTime.now().format(FORMATTER));
    }
}
