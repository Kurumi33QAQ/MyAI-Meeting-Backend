package com.zsj.meetingagent.interview.rule;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 上下文追问问题工厂。
 * 根据原问题、候选人回答和 AI 反馈选择具体技术切入点，避免生成“请补充技术细节”这类无法直接作答的问题。
 */
public final class ContextualFollowUpQuestionFactory {

    private static final Pattern METRIC_PATTERN = Pattern.compile(
            "\\d+(?:\\.\\d+)?(?:%|ms|s|秒|毫秒|万|次|倍|条|个|QPS|TPS)?"
    );

    private ContextualFollowUpQuestionFactory() {
    }

    public static String forLowScore(FollowUpRuleContext context) {
        String answer = normalize(context.answer());
        if (isRefusalAnswer(answer)) {
            return "如果你暂时没有完整答案，请从你最熟悉的一个项目模块开始：这个模块解决了什么问题、你亲自负责什么、最终实现了什么效果？";
        }
        return buildTechnicalQuestion(buildContextText(context), context.followUpDirection());
    }

    public static String forAiFeedback(FollowUpRuleContext context) {
        String feedback = normalize(context.aiFeedback());
        String contextText = buildContextText(context);

        if (containsAny(feedback, "职责", "贡献", "亲自", "真实性")) {
            return "请从刚才提到的项目中选择一个你亲自负责的模块，具体说明你的代码职责、遇到的难点，以及最终如何验证功能正确。";
        }
        if (containsAny(feedback, "量化", "指标", "结果", "效果")) {
            return buildMetricQuestion(contextText);
        }
        if (containsAny(feedback, "选型", "理由", "权衡", "为什么")) {
            return buildTechnicalQuestion(contextText, context.followUpDirection());
        }
        if (containsAny(feedback, "岗位", "匹配", "关联")) {
            return "结合刚才提到的项目，请选择一项能迁移到目标岗位的能力，说明它解决过什么实际问题，以及你为什么认为它与该岗位相关。";
        }
        return buildTechnicalQuestion(contextText, context.followUpDirection());
    }

    public static String buildMetricQuestion(String answer) {
        String technology = primaryTechnology(answer);
        if (StringUtils.hasText(technology)) {
            return "你提到了 " + technology + "，请说明引入前后的可验证变化，例如响应时间、吞吐量、错误率、资源占用或业务成功率。";
        }
        return "请为刚才的方案补充一个可验证结果：优化前是什么状态，采用方案后哪项指标发生了什么变化？";
    }

    public static boolean containsMetric(String answer) {
        String normalized = normalize(answer);
        return METRIC_PATTERN.matcher(normalized).find()
                || containsAny(normalized, "响应时间", "查询耗时", "错误率", "成功率", "吞吐量", "QPS", "TPS", "提升", "降低");
    }

    private static String buildTechnicalQuestion(String answer, String followUpDirection) {
        if (containsAny(answer, "Redis", "redis")
                && containsAny(answer, "String", "string")
                && containsAny(answer, "Set", "set")) {
            return "你提到 Redis 使用了 String 和 Set，请分别说明它们保存什么业务数据、key 如何设计，以及为什么没有选择 Hash 或 ZSet？";
        }
        if (containsAny(answer, "RabbitMQ", "rabbitmq", "消息队列", "秒杀")) {
            return "你提到使用 RabbitMQ 和 Lua 处理秒杀高并发，请具体说明库存预扣、消息重复消费和最终一致性分别如何保证？";
        }
        if (containsAny(answer, "Redis", "redis", "黑名单")) {
            return "你提到使用 Redis 管理登录鉴权黑名单，请具体说明 key 与过期时间如何设计，以及怎样确保被注销的 token 立即失效？";
        }
        if (containsAny(answer, "WebSocket", "websocket", "实时推送")) {
            return "你提到使用 WebSocket 推送订单和秒杀消息，请具体说明用户连接如何鉴权、连接与用户如何绑定，以及断线后如何处理消息？";
        }
        if (containsAny(answer, "RBAC", "JWT", "Spring Security", "security", "权限")) {
            return "你提到 JWT、Spring Security 和 RBAC，请具体说明一次请求从 token 校验到角色、资源权限判断的完整链路。";
        }
        if (containsAny(answer, "MySQL", "Mysql", "mysql", "MyBatis", "数据库")) {
            return "你提到负责数据库设计，请选择一张核心业务表，说明表结构、索引设计、事务边界，以及如何避免慢查询或数据不一致。";
        }

        String direction = normalize(followUpDirection);
        if (StringUtils.hasText(direction)) {
            return "请围绕“" + shorten(direction, 40) + "”选择一个具体实现点，说明当时的问题、你的方案、技术取舍和验证结果。";
        }
        return "请从刚才的回答中选择一个你亲自实现的模块，具体说明输入、处理流程、异常场景和最终验证结果。";
    }

    private static String buildContextText(FollowUpRuleContext context) {
        return String.join(
                "\n",
                normalize(context.question()),
                normalize(context.answer()),
                normalize(context.aiFeedback()),
                normalize(context.evaluationPoints()),
                normalize(context.followUpDirection())
        );
    }

    private static String primaryTechnology(String answer) {
        for (String technology : new String[]{
                "RabbitMQ", "Redis", "WebSocket", "Spring Security", "JWT", "RBAC", "MySQL", "MyBatis"
        }) {
            if (answer.toLowerCase().contains(technology.toLowerCase())) {
                return technology;
            }
        }
        if (answer.contains("数据库")) {
            return "数据库方案";
        }
        return "";
    }

    private static boolean isRefusalAnswer(String answer) {
        String compact = answer.replaceAll("[\\s，。,.！!？?]", "");
        return compact.length() <= 20 && containsAny(
                compact,
                "不知道",
                "不会",
                "不清楚",
                "不了解",
                "没做过",
                "不懂",
                "忘了"
        );
    }

    private static String shorten(String value, int maxLength) {
        String normalized = normalize(value);
        return normalized.substring(0, Math.min(normalized.length(), maxLength));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
