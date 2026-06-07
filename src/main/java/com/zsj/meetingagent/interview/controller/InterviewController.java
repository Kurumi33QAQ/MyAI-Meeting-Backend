package com.zsj.meetingagent.interview.controller;

import com.zsj.meetingagent.auth.security.LoginUserContext;
import com.zsj.meetingagent.common.result.ApiResponse;
import com.zsj.meetingagent.common.vo.PageResponse;
import com.zsj.meetingagent.interview.dto.CreateInterviewSessionRequest;
import com.zsj.meetingagent.interview.dto.SubmitInterviewAnswerRequest;
import com.zsj.meetingagent.interview.service.InterviewService;
import com.zsj.meetingagent.interview.vo.InterviewAnswerResponse;
import com.zsj.meetingagent.interview.vo.InterviewConversationResponse;
import com.zsj.meetingagent.interview.vo.InterviewReportResponse;
import com.zsj.meetingagent.interview.vo.InterviewRecordResponse;
import com.zsj.meetingagent.interview.vo.InterviewRuntimeStateResponse;
import com.zsj.meetingagent.interview.vo.InterviewSessionResponse;
import com.zsj.meetingagent.agent.vo.AgentStepResponse;
import com.zsj.meetingagent.resume.service.ResumeService;
import com.zsj.meetingagent.resume.vo.ResumePreviewResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模拟面试接口控制器。
 * 提供创建会话、生成题目、提交回答和查看报告的新风格接口。
 */
@RestController
@RequestMapping
public class InterviewController {

    private final InterviewService interviewService;
    private final ResumeService resumeService;

    public InterviewController(InterviewService interviewService, ResumeService resumeService) {
        this.interviewService = interviewService;
        this.resumeService = resumeService;
    }

    @PostMapping("/api/interview-sessions")
    public ApiResponse<InterviewSessionResponse> createSession(
            @Valid @RequestBody CreateInterviewSessionRequest request
    ) {
        return ApiResponse.success(interviewService.createSession(LoginUserContext.currentUsername(), request));
    }

    @GetMapping("/api/interview-sessions")
    public ApiResponse<PageResponse<InterviewConversationResponse>> pageInterviewConversations(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(interviewService.pageInterviewConversations(
                LoginUserContext.currentUsername(),
                current,
                size,
                status,
                keyword
        ));
    }

    @GetMapping("/api/interviews")
    public ApiResponse<PageResponse<InterviewRecordResponse>> pageInterviewRecords(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(required = false) Integer maxScore,
            @RequestParam(required = false) String interviewDirection
    ) {
        return ApiResponse.success(interviewService.pageInterviewRecords(
                LoginUserContext.currentUsername(),
                pageNum,
                pageSize,
                sessionId,
                minScore,
                maxScore,
                interviewDirection
        ));
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

    @GetMapping("/api/interview-sessions/{sessionId}/restore")
    public ApiResponse<Map<String, Object>> restoreSession(@PathVariable String sessionId) {
        InterviewSessionResponse session = interviewService.getSession(LoginUserContext.currentUsername(), sessionId);
        return ApiResponse.success(toRestorePayload(session));
    }

    @GetMapping("/api/interview-sessions/{sessionId}/resume/preview")
    public ResponseEntity<byte[]> previewResume(@PathVariable String sessionId) {
        InterviewSessionResponse session = interviewService.getSession(LoginUserContext.currentUsername(), sessionId);
        ResumePreviewResponse preview = resumeService.getResumePreview(LoginUserContext.currentUsername(), session.resumeId());
        return buildPdfPreviewResponse(preview);
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

    private Map<String, Object> toRestorePayload(InterviewSessionResponse session) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", session.sessionId());
        payload.put("status", session.status());
        payload.put("canResume", !session.status().name().equals("COMPLETED"));
        payload.put("resumeFileUrl", session.resumeId());
        payload.put("resumeScore", 80);
        payload.put("interviewType", session.jobTitle());
        Map<String, String> suggestions = new LinkedHashMap<>();
        session.questions().forEach(question -> suggestions.put(
                String.valueOf(question.questionOrder()),
                question.evaluationPoints()
        ));
        payload.put("suggestions", suggestions);
        return payload;
    }

    private ResponseEntity<byte[]> buildPdfPreviewResponse(ResumePreviewResponse preview) {
        String safeFileName = preview.fileName().replace("\"", "");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeFileName + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(preview.bytes().length))
                .contentType(MediaType.parseMediaType(preview.contentType()))
                .body(preview.bytes());
    }
}
