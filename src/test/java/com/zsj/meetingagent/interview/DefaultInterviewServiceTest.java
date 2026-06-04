package com.zsj.meetingagent.interview;

import com.zsj.meetingagent.ai.dto.AiChatRequest;
import com.zsj.meetingagent.ai.service.AiChatService;
import com.zsj.meetingagent.ai.vo.AiChatResponse;
import com.zsj.meetingagent.interview.dto.CreateInterviewSessionRequest;
import com.zsj.meetingagent.interview.dto.SubmitInterviewAnswerRequest;
import com.zsj.meetingagent.interview.entity.InterviewQuestionSnapshotDocument;
import com.zsj.meetingagent.interview.entity.InterviewSessionDocument;
import com.zsj.meetingagent.interview.enums.InterviewSessionStatus;
import com.zsj.meetingagent.interview.mapper.InterviewRecordMapper;
import com.zsj.meetingagent.interview.prompt.InterviewPromptBuilder;
import com.zsj.meetingagent.interview.repository.InterviewQuestionSnapshotRepository;
import com.zsj.meetingagent.interview.repository.InterviewRuntimeSnapshotRepository;
import com.zsj.meetingagent.interview.repository.InterviewSessionRepository;
import com.zsj.meetingagent.interview.service.impl.DefaultInterviewService;
import com.zsj.meetingagent.interview.vo.InterviewAnswerResponse;
import com.zsj.meetingagent.interview.vo.InterviewSessionResponse;
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
    private InterviewRuntimeSnapshotRepository runtimeRepository;

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
        when(runtimeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DefaultInterviewService service = new DefaultInterviewService(
                resumeService,
                aiChatService,
                new InterviewPromptBuilder(),
                interviewRecordMapper,
                sessionRepository,
                questionRepository,
                runtimeRepository
        );

        InterviewSessionResponse created = service.createSession("alice", new CreateInterviewSessionRequest(
                "resume-1",
                "Java 后端开发",
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
        assertThat(answer.status()).isEqualTo(InterviewSessionStatus.COMPLETED);
        assertThat(answer.score()).isGreaterThanOrEqualTo(90);
        assertThat(service.getReport("alice", created.sessionId()).totalScore()).isGreaterThanOrEqualTo(90);
        assertThat(questionCaptor.getValue().getUserAnswer()).contains("Spring Boot");
    }
}
