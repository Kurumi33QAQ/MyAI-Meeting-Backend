package com.zsj.meetingagent.interview.prompt;

import com.zsj.meetingagent.rag.vo.EvidenceResponse;
import com.zsj.meetingagent.resume.vo.ResumeResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 模拟面试 Prompt 构造器。
 * Prompt 文案使用本项目自己的表达，避免照搬参考项目，同时集中管理便于后续做版本化。
 */
@Component
public class InterviewPromptBuilder {

    public String buildQuestionPrompt(
            ResumeResponse resume,
            String jobTitle,
            String companyName,
            String jobDescription,
            int questionCount,
            List<EvidenceResponse> evidenceList
    ) {
        return """
                请作为 Java 后端模拟面试官，根据候选人简历摘要和目标岗位生成面试题建议。

                简历摘要：
                %s

                目标岗位：
                %s

                目标公司：
                %s

                岗位 JD：
                %s

                可引用证据：
                %s

                需要题目数量：
                %d

                要求：
                1. 题目要围绕 Java 后端、项目经历、数据库、系统设计、问题排查、目标公司和岗位 JD。
                2. 优先结合可引用证据出题，不要编造简历或 JD 中没有的信息。
                3. 每道题必须尽量不同，不要重复“介绍一个项目”这种泛问题。
                4. 只输出 JSON，不要输出 Markdown，不要解释。
                5. JSON 格式必须如下：
                {
                  "questions": [
                    {
                      "question": "面试题正文",
                      "referenceAnswer": "参考回答要点",
                      "evaluationPoints": "考察点",
                      "followUpDirection": "追问方向"
                    }
                  ]
                }
                6. 如果证据不足，请在题目中询问候选人澄清事实，而不是硬编细节。
                """.formatted(
                resume.summary(),
                jobTitle,
                blankToDefault(companyName, "未填写"),
                blankToDefault(jobDescription, "未提供"),
                buildEvidenceText(evidenceList),
                questionCount
        );
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

    private String buildEvidenceText(List<EvidenceResponse> evidenceList) {
        if (evidenceList == null || evidenceList.isEmpty()) {
            return "未召回到证据。";
        }
        StringBuilder builder = new StringBuilder();
        for (EvidenceResponse evidence : evidenceList) {
            builder.append("- evidenceId=")
                    .append(evidence.evidenceId())
                    .append("，类型=")
                    .append(evidence.documentType())
                    .append("，章节=")
                    .append(evidence.sectionName())
                    .append("，内容=")
                    .append(evidence.content())
                    .append('\n');
        }
        return builder.toString();
    }
}
