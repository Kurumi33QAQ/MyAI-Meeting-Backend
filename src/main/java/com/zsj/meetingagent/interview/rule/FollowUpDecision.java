package com.zsj.meetingagent.interview.rule;

import java.util.List;

/**
 * LiteFlow 追问裁决结果。
 * shouldFollowUp 表示是否追问，followUpQuestion 是最终要返回给前端的问题。
 */
public record FollowUpDecision(
        boolean shouldFollowUp,
        String followUpQuestion,
        String reason,
        List<FollowUpRuleTrace> traces
) {

    public String traceSummary() {
        return traces.stream()
                .map(trace -> "%s[%s]：%s".formatted(trace.nodeName(), trace.hit() ? "命中" : "未命中", trace.reason()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }
}
