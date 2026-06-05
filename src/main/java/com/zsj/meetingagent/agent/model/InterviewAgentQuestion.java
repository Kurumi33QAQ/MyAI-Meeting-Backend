package com.zsj.meetingagent.agent.model;

import java.util.List;

/**
 * 多 Agent 协同生成的面试题计划。
 * 这里保存题目正文、考察点和证据来源，面试服务会进一步落到题目快照中。
 */
public record InterviewAgentQuestion(
        String question,
        String referenceAnswer,
        String evaluationPoints,
        String followUpDirection,
        List<String> evidenceIds,
        String evidenceSummary
) {
}
