package com.zsj.meetingagent.resume;

import com.zsj.meetingagent.resume.service.ResumeService;
import com.zsj.meetingagent.resume.vo.ResumeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 简历接口测试。
 * 验证文本简历录入和查询接口需要登录，并且能返回统一响应结构。
 */
@SpringBootTest(properties = {
        "app.ai.mock-enabled=true",
        "app.ai.default-model=gpt-4o-mini",
        "spring.ai.openai.chat.options.model=gpt-4o-mini"
})
@AutoConfigureMockMvc
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResumeService resumeService;

    @Test
    void resumeTextEndpointRequiresLogin() throws Exception {
        mockMvc.perform(post("/api/resumes/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fileName": "resume.txt",
                                  "content": "Java 后端简历"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadTextAndGetResume() throws Exception {
        String token = registerAndLogin();
        Instant now = Instant.now();
        ResumeResponse response = new ResumeResponse(
                "resume-1",
                "resume.txt",
                "text/plain",
                128L,
                "TEXT",
                "Java Spring Boot 项目经历",
                now,
                now
        );
        when(resumeService.uploadText(anyString(), any())).thenReturn(response);
        when(resumeService.getResume(anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/resumes/text")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fileName": "resume.txt",
                                  "content": "Java Spring Boot 项目经历"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.resumeId").value("resume-1"))
                .andExpect(jsonPath("$.data.summary").value("Java Spring Boot 项目经历"));

        mockMvc.perform(get("/api/resumes/resume-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("resume.txt"));
    }

    private String registerAndLogin() throws Exception {
        String username = "resume_controller_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "12345678"
                                }
                                """.formatted(username)))
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
