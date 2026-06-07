package com.zsj.meetingagent.frontendadapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.meetingagent.agent.dto.AgentRunRequest;
import com.zsj.meetingagent.agent.service.AgentRunService;
import com.zsj.meetingagent.agent.vo.AgentRunResponse;
import com.zsj.meetingagent.chat.vo.StreamChunkResponse;
import com.zsj.meetingagent.auth.security.LoginUserContext;
import com.zsj.meetingagent.common.exception.BusinessException;
import com.zsj.meetingagent.common.result.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 旧前端 Agent 接口兼容层。
 * 负责把 `/api/xunzhi/v1/agents/**` 请求转发到本项目自己的 Agent Run 服务。
 */
@RestController
@RequestMapping("/api/xunzhi/v1/agents")
public class LegacyAgentController {

    private static final long NO_TIMEOUT = 0L;
    private static final int CHUNK_SIZE = 18;

    private final AgentRunService agentRunService;
    private final ObjectMapper objectMapper;

    public LegacyAgentController(AgentRunService agentRunService, ObjectMapper objectMapper) {
        this.agentRunService = agentRunService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/sessions")
    public ApiResponse<Map<String, Object>> createSession(@RequestBody(required = false) Map<String, Object> request) {
        String sessionId = UUID.randomUUID().toString();
        String firstMessage = stringValue(request, "firstMessage");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("id", sessionId);
        payload.put("conversationTitle", firstMessage == null ? "Agent 会话" : shorten(firstMessage, 30));
        payload.put("status", "CREATED");
        return ApiResponse.success(payload);
    }

    @PostMapping(value = "/sessions/{sessionId}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @PathVariable String sessionId,
            @RequestParam(required = false) String username,
            @RequestBody Map<String, Object> request
    ) {
        SseEmitter emitter = new SseEmitter(NO_TIMEOUT);
        String input = firstStringValue(request, "inputMessage", "message", "content");
        if (input == null || input.isBlank()) {
            completeWithError(emitter, new BusinessException("G0401", "Agent 输入内容不能为空"));
            return emitter;
        }
        String currentUsername = LoginUserContext.currentUsername();

        /*
         * 旧前端期望 SSE；当前核心 AgentRunService 是同步返回。
         * 这里异步执行 Agent 后把最终回答分片发送，保证旧页面能接收流式格式。
         */
        CompletableFuture.runAsync(() -> {
            try {
                AgentRunResponse run = agentRunService.run(
                        currentUsername,
                        new AgentRunRequest(input, sessionId, null, null)
                );
                sendTextAsChunks(emitter, run.finalAnswer() == null ? "Agent 没有生成最终回答。" : run.finalAnswer());
                completeStream(emitter);
            } catch (Exception ex) {
                completeWithError(emitter, ex);
            }
        });
        return emitter;
    }

    private void sendTextAsChunks(SseEmitter emitter, String text) throws IOException {
        for (int start = 0; start < text.length(); start += CHUNK_SIZE) {
            String chunk = text.substring(start, Math.min(start + CHUNK_SIZE, text.length()));
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(objectMapper.writeValueAsString(StreamChunkResponse.content(chunk))));
        }
    }

    private void completeStream(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
        } catch (IOException ignored) {
            // 浏览器主动断开时可能无法发送 done，直接完成即可。
        } finally {
            emitter.complete();
        }
    }

    private void completeWithError(SseEmitter emitter, Exception error) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(objectMapper.writeValueAsString(StreamChunkResponse.error("Agent 流式响应失败：" + error.getMessage()))));
        } catch (IOException ignored) {
            // 连接不可用时无法再推送错误事件。
        } finally {
            emitter.complete();
        }
    }

    private String firstStringValue(Map<String, Object> request, String... keys) {
        for (String key : keys) {
            String value = stringValue(request, key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String stringValue(Map<String, Object> request, String key) {
        if (request == null) {
            return null;
        }
        Object value = request.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String shorten(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
