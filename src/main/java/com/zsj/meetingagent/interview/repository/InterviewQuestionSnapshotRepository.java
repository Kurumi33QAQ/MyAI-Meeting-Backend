package com.zsj.meetingagent.interview.repository;

import com.zsj.meetingagent.interview.entity.InterviewQuestionSnapshotDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * 面试题目快照 MongoDB 仓储。
 * 负责按会话顺序查询题目，以及校验题目是否属于当前用户。
 */
public interface InterviewQuestionSnapshotRepository extends MongoRepository<InterviewQuestionSnapshotDocument, String> {

    List<InterviewQuestionSnapshotDocument> findBySessionIdAndUsernameOrderByQuestionOrderAsc(String sessionId, String username);

    Optional<InterviewQuestionSnapshotDocument> findByQuestionIdAndSessionIdAndUsername(String questionId, String sessionId, String username);
}
