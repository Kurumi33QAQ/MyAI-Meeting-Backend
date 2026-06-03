package com.zsj.meetingagent.ai.vo;

/**
 * AI 模型选项返回结果。
 * 前端可用它渲染模型下拉框，后续也可以扩展价格、上下文长度、是否支持流式等字段。
 */
public record AiModelOptionResponse(
        String model,
        String displayName,
        String provider,
        boolean mock
) {
}
