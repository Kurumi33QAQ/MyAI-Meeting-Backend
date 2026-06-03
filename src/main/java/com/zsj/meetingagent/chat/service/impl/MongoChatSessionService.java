package com.zsj.meetingagent.chat.service.impl;

import com.zsj.meetingagent.chat.entity.ChatConversationDocument;
import com.zsj.meetingagent.chat.entity.ChatMessageDocument;
import com.zsj.meetingagent.chat.repository.ChatConversationRepository;
import com.zsj.meetingagent.chat.repository.ChatMessageRepository;
import com.zsj.meetingagent.chat.service.ChatSessionService;
import com.zsj.meetingagent.chat.vo.ChatMessageResponse;
import com.zsj.meetingagent.chat.vo.ChatSessionResponse;
import com.zsj.meetingagent.common.exception.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * MongoDB 聊天会话服务实现。
 * 会话和消息是长文本、半结构化数据，使用文档数据库便于后续扩展上下文快照、Agent trace 和 RAG 证据。
 */
@Service
public class MongoChatSessionService implements ChatSessionService {

    private static final int NOT_DELETED = 0;

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;

    public MongoChatSessionService(
            ChatConversationRepository conversationRepository,
            ChatMessageRepository messageRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    public ChatSessionResponse createSession(String username, String firstMessage, String model) {
        Instant now = Instant.now();
        ChatConversationDocument document = new ChatConversationDocument();
        document.setSessionId(UUID.randomUUID().toString());
        document.setUsername(username);
        document.setTitle(buildTitle(firstMessage));
        document.setModel(model);
        document.setMessageCount(0);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        document.setDeleted(NOT_DELETED);
        return toSessionResponse(conversationRepository.save(document));
    }

    @Override
    public ChatSessionResponse ensureSession(String username, String sessionId, String firstMessage, String model) {
        if (!StringUtils.hasText(sessionId)) {
            return createSession(username, firstMessage, model);
        }
        ChatConversationDocument document = findConversation(username, sessionId);
        return toSessionResponse(document);
    }

    @Override
    public ChatMessageResponse saveUserMessage(String username, String sessionId, String content, String model) {
        return saveMessage(username, sessionId, "user", content, model);
    }

    @Override
    public ChatMessageResponse saveAssistantMessage(String username, String sessionId, String content, String model) {
        return saveMessage(username, sessionId, "assistant", content, model);
    }

    @Override
    public List<ChatSessionResponse> listSessions(String username, long current, long size) {
        int page = Math.max((int) current - 1, 0);
        int pageSize = Math.min(Math.max((int) size, 1), 100);
        return conversationRepository
                .findByUsernameAndDeletedOrderByUpdatedAtDesc(username, NOT_DELETED, PageRequest.of(page, pageSize))
                .stream()
                .map(this::toSessionResponse)
                .toList();
    }

    @Override
    public long countSessions(String username) {
        return conversationRepository.countByUsernameAndDeleted(username, NOT_DELETED);
    }

    @Override
    public List<ChatMessageResponse> listMessages(String username, String sessionId) {
        findConversation(username, sessionId);
        return messageRepository
                .findBySessionIdAndUsernameAndDeletedOrderBySequenceAsc(sessionId, username, NOT_DELETED)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    private ChatMessageResponse saveMessage(String username, String sessionId, String role, String content, String model) {
        ChatConversationDocument conversation = findConversation(username, sessionId);
        long messageCount = messageRepository.countBySessionIdAndUsernameAndDeleted(sessionId, username, NOT_DELETED);
        Instant now = Instant.now();

        ChatMessageDocument message = new ChatMessageDocument();
        message.setSessionId(sessionId);
        message.setUsername(username);
        message.setRole(role);
        message.setContent(content);
        message.setModel(StringUtils.hasText(model) ? model : conversation.getModel());
        message.setSequence((int) messageCount + 1);
        message.setCreatedAt(now);
        message.setDeleted(NOT_DELETED);

        conversation.setMessageCount((int) messageCount + 1);
        conversation.setUpdatedAt(now);
        if (StringUtils.hasText(model)) {
            conversation.setModel(model);
        }
        conversationRepository.save(conversation);
        return toMessageResponse(messageRepository.save(message));
    }

    private ChatConversationDocument findConversation(String username, String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new BusinessException("C0401", "会话不能为空");
        }
        return conversationRepository.findBySessionIdAndUsernameAndDeleted(sessionId, username, NOT_DELETED)
                .orElseThrow(() -> new BusinessException("C0404", "会话不存在或无权访问"));
    }

    private String buildTitle(String firstMessage) {
        if (!StringUtils.hasText(firstMessage)) {
            return "新的对话";
        }
        String trimmed = firstMessage.trim();
        return trimmed.substring(0, Math.min(trimmed.length(), 20));
    }

    private ChatSessionResponse toSessionResponse(ChatConversationDocument document) {
        return new ChatSessionResponse(
                document.getSessionId(),
                document.getTitle(),
                document.getModel(),
                document.getMessageCount(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    private ChatMessageResponse toMessageResponse(ChatMessageDocument document) {
        return new ChatMessageResponse(
                document.getId(),
                document.getSessionId(),
                document.getRole(),
                document.getContent(),
                document.getModel(),
                document.getSequence(),
                document.getCreatedAt()
        );
    }
}
