package com.zsj.meetingagent.chat.vo;

/**
 * 旧前端创建会话响应。
 * 现在返回 MongoDB 持久化会话的 sessionId，用于兼容旧前端 AI 页面。
 */
public record LegacyConversationResponse(
        String sessionId,
        String conversationTitle
) {
}
