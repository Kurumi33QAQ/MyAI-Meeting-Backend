package com.zsj.meetingagent.chat.vo;

/**
 * 旧前端创建会话响应。
 * 当前只返回临时 sessionId，真正的会话持久化放到阶段 4。
 */
public record LegacyConversationResponse(
        String sessionId,
        String conversationTitle
) {
}
