package com.zsj.meetingagent.agent.role;

import com.zsj.meetingagent.agent.model.InterviewAgentOutput;
import com.zsj.meetingagent.agent.model.InterviewOrchestrationContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 回答评估 Agent。
 * 当前先基于后端评分和 AI 建议做结构化归纳，阶段 7.6 会把是否追问交给 LiteFlow 规则链裁决。
 */
@Component
public class AnswerReviewAgent implements InterviewAgentRole {

    @Override
    public String roleName() {
        return "回答评估 Agent";
    }

    @Override
    public InterviewAgentOutput analyze(InterviewOrchestrationContext context, List<InterviewAgentOutput> previousOutputs) {
        return new InterviewAgentOutput(
                roleName(),
                "回答评估需要同时关注考察点命中、项目真实性、量化结果和岗位匹配度。",
                "当前阶段记录评分观察，追问决策会在阶段 7.6 交给 LiteFlow 规则链。"
        );
    }

    public InterviewAgentOutput review(String question, String answer, int score, String aiFeedback) {
        String level = score >= 85 ? "回答质量较好" : score >= 70 ? "回答基本可用" : "回答需要明显补强";
        String details = "题目：" + question + "\n候选人回答：" + answer + "\nAI 建议：" + aiFeedback;
        return new InterviewAgentOutput(roleName(), level + "，得分：" + score, details);
    }
}
