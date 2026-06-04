package com.zsj.meetingagent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 基础配置。
 * 当前用于统一处理前端跨域访问，让本项目后端可以被本地前端页面和兼容接口正常调用。
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
        /*
         * 前端登录、SSE 聊天和后续 Agent/面试接口都会走 /api 前缀。
         * 这里集中放开跨域，避免每个 Controller 单独写跨域配置。
         */
        registry.addMapping("/api/**")
                .allowedOrigins(corsConfig.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
