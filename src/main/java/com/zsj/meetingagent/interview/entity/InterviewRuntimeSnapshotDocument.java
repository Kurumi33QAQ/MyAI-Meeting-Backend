package com.zsj.meetingagent.interview.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * 面试运行快照 MongoDB 文档。
 * 记录创建会话、生成题目、提交回答、生成报告等关键步骤，延续 Agent 可追踪设计。
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
