# 接口文档

统一响应格式：

```json
{
  "code": "0",
  "message": "success",
  "data": {}
}
```

除注册、登录、健康检查和 TTS 音频下载外，请求一般需要携带：

```text
Authorization: Bearer <token>
```

## 认证与用户

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register` | 注册用户 |
| POST | `/api/auth/login` | 登录并返回 Sa-Token token |
| GET | `/api/auth/me` | 获取当前用户 |
| POST | `/api/auth/logout` | 退出登录 |

登录请求示例：

```json
{
  "username": "buyer_001",
  "password": "123456"
}
```

## AI 对话

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/ai/models` | 获取模型选项 |
| POST | `/api/ai/chat` | 同步 AI 对话 |
| POST | `/api/ai/sessions` | 创建聊天会话 |
| GET | `/api/ai/sessions` | 查询聊天会话 |
| GET | `/api/ai/sessions/{sessionId}/messages` | 查询会话消息 |
| POST | `/api/ai/chat/stream` | SSE 流式输出 |

同步对话请求：

```json
{
  "message": "Spring Boot 自动配置是什么？",
  "model": "deepseek-v4-flash"
}
```

## Agent

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/agent-runs` | 启动一次 Agent 执行 |
| GET | `/api/agent-runs/{runId}` | 查看 Agent run 和 step trace |

## 简历

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/resumes/text` | 上传文本简历 |
| POST | `/api/resumes/upload` | 上传文件简历 |
| GET | `/api/resumes/{resumeId}` | 查询简历 |

## 模拟面试

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/interview-sessions` | 创建面试会话 |
| GET | `/api/interview-sessions` | 分页查询面试会话 |
| POST | `/api/interview-sessions/{sessionId}/questions` | 生成面试题 |
| POST | `/api/interview-sessions/{sessionId}/answers` | 提交回答 |
| GET | `/api/interview-sessions/{sessionId}` | 查询会话详情 |
| GET | `/api/interview-sessions/{sessionId}/restore` | 恢复前端运行态 |
| GET | `/api/interview-sessions/{sessionId}/resume/preview` | 预览简历 PDF |
| GET | `/api/interview-sessions/{sessionId}/agent-traces` | 查看多 Agent 轨迹 |
| GET | `/api/interview-sessions/{sessionId}/runtime-state` | 查看运行时快照 |
| POST | `/api/interview-sessions/{sessionId}/recover` | 从冷快照恢复运行时 |
| GET | `/api/interviews/{sessionId}/report` | 查看面试报告 |
| GET | `/api/interviews` | 分页查询历史面试记录 |

创建面试请求：

```json
{
  "resumeId": "resume-id",
  "jobTitle": "Java 后端开发实习生",
  "companyName": "字节跳动",
  "jobDescription": "负责服务端接口开发、缓存和消息队列治理",
  "questionCount": null
}
```

岗位、公司和 JD 都是可选字段。如果全部为空，系统只基于简历出题；如果用户填写，系统才会触发岗位情报检索。

提交回答请求：

```json
{
  "questionId": "Q1",
  "answer": "我在项目中负责用户权限模块，使用 Sa-Token 做登录态..."
}
```

追问返回中会出现：

```json
{
  "isFollowUp": true,
  "questionId": "Q1",
  "followUpQuestion": "你刚才提到 Redis 缓存，请说明缓存 key 设计、过期策略以及如何避免脏数据。",
  "nextQuestionNumber": "Q1-F1",
  "answeredCount": 1,
  "questionCount": 8
}
```

## RAG 与 Evaluation

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/retrieval/evidence` | 检索 evidence |
| POST | `/api/evaluations/runs` | 运行评估 |
| GET | `/api/evaluations/runs/{runId}` | 查询评估报告 |

评估策略包括：

- `baseline`
- `rag_without_rerank`
- `rag_with_rerank`
- `self_check_rag`

## AI Guard

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/ai/guard/health` | 查看 Redis/Guard 健康状态 |
| GET | `/api/ai/guard/stats` | 查看调用指标快照 |

## 媒体能力

| 类型 | 路径 | 说明 |
| --- | --- | --- |
| WebSocket | `/api/ws/speech/transcription/{userId}?token=<token>` | 新风格语音转写连接 |
| WebSocket | `/api/xunzhi/v1/xunfei/audio-to-text/{userId}?token=<token>` | 旧前端兼容语音转写连接 |
| POST | `/api/media/tts/tasks` | 创建 TTS 任务 |
| POST | `/api/media/tts/synthesize` | 同步合成 |
| GET | `/api/media/tts/tasks/{taskId}` | 查询任务 |
| GET | `/api/media/tts/tasks/{taskId}/audio` | 下载音频 |

当前 ASR/TTS 是本地降级链路：ASR 返回中文降级转写提示，TTS 返回短静音 WAV，用于保证前端链路可运行。

## 旧前端兼容接口

旧前端仍可调用 `/api/xunzhi/v1/**`，但核心业务推荐使用新接口。兼容层只做路径和字段适配，不承载核心业务逻辑。

