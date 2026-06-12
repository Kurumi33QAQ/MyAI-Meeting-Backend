package com.zsj.meetingagent.media.asr;

import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Instant;

/**
 * 本地降级版音频转写服务。
 * 当前不假装完成真实语音识别，只返回明确中文提示，保证前端录音链路可联调且不误导用户。
 */
@Service
@ConditionalOnProperty(prefix = "app.media.asr", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalFallbackAudioTranscriptionService implements AudioTranscriptionService {

    private static final String FALLBACK_TEXT = "已收到音频，但当前后端处于本地降级转写模式，请直接输入你的回答文本。";

    @Override
    public AudioTranscriptionSession startSession(String websocketSessionId, String username, String clientUserId) {
        return new AudioTranscriptionSession(websocketSessionId, username, clientUserId, Instant.now());
    }

    @Override
    public String acceptAudioChunk(AudioTranscriptionSession session, byte[] audioBytes) {
        session.appendAudio(audioBytes);
        /*
         * ASR 降级模式只在首个音频片段返回一次提示。
         * 如果每个 PCM 分片都返回文本，前端输入框会被重复提示刷屏。
         */
        if (session.fallbackNoticeSent()) {
            return null;
        }
        session.markFallbackNoticeSent();
        return FALLBACK_TEXT;
    }

    @Override
    public String stopSession(AudioTranscriptionSession session) {
        if (session.audioChunkCount() == 0) {
            return "录音已结束，本次没有收到可转写的音频片段。";
        }
        return FALLBACK_TEXT;
    }
}
