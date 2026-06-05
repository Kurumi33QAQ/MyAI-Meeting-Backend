package com.zsj.meetingagent.evaluation.model;

/**
 * 单条 case 的指标判断结果。
 * 业务上先判断正确性、引用准确性，再据此判断是否属于幻觉回答。
 */
public record EvaluationCaseScore(
        boolean answerCorrect,
        boolean citationCorrect,
        boolean hallucinated
) {
}
