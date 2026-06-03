package com.zsj.meetingagent.chat.vo;

import java.time.Instant;

/**
 * 聊天会话返回对象。
 * 前端会话列表只需要展示标题、时间和消息数量，具体消息通过历史接口单独加载。
 */
public record ChatSessionResponse(
        String sessionId,
        String title,
        String model,
        int messageCount,
        Instant createdAt,
        Instant updatedAt
) {
}
