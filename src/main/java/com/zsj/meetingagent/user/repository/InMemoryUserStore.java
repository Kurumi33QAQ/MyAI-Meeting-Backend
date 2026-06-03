package com.zsj.meetingagent.user.repository;

import com.zsj.meetingagent.user.entity.UserAccount;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 阶段 1 的内存用户仓储。
 * 它让登录注册先跑通，不依赖本地 MySQL；阶段 4/后续数据库阶段会替换为真实持久化实现。
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
