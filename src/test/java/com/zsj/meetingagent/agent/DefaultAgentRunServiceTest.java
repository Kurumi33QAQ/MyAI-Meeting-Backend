package com.zsj.meetingagent.agent;

import com.zsj.meetingagent.agent.dto.AgentRunRequest;
import com.zsj.meetingagent.agent.entity.AgentRunDocument;
import com.zsj.meetingagent.agent.entity.AgentStepTraceDocument;
import com.zsj.meetingagent.agent.enums.AgentRunStatus;
import com.zsj.meetingagent.agent.enums.AgentStepType;
import com.zsj.meetingagent.agent.repository.AgentRunRepository;
import com.zsj.meetingagent.agent.repository.AgentStepTraceRepository;
import com.zsj.meetingagent.agent.service.impl.DefaultAgentRunService;
import com.zsj.meetingagent.agent.tool.AgentTool;
import com.zsj.meetingagent.agent.tool.AgentToolContext;
import com.zsj.meetingagent.agent.tool.AgentToolResult;
import com.zsj.meetingagent.ai.config.AiModelProperties;
import com.zsj.meetingagent.ai.service.AiChatService;
import com.zsj.meetingagent.ai.vo.AiChatResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAgentRunServiceTest {

    @Mock
    private AgentRunRepository agentRunRepository;

    @Mock
    private AgentStepTraceRepository stepTraceRepository;

    @Mock
    private AiChatService aiChatService;

    @Test
    void runAgentSavesThoughtActionObservationAndFinalAnswer() {
        ArgumentCaptor<AgentRunDocument> runCaptor = ArgumentCaptor.forClass(AgentRunDocument.class);
        ArgumentCaptor<AgentStepTraceDocument> stepCaptor = ArgumentCaptor.forClass(AgentStepTraceDocument.class);
        AgentTool fakeTool = new AgentTool() {
            @Override
            public String name() {
                return "current_time";
            }

            @Override
            public String description() {
                return "测试工具";
            }

            @Override
            public boolean supports(AgentToolContext context) {
                return true;
            }

            @Override
            public AgentToolResult execute(AgentToolContext context) {
                return new AgentToolResult(name(), "观察结果");
            }
        };

        when(agentRunRepository.save(runCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(stepTraceRepository.save(stepCaptor.capture()))
                .thenAnswer(invocation -> {
                    AgentStepTraceDocument step = invocation.getArgument(0);
                    step.setId("step-" + step.getStepOrder());
                    return step;
                });
        when(stepTraceRepository.findByRunIdAndUsernameOrderByStepOrderAsc(anyString(), anyString()))
                .thenAnswer(invocation -> stepCaptor.getAllValues());
        when(aiChatService.chat(any()))
                .thenReturn(new AiChatResponse("最终回答", "gpt-4o-mini", "mock", 12, true));

        DefaultAgentRunService service = new DefaultAgentRunService(
                agentRunRepository,
                stepTraceRepository,
                List.of(fakeTool),
                aiChatService,
                testAiModelProperties()
        );

        var response = service.run("alice", new AgentRunRequest("现在几点", null, "gpt-4o-mini", 4));

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.finalAnswer()).isEqualTo("最终回答");
        assertThat(response.steps()).hasSize(4);
        assertThat(response.steps().get(0).stepType()).isEqualTo(AgentStepType.THOUGHT);
        assertThat(response.steps().get(1).stepType()).isEqualTo(AgentStepType.ACTION);
        assertThat(response.steps().get(2).stepType()).isEqualTo(AgentStepType.OBSERVATION);
        assertThat(response.steps().get(3).stepType()).isEqualTo(AgentStepType.FINAL_ANSWER);
        assertThat(runCaptor.getAllValues().getLast().getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(runCaptor.getAllValues().getLast().getUpdatedAt()).isAfterOrEqualTo(Instant.EPOCH);
    }

    private AiModelProperties testAiModelProperties() {
        AiModelProperties properties = new AiModelProperties();
        properties.setDefaultModel("gpt-4o-mini");
        return properties;
    }
}
