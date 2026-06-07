# 简历与面试表达

本文档只写当前项目已经落地或明确降级的能力，不能编造未跑出的指标。

## 简历项目描述

可以写：

> MyAI-Meeting-Backend：基于 Spring Boot 3、Spring AI、Sa-Token、MySQL、MongoDB、Redis 和 LiteFlow 实现的 AI 模拟面试 Agent 后端。系统支持简历解析、目标岗位/JD 可选输入、RAG evidence 检索、多 Agent 协同出题、SSE 流式对话、LiteFlow 追问裁决、自适应题量、长会话恢复、Redis AI 调用治理和 evaluation 自动评测。

## 可写亮点

1. 基于 Sa-Token 实现登录认证，并用 MySQL 持久化用户、简历、面试记录和评估报告。
2. 基于 MongoDB 存储聊天消息、Agent trace、面试题快照和运行时快照，支持长会话恢复。
3. 基于 Spring AI 接入兼容 OpenAI 协议的大模型，并实现同步对话和 SSE 流式响应。
4. 设计多 Agent 协同面试编排，将简历分析、岗位上下文、出题、评分和总结拆成可追踪角色。
5. 使用 LiteFlow 做追问规则裁决，结合专用追问生成器实现 `F1/F2/F3` 多轮追问。
6. 设计结构化 chunk 策略，将简历、岗位 JD、岗位情报切分成可检索 evidence。
7. 使用本地召回 + rerank + 自检拒答构建 RAG 防幻觉 MVP，并通过 evaluation 模块统计效果。
8. 使用 Redis 实现 AI 调用限流、Single-flight、超时和降级，减少重复请求和模型不稳定影响。
9. 支持 WebSocket 语音转写和 TTS 接口的本地降级闭环，覆盖 HTTP、SSE、WebSocket 三种交互方式。

## 不能写成已完成的内容

- 不能写“已接入 Milvus/pgvector/向量数据库”，当前没有。
- 不能写“深度 reranker”，当前 rerank 是本地业务特征重排序。
- 不能写“已接入讯飞实时 ASR/TTS”，当前是本地降级闭环。
- 不能写“神态分析评分已上线”，当前没有实现。
- 不能提前写“幻觉率降低 X%”，除非使用正式测试集跑出了报告。

## 面试官可能追问

### 为什么 MySQL 和 MongoDB 都用

MySQL 适合保存用户、简历元数据、面试记录、知识库主数据和评测记录，因为这些数据结构稳定、查询维度明确。MongoDB 适合保存 AI 会话消息、Agent trace、题目快照和运行时快照，因为这些字段长文本多、结构会随 Prompt 和 Agent 版本变化。

### 为什么不用固定 5 题

固定 5 题更像 demo，不像真实面试。当前后端默认 8 道起步，根据回答质量扩到 12/15 道，追问独立挂在主问题下。这样既能覆盖基础、项目、架构和排障，也能让用户感知面试过程是动态的。

### 为什么 RAG 先不用向量库

阶段目标是先跑通“结构化 chunk -> evidence 召回 -> rerank -> 引用 -> evaluation”的闭环。当前用本地召回更容易控制和验证，后续可以替换为 Spring AI VectorStore 或向量数据库，而不改变上层接口。

### 为什么媒体能力做降级

ASR/TTS 依赖供应商、密钥、费用、浏览器权限和音频格式转换。项目核心价值是 AI 面试 Agent，因此先实现 WebSocket 鉴权、事件格式、音频帧接收和 TTS 任务接口，保证前端链路能跑；真实供应商后续只替换 service 实现。

