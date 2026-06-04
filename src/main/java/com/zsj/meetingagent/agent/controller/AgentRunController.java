package com.zsj.meetingagent.agent.controller;

import com.zsj.meetingagent.auth.security.LoginUserContext;
import com.zsj.meetingagent.agent.dto.AgentRunRequest;
import com.zsj.meetingagent.agent.service.AgentRunService;
import com.zsj.meetingagent.agent.vo.AgentRunResponse;
import com.zsj.meetingagent.common.result.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent Run 控制器。
 * 提供新风格 `/api/agent-runs` 接口，用于创建和查询一次 Agent Thought-Action-Observation 执行。
 */
@RestController
@RequestMapping("/api/agent-runs")
public class AgentRunController {

    private final AgentRunService agentRunService;

    public AgentRunController(AgentRunService agentRunService) {
        this.agentRunService = agentRunService;
    }

    @PostMapping
    public ApiResponse<AgentRunResponse> run(@Valid @RequestBody AgentRunRequest request) {
        return ApiResponse.success(agentRunService.run(LoginUserContext.currentUsername(), request));
    }

    @GetMapping("/{runId}")
    public ApiResponse<AgentRunResponse> getRun(@PathVariable String runId) {
        return ApiResponse.success(agentRunService.getRun(LoginUserContext.currentUsername(), runId));
    }
}
