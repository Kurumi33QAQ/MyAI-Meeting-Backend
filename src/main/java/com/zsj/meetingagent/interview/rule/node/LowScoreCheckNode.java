package com.zsj.meetingagent.interview.rule.node;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.zsj.meetingagent.interview.rule.ContextualFollowUpQuestionFactory;
import com.zsj.meetingagent.interview.rule.FollowUpRuleContext;

/**
 * 低分判断节点。
 * 分数偏低时优先追问项目细节，帮助候选人补充关键事实。
 */
@LiteflowComponent("lowScoreCheckNode")
public class LowScoreCheckNode extends NodeComponent {

    private static final int LOW_SCORE_THRESHOLD = 75;

    @Override
    public void process() {
        FollowUpRuleContext context = getContextBean(FollowUpRuleContext.class);
        if (context.score() < LOW_SCORE_THRESHOLD) {
            context.propose(
                    "低分判断节点",
                    ContextualFollowUpQuestionFactory.forLowScore(context),
                    "得分低于 " + LOW_SCORE_THRESHOLD + "，需要通过追问确认真实项目能力。",
                    true
            );
            return;
        }
        context.addTrace("低分判断节点", false, "得分不低于低分阈值。");
    }
}
