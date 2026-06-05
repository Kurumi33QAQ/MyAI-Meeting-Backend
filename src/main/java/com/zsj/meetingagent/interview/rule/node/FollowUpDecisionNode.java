package com.zsj.meetingagent.interview.rule.node;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.zsj.meetingagent.interview.rule.FollowUpRuleContext;

/**
 * 最终追问裁决节点。
 * 前面的节点只提出候选追问或保护规则，本节点统一产出最终结果。
 */
@LiteflowComponent("followUpDecisionNode")
public class FollowUpDecisionNode extends NodeComponent {

    @Override
    public void process() {
        FollowUpRuleContext context = getContextBean(FollowUpRuleContext.class);
        context.completeDecision();
    }
}
