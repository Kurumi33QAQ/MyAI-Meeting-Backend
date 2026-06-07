package com.zsj.meetingagent.websocket.auth;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

/**
 * WebSocket token 校验器。
 * 负责从查询参数或请求头中提取 Sa-Token token，让实时媒体连接复用 HTTP 登录态。
 */
@Component
public class WebSocketTokenVerifier {

    private static final String BEARER_PREFIX = "Bearer ";

    public Optional<WebSocketLoginUser> verify(WebSocketSession session) {
        String token = extractToken(session);
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                return Optional.empty();
            }
            return Optional.of(new WebSocketLoginUser(loginId.toString(), token));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private String extractToken(WebSocketSession session) {
        String tokenFromQuery = extractTokenFromQuery(session.getUri());
        if (StringUtils.hasText(tokenFromQuery)) {
            return tokenFromQuery;
        }
        HttpHeaders headers = session.getHandshakeHeaders();
        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization)) {
            return stripBearerPrefix(authorization);
        }
        String saToken = headers.getFirst("satoken");
        return StringUtils.hasText(saToken) ? saToken.trim() : null;
    }

    private String extractTokenFromQuery(URI uri) {
        if (uri == null) {
            return null;
        }
        var params = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        String token = firstNonBlank(params.getFirst("token"), params.getFirst("access_token"), params.getFirst("Authorization"));
        return stripBearerPrefix(token);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String stripBearerPrefix(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.startsWith(BEARER_PREFIX)) {
            return normalized.substring(BEARER_PREFIX.length()).trim();
        }
        return normalized;
    }
}
