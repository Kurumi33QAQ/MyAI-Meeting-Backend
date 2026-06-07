package com.zsj.meetingagent.interview.followup;

import java.util.List;

/**
 * 追问生成上下文。
 * 汇总简历、目标岗位、本轮问答、评分反馈和规则兜底问题，让追问能够沿候选人真实项目逐步深挖。
 */
public record FollowUpQuestionRequest(
        String sessionId,
        String resumeSummary,
        String jobTitle,
        String companyName,
        String jobDescription,
        String question,
        String answer,
        String evaluationPoints,
        String followUpDirection,
        String aiFeedback,
        String fallbackQuestion,
        List<String> previousFollowUps
) {
    public FollowUpQuestionRequest(
            String sessionId,
            String resumeSummary,
            String jobTitle,
            String companyName,
            String jobDescription,
            String question,
            String answer,
            String evaluationPoints,
            String followUpDirection,
            String aiFeedback,
            String fallbackQuestion
    ) {
        this(
                sessionId,
                resumeSummary,
                jobTitle,
                companyName,
                jobDescription,
                question,
                answer,
                evaluationPoints,
                followUpDirection,
                aiFeedback,
                fallbackQuestion,
                List.of()
        );
    }
}
