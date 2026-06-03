package com.zsj.meetingagent.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求参数。
 * 当前阶段只做基础账号注册，后续接入 MySQL 后会补充唯一索引和更完整的资料校验。
 */
public record RegisterRequest(
        @NotBlank(message = "不能为空") @Size(max = 64, message = "长度不能超过 64") String username,
        @NotBlank(message = "不能为空") @Size(min = 6, max = 64, message = "长度必须在 6 到 64 之间") String password,
        @Size(max = 64, message = "长度不能超过 64") String realName,
        @Size(max = 32, message = "长度不能超过 32") String phone,
        @Size(max = 128, message = "长度不能超过 128") String mail
) {
}
