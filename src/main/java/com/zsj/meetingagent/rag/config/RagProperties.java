package com.zsj.meetingagent.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 检索配置。
 * 支持本地文本召回和可选 pgvector 向量召回，最终都进入同一套 rerank 与 evidence 引用流程。
 */
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    private int defaultTopK = 20;

    private int defaultFinalK = 5;

    private double minConfidence = 0.15;

    private Vector vector = new Vector();

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

    public Vector getVector() {
        return vector;
    }

    public void setVector(Vector vector) {
        this.vector = vector;
    }

    /**
     * pgvector 向量索引配置。
     * 这里单独放在 app.rag.vector 下，避免和 MySQL 主业务数据源混在一起。
     */
    public static class Vector {

        private boolean enabled = false;

        private boolean required = false;

        private String jdbcUrl = "jdbc:postgresql://localhost:5432/my_ai_meeting_vector";

        private String username = "postgres";

        private String password = "1234";

        private int dimension = 1536;

        private String tableName = "rag_vector_chunk";

        private String embeddingModel = "text-embedding-3-small";

        private boolean embeddingMockEnabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getDimension() {
            return dimension;
        }

        public void setDimension(int dimension) {
            this.dimension = dimension;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getEmbeddingModel() {
            return embeddingModel;
        }

        public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }

        public boolean isEmbeddingMockEnabled() {
            return embeddingMockEnabled;
        }

        public void setEmbeddingMockEnabled(boolean embeddingMockEnabled) {
            this.embeddingMockEnabled = embeddingMockEnabled;
        }
    }
}
