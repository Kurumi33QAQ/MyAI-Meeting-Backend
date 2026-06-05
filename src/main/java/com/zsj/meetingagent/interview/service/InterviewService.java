package com.zsj.meetingagent.interview.service;

import com.zsj.meetingagent.interview.dto.CreateInterviewSessionRequest;
import com.zsj.meetingagent.interview.dto.SubmitInterviewAnswerRequest;
import com.zsj.meetingagent.interview.vo.InterviewAnswerResponse;
import com.zsj.meetingagent.interview.vo.InterviewReportResponse;
import com.zsj.meetingagent.interview.vo.InterviewSessionResponse;
import com.zsj.meetingagent.agent.vo.AgentStepResponse;

import java.util.List;

/**
 * 模拟面试服务接口。
 * 定义创建会话、生成题目、提交回答和查询报告等核心业务能力。
 */
public interface InterviewService {

    InterviewSessionResponse createSession(String username, CreateInterviewSessionRequest request);

    InterviewSessionResponse createSession(String username, String sessionId, CreateInterviewSessionRequest request);

    InterviewSessionResponse generateQuestions(String username, String sessionId);

    InterviewAnswerResponse submitAnswer(String username, String sessionId, SubmitInterviewAnswerRequest request);

    InterviewSessionResponse getSession(String username, String sessionId);

    InterviewReportResponse getReport(String username, String sessionId);

    List<AgentStepResponse> listAgentTraces(String username, String sessionId);
}
