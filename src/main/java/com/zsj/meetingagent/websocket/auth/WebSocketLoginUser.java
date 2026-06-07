package com.zsj.meetingagent.websocket.auth;

/**
 * WebSocket 已登录用户信息。
 * username 来自 Sa-Token 登录态，token 保留给后续供应商透传或审计使用。
 */
public record WebSocketLoginUser(
        String username,
        String token
) {
}
