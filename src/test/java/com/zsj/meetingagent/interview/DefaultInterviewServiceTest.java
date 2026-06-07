package com.zsj.meetingagent.interview;

import com.zsj.meetingagent.ai.dto.AiChatRequest;
import com.zsj.meetingagent.ai.config.AiModelProperties;
import com.zsj.meetingagent.ai.service.AiChatService;
import com.zsj.meetingagent.ai.vo.AiChatResponse;
import com.zsj.meetingagent.agent.model.InterviewAgentOutput;
import com.zsj.meetingagent.agent.model.InterviewAgentQuestion;
import com.zsj.meetingagent.agent.model.InterviewOrchestrationResult;
import com.zsj.meetingagent.agent.orchestrator.InterviewOrchestrator;
import com.zsj.meetingagent.interview.dto.CreateInterviewSessionRequest;
import com.zsj.meetingagent.interview.dto.SubmitInterviewAnswerRequest;
import com.zsj.meetingagent.interview.config.InterviewProperties;
import com.zsj.meetingagent.interview.entity.InterviewFollowUpRound;
import com.zsj.meetingagent.interview.entity.InterviewQuestionSnapshotDocument;
import com.zsj.meetingagent.interview.entity.InterviewSessionDocument;
import com.zsj.meetingagent.interview.enums.InterviewSessionStatus;
import com.zsj.meetingagent.interview.followup.FollowUpQuestionGenerator;
import com.zsj.meetingagent.interview.mapper.InterviewRecordMapper;
import com.zsj.meetingagent.interview.prompt.InterviewPromptBuilder;
import com.zsj.meetingagent.interview.repository.InterviewQuestionSnapshotRepository;
import com.zsj.meetingagent.interview.repository.InterviewSessionRepository;
import com.zsj.meetingagent.interview.rule.FollowUpDecision;
import com.zsj.meetingagent.interview.rule.FollowUpDecisionService;
import com.zsj.meetingagent.interview.rule.FollowUpRuleTrace;
import com.zsj.meetingagent.interview.runtime.InterviewRuntimeService;
import com.zsj.meetingagent.interview.adaptive.DefaultInterviewProgressPolicy;
import com.zsj.meetingagent.interview.scoring.DefaultAnswerScoringService;
import com.zsj.meetingagent.interview.service.impl.DefaultInterviewService;
import com.zsj.meetingagent.interview.vo.InterviewAnswerResponse;
import com.zsj.meetingagent.interview.vo.InterviewRuntimeStateResponse;
import com.zsj.meetingagent.interview.vo.InterviewSessionResponse;
import com.zsj.meetingagent.knowledge.model.JobIntelligenceReport;
import com.zsj.meetingagent.knowledge.service.JobIntelligenceSearchService;
import com.zsj.meetingagent.rag.service.KnowledgeIngestionService;
import com.zsj.meetingagent.rag.service.RetrievalService;
import com.zsj.meetingagent.resume.service.ResumeService;
import com.zsj.meetingagent.resume.vo.ResumeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultInterviewServiceTest {

    @Mock
    private ResumeService resumeService;

    @Mock
    private AiChatService aiChatService;

    @Mock
    private InterviewRecordMapper interviewRecordMapper;

    @Mock
    private InterviewSessionRepository sessionRepository;

    @Mock
    private InterviewQuestionSnapshotRepository questionRepository;

    @Mock
    private InterviewRuntimeService runtimeService;

    @Mock
    private KnowledgeIngestionService knowledgeIngestionService;

    @Mock
    private RetrievalService retrievalService;

    @Mock
    private InterviewOrchestrator interviewOrchestrator;

    @Mock
    private FollowUpDecisionService followUpDecisionService;

    @Mock
    private JobIntelligenceSearchService jobIntelligenceSearchService;

    @Mock
    private FollowUpQuestionGenerator followUpQuestionGenerator;

    @Test
    void createGenerateAnswerAndReport() {
        AtomicReference<InterviewSessionDocument> sessionRef = new AtomicReference<>();
        List<InterviewQuestionSnapshotDocument> questions = new ArrayList<>();
        ArgumentCaptor<InterviewQuestionSnapshotDocument> questionCaptor = ArgumentCaptor.forClass(InterviewQuestionSnapshotDocument.class);

        when(resumeService.getResume(anyString(), anyString()))
                .thenReturn(new ResumeResponse("resume-1", "resume.txt", "text/plain", 100L, "RESUME", "Java Spring Boot MySQL 项目经历", Instant.now(), Instant.now()));
        when(aiChatService.chat(any(AiChatRequest.class)))
                .thenReturn(new AiChatResponse("AI 反馈建议", "gpt-4o-mini", "mock", 1, true));
        when(sessionRepository.save(any(InterviewSessionDocument.class))).thenAnswer(invocation -> {
            InterviewSessionDocument document = invocation.getArgument(0);
            sessionRef.set(document);
            return document;
        });
        when(sessionRepository.findBySessionIdAndUsername(anyString(), anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(sessionRef.get()));
        when(questionRepository.findBySessionIdAndUsernameOrderByQuestionOrderAsc(anyString(), anyString()))
                .thenReturn(questions);
        when(questionRepository.saveAll(any())).thenAnswer(invocation -> {
            List<InterviewQuestionSnapshotDocument> saved = invocation.getArgument(0);
            questions.clear();
            questions.addAll(saved);
            return saved;
        });
        when(questionRepository.findByQuestionIdAndSessionIdAndUsername(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> questions.stream()
                        .filter(question -> question.getQuestionId().equals(invocation.getArgument(0)))
                        .findFirst());
        when(questionRepository.save(questionCaptor.capture())).thenAnswer(invocation -> {
            InterviewQuestionSnapshotDocument saved = invocation.getArgument(0);
            questions.replaceAll(item -> item.getQuestionId().equals(saved.getQuestionId()) ? saved : item);
            return saved;
        });
        when(interviewRecordMapper.insert(any())).thenReturn(1);
        when(interviewRecordMapper.updateProgress(any())).thenReturn(1);
        when(runtimeService.recordSnapshot(any(), any(), anyString(), anyString())).thenReturn(null);
        when(retrievalService.retrieveForInterview(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(List.of());
        when(jobIntelligenceSearchService.search(anyString(), anyString(), anyString()))
                .thenReturn(JobIntelligenceReport.disabled("测试环境未启用联网搜索"));
        when(interviewOrchestrator.designQuestions(any())).thenReturn(new InterviewOrchestrationResult(
                "agent-run-1",
                List.of(new InterviewAgentOutput("简历分析 Agent", "发现 Java 项目经历", "适合追问项目真实性")),
                List.of(new InterviewAgentQuestion(
                        "请介绍一个 Java 后端项目",
                        "应包含背景、职责、方案和结果。",
                        "项目理解、技术细节、表达结构",
                        "追问技术细节",
                        List.of("evidence-1"),
                        "项目经历：Spring Boot 接口开发"
                )),
                "多 Agent 出题完成"
        ));
        when(interviewOrchestrator.reviewAnswer(anyString(), anyString(), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(new InterviewAgentOutput("回答评估 Agent", "回答质量较好，得分：90", "命中项目和技术细节"));
        when(followUpDecisionService.decide(any())).thenReturn(new FollowUpDecision(
                false,
                null,
                "当前回答信息充足，无需追问",
                List.of(new FollowUpRuleTrace("追问决策节点", false, "当前回答信息充足"))
        ));
        DefaultInterviewService service = new DefaultInterviewService(
                resumeService,
                aiChatService,
                testAiModelProperties(),
                new InterviewPromptBuilder(),
                interviewRecordMapper,
                sessionRepository,
                questionRepository,
                runtimeService,
                knowledgeIngestionService,
                retrievalService,
                interviewOrchestrator,
                followUpDecisionService,
                jobIntelligenceSearchService,
                new DefaultAnswerScoringService(),
                new DefaultInterviewProgressPolicy(),
                followUpQuestionGenerator,
                new InterviewProperties()
        );

        InterviewSessionResponse created = service.createSession("alice", new CreateInterviewSessionRequest(
                "resume-1",
                "Java 后端开发",
                "示例公司",
                "熟悉 Spring Boot 和 MySQL",
                1
        ));
        InterviewSessionResponse generated = service.generateQuestions("alice", created.sessionId());
        InterviewAnswerResponse answer = service.submitAnswer("alice", created.sessionId(), new SubmitInterviewAnswerRequest(
                generated.questions().getFirst().questionId(),
                "我负责 Java Spring Boot 项目的接口设计，因为原查询存在慢 SQL，所以通过日志和压测定位问题，增加 MySQL 联合索引并使用 Redis 缓存，将接口耗时从 300ms 降低到 80ms。"
        ));

        assertThat(created.status()).isEqualTo(InterviewSessionStatus.CREATED);
        assertThat(generated.questions()).hasSize(1);
        assertThat(generated.questions().getFirst().agentRunId()).isEqualTo("agent-run-1");
        assertThat(answer.status()).isEqualTo(InterviewSessionStatus.COMPLETED);
        assertThat(answer.score()).isGreaterThanOrEqualTo(85);
        assertThat(answer.followUpRuleTrace()).contains("追问决策节点");
        assertThat(service.getReport("alice", created.sessionId()).totalScore()).isGreaterThanOrEqualTo(85);
        assertThat(questionCaptor.getValue().getUserAnswer()).contains("Spring Boot");
    }

    @Test
    void runtimeStateIsDelegatedToRuntimeService() {
        DefaultInterviewService service = new DefaultInterviewService(
                resumeService,
                aiChatService,
                testAiModelProperties(),
                new InterviewPromptBuilder(),
                interviewRecordMapper,
                sessionRepository,
                questionRepository,
                runtimeService,
                knowledgeIngestionService,
                retrievalService,
                interviewOrchestrator,
                followUpDecisionService,
                jobIntelligenceSearchService,
                new DefaultAnswerScoringService(),
                new DefaultInterviewProgressPolicy(),
                followUpQuestionGenerator,
                new InterviewProperties()
        );
        when(runtimeService.recover(anyString(), anyString())).thenReturn(new com.zsj.meetingagent.interview.runtime.InterviewRuntimeState(
                "session-1",
                "alice",
                InterviewSessionStatus.ANSWERING,
                2,
                1,
                3,
                null,
                4,
                com.zsj.meetingagent.interview.runtime.InterviewRuntimeRestoreSource.COLD_MONGO,
                Instant.now()
        ));

        InterviewRuntimeStateResponse response = service.getRuntimeState("alice", "session-1");

        assertThat(response.sessionId()).isEqualTo("session-1");
        assertThat(response.restoreSource().name()).isEqualTo("COLD_MONGO");
        assertThat(response.currentQuestionIndex()).isEqualTo(2);
    }

    @Test
    void followUpAnswerCanTriggerSecondFollowUpRound() {
        InterviewSessionDocument session = new InterviewSessionDocument();
        session.setSessionId("session-1");
        session.setUsername("alice");
        session.setResumeId("resume-1");
        session.setStatus(InterviewSessionStatus.ANSWERING);
        session.setAdaptiveQuestionCount(true);
        session.setQuestionCount(8);
        session.setMaxQuestionCount(15);
        session.setAnsweredCount(1);
        session.setUpdatedAt(Instant.now());

        InterviewFollowUpRound firstFollowUp = new InterviewFollowUpRound();
        firstFollowUp.setRound(1);
        firstFollowUp.setQuestion("你项目中 Redis String 和 Set 分别保存了什么业务数据？");
        firstFollowUp.setCreatedAt(Instant.now());
        InterviewQuestionSnapshotDocument question = new InterviewQuestionSnapshotDocument();
        question.setQuestionId("question-1");
        question.setSessionId("session-1");
        question.setUsername("alice");
        question.setQuestionOrder(1);
        question.setQuestion("请说明项目中的 Redis 使用方式");
        question.setEvaluationPoints("Redis 数据结构、key 设计、过期策略");
        question.setFollowUpDirection("追问 Redis 设计细节");
        question.setUserAnswer("用了 String 和 Set");
        question.setScore(45);
        question.setFollowUps(new ArrayList<>(List.of(firstFollowUp)));

        AtomicReference<InterviewQuestionSnapshotDocument> questionRef = new AtomicReference<>(question);
        when(sessionRepository.findBySessionIdAndUsername("session-1", "alice")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(InterviewSessionDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionRepository.findByQuestionIdAndSessionIdAndUsername("question-1", "session-1", "alice"))
                .thenReturn(Optional.of(question));
        when(questionRepository.findBySessionIdAndUsernameOrderByQuestionOrderAsc("session-1", "alice"))
                .thenAnswer(invocation -> List.of(questionRef.get()));
        when(questionRepository.save(any(InterviewQuestionSnapshotDocument.class))).thenAnswer(invocation -> {
            InterviewQuestionSnapshotDocument saved = invocation.getArgument(0);
            questionRef.set(saved);
            return saved;
        });
        when(resumeService.getResume("alice", "resume-1"))
                .thenReturn(new ResumeResponse("resume-1", "resume.txt", "text/plain", 100L, "RESUME", "MyMallPlatform 使用 Redis、RabbitMQ 和 JWT", Instant.now(), Instant.now()));
        when(aiChatService.chat(any(AiChatRequest.class)))
                .thenReturn(new AiChatResponse("回答仍缺少 key 设计和过期策略", "gpt-4o-mini", "mock", 1, true));
        when(followUpDecisionService.decide(any())).thenReturn(new FollowUpDecision(
                true,
                "继续追问 Redis key 和过期策略",
                "F1 回答仍缺少 Redis 设计细节",
                List.of(new FollowUpRuleTrace("缺失考点判断节点", true, "缺少 key 和过期策略"))
        ));
        when(followUpQuestionGenerator.generate(any()))
                .thenReturn("你在这个项目里 Redis String 和 Set 的 key 分别如何命名，过期时间为什么这样设置？");
        when(interviewRecordMapper.updateProgress(any())).thenReturn(1);
        when(runtimeService.recordSnapshot(any(), any(), anyString(), anyString())).thenReturn(null);

        InterviewAnswerResponse response = newService().submitAnswer(
                "alice",
                "session-1",
                new SubmitInterviewAnswerRequest("question-1-F1", "String 存登录状态，Set 存用户集合，但是过期时间没考虑清楚。")
        );

        assertThat(response.isFollowUp()).isTrue();
        assertThat(response.followUpCount()).isEqualTo(2);
        assertThat(response.nextQuestionNumber()).isEqualTo("question-1-F2");
        assertThat(questionRef.get().getFollowUps()).hasSize(2);
        assertThat(questionRef.get().getFollowUps().get(1).getQuestion()).contains("key");
    }

    private AiModelProperties testAiModelProperties() {
        AiModelProperties properties = new AiModelProperties();
        properties.setDefaultModel("gpt-4o-mini");
        return properties;
    }

    private DefaultInterviewService newService() {
        return new DefaultInterviewService(
                resumeService,
                aiChatService,
                testAiModelProperties(),
                new InterviewPromptBuilder(),
                interviewRecordMapper,
                sessionRepository,
                questionRepository,
                runtimeService,
                knowledgeIngestionService,
                retrievalService,
                interviewOrchestrator,
                followUpDecisionService,
                jobIntelligenceSearchService,
                new DefaultAnswerScoringService(),
                new DefaultInterviewProgressPolicy(),
                followUpQuestionGenerator,
                new InterviewProperties()
        );
    }
}
