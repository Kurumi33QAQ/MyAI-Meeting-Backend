package com.zsj.meetingagent.agent.role;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.meetingagent.agent.model.InterviewAgentOutput;
import com.zsj.meetingagent.agent.model.InterviewAgentQuestion;
import com.zsj.meetingagent.agent.model.InterviewOrchestrationContext;
import com.zsj.meetingagent.rag.vo.EvidenceResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 出题设计 Agent。
 * 负责把简历分析、岗位分析和 RAG 证据合并为可落库的面试题计划。
 */
@Component
public class QuestionDesignAgent implements InterviewAgentRole {

    private final ObjectMapper objectMapper;

    public QuestionDesignAgent(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String roleName() {
        return "出题设计 Agent";
    }

    @Override
    public InterviewAgentOutput analyze(InterviewOrchestrationContext context, List<InterviewAgentOutput> previousOutputs) {
        String source = StringUtils.hasText(context.aiSuggestion()) ? "真实模型出题建议" : "本地兜底模板";
        return new InterviewAgentOutput(
                roleName(),
                "将围绕项目经历、岗位技术栈、问题排查、系统设计和表达结构生成题目，当前题目来源：" + source + "。",
                "已接收前置 Agent 输出 " + previousOutputs.size() + " 条，并会优先绑定 evidenceId 便于后续 evaluation 统计。"
        );
    }

    public List<InterviewAgentQuestion> designQuestions(InterviewOrchestrationContext context) {
        List<EvidenceResponse> evidenceList = context.evidenceList() == null ? List.of() : context.evidenceList();
        List<String> evidenceIds = evidenceList.stream().map(EvidenceResponse::evidenceId).toList();
        String evidenceSummary = buildEvidenceSummary(evidenceList);
        List<InterviewAgentQuestion> aiQuestions = parseAiQuestions(context.aiSuggestion(), context.questionCount(), evidenceIds, evidenceSummary);
        if (!aiQuestions.isEmpty()) {
            return completeQuestionCount(aiQuestions, context, evidenceIds, evidenceSummary);
        }
        return buildFallbackQuestions(context, evidenceList, evidenceIds, evidenceSummary);
    }

    private List<InterviewAgentQuestion> parseAiQuestions(String aiSuggestion, int questionCount, List<String> evidenceIds, String evidenceSummary) {
        if (!StringUtils.hasText(aiSuggestion)) {
            return List.of();
        }
        try {
            /*
             * 真实模型有时会把 JSON 包在 ```json 代码块里。
             * 先抽取 JSON 对象再交给 Jackson，避免因为 Markdown 包裹导致整批题目回退到模板。
             */
            JsonNode root = objectMapper.readTree(extractJsonObject(aiSuggestion));
            JsonNode questionsNode = root.path("questions");
            if (!questionsNode.isArray()) {
                return List.of();
            }

            List<InterviewAgentQuestion> questions = new ArrayList<>();
            for (JsonNode node : questionsNode) {
                String question = text(node, "question");
                if (!StringUtils.hasText(question)) {
                    continue;
                }
                questions.add(new InterviewAgentQuestion(
                        question,
                        blankToDefault(text(node, "referenceAnswer"), "回答应包含业务背景、个人职责、技术方案、权衡理由和结果数据。"),
                        blankToDefault(text(node, "evaluationPoints"), "考察岗位匹配度、项目真实性、Java 后端基础、问题排查能力和表达结构。"),
                        blankToDefault(text(node, "followUpDirection"), "根据回答继续追问具体技术细节、真实贡献、取舍理由和数据指标。"),
                        evidenceIds,
                        evidenceSummary
                ));
                if (questions.size() >= questionCount) {
                    break;
                }
            }
            return questions;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<InterviewAgentQuestion> completeQuestionCount(
            List<InterviewAgentQuestion> aiQuestions,
            InterviewOrchestrationContext context,
            List<String> evidenceIds,
            String evidenceSummary
    ) {
        if (aiQuestions.size() >= context.questionCount()) {
            return aiQuestions;
        }
        List<InterviewAgentQuestion> completed = new ArrayList<>(aiQuestions);
        List<InterviewAgentQuestion> fallback = buildFallbackQuestions(context, context.evidenceList(), evidenceIds, evidenceSummary);
        for (InterviewAgentQuestion question : fallback) {
            if (completed.size() >= context.questionCount()) {
                break;
            }
            completed.add(question);
        }
        return completed;
    }

    private List<InterviewAgentQuestion> buildFallbackQuestions(
            InterviewOrchestrationContext context,
            List<EvidenceResponse> evidenceList,
            List<String> evidenceIds,
            String evidenceSummary
    ) {
        List<EvidenceResponse> safeEvidenceList = evidenceList == null ? List.of() : evidenceList;
        String jobTitle = blankToDefault(context.jobTitle(), "Java 后端开发");
        String companyName = blankToDefault(context.companyName(), "目标公司");
        String jdTopic = extractJdTopic(context.jobDescription());
        String evidenceTopic = evidenceTopic(safeEvidenceList);
        List<String> templates = List.of(
                "你面试的是 %s 的 %s。请结合简历中最相关的项目，说明你承担的后端职责、核心接口设计和最终效果。",
                "岗位 JD 重点提到“%s”。请讲一个你在项目中用 Java、Spring Boot、数据库或缓存解决类似问题的经历。",
                "结合证据章节“%s”，请说明这个经历里最能证明你岗位匹配度的技术细节，以及你如何验证方案有效。",
                "如果 %s 的面试官追问系统稳定性，你会如何排查接口变慢、错误率升高或数据不一致？请结合你做过的项目回答。",
                "请基于目标岗位 %s，说明你在数据库设计、事务一致性、缓存使用或接口性能优化方面最有说服力的一次实践。",
                "如果入职后要维护一个 AI 面试或智能问答模块，请结合你的项目经历说明你会如何拆分后端服务、存储和异常兜底。"
        );
        List<InterviewAgentQuestion> questions = new ArrayList<>();
        for (int index = 0; index < context.questionCount(); index++) {
            String questionText = switch (index % templates.size()) {
                case 0 -> templates.get(0).formatted(companyName, jobTitle);
                case 1 -> templates.get(1).formatted(jdTopic);
                case 2 -> templates.get(2).formatted(evidenceTopic);
                case 3 -> templates.get(3).formatted(companyName);
                case 4 -> templates.get(4).formatted(jobTitle);
                default -> templates.get(5);
            };
            questions.add(new InterviewAgentQuestion(
                    questionText,
                    "回答应包含业务背景、个人职责、技术方案、权衡理由、问题定位过程和可量化结果。",
                    "考察岗位匹配度、项目真实性、Java 后端基础、问题排查能力、表达结构和结果意识。岗位上下文：" + jobTitle,
                    "根据回答继续追问具体技术细节、真实贡献、取舍理由、岗位 JD 关联度和数据指标。",
                    evidenceIds,
                    evidenceSummary
            ));
        }
        return questions;
    }

    private String extractJsonObject(String value) {
        String trimmed = value.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return "";
        }
        return value.asText("").trim();
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

    private String extractJdTopic(String jobDescription) {
        if (!StringUtils.hasText(jobDescription)) {
            return "Java 后端、Spring Boot、数据库和接口设计";
        }
        String normalized = jobDescription.replace('\n', ' ').replace('\r', ' ').trim();
        return shorten(normalized, 80);
    }

    private String blankToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String shorten(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.substring(0, Math.min(trimmed.length(), maxLength));
    }
}
