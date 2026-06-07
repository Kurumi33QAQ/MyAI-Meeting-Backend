package com.zsj.meetingagent.media.asr;

/**
 * 音频转写服务。
 * 负责管理 WebSocket 录音会话，并在没有真实 ASR 供应商时提供中文降级提示。
 */
public interface AudioTranscriptionService {

    AudioTranscriptionSession startSession(String websocketSessionId, String username, String clientUserId);

    String acceptAudioChunk(AudioTranscriptionSession session, int bytesLength);

    String stopSession(AudioTranscriptionSession session);
}
