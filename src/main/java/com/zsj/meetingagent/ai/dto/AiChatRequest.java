package com.zsj.meetingagent.ai.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 同步 AI 对话请求参数。
 * message 是用户输入；sessionId、model、systemPrompt 和 temperature 用于会话存储、模型切换和 Prompt 调整。
 */
public record AiChatRequest(
        @NotBlank(message = "不能为空")
        @Size(max = 4000, message = "长度不能超过 4000 个字符")
        String message,

        @Size(max = 80, message = "长度不能超过 80 个字符")
        String sessionId,

        @Size(max = 80, message = "长度不能超过 80 个字符")
        String model,

        @Size(max = 2000, message = "长度不能超过 2000 个字符")
        String systemPrompt,

        @DecimalMin(value = "0.0", message = "不能小于 0")
        @DecimalMax(value = "2.0", message = "不能大于 2")
        Double temperature
) {
}
