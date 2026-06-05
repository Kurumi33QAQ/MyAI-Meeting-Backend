package com.zsj.meetingagent.limit.service;

import com.zsj.meetingagent.limit.model.AiGuardRequest;
import com.zsj.meetingagent.limit.model.AiGuardResult;

import java.util.function.Supplier;

/**
 * AI 调用守卫服务接口。
 * 负责把限流、Single-flight、等待回放、超时和降级统一封装在业务调用外层。
 */
public interface AiCallGuardService {

    AiGuardResult executeText(AiGuardRequest request, Supplier<String> ownerCall);

    boolean redisAvailable();

    boolean enabled();
}
