package com.zsj.meetingagent.auth.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.meetingagent.auth.config.AuthProperties;
import com.zsj.meetingagent.common.exception.BusinessException;
import com.zsj.meetingagent.common.result.ApiResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JWT 生成与校验服务。
 * 这里使用 JDK 自带 HMAC-SHA256 实现，避免阶段 1 为了 JWT 额外引入第三方依赖。
 */
@Service
@EnableConfigurationProperties(AuthProperties.class)
public class JwtTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final AuthProperties authProperties;
    private final ObjectMapper objectMapper;

    public JwtTokenService(AuthProperties authProperties, ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
    }

    public String createToken(String username) {
        long now = Instant.now().getEpochSecond();
        long expiresAt = now + authProperties.getTokenExpireMinutes() * 60;

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", username);
        payload.put("iat", now);
        payload.put("exp", expiresAt);

        String unsignedToken = encodeJson(header) + "." + encodeJson(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public String parseUsername(String token) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3) {
            throw invalidToken();
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw invalidToken();
        }

        Map<String, Object> payload = decodeJson(parts[1]);
        Object subject = payload.get("sub");
        Object expiresAt = payload.get("exp");
        if (!(subject instanceof String username) || username.isBlank()) {
            throw invalidToken();
        }
        if (!(expiresAt instanceof Number exp) || exp.longValue() < Instant.now().getEpochSecond()) {
            throw new BusinessException("A0401", "登录已过期，请重新登录");
        }
        return username;
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception ex) {
            throw new BusinessException(ApiResponse.SERVICE_ERROR_CODE, "JWT 生成失败");
        }
    }

    private Map<String, Object> decodeJson(String encoded) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(encoded);
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            throw invalidToken();
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    authProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(keySpec);
            byte[] signature = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception ex) {
            throw new BusinessException(ApiResponse.SERVICE_ERROR_CODE, "JWT 签名失败");
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null || left.length() != right.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length(); i++) {
            result |= left.charAt(i) ^ right.charAt(i);
        }
        return result == 0;
    }

    private BusinessException invalidToken() {
        return new BusinessException("A0401", "无效的登录凭证");
    }
}
