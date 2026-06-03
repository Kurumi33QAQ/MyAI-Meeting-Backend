package com.zsj.meetingagent.common.web;

import com.zsj.meetingagent.common.result.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * 项目健康检查接口。
 * 在业务模块尚未接入前，为前端联调和本地开发提供一个稳定的 JSON 测试入口。
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "service", "meeting-agent-backend",
                "time", Instant.now().toString()
        ));
    }
}
