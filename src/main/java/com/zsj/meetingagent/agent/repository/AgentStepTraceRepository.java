package com.zsj.meetingagent.agent.repository;

import com.zsj.meetingagent.agent.entity.AgentStepTraceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Agent 步骤轨迹的 MongoDB 仓储。
 * 按 stepOrder 排序可以还原 Thought-Action-Observation-Final Answer 的执行过程。
 */
public interface AgentStepTraceRepository extends MongoRepository<AgentStepTraceDocument, String> {

    List<AgentStepTraceDocument> findByRunIdAndUsernameOrderByStepOrderAsc(String runId, String username);
}
