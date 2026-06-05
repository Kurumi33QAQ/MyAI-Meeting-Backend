package com.zsj.meetingagent.agent.repository;

import com.zsj.meetingagent.agent.entity.AgentRunDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Agent Run 的 MongoDB 仓储。
 * 查询时同时使用 runId 和 username，避免用户访问到不属于自己的 Agent 执行记录。
 */
public interface AgentRunRepository extends MongoRepository<AgentRunDocument, String> {

    Optional<AgentRunDocument> findByRunIdAndUsername(String runId, String username);

    List<AgentRunDocument> findBySessionIdAndUsernameOrderByCreatedAtDesc(String sessionId, String username);
}
