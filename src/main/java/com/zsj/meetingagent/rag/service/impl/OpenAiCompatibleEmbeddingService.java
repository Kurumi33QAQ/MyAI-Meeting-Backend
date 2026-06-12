package com.zsj.meetingagent.rag.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.zsj.meetingagent.common.exception.BusinessException;
import com.zsj.meetingagent.rag.config.RagProperties;
import com.zsj.meetingagent.rag.service.EmbeddingService;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 embedding 服务。
 * 真实模式调用 /v1/embeddings；测试或本地无 Key 时可使用确定性 mock 向量，但 mock 向量不能写成真实模型效果。
 */
@Service
public class OpenAiCompatibleEmbeddingService implements EmbeddingService {

    private final RagProperties ragProperties;
    private final Environment environment;
    private final RestClient restClient;

    public OpenAiCompatibleEmbeddingService(RagProperties ragProperties, Environment environment, RestClient.Builder restClientBuilder) {
        this.ragProperties = ragProperties;
        this.environment = environment;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public List<Double> embed(String text) {
        RagProperties.Vector vector = ragProperties.getVector();
        if (vector.isEmbeddingMockEnabled()) {
            return deterministicEmbedding(text, vector.getDimension());
        }
        String apiKey = environment.getProperty("spring.ai.openai.api-key", "");
        String baseUrl = environment.getProperty("spring.ai.openai.base-url", "https://api.openai.com");
        if (!StringUtils.hasText(apiKey) || apiKey.contains("dummy")) {
            throw new BusinessException("R0501", "向量化需要配置真实 OPENAI_API_KEY，或仅在测试环境启用 RAG_VECTOR_EMBEDDING_MOCK_ENABLED");
        }

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", vector.getEmbeddingModel());
        request.put("input", text);

        JsonNode response = restClient.post()
                .uri(trimTrailingSlash(baseUrl) + "/v1/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(request)
                .retrieve()
                .body(JsonNode.class);
        JsonNode embeddingNode = response == null ? null : response.path("data").path(0).path("embedding");
        if (embeddingNode == null || !embeddingNode.isArray() || embeddingNode.isEmpty()) {
            throw new BusinessException("R0502", "Embedding 服务没有返回可用向量");
        }
        List<Double> embedding = new ArrayList<>();
        embeddingNode.forEach(node -> embedding.add(node.asDouble()));
        return embedding;
    }

    private List<Double> deterministicEmbedding(String text, int dimension) {
        byte[] seed = sha256(StringUtils.hasText(text) ? text : "");
        List<Double> values = new ArrayList<>(dimension);
        double norm = 0.0;
        for (int i = 0; i < dimension; i++) {
            int value = seed[i % seed.length] & 0xff;
            double normalized = (value / 255.0) * 2.0 - 1.0;
            values.add(normalized);
            norm += normalized * normalized;
        }
        double sqrt = Math.sqrt(Math.max(norm, 1e-9));
        for (int i = 0; i < values.size(); i++) {
            values.set(i, values.get(i) / sqrt);
        }
        return values;
    }

    private byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }

    private String trimTrailingSlash(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "https://api.openai.com";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
