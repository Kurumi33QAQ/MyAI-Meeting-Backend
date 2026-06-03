package com.zsj.meetingagent.user.entity;

import java.time.Instant;

/**
 * 用户账号领域对象。
 * 阶段 1 使用内存存储，字段设计保持接近后续 MySQL users 表，便于平滑迁移。
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
