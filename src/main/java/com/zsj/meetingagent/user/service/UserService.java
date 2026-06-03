package com.zsj.meetingagent.user.service;

import com.zsj.meetingagent.auth.dto.RegisterRequest;
import com.zsj.meetingagent.common.exception.BusinessException;
import com.zsj.meetingagent.user.entity.UserAccount;
import com.zsj.meetingagent.user.repository.UserStore;
import com.zsj.meetingagent.user.vo.UserProfileResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户业务服务。
 * 负责注册、用户查询和密码校验，认证模块只调用这里暴露的业务能力。
 */
@Service
public class UserService {

    private final AtomicLong idGenerator = new AtomicLong(1000);
    private final UserStore userStore;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserStore userStore, PasswordEncoder passwordEncoder) {
        this.userStore = userStore;
        this.passwordEncoder = passwordEncoder;
    }

    public UserProfileResponse register(RegisterRequest request) {
        String username = request.username().trim();
        if (userStore.existsByUsername(username)) {
            throw new BusinessException("A0101", "用户名已存在");
        }

        Instant now = Instant.now();
        UserAccount account = new UserAccount(
                idGenerator.incrementAndGet(),
                username,
                passwordEncoder.encode(request.password()),
                request.realName(),
                request.phone(),
                request.mail(),
                null,
                now,
                now,
                0
        );
        return toProfile(userStore.save(account));
    }

    public UserProfileResponse verifyPassword(String username, String rawPassword) {
        UserAccount account = findAccount(username);
        if (!passwordEncoder.matches(rawPassword, account.passwordHash())) {
            throw new BusinessException("A0201", "用户名或密码错误");
        }
        return toProfile(account);
    }

    public UserProfileResponse getUserByUsername(String username) {
        return toProfile(findAccount(username));
    }

    public boolean hasUsername(String username) {
        return userStore.existsByUsername(username);
    }

    private UserAccount findAccount(String username) {
        return userStore.findByUsername(username)
                .orElseThrow(() -> new BusinessException("A0201", "用户不存在"));
    }

    private UserProfileResponse toProfile(UserAccount account) {
        return new UserProfileResponse(
                account.id(),
                account.username(),
                account.realName(),
                account.phone(),
                account.mail(),
                account.avatar(),
                account.createTime(),
                account.updateTime(),
                account.deleted()
        );
    }
}
