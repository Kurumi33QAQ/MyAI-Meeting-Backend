package com.zsj.meetingagent.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.zsj.meetingagent.chat.service.ChatSessionService;
import com.zsj.meetingagent.chat.vo.ChatMessageResponse;
import com.zsj.meetingagent.chat.vo.ChatSessionResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.ai.mock-enabled=true",
        "app.ai.default-model=gpt-4o-mini",
        "spring.ai.openai.chat.options.model=gpt-4o-mini"
})
@AutoConfigureMockMvc
class ChatStreamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatSessionService chatSessionService;

    @Test
    void streamEndpointRequiresLogin() throws Exception {
        mockMvc.perform(post("/api/ai/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "请解释 SSE"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void streamEndpointReturnsSseChunksForLoggedInUser() throws Exception {
        String token = registerAndLogin();
        mockChatSession("stage3-stream-session");

        MvcResult result = mockMvc.perform(post("/api/ai/chat/stream")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {
                                  "message": "请解释 SSE"
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        result.getAsyncResult(5000);
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("\"content\"")))
                .andExpect(content().string(containsString("[DONE]")));
    }

    @Test
    void legacyFrontendAiEndpointsKeepChatPageUsable() throws Exception {
        String token = registerAndLogin();
        mockChatSession("test-session");

        mockMvc.perform(get("/api/xunzhi/v1/ai-properties")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.records[0].modelName").value("gpt-4o-mini"));

        mockMvc.perform(post("/api/xunzhi/v1/ai/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userName": "stage3_user",
                                  "firstMessage": "你好",
                                  "aiId": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId", not(blankOrNullString())));

        MvcResult streamResult = mockMvc.perform(post("/api/xunzhi/v1/ai/sessions/test-session/chat")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("username", "stage3_user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {
                                  "sessionId": "test-session",
                                  "inputMessage": "你好",
                                  "userName": "stage3_user",
                                  "aiId": 1,
                                  "messageSeq": 1
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        streamResult.getAsyncResult(5000);
        mockMvc.perform(asyncDispatch(streamResult))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"content\"")))
                .andExpect(content().string(containsString("[DONE]")));
    }

    private String registerAndLogin() throws Exception {
        String username = "stage3_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "12345678",
                                  "realName": "阶段三测试用户",
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

    private void mockChatSession(String sessionId) {
        ChatSessionResponse session = new ChatSessionResponse(
                sessionId,
                "测试会话",
                "gpt-4o-mini",
                0,
                Instant.now(),
                Instant.now()
        );
        when(chatSessionService.ensureSession(anyString(), nullable(String.class), anyString(), nullable(String.class)))
                .thenReturn(session);
        when(chatSessionService.createSession(anyString(), nullable(String.class), nullable(String.class)))
                .thenReturn(session);
        when(chatSessionService.listSessions(anyString(), anyLong(), anyLong()))
                .thenReturn(List.of(session));
        when(chatSessionService.countSessions(anyString()))
                .thenReturn(1L);
        when(chatSessionService.listMessages(anyString(), anyString()))
                .thenReturn(List.of(new ChatMessageResponse(
                        "message-1",
                        sessionId,
                        "user",
                        "你好",
                        "gpt-4o-mini",
                        1,
                        Instant.now()
                )));
    }
}
