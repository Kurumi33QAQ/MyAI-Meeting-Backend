package com.zsj.meetingagent.rag.vo;

/**
 * RAG 证据片段响应。
 * evidenceId 用于后续回答引用和 evaluation 统计 citation_accuracy。
 */
public record EvidenceResponse(
        String evidenceId,
        String documentId,
        String sourceId,
        String documentType,
        String sectionName,
        String content,
        String summary,
        String tags,
        double recallScore,
        double rerankScore
) {
}
