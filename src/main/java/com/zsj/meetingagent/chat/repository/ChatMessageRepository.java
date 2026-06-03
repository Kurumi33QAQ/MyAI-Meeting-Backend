package com.zsj.meetingagent.chat.repository;

import com.zsj.meetingagent.chat.entity.ChatMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * 聊天消息 MongoDB Repository。
 * 消息按 sessionId 和 sequence 升序恢复，前端刷新后能得到稳定的历史顺序。
 */
public interface ChatMessageRepository extends MongoRepository<ChatMessageDocument, String> {

    List<ChatMessageDocument> findBySessionIdAndUsernameAndDeletedOrderBySequenceAsc(String sessionId, String username, int deleted);

    long countBySessionIdAndUsernameAndDeleted(String sessionId, String username, int deleted);
}
