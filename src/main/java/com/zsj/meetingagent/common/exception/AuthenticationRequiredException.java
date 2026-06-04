package com.zsj.meetingagent.common.exception;

/**
 * 未登录异常。
 * Sa-Token 拦截器发现请求没有有效登录态时抛出，统一转换为中文 403 响应。
 */
public class AuthenticationRequiredException extends RuntimeException {

    public AuthenticationRequiredException(String message) {
        super(message);
    }
}
