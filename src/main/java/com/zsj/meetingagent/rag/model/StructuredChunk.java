package com.zsj.meetingagent.rag.model;

/**
 * 业务化 chunk 中间模型。
 * chunk 还没入库前先用这个对象承载结构信息，避免在切分逻辑里直接拼 SQL 实体。
 */
public record StructuredChunk(
        String documentType,
        String sectionName,
        int chunkIndex,
        int sectionOrder,
        String content,
        String summary,
        String tags,
        String metadataJson
) {
}
