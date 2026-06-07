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

    /**
     * 多 Agent 出题编排的 runId。
     * 通过它可以关联 agent_run 和 agent_step_trace，复盘这道题由哪些 Agent 共同生成。
     */
    private String agentRunId;

    private String userAnswer;

    private Integer score;

    private String feedback;

    private String followUpQuestion;

    /**
     * 追问作为独立可回答回合保存，但仍归属于当前主问题，不重复计入主问题完成数量。
     */
    private String followUpAnswer;

    private Integer followUpScore;

    private String followUpFeedback;

    private Instant followUpAnsweredAt;

    /**
     * LiteFlow 追问规则链轨迹。
     * 记录每个规则节点是否命中，方便解释为什么追问或为什么不追问。
     */
    private String followUpRuleTrace;

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

    public String getAgentRunId() {
        return agentRunId;
    }

    public void setAgentRunId(String agentRunId) {
        this.agentRunId = agentRunId;
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

    public String getFollowUpAnswer() {
        return followUpAnswer;
    }

    public void setFollowUpAnswer(String followUpAnswer) {
        this.followUpAnswer = followUpAnswer;
    }

    public Integer getFollowUpScore() {
        return followUpScore;
    }

    public void setFollowUpScore(Integer followUpScore) {
        this.followUpScore = followUpScore;
    }

    public String getFollowUpFeedback() {
        return followUpFeedback;
    }

    public void setFollowUpFeedback(String followUpFeedback) {
        this.followUpFeedback = followUpFeedback;
    }

    public Instant getFollowUpAnsweredAt() {
        return followUpAnsweredAt;
    }

    public void setFollowUpAnsweredAt(Instant followUpAnsweredAt) {
        this.followUpAnsweredAt = followUpAnsweredAt;
    }

    public String getFollowUpRuleTrace() {
        return followUpRuleTrace;
    }

    public void setFollowUpRuleTrace(String followUpRuleTrace) {
        this.followUpRuleTrace = followUpRuleTrace;
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
