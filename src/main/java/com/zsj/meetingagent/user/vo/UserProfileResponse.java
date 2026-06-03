package com.zsj.meetingagent.user.vo;

import java.time.Instant;

/**
 * 返回给前端的用户资料。
 * 不包含密码哈希，避免把敏感字段暴露给浏览器。
 */
public record UserProfileResponse(
        Long id,
        String username,
        String realName,
        String phone,
        String mail,
        String avatar,
        Instant createTime,
        Instant updateTime,
        int delFlag
) {
}
