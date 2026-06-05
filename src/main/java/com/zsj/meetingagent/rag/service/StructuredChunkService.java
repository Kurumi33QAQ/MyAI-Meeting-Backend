package com.zsj.meetingagent.rag.service;

import com.zsj.meetingagent.rag.model.StructuredChunk;

import java.util.List;

/**
 * 结构化 chunk 服务。
 * 根据简历、岗位 JD、题库等不同文档类型采用不同切分策略。
 */
public interface StructuredChunkService {

    List<StructuredChunk> chunkResume(String resumeText);

    List<StructuredChunk> chunkJobDescription(String jobTitle, String companyName, String jobDescription);

    List<StructuredChunk> chunkInterviewQuestionBank(String question, String referenceAnswer, String evaluationPoints);
}
