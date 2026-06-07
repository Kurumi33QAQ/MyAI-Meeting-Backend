package com.zsj.meetingagent.interview.scoring;

/**
 * 面试回答评分结果。
 * 同时返回分数和可直接展示给用户的中文反馈，避免评分规则与反馈文案相互脱节。
 */
public record AnswerScoreResult(
        int score,
        String feedback
) {
}
