package com.zsj.meetingagent.rag.service;

import java.util.List;

/**
 * 文本向量化服务。
 * RAG 入库和检索都通过该接口获取 embedding，便于在 OpenAI 兼容接口、本地 mock 或后续私有 embedding 模型之间切换。
 */
public interface EmbeddingService {

    List<Double> embed(String text);
}
