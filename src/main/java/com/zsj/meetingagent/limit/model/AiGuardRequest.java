package com.zsj.meetingagent.limit.model;

import com.zsj.meetingagent.limit.enums.AiCallOperation;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * AI 调用守卫请求。
 * requestKey 用来判断两个请求是否等价，是 Single-flight 去重和结果回放的核心。
 */
public record AiGuardRequest(
        String username,
        AiCallOperation operation,
        String requestKey,
        String model
) {

    public static AiGuardRequest of(String username, AiCallOperation operation, String model, String rawKey) {
        String safeUsername = StringUtils.hasText(username) ? username.trim() : "anonymous";
        String safeModel = StringUtils.hasText(model) ? model.trim() : "default";
        String safeRawKey = "%s|%s|%s|%s".formatted(safeUsername, operation.name(), safeModel, rawKey == null ? "" : rawKey);
        return new AiGuardRequest(safeUsername, operation, sha256(safeRawKey), safeModel);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256，无法生成 AI 调用 requestKey", ex);
        }
    }
}
