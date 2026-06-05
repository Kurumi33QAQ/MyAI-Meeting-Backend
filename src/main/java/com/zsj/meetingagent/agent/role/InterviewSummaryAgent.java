package com.zsj.meetingagent.agent.role;

import com.zsj.meetingagent.agent.model.InterviewAgentOutput;
import com.zsj.meetingagent.agent.model.InterviewOrchestrationContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 面试总结 Agent。
 * 负责把前置角色的输出汇总成可追踪结论，后续阶段会进入正式报告和 evaluation。
 */
@Component
public class InterviewSummaryAgent implements InterviewAgentRole {

    @Override
    public String roleName() {
        return "面试总结 Agent";
    }

    @Override
    public InterviewAgentOutput analyze(InterviewOrchestrationContext context, List<InterviewAgentOutput> previousOutputs) {
        String roles = previousOutputs.stream()
                .map(InterviewAgentOutput::roleName)
                .reduce((left, right) -> left + "、" + right)
                .orElse("暂无角色输出");
        return new InterviewAgentOutput(
                roleName(),
                "本次面试题由多 Agent 协同生成，已综合：" + roles,
                "后续报告会继续吸收评分、追问和岗位匹配度指标，避免只给泛泛建议。"
        );
    }
}
