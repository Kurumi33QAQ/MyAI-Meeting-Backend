package com.zsj.meetingagent.ai.service;

import com.zsj.meetingagent.ai.dto.AiChatRequest;
import com.zsj.meetingagent.ai.vo.AiChatResponse;
import reactor.core.publisher.Flux;

/**
 * AI 对话服务接口。
 * 阶段 3 开始同时支持同步回答和流式回答，因此先抽出接口，为后续 RAG、Agent、多模型策略留扩展点。
 */
public interface AiChatService {

    AiChatResponse chat(AiChatRequest request);

    Flux<String> streamChat(AiChatRequest request);
}
