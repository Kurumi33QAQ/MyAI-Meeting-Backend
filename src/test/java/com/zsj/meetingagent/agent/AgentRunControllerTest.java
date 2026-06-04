package com.zsj.meetingagent.agent;

import com.zsj.meetingagent.agent.enums.AgentRunStatus;
import com.zsj.meetingagent.agent.enums.AgentStepType;
import com.zsj.meetingagent.agent.service.AgentRunService;
import com.zsj.meetingagent.agent.vo.AgentRunResponse;
import com.zsj.meetingagent.agent.vo.AgentStepResponse;
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
class AgentRunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgentRunService agentRunService;

    @Test
    void agentRunEndpointsRequireLogin() throws Exception {
        mockMvc.perform(post("/api/agent-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "input": "现在几点"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAndQueryAgentRunReturnSteps() throws Exception {
        String token = registerAndLogin();
        Instant now = Instant.now();
        AgentRunResponse response = new AgentRunResponse(
                "run-1",
                AgentRunStatus.COMPLETED,
                "现在几点",
                null,
                "gpt-4o-mini",
                "当前时间工具已返回结果。",
                null,
                now,
                now,
                List.of(
                        new AgentStepResponse(AgentStepType.THOUGHT, 1, null, "判断用户需要时间信息", now),
                        new AgentStepResponse(AgentStepType.ACTION, 2, "current_time", "调用时间工具", now),
                        new AgentStepResponse(AgentStepType.OBSERVATION, 3, "current_time", "后端服务器当前时间", now),
                        new AgentStepResponse(AgentStepType.FINAL_ANSWER, 4, null, "当前时间工具已返回结果。", now)
                )
        );

        when(agentRunService.run(anyString(), any())).thenReturn(response);
        when(agentRunService.getRun(anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/agent-runs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "input": "现在几点",
                                  "maxSteps": 4
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.runId").value("run-1"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.steps[0].stepType").value("THOUGHT"))
                .andExpect(jsonPath("$.data.steps[1].toolName").value("current_time"))
                .andExpect(jsonPath("$.data.steps[3].stepType").value("FINAL_ANSWER"));

        mockMvc.perform(get("/api/agent-runs/run-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runId").value("run-1"))
                .andExpect(jsonPath("$.data.steps.length()").value(4));
    }

    @Test
    void invalidAgentRunRequestReturnsChineseMessage() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(post("/api/agent-runs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "input": "",
                                  "maxSteps": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0001"))
                .andExpect(jsonPath("$.message", not(blankOrNullString())));
    }

    private String registerAndLogin() throws Exception {
        String username = "agent_controller_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
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
