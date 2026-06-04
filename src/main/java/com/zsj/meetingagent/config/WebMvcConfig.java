package com.zsj.meetingagent.config;

import com.zsj.meetingagent.auth.security.SaTokenAuthInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 基础配置。
 * 当前用于统一处理前端跨域访问，让本项目后端可以被本地前端页面和兼容接口正常调用。
 */
@Configuration
@EnableConfigurationProperties(CorsConfig.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final CorsConfig corsConfig;
    private final SaTokenAuthInterceptor saTokenAuthInterceptor;

    public WebMvcConfig(CorsConfig corsConfig, SaTokenAuthInterceptor saTokenAuthInterceptor) {
        this.corsConfig = corsConfig;
        this.saTokenAuthInterceptor = saTokenAuthInterceptor;
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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        /*
         * Sa-Token 认证统一在 MVC 拦截器中完成。
         * Controller 只读取 LoginUserContext，不需要关心 token 来自 Header 还是后续 WebSocket 参数。
         */
        registry.addInterceptor(saTokenAuthInterceptor)
                .addPathPatterns("/api/**");
    }
}
