package com.zsj.meetingagent.chat.service;

import com.zsj.meetingagent.chat.dto.ChatStreamRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 流式聊天服务。
 * 把 AI 模型增量输出转换为浏览器可以逐段接收的 Server-Sent Events。
 */
public interface ChatStreamService {

    SseEmitter stream(ChatStreamRequest request);
}
