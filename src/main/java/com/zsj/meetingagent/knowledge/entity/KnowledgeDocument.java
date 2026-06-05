package com.zsj.meetingagent.knowledge.entity;

import java.time.Instant;

/**
 * 知识库文档主数据。
 * 记录简历、岗位 JD、面试题库等原始文档的来源信息，具体可检索内容拆到 knowledge_chunk 表中。
 */
public record KnowledgeDocument(
        Long id,
        String documentId,
        String username,
        String sourceId,
        String documentType,
        String title,
        String tags,
        Instant createdAt,
        Instant updatedAt,
        int deleted
) {
}
