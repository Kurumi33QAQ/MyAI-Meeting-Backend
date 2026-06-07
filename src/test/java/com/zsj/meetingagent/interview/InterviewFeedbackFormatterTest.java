package com.zsj.meetingagent.interview;

import com.zsj.meetingagent.interview.support.InterviewFeedbackFormatter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 面试反馈格式化测试。
 * 验证反馈分区清晰、重复标题被清理，并且长反馈不会被截成半句话。
 */
class InterviewFeedbackFormatterTest {

    @Test
    void formatsFeedbackIntoThreeIndependentSections() {
        String result = InterviewFeedbackFormatter.formatMainFeedback(
                "回答基本命中问题。",
                "反馈：项目描述较完整，但需要补充技术选型原因。",
                "回答质量较好，得分：88"
        );

        assertThat(result)
                .contains("**规则评分**")
                .contains("**AI 面试官反馈**")
                .contains("项目描述较完整")
                .contains("**多 Agent 评估结论**")
                .contains("回答质量较好，得分：88")
                .doesNotContain("AI 面试官反馈**\n\n反馈：");
    }

    @Test
    void keepsOnlyCompleteSentencesWhenFeedbackIsTooLong() {
        String completeSentence = "这是一个完整的改进建议。";
        String longFeedback = completeSentence.repeat(80) + "这是一段会超过限制的后续内容";

        String result = InterviewFeedbackFormatter.formatMainFeedback(
                "规则评分说明。",
                longFeedback,
                "多 Agent 结论。"
        );

        assertThat(result)
                .contains("（反馈内容较长，已省略后续段落。）")
                .doesNotContain("这是一段会超过限制的后续内容");
    }
}
