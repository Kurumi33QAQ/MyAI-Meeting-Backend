package com.zsj.meetingagent.user.repository;

import com.zsj.meetingagent.user.entity.UserAccount;

import java.util.Optional;

/**
 * 用户存储接口。
 * 当前默认实现是 MySQL，测试或 memory-user-store profile 可以切换为内存实现。
 * Service 层只依赖这个接口，不直接关心用户数据具体来自哪种存储。
 */
public interface UserStore {

    UserAccount save(UserAccount user);

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);
}
