package com.zsj.meetingagent.media.asr;

import java.time.Instant;

/**
 * 音频转写会话状态。
 * 当前记录连接、音频片段数量和降级提示，后续接入真实 ASR 时可扩展为供应商会话。
 */
public class AudioTranscriptionSession {

    private final String sessionId;
    private final String username;
    private final String clientUserId;
    private final Instant startedAt;
    private int audioChunkCount;
    private boolean fallbackNoticeSent;

    public AudioTranscriptionSession(String sessionId, String username, String clientUserId, Instant startedAt) {
        this.sessionId = sessionId;
        this.username = username;
        this.clientUserId = clientUserId;
        this.startedAt = startedAt;
    }

    public String sessionId() {
        return sessionId;
    }

    public String username() {
        return username;
    }

    public String clientUserId() {
        return clientUserId;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public int audioChunkCount() {
        return audioChunkCount;
    }

    public void increaseAudioChunkCount() {
        this.audioChunkCount++;
    }

    public boolean fallbackNoticeSent() {
        return fallbackNoticeSent;
    }

    public void markFallbackNoticeSent() {
        this.fallbackNoticeSent = true;
    }
}
