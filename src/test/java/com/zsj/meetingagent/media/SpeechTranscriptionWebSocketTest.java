package com.zsj.meetingagent.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 语音转写 WebSocket 集成测试。
 * 验证 Sa-Token token 可以通过查询参数完成握手鉴权，并能收发控制事件和音频分片。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.ai.mock-enabled=true",
                "spring.ai.openai.chat.options.model=gpt-4o-mini"
        }
)
class SpeechTranscriptionWebSocketTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void websocketAcceptsTokenAndReturnsTranscriptionEvents() throws Exception {
        String token = registerAndLogin();
        CountDownLatch latch = new CountDownLatch(4);
        List<String> eventTypes = new CopyOnWriteArrayList<>();

        WebSocketSession session = new StandardWebSocketClient()
                .execute(new TextWebSocketHandler() {
                    @Override
                    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                        JsonNode node = objectMapper.readTree(message.getPayload());
                        eventTypes.add(node.path("type").asText());
                        latch.countDown();
                    }
                }, "ws://localhost:" + port + "/api/ws/speech/transcription/1?token=" + token)
                .get(5, TimeUnit.SECONDS);

        session.sendMessage(new TextMessage("{\"type\":\"start_transcription\"}"));
        session.sendMessage(new BinaryMessage(new byte[]{1, 2, 3, 4}));
        session.sendMessage(new TextMessage("{\"type\":\"stop_transcription\"}"));

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(eventTypes).contains("connected", "transcription_started", "transcription", "final");
        session.close();
    }

    private String registerAndLogin() throws Exception {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
        String username = "ws_user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        client.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "123456",
                          "realName": "WebSocket 测试用户"
                        }
                        """.formatted(username))
                .retrieve()
                .toBodilessEntity();

        String response = client.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "123456"
                        }
                        """.formatted(username))
                .retrieve()
                .body(String.class);
        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("token").asText();
    }
}
