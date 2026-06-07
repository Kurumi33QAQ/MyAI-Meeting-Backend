package com.zsj.meetingagent.interview.entity;

import java.time.Instant;

/**
 * 单轮追问快照。
 * 一个主问题可以拥有 F1/F2/F3 多轮追问，每一轮都保存问题、回答、评分和规则链轨迹。
 */
public class InterviewFollowUpRound {

    private int round;

    private String question;

    private String answer;

    private Integer score;

    private String feedback;

    private String ruleTrace;

    private Instant createdAt;

    private Instant answeredAt;

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
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

    public String getRuleTrace() {
        return ruleTrace;
    }

    public void setRuleTrace(String ruleTrace) {
        this.ruleTrace = ruleTrace;
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
