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
import com.zsj.meetingagent.interview.entity.InterviewQuestionSnapshotDocument;
import com.zsj.meetingagent.interview.entity.InterviewSessionDocument;
import com.zsj.meetingagent.interview.enums.InterviewSessionStatus;
import com.zsj.meetingagent.interview.mapper.InterviewRecordMapper;
import com.zsj.meetingagent.interview.prompt.InterviewPromptBuilder;
import com.zsj.meetingagent.interview.repository.InterviewQuestionSnapshotRepository;
import com.zsj.meetingagent.interview.repository.InterviewSessionRepository;
import com.zsj.meetingagent.interview.rule.FollowUpDecision;
import com.zsj.meetingagent.interview.rule.FollowUpDecisionService;
import com.zsj.meetingagent.interview.rule.FollowUpRuleTrace;
import com.zsj.meetingagent.interview.runtime.InterviewRuntimeService;
import com.zsj.meetingagent.interview.service.impl.DefaultInterviewService;
import com.zsj.meetingagent.interview.vo.InterviewAnswerResponse;
import com.zsj.meetingagent.interview.vo.InterviewRuntimeStateResponse;
import com.zsj.meetingagent.interview.vo.InterviewSessionResponse;
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
                true,
                "请补充一个具体量化结果。",
                "缺少量化结果",
                List.of(new FollowUpRuleTrace("缺失考点判断节点", true, "回答缺少量化结果"))
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
                followUpDecisionService
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
                "我负责 Java Spring Boot 项目接口设计，使用 MySQL 和 Redis 优化查询耗时，并通过指标观察将接口耗时降低。"
        ));

        assertThat(created.status()).isEqualTo(InterviewSessionStatus.CREATED);
        assertThat(generated.questions()).hasSize(1);
        assertThat(generated.questions().getFirst().agentRunId()).isEqualTo("agent-run-1");
        assertThat(answer.status()).isEqualTo(InterviewSessionStatus.COMPLETED);
        assertThat(answer.score()).isGreaterThanOrEqualTo(90);
        assertThat(answer.followUpRuleTrace()).contains("缺失考点判断节点");
        assertThat(service.getReport("alice", created.sessionId()).totalScore()).isGreaterThanOrEqualTo(90);
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
                followUpDecisionService
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

    private AiModelProperties testAiModelProperties() {
        AiModelProperties properties = new AiModelProperties();
        properties.setDefaultModel("gpt-4o-mini");
        return properties;
    }
}
