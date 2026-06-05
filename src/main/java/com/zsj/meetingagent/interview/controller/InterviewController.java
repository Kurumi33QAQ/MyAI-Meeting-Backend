package com.zsj.meetingagent.interview.controller;

import com.zsj.meetingagent.auth.security.LoginUserContext;
import com.zsj.meetingagent.common.result.ApiResponse;
import com.zsj.meetingagent.interview.dto.CreateInterviewSessionRequest;
import com.zsj.meetingagent.interview.dto.SubmitInterviewAnswerRequest;
import com.zsj.meetingagent.interview.service.InterviewService;
import com.zsj.meetingagent.interview.vo.InterviewAnswerResponse;
import com.zsj.meetingagent.interview.vo.InterviewReportResponse;
import com.zsj.meetingagent.interview.vo.InterviewRuntimeStateResponse;
import com.zsj.meetingagent.interview.vo.InterviewSessionResponse;
import com.zsj.meetingagent.agent.vo.AgentStepResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模拟面试接口控制器。
 * 提供创建会话、生成题目、提交回答和查看报告的新风格接口。
 */
@RestController
@RequestMapping
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping("/api/interview-sessions")
    public ApiResponse<InterviewSessionResponse> createSession(
            @Valid @RequestBody CreateInterviewSessionRequest request
    ) {
        return ApiResponse.success(interviewService.createSession(LoginUserContext.currentUsername(), request));
    }

    @PostMapping("/api/interview-sessions/{sessionId}/questions")
    public ApiResponse<InterviewSessionResponse> generateQuestions(@PathVariable String sessionId) {
        return ApiResponse.success(interviewService.generateQuestions(LoginUserContext.currentUsername(), sessionId));
    }

    @PostMapping("/api/interview-sessions/{sessionId}/answers")
    public ApiResponse<InterviewAnswerResponse> submitAnswer(
            @PathVariable String sessionId,
            @Valid @RequestBody SubmitInterviewAnswerRequest request
    ) {
        return ApiResponse.success(interviewService.submitAnswer(LoginUserContext.currentUsername(), sessionId, request));
    }

    @GetMapping("/api/interview-sessions/{sessionId}")
    public ApiResponse<InterviewSessionResponse> getSession(@PathVariable String sessionId) {
        return ApiResponse.success(interviewService.getSession(LoginUserContext.currentUsername(), sessionId));
    }

    @GetMapping("/api/interviews/{sessionId}/report")
    public ApiResponse<InterviewReportResponse> getReport(@PathVariable String sessionId) {
        return ApiResponse.success(interviewService.getReport(LoginUserContext.currentUsername(), sessionId));
    }

    @GetMapping("/api/interview-sessions/{sessionId}/agent-traces")
    public ApiResponse<List<AgentStepResponse>> listAgentTraces(@PathVariable String sessionId) {
        return ApiResponse.success(interviewService.listAgentTraces(LoginUserContext.currentUsername(), sessionId));
    }

    @GetMapping("/api/interview-sessions/{sessionId}/runtime-state")
    public ApiResponse<InterviewRuntimeStateResponse> getRuntimeState(@PathVariable String sessionId) {
        return ApiResponse.success(interviewService.getRuntimeState(LoginUserContext.currentUsername(), sessionId));
    }

    @PostMapping("/api/interview-sessions/{sessionId}/recover")
    public ApiResponse<InterviewRuntimeStateResponse> recoverRuntimeState(@PathVariable String sessionId) {
        return ApiResponse.success(interviewService.recoverRuntimeState(LoginUserContext.currentUsername(), sessionId));
    }
}
