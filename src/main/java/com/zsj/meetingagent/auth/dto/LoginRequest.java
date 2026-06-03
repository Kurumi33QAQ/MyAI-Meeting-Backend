package com.zsj.meetingagent.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求参数。
 * 登录只接收用户名和密码，密码校验放在 UserService 中统一处理。
 */
public record LoginRequest(
        @NotBlank(message = "不能为空")
        String username,

        @NotBlank(message = "不能为空")
        String password
) {
}
