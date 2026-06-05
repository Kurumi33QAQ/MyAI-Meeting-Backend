package com.zsj.meetingagent.agent.model;

/**
 * 面试 Agent 角色输出。
 * 每个角色只负责一个分析维度，编排器再把多个角色结果合并成最终出题或评分依据。
 */
public record InterviewAgentOutput(
        String roleName,
        String summary,
        String details
) {
}
