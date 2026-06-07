package com.zsj.meetingagent.interview.prompt;

import com.zsj.meetingagent.rag.vo.EvidenceResponse;
import com.zsj.meetingagent.resume.vo.ResumeResponse;
import com.zsj.meetingagent.interview.followup.FollowUpQuestionRequest;
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
        if (!hasJobContext(jobTitle, companyName, jobDescription)) {
            return """
                    请作为资深技术面试官，只根据候选人简历和可引用证据生成模拟面试题。

                    简历摘要：
                    %s

                    可引用证据：
                    %s

                    需要题目数量：
                    %d

                    要求：
                    1. 用户没有填写目标岗位、公司或 JD，不要假设候选人应聘 Java 后端或任何具体岗位。
                    2. 优先追问简历里真实出现的项目、实习、技能、职责、技术选型、问题排查和结果数据。
                    3. 每道题必须尽量绑定不同简历细节，避免重复“介绍一个项目”等泛问题。
                    4. 每道题应控制在 60 到 140 个中文字符，必须像面试官直接向候选人提问，不能写成题目分析说明。
                    5. 只输出严格 JSON 对象，不要输出 Markdown，不要使用 ```json 代码块，不要解释。
                    6. questions 数组长度必须等于“需要题目数量”。
                    7. JSON 格式必须如下：
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
                    8. 简历证据不足时，要求候选人澄清，不得编造经历。
                    """.formatted(
                    resume.summary(),
                    buildEvidenceText(evidenceList),
                    questionCount
            );
        }
        return """
                请作为资深技术面试官，根据候选人简历摘要、目标岗位和公开岗位情报生成面试题建议。

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
                1. 题目要围绕候选人真实简历、目标岗位、目标公司、岗位 JD 和公开岗位情报。
                2. 优先结合可引用证据出题，不要编造简历或 JD 中没有的信息。
                3. 每道题必须尽量不同，不要重复“介绍一个项目”这种泛问题。
                4. 如果证据中包含 JOB_MARKET_INTELLIGENCE，分析公开招聘要求和面试经验中反复出现的能力点，但不要把未经证实的网页内容描述成公司官方要求。
                5. 每道题应控制在 60 到 140 个中文字符，必须结合至少一个具体实体，例如项目名、技术栈、岗位能力点或证据章节。
                6. 问题必须像面试官直接向候选人提问，不要出现“如果某公司的面试官继续追问”“用户提供的岗位信息重点提到”等旁白。
                7. 只输出严格 JSON 对象，不要输出 Markdown，不要使用 ```json 代码块，不要解释。
                8. questions 数组长度必须等于“需要题目数量”。
                9. JSON 格式必须如下：
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
                10. 如果证据不足，请在题目中询问候选人澄清事实，而不是硬编细节。
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
                请作为中文友好的技术模拟面试官，评估候选人回答。

                面试题：
                %s

                考察点：
                %s

                候选人回答：
                %s

                请给出简洁、完整的中文反馈，并遵守以下要求：
                1. 先说明回答做得好的地方。
                2. 再列出最多 3 个需要补充的关键点。
                3. 每个要点必须是完整句子，不能以半句话结束。
                4. 不要输出“反馈：”“AI 建议：”等标题，后端会统一添加分区标题。
                5. 总长度控制在 500 个中文字符以内。
                """.formatted(question, evaluationPoints, answer);
    }

    public String buildFollowUpQuestionPrompt(FollowUpQuestionRequest request) {
        return """
                你正在直接担任本次中文技术模拟面试的面试官，请根据上下文继续追问候选人。

                候选人简历摘要：
                %s

                目标岗位：
                %s

                目标公司：
                %s

                岗位描述：
                %s

                当前题目：
                %s

                候选人回答：
                %s

                考察点：
                %s

                预设追问方向：
                %s

                评分反馈：
                %s

                后端规则给出的兜底追问：
                %s

                前序追问链：
                %s

                输出要求：
                1. 只输出一条中文问句，以问号结尾，不输出标题、评分、答案、JSON 或 Markdown。
                2. 直接向候选人提问。禁止出现“如果某公司的面试官继续追问”“假设你在面试”“面试官会问”等旁白。
                3. 问题长度控制在 35 到 100 个中文字符。
                4. 必须复用回答或当前题目中的具体技术实体，例如 Redis、String、Set、Hash、JWT、MySQL、RabbitMQ 或 WebSocket。
                5. 简历和岗位信息只用于判断深挖方向，不要把公司名称生硬写进问题，不要冒充公司官方面试题。
                6. 每次只深挖一个维度：数据结构选型、实现细节、边界条件、故障排查、性能指标或技术取舍。
                7. 优先沿候选人刚才的回答向下一层追问。例如候选人说用了 Redis String 和 Set，就追问各自保存什么业务数据、key 如何设计，或为何不选 Hash/ZSet；不要重新问整个项目流程。
                8. 禁止输出“请补充你没有展开的技术细节”“请详细说说”“说明问题现象、排查过程和结果”等可套用于任何项目的泛化问题。
                9. 如果回答信息很少，应给出明确作答范围，而不是让候选人自行猜测方向。
                10. 如果前序追问链不为空，新问题必须比上一轮更具体，不能重复已经问过的点。
                """.formatted(
                blankToDefault(request.resumeSummary(), "未提供"),
                blankToDefault(request.jobTitle(), "未填写，本次只依据简历和回答"),
                blankToDefault(request.companyName(), "未填写"),
                blankToDefault(request.jobDescription(), "未填写"),
                blankToDefault(request.question(), "未提供"),
                blankToDefault(request.answer(), "未提供"),
                blankToDefault(request.evaluationPoints(), "未提供"),
                blankToDefault(request.followUpDirection(), "未提供"),
                blankToDefault(request.aiFeedback(), "未提供"),
                blankToDefault(request.fallbackQuestion(), "未提供"),
                buildPreviousFollowUpText(request.previousFollowUps())
        );
    }

    private String buildPreviousFollowUpText(List<String> previousFollowUps) {
        if (previousFollowUps == null || previousFollowUps.isEmpty()) {
            return "无";
        }
        return String.join("\n", previousFollowUps);
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

    private boolean hasJobContext(String jobTitle, String companyName, String jobDescription) {
        return hasText(jobTitle) || hasText(companyName) || hasText(jobDescription);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
