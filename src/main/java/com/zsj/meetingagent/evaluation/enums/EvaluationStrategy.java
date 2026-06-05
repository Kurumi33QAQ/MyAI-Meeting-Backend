package com.zsj.meetingagent.evaluation.enums;

/**
 * 评测方案枚举。
 * 同一批测试集会依次跑这些方案，用来比较普通直答、基础检索、rerank 和自检拒答的效果差异。
 */
public enum EvaluationStrategy {

    BASELINE("baseline", "普通大模型直接回答"),
    RAG_WITHOUT_RERANK("rag_without_rerank", "基础召回，不做 rerank"),
    RAG_WITH_RERANK("rag_with_rerank", "结构化 chunk + rerank"),
    SELF_CHECK_RAG("self_check_rag", "结构化 chunk + rerank + 回答自检");

    private final String code;
    private final String description;

    EvaluationStrategy(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String code() {
        return code;
    }

    public String description() {
        return description;
    }
}
