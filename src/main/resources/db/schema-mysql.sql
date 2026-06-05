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

CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '知识库文档主键ID',
    document_id VARCHAR(80) NOT NULL COMMENT '知识库文档业务ID',
    username VARCHAR(64) NOT NULL COMMENT '文档所属用户名',
    source_id VARCHAR(80) NOT NULL COMMENT '来源业务ID，例如 resumeId 或 interviewSessionId',
    document_type VARCHAR(40) NOT NULL COMMENT '文档类型：RESUME简历，JOB_DESCRIPTION岗位JD，QUESTION_BANK面试题库',
    title VARCHAR(200) NOT NULL COMMENT '文档标题，便于 DataGrip 可视化识别来源',
    tags VARCHAR(500) COMMENT '文档标签，逗号分隔',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0表示正常，1表示删除',
    UNIQUE KEY uk_knowledge_document_document_id (document_id),
    KEY idx_knowledge_document_user_type (username, document_type),
    KEY idx_knowledge_document_source (username, source_id, document_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档表，保存简历、岗位JD和题库等文档级元数据';

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '知识片段主键ID',
    chunk_id VARCHAR(80) NOT NULL COMMENT '知识片段业务ID，也是回答引用 evidenceId',
    document_id VARCHAR(80) NOT NULL COMMENT '所属知识库文档业务ID',
    username VARCHAR(64) NOT NULL COMMENT '片段所属用户名',
    source_id VARCHAR(80) NOT NULL COMMENT '来源业务ID，例如 resumeId 或 interviewSessionId',
    document_type VARCHAR(40) NOT NULL COMMENT '文档类型：RESUME简历，JOB_DESCRIPTION岗位JD，QUESTION_BANK面试题库',
    section_name VARCHAR(80) NOT NULL COMMENT '业务章节名，例如项目经历、技能栈、岗位职责、任职要求',
    chunk_index INT NOT NULL DEFAULT 0 COMMENT '同一章节内的片段序号',
    section_order INT NOT NULL DEFAULT 0 COMMENT '章节顺序，用于还原文档结构',
    content MEDIUMTEXT NOT NULL COMMENT '片段正文内容，用于检索、rerank和回答引用',
    summary VARCHAR(1000) COMMENT '片段摘要，便于列表展示和快速理解',
    tags VARCHAR(500) COMMENT '片段标签，逗号分隔',
    metadata_json JSON COMMENT '片段元数据JSON，保存documentType、sectionName、chunkIndex等结构化信息',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0表示正常，1表示删除',
    UNIQUE KEY uk_knowledge_chunk_chunk_id (chunk_id),
    KEY idx_knowledge_chunk_user_type (username, document_type),
    KEY idx_knowledge_chunk_source (username, source_id, document_type),
    KEY idx_knowledge_chunk_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识片段表，保存结构化chunk和RAG证据引用内容';

CREATE TABLE IF NOT EXISTS evaluation_run (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评测任务主键ID',
    run_id VARCHAR(80) NOT NULL COMMENT '评测任务业务ID',
    username VARCHAR(64) NOT NULL COMMENT '评测任务创建用户',
    dataset_name VARCHAR(200) NOT NULL COMMENT '测试集名称或路径',
    total_cases INT NOT NULL DEFAULT 0 COMMENT '本次评测样本总数',
    baseline_summary_json JSON COMMENT 'baseline方案指标汇总JSON',
    rag_without_rerank_summary_json JSON COMMENT '基础RAG不带rerank方案指标汇总JSON',
    rag_with_rerank_summary_json JSON COMMENT '结构化chunk加rerank方案指标汇总JSON',
    self_check_rag_summary_json JSON COMMENT 'RAG加自检拒答方案指标汇总JSON',
    report_json_path VARCHAR(500) COMMENT '评测JSON报告输出路径',
    report_markdown_path VARCHAR(500) COMMENT '评测Markdown报告输出路径',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间',
    completed_at DATETIME(6) NOT NULL COMMENT '完成时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0表示正常，1表示删除',
    UNIQUE KEY uk_evaluation_run_run_id (run_id),
    KEY idx_evaluation_run_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评测任务表，保存RAG和Agent效果评测的汇总指标';

CREATE TABLE IF NOT EXISTS evaluation_case_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评测明细主键ID',
    run_id VARCHAR(80) NOT NULL COMMENT '所属评测任务业务ID',
    case_id VARCHAR(80) NOT NULL COMMENT '测试样本ID',
    strategy VARCHAR(60) NOT NULL COMMENT '评测方案：baseline、rag_without_rerank、rag_with_rerank、self_check_rag',
    category VARCHAR(80) COMMENT '测试样本分类，例如java_backend、interview、rag',
    question VARCHAR(1000) NOT NULL COMMENT '测试问题',
    answer MEDIUMTEXT COMMENT '方案生成的回答',
    ground_truth MEDIUMTEXT NOT NULL COMMENT '标准答案',
    cited_evidence_ids VARCHAR(1000) COMMENT '回答引用的证据ID，逗号分隔',
    answer_correct TINYINT NOT NULL DEFAULT 0 COMMENT '回答是否命中标准答案，1表示命中',
    hallucinated TINYINT NOT NULL DEFAULT 0 COMMENT '回答是否判定为幻觉，1表示存在幻觉',
    citation_correct TINYINT NOT NULL DEFAULT 0 COMMENT '引用证据是否正确，1表示正确',
    latency_ms BIGINT NOT NULL DEFAULT 0 COMMENT '单条样本响应耗时，单位毫秒',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间',
    KEY idx_evaluation_case_run (run_id),
    KEY idx_evaluation_case_strategy (run_id, strategy),
    KEY idx_evaluation_case_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评测样本结果表，保存每条case在不同方案下的明细指标';
