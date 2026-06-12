package com.zsj.meetingagent.rag.service.impl;

import com.zsj.meetingagent.knowledge.entity.KnowledgeChunk;
import com.zsj.meetingagent.knowledge.mapper.KnowledgeChunkMapper;
import com.zsj.meetingagent.rag.config.RagProperties;
import com.zsj.meetingagent.rag.dto.RetrieveEvidenceRequest;
import com.zsj.meetingagent.rag.model.VectorSearchResult;
import com.zsj.meetingagent.rag.service.RetrievalService;
import com.zsj.meetingagent.rag.service.VectorIndexService;
import com.zsj.meetingagent.rag.vo.EvidenceResponse;
import com.zsj.meetingagent.rag.vo.RetrieveEvidenceResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 默认 RAG 检索服务。
 * 支持“pgvector 向量召回 topK -> 业务 rerank -> 选择 finalK”，未启用向量库时回退到本地文本召回。
 */
@Service
public class DefaultRetrievalService implements RetrievalService {

    private final KnowledgeChunkMapper chunkMapper;
    private final TextScoreService textScoreService;
    private final RagProperties ragProperties;
    private final VectorIndexService vectorIndexService;

    public DefaultRetrievalService(
            KnowledgeChunkMapper chunkMapper,
            TextScoreService textScoreService,
            RagProperties ragProperties,
            VectorIndexService vectorIndexService
    ) {
        this.chunkMapper = chunkMapper;
        this.textScoreService = textScoreService;
        this.ragProperties = ragProperties;
        this.vectorIndexService = vectorIndexService;
    }

    @Override
    public RetrieveEvidenceResponse retrieve(String username, RetrieveEvidenceRequest request) {
        int topK = normalize(request.topK(), ragProperties.getDefaultTopK(), 1, 50);
        int finalK = normalize(request.finalK(), ragProperties.getDefaultFinalK(), 1, Math.min(topK, 10));
        List<String> documentTypes = request.documentTypes();

        List<ScoredChunk> recalled = vectorIndexService.enabled()
                ? vectorRecall(username, request.query(), documentTypes, topK)
                : localTextRecall(username, request.query(), documentTypes, topK);

        if (recalled.isEmpty() && vectorIndexService.enabled()) {
            recalled = localTextRecall(username, request.query(), documentTypes, topK);
        }

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
        boolean hasJobContext = StringUtils.hasText(jobTitle)
                || StringUtils.hasText(companyName)
                || StringUtils.hasText(jobDescription);
        String query = hasJobContext
                ? """
                  目标岗位：%s
                  目标公司：%s
                  岗位描述：%s
                  请召回候选人简历、岗位 JD 和公开岗位情报中能支撑模拟面试出题的证据。
                  """.formatted(blankToDefault(jobTitle, "未填写"), blankToDefault(companyName, "未填写"), blankToDefault(jobDescription, ""))
                : """
                  用户未填写目标岗位、公司或 JD。
                  请只召回候选人简历和面试题知识库中能支撑项目经历、技能、职责和技术取舍追问的证据。
                  """;
        RetrieveEvidenceResponse response = retrieve(username, new RetrieveEvidenceRequest(
                query,
                hasJobContext
                        ? List.of("RESUME", "JOB_DESCRIPTION", "QUESTION_BANK")
                        : List.of("RESUME", "QUESTION_BANK"),
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

    private List<ScoredChunk> vectorRecall(String username, String query, List<String> documentTypes, int topK) {
        List<VectorSearchResult> vectorResults = vectorIndexService.search(username, query, documentTypes, topK);
        if (vectorResults.isEmpty()) {
            return List.of();
        }
        List<String> chunkIds = vectorResults.stream()
                .map(VectorSearchResult::chunkId)
                .toList();
        Map<String, KnowledgeChunk> chunks = chunkMapper.findActiveByChunkIds(username, chunkIds).stream()
                .collect(Collectors.toMap(KnowledgeChunk::chunkId, Function.identity()));
        return vectorResults.stream()
                .map(result -> {
                    KnowledgeChunk chunk = chunks.get(result.chunkId());
                    return chunk == null ? null : new ScoredChunk(chunk, result.similarity());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private List<ScoredChunk> localTextRecall(String username, String query, List<String> documentTypes, int topK) {
        return chunkMapper.findActiveByUsername(username).stream()
                .filter(chunk -> matchDocumentType(chunk, documentTypes))
                .map(chunk -> scoreRecall(query, chunk))
                .filter(scored -> scored.recallScore() > 0)
                .sorted(Comparator.comparingDouble(ScoredChunk::recallScore).reversed())
                .limit(topK)
                .toList();
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
