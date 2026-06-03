package com.zsj.meetingagent.auth.vo;

import com.zsj.meetingagent.user.vo.UserProfileResponse;

/**
 * 登录成功后的响应。
 * token 由前端保存，并在后续请求中放入 Authorization: Bearer <token>。
 */
public record AuthResponse(
        String token,
        UserProfileResponse user
) {
}
