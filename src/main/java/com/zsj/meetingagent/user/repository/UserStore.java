package com.zsj.meetingagent.user.repository;

import com.zsj.meetingagent.user.entity.UserAccount;

import java.util.Optional;

/**
 * 用户存储接口。
 * 先用内存实现保证学习路径清晰，后续接入 MySQL 时只替换 repository 层。
 */
public interface UserStore {

    UserAccount save(UserAccount user);

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);
}
