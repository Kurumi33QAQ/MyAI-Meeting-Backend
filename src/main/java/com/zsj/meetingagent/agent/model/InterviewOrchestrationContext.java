package com.zsj.meetingagent.agent.model;

import com.zsj.meetingagent.rag.vo.EvidenceResponse;
import com.zsj.meetingagent.resume.vo.ResumeResponse;

import java.util.List;

/**
 * 多 Agent 面试编排上下文。
 * 负责把简历、岗位、JD 和 RAG 证据集中传给各个角色，避免角色直接访问 Controller 或数据库。
 */
public record InterviewOrchestrationContext(
        String username,
        String sessionId,
        ResumeResponse resume,
        String jobTitle,
        String companyName,
        String jobDescription,
        int questionCount,
        List<EvidenceResponse> evidenceList,
        String aiSuggestion
) {
}
