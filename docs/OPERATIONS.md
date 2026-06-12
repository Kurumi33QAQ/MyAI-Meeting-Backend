# 运行与验收手册

## 启动命令

启动开发依赖：

```powershell
docker compose -f docker-compose.dev.yml up -d
```

如果你使用本机 MySQL `localhost:3306`，保持默认配置即可。如果你使用 `docker-compose.dev.yml` 里的 MySQL，因为它映射到本机 `3307`，启动后端前需要设置：

```powershell
$env:MYSQL_URL="jdbc:mysql://localhost:3307/my_ai_meeting?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
```

如果要启用 pgvector 向量检索，先启动 compose 里的 `myai-pgvector`，再设置：

```powershell
$env:RAG_VECTOR_ENABLED="true"
$env:PGVECTOR_JDBC_URL="jdbc:postgresql://localhost:5432/my_ai_meeting_vector"
$env:PGVECTOR_USERNAME="postgres"
$env:PGVECTOR_PASSWORD="1234"
$env:RAG_EMBEDDING_MODEL="text-embedding-3-small"
```

如果要启用扫描 PDF OCR，需要本机已安装 Tesseract 和中文/英文语言包：

```powershell
$env:RESUME_OCR_ENABLED="true"
$env:RESUME_OCR_COMMAND="tesseract"
$env:RESUME_OCR_LANGUAGE="chi_sim+eng"
```

如果要启用真实 ASR/TTS 音频接口：

```powershell
$env:MEDIA_ASR_PROVIDER="openai"
$env:MEDIA_ASR_MODEL="whisper-1"
$env:MEDIA_TTS_PROVIDER="openai"
$env:MEDIA_TTS_MODEL="tts-1"
$env:MEDIA_TTS_VOICE="alloy"
```

后端：

```powershell
mvn spring-boot:run
```

前端：

```powershell
cd D:\javaee\AI-Meeting-Frontend
npm run dev
```

后端默认端口 `8002`，前端默认端口 `5173`。

## 查看端口

查看所有监听端口：

```powershell
Get-NetTCPConnection -State Listen | Sort-Object LocalPort | Select-Object LocalAddress,LocalPort,OwningProcess
```

查看某个端口：

```powershell
Get-NetTCPConnection -LocalPort 8002
```

根据 PID 查看进程：

```powershell
Get-Process -Id <PID>
```

停止进程：

```powershell
Stop-Process -Id <PID>
```

## 后端自动化测试

```powershell
mvn test
```

重点测试覆盖：

- 用户注册登录和 Sa-Token 鉴权
- AI 同步和 SSE
- MongoDB 聊天存储
- Agent run/step trace
- RAG chunk/retrieval/rerank
- evaluation 指标统计
- Redis AI Guard
- 模拟面试、追问和报告
- WebSocket ASR 降级和 TTS 音频下载

## 运行环境自检

后端启动后先调用：

```powershell
Invoke-RestMethod http://localhost:8002/api/system/readiness
```

重点看这些字段：

- `data.status`：`UP` 表示核心依赖可用，`DEGRADED` 表示至少有一个依赖异常。
- `data.dependencies.mysql`：检查用户、面试记录、评测报告等结构化数据存储。
- `data.dependencies.mongodb`：检查聊天消息、Agent trace、面试运行快照等半结构化数据存储。
- `data.dependencies.redis`：检查 Sa-Token 登录态、AI Guard、Single-flight 和热态缓存。
- `data.dependencies.ai`：检查 mock/真实模型模式、模型名、API Key 和 Base URL 是否配置完整。
- `data.dependencies.evaluation.caseCount`：检查 evaluation 默认测试集是否能被加载。
- `data.dependencies.pgvector`：检查 pgvector 是否启用、向量维度、embedding 模型和 Key 配置。
- `data.dependencies.ocr`：检查 OCR 是否启用、Tesseract 命令和语言包配置。
- `data.dependencies.media`：检查 ASR/TTS 当前使用本地降级还是 OpenAI Compatible 真实音频接口。

这个接口不会发起真实大模型或音频调用，也不会输出 API Key 明文。真实模型、pgvector、OCR 或音频联调失败时，建议先确认 readiness 对应依赖是否为 `UP`。

## 手动验收顺序

1. 注册用户，确认 MySQL `users` 有记录且密码不是明文。
2. 登录用户，确认前端保存 token。
3. 发送 AI 对话，确认 MongoDB 有 `chat_conversation` 和 `chat_message`。
4. 上传简历，确认 MySQL `uploaded_file` 有记录。
5. 填写或不填写岗位信息分别开始面试，确认题目上下文不同。
6. 回答“不知道”，确认得分低且触发追问。
7. 回答高质量答案，确认不强行追问。
8. 查看报告，确认问答回放、分数和追问链存在。
9. 刷新页面或重启后端，调用 restore/recover，确认会话可恢复。
10. 启用 pgvector 后重新上传简历或重新创建会话，让 chunk 写入向量索引，再调用 `/api/retrieval/evidence`。
11. 启用 OCR 后上传扫描版 PDF，确认不再提示“缺少文字层”，而是进入 Tesseract 识别。
12. 启用 `MEDIA_ASR_PROVIDER=openai` / `MEDIA_TTS_PROVIDER=openai` 后再测试语音链路；默认 local 模式只用于本地降级验收。

## DataGrip 可视化建议

MySQL：

```text
Host: localhost
Port: 3306
Database: my_ai_meeting
User: root
Password: 1234
```

MongoDB：

```text
URI: mongodb://localhost:27017/my_ai_meeting
```

Redis 可以使用 DataGrip Redis 连接或命令行查看：

```powershell
docker exec -it myai-redis redis-cli
keys meetingagent:*
```

## 常见问题

### 登录成功后上传简历提示 Unauthorized

优先检查：

- 前端 token 是否仍存在。
- Network 请求头是否有 `Authorization: Bearer <token>`。
- 后端是否重启后登录态丢失。
- Sa-Token Redis 是否可用。

### Mock 改成 false 仍像没有调用模型

优先检查：

- `OPENAI_API_KEY` 是否在当前终端生效。
- `OPENAI_BASE_URL` 是否指向兼容 OpenAI 的地址。
- `MODEL_ID` 是否是供应商支持的模型。
- `AI_GUARD_CALL_TIMEOUT` 是否过短。
- 后端日志是否出现模型调用失败后的中文降级提示。

### 面试题太泛

优先检查：

- 上传的 PDF 是否为文本型 PDF；如果是扫描版 PDF，确认 `RESUME_OCR_ENABLED=true`、Tesseract 命令可执行，并且已安装 `chi_sim`/`eng` 语言包。
- 岗位/公司/JD 是否真的传到了后端。
- `TAVILY_API_KEY` 是否配置，联网岗位情报是否成功。
- MongoDB 题目快照里的 evidence 和 agent trace 是否包含真实简历项目。

### MongoDB 看不到记录

确认聊天或面试是否真的走了后端接口，并刷新 DataGrip 的 MongoDB 数据源。常见集合：

- `chat_conversation`
- `chat_message`
- `agent_run`
- `agent_step_trace`
- `interview_session`
- `interview_question_snapshot`
- `interview_runtime_snapshot`
