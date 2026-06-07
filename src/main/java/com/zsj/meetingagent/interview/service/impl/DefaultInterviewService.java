package com.zsj.meetingagent.interview.service.impl;

import com.zsj.meetingagent.ai.dto.AiChatRequest;
import com.zsj.meetingagent.ai.config.AiModelProperties;
import com.zsj.meetingagent.ai.service.AiChatService;
import com.zsj.meetingagent.agent.model.InterviewAgentQuestion;
import com.zsj.meetingagent.agent.model.InterviewOrchestrationContext;
import com.zsj.meetingagent.agent.model.InterviewOrchestrationResult;
import com.zsj.meetingagent.agent.orchestrator.InterviewOrchestrator;
import com.zsj.meetingagent.common.exception.BusinessException;
import com.zsj.meetingagent.common.vo.PageResponse;
import com.zsj.meetingagent.interview.dto.CreateInterviewSessionRequest;
import com.zsj.meetingagent.interview.dto.SubmitInterviewAnswerRequest;
import com.zsj.meetingagent.interview.adaptive.InterviewProgressDecision;
import com.zsj.meetingagent.interview.adaptive.InterviewProgressPolicy;
import com.zsj.meetingagent.interview.config.InterviewProperties;
import com.zsj.meetingagent.interview.entity.InterviewFollowUpRound;
import com.zsj.meetingagent.interview.entity.InterviewQuestionSnapshotDocument;
import com.zsj.meetingagent.interview.entity.InterviewRecord;
import com.zsj.meetingagent.interview.entity.InterviewSessionDocument;
import com.zsj.meetingagent.interview.enums.InterviewSessionStatus;
import com.zsj.meetingagent.interview.followup.FollowUpQuestionGenerator;
import com.zsj.meetingagent.interview.followup.FollowUpQuestionRequest;
import com.zsj.meetingagent.interview.mapper.InterviewRecordMapper;
import com.zsj.meetingagent.interview.prompt.InterviewPromptBuilder;
import com.zsj.meetingagent.interview.repository.InterviewQuestionSnapshotRepository;
import com.zsj.meetingagent.interview.repository.InterviewSessionRepository;
import com.zsj.meetingagent.interview.rule.FollowUpDecision;
import com.zsj.meetingagent.interview.rule.FollowUpDecisionService;
import com.zsj.meetingagent.interview.rule.FollowUpRuleContext;
import com.zsj.meetingagent.interview.runtime.InterviewRuntimeService;
import com.zsj.meetingagent.interview.runtime.InterviewRuntimeState;
import com.zsj.meetingagent.interview.scoring.AnswerScoreResult;
import com.zsj.meetingagent.interview.scoring.AnswerScoringService;
import com.zsj.meetingagent.interview.service.InterviewService;
import com.zsj.meetingagent.interview.support.InterviewFeedbackFormatter;
import com.zsj.meetingagent.interview.vo.InterviewAnswerResponse;
import com.zsj.meetingagent.interview.vo.InterviewConversationResponse;
import com.zsj.meetingagent.interview.vo.InterviewFollowUpResponse;
import com.zsj.meetingagent.interview.vo.InterviewQuestionResponse;
import com.zsj.meetingagent.interview.vo.InterviewReportResponse;
import com.zsj.meetingagent.interview.vo.InterviewRecordResponse;
import com.zsj.meetingagent.interview.vo.InterviewRuntimeStateResponse;
import com.zsj.meetingagent.interview.vo.InterviewSessionResponse;
import com.zsj.meetingagent.knowledge.model.JobIntelligenceReport;
import com.zsj.meetingagent.knowledge.model.JobIntelligenceSource;
import com.zsj.meetingagent.knowledge.service.JobIntelligenceSearchService;
import com.zsj.meetingagent.rag.service.KnowledgeIngestionService;
import com.zsj.meetingagent.rag.service.RetrievalService;
import com.zsj.meetingagent.rag.vo.EvidenceResponse;
import com.zsj.meetingagent.resume.service.ResumeService;
import com.zsj.meetingagent.resume.vo.ResumeResponse;
import com.zsj.meetingagent.agent.vo.AgentStepResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 默认模拟面试服务实现。
 * 使用状态机控制面试流程，用 MongoDB 保存题目和运行快照，用 MySQL 保存结构化面试记录。
 */
@Service
public class DefaultInterviewService implements InterviewService {

    private static final int NOT_DELETED = 0;
    private static final String INTERVIEW_SYSTEM_PROMPT = """
            你是一个中文友好的技术模拟面试官。
            你的反馈要具体、可执行，并优先围绕候选人的真实项目经历和用户实际填写的岗位要求。
            如果用户没有填写岗位信息，不得擅自假设目标岗位或技术方向。
            """;

    private final ResumeService resumeService;
    private final AiChatService aiChatService;
    private final InterviewPromptBuilder promptBuilder;
    private final InterviewRecordMapper interviewRecordMapper;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionSnapshotRepository questionRepository;
    private final InterviewRuntimeService runtimeService;
    private final KnowledgeIngestionService knowledgeIngestionService;
    private final RetrievalService retrievalService;
    private final AiModelProperties aiModelProperties;
    private final InterviewOrchestrator interviewOrchestrator;
    private final FollowUpDecisionService followUpDecisionService;
    private final JobIntelligenceSearchService jobIntelligenceSearchService;
    private final AnswerScoringService answerScoringService;
    private final InterviewProgressPolicy interviewProgressPolicy;
    private final FollowUpQuestionGenerator followUpQuestionGenerator;
    private final InterviewProperties interviewProperties;

    public DefaultInterviewService(
            ResumeService resumeService,
            AiChatService aiChatService,
            AiModelProperties aiModelProperties,
            InterviewPromptBuilder promptBuilder,
            InterviewRecordMapper interviewRecordMapper,
            InterviewSessionRepository sessionRepository,
            InterviewQuestionSnapshotRepository questionRepository,
            InterviewRuntimeService runtimeService,
            KnowledgeIngestionService knowledgeIngestionService,
            RetrievalService retrievalService,
            InterviewOrchestrator interviewOrchestrator,
            FollowUpDecisionService followUpDecisionService,
            JobIntelligenceSearchService jobIntelligenceSearchService,
            AnswerScoringService answerScoringService,
            InterviewProgressPolicy interviewProgressPolicy,
            FollowUpQuestionGenerator followUpQuestionGenerator,
            InterviewProperties interviewProperties
    ) {
        this.resumeService = resumeService;
        this.aiChatService = aiChatService;
        this.aiModelProperties = aiModelProperties;
        this.promptBuilder = promptBuilder;
        this.interviewRecordMapper = interviewRecordMapper;
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.runtimeService = runtimeService;
        this.knowledgeIngestionService = knowledgeIngestionService;
        this.retrievalService = retrievalService;
        this.interviewOrchestrator = interviewOrchestrator;
        this.followUpDecisionService = followUpDecisionService;
        this.jobIntelligenceSearchService = jobIntelligenceSearchService;
        this.answerScoringService = answerScoringService;
        this.interviewProgressPolicy = interviewProgressPolicy;
        this.followUpQuestionGenerator = followUpQuestionGenerator;
        this.interviewProperties = interviewProperties;
    }

    @Override
    public InterviewSessionResponse createSession(String username, CreateInterviewSessionRequest request) {
        return createSession(username, UUID.randomUUID().toString(), request);
    }

    @Override
    public InterviewSessionResponse createSession(String username, String sessionId, CreateInterviewSessionRequest request) {
        validateCreateSessionRequest(request);
        if (!StringUtils.hasText(sessionId)) {
            throw new BusinessException("I0401", "面试会话不能为空");
        }
        ResumeResponse resume = resumeService.getResume(username, request.resumeId());
        boolean adaptiveQuestionCount = request.questionCount() == null;
        int questionCount = adaptiveQuestionCount
                ? interviewProgressPolicy.initialQuestionCount()
                : normalizeQuestionCount(request.questionCount());
        int maxQuestionCount = adaptiveQuestionCount
                ? interviewProgressPolicy.questionPoolSize()
                : questionCount;
        String jobTitle = normalizeOptional(request.jobTitle());
        String companyName = normalizeOptional(request.companyName());
        String jobDescription = normalizeOptional(request.jobDescription());
        Instant now = Instant.now();

        InterviewSessionDocument session = new InterviewSessionDocument();
        session.setSessionId(sessionId.trim());
        session.setUsername(username);
        session.setResumeId(resume.resumeId());
        session.setJobTitle(jobTitle);
        session.setCompanyName(companyName);
        session.setJobDescription(jobDescription);
        session.setStatus(InterviewSessionStatus.CREATED);
        session.setQuestionCount(questionCount);
        session.setAdaptiveQuestionCount(adaptiveQuestionCount);
        session.setMaxQuestionCount(maxQuestionCount);
        session.setAnsweredCount(0);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        sessionRepository.save(session);

        interviewRecordMapper.insert(new InterviewRecord(
                null,
                sessionId.trim(),
                username,
                resume.resumeId(),
                jobTitle,
                InterviewSessionStatus.CREATED.name(),
                questionCount,
                0,
                null,
                null,
                now,
                now,
                NOT_DELETED
        ));
        /*
         * 阶段 7 开始，创建面试会话时顺手把简历和 JD 入知识库。
         * 后续生成题目、回答自检和 evaluation 都可以复用同一批 chunk。
         */
        knowledgeIngestionService.ingestResume(username, resume.resumeId());
        knowledgeIngestionService.ingestJobDescription(
                username,
                sessionId.trim(),
                jobTitle,
                companyName,
                jobDescription
        );
        saveRuntime(
                session,
                "CREATE_SESSION",
                hasJobContext(jobTitle, companyName, jobDescription)
                        ? "创建模拟面试会话，用户提供了岗位上下文。"
                        : "创建模拟面试会话，用户未填写岗位信息，本次仅根据简历出题。"
        );
        return toSessionResponse(session, List.of());
    }

    @Override
    public InterviewSessionResponse generateQuestions(String username, String sessionId) {
        InterviewSessionDocument session = findSession(username, sessionId);
        if (session.getStatus() == InterviewSessionStatus.COMPLETED) {
            throw new BusinessException("I0402", "面试已完成，不能重新生成题目");
        }
        List<InterviewQuestionSnapshotDocument> existing = listQuestionDocuments(username, sessionId);
        if (!existing.isEmpty()) {
            runtimeService.recordSnapshot(session, existing, "GENERATE_QUESTIONS_REUSE", "复用已生成的面试题，不重复生成");
            return toSessionResponse(session, visibleQuestions(session, existing));
        }

        ResumeResponse resume = resumeService.getResume(username, session.getResumeId());
        JobIntelligenceReport jobIntelligence = jobIntelligenceSearchService.search(
                session.getJobTitle(),
                session.getCompanyName(),
                session.getJobDescription()
        );
        List<EvidenceResponse> evidenceList = new ArrayList<>(retrievalService.retrieveForInterview(
                username,
                session.getResumeId(),
                session.getSessionId(),
                session.getJobTitle(),
                session.getCompanyName(),
                session.getJobDescription()
        ));
        evidenceList.addAll(toJobIntelligenceEvidence(jobIntelligence));
        /*
         * AI 调用前先做 RAG 检索，把简历/JD 证据塞进 Prompt。
         * AI 建议不会直接覆盖后端题目结构，而是作为多 Agent 编排上下文的一部分。
         */
        int generatedQuestionCount = session.isAdaptiveQuestionCount()
                ? session.getMaxQuestionCount()
                : session.getQuestionCount();
        String aiSuggestion = aiChatService.chat(new AiChatRequest(
                promptBuilder.buildQuestionPrompt(
                        resume,
                        session.getJobTitle(),
                        session.getCompanyName(),
                        session.getJobDescription(),
                        generatedQuestionCount,
                        evidenceList
                ),
                null,
                defaultModel(),
                INTERVIEW_SYSTEM_PROMPT,
                null
        )).answer();
        saveRuntime(session, "GENERATE_QUESTIONS_AI_CONTEXT", shorten(aiSuggestion, 500));
        saveRuntime(session, "RAG_EVIDENCE", buildEvidenceRuntimeSummary(evidenceList));
        saveRuntime(session, "JOB_INTELLIGENCE_SEARCH", jobIntelligence.message());

        InterviewOrchestrationResult orchestrationResult = interviewOrchestrator.designQuestions(new InterviewOrchestrationContext(
                username,
                session.getSessionId(),
                resume,
                session.getJobTitle(),
                session.getCompanyName(),
                session.getJobDescription(),
                generatedQuestionCount,
                evidenceList,
                aiSuggestion
        ));
        saveRuntime(session, "MULTI_AGENT_ORCHESTRATION", orchestrationResult.traceSummary());

        List<InterviewQuestionSnapshotDocument> questions = buildQuestions(session, orchestrationResult);
        questionRepository.saveAll(questions);

        session.setStatus(InterviewSessionStatus.QUESTION_GENERATED);
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);
        updateRecord(session);
        saveRuntime(session, "GENERATE_QUESTIONS", "生成面试题数量：" + questions.size());
        return toSessionResponse(session, visibleQuestions(session, questions));
    }

    @Override
    public InterviewAnswerResponse submitAnswer(String username, String sessionId, SubmitInterviewAnswerRequest request) {
        validateSubmitAnswerRequest(request);
        InterviewSessionDocument session = findSession(username, sessionId);
        if (session.getStatus() == InterviewSessionStatus.CREATED) {
            throw new BusinessException("I0403", "请先生成面试题");
        }
        if (session.getStatus() == InterviewSessionStatus.COMPLETED) {
            throw new BusinessException("I0404", "面试已完成，不能继续提交回答");
        }
        if (isFollowUpQuestionId(request.questionId())) {
            return submitFollowUpAnswer(username, session, request);
        }
        InterviewQuestionSnapshotDocument question = questionRepository
                .findByQuestionIdAndSessionIdAndUsername(request.questionId(), sessionId, username)
                .orElseThrow(() -> new BusinessException("I0405", "面试题不存在或无权访问"));
        if (StringUtils.hasText(question.getUserAnswer())) {
            throw new BusinessException("I0406", "该题已经提交过回答");
        }

        AnswerScoreResult scoreResult = answerScoringService.score(
                question.getQuestion(),
                question.getEvaluationPoints(),
                request.answer()
        );
        int score = scoreResult.score();
        String aiFeedback = aiChatService.chat(new AiChatRequest(
                promptBuilder.buildAnswerReviewPrompt(question.getQuestion(), request.answer(), question.getEvaluationPoints()),
                null,
                defaultModel(),
                INTERVIEW_SYSTEM_PROMPT,
                null
        )).answer();
        var reviewOutput = interviewOrchestrator.reviewAnswer(
                username,
                sessionId,
                question.getQuestion(),
                request.answer(),
                score,
                aiFeedback
        );
        FollowUpDecision followUpDecision = followUpDecisionService.decide(new FollowUpRuleContext(
                sessionId,
                question.getQuestionId(),
                question.getQuestion(),
                request.answer(),
                score,
                aiFeedback,
                question.getEvaluationPoints(),
                question.getFollowUpDirection(),
                question.getEvidenceIds(),
                session.getStatus(),
                existingFollowUpCount(question),
                interviewProperties.getMaxFollowUpCount()
        ));
        ResumeResponse resumeContext = resumeService.getResume(username, session.getResumeId());
        String followUpQuestion = followUpDecision.shouldFollowUp()
                ? followUpQuestionGenerator.generate(new FollowUpQuestionRequest(
                        sessionId,
                        resumeContext.summary(),
                        session.getJobTitle(),
                        session.getCompanyName(),
                        session.getJobDescription(),
                        question.getQuestion(),
                        request.answer(),
                        question.getEvaluationPoints(),
                        question.getFollowUpDirection(),
                        aiFeedback,
                        followUpDecision.followUpQuestion()
                ))
                : null;
        InterviewFollowUpRound createdFollowUp = null;
        if (StringUtils.hasText(followUpQuestion)) {
            createdFollowUp = appendFollowUpRound(question, followUpQuestion, followUpDecision.traceSummary());
        }
        syncLegacyFollowUpFields(question);

        question.setUserAnswer(request.answer().trim());
        question.setScore(score);
        question.setFeedback(InterviewFeedbackFormatter.formatMainFeedback(
                scoreResult.feedback(),
                aiFeedback,
                reviewOutput.summary()
        ));
        question.setFollowUpRuleTrace(followUpDecision.traceSummary());
        question.setAnsweredAt(Instant.now());
        questionRepository.save(question);

        int answeredCount = (int) listQuestionDocuments(username, sessionId).stream()
                .filter(item -> StringUtils.hasText(item.getUserAnswer()))
                .count();
        session.setAnsweredCount(answeredCount);
        int averageScore = calculateAverageScore(username, sessionId);
        InterviewProgressDecision progressDecision = interviewProgressPolicy.decide(
                session.isAdaptiveQuestionCount(),
                answeredCount,
                averageScore,
                session.getQuestionCount(),
                effectiveMaxQuestionCount(session),
                StringUtils.hasText(followUpQuestion)
        );
        session.setQuestionCount(progressDecision.targetQuestionCount());
        session.setStatus(progressDecision.complete() ? InterviewSessionStatus.COMPLETED : InterviewSessionStatus.ANSWERING);
        if (progressDecision.complete()) {
            session.setTotalScore(averageScore);
            session.setReportSummary(buildReportSummary(session.getTotalScore(), answeredCount, session.getQuestionCount()));
            session.setCompletedAt(Instant.now());
        }
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);
        updateRecord(session);
        saveRuntime(session, "SUBMIT_ANSWER", "提交第 " + question.getQuestionOrder() + " 题，评分：" + score);
        saveRuntime(session, "FOLLOW_UP_RULE_TRACE", followUpDecision.traceSummary());
        saveRuntime(session, "ADAPTIVE_PROGRESS", progressDecision.reason());

        return new InterviewAnswerResponse(
                sessionId,
                question.getQuestionId(),
                score,
                question.getFeedback(),
                question.getFollowUpQuestion(),
                createdFollowUp == null ? null : followUpQuestionId(question.getQuestionId(), createdFollowUp.getRound()),
                createdFollowUp != null,
                createdFollowUp == null ? 0 : createdFollowUp.getRound(),
                question.getFollowUpRuleTrace(),
                session.getStatus(),
                session.getAnsweredCount(),
                session.getQuestionCount()
        );
    }

    private InterviewAnswerResponse submitFollowUpAnswer(
            String username,
            InterviewSessionDocument session,
            SubmitInterviewAnswerRequest request
    ) {
        String parentQuestionId = parentQuestionId(request.questionId());
        InterviewQuestionSnapshotDocument question = questionRepository
                .findByQuestionIdAndSessionIdAndUsername(parentQuestionId, session.getSessionId(), username)
                .orElseThrow(() -> new BusinessException("I0405", "追问所属的主问题不存在或无权访问"));
        ensureFollowUpRounds(question);
        int roundNumber = followUpRoundNumber(request.questionId());
        InterviewFollowUpRound currentFollowUp = findFollowUpRound(question, roundNumber);
        if (currentFollowUp == null || !StringUtils.hasText(currentFollowUp.getQuestion())) {
            throw new BusinessException("I0408", "当前主问题没有待回答的追问");
        }
        if (StringUtils.hasText(currentFollowUp.getAnswer())) {
            throw new BusinessException("I0409", "该追问已经提交过回答");
        }

        AnswerScoreResult scoreResult = answerScoringService.score(
                currentFollowUp.getQuestion(),
                question.getEvaluationPoints(),
                request.answer()
        );
        String aiFeedback = aiChatService.chat(new AiChatRequest(
                promptBuilder.buildAnswerReviewPrompt(
                        currentFollowUp.getQuestion(),
                        request.answer(),
                        question.getEvaluationPoints()
                ),
                null,
                defaultModel(),
                INTERVIEW_SYSTEM_PROMPT,
                null
        )).answer();
        currentFollowUp.setAnswer(request.answer().trim());
        currentFollowUp.setScore(scoreResult.score());
        currentFollowUp.setFeedback(InterviewFeedbackFormatter.formatFollowUpFeedback(
                scoreResult.feedback(),
                aiFeedback
        ));
        currentFollowUp.setAnsweredAt(Instant.now());

        FollowUpDecision nextDecision = followUpDecisionService.decide(new FollowUpRuleContext(
                session.getSessionId(),
                question.getQuestionId(),
                currentFollowUp.getQuestion(),
                request.answer(),
                scoreResult.score(),
                aiFeedback,
                question.getEvaluationPoints(),
                question.getFollowUpDirection(),
                question.getEvidenceIds(),
                session.getStatus(),
                existingFollowUpCount(question),
                interviewProperties.getMaxFollowUpCount()
        ));
        ResumeResponse resumeContext = resumeService.getResume(username, session.getResumeId());
        String nextFollowUpQuestion = nextDecision.shouldFollowUp()
                ? followUpQuestionGenerator.generate(new FollowUpQuestionRequest(
                        session.getSessionId(),
                        resumeContext.summary(),
                        session.getJobTitle(),
                        session.getCompanyName(),
                        session.getJobDescription(),
                        currentFollowUp.getQuestion(),
                        request.answer(),
                        question.getEvaluationPoints(),
                        question.getFollowUpDirection(),
                        aiFeedback,
                        nextDecision.followUpQuestion(),
                        buildFollowUpHistory(question)
                ))
                : null;
        InterviewFollowUpRound nextFollowUp = null;
        if (StringUtils.hasText(nextFollowUpQuestion)) {
            nextFollowUp = appendFollowUpRound(question, nextFollowUpQuestion, nextDecision.traceSummary());
        }
        syncLegacyFollowUpFields(question);
        questionRepository.save(question);

        int averageScore = calculateAverageScore(username, session.getSessionId());
        InterviewProgressDecision progressDecision = interviewProgressPolicy.decide(
                session.isAdaptiveQuestionCount(),
                session.getAnsweredCount(),
                averageScore,
                session.getQuestionCount(),
                effectiveMaxQuestionCount(session),
                nextFollowUp != null
        );
        session.setQuestionCount(progressDecision.targetQuestionCount());
        session.setStatus(progressDecision.complete() ? InterviewSessionStatus.COMPLETED : InterviewSessionStatus.ANSWERING);
        if (progressDecision.complete()) {
            session.setTotalScore(averageScore);
            session.setReportSummary(buildReportSummary(
                    session.getTotalScore(),
                    session.getAnsweredCount(),
                    session.getQuestionCount()
            ));
            session.setCompletedAt(Instant.now());
        }
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);
        updateRecord(session);
        saveRuntime(session, "SUBMIT_FOLLOW_UP", "提交第 " + question.getQuestionOrder() + " 题第 " + roundNumber + " 轮追问，评分：" + scoreResult.score());
        saveRuntime(session, "FOLLOW_UP_RULE_TRACE", nextDecision.traceSummary());
        saveRuntime(session, "ADAPTIVE_PROGRESS", progressDecision.reason());

        return new InterviewAnswerResponse(
                session.getSessionId(),
                request.questionId(),
                scoreResult.score(),
                currentFollowUp.getFeedback(),
                nextFollowUp == null ? null : nextFollowUp.getQuestion(),
                nextFollowUp == null ? null : followUpQuestionId(question.getQuestionId(), nextFollowUp.getRound()),
                nextFollowUp != null,
                nextFollowUp == null ? 0 : nextFollowUp.getRound(),
                nextDecision.traceSummary(),
                session.getStatus(),
                session.getAnsweredCount(),
                session.getQuestionCount()
        );
    }

    @Override
    public InterviewSessionResponse getSession(String username, String sessionId) {
        InterviewSessionDocument session = findSession(username, sessionId);
        runtimeService.recover(username, sessionId);
        return toSessionResponse(session, visibleQuestions(session, listQuestionDocuments(username, sessionId)));
    }

    @Override
    public InterviewReportResponse getReport(String username, String sessionId) {
        InterviewSessionDocument session = findSession(username, sessionId);
        List<InterviewQuestionResponse> questions = visibleQuestions(session, listQuestionDocuments(username, sessionId))
                .stream()
                .map(this::toQuestionResponse)
                .toList();
        return new InterviewReportResponse(
                session.getSessionId(),
                session.getStatus(),
                session.getTotalScore(),
                session.getAnsweredCount(),
                session.getQuestionCount(),
                session.getReportSummary(),
                questions
        );
    }

    @Override
    public List<AgentStepResponse> listAgentTraces(String username, String sessionId) {
        findSession(username, sessionId);
        return interviewOrchestrator.listTraces(username, sessionId);
    }

    @Override
    public InterviewRuntimeStateResponse getRuntimeState(String username, String sessionId) {
        return toRuntimeStateResponse(runtimeService.recover(username, sessionId));
    }

    @Override
    public InterviewRuntimeStateResponse recoverRuntimeState(String username, String sessionId) {
        return toRuntimeStateResponse(runtimeService.recover(username, sessionId));
    }

    @Override
    public PageResponse<InterviewRecordResponse> pageInterviewRecords(
            String username,
            int current,
            int size,
            String sessionId,
            Integer minScore,
            Integer maxScore,
            String interviewDirection
    ) {
        int normalizedCurrent = normalizePageCurrent(current);
        int normalizedSize = normalizePageSize(size);
        int offset = (normalizedCurrent - 1) * normalizedSize;
        /*
         * 历史面试列表使用 MySQL interview_record。
         * MongoDB 保存详细问答快照，MySQL 保存列表页需要快速扫描的结构化摘要。
         */
        List<InterviewRecord> records = interviewRecordMapper.findPageByUsername(
                username,
                blankToNull(sessionId),
                null,
                false,
                null,
                blankToNull(interviewDirection),
                minScore,
                maxScore,
                offset,
                normalizedSize
        );
        long total = interviewRecordMapper.countByUsername(
                username,
                blankToNull(sessionId),
                null,
                false,
                null,
                blankToNull(interviewDirection),
                minScore,
                maxScore
        );
        return PageResponse.of(records.stream().map(this::toRecordResponse).toList(), total, normalizedCurrent, normalizedSize);
    }

    @Override
    public PageResponse<InterviewConversationResponse> pageInterviewConversations(
            String username,
            int current,
            int size,
            String status,
            String keyword
    ) {
        int normalizedCurrent = normalizePageCurrent(current);
        int normalizedSize = normalizePageSize(size);
        int offset = (normalizedCurrent - 1) * normalizedSize;
        String storedStatus = normalizeConversationStatus(status);
        boolean activeOnly = isActiveConversationStatus(status);
        List<InterviewRecord> records = interviewRecordMapper.findPageByUsername(
                username,
                null,
                storedStatus,
                activeOnly,
                blankToNull(keyword),
                null,
                null,
                null,
                offset,
                normalizedSize
        );
        long total = interviewRecordMapper.countByUsername(
                username,
                null,
                storedStatus,
                activeOnly,
                blankToNull(keyword),
                null,
                null,
                null
        );
        return PageResponse.of(records.stream().map(this::toConversationResponse).toList(), total, normalizedCurrent, normalizedSize);
    }

    private InterviewSessionDocument findSession(String username, String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new BusinessException("I0401", "面试会话不能为空");
        }
        return sessionRepository.findBySessionIdAndUsername(sessionId, username)
                .orElseThrow(() -> new BusinessException("I0404", "面试会话不存在或无权访问"));
    }

    private void validateCreateSessionRequest(CreateInterviewSessionRequest request) {
        /*
         * 新接口会先经过 @Valid，旧前端兼容接口为了适配 Map 入参可能绕过参数校验。
         * 服务层再做一次关键字段兜底，可以保证所有入口都返回中文业务错误，而不是空指针。
         */
        if (request == null || !StringUtils.hasText(request.resumeId())) {
            throw new BusinessException("I0407", "简历不能为空");
        }
    }

    private List<EvidenceResponse> toJobIntelligenceEvidence(JobIntelligenceReport report) {
        if (report == null || !report.successful() || report.sources() == null) {
            return List.of();
        }
        return report.sources().stream()
                .map(this::toJobIntelligenceEvidence)
                .toList();
    }

    private EvidenceResponse toJobIntelligenceEvidence(JobIntelligenceSource source) {
        String evidenceId = "web-" + Integer.toUnsignedString(source.url().hashCode(), 16);
        return new EvidenceResponse(
                evidenceId,
                "job-intelligence-search",
                source.url(),
                "JOB_MARKET_INTELLIGENCE",
                source.title(),
                source.content() + "\n来源：" + source.url(),
                shorten(source.content(), 180),
                "web-search,job-intelligence",
                source.score(),
                Math.min(1.0, source.score() + 0.08)
        );
    }

    private void validateSubmitAnswerRequest(SubmitInterviewAnswerRequest request) {
        if (request == null || !StringUtils.hasText(request.questionId())) {
            throw new BusinessException("I0409", "面试题不能为空");
        }
        if (!StringUtils.hasText(request.answer())) {
            throw new BusinessException("I0410", "回答不能为空");
        }
    }

    private List<InterviewQuestionSnapshotDocument> buildQuestions(InterviewSessionDocument session, InterviewOrchestrationResult orchestrationResult) {
        List<InterviewQuestionSnapshotDocument> questions = new ArrayList<>();
        Instant now = Instant.now();
        List<InterviewAgentQuestion> questionPlans = orchestrationResult.questions();
        for (int index = 0; index < questionPlans.size(); index++) {
            InterviewAgentQuestion plan = questionPlans.get(index);
            InterviewQuestionSnapshotDocument question = new InterviewQuestionSnapshotDocument();
            question.setQuestionId(UUID.randomUUID().toString());
            question.setSessionId(session.getSessionId());
            question.setUsername(session.getUsername());
            question.setQuestionOrder(index + 1);
            question.setQuestion(plan.question());
            question.setReferenceAnswer(plan.referenceAnswer());
            question.setEvaluationPoints(plan.evaluationPoints());
            question.setFollowUpDirection(plan.followUpDirection());
            question.setEvidenceIds(plan.evidenceIds());
            question.setEvidenceSummary(plan.evidenceSummary());
            question.setAgentRunId(orchestrationResult.runId());
            question.setCreatedAt(now);
            questions.add(question);
        }
        return questions;
    }

    private int normalizeQuestionCount(Integer questionCount) {
        if (questionCount == null) {
            return interviewProgressPolicy.initialQuestionCount();
        }
        return Math.max(1, Math.min(questionCount, interviewProgressPolicy.questionPoolSize()));
    }

    private int normalizePageCurrent(int current) {
        return Math.max(1, current);
    }

    private int normalizePageSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    private int existingFollowUpCount(InterviewQuestionSnapshotDocument question) {
        return followUpRounds(question).size();
    }

    private InterviewFollowUpRound appendFollowUpRound(
            InterviewQuestionSnapshotDocument question,
            String followUpQuestion,
            String ruleTrace
    ) {
        List<InterviewFollowUpRound> rounds = ensureFollowUpRounds(question);
        InterviewFollowUpRound round = new InterviewFollowUpRound();
        round.setRound(rounds.size() + 1);
        round.setQuestion(followUpQuestion.trim());
        round.setRuleTrace(ruleTrace);
        round.setCreatedAt(Instant.now());
        rounds.add(round);
        question.setFollowUps(rounds);
        return round;
    }

    private List<InterviewFollowUpRound> followUpRounds(InterviewQuestionSnapshotDocument question) {
        if (question.getFollowUps() != null && !question.getFollowUps().isEmpty()) {
            return question.getFollowUps().stream()
                    .filter(round -> StringUtils.hasText(round.getQuestion()))
                    .toList();
        }
        if (!StringUtils.hasText(question.getFollowUpQuestion())) {
            return List.of();
        }
        /*
         * 兼容阶段 10.2 之前已经落库的单字段追问数据。
         * MongoDB 老文档没有 followUps 列表时，运行时临时折叠成 F1，避免历史会话无法恢复。
         */
        InterviewFollowUpRound legacyRound = new InterviewFollowUpRound();
        legacyRound.setRound(1);
        legacyRound.setQuestion(question.getFollowUpQuestion());
        legacyRound.setAnswer(question.getFollowUpAnswer());
        legacyRound.setScore(question.getFollowUpScore());
        legacyRound.setFeedback(question.getFollowUpFeedback());
        legacyRound.setRuleTrace(question.getFollowUpRuleTrace());
        legacyRound.setAnsweredAt(question.getFollowUpAnsweredAt());
        return List.of(legacyRound);
    }

    private List<InterviewFollowUpRound> ensureFollowUpRounds(InterviewQuestionSnapshotDocument question) {
        List<InterviewFollowUpRound> rounds = new ArrayList<>(followUpRounds(question));
        question.setFollowUps(rounds);
        return rounds;
    }

    private InterviewFollowUpRound findFollowUpRound(InterviewQuestionSnapshotDocument question, int roundNumber) {
        return followUpRounds(question).stream()
                .filter(round -> round.getRound() == roundNumber)
                .findFirst()
                .orElse(null);
    }

    private void syncLegacyFollowUpFields(InterviewQuestionSnapshotDocument question) {
        List<InterviewFollowUpRound> rounds = followUpRounds(question);
        if (rounds.isEmpty()) {
            question.setFollowUpQuestion(null);
            question.setFollowUpAnswer(null);
            question.setFollowUpScore(null);
            question.setFollowUpFeedback(null);
            question.setFollowUpAnsweredAt(null);
            return;
        }
        InterviewFollowUpRound firstRound = rounds.get(0);
        question.setFollowUpQuestion(firstRound.getQuestion());
        question.setFollowUpAnswer(firstRound.getAnswer());
        question.setFollowUpScore(firstRound.getScore());
        question.setFollowUpFeedback(firstRound.getFeedback());
        question.setFollowUpAnsweredAt(firstRound.getAnsweredAt());
    }

    private List<String> buildFollowUpHistory(InterviewQuestionSnapshotDocument question) {
        return followUpRounds(question).stream()
                .map(round -> "F" + round.getRound() + " 问：" + round.getQuestion()
                        + "；答：" + (StringUtils.hasText(round.getAnswer()) ? round.getAnswer() : "未回答"))
                .toList();
    }

    private int calculateAverageScore(String username, String sessionId) {
        List<InterviewQuestionSnapshotDocument> questions = listQuestionDocuments(username, sessionId);
        return (int) Math.round(questions.stream()
                .filter(item -> item.getScore() != null)
                .mapToInt(item -> {
                    List<InterviewFollowUpRound> answeredFollowUps = followUpRounds(item).stream()
                            .filter(round -> round.getScore() != null)
                            .toList();
                    if (answeredFollowUps.isEmpty()) {
                        return item.getScore();
                    }
                    double followUpAverage = answeredFollowUps.stream()
                            .mapToInt(InterviewFollowUpRound::getScore)
                            .average()
                            .orElse(item.getScore());
                    // 多轮追问用于校正主问题判断，但不应完全覆盖首次回答表现。
                    return (int) Math.round(item.getScore() * 0.65 + followUpAverage * 0.35);
                })
                .average()
                .orElse(0));
    }

    private List<InterviewQuestionSnapshotDocument> visibleQuestions(
            InterviewSessionDocument session,
            List<InterviewQuestionSnapshotDocument> questions
    ) {
        return questions.stream()
                .filter(question -> question.getQuestionOrder() <= session.getQuestionCount())
                .toList();
    }

    private int effectiveMaxQuestionCount(InterviewSessionDocument session) {
        return session.getMaxQuestionCount() > 0
                ? session.getMaxQuestionCount()
                : session.getQuestionCount();
    }

    private boolean isFollowUpQuestionId(String questionId) {
        return StringUtils.hasText(questionId) && questionId.matches(".+-F\\d+");
    }

    private String parentQuestionId(String followUpQuestionId) {
        int markerIndex = followUpQuestionId.lastIndexOf("-F");
        if (markerIndex <= 0) {
            throw new BusinessException("I0411", "追问题号格式不正确");
        }
        return followUpQuestionId.substring(0, markerIndex);
    }

    private int followUpRoundNumber(String followUpQuestionId) {
        int markerIndex = followUpQuestionId.lastIndexOf("-F");
        if (markerIndex <= 0 || markerIndex + 2 >= followUpQuestionId.length()) {
            throw new BusinessException("I0411", "追问题号格式不正确");
        }
        try {
            return Integer.parseInt(followUpQuestionId.substring(markerIndex + 2));
        } catch (NumberFormatException ex) {
            throw new BusinessException("I0411", "追问题号格式不正确");
        }
    }

    private String followUpQuestionId(String parentQuestionId, int round) {
        return parentQuestionId + "-F" + round;
    }

    private String buildReportSummary(Integer totalScore, int answeredCount, int questionCount) {
        if (totalScore == null) {
            return "面试尚未完成，暂无完整报告。";
        }
        String level = totalScore >= 85 ? "表现较好" : totalScore >= 70 ? "基础可用" : "仍需加强";
        return "本次模拟面试已完成 " + answeredCount + "/" + questionCount + " 题，总分 " + totalScore
                + "，整体评价：" + level + "。建议继续补充项目细节、技术取舍和量化结果。";
    }

    private void updateRecord(InterviewSessionDocument session) {
        interviewRecordMapper.updateProgress(new InterviewRecord(
                null,
                session.getSessionId(),
                session.getUsername(),
                session.getResumeId(),
                session.getJobTitle(),
                session.getStatus().name(),
                session.getQuestionCount(),
                session.getAnsweredCount(),
                session.getTotalScore(),
                session.getReportSummary(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                NOT_DELETED
        ));
    }

    private void saveRuntime(InterviewSessionDocument session, String stepType, String content) {
        /*
         * 每个关键步骤都同时刷新 Redis 热态和 MongoDB 冷快照。
         * 这样页面刷新优先走 Redis，Redis 丢失时仍能从 MongoDB 找回最新进度。
         */
        runtimeService.recordSnapshot(session, listQuestionDocuments(session.getUsername(), session.getSessionId()), stepType, content);
    }

    private List<InterviewQuestionSnapshotDocument> listQuestionDocuments(String username, String sessionId) {
        return questionRepository.findBySessionIdAndUsernameOrderByQuestionOrderAsc(sessionId, username);
    }

    private InterviewSessionResponse toSessionResponse(InterviewSessionDocument session, List<InterviewQuestionSnapshotDocument> questions) {
        return new InterviewSessionResponse(
                session.getSessionId(),
                session.getResumeId(),
                session.getJobTitle(),
                session.getCompanyName(),
                session.getJobDescription(),
                session.getStatus(),
                session.getQuestionCount(),
                session.getAnsweredCount(),
                session.getTotalScore(),
                session.getReportSummary(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                questions.stream().map(this::toQuestionResponse).toList()
        );
    }

    private InterviewQuestionResponse toQuestionResponse(InterviewQuestionSnapshotDocument question) {
        return new InterviewQuestionResponse(
                question.getQuestionId(),
                question.getQuestionOrder(),
                question.getQuestion(),
                question.getReferenceAnswer(),
                question.getEvaluationPoints(),
                question.getFollowUpDirection(),
                question.getEvidenceIds() == null ? List.of() : question.getEvidenceIds(),
                question.getEvidenceSummary(),
                question.getAgentRunId(),
                question.getUserAnswer(),
                question.getScore(),
                question.getFeedback(),
                question.getFollowUpQuestion(),
                question.getFollowUpAnswer(),
                question.getFollowUpScore(),
                question.getFollowUpFeedback(),
                question.getFollowUpRuleTrace(),
                followUpRounds(question).stream()
                        .map(round -> toFollowUpResponse(question.getQuestionId(), round))
                        .toList(),
                question.getCreatedAt(),
                question.getAnsweredAt(),
                question.getFollowUpAnsweredAt()
        );
    }

    private InterviewFollowUpResponse toFollowUpResponse(String parentQuestionId, InterviewFollowUpRound round) {
        return new InterviewFollowUpResponse(
                round.getRound(),
                followUpQuestionId(parentQuestionId, round.getRound()),
                round.getQuestion(),
                round.getAnswer(),
                round.getScore(),
                round.getFeedback(),
                round.getRuleTrace(),
                round.getCreatedAt(),
                round.getAnsweredAt()
        );
    }

    private InterviewRuntimeStateResponse toRuntimeStateResponse(InterviewRuntimeState state) {
        return new InterviewRuntimeStateResponse(
                state.sessionId(),
                state.status(),
                state.currentQuestionIndex(),
                state.answeredCount(),
                state.questionCount(),
                state.totalScore(),
                state.version(),
                state.restoreSource(),
                state.updatedAt()
        );
    }

    private InterviewRecordResponse toRecordResponse(InterviewRecord record) {
        Integer score = record.totalScore();
        return new InterviewRecordResponse(
                record.id(),
                0,
                record.sessionId(),
                80,
                score,
                record.status(),
                record.questionCount(),
                score,
                score,
                score,
                record.reportSummary(),
                record.jobTitle(),
                record.createdAt(),
                isCompleted(record.status()) ? record.updatedAt() : null,
                record.createdAt(),
                record.updatedAt()
        );
    }

    private InterviewConversationResponse toConversationResponse(InterviewRecord record) {
        String status = isCompleted(record.status()) ? "COMPLETED" : "IN_PROGRESS";
        String interviewType = StringUtils.hasText(record.jobTitle()) ? record.jobTitle() : "简历综合面试";
        return new InterviewConversationResponse(
                record.sessionId(),
                interviewType + "模拟面试",
                status,
                interviewType,
                record.resumeId(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    private boolean isCompleted(String status) {
        return InterviewSessionStatus.COMPLETED.name().equals(status);
    }

    private String normalizeConversationStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if ("ACTIVE".equals(normalized) || "IN_PROGRESS".equals(normalized)) {
            return null;
        }
        return normalized;
    }

    private boolean isActiveConversationStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String normalized = status.trim().toUpperCase();
        return "ACTIVE".equals(normalized) || "IN_PROGRESS".equals(normalized);
    }

    private String blankToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private boolean hasJobContext(String jobTitle, String companyName, String jobDescription) {
        return StringUtils.hasText(jobTitle)
                || StringUtils.hasText(companyName)
                || StringUtils.hasText(jobDescription);
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String defaultModel() {
        /*
         * 面试模块不能硬编码某个供应商模型名。
         * 真实调用时统一读取 MODEL_ID/app.ai.default-model，避免 DeepSeek 环境仍请求 gpt-4o-mini。
         */
        return aiModelProperties.getDefaultModel();
    }

    private String buildEvidenceRuntimeSummary(List<EvidenceResponse> evidenceList) {
        if (evidenceList.isEmpty()) {
            return "未召回到可用证据，后续阶段将接入低置信度拒答。";
        }
        return evidenceList.stream()
                .map(evidence -> "[%s] %s/%s score=%s：%s".formatted(
                        evidence.evidenceId(),
                        evidence.documentType(),
                        evidence.sectionName(),
                        evidence.rerankScore(),
                        shorten(evidence.content(), 120)
                ))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private String buildQuestionEvidenceSummary(List<EvidenceResponse> evidenceList) {
        if (evidenceList.isEmpty()) {
            return "本题暂未绑定证据，后续会通过低置信度拒答避免无依据出题。";
        }
        return evidenceList.stream()
                .limit(3)
                .map(evidence -> "%s：%s".formatted(evidence.sectionName(), shorten(evidence.summary(), 80)))
                .reduce((left, right) -> left + "；" + right)
                .orElse("");
    }

    private String shorten(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.substring(0, Math.min(trimmed.length(), maxLength));
    }
}
