package com.zsj.meetingagent.rag.service.impl;

import com.zsj.meetingagent.knowledge.entity.KnowledgeChunk;
import com.zsj.meetingagent.knowledge.mapper.KnowledgeChunkMapper;
import com.zsj.meetingagent.rag.config.RagProperties;
import com.zsj.meetingagent.rag.dto.RetrieveEvidenceRequest;
import com.zsj.meetingagent.rag.service.RetrievalService;
import com.zsj.meetingagent.rag.vo.EvidenceResponse;
import com.zsj.meetingagent.rag.vo.RetrieveEvidenceResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 默认 RAG 检索服务。
 * 当前实现为“候选召回 topK -> rerank 重排序 -> 选择 finalK”，后续可把第一阶段替换成向量数据库。
 */
@Service
public class DefaultRetrievalService implements RetrievalService {

    private final KnowledgeChunkMapper chunkMapper;
    private final TextScoreService textScoreService;
    private final RagProperties ragProperties;

    public DefaultRetrievalService(
            KnowledgeChunkMapper chunkMapper,
            TextScoreService textScoreService,
            RagProperties ragProperties
    ) {
        this.chunkMapper = chunkMapper;
        this.textScoreService = textScoreService;
        this.ragProperties = ragProperties;
    }

    @Override
    public RetrieveEvidenceResponse retrieve(String username, RetrieveEvidenceRequest request) {
        int topK = normalize(request.topK(), ragProperties.getDefaultTopK(), 1, 50);
        int finalK = normalize(request.finalK(), ragProperties.getDefaultFinalK(), 1, Math.min(topK, 10));
        List<String> documentTypes = request.documentTypes();

        List<ScoredChunk> recalled = chunkMapper.findActiveByUsername(username).stream()
                .filter(chunk -> matchDocumentType(chunk, documentTypes))
                .map(chunk -> scoreRecall(request.query(), chunk))
                .filter(scored -> scored.recallScore() > 0)
                .sorted(Comparator.comparingDouble(ScoredChunk::recallScore).reversed())
                .limit(topK)
                .toList();

        List<EvidenceResponse> selected = recalled.stream()
                .map(scored -> toEvidence(scored, rerank(request.query(), scored)))
                .sorted(Comparator.comparingDouble(EvidenceResponse::rerankScore).reversed())
                .limit(finalK)
                .toList();

        double confidence = selected.stream()
                .mapToDouble(EvidenceResponse::rerankScore)
                .max()
                .orElse(0.0);
        return new RetrieveEvidenceResponse(request.query(), recalled.size(), selected.size(), confidence, selected);
    }

    @Override
    public List<EvidenceResponse> retrieveForInterview(
            String username,
            String resumeId,
            String sessionId,
            String jobTitle,
            String companyName,
            String jobDescription
    ) {
        String query = """
                目标岗位：%s
                目标公司：%s
                岗位描述：%s
                请召回候选人简历和岗位 JD 中能支撑模拟面试出题的证据。
                """.formatted(blankToDefault(jobTitle, "未填写"), blankToDefault(companyName, "未填写"), blankToDefault(jobDescription, ""));
        RetrieveEvidenceResponse response = retrieve(username, new RetrieveEvidenceRequest(
                query,
                List.of("RESUME", "JOB_DESCRIPTION", "QUESTION_BANK"),
                20,
                5
        ));
        return response.evidenceList();
    }

    private ScoredChunk scoreRecall(String query, KnowledgeChunk chunk) {
        String candidate = "%s\n%s\n%s\n%s".formatted(
                chunk.sectionName(),
                chunk.summary(),
                chunk.tags(),
                chunk.content()
        );
        return new ScoredChunk(chunk, textScoreService.score(query, candidate));
    }

    private double rerank(String query, ScoredChunk scored) {
        KnowledgeChunk chunk = scored.chunk();
        double score = scored.recallScore();
        /*
         * rerank 阶段加入业务特征：面试出题更需要项目经历、技能栈和 JD 要求。
         * 这不是最终版深度 reranker，但能清楚体现“召回”和“重排序”是两个阶段。
         */
        String sectionName = chunk.sectionName();
        if (containsAny(sectionName, "项目", "技能", "任职要求", "岗位职责", "技术栈")) {
            score += 0.12;
        }
        String lowerType = chunk.documentType().toLowerCase(Locale.ROOT);
        if (query.contains("岗位") && lowerType.contains("job")) {
            score += 0.08;
        }
        if (query.contains("简历") && lowerType.contains("resume")) {
            score += 0.08;
        }
        return Math.min(score, 1.0);
    }

    private EvidenceResponse toEvidence(ScoredChunk scored, double rerankScore) {
        KnowledgeChunk chunk = scored.chunk();
        return new EvidenceResponse(
                chunk.chunkId(),
                chunk.documentId(),
                chunk.sourceId(),
                chunk.documentType(),
                chunk.sectionName(),
                chunk.content(),
                chunk.summary(),
                chunk.tags(),
                round(scored.recallScore()),
                round(rerankScore)
        );
    }

    private boolean matchDocumentType(KnowledgeChunk chunk, List<String> documentTypes) {
        if (CollectionUtils.isEmpty(documentTypes)) {
            return true;
        }
        return documentTypes.stream()
                .filter(StringUtils::hasText)
                .map(type -> type.trim().toUpperCase(Locale.ROOT))
                .anyMatch(type -> type.equals(chunk.documentType()));
    }

    private boolean containsAny(String text, String... keywords) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private int normalize(Integer value, int defaultValue, int min, int max) {
        int normalized = value == null ? defaultValue : value;
        return Math.max(min, Math.min(normalized, max));
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private String blankToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private record ScoredChunk(KnowledgeChunk chunk, double recallScore) {
    }
}
