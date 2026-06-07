package com.zsj.meetingagent.interview.scoring;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 默认面试回答评分实现。
 * 使用可解释规则提供稳定基础分，后续可再叠加 LLM 结构化评分和 evaluation 校准。
 */
@Service
public class DefaultAnswerScoringService implements AnswerScoringService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)?(?:%|ms|s|秒|毫秒|万|次|倍|条|个)?");

    @Override
    public AnswerScoreResult score(String question, String evaluationPoints, String answer) {
        String text = normalize(answer);
        if (isRefusalAnswer(text)) {
            return new AnswerScoreResult(
                    text.length() <= 4 ? 0 : 5,
                    "当前回答没有提供可评分的有效信息。建议先说明你是否接触过该问题，再补充相关经历、理解或排查思路。"
            );
        }

        int score = 15;
        score += lengthScore(text);
        score += containsAny(text, "负责", "参与", "实现", "设计", "开发", "主导", "优化") ? 15 : 0;
        score += containsAny(text, "因为", "因此", "考虑", "权衡", "选择", "原因", "避免", "相比") ? 10 : 0;
        score += containsAny(text, "排查", "定位", "复现", "日志", "监控", "测试", "压测", "验证") ? 10 : 0;
        score += containsTechnicalDetail(text) ? 15 : 0;
        score += NUMBER_PATTERN.matcher(text).find()
                || containsAny(text, "提升", "降低", "耗时", "QPS", "错误率", "成功率", "结果", "指标")
                ? 15 : 0;
        score += keywordOverlapScore(question, evaluationPoints, text);

        int normalizedScore = Math.min(score, 100);
        return new AnswerScoreResult(normalizedScore, buildFeedback(normalizedScore));
    }

    private boolean isRefusalAnswer(String text) {
        if (!StringUtils.hasText(text)) {
            return true;
        }
        String compact = text.replaceAll("[\\s，。,.！!？?]", "");
        return compact.length() <= 20 && containsAny(
                compact,
                "不知道",
                "不会",
                "不清楚",
                "不了解",
                "没做过",
                "没有做过",
                "不懂",
                "忘了",
                "不会回答"
        );
    }

    private int lengthScore(String text) {
        int length = text.length();
        if (length < 15) {
            return 5;
        }
        if (length < 50) {
            return 12;
        }
        if (length < 120) {
            return 20;
        }
        return 25;
    }

    private boolean containsTechnicalDetail(String text) {
        return containsAny(
                text,
                "Spring",
                "MySQL",
                "Redis",
                "MongoDB",
                "Java",
                "SQL",
                "接口",
                "索引",
                "缓存",
                "事务",
                "并发",
                "消息队列",
                "数据库",
                "算法",
                "复杂度",
                "权限",
                "RBAC",
                "鉴权"
        );
    }

    private int keywordOverlapScore(String question, String evaluationPoints, String answer) {
        String source = normalize((question == null ? "" : question) + " " + (evaluationPoints == null ? "" : evaluationPoints));
        String lowerAnswer = answer.toLowerCase(Locale.ROOT);
        int hitCount = 0;
        for (String keyword : new String[]{
                "项目", "职责", "技术", "方案", "性能", "稳定性", "权限", "数据库",
                "缓存", "并发", "算法", "测试", "排查", "结果", "岗位"
        }) {
            if (source.contains(keyword) && lowerAnswer.contains(keyword.toLowerCase(Locale.ROOT))) {
                hitCount++;
            }
        }
        return Math.min(hitCount * 3, 10);
    }

    private String buildFeedback(int score) {
        if (score >= 85) {
            return "回答较完整，能够说明个人职责、技术方案、分析过程和结果。";
        }
        if (score >= 65) {
            return "回答基本命中问题，但仍建议补充更具体的技术取舍、验证过程或量化结果。";
        }
        if (score >= 35) {
            return "回答包含少量有效信息，但缺少关键技术细节、个人贡献和结果证明。";
        }
        return "回答信息不足，暂时无法证明相关能力。建议按“背景、职责、方案、排查、结果”重新组织。";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
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
