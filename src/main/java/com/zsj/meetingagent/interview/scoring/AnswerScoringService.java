package com.zsj.meetingagent.interview.scoring;

/**
 * 面试回答评分服务。
 * 评分实现需要识别拒答、技术细节、个人职责、分析过程和量化结果。
 */
public interface AnswerScoringService {

    AnswerScoreResult score(String question, String evaluationPoints, String answer);
}
