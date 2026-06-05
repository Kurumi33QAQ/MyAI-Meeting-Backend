package com.zsj.meetingagent.limit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.ai.mock-enabled=true",
        "app.ai-guard.enabled=false"
})
@AutoConfigureMockMvc
class AiGuardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void guardStatsRequiresLogin() throws Exception {
        mockMvc.perform(get("/api/ai/guard/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loggedInUserCanReadGuardStats() throws Exception {
        String token = registerAndLogin("stage85_guard_user");

        mockMvc.perform(get("/api/ai/guard/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.totalCalls").exists())
                .andExpect(jsonPath("$.data.ownerCalls").exists())
                .andExpect(jsonPath("$.data.replayedCalls").exists());
    }

    private String registerAndLogin(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "12345678",
                                  "realName": "阶段八点五测试用户",
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
