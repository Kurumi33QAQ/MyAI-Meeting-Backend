package com.zsj.meetingagent.rag.controller;

import com.zsj.meetingagent.auth.security.LoginUserContext;
import com.zsj.meetingagent.common.result.ApiResponse;
import com.zsj.meetingagent.rag.dto.RetrieveEvidenceRequest;
import com.zsj.meetingagent.rag.service.RetrievalService;
import com.zsj.meetingagent.rag.vo.RetrieveEvidenceResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 证据检索接口。
 * 给前端调试、Agent 工具调用和后续 evaluation 模块提供统一的证据召回入口。
 */
@RestController
public class RetrievalController {

    private final RetrievalService retrievalService;

    public RetrievalController(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @PostMapping("/api/retrieval/evidence")
    public ApiResponse<RetrieveEvidenceResponse> retrieveEvidence(@Valid @RequestBody RetrieveEvidenceRequest request) {
        return ApiResponse.success(retrievalService.retrieve(LoginUserContext.currentUsername(), request));
    }
}
