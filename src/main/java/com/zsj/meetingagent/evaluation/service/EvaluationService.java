package com.zsj.meetingagent.evaluation.service;

import com.zsj.meetingagent.evaluation.dto.RunEvaluationRequest;
import com.zsj.meetingagent.evaluation.vo.EvaluationRunResponse;

/**
 * Evaluation 评测服务。
 * 负责执行对照实验、统计指标、落库并生成报告。
 */
public interface EvaluationService {

    EvaluationRunResponse runEvaluation(String username, RunEvaluationRequest request);

    EvaluationRunResponse getEvaluationRun(String username, String runId);
}
