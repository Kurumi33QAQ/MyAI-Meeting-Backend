package com.zsj.meetingagent.common.result;

import java.time.Instant;

/**
 * 后端统一响应对象。
 * code 字段保留前端容易兼容的成功值，同时使用本项目自己的包名和命名方式。
 */
public record ApiResponse<T>(
        String code,
        String message,
        T data,
        String requestId,
        Instant timestamp
) {

    public static final String SUCCESS_CODE = "0";
    public static final String VALIDATION_ERROR_CODE = "A0001";
    public static final String AUTH_ERROR_CODE = "A0301";
    public static final String SERVICE_ERROR_CODE = "B0001";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, "success", data, null, Instant.now());
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static ApiResponse<Void> failure(String code, String message) {
        return new ApiResponse<>(code, message, null, null, Instant.now());
    }
}
