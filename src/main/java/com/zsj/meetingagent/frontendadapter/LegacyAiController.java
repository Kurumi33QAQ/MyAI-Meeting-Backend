package com.zsj.meetingagent.frontendadapter;

import com.zsj.meetingagent.ai.config.AiModelProperties;
import com.zsj.meetingagent.chat.dto.ChatStreamRequest;
import com.zsj.meetingagent.chat.dto.LegacyChatStreamRequest;
import com.zsj.meetingagent.chat.service.ChatSessionService;
import com.zsj.meetingagent.chat.service.ChatStreamService;
import com.zsj.meetingagent.chat.vo.ChatMessageResponse;
import com.zsj.meetingagent.chat.vo.LegacyConversationResponse;
import com.zsj.meetingagent.chat.vo.LegacyPageResponse;
import com.zsj.meetingagent.chat.vo.ChatSessionResponse;
import com.zsj.meetingagent.common.result.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
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

/**
 * 旧前端 AI 接口兼容层。
 * 旧路径只在这里出现，核心流式输出仍复用 chat 模块，避免把参考项目命名扩散到新后端结构里。
 */
@RestController
@RequestMapping("/api/xunzhi/v1")
@EnableConfigurationProperties(AiModelProperties.class)
public class LegacyAiController {

    private final ChatSessionService chatSessionService;

    private final ChatStreamService chatStreamService;

    private final AiModelProperties aiModelProperties;

    public LegacyAiController(
            ChatSessionService chatSessionService,
            ChatStreamService chatStreamService,
            AiModelProperties aiModelProperties
    ) {
        this.chatSessionService = chatSessionService;
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
            @RequestParam(defaultValue = "10") long size,
            Authentication authentication
    ) {
        String username = authentication.getName();
        List<Map<String, Object>> records = chatSessionService.listSessions(username, current, size)
                .stream()
                .map(this::toLegacyConversationRecord)
                .toList();
        return ApiResponse.success(LegacyPageResponse.of(records, chatSessionService.countSessions(username), current, size));
    }

    @PostMapping("/ai/conversations")
    public ApiResponse<LegacyConversationResponse> createConversation(
            @RequestBody Map<String, Object> request,
            Authentication authentication
    ) {
        String firstMessage = String.valueOf(request.getOrDefault("firstMessage", ""));
        ChatSessionResponse session = chatSessionService.createSession(authentication.getName(), firstMessage, aiModelProperties.getDefaultModel());
        return ApiResponse.success(new LegacyConversationResponse(session.sessionId(), session.title()));
    }

    @GetMapping("/ai/history/{sessionId}")
    public ApiResponse<List<Map<String, Object>>> listHistory(@PathVariable String sessionId, Authentication authentication) {
        return ApiResponse.success(chatSessionService.listMessages(authentication.getName(), sessionId)
                .stream()
                .map(this::toLegacyMessageRecord)
                .toList());
    }

    @GetMapping("/ai/history/page")
    public ApiResponse<LegacyPageResponse<Map<String, Object>>> pageHistory(
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            Authentication authentication
    ) {
        if (sessionId == null || sessionId.isBlank()) {
            return ApiResponse.success(LegacyPageResponse.empty(current, size));
        }
        List<Map<String, Object>> records = chatSessionService.listMessages(authentication.getName(), sessionId)
                .stream()
                .map(this::toLegacyMessageRecord)
                .toList();
        return ApiResponse.success(LegacyPageResponse.of(records, records.size(), current, size));
    }

    @PostMapping(value = "/ai/sessions/{sessionId}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @PathVariable String sessionId,
            @RequestParam(required = false) String username,
            @Valid @RequestBody LegacyChatStreamRequest request,
            Authentication authentication
    ) {
        ChatStreamRequest chatRequest = new ChatStreamRequest(
                request.inputMessage(),
                sessionId,
                null,
                null,
                null
        );
        return chatStreamService.stream(chatRequest, authentication.getName());
    }

    private Map<String, Object> toLegacyConversationRecord(ChatSessionResponse session) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", session.sessionId());
        record.put("sessionId", session.sessionId());
        record.put("conversationTitle", session.title());
        record.put("title", session.title());
        record.put("modelName", session.model());
        record.put("messageCount", session.messageCount());
        record.put("createTime", session.createdAt());
        record.put("updateTime", session.updatedAt());
        record.put("delFlag", 0);
        return record;
    }

    private Map<String, Object> toLegacyMessageRecord(ChatMessageResponse message) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", message.id());
        record.put("sessionId", message.sessionId());
        record.put("role", message.role());
        record.put("messageType", message.role());
        record.put("content", message.content());
        record.put("messageContent", message.content());
        record.put("modelName", message.model());
        record.put("messageSeq", message.sequence());
        record.put("createTime", message.createdAt());
        record.put("delFlag", 0);
        return record;
    }
}
