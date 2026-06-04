package com.zsj.meetingagent.agent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建 Agent Run 的请求参数。
 * input 是用户问题，sessionId 可选，用于让 Agent 工具读取指定聊天会话的历史消息。
 */
public record AgentRunRequest(
        @NotBlank(message = "不能为空")
        @Size(max = 2000, message = "长度不能超过 2000 个字符")
        String input,

        @Size(max = 80, message = "长度不能超过 80 个字符")
        String sessionId,

        @Size(max = 80, message = "长度不能超过 80 个字符")
        String model,

        @Min(value = 4, message = "不能小于 4")
        @Max(value = 8, message = "不能大于 8")
        Integer maxSteps
) {
}
