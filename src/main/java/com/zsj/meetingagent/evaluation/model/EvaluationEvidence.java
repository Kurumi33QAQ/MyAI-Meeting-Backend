package com.zsj.meetingagent.evaluation.model;

/**
 * 评测用标准证据。
 * evidenceId 是后续计算 citation_accuracy 的依据，text 是判断回答是否有证据支撑的来源。
 */
public record EvaluationEvidence(
        String id,
        String text,
        String documentType,
        String sectionName,
        String tags
) {
}
