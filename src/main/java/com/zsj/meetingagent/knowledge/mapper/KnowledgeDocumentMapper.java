package com.zsj.meetingagent.knowledge.mapper;

import com.zsj.meetingagent.knowledge.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;

/**
 * 知识库文档 Mapper。
 * 负责维护文档级主数据，chunk 明细由 KnowledgeChunkMapper 单独管理。
 */
@Mapper
public interface KnowledgeDocumentMapper {

    @Insert("""
            INSERT INTO knowledge_document (
                document_id, username, source_id, document_type, title, tags,
                created_at, updated_at, deleted
            ) VALUES (
                #{document.documentId}, #{document.username}, #{document.sourceId}, #{document.documentType},
                #{document.title}, #{document.tags}, #{document.createdAt}, #{document.updatedAt}, #{document.deleted}
            )
            """)
    int insert(@Param("document") KnowledgeDocument document);

    @Update("""
            UPDATE knowledge_document
            SET deleted = 1, updated_at = #{updatedAt}
            WHERE username = #{username}
              AND source_id = #{sourceId}
              AND document_type = #{documentType}
              AND deleted = 0
            """)
    int softDeleteBySource(
            @Param("username") String username,
            @Param("sourceId") String sourceId,
            @Param("documentType") String documentType,
            @Param("updatedAt") java.time.Instant updatedAt
    );

    @Select("""
            SELECT
                id,
                document_id AS documentId,
                username,
                source_id AS sourceId,
                document_type AS documentType,
                title,
                tags,
                created_at AS createdAt,
                updated_at AS updatedAt,
                deleted
            FROM knowledge_document
            WHERE document_id = #{documentId}
              AND username = #{username}
              AND deleted = 0
            LIMIT 1
            """)
    Optional<KnowledgeDocument> findByDocumentIdAndUsername(
            @Param("documentId") String documentId,
            @Param("username") String username
    );
}
