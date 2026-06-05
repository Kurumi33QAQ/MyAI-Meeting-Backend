package com.zsj.meetingagent.interview.rule;

/**
 * 追问裁决服务。
 * 面试业务只关心裁决结果，不直接感知 LiteFlow 的执行细节。
 */
public interface FollowUpDecisionService {

    FollowUpDecision decide(FollowUpRuleContext context);
}
