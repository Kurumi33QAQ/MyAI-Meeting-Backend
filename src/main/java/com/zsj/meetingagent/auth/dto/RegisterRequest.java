package com.zsj.meetingagent.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求参数。
 * 当前只保留基础账号字段，后续接入验证码、邮箱校验或用户资料完善时再扩展。
 */
public record RegisterRequest(
        @NotBlank(message = "不能为空")
        @Size(max = 64, message = "长度不能超过 64 个字符")
        String username,

        @NotBlank(message = "不能为空")
        @Size(min = 6, max = 64, message = "长度必须在 6 到 64 个字符之间")
        String password,

        @Size(max = 64, message = "长度不能超过 64 个字符")
        String realName,

        @Size(max = 32, message = "长度不能超过 32 个字符")
        String phone,

        @Size(max = 128, message = "长度不能超过 128 个字符")
        String mail
) {
}
