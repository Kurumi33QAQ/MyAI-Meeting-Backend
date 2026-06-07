package com.zsj.meetingagent.websocket.speech;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.meetingagent.media.asr.AudioTranscriptionService;
import com.zsj.meetingagent.media.asr.AudioTranscriptionSession;
import com.zsj.meetingagent.websocket.auth.WebSocketLoginUser;
import com.zsj.meetingagent.websocket.auth.WebSocketTokenVerifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音转文字 WebSocket 处理器。
 * 负责 token 鉴权、控制指令处理、音频分片接收和前端增量转写事件返回。
 */
@Component
public class SpeechTranscriptionWebSocketHandler extends AbstractWebSocketHandler {

    private final WebSocketTokenVerifier tokenVerifier;
    private final AudioTranscriptionService transcriptionService;
    private final ObjectMapper objectMapper;
    private final Map<String, AudioTranscriptionSession> sessions = new ConcurrentHashMap<>();

    public SpeechTranscriptionWebSocketHandler(
            WebSocketTokenVerifier tokenVerifier,
            AudioTranscriptionService transcriptionService,
            ObjectMapper objectMapper
    ) {
        this.tokenVerifier = tokenVerifier;
        this.transcriptionService = transcriptionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        WebSocketLoginUser loginUser = tokenVerifier.verify(session)
                .orElse(null);
        if (loginUser == null) {
            sendEvent(session, "error", Map.of("message", "请先登录后再使用语音转写"));
            session.close(CloseStatus.POLICY_VIOLATION.withReason("未登录"));
            return;
        }
        String clientUserId = extractClientUserId(session);
        AudioTranscriptionSession transcriptionSession = transcriptionService.startSession(
                session.getId(),
                loginUser.username(),
                clientUserId
        );
        sessions.put(session.getId(), transcriptionSession);
        sendEvent(session, "connected", Map.of(
                "message", "语音转写连接已建立",
                "sessionId", transcriptionSession.sessionId(),
                "userId", transcriptionSession.clientUserId()
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<?, ?> payload = objectMapper.readValue(message.getPayload(), Map.class);
        Object rawType = payload.get("type");
        String type = rawType == null ? "" : String.valueOf(rawType);
        AudioTranscriptionSession transcriptionSession = sessions.get(session.getId());
        if (transcriptionSession == null) {
            sendEvent(session, "error", Map.of("message", "语音转写会话不存在，请重新连接"));
            return;
        }
        switch (type) {
            case "ping" -> sendEvent(session, "pong", Map.of("message", "pong"));
            case "start_transcription" -> sendEvent(session, "transcription_started", Map.of("message", "语音转写已开始"));
            case "stop_transcription" -> sendEvent(session, "final", Map.of(
                    "data", transcriptionService.stopSession(transcriptionSession),
                    "message", "语音转写已结束"
            ));
            case "get_status" -> sendEvent(session, "status", Map.of(
                    "message", "语音转写会话运行中",
                    "chunkCount", transcriptionSession.audioChunkCount(),
                    "startedAt", transcriptionSession.startedAt().toString()
            ));
            default -> sendEvent(session, "error", Map.of("message", "不支持的语音转写指令：" + type));
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        AudioTranscriptionSession transcriptionSession = sessions.get(session.getId());
        if (transcriptionSession == null) {
            sendEvent(session, "error", Map.of("message", "语音转写会话不存在，请重新连接"));
            return;
        }
        String text = transcriptionService.acceptAudioChunk(transcriptionSession, message.getPayloadLength());
        if (StringUtils.hasText(text)) {
            sendEvent(session, "transcription", Map.of(
                    "data", text,
                    "message", "当前使用本地降级转写"
            ));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    private void sendEvent(WebSocketSession session, String type, Map<String, Object> payload) throws IOException {
        if (!session.isOpen()) {
            return;
        }
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("type", type);
        body.put("timestamp", Instant.now().toEpochMilli());
        body.putAll(payload);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(body)));
    }

    private String extractClientUserId(WebSocketSession session) {
        String path = session.getUri() == null ? "" : session.getUri().getPath();
        int index = path.lastIndexOf('/');
        if (index < 0 || index == path.length() - 1) {
            return "unknown";
        }
        return path.substring(index + 1);
    }
}
