package com.zsj.meetingagent.agent.vo;

import com.zsj.meetingagent.agent.enums.AgentRunStatus;

import java.time.Instant;
import java.util.List;

/**
 * Agent Run 响应。
 * 返回最终答案的同时返回完整步骤轨迹，便于学习 Thought-Action-Observation 的执行过程。
 */
public record AgentRunResponse(
        String runId,
        AgentRunStatus status,
        String input,
        String sessionId,
        String model,
        String finalAnswer,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt,
        List<AgentStepResponse> steps
) {
}
