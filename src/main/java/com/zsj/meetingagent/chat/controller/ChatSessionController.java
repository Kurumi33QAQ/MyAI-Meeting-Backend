package com.zsj.meetingagent.chat.controller;

import com.zsj.meetingagent.chat.dto.CreateChatSessionRequest;
import com.zsj.meetingagent.chat.service.ChatSessionService;
import com.zsj.meetingagent.chat.vo.ChatMessageResponse;
import com.zsj.meetingagent.chat.vo.ChatSessionResponse;
import com.zsj.meetingagent.common.result.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 聊天会话接口。
 * 提供本项目新风格的会话创建、会话列表和消息历史接口。
 * 旧前端路径继续放在 frontendadapter 中适配，后续接口命名统一时再逐步迁移。
 */
@RestController
@RequestMapping("/api/ai/sessions")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    public ChatSessionController(ChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService;
    }

    @PostMapping
    public ApiResponse<ChatSessionResponse> createSession(
            @Valid @RequestBody CreateChatSessionRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(chatSessionService.createSession(
                authentication.getName(),
                request.firstMessage(),
                request.model()
        ));
    }

    @GetMapping
    public ApiResponse<List<ChatSessionResponse>> listSessions(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            Authentication authentication
    ) {
        return ApiResponse.success(chatSessionService.listSessions(authentication.getName(), current, size));
    }

    @GetMapping("/{sessionId}/messages")
    public ApiResponse<List<ChatMessageResponse>> listMessages(
            @PathVariable String sessionId,
            Authentication authentication
    ) {
        return ApiResponse.success(chatSessionService.listMessages(authentication.getName(), sessionId));
    }
}
