package com.zsj.meetingagent.agent.role;

import com.zsj.meetingagent.agent.model.InterviewAgentOutput;
import com.zsj.meetingagent.agent.model.InterviewOrchestrationContext;
import com.zsj.meetingagent.rag.vo.EvidenceResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 岗位上下文 Agent。
 * 负责分析目标岗位、公司和 JD，把出题方向从“泛 Java 面试”拉回到具体岗位要求。
 */
@Component
public class JobContextAgent implements InterviewAgentRole {

    @Override
    public String roleName() {
        return "岗位上下文 Agent";
    }

    @Override
    public InterviewAgentOutput analyze(InterviewOrchestrationContext context, List<InterviewAgentOutput> previousOutputs) {
        if (!hasJobContext(context)) {
            return new InterviewAgentOutput(
                    roleName(),
                    "用户未填写目标岗位、公司或 JD，本轮不假设岗位方向。",
                    "跳过岗位定向分析，后续出题只使用简历项目、技能和可引用简历证据。"
            );
        }
        List<EvidenceResponse> jobEvidence = context.evidenceList().stream()
                .filter(evidence -> "JOB_DESCRIPTION".equalsIgnoreCase(evidence.documentType())
                        || "JOB_MARKET_INTELLIGENCE".equalsIgnoreCase(evidence.documentType()))
                .toList();
        String company = StringUtils.hasText(context.companyName()) ? context.companyName() : "未指定公司";
        String jobTitle = StringUtils.hasText(context.jobTitle()) ? context.jobTitle() : "用户描述的目标方向";
        String summary = "目标是 " + company + " 的 " + jobTitle + "，题目应贴近岗位职责、技术栈和加分项。";
        String details = jobEvidence.isEmpty()
                ? "用户没有提供足够 JD 证据，当前只能结合岗位名称和简历出题。"
                : "命中 JD 证据 " + jobEvidence.size() + " 条，建议优先覆盖：" + jobEvidence.stream()
                .map(EvidenceResponse::sectionName)
                .distinct()
                .reduce((left, right) -> left + "、" + right)
                .orElse("岗位要求");
        return new InterviewAgentOutput(roleName(), summary, details);
    }

    private boolean hasJobContext(InterviewOrchestrationContext context) {
        return StringUtils.hasText(context.jobTitle())
                || StringUtils.hasText(context.companyName())
                || StringUtils.hasText(context.jobDescription());
    }
}
