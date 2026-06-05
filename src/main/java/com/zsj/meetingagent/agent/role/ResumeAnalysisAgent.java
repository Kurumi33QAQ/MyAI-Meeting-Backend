package com.zsj.meetingagent.agent.role;

import com.zsj.meetingagent.agent.model.InterviewAgentOutput;
import com.zsj.meetingagent.agent.model.InterviewOrchestrationContext;
import com.zsj.meetingagent.rag.vo.EvidenceResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 简历分析 Agent。
 * 负责从简历摘要和简历 evidence 中提取候选人的技能、项目线索和需要追问的薄弱点。
 */
@Component
public class ResumeAnalysisAgent implements InterviewAgentRole {

    @Override
    public String roleName() {
        return "简历分析 Agent";
    }

    @Override
    public InterviewAgentOutput analyze(InterviewOrchestrationContext context, List<InterviewAgentOutput> previousOutputs) {
        List<EvidenceResponse> resumeEvidence = context.evidenceList().stream()
                .filter(evidence -> "RESUME".equalsIgnoreCase(evidence.documentType()))
                .toList();
        String skillSignal = containsAny(context.resume().summary(), "Spring", "Spring Boot", "MySQL", "Redis")
                ? "简历中出现 Java 后端常见技术栈，可优先追问真实项目职责和技术取舍。"
                : "简历技术栈信号不够明确，需要先追问候选人最熟悉的后端项目。";
        String evidenceSummary = resumeEvidence.isEmpty()
                ? "暂无简历 evidence，后续应补充真实 PDF 解析和结构化简历字段。"
                : "命中简历证据 " + resumeEvidence.size() + " 条，重点章节：" + resumeEvidence.stream()
                .map(EvidenceResponse::sectionName)
                .distinct()
                .reduce((left, right) -> left + "、" + right)
                .orElse("未分类");
        return new InterviewAgentOutput(roleName(), skillSignal, evidenceSummary);
    }

    private boolean containsAny(String text, String... keywords) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
