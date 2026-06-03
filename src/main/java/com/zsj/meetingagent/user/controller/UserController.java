package com.zsj.meetingagent.user.controller;

import com.zsj.meetingagent.common.result.ApiResponse;
import com.zsj.meetingagent.user.service.UserService;
import com.zsj.meetingagent.user.vo.UserProfileResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户资料接口控制器。
 * 认证行为放在 auth 模块，这里只处理用户资料查询。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{username}")
    public ApiResponse<UserProfileResponse> getUser(@PathVariable String username) {
        return ApiResponse.success(userService.getUserByUsername(username));
    }

    @GetMapping("/exists")
    public ApiResponse<Boolean> exists(@RequestParam String username) {
        return ApiResponse.success(userService.hasUsername(username));
    }
}
