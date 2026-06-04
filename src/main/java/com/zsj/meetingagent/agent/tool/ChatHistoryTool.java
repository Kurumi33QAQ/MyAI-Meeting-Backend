package com.zsj.meetingagent.agent.tool;

import com.zsj.meetingagent.chat.service.ChatSessionService;
import com.zsj.meetingagent.chat.vo.ChatMessageResponse;
import com.zsj.meetingagent.chat.vo.ChatSessionResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天历史工具。
 * 复用阶段 4 的会话存储能力，让 Agent 可以基于用户自己的历史消息做观察。
 */
@Component
public class ChatHistoryTool implements AgentTool {

    private final ChatSessionService chatSessionService;

    public ChatHistoryTool(ChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService;
    }

    @Override
    public String name() {
        return "chat_history";
    }

    @Override
    public String description() {
        return "查询当前用户的聊天会话历史";
    }

    @Override
    public boolean supports(AgentToolContext context) {
        String input = context.input();
        return hasText(context.sessionId())
                || (input != null && (input.contains("历史") || input.contains("会话") || input.contains("聊天") || input.contains("刚才")));
    }

    @Override
    public AgentToolResult execute(AgentToolContext context) {
        String sessionId = context.sessionId();
        if (!hasText(sessionId)) {
            List<ChatSessionResponse> sessions = chatSessionService.listSessions(context.username(), 1, 1);
            if (sessions.isEmpty()) {
                return new AgentToolResult(name(), "未查询到当前用户的聊天会话。");
            }
            sessionId = sessions.get(0).sessionId();
        }

        List<ChatMessageResponse> messages = chatSessionService.listMessages(context.username(), sessionId);
        if (messages.isEmpty()) {
            return new AgentToolResult(name(), "会话 " + sessionId + " 中暂无消息。");
        }

        String summary = messages.stream()
                .limit(6)
                .map(message -> message.role() + "：" + truncate(message.content(), 120))
                .collect(Collectors.joining("\n"));
        return new AgentToolResult(name(), "查询到会话 " + sessionId + " 的最近消息：\n" + summary);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
