package com.zsj.meetingagent.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TTS 接口测试。
 * 验证旧前端路径和本项目新路径都能拿到可播放音频任务。
 */
@SpringBootTest(properties = {
        "app.ai.mock-enabled=true",
        "spring.ai.openai.chat.options.model=gpt-4o-mini"
})
@AutoConfigureMockMvc
class TtsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void ttsEndpointsRequireLogin() throws Exception {
        mockMvc.perform(post("/api/xunzhi/v1/xunfei/tts/synthesize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"你好\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void legacyTtsSynthesizeReturnsPlayableAudioUrl() throws Exception {
        String token = registerAndLogin();
        String response = mockMvc.perform(post("/api/xunzhi/v1/xunfei/tts/synthesize")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"你好，我是 AI 面试官\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.audioUrl").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode root = objectMapper.readTree(response);
        String audioUrl = root.path("data").path("audioUrl").asText();

        byte[] audioBytes = mockMvc.perform(get(audioUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/wav"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        assertThat(new String(audioBytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("RIFF");
        assertThat(new String(audioBytes, 8, 4, StandardCharsets.US_ASCII)).isEqualTo("WAVE");
    }

    private String registerAndLogin() throws Exception {
        String username = "media_user_" + System.nanoTime();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "123456",
                                  "realName": "媒体测试用户"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk());

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "123456"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(loginResponse).path("data").path("token").asText();
    }
}
