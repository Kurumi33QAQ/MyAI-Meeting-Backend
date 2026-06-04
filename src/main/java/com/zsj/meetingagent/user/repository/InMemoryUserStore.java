package com.zsj.meetingagent.user.repository;

import com.zsj.meetingagent.user.entity.UserAccount;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存用户仓储。
 * 仅在 memory-user-store profile 下启用，适合本地快速演示或特殊测试；默认运行使用 MySQL 持久化用户。
 */
@Repository
@Profile("memory-user-store")
public class InMemoryUserStore implements UserStore {

    private final Map<String, UserAccount> users = new ConcurrentHashMap<>();

    @Override
    public UserAccount save(UserAccount user) {
        users.put(user.username(), user);
        return user;
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    @Override
    public boolean existsByUsername(String username) {
        return users.containsKey(username);
    }
}
