package com.zsj.meetingagent.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 检索配置。
 * 当前阶段使用本地关键词召回和 rerank，后续接向量库时保留同一组 topK/finalK 参数。
 */
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    private int defaultTopK = 20;

    private int defaultFinalK = 5;

    private double minConfidence = 0.15;

    public int getDefaultTopK() {
        return defaultTopK;
    }

    public void setDefaultTopK(int defaultTopK) {
        this.defaultTopK = defaultTopK;
    }

    public int getDefaultFinalK() {
        return defaultFinalK;
    }

    public void setDefaultFinalK(int defaultFinalK) {
        this.defaultFinalK = defaultFinalK;
    }

    public double getMinConfidence() {
        return minConfidence;
    }

    public void setMinConfidence(double minConfidence) {
        this.minConfidence = minConfidence;
    }
}
