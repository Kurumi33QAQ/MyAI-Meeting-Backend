package com.zsj.meetingagent.interview.rule;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 上下文追问问题工厂。
 * 当规则链需要兜底生成追问时，根据原问题、候选人回答和 AI 反馈选择具体技术切入点，避免生成过于宽泛的问题。
 */
public final class ContextualFollowUpQuestionFactory {

    private static final Pattern METRIC_PATTERN = Pattern.compile(
            "\\d+(?:\\.\\d+)?(?:%|ms|s|秒|毫秒|万次|倍|条|个|QPS|TPS)?"
    );

    private ContextualFollowUpQuestionFactory() {
    }

    public static String forLowScore(FollowUpRuleContext context) {
        String answer = normalize(context.answer());
        if (isRefusalAnswer(answer)) {
            return buildRefusalRecoveryQuestion(context);
        }
        return buildTechnicalQuestion(buildContextText(context), context.followUpDirection());
    }

    public static String forAiFeedback(FollowUpRuleContext context) {
        String feedback = normalize(context.aiFeedback());
        String contextText = buildContextText(context);

        if (containsAny(feedback, "职责", "贡献", "亲自", "真实性")) {
            return "请围绕刚才这道题，补充一个你亲自改过的代码入口：它属于哪个模块、接口或方法名是什么、你改动了哪段逻辑、上线后如何确认结果？";
        }
        if (containsAny(feedback, "量化", "指标", "结果", "效果")) {
            return buildMetricQuestion(contextText);
        }
        if (containsAny(feedback, "选型", "理由", "权衡", "为什么")) {
            return buildTechnicalQuestion(contextText, context.followUpDirection());
        }
        if (containsAny(feedback, "岗位", "匹配", "关联")) {
            return buildRoleMatchQuestion(contextText);
        }
        return buildTechnicalQuestion(contextText, context.followUpDirection());
    }

    public static String buildMetricQuestion(String answer) {
        String technology = primaryTechnology(answer);
        if (StringUtils.hasText(technology)) {
            return "你提到了 " + technology + "，请补充一个可验证结果：优化前的具体问题是什么，优化后哪个指标发生变化，比如响应时间、吞吐量、错误率或业务成功率？";
        }
        return "请为刚才的方案补充一个可验证结果：优化前是什么状态，采用方案后哪项指标发生了什么变化？";
    }

    public static boolean containsMetric(String answer) {
        String normalized = normalize(answer);
        return METRIC_PATTERN.matcher(normalized).find()
                || containsAny(normalized, "响应时间", "查询耗时", "错误率", "成功率", "吞吐量", "QPS", "TPS", "提升", "降低");
    }

    private static String buildRefusalRecoveryQuestion(FollowUpRuleContext context) {
        String contextText = buildContextText(context);
        if (containsAny(contextText, "JWT", "token", "Token", "Spring Security", "Sa-Token", "鉴权")) {
            return "刚才问到登录鉴权链路。请你先回答一个最小闭环：token 在哪个过滤器或拦截器里校验，校验失败返回什么状态码，前端收到后如何处理？";
        }
        if (containsAny(contextText, "Redis", "redis", "缓存", "黑名单")) {
            return "刚才问到 Redis。请你结合项目里的一个具体场景说明：Redis key 怎么命名，value 存什么，过期时间如何设置，为什么这样设计？";
        }
        if (containsAny(contextText, "RabbitMQ", "rabbitmq", "消息队列", "秒杀")) {
            return "刚才问到高并发或消息队列。请你按“请求进入、库存扣减、消息发送、消费落库、异常补偿”的顺序，说明你项目里的处理链路。";
        }
        if (containsAny(contextText, "WebSocket", "websocket", "实时推送")) {
            return "刚才问到 WebSocket。请你说明连接建立时如何鉴权，服务端如何把连接绑定到用户，断线后未读消息如何处理？";
        }
        if (containsAny(contextText, "MySQL", "mysql", "MyBatis", "数据库", "表结构")) {
            return "刚才问到数据库设计。请你选一张项目核心表，说明字段设计、索引设计、事务边界，以及怎样避免慢查询或数据不一致。";
        }
        return buildRoleMatchQuestion(contextText);
    }

    private static String buildTechnicalQuestion(String answer, String followUpDirection) {
        if (containsAny(answer, "JWT", "token", "Token", "Spring Security", "Sa-Token", "RBAC", "鉴权", "权限")) {
            return "请把一次受保护接口请求讲完整：token 从请求头进入后经过哪个认证组件，如何解析用户身份，怎样判断角色和资源权限，失败时返回什么结果？";
        }
        if (containsAny(answer, "Redis", "redis")
                && containsAny(answer, "String", "string", "Set", "set", "Hash", "ZSet")) {
            return "你提到了 Redis 数据结构，请分别说明 String、Set 或 Hash 在项目里保存什么业务数据，key 如何设计，以及为什么没有选择其他结构？";
        }
        if (containsAny(answer, "Redis", "redis", "黑名单", "缓存")) {
            return "请结合你项目里的 Redis 场景说明：key 的命名规则、过期时间、缓存穿透或击穿风险，以及你如何验证 Redis 逻辑生效？";
        }
        if (containsAny(answer, "RabbitMQ", "rabbitmq", "消息队列", "秒杀")) {
            return "你提到了 RabbitMQ 或秒杀场景，请具体说明库存预扣、消息重复消费和最终一致性分别如何保证？";
        }
        if (containsAny(answer, "WebSocket", "websocket", "实时推送")) {
            return "你提到了 WebSocket，请具体说明用户连接如何鉴权、连接与用户如何绑定，以及断线后消息如何补偿？";
        }
        if (containsAny(answer, "MySQL", "Mysql", "mysql", "MyBatis", "数据库")) {
            return "你提到了数据库设计，请选择一张核心业务表，说明表结构、索引设计、事务边界，以及如何定位慢查询。";
        }

        String direction = normalize(followUpDirection);
        if (StringUtils.hasText(direction)) {
            return "请围绕“" + shorten(direction, 40) + "”选择一个具体实现点，说明当时的问题、你的方案、技术取舍和验证结果。";
        }
        return buildRoleMatchQuestion(answer);
    }

    private static String buildRoleMatchQuestion(String contextText) {
        String technology = primaryTechnology(contextText);
        if (StringUtils.hasText(technology)) {
            return "目标岗位会关注 " + technology + " 的真实使用能力。请结合你的项目说明：你在哪个模块用了它，解决了什么问题，你负责哪部分，结果如何验证？";
        }
        return "请从简历里的一个具体项目模块切入回答：模块解决了什么业务问题、你亲自负责哪段后端逻辑、遇到什么难点、最终如何验证结果？";
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
                "RabbitMQ", "Redis", "WebSocket", "Spring Security", "Sa-Token", "JWT", "RBAC", "MySQL", "MyBatis"
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
        String compact = answer.replaceAll("[\\s，。,.？?！!]", "");
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
