package com.zsj.meetingagent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RAG 证据检索接口测试。
 * 覆盖“上传简历 -> 创建面试会话入库 chunk -> 检索 evidence”的核心闭环。
 */
@SpringBootTest(properties = {
        "app.ai.mock-enabled=true",
        "app.ai.default-model=gpt-4o-mini",
        "spring.ai.openai.chat.options.model=gpt-4o-mini"
})
@AutoConfigureMockMvc
class RetrievalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void retrieveEvidenceAfterInterviewSessionCreated() throws Exception {
        String token = registerAndLogin();
        String resumeId = uploadResumeText(token);

        mockMvc.perform(post("/api/interview-sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resumeId": "%s",
                                  "jobTitle": "Java 后端开发工程师",
                                  "companyName": "示例科技",
                                  "jobDescription": "负责 Spring Boot 微服务接口开发，要求熟悉 MySQL、Redis、MongoDB 和接口性能优化。",
                                  "questionCount": 2
                                }
                                """.formatted(resumeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(post("/api/retrieval/evidence")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "Spring Boot MySQL 项目经历 岗位职责",
                                  "documentTypes": ["RESUME", "JOB_DESCRIPTION"],
                                  "topK": 20,
                                  "finalK": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.selectedCount", greaterThan(0)))
                .andExpect(jsonPath("$.data.evidenceList[0].evidenceId", not(blankOrNullString())))
                .andExpect(jsonPath("$.data.evidenceList[0].sectionName", not(blankOrNullString())));
    }

    private String registerAndLogin() throws Exception {
        String username = "rag_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
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

    private String uploadResumeText(String token) throws Exception {
        String response = mockMvc.perform(post("/api/resumes/text")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fileName": "rag-resume.txt",
                                  "content": "基本信息：Java 后端学习者。\\n技能栈：Java、Spring Boot、MySQL、Redis、MongoDB、SSE。\\n项目经历：MyAI-Meeting Backend 使用 Spring Boot 实现 AI 模拟面试，使用 MySQL 保存用户和面试记录，使用 MongoDB 保存 AI 会话快照。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumeId", not(blankOrNullString())))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return response.replaceAll(".*\\\"resumeId\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }
}
