package com.zsj.meetingagent.ai.service.impl;

import com.zsj.meetingagent.ai.config.AiModelProperties;
import com.zsj.meetingagent.ai.dto.AiChatRequest;
import com.zsj.meetingagent.ai.service.AiChatService;
import com.zsj.meetingagent.ai.vo.AiChatResponse;
import com.zsj.meetingagent.common.exception.BusinessException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 默认 AI 对话服务实现。
 * 负责组织 Prompt、选择模型，并根据 mock 开关决定走本地模拟回答还是真实 Spring AI 调用。
 */
@Service
@EnableConfigurationProperties(AiModelProperties.class)
public class DefaultAiChatService implements AiChatService {

    private static final String AI_CALL_ERROR_CODE = "AI0001";

    private final AiModelProperties aiModelProperties;

    private final ChatClient.Builder chatClientBuilder;

    public DefaultAiChatService(AiModelProperties aiModelProperties, ChatClient.Builder chatClientBuilder) {
        this.aiModelProperties = aiModelProperties;
        this.chatClientBuilder = chatClientBuilder;
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        String model = chooseModel(request.model());
        double temperature = chooseTemperature(request.temperature());
        String systemPrompt = chooseSystemPrompt(request.systemPrompt());
        Instant startedAt = Instant.now();

        String answer = aiModelProperties.isMockEnabled()
                ? mockAnswer(request.message())
                : callLargeModel(request.message(), systemPrompt, model, temperature);

        long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
        return new AiChatResponse(answer, model, aiModelProperties.getProvider(), latencyMs, aiModelProperties.isMockEnabled());
    }

    @Override
    public Flux<String> streamChat(AiChatRequest request) {
        String model = chooseModel(request.model());
        double temperature = chooseTemperature(request.temperature());
        String systemPrompt = chooseSystemPrompt(request.systemPrompt());

        if (aiModelProperties.isMockEnabled()) {
            return Flux.fromIterable(splitForMockStream(mockAnswer(request.message())))
                    .delayElements(Duration.ofMillis(35));
        }

        try {
            /*
             * Spring AI 的 stream() 会把模型增量输出转换为 Flux。
             * Controller 层不用关心供应商细节，只需要把这些片段转成 SSE 事件发送给前端。
             */
            return chatClientBuilder
                    .defaultOptions(OpenAiChatOptions.builder()
                            .model(model)
                            .temperature(temperature)
                            .build())
                    .build()
                    .prompt()
                    .system(systemPrompt)
                    .user(request.message())
                    .stream()
                    .content();
        } catch (Exception ex) {
            return Flux.error(new BusinessException(AI_CALL_ERROR_CODE, "AI 模型流式调用失败，请检查 API Key、模型名称或网络配置"));
        }
    }

    private String callLargeModel(String message, String systemPrompt, String model, double temperature) {
        try {
            /*
             * 同步调用用于阶段 2 的普通对话接口。
             * 阶段 3 新增的 streamChat 会走流式调用，两者共用模型选择和 Prompt 选择逻辑。
             */
            return chatClientBuilder
                    .defaultOptions(OpenAiChatOptions.builder()
                            .model(model)
                            .temperature(temperature)
                            .build())
                    .build()
                    .prompt()
                    .system(systemPrompt)
                    .user(message)
                    .call()
                    .content();
        } catch (Exception ex) {
            throw new BusinessException(AI_CALL_ERROR_CODE, "AI 模型调用失败，请检查 API Key、模型名称或网络配置");
        }
    }

    private String chooseModel(String requestedModel) {
        if (StringUtils.hasText(requestedModel)) {
            return requestedModel.trim();
        }
        return aiModelProperties.getDefaultModel();
    }

    private double chooseTemperature(Double requestedTemperature) {
        if (requestedTemperature != null) {
            return requestedTemperature;
        }
        return aiModelProperties.getDefaultTemperature();
    }

    private String chooseSystemPrompt(String requestedSystemPrompt) {
        if (StringUtils.hasText(requestedSystemPrompt)) {
            return requestedSystemPrompt.trim();
        }
        return aiModelProperties.getDefaultSystemPrompt();
    }

    private String mockAnswer(String message) {
        return "【本地模拟回答】我已经收到你的问题：“%s”。阶段 3 当前用于验证 SSE 流式输出链路；配置真实 API Key 并关闭 mock 后，会改为流式调用真实大模型。"
                .formatted(message);
    }

    private List<String> splitForMockStream(String answer) {
        List<String> chunks = new ArrayList<>();
        int chunkSize = 4;
        for (int index = 0; index < answer.length(); index += chunkSize) {
            chunks.add(answer.substring(index, Math.min(index + chunkSize, answer.length())));
        }
        return chunks;
    }
}
