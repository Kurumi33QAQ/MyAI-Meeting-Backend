package com.zsj.meetingagent.media.tts;

import java.time.Instant;

/**
 * TTS 任务实体。
 * 当前保存在内存中用于本地联调，后续可迁移到 Redis 或 MySQL 保存异步任务状态。
 */
public record TtsTask(
        String taskId,
        String username,
        String text,
        Instant createdAt,
        byte[] audioBytes
) {
}
