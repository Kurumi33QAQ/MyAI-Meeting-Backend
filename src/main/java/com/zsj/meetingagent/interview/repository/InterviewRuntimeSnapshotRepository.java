package com.zsj.meetingagent.interview.repository;

import com.zsj.meetingagent.interview.entity.InterviewRuntimeSnapshotDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * 面试运行快照 MongoDB 仓储。
 * 运行快照既用于调试复盘，也作为 Redis 热态丢失后的冷恢复来源。
 */
public interface InterviewRuntimeSnapshotRepository extends MongoRepository<InterviewRuntimeSnapshotDocument, String> {

    Optional<InterviewRuntimeSnapshotDocument> findFirstBySessionIdAndUsernameOrderByVersionDescCreatedAtDesc(
            String sessionId,
            String username
    );
}
