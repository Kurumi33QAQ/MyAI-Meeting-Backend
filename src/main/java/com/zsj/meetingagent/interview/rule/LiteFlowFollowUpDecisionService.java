package com.zsj.meetingagent.interview.rule;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.zsj.meetingagent.common.exception.BusinessException;
import org.springframework.stereotype.Service;

/**
 * 基于 LiteFlow 的追问裁决服务实现。
 * 负责启动规则链，并把 LiteFlow 执行后的上下文转换为业务可用的 FollowUpDecision。
 */
@Service
public class LiteFlowFollowUpDecisionService implements FollowUpDecisionService {

    private static final String FOLLOW_UP_CHAIN_ID = "interviewFollowUpChain";

    private final FlowExecutor flowExecutor;

    public LiteFlowFollowUpDecisionService(FlowExecutor flowExecutor) {
        this.flowExecutor = flowExecutor;
    }

    @Override
    public FollowUpDecision decide(FollowUpRuleContext context) {
        LiteflowResponse response = flowExecutor.execute2Resp(FOLLOW_UP_CHAIN_ID, null, context);
        if (!response.isSuccess()) {
            throw new BusinessException("I0411", "追问规则链执行失败，请稍后重试");
        }
        FollowUpRuleContext resultContext = response.getContextBean(FollowUpRuleContext.class);
        if (resultContext.decision() == null) {
            throw new BusinessException("I0412", "追问规则链未产出裁决结果");
        }
        return resultContext.decision();
    }
}
