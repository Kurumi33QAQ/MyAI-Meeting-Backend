package com.zsj.meetingagent.ai.service;

import com.zsj.meetingagent.ai.dto.AiChatRequest;
import com.zsj.meetingagent.ai.vo.AiChatResponse;
import reactor.core.publisher.Flux;

/**
 * AI 对话服务接口。
 * 当前同时支持同步回答和流式回答，Agent 最终回答也复用这个接口。
 * 后续 RAG、多模型路由和评测对照实验可以在实现层扩展，不需要改 Controller。
 */
public interface AiChatService {

    AiChatResponse chat(AiChatRequest request);

    Flux<String> streamChat(AiChatRequest request);
}
