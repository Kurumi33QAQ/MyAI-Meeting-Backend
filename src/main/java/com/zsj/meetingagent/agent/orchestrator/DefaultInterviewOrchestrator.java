package com.zsj.meetingagent.agent.orchestrator;

import com.zsj.meetingagent.agent.entity.AgentRunDocument;
import com.zsj.meetingagent.agent.entity.AgentStepTraceDocument;
import com.zsj.meetingagent.agent.enums.AgentRunStatus;
import com.zsj.meetingagent.agent.enums.AgentStepType;
import com.zsj.meetingagent.agent.model.InterviewAgentOutput;
import com.zsj.meetingagent.agent.model.InterviewOrchestrationContext;
import com.zsj.meetingagent.agent.model.InterviewOrchestrationResult;
import com.zsj.meetingagent.agent.repository.AgentRunRepository;
import com.zsj.meetingagent.agent.repository.AgentStepTraceRepository;
import com.zsj.meetingagent.agent.role.AnswerReviewAgent;
import com.zsj.meetingagent.agent.role.InterviewAgentRole;
import com.zsj.meetingagent.agent.role.InterviewSummaryAgent;
import com.zsj.meetingagent.agent.role.JobContextAgent;
import com.zsj.meetingagent.agent.role.QuestionDesignAgent;
import com.zsj.meetingagent.agent.role.ResumeAnalysisAgent;
import com.zsj.meetingagent.agent.vo.AgentStepResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 默认多 Agent 面试编排实现。
 * 当前采用顺序编排，先保证角色边界、输入输出和轨迹可追踪，后续再升级并行协商、投票和稳定性治理。
 */
@Service
public class DefaultInterviewOrchestrator implements InterviewOrchestrator {

    private static final String ORCHESTRATOR_MODEL = "multi-agent-rule-orchestrator";

    private final AgentRunRepository agentRunRepository;
    private final AgentStepTraceRepository stepTraceRepository;
    private final ResumeAnalysisAgent resumeAnalysisAgent;
    private final JobContextAgent jobContextAgent;
    private final QuestionDesignAgent questionDesignAgent;
    private final AnswerReviewAgent answerReviewAgent;
    private final InterviewSummaryAgent interviewSummaryAgent;

    public DefaultInterviewOrchestrator(
            AgentRunRepository agentRunRepository,
            AgentStepTraceRepository stepTraceRepository,
            ResumeAnalysisAgent resumeAnalysisAgent,
            JobContextAgent jobContextAgent,
            QuestionDesignAgent questionDesignAgent,
            AnswerReviewAgent answerReviewAgent,
            InterviewSummaryAgent interviewSummaryAgent
    ) {
        this.agentRunRepository = agentRunRepository;
        this.stepTraceRepository = stepTraceRepository;
        this.resumeAnalysisAgent = resumeAnalysisAgent;
        this.jobContextAgent = jobContextAgent;
        this.questionDesignAgent = questionDesignAgent;
        this.answerReviewAgent = answerReviewAgent;
        this.interviewSummaryAgent = interviewSummaryAgent;
    }

    @Override
    public InterviewOrchestrationResult designQuestions(InterviewOrchestrationContext context) {
        String runId = createRun(context.username(), context.sessionId(), "MULTI_AGENT_QUESTION_DESIGN");
        int stepOrder = 1;
        saveStep(runId, context.username(), AgentStepType.THOUGHT, stepOrder++, "InterviewOrchestrator",
                "开始多 Agent 出题编排：先分析简历，再分析岗位/JD，最后设计题目并汇总结论。");
        List<InterviewAgentOutput> outputs = new ArrayList<>();
        stepOrder = executeRole(runId, context, outputs, resumeAnalysisAgent, stepOrder);
        stepOrder = executeRole(runId, context, outputs, jobContextAgent, stepOrder);
        stepOrder = executeRole(runId, context, outputs, questionDesignAgent, stepOrder);
        stepOrder = executeRole(runId, context, outputs, interviewSummaryAgent, stepOrder);
        var questions = questionDesignAgent.designQuestions(context);
        String finalSummary = "多 Agent 出题完成，生成题目数量：" + questions.size() + "，参与角色：" + roleNames(outputs);
        saveStep(runId, context.username(), AgentStepType.FINAL_ANSWER, stepOrder, "InterviewOrchestrator", finalSummary);
        completeRun(runId, context.username(), finalSummary);
        return new InterviewOrchestrationResult(runId, List.copyOf(outputs), questions, finalSummary);
    }

    @Override
    public InterviewAgentOutput reviewAnswer(String username, String sessionId, String question, String answer, int score, String aiFeedback) {
        String runId = createRun(username, sessionId, "MULTI_AGENT_ANSWER_REVIEW");
        saveStep(runId, username, AgentStepType.THOUGHT, 1, "InterviewOrchestrator",
                "开始回答评估编排：结合规则分数、AI 建议和后续 LiteFlow 追问裁决所需信息。");
        saveStep(runId, username, AgentStepType.ACTION, 2, answerReviewAgent.roleName(), "调用回答评估 Agent。");
        InterviewAgentOutput output = answerReviewAgent.review(question, answer, score, aiFeedback);
        saveStep(runId, username, AgentStepType.OBSERVATION, 3, answerReviewAgent.roleName(), output.summary() + "\n" + output.details());
        saveStep(runId, username, AgentStepType.FINAL_ANSWER, 4, "InterviewOrchestrator", output.summary());
        completeRun(runId, username, output.summary());
        return output;
    }

    @Override
    public List<AgentStepResponse> listTraces(String username, String sessionId) {
        return agentRunRepository.findBySessionIdAndUsernameOrderByCreatedAtDesc(sessionId, username)
                .stream()
                .flatMap(run -> stepTraceRepository.findByRunIdAndUsernameOrderByStepOrderAsc(run.getRunId(), username).stream())
                .sorted(Comparator.comparing(AgentStepTraceDocument::getCreatedAt))
                .map(step -> new AgentStepResponse(
                        step.getStepType(),
                        step.getStepOrder(),
                        step.getToolName(),
                        step.getContent(),
                        step.getCreatedAt()
                ))
                .toList();
    }

    private int executeRole(
            String runId,
            InterviewOrchestrationContext context,
            List<InterviewAgentOutput> outputs,
            InterviewAgentRole role,
            int stepOrder
    ) {
        /*
         * 每个角色都记录 ACTION 和 OBSERVATION。
         * 面试官追问时可以讲：这里不是黑盒一次性出题，而是把多个分析角色的中间结论持久化了。
         */
        saveStep(runId, context.username(), AgentStepType.ACTION, stepOrder++, role.roleName(), "调用 " + role.roleName());
        InterviewAgentOutput output = role.analyze(context, outputs);
        outputs.add(output);
        saveStep(runId, context.username(), AgentStepType.OBSERVATION, stepOrder++, role.roleName(), output.summary() + "\n" + output.details());
        return stepOrder;
    }

    private String createRun(String username, String sessionId, String input) {
        Instant now = Instant.now();
        String runId = UUID.randomUUID().toString();
        AgentRunDocument run = new AgentRunDocument();
        run.setRunId(runId);
        run.setUsername(username);
        run.setSessionId(sessionId);
        run.setInput(input);
        run.setModel(ORCHESTRATOR_MODEL);
        run.setStatus(AgentRunStatus.RUNNING);
        run.setCreatedAt(now);
        run.setUpdatedAt(now);
        agentRunRepository.save(run);
        return runId;
    }

    private void completeRun(String runId, String username, String finalAnswer) {
        agentRunRepository.findByRunIdAndUsername(runId, username).ifPresent(run -> {
            run.setStatus(AgentRunStatus.COMPLETED);
            run.setFinalAnswer(finalAnswer);
            run.setUpdatedAt(Instant.now());
            agentRunRepository.save(run);
        });
    }

    private void saveStep(String runId, String username, AgentStepType stepType, int stepOrder, String toolName, String content) {
        AgentStepTraceDocument step = new AgentStepTraceDocument();
        step.setRunId(runId);
        step.setUsername(username);
        step.setStepType(stepType);
        step.setStepOrder(stepOrder);
        step.setToolName(toolName);
        step.setContent(content);
        step.setCreatedAt(Instant.now());
        stepTraceRepository.save(step);
    }

    private String roleNames(List<InterviewAgentOutput> outputs) {
        return outputs.stream()
                .map(InterviewAgentOutput::roleName)
                .reduce((left, right) -> left + "、" + right)
                .orElse("无");
    }
}
