package com.zsj.meetingagent.interview.rule.node;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.zsj.meetingagent.interview.rule.FollowUpRuleContext;

/**
 * AI 建议判断节点。
 * 当大模型反馈中明确提到缺失、不足或需要补充时，把这个观察纳入追问裁决。
 */
@LiteflowComponent("aiSuggestionCheckNode")
public class AiSuggestionCheckNode extends NodeComponent {

    @Override
    public void process() {
        FollowUpRuleContext context = getContextBean(FollowUpRuleContext.class);
        String aiFeedback = context.aiFeedback() == null ? "" : context.aiFeedback();
        if (containsAny(aiFeedback, "缺", "不足", "补充", "没有命中", "需要")) {
            context.propose(
                    "AI 建议判断节点",
                    "根据刚才的反馈，请补充你没有展开的技术细节，并说明这个方案为什么适合当时的业务场景。",
                    "AI 反馈指出回答仍有缺失，需要追问补齐。",
                    false
            );
            return;
        }
        context.addTrace("AI 建议判断节点", false, "AI 反馈没有强烈追问信号。");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
