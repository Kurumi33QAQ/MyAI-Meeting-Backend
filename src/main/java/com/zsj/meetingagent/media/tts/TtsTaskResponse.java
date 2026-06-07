package com.zsj.meetingagent.media.tts;

/**
 * TTS 任务响应。
 * 字段名兼容现有前端 xunfeiTtsService 的 normalizeTaskResult 映射。
 */
public record TtsTaskResponse(
        String sid,
        String taskId,
        String taskStatus,
        Integer code,
        String message,
        String audioBase64,
        String audioUrl,
        String pybufContent,
        String pybufUrl,
        boolean completed,
        boolean success
) {
}
