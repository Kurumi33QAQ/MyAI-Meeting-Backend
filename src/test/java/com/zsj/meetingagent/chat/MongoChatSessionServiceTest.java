package com.zsj.meetingagent.chat;

import com.zsj.meetingagent.chat.entity.ChatConversationDocument;
import com.zsj.meetingagent.chat.entity.ChatMessageDocument;
import com.zsj.meetingagent.chat.repository.ChatConversationRepository;
import com.zsj.meetingagent.chat.repository.ChatMessageRepository;
import com.zsj.meetingagent.chat.service.impl.MongoChatSessionService;
import com.zsj.meetingagent.chat.vo.ChatSessionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MongoChatSessionServiceTest {

    @Mock
    private ChatConversationRepository conversationRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @InjectMocks
    private MongoChatSessionService chatSessionService;

    @Test
    void createSessionAndSaveMessagesWithSequence() {
        ArgumentCaptor<ChatConversationDocument> conversationCaptor = ArgumentCaptor.forClass(ChatConversationDocument.class);
        ArgumentCaptor<ChatMessageDocument> messageCaptor = ArgumentCaptor.forClass(ChatMessageDocument.class);

        when(conversationRepository.save(conversationCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.save(messageCaptor.capture()))
                .thenAnswer(invocation -> {
                    ChatMessageDocument message = invocation.getArgument(0);
                    message.setId("message-" + message.getSequence());
                    return message;
                });

        ChatSessionResponse session = chatSessionService.createSession("alice", "请解释 MongoDB 会话存储", "gpt-4o-mini");
        ChatConversationDocument savedConversation = conversationCaptor.getValue();
        when(conversationRepository.findBySessionIdAndUsernameAndDeleted(session.sessionId(), "alice", 0))
                .thenReturn(Optional.of(savedConversation));
        when(messageRepository.countBySessionIdAndUsernameAndDeleted(anyString(), anyString(), any(Integer.class)))
                .thenReturn(0L)
                .thenReturn(1L);

        chatSessionService.saveUserMessage("alice", session.sessionId(), "你好", "gpt-4o-mini");
        chatSessionService.saveAssistantMessage("alice", session.sessionId(), "你好，我是 AI。", "gpt-4o-mini");

        assertThat(session.title()).isEqualTo("请解释 MongoDB 会话存储");
        assertThat(messageCaptor.getAllValues()).hasSize(2);
        assertThat(messageCaptor.getAllValues().get(0).getRole()).isEqualTo("user");
        assertThat(messageCaptor.getAllValues().get(0).getSequence()).isEqualTo(1);
        assertThat(messageCaptor.getAllValues().get(1).getRole()).isEqualTo("assistant");
        assertThat(messageCaptor.getAllValues().get(1).getSequence()).isEqualTo(2);
        assertThat(savedConversation.getUpdatedAt()).isAfterOrEqualTo(Instant.EPOCH);
    }
}
