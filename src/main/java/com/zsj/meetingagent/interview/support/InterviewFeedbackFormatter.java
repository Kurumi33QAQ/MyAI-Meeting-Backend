package com.zsj.meetingagent.interview.support;

import org.springframework.util.StringUtils;

/**
 * 面试反馈文本格式化工具。
 * 负责清理模型重复标题、按完整句子控制长度，并将规则评分、AI 反馈和多 Agent 结论分区展示。
 */
public final class InterviewFeedbackFormatter {

    private static final int MAX_AI_FEEDBACK_LENGTH = 800;

    private InterviewFeedbackFormatter() {
    }

    public static String formatMainFeedback(
            String ruleFeedback,
            String aiFeedback,
            String agentSummary
    ) {
        return """
                **规则评分**

                %s

                **AI 面试官反馈**

                %s

                **多 Agent 评估结论**

                %s
                """.formatted(
                normalizeSection(ruleFeedback, "暂无规则评分说明。"),
                completeSentenceSummary(aiFeedback, MAX_AI_FEEDBACK_LENGTH),
                normalizeSection(agentSummary, "暂无多 Agent 补充结论。")
        ).trim();
    }

    public static String formatFollowUpFeedback(String ruleFeedback, String aiFeedback) {
        return """
                **追问评分**

                %s

                **AI 面试官反馈**

                %s
                """.formatted(
                normalizeSection(ruleFeedback, "暂无追问评分说明。"),
                completeSentenceSummary(aiFeedback, MAX_AI_FEEDBACK_LENGTH)
        ).trim();
    }

    static String completeSentenceSummary(String value, int maxLength) {
        String normalized = normalizeAiHeading(value);
        if (normalized.length() <= maxLength) {
            return normalized;
        }

        int sentenceEnd = findLastSentenceEnd(normalized, maxLength);
        if (sentenceEnd < maxLength / 2) {
            /*
             * 模型偶尔会输出没有标点的长段落。此时宁可保留完整内容，
             * 也不能像旧实现一样把中文词语截成半句，造成用户误解。
             */
            return normalized;
        }
        return normalized.substring(0, sentenceEnd + 1)
                + "\n\n（反馈内容较长，已省略后续段落。）";
    }

    private static String normalizeAiHeading(String value) {
        String normalized = normalizeSection(value, "模型暂未返回补充反馈。");
        return normalized.replaceFirst(
                "^(?i)(AI\\s*建议|AI\\s*反馈|反馈|建议)\\s*[:：]\\s*",
                ""
        ).trim();
    }

    private static String normalizeSection(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private static int findLastSentenceEnd(String value, int maxLength) {
        int lastIndex = -1;
        int upperBound = Math.min(value.length(), maxLength);
        for (int index = 0; index < upperBound; index++) {
            char current = value.charAt(index);
            if (current == '。'
                    || current == '！'
                    || current == '？'
                    || current == '；'
                    || current == '\n') {
                lastIndex = index;
            }
        }
        return lastIndex;
    }
}
