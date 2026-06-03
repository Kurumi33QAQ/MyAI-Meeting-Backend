package com.zsj.meetingagent.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.meetingagent.ai.dto.AiChatRequest;
import com.zsj.meetingagent.ai.service.AiChatService;
import com.zsj.meetingagent.chat.dto.ChatStreamRequest;
import com.zsj.meetingagent.chat.service.ChatStreamService;
import com.zsj.meetingagent.chat.vo.StreamChunkResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;

/**
 * 基于 SseEmitter 的流式聊天实现。
 * 当前项目使用 Spring MVC，所以先用 SseEmitter；如果后续整体切到 WebFlux，再考虑返回 Flux。
 */
@Service
public class SseChatStreamService implements ChatStreamService {

    private static final long NO_TIMEOUT = 0L;

    private final AiChatService aiChatService;

    private final ObjectMapper objectMapper;

    public SseChatStreamService(AiChatService aiChatService, ObjectMapper objectMapper) {
        this.aiChatService = aiChatService;
        this.objectMapper = objectMapper;
    }

    @Override
    public SseEmitter stream(ChatStreamRequest request) {
        SseEmitter emitter = new SseEmitter(NO_TIMEOUT);
        AiChatRequest aiRequest = new AiChatRequest(
                request.message(),
                request.model(),
                request.systemPrompt(),
                request.temperature()
        );

        /*
         * 订阅模型增量输出后，每个 chunk 都转成 SSE data 推给前端。
         * 前端 fetchEventSource 会解析 data.content，并逐段拼接成最终回答。
         */
        Disposable disposable = aiChatService.streamChat(aiRequest)
                .subscribe(
                        chunk -> sendChunk(emitter, chunk),
                        error -> completeWithError(emitter, error),
                        () -> completeStream(emitter)
                );

        emitter.onTimeout(() -> {
            disposable.dispose();
            emitter.complete();
        });
        emitter.onCompletion(disposable::dispose);
        emitter.onError(error -> disposable.dispose());

        return emitter;
    }

    private void sendChunk(SseEmitter emitter, String content) {
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(objectMapper.writeValueAsString(StreamChunkResponse.content(content))));
        } catch (IOException ex) {
            throw new IllegalStateException("SSE 消息发送失败", ex);
        }
    }

    private void completeStream(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name("done")
                    .data("[DONE]"));
        } catch (IOException ignored) {
            // 浏览器断开连接时可能发送失败，此时直接完成即可。
        } finally {
            emitter.complete();
        }
    }

    private void completeWithError(SseEmitter emitter, Throwable error) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(objectMapper.writeValueAsString(StreamChunkResponse.error("AI 流式响应失败"))));
        } catch (IOException ignored) {
            // 如果连接已经断开，无法再发送错误事件。
        } finally {
            emitter.completeWithError(error);
        }
    }
}
