package com.zsj.meetingagent.chat.vo;

import java.time.Instant;

/**
 * 聊天消息返回对象。
 * role 区分 user 和 assistant，前端据此决定消息显示在用户侧还是 AI 侧。
 */
public record ChatMessageResponse(
        String id,
        String sessionId,
        String role,
        String content,
        String model,
        int sequence,
        Instant createdAt
) {
}
