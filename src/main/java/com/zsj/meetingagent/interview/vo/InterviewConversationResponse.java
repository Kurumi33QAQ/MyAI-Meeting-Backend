package com.zsj.meetingagent.interview.vo;

import java.time.Instant;

/**
 * 面试会话列表响应。
 * 用于前端首页判断是否存在可继续的模拟面试会话。
 */
public record InterviewConversationResponse(
        String sessionId,
        String conversationTitle,
        String status,
        String interviewType,
        String resumeFileUrl,
        Instant createTime,
        Instant updateTime
) {
}
