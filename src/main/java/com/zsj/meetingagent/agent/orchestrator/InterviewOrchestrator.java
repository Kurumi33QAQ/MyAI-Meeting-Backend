package com.zsj.meetingagent.agent.orchestrator;

import com.zsj.meetingagent.agent.model.InterviewAgentOutput;
import com.zsj.meetingagent.agent.model.InterviewOrchestrationContext;
import com.zsj.meetingagent.agent.model.InterviewOrchestrationResult;
import com.zsj.meetingagent.agent.vo.AgentStepResponse;

import java.util.List;

/**
 * 多 Agent 面试编排器接口。
 * 面试模块只依赖这个入口，不直接知道每个 Agent 角色的内部实现。
 */
public interface InterviewOrchestrator {

    InterviewOrchestrationResult designQuestions(InterviewOrchestrationContext context);

    InterviewAgentOutput reviewAnswer(String username, String sessionId, String question, String answer, int score, String aiFeedback);

    List<AgentStepResponse> listTraces(String username, String sessionId);
}
