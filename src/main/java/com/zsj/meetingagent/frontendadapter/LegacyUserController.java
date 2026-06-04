package com.zsj.meetingagent.frontendadapter;

import com.zsj.meetingagent.auth.dto.LoginRequest;
import com.zsj.meetingagent.auth.dto.RegisterRequest;
import com.zsj.meetingagent.auth.service.AuthService;
import com.zsj.meetingagent.auth.vo.AuthResponse;
import com.zsj.meetingagent.common.result.ApiResponse;
import com.zsj.meetingagent.user.service.UserService;
import com.zsj.meetingagent.user.vo.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 旧前端接口兼容层。
 * 这里只做路径和返回字段适配，核心认证逻辑仍复用 auth/user 模块，避免把旧项目命名扩散到新代码里。
 */
@RestController
@RequestMapping("/api/xunzhi/v1/users")
public class LegacyUserController {

    private final AuthService authService;
    private final UserService userService;

    public LegacyUserController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success();
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.success(toLegacyAuthPayload(response, true));
    }

    @GetMapping("/check-login")
    public ApiResponse<Map<String, Object>> checkLogin(Authentication authentication) {
        Map<String, Object> payload = new LinkedHashMap<>();
        boolean loggedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        payload.put("isLogin", loggedIn);
        if (loggedIn) {
            AuthResponse response = authService.currentUser(authentication.getName());
            payload.putAll(toLegacyAuthPayload(response, false));
        }
        return ApiResponse.success(payload);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // 当前还没有 Redis token 黑名单，退出登录先由前端删除 token；服务端失效能力放到后续限流/缓存阶段扩展。
        return ApiResponse.success();
    }

    @GetMapping("/{username}")
    public ApiResponse<UserProfileResponse> getUser(@PathVariable String username) {
        return ApiResponse.success(userService.getUserByUsername(username));
    }

    @GetMapping("/actual/{username}")
    public ApiResponse<UserProfileResponse> getActualUser(@PathVariable String username) {
        return ApiResponse.success(userService.getUserByUsername(username));
    }

    @GetMapping("/has-username")
    public ApiResponse<Boolean> hasUsername(@RequestParam String username) {
        return ApiResponse.success(userService.hasUsername(username));
    }

    private Map<String, Object> toLegacyAuthPayload(AuthResponse response, boolean includeAdminFlag) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("token", response.token());
        payload.put("username", response.user().username());
        payload.put("user", response.user());
        if (includeAdminFlag) {
            payload.put("isAdmin", false);
        }
        return payload;
    }
}
