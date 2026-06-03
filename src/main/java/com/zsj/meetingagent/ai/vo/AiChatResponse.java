package com.zsj.meetingagent.ai.vo;

/**
 * 同步 AI 对话返回结果。
 * 除回答正文外保留模型和耗时，便于后续做接口性能统计和 evaluation 对照实验。
 */
public record AiChatResponse(
        String answer,
        String model,
        String provider,
        long latencyMs,
        boolean mock
) {
}
