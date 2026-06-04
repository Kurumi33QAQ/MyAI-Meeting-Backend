package com.zsj.meetingagent.chat.dto;

/**
 * 旧前端消息媒体请求。
 * 当前聊天链路暂不处理图片和文件，只保留字段用于兼容旧前端请求体。
 * 后续简历上传、知识库和 RAG 能力会重新设计文件类接口。
 */
public record LegacyMessageMediaRequest(
        String mediaType,
        String mediaUrl,
        String fileName,
        Long fileSize,
        String contentType
) {
}
