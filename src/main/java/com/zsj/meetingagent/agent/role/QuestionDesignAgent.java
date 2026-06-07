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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
        if (!hasJobContext(context)) {
            return buildResumeOnlyFallbackQuestions(context, safeEvidenceList, evidenceIds, evidenceSummary);
        }
        String jobTitle = blankToDefault(context.jobTitle(), "未指定具体岗位");
        String companyName = blankToDefault(context.companyName(), "未指定具体公司");
        String projectName = primaryProjectName(context, safeEvidenceList);
        List<String> technologies = extractTechnologySignals(context, safeEvidenceList);
        List<String> capabilities = extractJobCapabilitySignals(context, safeEvidenceList);
        List<String> baseQuestions = new ArrayList<>();
        baseQuestions.add("你这次面试目标是%s%s。请从简历里的%s开始，说明你亲自负责的模块、关键接口和最终结果。"
                .formatted(companyNamePrefix(companyName), jobTitle, projectName));
        baseQuestions.add("目标岗位关注%s。请结合%s里的%s，讲清楚你当时解决的业务问题、技术方案和验证方式。"
                .formatted(firstOrDefault(capabilities, "服务端研发能力"), projectName, firstOrDefault(technologies, "后端技术栈")));
        baseQuestions.add("请说明%s中一次你真正参与的问题排查：线上或联调时出现了什么现象，你如何定位根因，最后怎样确认修复有效？"
                .formatted(projectName));
        // 真实面试会优先围绕候选人简历里出现过的技术继续深挖，所以技术专项题要排在通用表达题前面。
        addTechnologySpecificQuestions(baseQuestions, projectName, technologies);
        baseQuestions.add("岗位需要%s。请结合你做过的%s，说明表结构、缓存 key、接口鉴权或消息流转里最关键的一个设计取舍。"
                .formatted(secondOrDefault(capabilities, "Web 后端基础"), projectName));
        baseQuestions.add("把%s里的经验迁移到%s岗位时，你会重点复用哪一项能力？请用一个具体模块证明，而不是只罗列技术名词。"
                .formatted(projectName, jobTitle));
        baseQuestions.add("请从%s中选择一个你能讲到代码层面的模块，说明请求入口、Service 处理、数据库或缓存访问，以及异常兜底。"
                .formatted(projectName));
        baseQuestions.add("目标岗位强调%s。请结合%s说明你如何保证服务稳定性，例如幂等、重试、超时、降级、日志或监控。"
                .formatted(thirdOrDefault(capabilities, "稳定性"), projectName));
        baseQuestions.add("你简历里出现了%s。请挑一个最熟悉的技术点，说明为什么选它、解决了什么问题，以及有没有替代方案。"
                .formatted(joinFirst(technologies, 4, "Spring Boot、MySQL、Redis")));
        return toAgentQuestions(pickUniqueQuestions(baseQuestions, context.questionCount()), evidenceIds, evidenceSummary, jobTitle);
    }

    private List<InterviewAgentQuestion> buildResumeOnlyFallbackQuestions(
            InterviewOrchestrationContext context,
            List<EvidenceResponse> evidenceList,
            List<String> evidenceIds,
            String evidenceSummary
    ) {
        String evidenceTopic = evidenceTopic(evidenceList);
        List<String> technologies = extractTechnologySignals(context, evidenceList);
        String projectName = primaryProjectName(context, evidenceList);
        List<String> templates = new ArrayList<>(List.of(
                "请结合简历中的“%s”，说明这段经历的业务背景、你的真实职责和最终结果。",
                "从你的简历中选择一个最熟悉的项目，解释一个关键技术选型，以及为什么没有选择其他方案。",
                "请讲一次你在简历项目中定位并解决实际问题的过程，包括现象、排查步骤、根因和验证方式。",
                "简历中哪些工作最能证明是你本人完成的？请补充具体代码模块、接口、数据结构或协作边界。",
                "请从简历经历中选择一项成果，说明你如何量化效果；如果当时没有指标，现在会如何补充。",
                "基于简历中出现的技能和项目，请说明你最薄弱的一环，以及你准备如何继续提升。"
        ));
        templates.add("请围绕%s里的%s，说明它处理的业务数据、核心流程、失败场景和你负责的代码边界。"
                .formatted(projectName, firstOrDefault(technologies, "核心模块")));
        addTechnologySpecificQuestions(templates, projectName, technologies);
        List<String> questionTexts = pickUniqueQuestions(templates.stream()
                .map(question -> question.contains("%s") ? question.formatted(evidenceTopic) : question)
                .toList(), context.questionCount());
        return questionTexts.stream()
                .map(question -> new InterviewAgentQuestion(
                        question,
                        "回答应严格基于简历事实，包含背景、职责、方案、取舍、问题定位和结果数据。",
                        "考察简历真实性、项目理解、技术思考、问题排查、表达结构和结果意识。",
                        "继续追问简历细节、个人贡献、技术取舍、问题根因和量化结果。",
                        evidenceIds,
                        evidenceSummary
                ))
                .toList();
    }

    private List<InterviewAgentQuestion> toAgentQuestions(
            List<String> questionTexts,
            List<String> evidenceIds,
            String evidenceSummary,
            String jobTitle
    ) {
        return questionTexts.stream()
                .map(question -> new InterviewAgentQuestion(
                        question,
                        "回答应包含业务背景、个人职责、技术方案、权衡理由、问题定位过程和可量化结果。",
                        "考察岗位匹配度、项目真实性、专业基础、问题排查能力、表达结构和结果意识。岗位上下文：" + jobTitle,
                        "根据回答继续追问具体技术细节、真实贡献、取舍理由、岗位 JD 关联度和数据指标。",
                        evidenceIds,
                        evidenceSummary
                ))
                .toList();
    }

    private void addTechnologySpecificQuestions(List<String> questions, String projectName, List<String> technologies) {
        String combined = String.join(" ", technologies).toLowerCase(Locale.ROOT);
        if (combined.contains("redis")) {
            questions.add("你在%s中使用了 Redis。请具体说明它保存了哪些业务数据、key 如何设计、过期策略是什么，以及为什么不用数据库直接查。"
                    .formatted(projectName));
        }
        if (combined.contains("rabbitmq")) {
            questions.add("你在%s中使用了 RabbitMQ。请说明消息从发送到消费的完整链路，以及如何处理重复消费、失败重试和消息堆积。"
                    .formatted(projectName));
        }
        if (combined.contains("websocket")) {
            questions.add("你在%s中使用了 WebSocket。请说明连接如何鉴权、用户和连接如何绑定，以及断线或多端登录时如何处理消息。"
                    .formatted(projectName));
        }
        if (combined.contains("jwt") || combined.contains("spring security") || combined.contains("rbac")) {
            questions.add("你在%s中做过鉴权和权限控制。请说明一次请求从 token 校验到角色、资源权限判断的完整链路。"
                    .formatted(projectName));
        }
        if (combined.contains("mysql") || combined.contains("mybatis")) {
            questions.add("请以%s中的一张核心业务表为例，说明字段设计、索引选择、事务边界，以及如何避免慢查询或数据不一致。"
                    .formatted(projectName));
        }
    }

    private List<String> pickUniqueQuestions(List<String> candidates, int questionCount) {
        Set<String> unique = new LinkedHashSet<>();
        for (String candidate : candidates) {
            String normalized = normalizeQuestion(candidate);
            if (StringUtils.hasText(normalized)) {
                unique.add(normalized);
            }
        }
        List<String> result = new ArrayList<>(unique);
        while (result.size() < questionCount) {
            int order = result.size() + 1;
            result.add("请补充第 " + order + " 个不同角度的项目细节：选择一个你亲自实现的模块，说明输入、处理流程、异常场景和验证结果。");
        }
        return result.subList(0, Math.min(questionCount, result.size()));
    }

    private String normalizeQuestion(String question) {
        if (question == null) {
            return "";
        }
        String normalized = question.replaceAll("\\s+", " ").trim();
        normalized = normalized.replaceAll("[。；;,.，]+$", "");
        if (!normalized.endsWith("？") && !normalized.endsWith("?")) {
            normalized += "？";
        }
        return normalized;
    }

    private String primaryProjectName(InterviewOrchestrationContext context, List<EvidenceResponse> evidenceList) {
        String source = buildContextText(context, evidenceList);
        for (String project : List.of("MyMallPlatform", "商城后台管理与用户端系统", "AI-Meeting", "码上面试")) {
            if (source.contains(project)) {
                return project;
            }
        }
        return "简历项目";
    }

    private List<String> extractTechnologySignals(InterviewOrchestrationContext context, List<EvidenceResponse> evidenceList) {
        String source = buildContextText(context, evidenceList);
        List<String> knownTechnologies = List.of(
                "Spring Boot", "Spring Security", "Sa-Token", "JWT", "MyBatis-Plus", "MyBatis",
                "MySQL", "Redis", "RabbitMQ", "WebSocket", "MongoDB", "SSE", "LiteFlow",
                "RAG", "Docker", "Vue3", "Element Plus", "OSS", "Lua", "RBAC", "BCrypt"
        );
        return knownTechnologies.stream()
                .filter(technology -> containsIgnoreCase(source, technology))
                .distinct()
                .limit(8)
                .toList();
    }

    private List<String> extractJobCapabilitySignals(InterviewOrchestrationContext context, List<EvidenceResponse> evidenceList) {
        String source = buildContextText(context, evidenceList);
        List<String> capabilities = new ArrayList<>();
        if (containsAny(source, "服务端", "后端", "服务质量", "稳定性")) {
            capabilities.add("服务端研发和稳定性");
        }
        if (containsAny(source, "协议", "架构", "存储", "缓存", "安全")) {
            capabilities.add("Web 后端协议、架构、存储、缓存和安全基础");
        }
        if (containsAny(source, "开发效率", "工具", "系统")) {
            capabilities.add("工具化和团队开发效率");
        }
        if (containsAny(source, "产品意识", "产品效果", "业务")) {
            capabilities.add("产品意识和业务结果导向");
        }
        if (containsAny(source, "Golang", "Go语言", "Go ")) {
            capabilities.add("将 Java 后端经验迁移到 Go 服务端研发");
        }
        if (capabilities.isEmpty()) {
            capabilities.add("岗位所需的后端工程能力");
        }
        return capabilities;
    }

    private String buildContextText(InterviewOrchestrationContext context, List<EvidenceResponse> evidenceList) {
        StringBuilder builder = new StringBuilder();
        if (context != null) {
            builder.append(blankToDefault(context.jobTitle(), "")).append('\n')
                    .append(blankToDefault(context.companyName(), "")).append('\n')
                    .append(blankToDefault(context.jobDescription(), "")).append('\n');
            if (context.resume() != null) {
                builder.append(blankToDefault(context.resume().summary(), "")).append('\n');
            }
        }
        if (evidenceList != null) {
            evidenceList.forEach(evidence -> builder.append(blankToDefault(evidence.sectionName(), ""))
                    .append('\n')
                    .append(blankToDefault(evidence.summary(), ""))
                    .append('\n')
                    .append(blankToDefault(evidence.content(), ""))
                    .append('\n'));
        }
        return builder.toString();
    }

    private String companyNamePrefix(String companyName) {
        if (!StringUtils.hasText(companyName) || "未指定具体公司".equals(companyName)) {
            return "";
        }
        return "“" + companyName + "”的";
    }

    private String firstOrDefault(List<String> values, String defaultValue) {
        return values.isEmpty() ? defaultValue : values.getFirst();
    }

    private String secondOrDefault(List<String> values, String defaultValue) {
        return values.size() < 2 ? defaultValue : values.get(1);
    }

    private String thirdOrDefault(List<String> values, String defaultValue) {
        return values.size() < 3 ? defaultValue : values.get(2);
    }

    private String joinFirst(List<String> values, int limit, String defaultValue) {
        if (values.isEmpty()) {
            return defaultValue;
        }
        return String.join("、", values.stream().limit(limit).toList());
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
                .map(evidence -> evidence.sectionName() + "：" + shorten(evidence.summary(), 80))
                .distinct()
                .limit(4)
                .reduce((left, right) -> left + "；" + right)
                .orElse("");
    }

    private String evidenceTopic(List<EvidenceResponse> evidenceList) {
        return evidenceList.stream()
                .findFirst()
                .map(EvidenceResponse::sectionName)
                .orElse("简历项目经历");
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        return text != null && keyword != null
                && text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && text.contains(keyword)) {
                return true;
            }
        }
        return false;
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

    private boolean hasJobContext(InterviewOrchestrationContext context) {
        return StringUtils.hasText(context.jobTitle())
                || StringUtils.hasText(context.companyName())
                || StringUtils.hasText(context.jobDescription());
    }
}
