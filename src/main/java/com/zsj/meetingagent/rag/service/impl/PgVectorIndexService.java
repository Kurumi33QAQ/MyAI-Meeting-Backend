package com.zsj.meetingagent.rag.service.impl;

import com.zsj.meetingagent.common.exception.BusinessException;
import com.zsj.meetingagent.knowledge.entity.KnowledgeChunk;
import com.zsj.meetingagent.rag.config.RagProperties;
import com.zsj.meetingagent.rag.model.VectorSearchResult;
import com.zsj.meetingagent.rag.service.EmbeddingService;
import com.zsj.meetingagent.rag.service.VectorIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * pgvector 向量索引实现。
 * 它不替代 MySQL 主数据，只把 chunk 的 embedding 写入 PostgreSQL/pgvector，用于第一阶段向量召回。
 */
@Service
public class PgVectorIndexService implements VectorIndexService {

    private static final Logger log = LoggerFactory.getLogger(PgVectorIndexService.class);

    private final RagProperties ragProperties;
    private final EmbeddingService embeddingService;

    public PgVectorIndexService(RagProperties ragProperties, EmbeddingService embeddingService) {
        this.ragProperties = ragProperties;
        this.embeddingService = embeddingService;
    }

    @Override
    public boolean enabled() {
        return ragProperties.getVector().isEnabled();
    }

    @Override
    public void deleteBySource(String username, String sourceId, String documentType) {
        if (!enabled()) {
            return;
        }
        try (Connection connection = connection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement("""
                    DELETE FROM %s
                    WHERE username = ?
                      AND source_id = ?
                      AND document_type = ?
                    """.formatted(tableName()))) {
                statement.setString(1, username);
                statement.setString(2, sourceId);
                statement.setString(3, documentType);
                statement.executeUpdate();
            }
        } catch (Exception e) {
            handleFailure("删除 pgvector 旧索引失败", e);
        }
    }

    @Override
    public void upsertChunk(KnowledgeChunk chunk) {
        if (!enabled()) {
            return;
        }
        try (Connection connection = connection()) {
            ensureSchema(connection);
            List<Double> embedding = embeddingService.embed(vectorText(chunk));
            validateDimension(embedding);
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO %s (
                        chunk_id, username, source_id, document_type, section_name,
                        content, metadata_json, embedding, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?::vector, ?)
                    ON CONFLICT (chunk_id) DO UPDATE SET
                        username = EXCLUDED.username,
                        source_id = EXCLUDED.source_id,
                        document_type = EXCLUDED.document_type,
                        section_name = EXCLUDED.section_name,
                        content = EXCLUDED.content,
                        metadata_json = EXCLUDED.metadata_json,
                        embedding = EXCLUDED.embedding,
                        updated_at = EXCLUDED.updated_at
                    """.formatted(tableName()))) {
                statement.setString(1, chunk.chunkId());
                statement.setString(2, chunk.username());
                statement.setString(3, chunk.sourceId());
                statement.setString(4, chunk.documentType());
                statement.setString(5, chunk.sectionName());
                statement.setString(6, chunk.content());
                statement.setString(7, StringUtils.hasText(chunk.metadataJson()) ? chunk.metadataJson() : "{}");
                statement.setString(8, toVectorLiteral(embedding));
                statement.setTimestamp(9, Timestamp.from(Instant.now()));
                statement.executeUpdate();
            }
        } catch (Exception e) {
            handleFailure("写入 pgvector 向量索引失败", e);
        }
    }

    @Override
    public List<VectorSearchResult> search(String username, String query, List<String> documentTypes, int topK) {
        if (!enabled() || !StringUtils.hasText(query)) {
            return List.of();
        }
        try (Connection connection = connection()) {
            ensureSchema(connection);
            List<Double> embedding = embeddingService.embed(query);
            validateDimension(embedding);
            String vector = toVectorLiteral(embedding);
            String typeFilter = buildDocumentTypeFilter(documentTypes);
            String sql = """
                    SELECT chunk_id, 1 - (embedding <=> ?::vector) AS similarity
                    FROM %s
                    WHERE username = ?
                    %s
                    ORDER BY embedding <=> ?::vector
                    LIMIT ?
                    """.formatted(tableName(), typeFilter);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int index = 1;
                statement.setString(index++, vector);
                statement.setString(index++, username);
                if (!CollectionUtils.isEmpty(documentTypes)) {
                    for (String type : normalizedTypes(documentTypes)) {
                        statement.setString(index++, type);
                    }
                }
                statement.setString(index++, vector);
                statement.setInt(index, topK);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<VectorSearchResult> results = new ArrayList<>();
                    while (resultSet.next()) {
                        results.add(new VectorSearchResult(
                                resultSet.getString("chunk_id"),
                                Math.max(0.0, resultSet.getDouble("similarity"))
                        ));
                    }
                    return results;
                }
            }
        } catch (Exception e) {
            handleFailure("pgvector 向量召回失败", e);
            return List.of();
        }
    }

    private void ensureSchema(Connection connection) throws Exception {
        int dimension = ragProperties.getVector().getDimension();
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE EXTENSION IF NOT EXISTS vector");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS %s (
                        chunk_id VARCHAR(80) PRIMARY KEY,
                        username VARCHAR(64) NOT NULL,
                        source_id VARCHAR(80) NOT NULL,
                        document_type VARCHAR(40) NOT NULL,
                        section_name VARCHAR(80) NOT NULL,
                        content TEXT NOT NULL,
                        metadata_json JSONB,
                        embedding vector(%d) NOT NULL,
                        updated_at TIMESTAMPTZ NOT NULL
                    )
                    """.formatted(tableName(), dimension));
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS %s_username_type_idx
                    ON %s(username, document_type)
                    """.formatted(tableName(), tableName()));
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS %s_embedding_hnsw_idx
                    ON %s USING hnsw (embedding vector_cosine_ops)
                    """.formatted(tableName(), tableName()));
        }
    }

    private Connection connection() throws Exception {
        RagProperties.Vector vector = ragProperties.getVector();
        Properties properties = new Properties();
        properties.setProperty("user", vector.getUsername());
        properties.setProperty("password", vector.getPassword());
        return DriverManager.getConnection(vector.getJdbcUrl(), properties);
    }

    private String buildDocumentTypeFilter(List<String> documentTypes) {
        List<String> types = normalizedTypes(documentTypes);
        if (types.isEmpty()) {
            return "";
        }
        return " AND document_type IN (" + types.stream().map(type -> "?").collect(Collectors.joining(",")) + ")";
    }

    private List<String> normalizedTypes(List<String> documentTypes) {
        if (CollectionUtils.isEmpty(documentTypes)) {
            return List.of();
        }
        return documentTypes.stream()
                .filter(StringUtils::hasText)
                .map(type -> type.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private void validateDimension(List<Double> embedding) {
        int expected = ragProperties.getVector().getDimension();
        if (embedding.size() != expected) {
            throw new BusinessException("R0503", "Embedding 维度不匹配，期望 %d，实际 %d".formatted(expected, embedding.size()));
        }
    }

    private String vectorText(KnowledgeChunk chunk) {
        return """
               章节：%s
               摘要：%s
               标签：%s
               正文：%s
               """.formatted(chunk.sectionName(), blank(chunk.summary()), blank(chunk.tags()), chunk.content());
    }

    private String toVectorLiteral(List<Double> embedding) {
        return embedding.stream()
                .map(value -> String.format(Locale.US, "%.8f", value))
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String tableName() {
        String tableName = ragProperties.getVector().getTableName();
        if (!tableName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new BusinessException("R0504", "pgvector 表名配置不合法");
        }
        return tableName;
    }

    private String blank(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    private void handleFailure(String message, Exception e) {
        if (ragProperties.getVector().isRequired()) {
            throw new BusinessException("R0505", message + "：" + e.getMessage());
        }
        log.warn("{}，已回退到本地文本召回。原因：{}", message, e.getMessage());
    }
}
