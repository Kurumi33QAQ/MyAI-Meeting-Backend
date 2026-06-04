package com.zsj.meetingagent.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.zsj.meetingagent.auth.dto.LoginRequest;
import com.zsj.meetingagent.auth.dto.RegisterRequest;
import com.zsj.meetingagent.auth.vo.AuthResponse;
import com.zsj.meetingagent.user.service.UserService;
import com.zsj.meetingagent.user.vo.UserProfileResponse;
import org.springframework.stereotype.Service;

/**
 * 认证业务服务。
 * 将注册、登录和 Sa-Token 登录态管理串起来，Controller 不直接接触密码校验细节。
 */
@Service
public class AuthService {

    private final UserService userService;

    public AuthService(UserService userService) {
        this.userService = userService;
    }

    public void register(RegisterRequest request) {
        userService.register(request);
    }

    public AuthResponse login(LoginRequest request) {
        UserProfileResponse user = userService.verifyPassword(request.username(), request.password());
        /*
         * Sa-Token 会为当前登录用户创建登录态，并把 token 保存到框架的 token 仓储中。
         * 现阶段使用默认存储；后续接 Redis 后，多实例服务也能共享登录态。
         */
        StpUtil.login(user.username());
        String token = StpUtil.getTokenValue();
        return new AuthResponse(token, user);
    }

    public AuthResponse currentUser(String username, String token) {
        UserProfileResponse user = userService.getUserByUsername(username);
        return new AuthResponse(token, user);
    }

    public void logout(String username) {
        StpUtil.logout(username);
    }
}
