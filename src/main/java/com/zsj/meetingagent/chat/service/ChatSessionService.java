package com.zsj.meetingagent.chat.service;

import com.zsj.meetingagent.chat.vo.ChatMessageResponse;
import com.zsj.meetingagent.chat.vo.ChatSessionResponse;

import java.util.List;

/**
 * 聊天会话服务接口。
 * 统一管理会话创建、权限校验和消息保存，避免同步接口和 SSE 接口各写一套存储逻辑。
 */
public interface ChatSessionService {

    ChatSessionResponse createSession(String username, String firstMessage, String model);

    ChatSessionResponse ensureSession(String username, String sessionId, String firstMessage, String model);

    ChatMessageResponse saveUserMessage(String username, String sessionId, String content, String model);

    ChatMessageResponse saveAssistantMessage(String username, String sessionId, String content, String model);

    List<ChatSessionResponse> listSessions(String username, long current, long size);

    long countSessions(String username);

    List<ChatMessageResponse> listMessages(String username, String sessionId);
}
