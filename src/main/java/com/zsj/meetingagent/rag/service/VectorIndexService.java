package com.zsj.meetingagent.rag.service;

import com.zsj.meetingagent.knowledge.entity.KnowledgeChunk;
import com.zsj.meetingagent.rag.model.VectorSearchResult;

import java.util.List;

/**
 * RAG 向量索引服务。
 * MySQL 保存 chunk 主数据，本接口负责把 chunk 同步到 pgvector，并在检索时先做向量召回。
 */
public interface VectorIndexService {

    boolean enabled();

    void deleteBySource(String username, String sourceId, String documentType);

    void upsertChunk(KnowledgeChunk chunk);

    List<VectorSearchResult> search(String username, String query, List<String> documentTypes, int topK);
}
