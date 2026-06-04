package com.zsj.meetingagent.user.repository;

import com.zsj.meetingagent.common.exception.BusinessException;
import com.zsj.meetingagent.user.entity.UserAccount;
import com.zsj.meetingagent.user.mapper.UserAccountMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MySQL 用户仓储实现。
 * 当前默认使用 users 表持久化注册用户，后端重启后仍能通过 JWT 中的用户名查回账号。
 */
@Primary
@Repository
public class MySqlUserStore implements UserStore {

    private final UserAccountMapper userAccountMapper;

    public MySqlUserStore(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    public UserAccount save(UserAccount user) {
        userAccountMapper.insert(user);
        /*
         * MySQL 自增 id 由数据库生成。插入后再按用户名查回完整账号，保证 Service 层拿到真实 id。
         */
        return userAccountMapper.findByUsername(user.username())
                .orElseThrow(() -> new BusinessException("A0102", "用户保存失败"));
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return userAccountMapper.findByUsername(username);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userAccountMapper.countByUsername(username) > 0;
    }
}
