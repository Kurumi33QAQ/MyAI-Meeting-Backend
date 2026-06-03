package com.zsj.meetingagent.auth.service;

import com.zsj.meetingagent.auth.dto.LoginRequest;
import com.zsj.meetingagent.auth.dto.RegisterRequest;
import com.zsj.meetingagent.auth.security.JwtTokenService;
import com.zsj.meetingagent.auth.vo.AuthResponse;
import com.zsj.meetingagent.user.service.UserService;
import com.zsj.meetingagent.user.vo.UserProfileResponse;
import org.springframework.stereotype.Service;

/**
 * 认证业务服务。
 * 将注册、登录和 token 生成串起来，Controller 不直接接触密码校验细节。
 */
@Service
public class AuthService {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserService userService, JwtTokenService jwtTokenService) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
    }

    public void register(RegisterRequest request) {
        userService.register(request);
    }

    public AuthResponse login(LoginRequest request) {
        UserProfileResponse user = userService.verifyPassword(request.username(), request.password());
        String token = jwtTokenService.createToken(user.username());
        return new AuthResponse(token, user);
    }

    public AuthResponse currentUser(String username) {
        UserProfileResponse user = userService.getUserByUsername(username);
        String refreshedToken = jwtTokenService.createToken(user.username());
        return new AuthResponse(refreshedToken, user);
    }
}
