package com.zsj.meetingagent.interview.followup.impl;

import com.zsj.meetingagent.ai.config.AiModelProperties;
import com.zsj.meetingagent.ai.dto.AiChatRequest;
import com.zsj.meetingagent.ai.service.AiChatService;
import com.zsj.meetingagent.interview.followup.FollowUpQuestionGenerator;
import com.zsj.meetingagent.interview.followup.FollowUpQuestionRequest;
import com.zsj.meetingagent.interview.prompt.InterviewPromptBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 基于大模型的追问生成器。
 * 模型只负责生成自然语言问题；具体性校验和规则兜底仍由后端掌控，避免模型返回空话或破坏面试流程。
 */
@Service
public class AiFollowUpQuestionGenerator implements FollowUpQuestionGenerator {

    private static final String FOLLOW_UP_SYSTEM_PROMPT = """
            你是中文技术面试追问官。
            你正在直接面试候选人，只能生成一条具体、可直接作答的追问，不评分、不总结、不解释。
            追问必须紧扣候选人刚才提到的技术、数据结构、模块、故障或结果，不得使用空泛套话。
            不得用“如果某公司的面试官继续追问”“面试官会问”等第三人称旁白。
            """;

    private final AiChatService aiChatService;
    private final InterviewPromptBuilder promptBuilder;
    private final AiModelProperties aiModelProperties;

    public AiFollowUpQuestionGenerator(
            AiChatService aiChatService,
            InterviewPromptBuilder promptBuilder,
            AiModelProperties aiModelProperties
    ) {
        this.aiChatService = aiChatService;
        this.promptBuilder = promptBuilder;
        this.aiModelProperties = aiModelProperties;
    }

    @Override
    public String generate(FollowUpQuestionRequest request) {
        String fallback = normalizeQuestion(request.fallbackQuestion());
        try {
            String generated = aiChatService.chat(new AiChatRequest(
                    promptBuilder.buildFollowUpQuestionPrompt(request),
                    request.sessionId(),
                    aiModelProperties.getDefaultModel(),
                    FOLLOW_UP_SYSTEM_PROMPT,
                    0.3
            )).answer();
            String normalized = normalizeQuestion(generated);
            return isConcreteQuestion(normalized, request) ? normalized : fallback;
        } catch (RuntimeException exception) {
            /*
             * 追问属于增强能力，模型短暂不可用时不能让整次答题失败。
             * 此时使用 LiteFlow 节点提供的上下文兜底问题继续面试。
             */
            return fallback;
        }
    }

    private String normalizeQuestion(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim()
                .replace("```text", "")
                .replace("```", "")
                .replaceFirst("^(追问问题|追问|问题)\\s*[:：]\\s*", "")
                .replaceAll("^[\"“”']+|[\"“”']+$", "")
                .trim();
        int lineBreak = normalized.indexOf('\n');
        if (lineBreak >= 0) {
            normalized = normalized.substring(0, lineBreak).trim();
        }
        if (!StringUtils.hasText(normalized)
                || "__FINISH__".equalsIgnoreCase(normalized)
                || normalized.contains("【本地模拟回答】")) {
            return null;
        }
        normalized = normalized.replaceAll("[。；;,.，]+$", "");
        if (!normalized.endsWith("？") && !normalized.endsWith("?")) {
            normalized += "？";
        }
        return normalized;
    }

    private boolean isConcreteQuestion(String question, FollowUpQuestionRequest request) {
        if (!StringUtils.hasText(question) || question.length() < 18 || question.length() > 140) {
            return false;
        }
        if (containsAny(
                question,
                "补充你没有展开的技术细节",
                "补充技术细节",
                "具体说明这个方案",
                "具体说明你的技术细节",
                "进一步说明这个方案",
                "结合实际展开说明",
                "说明问题现象、排查过程",
                "问题现象、排查过程、取舍和验证结果",
                "详细说说你的项目",
                "再展开说明一下",
                "还有什么要补充"
        )) {
            return false;
        }
        if (question.contains("面试官")
                || (question.contains("如果") && question.contains("继续追问"))
                || question.contains("假设你在面试")) {
            return false;
        }
        if (!mentionsContextTopicWhenAvailable(question, request)) {
            return false;
        }
        return containsAny(
                question,
                "如何", "为什么", "具体", "哪一", "什么指标", "什么场景",
                "异常", "边界", "索引", "事务", "缓存", "锁", "重试", "超时",
                "一致性", "幂等", "鉴权", "权限", "消息", "吞吐量", "响应时间",
                "错误率", "成功率", "定位", "验证", "取舍", "职责"
        );
    }

    private boolean mentionsContextTopicWhenAvailable(String question, FollowUpQuestionRequest request) {
        String[] topics = {
                "Redis", "JWT", "Spring Security", "RBAC", "MySQL", "MyBatis",
                "RabbitMQ", "Kafka", "WebSocket", "Lua", "Spring Boot", "MongoDB",
                "Elasticsearch", "Docker", "Kubernetes", "微服务", "事务", "索引",
                "缓存", "消息队列", "秒杀", "权限", "鉴权", "String", "Set",
                "Hash", "ZSet", "List", "压测", "QPS", "TPS"
        };
        String primaryContext = joinContext(request.question(), request.answer(), request.aiFeedback());
        boolean contextContainsKnownTopic = false;
        for (String topic : topics) {
            if (containsIgnoreCase(primaryContext, topic)) {
                contextContainsKnownTopic = true;
                if (containsIgnoreCase(question, topic)) {
                    return true;
                }
            }
        }
        return !contextContainsKnownTopic;
    }

    private String joinContext(String... values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                result.append(value).append('\n');
            }
        }
        return result.toString();
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        return text != null && text.toLowerCase().contains(keyword.toLowerCase());
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
