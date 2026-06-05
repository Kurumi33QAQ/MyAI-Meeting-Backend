package com.zsj.meetingagent.rag.service;

import com.zsj.meetingagent.rag.dto.RetrieveEvidenceRequest;
import com.zsj.meetingagent.rag.vo.EvidenceResponse;
import com.zsj.meetingagent.rag.vo.RetrieveEvidenceResponse;

import java.util.List;

/**
 * RAG 检索服务。
 * 对外提供证据召回能力，也给面试 Agent 生成题目时复用。
 */
public interface RetrievalService {

    RetrieveEvidenceResponse retrieve(String username, RetrieveEvidenceRequest request);

    List<EvidenceResponse> retrieveForInterview(
            String username,
            String resumeId,
            String sessionId,
            String jobTitle,
            String companyName,
            String jobDescription
    );
}
