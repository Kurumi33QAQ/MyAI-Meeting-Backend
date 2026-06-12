# MyAI-Meeting-Backend

这是一个面向 Java 后端学习和简历展示的 AI 模拟面试 Agent 后端项目。项目参考 `AI-Meeting` 的功能方向，但不照搬原项目包名、接口路径、Prompt、README 表达和作者风格，而是重构为自己的模块结构、接口风格和工程亮点。

当前后端已经能对接 `AI-Meeting-Frontend` 的核心页面：登录注册、AI 对话、SSE 流式输出、会话历史、简历上传、模拟面试、追问、报告、历史面试和媒体降级链路。

## 已完成能力

| 模块 | 已落地能力 | 当前边界 |
| --- | --- | --- |
| 认证 | Sa-Token 登录态、BCrypt 密码加密、MySQL 用户持久化、旧前端登录路径兼容 | 不再使用 JWT 作为最终方案，旧 JWT 阶段只保留在计划复盘中 |
| AI 对话 | Spring AI 兼容 OpenAI/DeepSeek 风格模型、同步对话、SSE 流式输出、MongoDB 会话消息快照 | SSE 已支持流式发送，真实效果依赖模型服务是否支持流式返回 |
| Agent | Thought-Action-Observation 执行循环、工具调用、Agent run/step trace 落 MongoDB | 通用 Agent 先支持基础工具，复杂工具生态仍可扩展 |
| 模拟面试 | PDF/文本简历解析、目标岗位/公司/JD 可选输入、Tavily 岗位情报检索、多 Agent 协同出题、评分、追问和报告 | 扫描版 PDF 需要后续 OCR；岗位搜索必须由用户输入触发 |
| 多轮追问 | LiteFlow 规则链裁决是否追问，专用生成器生成具体追问，支持 `F1/F2/F3` 多轮追问链 | 追问质量仍需要更大测试集继续评估 |
| 自适应题量 | 默认 8 道起步，根据表现扩展到 12/15 道，前端展示已答题数和当前总题数 | 整场真实面试样本仍需继续积累 |
| RAG | 简历/JD 结构化 chunk、本地召回、rerank、evidence 引用、证据检索接口 | 当前不是向量数据库，也不是深度学习 reranker，不能写成已接入 Milvus/pgvector |
| Evaluation | `baseline`、`rag_without_rerank`、`rag_with_rerank`、`self_check_rag` 四种策略评估，输出 JSON/Markdown 报告 | 小样本 sanity 指标不能直接写进正式简历，正式指标必须用扩充测试集重新跑 |
| 稳定性治理 | Redis 限流、Single-flight、调用超时、失败降级、Guard 状态接口 | 当前未引入 Redisson；SSE 回放仍是后续增强点 |
| 长会话恢复 | Redis 热态、MongoDB 冷快照、后端重启/缓存丢失后的恢复接口 | 前端页面级刷新恢复仍建议继续人工复测 |
| 媒体能力 | WebSocket Sa-Token 鉴权、ASR 降级转写事件、TTS 任务/查询/音频下载降级闭环 | 没有接入真实讯飞 ASR/TTS；TTS 当前是静音 WAV 占位音频 |

## 技术栈

- Java 17、Spring Boot 3
- Sa-Token、BCrypt
- Spring AI OpenAI Compatible API
- MyBatis、MySQL
- Spring Data MongoDB
- Spring Data Redis
- LiteFlow
- Spring Web MVC、SSE、WebSocket
- PDFBox
- JUnit 5、Spring Boot Test

## 快速启动

### 1. 准备基础服务

MySQL 默认连接：

```text
数据库：my_ai_meeting
用户名：root
密码：1234
```

MongoDB 默认连接：

```text
mongodb://localhost:27017/my_ai_meeting
```

Redis 默认连接：

```text
localhost:6379
```

可以用 Docker 快速启动完整开发依赖：

```powershell
docker compose -f docker-compose.dev.yml up -d
```

`docker-compose.dev.yml` 中 MySQL 映射到本机 `3307`，避免和你电脑已有的 `3306` MySQL 冲突。如果使用 compose 内的 MySQL 启动后端，请额外设置：

```powershell
$env:MYSQL_URL="jdbc:mysql://localhost:3307/my_ai_meeting?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
```

### 2. 配置环境变量

推荐使用本机已有变量名：

```powershell
$env:OPENAI_API_KEY="你的模型 Key"
$env:OPENAI_BASE_URL="https://api.deepseek.com"
$env:MODEL_ID="deepseek-v4-flash"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="1234"
$env:MONGODB_URI="mongodb://localhost:27017/my_ai_meeting"
```

如果需要联网岗位情报：

```powershell
$env:TAVILY_API_KEY="你的 Tavily Key"
```

没有 `TAVILY_API_KEY` 时，系统不会伪装联网搜索结果，会自动降级为“简历 + 用户填写岗位/JD”出题。

### 3. 启动后端

```powershell
mvn spring-boot:run
```

默认端口：

```text
http://localhost:8002
```

健康检查：

```powershell
Invoke-RestMethod http://localhost:8002/api/health
```

运行环境自检：

```powershell
Invoke-RestMethod http://localhost:8002/api/system/readiness
```

自检接口会检查 MySQL/H2、MongoDB、Redis、AI 配置和 evaluation 数据集是否可用，不会输出 API Key 明文。真实模型模式下如果缺少 `OPENAI_API_KEY` 或 `OPENAI_BASE_URL`，该接口会显示 `DEGRADED`，方便先排查环境再联调页面。

### 4. 运行测试

```powershell
mvn test
```

## 数据库职责

| 存储 | 职责 |
| --- | --- |
| MySQL | 用户、简历文件、知识库文档/chunk、面试记录、evaluation run/case result 等结构化主数据 |
| MongoDB | AI 会话消息、Agent run/step trace、面试会话快照、题目快照、运行时快照等长文本和半结构化数据 |
| Redis | Sa-Token 登录态、AI 限流、Single-flight 锁、AI 结果缓存、面试热态恢复数据 |

MySQL 建表脚本位于 `src/main/resources/db/schema-mysql.sql`，字段均要求维护中文 COMMENT。MongoDB 没有 MySQL 式列备注，字段含义通过 Java 文档实体中文注释、接口文档和样例说明维护。

## 常用接口

详细接口见 [docs/API.md](docs/API.md)。

核心新风格接口：

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/ai/chat`
- `POST /api/ai/chat/stream`
- `POST /api/ai/sessions`
- `POST /api/resumes/upload`
- `POST /api/interview-sessions`
- `POST /api/interview-sessions/{sessionId}/questions`
- `POST /api/interview-sessions/{sessionId}/answers`
- `GET /api/interviews/{sessionId}/report`
- `POST /api/retrieval/evidence`
- `POST /api/evaluations/runs`
- `GET /api/ai/guard/health`

兼容旧前端的 `/api/xunzhi/v1/**` 路径仍保留在 `frontendadapter` 模块中，方便现有前端逐步迁移。

## 项目文档

- [架构说明](docs/ARCHITECTURE.md)
- [接口文档](docs/API.md)
- [模拟面试 Agent 讲解](docs/INTERVIEW_AGENT_PLAYBOOK.md)
- [Evaluation 测试集说明](docs/EVALUATION_DATASET.md)
- [运行与验收手册](docs/OPERATIONS.md)
- [简历与面试表达](docs/RESUME_GUIDE.md)
- [项目面试讲解手册](docs/PROJECT_PRESENTATION.md)

## 不能夸大的地方

以下内容已经在代码中有边界控制，README 和简历也必须如实表达：

- 当前 RAG 是结构化 chunk + 本地召回 + rerank，不是向量数据库方案。
- 当前 evaluation 已内置 21 条中文项目问答测试集并支持分类报告；正式简历数据必须来自你实际运行生成的报告。
- 当前 ASR/TTS 是本地降级闭环，不是真实讯飞实时语音识别/合成。
- 当前神态/表情分析没有实现，不能写进已完成能力。
- 当前 PDF 解析支持文本型 PDF，扫描版 PDF 需要后续 OCR。
