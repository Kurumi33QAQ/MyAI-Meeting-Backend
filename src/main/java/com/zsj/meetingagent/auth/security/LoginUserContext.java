package com.zsj.meetingagent.auth.security;

import com.zsj.meetingagent.common.exception.AuthenticationRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * 当前登录用户上下文。
 * 业务层不直接解析 token，而是从这里读取 Sa-Token 拦截器已经识别出的用户名。
 */
public final class LoginUserContext {

    public static final String LOGIN_USERNAME_ATTRIBUTE = "meetingAgent.loginUsername";
    public static final String LOGIN_TOKEN_ATTRIBUTE = "meetingAgent.loginToken";

    private LoginUserContext() {
    }

    public static void bind(HttpServletRequest request, String username, String token) {
        request.setAttribute(LOGIN_USERNAME_ATTRIBUTE, username);
        request.setAttribute(LOGIN_TOKEN_ATTRIBUTE, token);
    }

    public static String currentUsername() {
        return tryCurrentUsername()
                .orElseThrow(() -> new AuthenticationRequiredException("请先登录后再访问"));
    }

    public static Optional<String> tryCurrentUsername() {
        return currentRequest()
                .map(request -> request.getAttribute(LOGIN_USERNAME_ATTRIBUTE))
                .filter(String.class::isInstance)
                .map(String.class::cast);
    }

    public static Optional<String> currentToken() {
        return currentRequest()
                .map(request -> request.getAttribute(LOGIN_TOKEN_ATTRIBUTE))
                .filter(String.class::isInstance)
                .map(String.class::cast);
    }

    private static Optional<HttpServletRequest> currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return Optional.of(servletRequestAttributes.getRequest());
        }
        return Optional.empty();
    }
}
