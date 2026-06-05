package com.zsj.meetingagent.limit.controller;

import com.zsj.meetingagent.auth.security.LoginUserContext;
import com.zsj.meetingagent.common.result.ApiResponse;
import com.zsj.meetingagent.limit.model.AiGuardMetricSnapshot;
import com.zsj.meetingagent.limit.service.AiCallGuardService;
import com.zsj.meetingagent.limit.service.AiGuardMetricService;
import com.zsj.meetingagent.limit.vo.AiGuardHealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 调用稳定性治理验收接口。
 * 提供 Guard 开关、Redis 可用性和调用治理指标，方便阶段 8.5 做后端压测验证。
 */
@RestController
@RequestMapping("/api/ai/guard")
public class AiGuardController {

    private final AiCallGuardService aiCallGuardService;
    private final AiGuardMetricService metricService;

    public AiGuardController(AiCallGuardService aiCallGuardService, AiGuardMetricService metricService) {
        this.aiCallGuardService = aiCallGuardService;
        this.metricService = metricService;
    }

    @GetMapping("/health")
    public ApiResponse<AiGuardHealthResponse> health() {
        LoginUserContext.currentUsername();
        boolean enabled = aiCallGuardService.enabled();
        boolean redisAvailable = aiCallGuardService.redisAvailable();
        String mode = enabled
                ? (redisAvailable ? "redis-single-flight" : "redis-bypass")
                : "disabled";
        return ApiResponse.success(new AiGuardHealthResponse(enabled, redisAvailable, mode));
    }

    @GetMapping("/stats")
    public ApiResponse<AiGuardMetricSnapshot> stats() {
        LoginUserContext.currentUsername();
        return ApiResponse.success(metricService.snapshot());
    }
}
