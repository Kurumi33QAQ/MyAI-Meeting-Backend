package com.zsj.meetingagent.chat.dto;

import jakarta.validation.constraints.Size;

/**
 * 创建聊天会话请求。
 * firstMessage 用于生成会话标题，真正的消息保存会在发送聊天时完成。
 */
public record CreateChatSessionRequest(
        @Size(max = 4000, message = "长度不能超过 4000 个字符")
        String firstMessage,

        @Size(max = 80, message = "长度不能超过 80 个字符")
        String model
) {
}
