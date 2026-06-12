package com.zsj.meetingagent.media.asr;

import com.fasterxml.jackson.databind.JsonNode;
import com.zsj.meetingagent.media.MediaProviderProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Instant;

/**
 * OpenAI Compatible ASR 实现。
 * WebSocket 接收音频分片，停止录音时把累计音频提交到 /v1/audio/transcriptions 进行真实转写。
 */
@Service
@ConditionalOnProperty(prefix = "app.media.asr", name = "provider", havingValue = "openai")
public class OpenAiAudioTranscriptionService implements AudioTranscriptionService {

    private final MediaProviderProperties properties;
    private final Environment environment;
    private final RestClient restClient;

    public OpenAiAudioTranscriptionService(MediaProviderProperties properties, Environment environment, RestClient.Builder builder) {
        this.properties = properties;
        this.environment = environment;
        this.restClient = builder.build();
    }

    @Override
    public AudioTranscriptionSession startSession(String websocketSessionId, String username, String clientUserId) {
        return new AudioTranscriptionSession(websocketSessionId, username, clientUserId, Instant.now());
    }

    @Override
    public String acceptAudioChunk(AudioTranscriptionSession session, byte[] audioBytes) {
        session.appendAudio(audioBytes);
        // OpenAI Whisper 类接口不是实时双向流式协议，这里只累计音频，stop 时统一转写。
        return null;
    }

    @Override
    public String stopSession(AudioTranscriptionSession session) {
        byte[] audioBytes = session.audioBytes();
        if (audioBytes.length == 0) {
            return "录音已结束，本次没有收到可转写的音频片段。";
        }
        String apiKey = environment.getProperty("spring.ai.openai.api-key", "");
        String baseUrl = environment.getProperty("spring.ai.openai.base-url", "https://api.openai.com");
        if (!StringUtils.hasText(apiKey) || apiKey.contains("dummy")) {
            return "真实 ASR 未配置 API Key，请配置 OPENAI_API_KEY 后重试。";
        }
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("model", properties.getAsr().getModel());
            body.add("language", properties.getAsr().getLanguage());
            body.add("file", new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return "meeting-answer.wav";
                }
            });
            JsonNode response = restClient.post()
                    .uri(trimTrailingSlash(baseUrl) + "/v1/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String text = response == null ? "" : response.path("text").asText("");
            return StringUtils.hasText(text) ? text : "ASR 服务没有返回可用文本，请检查音频格式。";
        } catch (RuntimeException ex) {
            return "真实 ASR 调用失败：" + ex.getMessage();
        }
    }

    private String trimTrailingSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
