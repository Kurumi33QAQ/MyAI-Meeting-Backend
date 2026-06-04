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
 * 当前提供一次请求一次完整回答的同步聊天能力，并把用户输入和 AI 回复写入 MongoDB 会话历史。
 * SSE 流式聊天由 chat 模块的 ChatStreamController 提供，Agent 最终回答会复用 AiChatService。
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
        // 先保存用户消息，再调用模型；这样即使模型调用失败，也能保留用户真实输入用于排查问题。
        chatSessionService.saveUserMessage(username, session.sessionId(), request.message(), request.model());
        AiChatResponse response = aiChatService.chat(request);
        // AI 回复保存到同一个 session，前端刷新历史时才能看到完整的一问一答。
        chatSessionService.saveAssistantMessage(username, session.sessionId(), response.answer(), response.model());
        return ApiResponse.success(response);
    }

    private String currentUsername(Authentication authentication) {
        return authentication.getName();
    }
}
