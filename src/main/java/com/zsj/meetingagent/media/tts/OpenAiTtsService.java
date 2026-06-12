package com.zsj.meetingagent.media.tts;

import com.zsj.meetingagent.media.MediaProviderProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenAI Compatible TTS 实现。
 * 调用 /v1/audio/speech 生成真实音频，并复用现有任务查询和音频下载接口。
 */
@Service
@ConditionalOnProperty(prefix = "app.media.tts", name = "provider", havingValue = "openai")
public class OpenAiTtsService implements TtsService {

    private final MediaProviderProperties properties;
    private final Environment environment;
    private final RestClient restClient;
    private final Map<String, TtsTask> tasks = new ConcurrentHashMap<>();

    public OpenAiTtsService(MediaProviderProperties properties, Environment environment, RestClient.Builder builder) {
        this.properties = properties;
        this.environment = environment;
        this.restClient = builder.build();
    }

    @Override
    public TtsTaskResponse createTask(String username, TtsRequest request) {
        String taskId = UUID.randomUUID().toString();
        String apiKey = environment.getProperty("spring.ai.openai.api-key", "");
        String baseUrl = environment.getProperty("spring.ai.openai.base-url", "https://api.openai.com");
        if (!StringUtils.hasText(apiKey) || apiKey.contains("dummy")) {
            return failed(taskId, "真实 TTS 未配置 API Key，请配置 OPENAI_API_KEY 后重试。");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", properties.getTts().getModel());
            body.put("input", request.text());
            body.put("voice", StringUtils.hasText(request.vcn()) ? request.vcn() : properties.getTts().getVoice());
            body.put("response_format", normalizeResponseFormat());
            byte[] audioBytes = restClient.post()
                    .uri(trimTrailingSlash(baseUrl) + "/v1/audio/speech")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(byte[].class);
            if (audioBytes == null || audioBytes.length == 0) {
                return failed(taskId, "TTS 服务没有返回可播放音频。");
            }
            TtsTask task = new TtsTask(taskId, username, request.text(), Instant.now(), audioBytes);
            tasks.put(taskId, task);
            return success(task, "真实 TTS 语音合成完成。");
        } catch (RuntimeException ex) {
            return failed(taskId, "真实 TTS 调用失败：" + ex.getMessage());
        }
    }

    @Override
    public Optional<TtsTaskResponse> getTask(String username, String taskId) {
        return findTask(username, taskId).map(task -> success(task, "真实 TTS 语音合成完成。"));
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

    private TtsTaskResponse success(TtsTask task, String message) {
        String audioUrl = "/api/media/tts/tasks/" + task.taskId() + "/audio";
        return new TtsTaskResponse(
                task.taskId(),
                task.taskId(),
                "5",
                0,
                message,
                null,
                audioUrl,
                null,
                audioUrl,
                true,
                true
        );
    }

    private TtsTaskResponse failed(String taskId, String message) {
        return new TtsTaskResponse(
                taskId,
                taskId,
                "failed",
                1,
                message,
                null,
                null,
                null,
                null,
                true,
                false
        );
    }

    private String normalizeResponseFormat() {
        String format = properties.getTts().getResponseFormat();
        return StringUtils.hasText(format) ? format : "wav";
    }

    private String trimTrailingSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
