package com.zsj.meetingagent.auth.security;

import cn.dev33.satoken.stp.StpUtil;
import com.zsj.meetingagent.common.exception.AuthenticationRequiredException;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * Sa-Token 登录态拦截器。
 * 负责兼容前端现有 Authorization: Bearer token 请求头，并把当前用户名绑定到请求上下文。
 */
@Component
public class SaTokenAuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/health",
            "/actuator/health",
            "/api/auth/login",
            "/api/auth/register",
            "/api/ai/models",
            "/api/xunzhi/v1/users/login",
            "/api/xunzhi/v1/users/register",
            "/api/xunzhi/v1/users/check-login",
            "/api/xunzhi/v1/users/has-username"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (shouldSkipAuthentication(request)) {
            return true;
        }

        /*
         * 先尝试识别 token，再判断路径是否公开。
         * 这样旧前端 check-login 不带 token 时能返回未登录，带 token 时也能拿到当前用户信息。
         */
        bindLoginUserIfTokenValid(request);

        if (isPublicPath(request)) {
            return true;
        }
        if (LoginUserContext.tryCurrentUsername().isPresent()) {
            return true;
        }
        throw new AuthenticationRequiredException("请先登录后再访问");
    }

    private boolean shouldSkipAuthentication(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || request.getDispatcherType() == DispatcherType.ASYNC;
    }

    private void bindLoginUserIfTokenValid(HttpServletRequest request) {
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            return;
        }
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId != null) {
                LoginUserContext.bind(request, loginId.toString(), token);
            }
        } catch (RuntimeException ignored) {
            // 公开接口允许携带失效 token 后继续返回未登录；受保护接口会在后续统一返回中文 403。
        }
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization)) {
            if (authorization.startsWith(BEARER_PREFIX)) {
                return authorization.substring(BEARER_PREFIX.length()).trim();
            }
            return authorization.trim();
        }
        String saTokenHeader = request.getHeader("satoken");
        if (StringUtils.hasText(saTokenHeader)) {
            return saTokenHeader.trim();
        }
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam.trim();
        }
        return null;
    }

    private boolean isPublicPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        String finalPath = path;
        return PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, finalPath));
    }
}
