package com.zsj.meetingagent.chat.controller;

import com.zsj.meetingagent.auth.security.LoginUserContext;
import com.zsj.meetingagent.chat.dto.ChatStreamRequest;
import com.zsj.meetingagent.chat.service.ChatStreamService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 新风格 SSE 流式聊天接口。
 * 该接口属于本项目自己的路径风格，旧前端路径只放在 frontendadapter 中做兼容。
 */
@RestController
@RequestMapping("/api/ai/chat")
public class ChatStreamController {

    private final ChatStreamService chatStreamService;

    public ChatStreamController(ChatStreamService chatStreamService) {
        this.chatStreamService = chatStreamService;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatStreamRequest request) {
        return chatStreamService.stream(request, LoginUserContext.currentUsername());
    }
}
