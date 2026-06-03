package com.zsj.meetingagent.chat.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 旧前端流式聊天请求。
 * 字段名保持旧前端契约，避免修改前端；进入业务层前会转换为本项目自己的 ChatStreamRequest。
 */
public record LegacyChatStreamRequest(
        String sessionId,

        @NotBlank(message = "不能为空")
        String inputMessage,

        String userName,

        Long aiId,

        Integer messageSeq,

        List<String> imageUrls,

        List<LegacyMessageMediaRequest> mediaList
) {
}
