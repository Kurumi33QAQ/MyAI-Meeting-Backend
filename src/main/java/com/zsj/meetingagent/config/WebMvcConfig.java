package com.zsj.meetingagent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 基础配置。
 * 当前阶段先处理前端跨域访问，阶段 1 会在此基础上加入 JWT 鉴权。
 */
@Configuration
@EnableConfigurationProperties(CorsConfig.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final CorsConfig corsConfig;

    public WebMvcConfig(CorsConfig corsConfig) {
        this.corsConfig = corsConfig;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(corsConfig.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
