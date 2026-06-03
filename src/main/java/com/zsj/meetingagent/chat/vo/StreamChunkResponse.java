package com.zsj.meetingagent.chat.vo;

/**
 * SSE 分片响应。
 * 前端会读取 content 字段逐段拼接；done 和 error 为后续更复杂状态预留。
 */
public record StreamChunkResponse(
        String content,
        Boolean done,
        String error
) {

    public static StreamChunkResponse content(String content) {
        return new StreamChunkResponse(content, false, null);
    }

    public static StreamChunkResponse error(String message) {
        return new StreamChunkResponse(null, false, message);
    }
}
