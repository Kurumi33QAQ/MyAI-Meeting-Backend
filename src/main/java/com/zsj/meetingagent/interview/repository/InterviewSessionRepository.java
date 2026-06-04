package com.zsj.meetingagent.interview.repository;

import com.zsj.meetingagent.interview.entity.InterviewSessionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * 面试会话 MongoDB 仓储。
 * 查询时带 username，避免用户访问不属于自己的面试会话。
 */
public interface InterviewSessionRepository extends MongoRepository<InterviewSessionDocument, String> {

    Optional<InterviewSessionDocument> findBySessionIdAndUsername(String sessionId, String username);
}
