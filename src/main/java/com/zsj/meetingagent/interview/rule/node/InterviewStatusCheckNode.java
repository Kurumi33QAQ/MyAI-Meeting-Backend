package com.zsj.meetingagent.interview.rule.node;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.zsj.meetingagent.interview.enums.InterviewSessionStatus;
import com.zsj.meetingagent.interview.rule.FollowUpRuleContext;

/**
 * 面试状态保护节点。
 * 已完成的面试不能继续追问，避免前端重复提交造成状态回退。
 */
@LiteflowComponent("interviewStatusCheckNode")
public class InterviewStatusCheckNode extends NodeComponent {

    @Override
    public void process() {
        FollowUpRuleContext context = getContextBean(FollowUpRuleContext.class);
        if (context.sessionStatus() == InterviewSessionStatus.COMPLETED) {
            context.suppress("面试状态保护节点", "面试已完成，禁止继续追问。");
            return;
        }
        context.addTrace("面试状态保护节点", false, "当前状态允许进行追问裁决。");
    }
}
