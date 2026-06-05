package com.zsj.meetingagent.limit.service;

import com.zsj.meetingagent.limit.model.AiGuardRequest;

/**
 * AI 调用限流服务。
 * 当前按用户和业务类型做分钟级限流，防止大模型接口被重复点击或脚本刷爆。
 */
public interface AiRateLimitService {

    boolean tryAcquire(AiGuardRequest request);
}
