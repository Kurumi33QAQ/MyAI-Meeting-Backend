package com.zsj.meetingagent.evaluation.model;

import java.util.List;

/**
 * 单条评测样本。
 * question 是输入问题，groundTruth 是标准答案，evidence 是允许回答引用的证据集合。
 */
public record EvaluationCase(
        String id,
        String question,
        String groundTruth,
        List<EvaluationEvidence> evidence,
        String category
) {
}
