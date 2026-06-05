package com.zsj.meetingagent.evaluation;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Evaluation 自动评测接口测试。
 * 覆盖“跑评测 -> 生成指标 -> 写报告路径 -> 查询历史评测”的完整后端闭环。
 */
@SpringBootTest(properties = {
        "app.ai.mock-enabled=true",
        "app.ai.default-model=gpt-4o-mini",
        "spring.ai.openai.chat.options.model=gpt-4o-mini",
        "app.evaluation.report-dir=target/test-reports/evaluation"
})
@AutoConfigureMockMvc
class EvaluationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void runEvaluationAndGetReport() throws Exception {
        String token = registerAndLogin();

        String response = mockMvc.perform(post("/api/evaluations/runs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "datasetPath": "classpath:evaluation/eval_cases.json",
                                  "maxCases": 3,
                                  "writeReport": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.runId", not(blankOrNullString())))
                .andExpect(jsonPath("$.data.totalCases").value(3))
                .andExpect(jsonPath("$.data.summaries.length()").value(4))
                .andExpect(jsonPath("$.data.caseResults.length()").value(12))
                .andExpect(jsonPath("$.data.reportJsonPath", not(blankOrNullString())))
                .andExpect(jsonPath("$.data.reportMarkdownPath", not(blankOrNullString())))
                .andExpect(jsonPath("$.data.summaries[0].totalCases").value(3))
                .andExpect(jsonPath("$.data.summaries[2].citationCorrectCount", greaterThan(0)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String runId = response.replaceAll(".*\\\"runId\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(get("/api/evaluations/runs/" + runId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.runId").value(runId))
                .andExpect(jsonPath("$.data.caseResults.length()").value(12));
    }

    @Test
    void evaluationEndpointsRequireLogin() throws Exception {
        mockMvc.perform(post("/api/evaluations/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    private String registerAndLogin() throws Exception {
        String username = "evaluation_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
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
