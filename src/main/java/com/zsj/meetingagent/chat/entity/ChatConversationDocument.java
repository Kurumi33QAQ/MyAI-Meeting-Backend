package com.zsj.meetingagent.chat.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * 聊天会话 MongoDB 文档。
 * 会话属于半结构化 AI 数据，标题、模型、消息数量和更新时间会随着对话持续变化。
 */
@Document(collection = "chat_conversation")
@CompoundIndex(name = "idx_conversation_user_updated", def = "{'username': 1, 'updatedAt': -1}")
public class ChatConversationDocument {

    @Id
    private String sessionId;

    @Indexed
    private String username;

    private String title;

    private String model;

    private int messageCount;

    private Instant createdAt;

    private Instant updatedAt;

    private int deleted;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
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

    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }
}
