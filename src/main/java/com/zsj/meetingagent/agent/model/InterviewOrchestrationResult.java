package com.zsj.meetingagent.agent.model;

import java.util.List;

/**
 * 多 Agent 编排结果。
 * runId 用于关联 MongoDB 中的 agent_run 和 agent_step_trace，questions 则交给面试模块落库。
 */
public record InterviewOrchestrationResult(
        String runId,
        List<InterviewAgentOutput> agentOutputs,
        List<InterviewAgentQuestion> questions,
        String traceSummary
) {
}
