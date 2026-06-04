package com.zsj.meetingagent.agent.entity;

import com.zsj.meetingagent.agent.enums.AgentRunStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Agent 运行文档。
 * MongoDB 中一条记录代表一次完整 Agent 执行，适合保存输入、最终回答和运行状态这类半结构化快照。
 */
@Document(collection = "agent_run")
public class AgentRunDocument {

    @Id
    private String id;

    /**
     * 对外暴露的运行 ID，使用 UUID，避免直接暴露 MongoDB ObjectId。
     */
    private String runId;

    /**
     * 用户名用于做数据隔离，查询 Agent Run 时必须同时匹配 username。
     */
    private String username;

    private String input;

    private String sessionId;

    private String model;

    private AgentRunStatus status;

    private String finalAnswer;

    private String errorMessage;

    private Instant createdAt;

    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public AgentRunStatus getStatus() {
        return status;
    }

    public void setStatus(AgentRunStatus status) {
        this.status = status;
    }

    public String getFinalAnswer() {
        return finalAnswer;
    }

    public void setFinalAnswer(String finalAnswer) {
        this.finalAnswer = finalAnswer;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
}
