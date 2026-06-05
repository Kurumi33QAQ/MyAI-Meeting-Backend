package com.zsj.meetingagent.interview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.meetingagent.interview.entity.InterviewQuestionSnapshotDocument;
import com.zsj.meetingagent.interview.entity.InterviewRuntimeSnapshotDocument;
import com.zsj.meetingagent.interview.entity.InterviewSessionDocument;
import com.zsj.meetingagent.interview.enums.InterviewSessionStatus;
import com.zsj.meetingagent.interview.repository.InterviewQuestionSnapshotRepository;
import com.zsj.meetingagent.interview.repository.InterviewRuntimeSnapshotRepository;
import com.zsj.meetingagent.interview.repository.InterviewSessionRepository;
import com.zsj.meetingagent.interview.runtime.DefaultInterviewRuntimeService;
import com.zsj.meetingagent.interview.runtime.InterviewRuntimeRestoreSource;
import com.zsj.meetingagent.interview.runtime.InterviewRuntimeState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultInterviewRuntimeServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private InterviewSessionRepository sessionRepository;

    @Mock
    private InterviewQuestionSnapshotRepository questionRepository;

    @Mock
    private InterviewRuntimeSnapshotRepository snapshotRepository;

    private DefaultInterviewRuntimeService runtimeService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        runtimeService = new DefaultInterviewRuntimeService(
                redisTemplate,
                new ObjectMapper().findAndRegisterModules(),
                sessionRepository,
                questionRepository,
                snapshotRepository
        );
    }

    @Test
    void recordSnapshotWritesHotRedisAndColdMongoSnapshot() {
        when(snapshotRepository.findFirstBySessionIdAndUsernameOrderByVersionDescCreatedAtDesc("session-1", "alice"))
                .thenReturn(Optional.empty());
        when(snapshotRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<InterviewRuntimeSnapshotDocument> snapshotCaptor = ArgumentCaptor.forClass(InterviewRuntimeSnapshotDocument.class);

        InterviewRuntimeState state = runtimeService.recordSnapshot(
                session(InterviewSessionStatus.ANSWERING, 3, 1, null),
                List.of(answeredQuestion(1), unansweredQuestion(2)),
                "SUBMIT_ANSWER",
                "提交第 1 题"
        );

        assertThat(state.version()).isEqualTo(1);
        assertThat(state.currentQuestionIndex()).isEqualTo(2);
        assertThat(state.answeredCount()).isEqualTo(1);
        verify(valueOperations).set(contains("meetingagent:interview:runtime:alice:session-1"), anyString(), any(Duration.class));
        verify(snapshotRepository).save(snapshotCaptor.capture());
        assertThat(snapshotCaptor.getValue().getStepType()).isEqualTo("SUBMIT_ANSWER");
        assertThat(snapshotCaptor.getValue().getRestoreSource()).isEqualTo("HOT_REDIS");
    }

    @Test
    void recoverUsesColdMongoWhenRedisHotStateMissing() {
        when(valueOperations.get("meetingagent:interview:runtime:alice:session-1")).thenReturn(null);
        when(snapshotRepository.findFirstBySessionIdAndUsernameOrderByVersionDescCreatedAtDesc("session-1", "alice"))
                .thenReturn(Optional.of(snapshot(InterviewSessionStatus.ANSWERING, 2, 1, 3, 5)));

        InterviewRuntimeState state = runtimeService.recover("alice", "session-1");

        assertThat(state.restoreSource()).isEqualTo(InterviewRuntimeRestoreSource.COLD_MONGO);
        assertThat(state.version()).isEqualTo(5);
        assertThat(state.currentQuestionIndex()).isEqualTo(2);
        verify(valueOperations).set(eq("meetingagent:interview:runtime:alice:session-1"), anyString(), any(Duration.class));
    }

    @Test
    void recoverRebuildsFromSessionWhenNoRedisAndNoColdSnapshot() {
        when(valueOperations.get("meetingagent:interview:runtime:alice:session-1")).thenReturn(null);
        when(snapshotRepository.findFirstBySessionIdAndUsernameOrderByVersionDescCreatedAtDesc("session-1", "alice"))
                .thenReturn(Optional.empty());
        when(sessionRepository.findBySessionIdAndUsername("session-1", "alice"))
                .thenReturn(Optional.of(session(InterviewSessionStatus.ANSWERING, 3, 1, null)));
        when(questionRepository.findBySessionIdAndUsernameOrderByQuestionOrderAsc("session-1", "alice"))
                .thenReturn(List.of(answeredQuestion(1), unansweredQuestion(2), unansweredQuestion(3)));
        when(snapshotRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InterviewRuntimeState state = runtimeService.recover("alice", "session-1");

        assertThat(state.restoreSource()).isEqualTo(InterviewRuntimeRestoreSource.REBUILT_FROM_SESSION);
        assertThat(state.currentQuestionIndex()).isEqualTo(2);
        assertThat(state.answeredCount()).isEqualTo(1);
        assertThat(state.version()).isEqualTo(1);
    }

    private InterviewSessionDocument session(
            InterviewSessionStatus status,
            int questionCount,
            int answeredCount,
            Integer totalScore
    ) {
        InterviewSessionDocument session = new InterviewSessionDocument();
        session.setSessionId("session-1");
        session.setUsername("alice");
        session.setStatus(status);
        session.setQuestionCount(questionCount);
        session.setAnsweredCount(answeredCount);
        session.setTotalScore(totalScore);
        return session;
    }

    private InterviewQuestionSnapshotDocument answeredQuestion(int order) {
        InterviewQuestionSnapshotDocument question = unansweredQuestion(order);
        question.setUserAnswer("我完成了第 " + order + " 题");
        return question;
    }

    private InterviewQuestionSnapshotDocument unansweredQuestion(int order) {
        InterviewQuestionSnapshotDocument question = new InterviewQuestionSnapshotDocument();
        question.setQuestionId("question-" + order);
        question.setSessionId("session-1");
        question.setUsername("alice");
        question.setQuestionOrder(order);
        return question;
    }

    private InterviewRuntimeSnapshotDocument snapshot(
            InterviewSessionStatus status,
            int currentQuestionIndex,
            int answeredCount,
            int questionCount,
            long version
    ) {
        InterviewRuntimeSnapshotDocument snapshot = new InterviewRuntimeSnapshotDocument();
        snapshot.setSessionId("session-1");
        snapshot.setUsername("alice");
        snapshot.setStatus(status.name());
        snapshot.setCurrentQuestionIndex(currentQuestionIndex);
        snapshot.setAnsweredCount(answeredCount);
        snapshot.setQuestionCount(questionCount);
        snapshot.setVersion(version);
        snapshot.setRestoreSource("HOT_REDIS");
        snapshot.setCreatedAt(Instant.now());
        return snapshot;
    }
}
