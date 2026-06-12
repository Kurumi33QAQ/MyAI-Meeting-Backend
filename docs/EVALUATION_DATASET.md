# Evaluation 测试集说明

本文档说明当前默认评测集的设计目的、覆盖范围和后续扩充规则。评测数据位于：

```text
src/main/resources/evaluation/eval_cases.json
src/test/resources/evaluation/eval_cases.json
```

## 当前覆盖范围

当前默认测试集包含 21 条中文样本，覆盖这些类别：

| 类别 | 主要验证内容 |
| --- | --- |
| `java_backend` | Spring Boot、认证安全、MySQL/MongoDB 分工等 Java 后端基础 |
| `ai_chat` | SSE 流式响应和 AI 对话工程化 |
| `agent` / `interview_agent` | Thought-Action-Observation、多 Agent 协同和 trace |
| `rag` / `rag_boundary` | 结构化 chunk、pgvector 向量召回、本地召回兜底、业务 rerank、RAG 边界和不能夸大的能力 |
| `interview_rule` | LiteFlow 追问规则链、追问质量和具体追问 |
| `interview_flow` | 自适应题量、可选岗位/JD、无岗位默认行为 |
| `ai_guard` | Redis Single-flight、限流、超时和中文降级 |
| `runtime` | Redis 热态和 MongoDB 冷快照恢复 |
| `evaluation` | 幻觉率、命中率、引用准确率等指标定义 |
| `media_boundary` | OpenAI Compatible ASR/TTS 与讯飞专有协议的边界 |
| `resume_boundary` | 文本型 PDF、扫描版 PDF 和 Tesseract OCR 边界 |
| `api_design` | 新旧接口隔离和 `frontendadapter` 适配层 |

## 单条 case 格式

```json
{
  "id": "rag-001",
  "question": "为什么简历 RAG 的 chunk 不应该只按固定字数切分？",
  "groundTruth": "标准答案",
  "evidence": [
    {
      "id": "ev-rag-001",
      "text": "可引用证据文本",
      "documentType": "PROJECT_DOC",
      "sectionName": "chunk策略",
      "tags": "rag,core,resume"
    }
  ],
  "category": "rag"
}
```

## 指标解释

| 指标 | 含义 |
| --- | --- |
| `hallucination_rate` | 无证据支持或与标准答案冲突的问题数 / 总问题数 |
| `answer_hit_rate` | 回答命中标准答案的问题数 / 总问题数 |
| `citation_accuracy` | 引用正确 evidence 的问题数 / 总问题数 |
| `avg_latency_ms` | 平均响应耗时 |

报告会输出两层表格：

1. `Overall Metrics`：按策略汇总。
2. `Category Breakdown`：按策略和类别拆分，方便判断某类问题是否薄弱。

## 运行方式

登录后调用：

```http
POST /api/evaluations/runs
Authorization: Bearer <token>
Content-Type: application/json

{
  "datasetPath": "classpath:evaluation/eval_cases.json",
  "writeReport": true
}
```

报告默认生成到：

```text
reports/evaluation/evaluation_report.json
reports/evaluation/evaluation_report.md
```

测试环境生成到：

```text
target/test-reports/evaluation
```

如果只想在本地直接生成正式报告文件，可以运行：

```bash
mvn -q "-Dtest=EvaluationReportExportTest" test
```

该测试会读取默认 21 条评测集，并把报告写入 `reports/evaluation`。如果后续接入真实模型评测，必须在报告中标注数据集规模、模型、是否启用 pgvector、是否启用真实 embedding。

## 简历使用规则

- 可以写“实现 evaluation 自动评测模块，支持四种策略对照和分类指标统计”。
- 没有真实运行正式报告前，不要写“幻觉率降低 X%”。
- 如果要写具体数字，必须注明测试集规模和评测条件，例如“在 21 条中文项目问答测试集上，使用当前本地评测器……”，并且数字必须来自生成的报告。
- 如果只是跑了确定性评测器，不能把结果包装成真实线上大模型效果；只有接入真实模型、真实 embedding 和真实样本后，才能写成更强的实验结论。

## 后续扩充方向

1. 增加真实简历和真实 JD 的 case，验证题目是否贴合项目经历。
2. 增加追问质量 case，统计无效追问率、重复追问率和追问额外延迟。
3. 增加 prompt injection case，验证低置信度拒答和安全边界。
4. 增加 `local_rag`、`pgvector_rag`、`pgvector_rag_with_rerank` 的独立对照，区分本地文本召回和 pgvector 向量召回的命中差异。
5. 增加真实 ASR/TTS 媒体链路成功率和平均延迟统计；如果以后接讯飞，需要单独增加讯飞协议 Provider 的验收 case。
