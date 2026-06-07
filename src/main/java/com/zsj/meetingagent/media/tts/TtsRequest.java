package com.zsj.meetingagent.media.tts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * TTS 语音合成请求。
 * 当前只强制要求 text，音色、语速、音量等参数保留给真实供应商接入。
 */
public record TtsRequest(
        @NotBlank(message = "不能为空")
        @Size(max = 2000, message = "长度不能超过 2000 个字符")
        String text,
        String vcn,
        String language,
        Integer speed,
        Integer volume,
        Integer pitch,
        Integer rhy,
        String audioEncoding,
        Integer sampleRate,
        Integer timeoutSeconds,
        Integer pollIntervalMs
) {
}
