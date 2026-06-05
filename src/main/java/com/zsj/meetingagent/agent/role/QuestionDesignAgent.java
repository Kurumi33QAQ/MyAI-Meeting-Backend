package com.zsj.meetingagent.agent.role;

import com.zsj.meetingagent.agent.model.InterviewAgentOutput;
import com.zsj.meetingagent.agent.model.InterviewAgentQuestion;
import com.zsj.meetingagent.agent.model.InterviewOrchestrationContext;
import com.zsj.meetingagent.rag.vo.EvidenceResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 出题设计 Agent。
 * 负责把简历分析、岗位分析和 RAG 证据合并为可落库的面试题计划。
 */
@Component
public class QuestionDesignAgent implements InterviewAgentRole {

    @Override
    public String roleName() {
        return "出题设计 Agent";
    }

    @Override
    public InterviewAgentOutput analyze(InterviewOrchestrationContext context, List<InterviewAgentOutput> previousOutputs) {
        return new InterviewAgentOutput(
                roleName(),
                "将围绕项目经历、岗位技术栈、问题排查、系统设计和表达结构生成题目。",
                "已接收前置 Agent 输出 " + previousOutputs.size() + " 条，并会优先绑定 evidenceId 便于后续 evaluation 统计。"
        );
    }

    public List<InterviewAgentQuestion> designQuestions(InterviewOrchestrationContext context) {
        List<EvidenceResponse> evidenceList = context.evidenceList() == null ? List.of() : context.evidenceList();
        List<String> evidenceIds = evidenceList.stream().map(EvidenceResponse::evidenceId).toList();
        String evidenceSummary = buildEvidenceSummary(evidenceList);
        List<String> templates = List.of(
                "结合你的简历和目标岗位 %s，请介绍一个最能证明你后端开发能力的项目，并说明你的真实职责。",
                "目标岗位要求你具备 %s 相关能力。请说明你在项目中如何使用 Spring Boot、数据库或缓存解决实际问题。",
                "如果面试官关注 %s 的系统稳定性，你会如何排查接口慢、错误率升高或数据不一致问题？",
                "请结合你过往项目，讲一次技术选型或性能优化的过程，并说明为什么这样设计。",
                "如果入职后要维护一个 AI 面试或智能问答模块，你会如何拆分后端服务、存储和异常兜底？",
                "请说明你相对 %s 的优势和短板，并给出你准备补强的具体计划。"
        );
        List<InterviewAgentQuestion> questions = new ArrayList<>();
        for (int index = 0; index < context.questionCount(); index++) {
            String topic = index % 2 == 0 ? context.jobTitle() : evidenceTopic(evidenceList);
            questions.add(new InterviewAgentQuestion(
                    templates.get(index % templates.size()).formatted(topic),
                    "回答应包含业务背景、个人职责、技术方案、权衡理由、问题定位过程和可量化结果。",
                    "考察岗位匹配度、项目真实性、Java 后端基础、问题排查能力、表达结构和结果意识。",
                    "根据回答继续追问具体技术细节、真实贡献、取舍理由和数据指标。",
                    evidenceIds,
                    evidenceSummary
            ));
        }
        return questions;
    }

    private String buildEvidenceSummary(List<EvidenceResponse> evidenceList) {
        if (evidenceList.isEmpty()) {
            return "暂无证据绑定，后续阶段会通过低置信度拒答和真实向量检索继续增强。";
        }
        return evidenceList.stream()
                .limit(3)
                .map(evidence -> evidence.sectionName() + "：" + shorten(evidence.summary(), 80))
                .reduce((left, right) -> left + "；" + right)
                .orElse("");
    }

    private String evidenceTopic(List<EvidenceResponse> evidenceList) {
        return evidenceList.stream()
                .findFirst()
                .map(EvidenceResponse::sectionName)
                .orElse("Java 后端项目");
    }

    private String shorten(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.substring(0, Math.min(trimmed.length(), maxLength));
    }
}
