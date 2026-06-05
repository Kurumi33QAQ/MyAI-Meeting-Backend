package com.zsj.meetingagent.knowledge.entity;

import java.time.Instant;

/**
 * 知识库片段。
 * 一个文档会被按业务结构拆成多个 chunk，检索和 rerank 都围绕 chunk 进行。
 */
public record KnowledgeChunk(
        Long id,
        String chunkId,
        String documentId,
        String username,
        String sourceId,
        String documentType,
        String sectionName,
        int chunkIndex,
        int sectionOrder,
        String content,
        String summary,
        String tags,
        String metadataJson,
        Instant createdAt,
        Instant updatedAt,
        int deleted
) {
}
