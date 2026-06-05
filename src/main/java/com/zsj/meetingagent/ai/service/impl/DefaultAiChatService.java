package com.zsj.meetingagent.ai.service.impl;

import com.zsj.meetingagent.ai.config.AiModelProperties;
import com.zsj.meetingagent.ai.dto.AiChatRequest;
import com.zsj.meetingagent.ai.service.AiChatService;
import com.zsj.meetingagent.ai.vo.AiChatResponse;
import com.zsj.meetingagent.auth.security.LoginUserContext;
import com.zsj.meetingagent.common.exception.BusinessException;
import com.zsj.meetingagent.limit.enums.AiCallOperation;
import com.zsj.meetingagent.limit.model.AiGuardRequest;
import com.zsj.meetingagent.limit.model.AiGuardResult;
import com.zsj.meetingagent.limit.service.AiCallGuardService;
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
 * 负责组织 Prompt、选择模型，并在真实同步调用前接入 AI Guard，降低重复提交、超时和模型异常对业务的影响。
 */
@Service
@EnableConfigurationProperties(AiModelProperties.class)
public class DefaultAiChatService implements AiChatService {

    private static final String AI_CALL_ERROR_CODE = "AI0001";

    private final AiModelProperties aiModelProperties;

    private final ChatClient.Builder chatClientBuilder;

    private final AiCallGuardService aiCallGuardService;

    public DefaultAiChatService(
            AiModelProperties aiModelProperties,
            ChatClient.Builder chatClientBuilder,
            AiCallGuardService aiCallGuardService
    ) {
        this.aiModelProperties = aiModelProperties;
        this.chatClientBuilder = chatClientBuilder;
        this.aiCallGuardService = aiCallGuardService;
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        String model = chooseModel(request.model());
        double temperature = chooseTemperature(request.temperature());
        String systemPrompt = chooseSystemPrompt(request.systemPrompt());
        Instant startedAt = Instant.now();

        String answer;
        long latencyMs;
        if (aiModelProperties.isMockEnabled()) {
            // mock 模式用于本地开发和自动化测试，避免每次测试都依赖真实 API Key、网络和账户余额。
            answer = mockAnswer(request.message());
            latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
        } else {
            /*
             * 真实模型调用统一经过 AI Guard。
             * 这样普通聊天、模拟面试出题和 Agent 复用 AiChatService 时，都能获得限流、Single-flight 和降级能力。
             */
            AiGuardResult guardResult = aiCallGuardService.executeText(
                    buildGuardRequest(request, systemPrompt, model, temperature),
                    () -> callLargeModel(request.message(), systemPrompt, model, temperature)
            );
            answer = guardResult.answer();
            latencyMs = guardResult.latencyMs();
        }
        return new AiChatResponse(answer, model, aiModelProperties.getProvider(), latencyMs, aiModelProperties.isMockEnabled());
    }

    @Override
    public Flux<String> streamChat(AiChatRequest request) {
        String model = chooseModel(request.model());
        double temperature = chooseTemperature(request.temperature());
        String systemPrompt = chooseSystemPrompt(request.systemPrompt());

        if (aiModelProperties.isMockEnabled()) {
            // mock 流式输出也拆成小片段，方便前端验证 SSE 逐段拼接逻辑。
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
             * 同步调用用于普通聊天接口和 Agent 最终回答。
             * 流式接口会走 streamChat，两者共用模型选择和 Prompt 选择逻辑。
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

    private AiGuardRequest buildGuardRequest(AiChatRequest request, String systemPrompt, String model, double temperature) {
        String username = LoginUserContext.tryCurrentUsername().orElse("system");
        String rawKey = "%s|%s|%s|%s".formatted(
                request.message(),
                systemPrompt,
                model,
                temperature
        );
        return AiGuardRequest.of(username, resolveOperation(request.message(), systemPrompt), model, rawKey);
    }

    private AiCallOperation resolveOperation(String message, String systemPrompt) {
        String text = (message == null ? "" : message) + "\n" + (systemPrompt == null ? "" : systemPrompt);
        if (containsAny(text, "模拟面试", "面试题", "出题", "候选人", "简历")) {
            return AiCallOperation.INTERVIEW_QUESTION_GENERATION;
        }
        return AiCallOperation.CHAT_SYNC;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
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
        return "【本地模拟回答】我已经收到你的问题：“%s”。当前 mock 模式用于验证后端接口、SSE 分片、会话存储和 Agent 编排链路；配置真实 API Key 并关闭 mock 后，会调用真实大模型。"
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
