package com.zsj.meetingagent.media.tts;

import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地降级版 TTS 服务。
 * 当前生成一段短静音 WAV，保证前端播放链路可验证；真实发音供应商后续只需替换本服务实现。
 */
@Service
@ConditionalOnProperty(prefix = "app.media.tts", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalFallbackTtsService implements TtsService {

    private static final int SAMPLE_RATE = 16000;
    private static final int SILENCE_MILLIS = 300;
    private final Map<String, TtsTask> tasks = new ConcurrentHashMap<>();

    @Override
    public TtsTaskResponse createTask(String username, TtsRequest request) {
        String taskId = UUID.randomUUID().toString();
        byte[] audioBytes = createSilentWav();
        TtsTask task = new TtsTask(taskId, username, request.text(), Instant.now(), audioBytes);
        tasks.put(taskId, task);
        return toResponse(task);
    }

    @Override
    public Optional<TtsTaskResponse> getTask(String username, String taskId) {
        return findTask(username, taskId).map(this::toResponse);
    }

    @Override
    public Optional<byte[]> getAudioBytes(String username, String taskId) {
        return findTask(username, taskId).map(TtsTask::audioBytes);
    }

    @Override
    public Optional<byte[]> getPublicAudioBytes(String taskId) {
        return Optional.ofNullable(tasks.get(taskId)).map(TtsTask::audioBytes);
    }

    private Optional<TtsTask> findTask(String username, String taskId) {
        TtsTask task = tasks.get(taskId);
        if (task == null || !task.username().equals(username)) {
            return Optional.empty();
        }
        return Optional.of(task);
    }

    private TtsTaskResponse toResponse(TtsTask task) {
        String audioUrl = "/api/media/tts/tasks/" + task.taskId() + "/audio";
        return new TtsTaskResponse(
                task.taskId(),
                task.taskId(),
                "5",
                0,
                "当前使用本地降级 TTS，已生成可播放的静音音频占位。",
                null,
                audioUrl,
                null,
                audioUrl,
                true,
                true
        );
    }

    private byte[] createSilentWav() {
        int sampleCount = SAMPLE_RATE * SILENCE_MILLIS / 1000;
        int dataSize = sampleCount * 2;
        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + dataSize);
        writeAscii(out, "RIFF");
        writeInt(out, 36 + dataSize);
        writeAscii(out, "WAVE");
        writeAscii(out, "fmt ");
        writeInt(out, 16);
        writeShort(out, (short) 1);
        writeShort(out, (short) 1);
        writeInt(out, SAMPLE_RATE);
        writeInt(out, SAMPLE_RATE * 2);
        writeShort(out, (short) 2);
        writeShort(out, (short) 16);
        writeAscii(out, "data");
        writeInt(out, dataSize);
        out.writeBytes(new byte[dataSize]);
        return out.toByteArray();
    }

    private void writeAscii(ByteArrayOutputStream out, String value) {
        out.writeBytes(value.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    private void writeInt(ByteArrayOutputStream out, int value) {
        out.writeBytes(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array());
    }

    private void writeShort(ByteArrayOutputStream out, short value) {
        out.writeBytes(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array());
    }
}
