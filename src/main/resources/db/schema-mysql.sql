/*
 阶段 4 MySQL 初始化脚本。
 users 表保存登录注册相关的强结构化数据；密码只保存 BCrypt 哈希，不保存明文。
 后续新增 MySQL 表和字段时，必须补充中文 COMMENT，方便在 DataGrip 中可视化理解表结构。
*/
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户主键ID',
    username VARCHAR(64) NOT NULL COMMENT '登录用户名',
    password_hash VARCHAR(120) NOT NULL COMMENT 'BCrypt加密后的密码哈希',
    real_name VARCHAR(64) COMMENT '真实姓名或展示昵称',
    phone VARCHAR(32) COMMENT '手机号',
    mail VARCHAR(120) COMMENT '邮箱地址',
    avatar VARCHAR(500) COMMENT '头像地址',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0表示正常，1表示删除',
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账户表，保存注册登录和用户资料基础信息';
