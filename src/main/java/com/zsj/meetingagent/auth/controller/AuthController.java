package com.zsj.meetingagent.auth.controller;

import com.zsj.meetingagent.auth.dto.LoginRequest;
import com.zsj.meetingagent.auth.dto.RegisterRequest;
import com.zsj.meetingagent.auth.service.AuthService;
import com.zsj.meetingagent.auth.vo.AuthResponse;
import com.zsj.meetingagent.common.result.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口控制器。
 * 负责处理注册、登录、获取当前用户信息和退出登录等认证相关请求。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success();
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<AuthResponse> currentUser(Authentication authentication) {
        return ApiResponse.success(authService.currentUser(authentication.getName()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // JWT 是无状态凭证；阶段 1 先由前端删除 token，后续可接入 Redis 黑名单实现服务端失效。
        return ApiResponse.success();
    }
}
