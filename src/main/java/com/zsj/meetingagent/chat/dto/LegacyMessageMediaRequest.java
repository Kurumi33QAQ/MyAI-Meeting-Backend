package com.zsj.meetingagent.chat.dto;

/**
 * 旧前端消息媒体请求。
 * 阶段 3 暂不处理图片和文件，只保留字段用于兼容请求体，阶段 6/7 再结合简历和知识库扩展。
 */
public record LegacyMessageMediaRequest(
        String mediaType,
        String mediaUrl,
        String fileName,
        Long fileSize,
        String contentType
) {
}
