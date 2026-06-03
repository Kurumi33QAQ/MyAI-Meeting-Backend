package com.zsj.meetingagent.chat;

import com.zsj.meetingagent.chat.service.ChatSessionService;
import com.zsj.meetingagent.chat.vo.ChatMessageResponse;
import com.zsj.meetingagent.chat.vo.ChatSessionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.ai.mock-enabled=true",
        "app.ai.default-model=gpt-4o-mini",
        "spring.ai.openai.chat.options.model=gpt-4o-mini"
})
@AutoConfigureMockMvc
class ChatSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatSessionService chatSessionService;

    @Test
    void sessionEndpointsRequireLogin() throws Exception {
        mockMvc.perform(get("/api/ai/sessions"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sessionEndpointsReturnConversationAndMessages() throws Exception {
        String token = registerAndLogin();
        Instant now = Instant.now();
        ChatSessionResponse session = new ChatSessionResponse("session-1", "你好", "gpt-4o-mini", 2, now, now);
        ChatMessageResponse message = new ChatMessageResponse("message-1", "session-1", "user", "你好", "gpt-4o-mini", 1, now);

        when(chatSessionService.createSession(anyString(), nullable(String.class), nullable(String.class)))
                .thenReturn(session);
        when(chatSessionService.listSessions(anyString(), anyLong(), anyLong()))
                .thenReturn(List.of(session));
        when(chatSessionService.listMessages(anyString(), anyString()))
                .thenReturn(List.of(message));

        mockMvc.perform(post("/api/ai/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstMessage": "你好",
                                  "model": "gpt-4o-mini"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value("session-1"))
                .andExpect(jsonPath("$.data.title").value("你好"));

        mockMvc.perform(get("/api/ai/sessions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sessionId").value("session-1"));

        mockMvc.perform(get("/api/ai/sessions/session-1/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("你好"))
                .andExpect(jsonPath("$.data[0].role").value("user"));
    }

    private String registerAndLogin() throws Exception {
        String username = "stage4_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "12345678",
                                  "realName": "阶段四测试用户",
                                  "mail": "%s@example.com"
                                }
                                """.formatted(username, username)))
                .andExpect(status().isOk());

        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "12345678"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", not(blankOrNullString())))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }
}
