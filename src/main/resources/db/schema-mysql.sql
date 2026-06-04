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

CREATE TABLE IF NOT EXISTS uploaded_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '上传文件主键ID',
    file_id VARCHAR(80) NOT NULL COMMENT '文件业务ID，对外作为简历ID使用',
    username VARCHAR(64) NOT NULL COMMENT '文件所属用户名',
    file_name VARCHAR(120) NOT NULL COMMENT '原始文件名或用户填写的文件名',
    content_type VARCHAR(120) COMMENT '文件 MIME 类型',
    file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小，单位字节',
    document_type VARCHAR(32) NOT NULL COMMENT '文档类型，当前 RESUME 表示简历',
    text_content MEDIUMTEXT NOT NULL COMMENT '解析后的文本内容，阶段6先保存文本简历正文',
    summary VARCHAR(1000) COMMENT '简历摘要，供面试题生成和列表展示使用',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0表示正常，1表示删除',
    UNIQUE KEY uk_uploaded_file_file_id (file_id),
    KEY idx_uploaded_file_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上传文件表，保存简历文本和文件元信息';

CREATE TABLE IF NOT EXISTS interview_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '面试记录主键ID',
    session_id VARCHAR(80) NOT NULL COMMENT '面试会话业务ID',
    username VARCHAR(64) NOT NULL COMMENT '面试所属用户名',
    resume_id VARCHAR(80) NOT NULL COMMENT '关联的简历ID，对应 uploaded_file.file_id',
    job_title VARCHAR(120) NOT NULL COMMENT '目标岗位名称',
    status VARCHAR(32) NOT NULL COMMENT '面试状态，CREATED/QUESTION_GENERATED/ANSWERING/COMPLETED',
    question_count INT NOT NULL DEFAULT 0 COMMENT '计划题目数量',
    answered_count INT NOT NULL DEFAULT 0 COMMENT '已回答题目数量',
    total_score INT COMMENT '总分，所有已评分题目的平均分',
    report_summary VARCHAR(2000) COMMENT '面试报告摘要和改进建议',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0表示正常，1表示删除',
    UNIQUE KEY uk_interview_record_session_id (session_id),
    KEY idx_interview_record_username (username),
    KEY idx_interview_record_resume_id (resume_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模拟面试记录表，保存面试结构化状态和报告摘要';
