package com.zsj.meetingagent.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 登录认证配置。
 * JWT 密钥和有效期放在配置里，便于本地学习和部署环境分别管理。
 */
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private String jwtSecret;

    private long tokenExpireMinutes = 1440;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getTokenExpireMinutes() {
        return tokenExpireMinutes;
    }

    public void setTokenExpireMinutes(long tokenExpireMinutes) {
        this.tokenExpireMinutes = tokenExpireMinutes;
    }
}
