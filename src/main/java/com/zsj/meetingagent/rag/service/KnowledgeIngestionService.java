package com.zsj.meetingagent.rag.service;

/**
 * 知识入库服务。
 * 负责把简历、岗位 JD、面试题等业务数据转换为知识库文档和 chunk。
 */
public interface KnowledgeIngestionService {

    void ingestResume(String username, String resumeId);

    void ingestJobDescription(String username, String sessionId, String jobTitle, String companyName, String jobDescription);
}
