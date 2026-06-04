package com.zsj.meetingagent.agent.service.impl;

import com.zsj.meetingagent.agent.dto.AgentRunRequest;
import com.zsj.meetingagent.agent.entity.AgentRunDocument;
import com.zsj.meetingagent.agent.entity.AgentStepTraceDocument;
import com.zsj.meetingagent.agent.enums.AgentRunStatus;
import com.zsj.meetingagent.agent.enums.AgentStepType;
import com.zsj.meetingagent.agent.repository.AgentRunRepository;
import com.zsj.meetingagent.agent.repository.AgentStepTraceRepository;
import com.zsj.meetingagent.agent.service.AgentRunService;
import com.zsj.meetingagent.agent.tool.AgentTool;
import com.zsj.meetingagent.agent.tool.AgentToolContext;
import com.zsj.meetingagent.agent.tool.AgentToolResult;
import com.zsj.meetingagent.agent.vo.AgentRunResponse;
import com.zsj.meetingagent.agent.vo.AgentStepResponse;
import com.zsj.meetingagent.ai.dto.AiChatRequest;
import com.zsj.meetingagent.ai.service.AiChatService;
import com.zsj.meetingagent.ai.vo.AiChatResponse;
import com.zsj.meetingagent.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 默认 Agent 运行实现。
 * 阶段 5 先用规则选择工具，重点让 Thought-Action-Observation-Final Answer 的后端链路清晰可追踪。
 */
@Service
public class DefaultAgentRunService implements AgentRunService {

    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final String AGENT_SYSTEM_PROMPT = """
            你是一个中文友好的 Java 后端 Agent。
            你必须基于用户问题和工具观察结果回答，不要编造工具没有返回的信息。
            回答要简洁，并说明你使用了哪个工具。
            """;

    private final AgentRunRepository agentRunRepository;
    private final AgentStepTraceRepository stepTraceRepository;
    private final List<AgentTool> tools;
    private final AiChatService aiChatService;

    public DefaultAgentRunService(
            AgentRunRepository agentRunRepository,
            AgentStepTraceRepository stepTraceRepository,
            List<AgentTool> tools,
            AiChatService aiChatService
    ) {
        this.agentRunRepository = agentRunRepository;
        this.stepTraceRepository = stepTraceRepository;
        this.tools = tools;
        this.aiChatService = aiChatService;
    }

    @Override
    public AgentRunResponse run(String username, AgentRunRequest request) {
        String runId = UUID.randomUUID().toString();
        String model = hasText(request.model()) ? request.model() : DEFAULT_MODEL;
        Instant now = Instant.now();

        AgentRunDocument run = new AgentRunDocument();
        run.setRunId(runId);
        run.setUsername(username);
        run.setInput(request.input().trim());
        run.setSessionId(blankToNull(request.sessionId()));
        run.setModel(model);
        run.setStatus(AgentRunStatus.RUNNING);
        run.setCreatedAt(now);
        run.setUpdatedAt(now);
        agentRunRepository.save(run);

        try {
            String thought = buildThought(request);
            saveStep(runId, username, AgentStepType.THOUGHT, 1, null, thought);

            AgentToolContext context = new AgentToolContext(username, request.input().trim(), blankToNull(request.sessionId()), model);
            AgentTool tool = selectTool(context);
            saveStep(runId, username, AgentStepType.ACTION, 2, tool.name(), "调用工具：" + tool.name() + "，原因：" + tool.description());

            AgentToolResult observation = tool.execute(context);
            saveStep(runId, username, AgentStepType.OBSERVATION, 3, observation.toolName(), observation.observation());

            String finalAnswer = generateFinalAnswer(request.input().trim(), observation, model);
            saveStep(runId, username, AgentStepType.FINAL_ANSWER, 4, null, finalAnswer);

            run.setStatus(AgentRunStatus.COMPLETED);
            run.setFinalAnswer(finalAnswer);
            run.setUpdatedAt(Instant.now());
            agentRunRepository.save(run);
            return toResponse(run, listSteps(username, runId));
        } catch (RuntimeException ex) {
            run.setStatus(AgentRunStatus.FAILED);
            run.setErrorMessage(ex.getMessage());
            run.setUpdatedAt(Instant.now());
            agentRunRepository.save(run);
            throw ex;
        }
    }

    @Override
    public AgentRunResponse getRun(String username, String runId) {
        AgentRunDocument run = agentRunRepository.findByRunIdAndUsername(runId, username)
                .orElseThrow(() -> new BusinessException("G0404", "Agent 运行记录不存在或无权访问"));
        return toResponse(run, listSteps(username, runId));
    }

    private String buildThought(AgentRunRequest request) {
        if (hasText(request.sessionId()) || request.input().contains("历史") || request.input().contains("聊天")) {
            return "用户的问题可能需要结合聊天历史，因此准备查询会话消息作为观察结果。";
        }
        if (request.input().contains("时间") || request.input().contains("今天") || request.input().contains("现在")) {
            return "用户的问题和当前时间有关，因此准备调用时间工具。";
        }
        return "用户提出了通用问题，阶段 5 简化版 Agent 会先调用稳定工具获得观察结果，再生成最终回答。";
    }

    private AgentTool selectTool(AgentToolContext context) {
        return tools.stream()
                .sorted(Comparator.comparing(tool -> tool.name().equals("chat_history") ? 0 : 1))
                .filter(tool -> tool.supports(context))
                .findFirst()
                .orElseGet(() -> tools.stream()
                        .filter(tool -> tool.name().equals("current_time"))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException("G0501", "没有可用的 Agent 工具")));
    }

    private String generateFinalAnswer(String input, AgentToolResult observation, String model) {
        String prompt = """
                用户问题：
                %s

                工具名称：
                %s

                工具观察结果：
                %s

                请基于工具观察结果给出最终回答。
                """.formatted(input, observation.toolName(), observation.observation());
        AiChatResponse response = aiChatService.chat(new AiChatRequest(prompt, null, model, AGENT_SYSTEM_PROMPT, null));
        return response.answer();
    }

    private AgentStepTraceDocument saveStep(
            String runId,
            String username,
            AgentStepType stepType,
            int stepOrder,
            String toolName,
            String content
    ) {
        AgentStepTraceDocument step = new AgentStepTraceDocument();
        step.setRunId(runId);
        step.setUsername(username);
        step.setStepType(stepType);
        step.setStepOrder(stepOrder);
        step.setToolName(toolName);
        step.setContent(content);
        step.setCreatedAt(Instant.now());
        return stepTraceRepository.save(step);
    }

    private List<AgentStepResponse> listSteps(String username, String runId) {
        return stepTraceRepository.findByRunIdAndUsernameOrderByStepOrderAsc(runId, username).stream()
                .map(this::toStepResponse)
                .toList();
    }

    private AgentRunResponse toResponse(AgentRunDocument run, List<AgentStepResponse> steps) {
        return new AgentRunResponse(
                run.getRunId(),
                run.getStatus(),
                run.getInput(),
                run.getSessionId(),
                run.getModel(),
                run.getFinalAnswer(),
                run.getErrorMessage(),
                run.getCreatedAt(),
                run.getUpdatedAt(),
                steps
        );
    }

    private AgentStepResponse toStepResponse(AgentStepTraceDocument step) {
        return new AgentStepResponse(
                step.getStepType(),
                step.getStepOrder(),
                step.getToolName(),
                step.getContent(),
                step.getCreatedAt()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
