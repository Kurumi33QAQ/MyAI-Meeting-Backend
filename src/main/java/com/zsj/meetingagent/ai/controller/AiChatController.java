package com.zsj.meetingagent.ai.controller;

import com.zsj.meetingagent.ai.dto.AiChatRequest;
import com.zsj.meetingagent.ai.service.AiChatService;
import com.zsj.meetingagent.ai.vo.AiChatResponse;
import com.zsj.meetingagent.chat.service.ChatSessionService;
import com.zsj.meetingagent.chat.vo.ChatSessionResponse;
import com.zsj.meetingagent.common.result.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 基础 AI 对话接口。
 * 阶段 2 只处理一次请求一次完整回答，SSE 流式输出会在阶段 3 单独实现。
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiChatService aiChatService;
    private final ChatSessionService chatSessionService;

    public AiChatController(AiChatService aiChatService, ChatSessionService chatSessionService) {
        this.aiChatService = aiChatService;
        this.chatSessionService = chatSessionService;
    }

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request, Authentication authentication) {
        String username = currentUsername(authentication);
        ChatSessionResponse session = chatSessionService.ensureSession(
                username,
                request.sessionId(),
                request.message(),
                request.model()
        );
        chatSessionService.saveUserMessage(username, session.sessionId(), request.message(), request.model());
        AiChatResponse response = aiChatService.chat(request);
        chatSessionService.saveAssistantMessage(username, session.sessionId(), response.answer(), response.model());
        return ApiResponse.success(response);
    }

    private String currentUsername(Authentication authentication) {
        return authentication.getName();
    }
}
