package com.zsj.meetingagent.limit.enums;

/**
 * AI 调用失败类型。
 * 失败分类用于后续统计“是模型失败、Redis 失败、限流还是超时”，避免所有问题都只表现成 500。
 */
public enum AiCallFailureType {

    NONE("无失败"),
    RATE_LIMITED("用户调用过于频繁"),
    REDIS_UNAVAILABLE("Redis 暂不可用"),
    WAIT_TIMEOUT("等待重复请求结果超时"),
    MODEL_TIMEOUT("模型调用超时"),
    MODEL_ERROR("模型调用异常"),
    UNKNOWN("未知异常");

    private final String description;

    AiCallFailureType(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
