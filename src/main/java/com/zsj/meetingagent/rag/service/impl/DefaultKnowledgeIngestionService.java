package com.zsj.meetingagent.rag.service.impl;

import com.zsj.meetingagent.common.exception.BusinessException;
import com.zsj.meetingagent.knowledge.entity.KnowledgeChunk;
import com.zsj.meetingagent.knowledge.entity.KnowledgeDocument;
import com.zsj.meetingagent.knowledge.mapper.KnowledgeChunkMapper;
import com.zsj.meetingagent.knowledge.mapper.KnowledgeDocumentMapper;
import com.zsj.meetingagent.rag.model.StructuredChunk;
import com.zsj.meetingagent.rag.service.KnowledgeIngestionService;
import com.zsj.meetingagent.rag.service.StructuredChunkService;
import com.zsj.meetingagent.rag.service.VectorIndexService;
import com.zsj.meetingagent.resume.entity.UploadedFile;
import com.zsj.meetingagent.resume.mapper.UploadedFileMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 默认知识入库服务。
 * 把业务数据转换为 knowledge_document 和 knowledge_chunk，为检索、rerank、证据引用做准备。
 */
@Service
public class DefaultKnowledgeIngestionService implements KnowledgeIngestionService {

    private static final int NOT_DELETED = 0;

    private final UploadedFileMapper uploadedFileMapper;
    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final StructuredChunkService structuredChunkService;
    private final VectorIndexService vectorIndexService;

    public DefaultKnowledgeIngestionService(
            UploadedFileMapper uploadedFileMapper,
            KnowledgeDocumentMapper documentMapper,
            KnowledgeChunkMapper chunkMapper,
            StructuredChunkService structuredChunkService,
            VectorIndexService vectorIndexService
    ) {
        this.uploadedFileMapper = uploadedFileMapper;
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.structuredChunkService = structuredChunkService;
        this.vectorIndexService = vectorIndexService;
    }

    @Override
    public void ingestResume(String username, String resumeId) {
        UploadedFile resume = uploadedFileMapper.findByFileIdAndUsername(resumeId, username)
                .orElseThrow(() -> new BusinessException("R0401", "简历不存在或无权访问"));
        List<StructuredChunk> chunks = structuredChunkService.chunkResume(resume.textContent());
        saveDocumentAndChunks(
                username,
                resume.fileId(),
                "RESUME",
                "简历：" + resume.fileName(),
                "resume," + resume.documentType(),
                chunks
        );
    }

    @Override
    public void ingestJobDescription(String username, String sessionId, String jobTitle, String companyName, String jobDescription) {
        if (!StringUtils.hasText(jobTitle) && !StringUtils.hasText(jobDescription) && !StringUtils.hasText(companyName)) {
            return;
        }
        List<StructuredChunk> chunks = structuredChunkService.chunkJobDescription(jobTitle, companyName, jobDescription);
        saveDocumentAndChunks(
                username,
                sessionId,
                "JOB_DESCRIPTION",
                "岗位：" + blankToDefault(companyName, "未填写公司") + " - " + blankToDefault(jobTitle, "未填写岗位"),
                "job-description," + blankToDefault(jobTitle, "unknown"),
                chunks
        );
    }

    private void saveDocumentAndChunks(
            String username,
            String sourceId,
            String documentType,
            String title,
            String tags,
            List<StructuredChunk> chunks
    ) {
        Instant now = Instant.now();
        /*
         * 同一来源重复入库时先软删除旧版本，再插入新版本。
         * 这样简历或 JD 更新后检索不会同时命中新旧内容。
         */
        documentMapper.softDeleteBySource(username, sourceId, documentType, now);
        chunkMapper.softDeleteBySource(username, sourceId, documentType, now);
        vectorIndexService.deleteBySource(username, sourceId, documentType);

        String documentId = UUID.randomUUID().toString();
        documentMapper.insert(new KnowledgeDocument(
                null,
                documentId,
                username,
                sourceId,
                documentType,
                title,
                tags,
                now,
                now,
                NOT_DELETED
        ));

        for (StructuredChunk chunk : chunks) {
            KnowledgeChunk knowledgeChunk = new KnowledgeChunk(
                    null,
                    UUID.randomUUID().toString(),
                    documentId,
                    username,
                    sourceId,
                    chunk.documentType(),
                    chunk.sectionName(),
                    chunk.chunkIndex(),
                    chunk.sectionOrder(),
                    chunk.content(),
                    chunk.summary(),
                    chunk.tags(),
                    chunk.metadataJson(),
                    now,
                    now,
                    NOT_DELETED
            );
            chunkMapper.insert(knowledgeChunk);
            /*
             * MySQL 是 chunk 主数据，pgvector 是可选索引。
             * 向量索引写入失败时是否阻断上传，由 app.rag.vector.required 控制。
             */
            vectorIndexService.upsertChunk(knowledgeChunk);
        }
    }

    private String blankToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
