package com.zsj.meetingagent.user.mapper;

import com.zsj.meetingagent.user.entity.UserAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * 用户账号 MyBatis Mapper。
 * 负责访问 MySQL users 表，把阶段 1 的内存用户存储替换为可持久化的数据访问。
 */
@Mapper
public interface UserAccountMapper {

    @Insert("""
            INSERT INTO users (
                username, password_hash, real_name, phone, mail, avatar, created_at, updated_at, deleted
            ) VALUES (
                #{user.username}, #{user.passwordHash}, #{user.realName}, #{user.phone}, #{user.mail},
                #{user.avatar}, #{user.createTime}, #{user.updateTime}, #{user.deleted}
            )
            """)
    int insert(@Param("user") UserAccount user);

    @Select("""
            SELECT
                id,
                username,
                password_hash AS passwordHash,
                real_name AS realName,
                phone,
                mail,
                avatar,
                created_at AS createTime,
                updated_at AS updateTime,
                deleted
            FROM users
            WHERE username = #{username}
              AND deleted = 0
            LIMIT 1
            """)
    Optional<UserAccount> findByUsername(@Param("username") String username);

    @Select("""
            SELECT COUNT(1)
            FROM users
            WHERE username = #{username}
              AND deleted = 0
            """)
    long countByUsername(@Param("username") String username);
}
