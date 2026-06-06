package com.zsj.meetingagent.interview.vo;

import java.time.Instant;

/**
 * 面试历史记录响应。
 * 用于前端侧边栏和报告入口展示一次模拟面试的结构化摘要。
 */
public record InterviewRecordResponse(
        Long id,
        Integer userId,
        String sessionId,
        Integer resumeScore,
        Integer interviewScore,
        String interviewStatus,
        Integer questionCount,
        Integer compositeScore,
        Integer totalScore,
        Integer finalScore,
        String interviewSuggestions,
        String interviewDirection,
        Instant startTime,
        Instant endTime,
        Instant createTime,
        Instant updateTime
) {
}
