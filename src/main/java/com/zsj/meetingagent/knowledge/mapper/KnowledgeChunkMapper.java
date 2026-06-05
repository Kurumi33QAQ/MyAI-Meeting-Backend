package com.zsj.meetingagent.knowledge.mapper;

import com.zsj.meetingagent.knowledge.entity.KnowledgeChunk;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.Instant;
import java.util.List;

/**
 * 知识库 chunk Mapper。
 * 检索阶段会先从 MySQL 读取候选 chunk，后续可替换为真正的向量库召回。
 */
@Mapper
public interface KnowledgeChunkMapper {

    @Insert("""
            INSERT INTO knowledge_chunk (
                chunk_id, document_id, username, source_id, document_type, section_name,
                chunk_index, section_order, content, summary, tags, metadata_json,
                created_at, updated_at, deleted
            ) VALUES (
                #{chunk.chunkId}, #{chunk.documentId}, #{chunk.username}, #{chunk.sourceId}, #{chunk.documentType},
                #{chunk.sectionName}, #{chunk.chunkIndex}, #{chunk.sectionOrder}, #{chunk.content}, #{chunk.summary},
                #{chunk.tags}, #{chunk.metadataJson}, #{chunk.createdAt}, #{chunk.updatedAt}, #{chunk.deleted}
            )
            """)
    int insert(@Param("chunk") KnowledgeChunk chunk);

    @Update("""
            UPDATE knowledge_chunk
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
            @Param("updatedAt") Instant updatedAt
    );

    @Select("""
            SELECT
                id,
                chunk_id AS chunkId,
                document_id AS documentId,
                username,
                source_id AS sourceId,
                document_type AS documentType,
                section_name AS sectionName,
                chunk_index AS chunkIndex,
                section_order AS sectionOrder,
                content,
                summary,
                tags,
                metadata_json AS metadataJson,
                created_at AS createdAt,
                updated_at AS updatedAt,
                deleted
            FROM knowledge_chunk
            WHERE username = #{username}
              AND deleted = 0
            ORDER BY updated_at DESC, section_order ASC, chunk_index ASC
            """)
    List<KnowledgeChunk> findActiveByUsername(@Param("username") String username);
}
