package com.zsj.meetingagent.interview.service.impl;

import com.zsj.meetingagent.ai.dto.AiChatRequest;
import com.zsj.meetingagent.ai.config.AiModelProperties;
import com.zsj.meetingagent.ai.service.AiChatService;
import com.zsj.meetingagent.common.exception.BusinessException;
import com.zsj.meetingagent.interview.dto.CreateInterviewSessionRequest;
import com.zsj.meetingagent.interview.dto.SubmitInterviewAnswerRequest;
import com.zsj.meetingagent.interview.entity.InterviewQuestionSnapshotDocument;
import com.zsj.meetingagent.interview.entity.InterviewRecord;
import com.zsj.meetingagent.interview.entity.InterviewRuntimeSnapshotDocument;
import com.zsj.meetingagent.interview.entity.InterviewSessionDocument;
import com.zsj.meetingagent.interview.enums.InterviewSessionStatus;
import com.zsj.meetingagent.interview.mapper.InterviewRecordMapper;
import com.zsj.meetingagent.interview.prompt.InterviewPromptBuilder;
import com.zsj.meetingagent.interview.repository.InterviewQuestionSnapshotRepository;
import com.zsj.meetingagent.interview.repository.InterviewRuntimeSnapshotRepository;
import com.zsj.meetingagent.interview.repository.InterviewSessionRepository;
import com.zsj.meetingagent.interview.service.InterviewService;
import com.zsj.meetingagent.interview.vo.InterviewAnswerResponse;
import com.zsj.meetingagent.interview.vo.InterviewQuestionResponse;
import com.zsj.meetingagent.interview.vo.InterviewReportResponse;
import com.zsj.meetingagent.interview.vo.InterviewSessionResponse;
import com.zsj.meetingagent.rag.service.KnowledgeIngestionService;
import com.zsj.meetingagent.rag.service.RetrievalService;
import com.zsj.meetingagent.rag.vo.EvidenceResponse;
import com.zsj.meetingagent.resume.service.ResumeService;
import com.zsj.meetingagent.resume.vo.ResumeResponse;
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

    private static final int DEFAULT_QUESTION_COUNT = 5;
    private static final int MAX_QUESTION_COUNT = 10;
    private static final int NOT_DELETED = 0;
    private static final String INTERVIEW_SYSTEM_PROMPT = """
            你是一个中文友好的 Java 后端模拟面试官。
            你的反馈要具体、可执行，并优先围绕候选人的项目经历和 Java 后端基础能力。
            """;

    private final ResumeService resumeService;
    private final AiChatService aiChatService;
    private final InterviewPromptBuilder promptBuilder;
    private final InterviewRecordMapper interviewRecordMapper;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionSnapshotRepository questionRepository;
    private final InterviewRuntimeSnapshotRepository runtimeRepository;
    private final KnowledgeIngestionService knowledgeIngestionService;
    private final RetrievalService retrievalService;
    private final AiModelProperties aiModelProperties;

    public DefaultInterviewService(
            ResumeService resumeService,
            AiChatService aiChatService,
            AiModelProperties aiModelProperties,
            InterviewPromptBuilder promptBuilder,
            InterviewRecordMapper interviewRecordMapper,
            InterviewSessionRepository sessionRepository,
            InterviewQuestionSnapshotRepository questionRepository,
            InterviewRuntimeSnapshotRepository runtimeRepository,
            KnowledgeIngestionService knowledgeIngestionService,
            RetrievalService retrievalService
    ) {
        this.resumeService = resumeService;
        this.aiChatService = aiChatService;
        this.aiModelProperties = aiModelProperties;
        this.promptBuilder = promptBuilder;
        this.interviewRecordMapper = interviewRecordMapper;
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.runtimeRepository = runtimeRepository;
        this.knowledgeIngestionService = knowledgeIngestionService;
        this.retrievalService = retrievalService;
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
        int questionCount = normalizeQuestionCount(request.questionCount());
        Instant now = Instant.now();

        InterviewSessionDocument session = new InterviewSessionDocument();
        session.setSessionId(sessionId.trim());
        session.setUsername(username);
        session.setResumeId(resume.resumeId());
        session.setJobTitle(request.jobTitle().trim());
        session.setCompanyName(blankToDefault(request.companyName(), ""));
        session.setJobDescription(blankToDefault(request.jobDescription(), ""));
        session.setStatus(InterviewSessionStatus.CREATED);
        session.setQuestionCount(questionCount);
        session.setAnsweredCount(0);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        sessionRepository.save(session);

        interviewRecordMapper.insert(new InterviewRecord(
                null,
                sessionId.trim(),
                username,
                resume.resumeId(),
                request.jobTitle().trim(),
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
                request.jobTitle(),
                request.companyName(),
                request.jobDescription()
        );
        saveRuntime(sessionId, username, "CREATE_SESSION", "创建模拟面试会话，目标岗位：" + request.jobTitle().trim());
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
            return toSessionResponse(session, existing);
        }

        ResumeResponse resume = resumeService.getResume(username, session.getResumeId());
        List<EvidenceResponse> evidenceList = retrievalService.retrieveForInterview(
                username,
                session.getResumeId(),
                session.getSessionId(),
                session.getJobTitle(),
                session.getCompanyName(),
                session.getJobDescription()
        );
        /*
         * AI 调用前先做 RAG 检索，把简历/JD 证据塞进 Prompt。
         * 当前题目结构仍由后端规则生成，保证自动化测试和前端联调稳定。
         * AI 调用的结果先作为生成题目的参考说明保存到运行快照。
         * 当前题目结构仍由后端规则生成，保证自动化测试和前端联调稳定。
         */
        String aiSuggestion = aiChatService.chat(new AiChatRequest(
                promptBuilder.buildQuestionPrompt(
                        resume,
                        session.getJobTitle(),
                        session.getCompanyName(),
                        session.getJobDescription(),
                        session.getQuestionCount(),
                        evidenceList
                ),
                null,
                defaultModel(),
                INTERVIEW_SYSTEM_PROMPT,
                null
        )).answer();
        saveRuntime(sessionId, username, "GENERATE_QUESTIONS_AI_CONTEXT", shorten(aiSuggestion, 500));
        saveRuntime(sessionId, username, "RAG_EVIDENCE", buildEvidenceRuntimeSummary(evidenceList));

        List<InterviewQuestionSnapshotDocument> questions = buildQuestions(session, evidenceList);
        questionRepository.saveAll(questions);

        session.setStatus(InterviewSessionStatus.QUESTION_GENERATED);
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);
        updateRecord(session);
        saveRuntime(sessionId, username, "GENERATE_QUESTIONS", "生成面试题数量：" + questions.size());
        return toSessionResponse(session, questions);
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
        InterviewQuestionSnapshotDocument question = questionRepository
                .findByQuestionIdAndSessionIdAndUsername(request.questionId(), sessionId, username)
                .orElseThrow(() -> new BusinessException("I0405", "面试题不存在或无权访问"));
        if (StringUtils.hasText(question.getUserAnswer())) {
            throw new BusinessException("I0406", "该题已经提交过回答");
        }

        int score = scoreAnswer(request.answer());
        String heuristicFeedback = buildFeedback(score, request.answer());
        String aiFeedback = aiChatService.chat(new AiChatRequest(
                promptBuilder.buildAnswerReviewPrompt(question.getQuestion(), request.answer(), question.getEvaluationPoints()),
                null,
                defaultModel(),
                INTERVIEW_SYSTEM_PROMPT,
                null
        )).answer();

        question.setUserAnswer(request.answer().trim());
        question.setScore(score);
        question.setFeedback(heuristicFeedback + " AI 建议：" + shorten(aiFeedback, 220));
        question.setFollowUpQuestion(buildFollowUpQuestion(score, question));
        question.setAnsweredAt(Instant.now());
        questionRepository.save(question);

        int answeredCount = (int) listQuestionDocuments(username, sessionId).stream()
                .filter(item -> StringUtils.hasText(item.getUserAnswer()))
                .count();
        session.setAnsweredCount(answeredCount);
        session.setStatus(answeredCount >= session.getQuestionCount() ? InterviewSessionStatus.COMPLETED : InterviewSessionStatus.ANSWERING);
        if (session.getStatus() == InterviewSessionStatus.COMPLETED) {
            session.setTotalScore(calculateAverageScore(username, sessionId));
            session.setReportSummary(buildReportSummary(session.getTotalScore(), answeredCount, session.getQuestionCount()));
            session.setCompletedAt(Instant.now());
        }
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);
        updateRecord(session);
        saveRuntime(sessionId, username, "SUBMIT_ANSWER", "提交第 " + question.getQuestionOrder() + " 题，评分：" + score);

        return new InterviewAnswerResponse(
                sessionId,
                question.getQuestionId(),
                score,
                question.getFeedback(),
                question.getFollowUpQuestion(),
                session.getStatus(),
                session.getAnsweredCount(),
                session.getQuestionCount()
        );
    }

    @Override
    public InterviewSessionResponse getSession(String username, String sessionId) {
        InterviewSessionDocument session = findSession(username, sessionId);
        return toSessionResponse(session, listQuestionDocuments(username, sessionId));
    }

    @Override
    public InterviewReportResponse getReport(String username, String sessionId) {
        InterviewSessionDocument session = findSession(username, sessionId);
        List<InterviewQuestionResponse> questions = listQuestionDocuments(username, sessionId)
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
        if (!StringUtils.hasText(request.jobTitle())) {
            throw new BusinessException("I0408", "岗位名称不能为空");
        }
    }

    private void validateSubmitAnswerRequest(SubmitInterviewAnswerRequest request) {
        if (request == null || !StringUtils.hasText(request.questionId())) {
            throw new BusinessException("I0409", "面试题不能为空");
        }
        if (!StringUtils.hasText(request.answer())) {
            throw new BusinessException("I0410", "回答不能为空");
        }
    }

    private List<InterviewQuestionSnapshotDocument> buildQuestions(InterviewSessionDocument session, List<EvidenceResponse> evidenceList) {
        List<String> templates = List.of(
                "请结合你的简历，介绍一个最能体现 %s 能力的项目。",
                "你在项目中如何使用 Spring Boot、MySQL 或 Redis 解决实际问题？",
                "如果让你优化一个接口响应时间，你会从哪些后端指标和代码路径入手？",
                "请讲一次你定位 Bug 或排查线上问题的过程。",
                "请说明你如何理解数据库事务、索引和慢查询优化。",
                "如果团队要求你设计一个可扩展的 AI 面试模块，你会如何拆分后端服务？",
                "你如何保证接口安全、参数校验和错误提示的用户体验？",
                "请说明你在项目中做过的性能、稳定性或可维护性改进。",
                "面对不熟悉的业务需求，你会如何拆解任务并验证结果？",
                "请总结你相对目标岗位的优势和还需要补强的地方。"
        );
        List<InterviewQuestionSnapshotDocument> questions = new ArrayList<>();
        Instant now = Instant.now();
        for (int index = 0; index < session.getQuestionCount(); index++) {
            InterviewQuestionSnapshotDocument question = new InterviewQuestionSnapshotDocument();
            question.setQuestionId(UUID.randomUUID().toString());
            question.setSessionId(session.getSessionId());
            question.setUsername(session.getUsername());
            question.setQuestionOrder(index + 1);
            question.setQuestion(templates.get(index % templates.size()).formatted(session.getJobTitle()));
            question.setReferenceAnswer("回答应包含背景、职责、技术方案、遇到的问题、解决过程和量化结果。");
            question.setEvaluationPoints("考察 Java 后端基础、项目理解、问题排查、表达结构和结果意识。");
            question.setFollowUpDirection("根据候选人回答继续追问技术细节、权衡理由和真实贡献。");
            question.setEvidenceIds(evidenceList.stream().map(EvidenceResponse::evidenceId).toList());
            question.setEvidenceSummary(buildQuestionEvidenceSummary(evidenceList));
            question.setCreatedAt(now);
            questions.add(question);
        }
        return questions;
    }

    private int normalizeQuestionCount(Integer questionCount) {
        if (questionCount == null) {
            return DEFAULT_QUESTION_COUNT;
        }
        return Math.max(1, Math.min(questionCount, MAX_QUESTION_COUNT));
    }

    private int scoreAnswer(String answer) {
        String text = answer == null ? "" : answer;
        int score = 60;
        if (text.length() >= 80) {
            score += 10;
        }
        if (containsAny(text, "Java", "Spring", "MySQL", "Redis", "MongoDB")) {
            score += 10;
        }
        if (containsAny(text, "负责", "实现", "设计", "优化", "排查")) {
            score += 10;
        }
        if (containsAny(text, "指标", "耗时", "QPS", "错误率", "数据", "提升", "降低")) {
            score += 10;
        }
        return Math.min(score, 100);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String buildFeedback(int score, String answer) {
        if (score >= 90) {
            return "回答较完整，能体现项目背景、技术方案和结果意识。";
        }
        if (score >= 75) {
            return "回答基本可用，但建议补充更具体的技术细节和量化结果。";
        }
        return "回答偏泛，需要补充你的具体职责、技术选型理由、问题定位过程和最终效果。";
    }

    private String buildFollowUpQuestion(int score, InterviewQuestionSnapshotDocument question) {
        if (score >= 85) {
            return "如果把这个方案放到更高并发场景，你会优先优化哪里？";
        }
        return "你能再补充一个具体技术细节或量化结果，说明这件事确实由你负责吗？";
    }

    private int calculateAverageScore(String username, String sessionId) {
        List<InterviewQuestionSnapshotDocument> questions = listQuestionDocuments(username, sessionId);
        return (int) Math.round(questions.stream()
                .filter(item -> item.getScore() != null)
                .mapToInt(InterviewQuestionSnapshotDocument::getScore)
                .average()
                .orElse(0));
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

    private void saveRuntime(String sessionId, String username, String stepType, String content) {
        InterviewRuntimeSnapshotDocument snapshot = new InterviewRuntimeSnapshotDocument();
        snapshot.setSessionId(sessionId);
        snapshot.setUsername(username);
        snapshot.setStepType(stepType);
        snapshot.setContent(content);
        snapshot.setCreatedAt(Instant.now());
        runtimeRepository.save(snapshot);
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
                question.getUserAnswer(),
                question.getScore(),
                question.getFeedback(),
                question.getFollowUpQuestion(),
                question.getCreatedAt(),
                question.getAnsweredAt()
        );
    }

    private String blankToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
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
