package com.zsj.meetingagent.agent.enums;

/**
 * Agent 运行状态。
 * 用于标记一次 Agent 执行是运行中、已完成还是失败，方便后续做执行记录查询和失败排查。
 */
public enum AgentRunStatus {
    RUNNING,
    COMPLETED,
    FAILED
}
