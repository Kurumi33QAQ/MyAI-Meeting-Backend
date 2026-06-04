package com.zsj.meetingagent.user.entity;

import java.time.Instant;

/**
 * 用户账号领域对象。
 * 当前默认由 MySQL users 表持久化，也保留给测试或 memory-user-store profile 复用。
 */
public record UserAccount(
        Long id,
        String username,
        String passwordHash,
        String realName,
        String phone,
        String mail,
        String avatar,
        Instant createTime,
        Instant updateTime,
        int deleted
) {
}
