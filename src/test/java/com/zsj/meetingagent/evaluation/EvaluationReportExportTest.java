package com.zsj.meetingagent.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Evaluation 正式报告导出测试。
 * 该测试会把默认 21 条测试集跑完，并把报告输出到 reports/evaluation，供 README 和简历引用前人工复核。
 */
@SpringBootTest(properties = {
        "app.ai.mock-enabled=true",
        "app.evaluation.report-dir=reports/evaluation"
})
@AutoConfigureMockMvc
class EvaluationReportExportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void exportDefaultEvaluationReportToReportsDirectory() throws Exception {
        String token = registerAndLogin();

        String response = mockMvc.perform(post("/api/evaluations/runs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "datasetPath": "classpath:evaluation/eval_cases.json",
                                  "datasetName": "default-21-cases"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode data = objectMapper.readTree(response).path("data");
        Path jsonReport = Path.of(data.path("reportJsonPath").asText());
        Path markdownReport = Path.of(data.path("reportMarkdownPath").asText());
        assertThat(Files.exists(jsonReport)).isTrue();
        assertThat(Files.exists(markdownReport)).isTrue();
        assertThat(Files.readString(markdownReport)).contains("Category Breakdown");
    }

    private String registerAndLogin() throws Exception {
        String username = "evaluation_export_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "12345678",
                                  "realName": "评测报告导出用户"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk());

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "12345678"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(loginResponse).path("data").path("token").asText();
    }
}
