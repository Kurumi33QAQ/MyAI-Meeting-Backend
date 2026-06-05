package com.zsj.meetingagent.agent.role;

import com.zsj.meetingagent.agent.model.InterviewAgentOutput;
import com.zsj.meetingagent.agent.model.InterviewOrchestrationContext;

import java.util.List;

/**
 * 面试协同 Agent 角色接口。
 * 角色只产出自己的观察和建议，不直接决定最终业务状态，最终决策由编排器汇总。
 */
public interface InterviewAgentRole {

    String roleName();

    InterviewAgentOutput analyze(InterviewOrchestrationContext context, List<InterviewAgentOutput> previousOutputs);
}
