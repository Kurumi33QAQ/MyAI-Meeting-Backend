package com.zsj.meetingagent.chat.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新风格流式聊天请求。
 * 阶段 3 只关心单轮流式输出，会话持久化和历史恢复放到阶段 4。
 */
public record ChatStreamRequest(
        @NotBlank(message = "不能为空")
        @Size(max = 4000, message = "长度不能超过 4000 个字符")
        String message,

        @Size(max = 80, message = "长度不能超过 80 个字符")
        String model,

        @Size(max = 2000, message = "长度不能超过 2000 个字符")
        String systemPrompt,

        @DecimalMin(value = "0.0", message = "不能小于 0")
        @DecimalMax(value = "2.0", message = "不能大于 2")
        Double temperature
) {
}
