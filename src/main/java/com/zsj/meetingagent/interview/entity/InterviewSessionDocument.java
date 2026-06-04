package com.zsj.meetingagent.interview.entity;

import com.zsj.meetingagent.interview.enums.InterviewSessionStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * 面试会话 MongoDB 文档。
 * 保存岗位 JD、运行状态和会话快照，适合承载后续多轮追问和 Agent trace 扩展字段。
 */
@Document(collection = "interview_session")
@CompoundIndex(name = "idx_interview_session_user_updated", def = "{'username': 1, 'updatedAt': -1}")
public class InterviewSessionDocument {

    @Id
    private String sessionId;

    @Indexed
    private String username;

    @Indexed
    private String resumeId;

    private String jobTitle;

    private String jobDescription;

    private InterviewSessionStatus status;

    private int questionCount;

    private int answeredCount;

    private Integer totalScore;

    private String reportSummary;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant completedAt;

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

    public String getResumeId() {
        return resumeId;
    }

    public void setResumeId(String resumeId) {
        this.resumeId = resumeId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public InterviewSessionStatus getStatus() {
        return status;
    }

    public void setStatus(InterviewSessionStatus status) {
        this.status = status;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public int getAnsweredCount() {
        return answeredCount;
    }

    public void setAnsweredCount(int answeredCount) {
        this.answeredCount = answeredCount;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public String getReportSummary() {
        return reportSummary;
    }

    public void setReportSummary(String reportSummary) {
        this.reportSummary = reportSummary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
