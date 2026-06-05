package com.zsj.meetingagent.evaluation.controller;

import com.zsj.meetingagent.auth.security.LoginUserContext;
import com.zsj.meetingagent.common.result.ApiResponse;
import com.zsj.meetingagent.evaluation.dto.RunEvaluationRequest;
import com.zsj.meetingagent.evaluation.service.EvaluationService;
import com.zsj.meetingagent.evaluation.vo.EvaluationRunResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Evaluation 自动评测接口。
 * 用真实测试集跑对照实验，输出幻觉率、命中率、引用准确率和平均耗时。
 */
@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/runs")
    public ApiResponse<EvaluationRunResponse> runEvaluation(
            @Valid @RequestBody(required = false) RunEvaluationRequest request
    ) {
        return ApiResponse.success(evaluationService.runEvaluation(LoginUserContext.currentUsername(), request));
    }

    @GetMapping("/runs/{runId}")
    public ApiResponse<EvaluationRunResponse> getEvaluationRun(@PathVariable String runId) {
        return ApiResponse.success(evaluationService.getEvaluationRun(LoginUserContext.currentUsername(), runId));
    }
}
