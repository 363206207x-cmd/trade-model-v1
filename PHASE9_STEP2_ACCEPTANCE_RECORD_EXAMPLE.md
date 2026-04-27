# Phase 9 Step 2 - Acceptance Record Example (Filled)

## 说明

本文件为 **`PHASE9_STEP2_ACCEPTANCE_RECORD_TEMPLATE.md` 的已填样例**，用于示范「合格落档」应包含哪些信息。归档时请替换为**真实环境、真实请求、真实响应摘要与证据路径**；下表中的 `analysisId`、时间、路径等均为演示占位。

配套文档：

- `PHASE9_STEP2_RULE_VERSION_LOG_RETRIEVAL_GUIDE.md`
- `PHASE9_STEP2_ACCEPTANCE_RECORD_TEMPLATE.md`

---

## 基本信息（样例已填）

- 阶段：Phase 9
- Step：Step 2
- 验收主题：Rule Version Log 可检索升级
- 验收环境：本地联调（Spring Boot 默认端口）
- 接口基地址：`http://localhost:8080`
- 验收批次/轮次：Step 2 文档轮 · 样例归档 v1

---

## 1) 验收执行记录表（主表 · 三条场景样例）

> 下列三行分别对应：**按 analysisId 混查新旧形态数据**、**errorType + 时间窗**、**fallback 命中**。其余模板中的通用行可在正式验收时逐行补全。

| 验收项 | 预期结果 | 实际结果 | 证据链接/截图/接口返回 | 是否通过 | 负责人 | 日期 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 场景 A：按 `analysisId` 检索（同一次分析下新行与旧行形态可共存） | `GET .../rule-version-logs?analysisId=a-100&limit=20` 返回 `200`；`data` 为数组；新数据行结构化字段齐全；旧行若列缺失，响应中 `analysisId`/`errorType`/`changeCategory` 仍可读且 `fallbackMatched` 与实现一致 | `code=200`，`data.length>=1`；含 `ruleVersion` 与 `changeSummary`；至少一行 `fallbackMatched=false`（新形态）或混有 `true`（旧形态） | `curl` 见下文「场景 A」；原始 JSON：`artifacts/phase9/step2/rule-version-logs-a-100.json`（占位） | 通过 | 张三 | 2026-04-17 | 新旧是否同批出现取决于库内数据；无旧行时只验证新行即可 |
| 场景 B：按 `errorType` + 时间窗组合过滤 | 仅命中 `errorType=DATA_ISSUE` 且 `created_at` 落在窗口内的行；`limit` 生效 | 返回条数 `<=20`（或显式 `limit`）；每条 `errorType` 与过滤一致或为兼容解析等价 | `curl` 见下文「场景 B」；截图：`artifacts/phase9/step2/error-type-window.png`（占位） | 通过 | 李四 | 2026-04-17 | 时间字符串需与 DB/接口约定一致（见指南） |
| 场景 C：`fallbackMatched=true`（结构化列缺失、`changeSummary` 可解析） | 当 `analysis_id`/`error_type`/`change_category` 在库中缺失但 `changeSummary` 含 `key=value` 片段时，列表项展示补齐字段；`fallbackMatched=true` | 命中行 `fallbackMatched=true`；`changeSummary` 仍保留原文；`analysisId` 等与解析结果一致 | `curl` 见下文「场景 C」；`mvn test -Dtest=RuleVersionLogQueryServiceImplTest#listByAnalysisId_fillsFieldsFromChangeSummaryWhenStructuredColumnsMissing` 报告节选（占位） | 通过 | 王五 | 2026-04-17 | 无真实旧数据时，以自动化用例 + 手工造数二选一作为证据即可 |

---

## 2) 推荐证据清单（样例已填）

### 2.1 接口调用证据

**场景 A · 按 analysisId**

- 请求 URL：`http://localhost:8080/api/review/rule-version-logs`
- 请求参数：`analysisId=a-100&limit=20`
- 响应摘要：`code=200`，`msg=success`，`data` 非空时检查首条 `id`、`analysisId`、`ruleVersion`、`createdAt`、`fallbackMatched`
- 原始返回保存位置：`artifacts/phase9/step2/rule-version-logs-a-100.json`（占位，请替换为团队约定目录）

```bash
curl -sS "http://localhost:8080/api/review/rule-version-logs?analysisId=a-100&limit=20" | jq '.code, (.data|length), .data[0].fallbackMatched'
```

**场景 B · errorType + 时间窗**

```bash
curl -sS "http://localhost:8080/api/review/rule-version-logs?errorType=DATA_ISSUE&createdAtFrom=2026-04-01T00:00:00&createdAtTo=2026-04-17T23:59:59&limit=20"
```

**场景 C · 旧数据兼容（可与单测证据二选一）**

```bash
curl -sS "http://localhost:8080/api/review/rule-version-logs?analysisId=a-legacy-1&limit=10"
```

### 2.2 自动化测试证据（样例）

- 执行命令：`mvn test -Dtest=RuleVersionLogQueryServiceImplTest`
- 结果摘要：`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`
- 报告路径：`target/surefire-reports/TEST-org.example.trademodel.service.impl.RuleVersionLogQueryServiceImplTest.xml`（以本机构建为准）

---

## 3) 验收结论（样例）

- 本轮结论：**通过**
- 阻塞项：无
- 后续动作：正式验收时复制 `PHASE9_STEP2_ACCEPTANCE_RECORD_TEMPLATE.md`，将本样例中的占位路径替换为真实证据，并补全模板中其余通用验收行
- 结论确认人：验收负责人（占位）
- 结论日期：2026-04-17
