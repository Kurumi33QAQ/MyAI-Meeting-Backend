package com.zsj.meetingagent.chat.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * 聊天消息 MongoDB 文档。
 * 每条用户输入和 AI 回复都保存为独立快照，后续做上下文恢复、Agent trace 和 RAG 评估时可以复用。
 */
@Document(collection = "chat_message")
@CompoundIndex(name = "idx_message_session_sequence", def = "{'sessionId': 1, 'sequence': 1}")
public class ChatMessageDocument {

    @Id
    private String id;

    @Indexed
    private String sessionId;

    @Indexed
    private String username;

    private String role;

    private String content;

    private String model;

    private int sequence;

    private Instant createdAt;

    private int deleted;

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }
}
