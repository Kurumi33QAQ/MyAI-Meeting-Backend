package com.zsj.meetingagent.interview.rule;

/**
 * 追问规则节点轨迹。
 * 每条记录说明某个 LiteFlow 节点是否命中，以及为什么命中或跳过。
 */
public record FollowUpRuleTrace(
        String nodeName,
        boolean hit,
        String reason
) {
}
