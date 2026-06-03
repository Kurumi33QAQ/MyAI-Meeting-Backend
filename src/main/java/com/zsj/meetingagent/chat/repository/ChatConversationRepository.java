package com.zsj.meetingagent.chat.repository;

import com.zsj.meetingagent.chat.entity.ChatConversationDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * 聊天会话 MongoDB Repository。
 * 负责按当前登录用户查询会话，避免用户看到不属于自己的聊天记录。
 */
public interface ChatConversationRepository extends MongoRepository<ChatConversationDocument, String> {

    Optional<ChatConversationDocument> findBySessionIdAndUsernameAndDeleted(String sessionId, String username, int deleted);

    List<ChatConversationDocument> findByUsernameAndDeletedOrderByUpdatedAtDesc(String username, int deleted, Pageable pageable);

    long countByUsernameAndDeleted(String username, int deleted);
}
