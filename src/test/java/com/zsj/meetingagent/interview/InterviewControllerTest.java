package com.zsj.meetingagent.interview;

import com.zsj.meetingagent.interview.enums.InterviewSessionStatus;
import com.zsj.meetingagent.agent.enums.AgentStepType;
import com.zsj.meetingagent.agent.vo.AgentStepResponse;
import com.zsj.meetingagent.interview.service.InterviewService;
import com.zsj.meetingagent.interview.vo.InterviewAnswerResponse;
import com.zsj.meetingagent.interview.vo.InterviewQuestionResponse;
import com.zsj.meetingagent.interview.vo.InterviewReportResponse;
import com.zsj.meetingagent.interview.vo.InterviewRuntimeStateResponse;
import com.zsj.meetingagent.interview.vo.InterviewSessionResponse;
import com.zsj.meetingagent.interview.runtime.InterviewRuntimeRestoreSource;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
class InterviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InterviewService interviewService;

    @Test
    void interviewEndpointsRequireLogin() throws Exception {
        mockMvc.perform(post("/api/interview-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resumeId": "resume-1",
                                  "jobTitle": "Java 后端"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createGenerateAnswerAndReportEndpoints() throws Exception {
        String token = registerAndLogin();
        Instant now = Instant.now();
        InterviewQuestionResponse question = new InterviewQuestionResponse(
                "question-1",
                1,
                "请介绍一个 Java 后端项目",
                "应包含背景、职责、方案和结果。",
                "项目理解、技术细节、表达结构",
                "追问技术细节",
                List.of("evidence-1"),
                "项目经历：Spring Boot 接口开发",
                "agent-run-1",
                null,
                null,
                null,
                null,
                null,
                now,
                null
        );
        InterviewSessionResponse session = new InterviewSessionResponse(
                "session-1",
                "resume-1",
                "Java 后端开发",
                "示例公司",
                "熟悉 Spring Boot",
                InterviewSessionStatus.QUESTION_GENERATED,
                1,
                0,
                null,
                null,
                now,
                now,
                List.of(question)
        );
        when(interviewService.createSession(anyString(), any())).thenReturn(session);
        when(interviewService.generateQuestions(anyString(), anyString())).thenReturn(session);
        when(interviewService.submitAnswer(anyString(), anyString(), any()))
                .thenReturn(new InterviewAnswerResponse("session-1", "question-1", 90, "回答较完整", "请补充性能指标", "缺失考点判断节点[命中]", InterviewSessionStatus.COMPLETED, 1, 1));
        when(interviewService.getReport(anyString(), anyString()))
                .thenReturn(new InterviewReportResponse("session-1", InterviewSessionStatus.COMPLETED, 90, 1, 1, "整体表现较好", List.of(question)));
        when(interviewService.listAgentTraces(anyString(), anyString()))
                .thenReturn(List.of(new AgentStepResponse(AgentStepType.OBSERVATION, 1, "简历分析 Agent", "发现 Java 项目经历", now)));
        when(interviewService.getRuntimeState(anyString(), anyString()))
                .thenReturn(new InterviewRuntimeStateResponse("session-1", InterviewSessionStatus.ANSWERING, 1, 0, 1, null, 2, InterviewRuntimeRestoreSource.HOT_REDIS, now));
        when(interviewService.recoverRuntimeState(anyString(), anyString()))
                .thenReturn(new InterviewRuntimeStateResponse("session-1", InterviewSessionStatus.ANSWERING, 1, 0, 1, null, 2, InterviewRuntimeRestoreSource.COLD_MONGO, now));

        mockMvc.perform(post("/api/interview-sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resumeId": "resume-1",
                                  "jobTitle": "Java 后端开发",
                                  "companyName": "示例公司",
                                  "jobDescription": "熟悉 Spring Boot",
                                  "questionCount": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.sessionId").value("session-1"));

        mockMvc.perform(post("/api/interview-sessions/session-1/questions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions[0].questionId").value("question-1"));

        mockMvc.perform(post("/api/interview-sessions/session-1/answers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": "question-1",
                                  "answer": "我负责 Spring Boot 接口开发和 MySQL 优化。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(90));

        mockMvc.perform(get("/api/interviews/session-1/report")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalScore").value(90));

        mockMvc.perform(get("/api/interview-sessions/session-1/agent-traces")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].toolName").value("简历分析 Agent"));

        mockMvc.perform(get("/api/interview-sessions/session-1/runtime-state")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restoreSource").value("HOT_REDIS"));

        mockMvc.perform(post("/api/interview-sessions/session-1/recover")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restoreSource").value("COLD_MONGO"));
    }

    @Test
    void invalidCreateInterviewSessionReturnsChineseMessage() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(post("/api/interview-sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resumeId": "",
                                  "jobTitle": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0001"))
                .andExpect(jsonPath("$.message", not(blankOrNullString())));
    }

    private String registerAndLogin() throws Exception {
        String username = "interview_controller_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
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
