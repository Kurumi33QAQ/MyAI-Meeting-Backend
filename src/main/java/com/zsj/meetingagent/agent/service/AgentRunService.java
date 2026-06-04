package com.zsj.meetingagent.agent.service;

import com.zsj.meetingagent.agent.dto.AgentRunRequest;
import com.zsj.meetingagent.agent.vo.AgentRunResponse;

/**
 * Agent 运行服务接口。
 * Controller 只负责接收请求，具体 Thought-Action-Observation 执行流程放在 Service 中。
 */
public interface AgentRunService {

    AgentRunResponse run(String username, AgentRunRequest request);

    AgentRunResponse getRun(String username, String runId);
}
