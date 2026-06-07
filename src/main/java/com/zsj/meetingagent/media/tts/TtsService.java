package com.zsj.meetingagent.media.tts;

import java.util.Optional;

/**
 * TTS 语音合成服务。
 * 负责创建任务、查询任务和返回可播放音频内容。
 */
public interface TtsService {

    TtsTaskResponse createTask(String username, TtsRequest request);

    Optional<TtsTaskResponse> getTask(String username, String taskId);

    Optional<byte[]> getAudioBytes(String username, String taskId);

    Optional<byte[]> getPublicAudioBytes(String taskId);
}
