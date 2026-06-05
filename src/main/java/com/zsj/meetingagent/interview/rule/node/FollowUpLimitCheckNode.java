package com.zsj.meetingagent.interview.rule.node;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.zsj.meetingagent.interview.rule.FollowUpRuleContext;

/**
 * 追问次数限制节点。
 * 用规则节点限制追问次数，避免低分回答被无限追问，后续可改成可配置策略。
 */
@LiteflowComponent("followUpLimitCheckNode")
public class FollowUpLimitCheckNode extends NodeComponent {

    @Override
    public void process() {
        FollowUpRuleContext context = getContextBean(FollowUpRuleContext.class);
        if (context.existingFollowUpCount() >= context.maxFollowUpCount()) {
            context.suppress("追问次数限制节点", "当前题追问次数已达到上限：" + context.maxFollowUpCount());
            return;
        }
        context.addTrace("追问次数限制节点", false, "当前题仍可追问。");
    }
}
