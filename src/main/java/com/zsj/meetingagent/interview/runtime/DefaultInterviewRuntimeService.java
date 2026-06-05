package com.zsj.meetingagent.interview.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.meetingagent.common.exception.BusinessException;
import com.zsj.meetingagent.interview.entity.InterviewQuestionSnapshotDocument;
import com.zsj.meetingagent.interview.entity.InterviewRuntimeSnapshotDocument;
import com.zsj.meetingagent.interview.entity.InterviewSessionDocument;
import com.zsj.meetingagent.interview.enums.InterviewSessionStatus;
import com.zsj.meetingagent.interview.repository.InterviewQuestionSnapshotRepository;
import com.zsj.meetingagent.interview.repository.InterviewRuntimeSnapshotRepository;
import com.zsj.meetingagent.interview.repository.InterviewSessionRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 默认面试运行态服务。
 * 使用 Redis 保存热态、MongoDB 保存冷快照，恢复时按 Redis -> MongoDB -> 会话重建的顺序兜底。
 */
@Service
public class DefaultInterviewRuntimeService implements InterviewRuntimeService {

    private static final String RUNTIME_KEY_PREFIX = "meetingagent:interview:runtime";
    private static final Duration HOT_STATE_TTL = Duration.ofHours(12);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionSnapshotRepository questionRepository;
    private final InterviewRuntimeSnapshotRepository snapshotRepository;

    public DefaultInterviewRuntimeService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            InterviewSessionRepository sessionRepository,
            InterviewQuestionSnapshotRepository questionRepository,
            InterviewRuntimeSnapshotRepository snapshotRepository
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Override
    public InterviewRuntimeState recordSnapshot(
            InterviewSessionDocument session,
            List<InterviewQuestionSnapshotDocument> questions,
            String stepType,
            String content
    ) {
        long nextVersion = latestVersion(session.getUsername(), session.getSessionId()) + 1;
        InterviewRuntimeState state = buildState(
                session,
                questions,
                nextVersion,
                InterviewRuntimeRestoreSource.HOT_REDIS
        );
        saveHotState(state);
        saveColdSnapshot(state, stepType, content, InterviewRuntimeRestoreSource.HOT_REDIS);
        return state;
    }

    @Override
    public InterviewRuntimeState recover(String username, String sessionId) {
        validateSessionId(sessionId);
        Optional<InterviewRuntimeState> hotState = readHotState(username, sessionId);
        if (hotState.isPresent()) {
            return withRestoreSource(hotState.get(), InterviewRuntimeRestoreSource.HOT_REDIS);
        }

        Optional<InterviewRuntimeSnapshotDocument> coldSnapshot = snapshotRepository
                .findFirstBySessionIdAndUsernameOrderByVersionDescCreatedAtDesc(sessionId, username);
        if (coldSnapshot.isPresent() && StringUtils.hasText(coldSnapshot.get().getStatus())) {
            InterviewRuntimeState state = fromSnapshot(coldSnapshot.get(), InterviewRuntimeRestoreSource.COLD_MONGO);
            saveHotState(state);
            return state;
        }

        InterviewSessionDocument session = sessionRepository.findBySessionIdAndUsername(sessionId, username)
                .orElseThrow(() -> new BusinessException("I0404", "面试会话不存在或无权访问"));
        List<InterviewQuestionSnapshotDocument> questions = listQuestions(username, sessionId);
        InterviewRuntimeState rebuilt = buildState(
                session,
                questions,
                latestVersion(username, sessionId) + 1,
                InterviewRuntimeRestoreSource.REBUILT_FROM_SESSION
        );
        saveHotState(rebuilt);
        saveColdSnapshot(rebuilt, "RECOVER_REBUILD", "Redis 和冷快照不可用，根据会话和题目重建运行态", InterviewRuntimeRestoreSource.REBUILT_FROM_SESSION);
        return rebuilt;
    }

    private InterviewRuntimeState buildState(
            InterviewSessionDocument session,
            List<InterviewQuestionSnapshotDocument> questions,
            long version,
            InterviewRuntimeRestoreSource source
    ) {
        int answeredCount = countAnswered(questions);
        int currentQuestionIndex = calculateCurrentQuestionIndex(session, questions, answeredCount);
        return new InterviewRuntimeState(
                session.getSessionId(),
                session.getUsername(),
                session.getStatus(),
                currentQuestionIndex,
                answeredCount,
                session.getQuestionCount(),
                session.getTotalScore(),
                version,
                source,
                Instant.now()
        );
    }

    private int calculateCurrentQuestionIndex(
            InterviewSessionDocument session,
            List<InterviewQuestionSnapshotDocument> questions,
            int answeredCount
    ) {
        if (session.getStatus() == InterviewSessionStatus.CREATED || questions.isEmpty()) {
            return 0;
        }
        if (session.getStatus() == InterviewSessionStatus.COMPLETED) {
            return Math.max(questions.size(), answeredCount);
        }
        return questions.stream()
                .filter(question -> !StringUtils.hasText(question.getUserAnswer()))
                .map(InterviewQuestionSnapshotDocument::getQuestionOrder)
                .min(Comparator.naturalOrder())
                .orElse(Math.min(answeredCount + 1, Math.max(questions.size(), session.getQuestionCount())));
    }

    private int countAnswered(List<InterviewQuestionSnapshotDocument> questions) {
        return (int) questions.stream()
                .filter(question -> StringUtils.hasText(question.getUserAnswer()))
                .count();
    }

    private long latestVersion(String username, String sessionId) {
        return snapshotRepository.findFirstBySessionIdAndUsernameOrderByVersionDescCreatedAtDesc(sessionId, username)
                .map(InterviewRuntimeSnapshotDocument::getVersion)
                .orElse(0L);
    }

    private List<InterviewQuestionSnapshotDocument> listQuestions(String username, String sessionId) {
        return questionRepository.findBySessionIdAndUsernameOrderByQuestionOrderAsc(sessionId, username);
    }

    private void saveHotState(InterviewRuntimeState state) {
        try {
            redisTemplate.opsForValue().set(runtimeKey(state.username(), state.sessionId()), objectMapper.writeValueAsString(state), HOT_STATE_TTL);
        } catch (Exception ex) {
            // Redis 热态失败不影响主流程，MongoDB 冷快照仍会保存可恢复状态。
        }
    }

    private Optional<InterviewRuntimeState> readHotState(String username, String sessionId) {
        try {
            String value = redisTemplate.opsForValue().get(runtimeKey(username, sessionId));
            if (!StringUtils.hasText(value)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, InterviewRuntimeState.class));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private void saveColdSnapshot(
            InterviewRuntimeState state,
            String stepType,
            String content,
            InterviewRuntimeRestoreSource source
    ) {
        InterviewRuntimeSnapshotDocument snapshot = new InterviewRuntimeSnapshotDocument();
        snapshot.setSessionId(state.sessionId());
        snapshot.setUsername(state.username());
        snapshot.setStepType(stepType);
        snapshot.setContent(content);
        snapshot.setStatus(state.status().name());
        snapshot.setCurrentQuestionIndex(state.currentQuestionIndex());
        snapshot.setAnsweredCount(state.answeredCount());
        snapshot.setQuestionCount(state.questionCount());
        snapshot.setTotalScore(state.totalScore());
        snapshot.setVersion(state.version());
        snapshot.setRestoreSource(source.name());
        snapshot.setCreatedAt(state.updatedAt());
        snapshotRepository.save(snapshot);
    }

    private InterviewRuntimeState fromSnapshot(
            InterviewRuntimeSnapshotDocument snapshot,
            InterviewRuntimeRestoreSource source
    ) {
        return new InterviewRuntimeState(
                snapshot.getSessionId(),
                snapshot.getUsername(),
                InterviewSessionStatus.valueOf(snapshot.getStatus()),
                snapshot.getCurrentQuestionIndex(),
                snapshot.getAnsweredCount(),
                snapshot.getQuestionCount(),
                snapshot.getTotalScore(),
                snapshot.getVersion(),
                source,
                Instant.now()
        );
    }

    private InterviewRuntimeState withRestoreSource(InterviewRuntimeState state, InterviewRuntimeRestoreSource source) {
        return new InterviewRuntimeState(
                state.sessionId(),
                state.username(),
                state.status(),
                state.currentQuestionIndex(),
                state.answeredCount(),
                state.questionCount(),
                state.totalScore(),
                state.version(),
                source,
                Instant.now()
        );
    }

    private void validateSessionId(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new BusinessException("I0401", "面试会话不能为空");
        }
    }

    private String runtimeKey(String username, String sessionId) {
        return "%s:%s:%s".formatted(RUNTIME_KEY_PREFIX, username, sessionId);
    }
}
