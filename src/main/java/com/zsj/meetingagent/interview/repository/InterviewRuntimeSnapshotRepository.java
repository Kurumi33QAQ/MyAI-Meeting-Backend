package com.zsj.meetingagent.interview.repository;

import com.zsj.meetingagent.interview.entity.InterviewRuntimeSnapshotDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * 面试运行快照 MongoDB 仓储。
 * 运行快照用于调试和复盘，不直接参与前端主流程。
 */
public interface InterviewRuntimeSnapshotRepository extends MongoRepository<InterviewRuntimeSnapshotDocument, String> {
}
