package com.zsj.meetingagent.interview.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * 面试题目快照 MongoDB 文档。
 * 保存题目、参考点、用户回答和评分结果，便于复盘每一题为什么给出对应建议。
 */
@Document(collection = "interview_question_snapshot")
@CompoundIndex(name = "idx_interview_question_order", def = "{'sessionId': 1, 'questionOrder': 1}")
public class InterviewQuestionSnapshotDocument {

    @Id
    private String questionId;

    @Indexed
    private String sessionId;

    @Indexed
    private String username;

    private int questionOrder;

    private String question;

    private String referenceAnswer;

    private String evaluationPoints;

    private String followUpDirection;

    private List<String> evidenceIds;

    private String evidenceSummary;

    private String userAnswer;

    private Integer score;

    private String feedback;

    private String followUpQuestion;

    private Instant createdAt;

    private Instant answeredAt;

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(int questionOrder) {
        this.questionOrder = questionOrder;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getReferenceAnswer() {
        return referenceAnswer;
    }

    public void setReferenceAnswer(String referenceAnswer) {
        this.referenceAnswer = referenceAnswer;
    }

    public String getEvaluationPoints() {
        return evaluationPoints;
    }

    public void setEvaluationPoints(String evaluationPoints) {
        this.evaluationPoints = evaluationPoints;
    }

    public String getFollowUpDirection() {
        return followUpDirection;
    }

    public void setFollowUpDirection(String followUpDirection) {
        this.followUpDirection = followUpDirection;
    }

    public List<String> getEvidenceIds() {
        return evidenceIds;
    }

    public void setEvidenceIds(List<String> evidenceIds) {
        this.evidenceIds = evidenceIds;
    }

    public String getEvidenceSummary() {
        return evidenceSummary;
    }

    public void setEvidenceSummary(String evidenceSummary) {
        this.evidenceSummary = evidenceSummary;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getFollowUpQuestion() {
        return followUpQuestion;
    }

    public void setFollowUpQuestion(String followUpQuestion) {
        this.followUpQuestion = followUpQuestion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(Instant answeredAt) {
        this.answeredAt = answeredAt;
    }
}
