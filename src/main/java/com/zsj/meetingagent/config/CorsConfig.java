package com.zsj.meetingagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 跨域配置属性。
 * 前端地址通过配置文件维护，方便本地开发和部署环境切换。
 */
@ConfigurationProperties(prefix = "app.cors")
public class CorsConfig {

    private List<String> allowedOrigins = new ArrayList<>();

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
