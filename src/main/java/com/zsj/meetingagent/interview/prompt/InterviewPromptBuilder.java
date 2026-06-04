package com.zsj.meetingagent.interview.prompt;

import com.zsj.meetingagent.resume.vo.ResumeResponse;
import org.springframework.stereotype.Component;

/**
 * 模拟面试 Prompt 构造器。
 * Prompt 文案使用本项目自己的表达，避免照搬参考项目，同时集中管理便于后续做版本化。
 */
@Component
public class InterviewPromptBuilder {

    public String buildQuestionPrompt(ResumeResponse resume, String jobTitle, String jobDescription, int questionCount) {
        return """
                请作为 Java 后端模拟面试官，根据候选人简历摘要和目标岗位生成面试题建议。

                简历摘要：
                %s

                目标岗位：
                %s

                岗位 JD：
                %s

                需要题目数量：
                %d

                要求：题目要围绕 Java 后端、项目经历、数据库、系统设计和问题排查能力。
                """.formatted(resume.summary(), jobTitle, blankToDefault(jobDescription, "未提供"), questionCount);
    }

    public String buildAnswerReviewPrompt(String question, String answer, String evaluationPoints) {
        return """
                请作为 Java 后端模拟面试官，评估候选人回答。

                面试题：
                %s

                考察点：
                %s

                候选人回答：
                %s

                请给出简洁反馈，重点指出回答是否命中考察点、还缺少什么。
                """.formatted(question, evaluationPoints, answer);
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
