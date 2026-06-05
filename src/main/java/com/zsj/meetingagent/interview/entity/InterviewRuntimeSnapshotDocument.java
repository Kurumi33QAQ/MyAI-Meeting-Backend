package com.zsj.meetingagent.interview.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * 面试运行快照 MongoDB 文档。
 * 同时保存步骤日志和可恢复运行态，Redis 热态丢失后可以从这里恢复面试进度。
 */
@Document(collection = "interview_runtime_snapshot")
@CompoundIndex(name = "idx_interview_runtime_session_time", def = "{'sessionId': 1, 'createdAt': 1}")
public class InterviewRuntimeSnapshotDocument {

    @Id
    private String id;

    @Indexed
    private String sessionId;

    @Indexed
    private String username;

    private String stepType;

    private String content;

    private String status;

    private int currentQuestionIndex;

    private int answeredCount;

    private int questionCount;

    private Integer totalScore;

    /**
     * 运行态版本号。
     * 每次关键状态变化递增，后续做 CAS 或排查旧状态覆盖新状态时可以用它判断先后顺序。
     */
    private long version;

    private String restoreSource;

    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getStepType() {
        return stepType;
    }

    public void setStepType(String stepType) {
        this.stepType = stepType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public void setCurrentQuestionIndex(int currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }

    public int getAnsweredCount() {
        return answeredCount;
    }

    public void setAnsweredCount(int answeredCount) {
        this.answeredCount = answeredCount;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getRestoreSource() {
        return restoreSource;
    }

    public void setRestoreSource(String restoreSource) {
        this.restoreSource = restoreSource;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
