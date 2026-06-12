package com.zsj.meetingagent.rag.model;

/**
 * 向量库召回结果。
 * chunkId 对应 MySQL knowledge_chunk.chunk_id，similarity 是 pgvector 余弦相似度换算后的分数。
 */
public record VectorSearchResult(
        String chunkId,
        double similarity
) {
}
