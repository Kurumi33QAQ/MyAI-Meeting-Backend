package com.zsj.meetingagent.frontendadapter;

import com.zsj.meetingagent.ai.config.AiModelProperties;
import com.zsj.meetingagent.chat.dto.ChatStreamRequest;
import com.zsj.meetingagent.chat.dto.LegacyChatStreamRequest;
import com.zsj.meetingagent.chat.service.ChatStreamService;
import com.zsj.meetingagent.chat.vo.LegacyConversationResponse;
import com.zsj.meetingagent.chat.vo.LegacyPageResponse;
import com.zsj.meetingagent.common.result.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 旧前端 AI 接口兼容层。
 * 旧路径只在这里出现，核心流式输出仍复用 chat 模块，避免把参考项目命名扩散到新后端结构里。
 */
@RestController
@RequestMapping("/api/xunzhi/v1")
@EnableConfigurationProperties(AiModelProperties.class)
public class LegacyAiController {

    private final ChatStreamService chatStreamService;

    private final AiModelProperties aiModelProperties;

    public LegacyAiController(ChatStreamService chatStreamService, AiModelProperties aiModelProperties) {
        this.chatStreamService = chatStreamService;
        this.aiModelProperties = aiModelProperties;
    }

    @GetMapping("/ai-properties")
    public ApiResponse<LegacyPageResponse<Map<String, Object>>> listAiProperties() {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("id", 1);
        model.put("aiName", "默认对话模型");
        model.put("aiType", aiModelProperties.getProvider());
        model.put("apiUrl", null);
        model.put("modelName", aiModelProperties.getDefaultModel());
        model.put("maxTokens", null);
        model.put("temperature", aiModelProperties.getDefaultTemperature());
        model.put("systemPrompt", aiModelProperties.getDefaultSystemPrompt());
        model.put("isEnabled", 1);
        model.put("enableThinking", 0);
        model.put("thinkingBudgetTokens", null);
        model.put("createTime", Instant.now().toString());
        model.put("updateTime", Instant.now().toString());
        model.put("delFlag", 0);
        return ApiResponse.success(LegacyPageResponse.single(model));
    }

    @GetMapping("/ai/conversations")
    public ApiResponse<LegacyPageResponse<Map<String, Object>>> listConversations(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return ApiResponse.success(LegacyPageResponse.empty(current, size));
    }

    @PostMapping("/ai/conversations")
    public ApiResponse<LegacyConversationResponse> createConversation(@RequestBody Map<String, Object> request) {
        String firstMessage = String.valueOf(request.getOrDefault("firstMessage", ""));
        String title = StringUtils.hasText(firstMessage) ? firstMessage.substring(0, Math.min(firstMessage.length(), 20)) : "新的对话";
        return ApiResponse.success(new LegacyConversationResponse(UUID.randomUUID().toString(), title));
    }

    @GetMapping("/ai/history/{sessionId}")
    public ApiResponse<List<Map<String, Object>>> listHistory(@PathVariable String sessionId) {
        return ApiResponse.success(List.of());
    }

    @GetMapping("/ai/history/page")
    public ApiResponse<LegacyPageResponse<Map<String, Object>>> pageHistory(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size
    ) {
        return ApiResponse.success(LegacyPageResponse.empty(current, size));
    }

    @PostMapping(value = "/ai/sessions/{sessionId}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @PathVariable String sessionId,
            @RequestParam(required = false) String username,
            @Valid @RequestBody LegacyChatStreamRequest request
    ) {
        ChatStreamRequest chatRequest = new ChatStreamRequest(
                request.inputMessage(),
                null,
                null,
                null
        );
        return chatStreamService.stream(chatRequest);
    }
}
