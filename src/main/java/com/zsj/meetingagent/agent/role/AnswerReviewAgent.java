package com.zsj.meetingagent.agent.role;

import com.zsj.meetingagent.agent.model.InterviewAgentOutput;
import com.zsj.meetingagent.agent.model.InterviewOrchestrationContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 回答评估 Agent。
 * 基于后端规则评分和 AI 反馈生成评估摘要，是否追问由 LiteFlow 规则链独立裁决。
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
                "评估结论用于生成反馈摘要，追问决策由 LiteFlow 规则链根据分数、缺失考点和追问次数统一处理。"
        );
    }

    public InterviewAgentOutput review(String question, String answer, int score, String aiFeedback) {
        String level = score >= 85 ? "回答质量较好" : score >= 70 ? "回答基本可用" : "回答需要明显补强";
        String details = "题目：" + question + "\n候选人回答：" + answer + "\nAI 建议：" + aiFeedback;
        return new InterviewAgentOutput(roleName(), level + "，得分：" + score, details);
    }
}
