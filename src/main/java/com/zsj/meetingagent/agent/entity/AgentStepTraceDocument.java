package com.zsj.meetingagent.agent.entity;

import com.zsj.meetingagent.agent.enums.AgentStepType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Agent 步骤轨迹文档。
 * 每条记录对应 Thought、Action、Observation 或 Final Answer 中的一步，方便后续复盘 Agent 为什么这样回答。
 */
@Document(collection = "agent_step_trace")
public class AgentStepTraceDocument {

    @Id
    private String id;

    private String runId;

    private String username;

    private AgentStepType stepType;

    private int stepOrder;

    private String toolName;

    private String content;

    private Instant createdAt;

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

    public AgentStepType getStepType() {
        return stepType;
    }

    public void setStepType(AgentStepType stepType) {
        this.stepType = stepType;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(int stepOrder) {
        this.stepOrder = stepOrder;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
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
