# Phase 9 Step 3 - Acceptance Record Example (Filled)

## 说明

本文件为 **`PHASE9_STEP3_ACCEPTANCE_RECORD_TEMPLATE.md` 的已填样例**，用于示范「合格留档」应包含哪些信息。归档时请替换为**真实环境、真实请求、真实响应摘要与证据路径**；下文中的 `analysisId`、时间、路径等均为演示占位。

配套文档：

- `PHASE9_STEP3_REVIEW_AGGREGATE_LAYERING_CONTRACT.md`
- `PHASE9_STEP3_ACCEPTANCE_RECORD_TEMPLATE.md`

---

## 基本信息（样例已填）

- 阶段：Phase 9
- Step：Step 3
- 验收主题：Review Aggregate 分层与性能护栏
- 验收环境：本地联调（Spring Boot 默认端口）
- 接口基地址：`http://localhost:8080`
- 验收批次/轮次：Step 3 文档轮 · 样例归档 v1
- 前端分支/版本：`feature/review-summary-detail-v1`（示例）
- 后端分支/版本：`feature/phase9-step3-layering`（示例）

---

## 1) 验收执行记录表（主表 · 三条场景样例）

> 下列三行分别对应：**summary 首屏轻载**、**detail + limit 护栏**、**错误语义（400/404）**。其余模板中的通用验收行可在正式验收时逐行补全。

| 验收项 | 预期结果 | 实际结果 | 证据链接/截图/接口返回 | 是否通过 | 负责人 | 日期 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 场景 A：summary 接口首屏摘要验证 | `GET /api/review/aggregate/{analysisId}/summary` 返回 `200`；包含 `run/decision/plan/reviewClosure/detailSections`；可用于首屏轻载 | `code=200`，`data.run/decision/plan/reviewClosure` 存在；`detailSections` 包含 `pushRecheck/missed/alerts/ruleVersionLogs/hotReset` | `curl` 见下文「场景 A」；原始 JSON：`artifacts/phase9/step3/summary-a-100.json`（占位） | 通过 | 张三 | 2026-04-17 | 验证首屏已不依赖 legacy 全量接口 |
| 场景 B：detail 接口 section + limit 护栏验证 | `GET .../detail?section=pushRecheck&limit=999` 返回 `200`；`limitApplied=50`；返回 `total/truncated`；且每条 push 的 `rechecks<=5` | `code=200`，`limitApplied=50`；`total` 有值；`truncated` 与条数一致；抽样检查 `rechecks` 最大长度为 `5` | `curl` 见下文「场景 B」；截图：`artifacts/phase9/step3/detail-limit-guardrail.png`（占位） | 通过 | 李四 | 2026-04-17 | 同时覆盖“外层 limit 护栏 + 内层 recheck 限流” |
| 场景 C：错误语义验证（非法 section 与不存在 analysis） | 非法 `section` 返回 `400`；不存在 `analysisId` 调 `summary/detail` 返回 `404` | 非法 `section` 返回 `400` 且 `msg` 含 `unsupported detail section`；不存在 `analysisId` 返回 `404` 且 `msg` 含 `analysis not found` | `curl` 见下文「场景 C」；原始返回：`artifacts/phase9/step3/error-semantics.json`（占位） | 通过 | 王五 | 2026-04-17 | 错误语义可直接给前端用于统一提示 |

---

## 2) 接口调用证据区（样例已填）

### 2.1 摘要接口证据（场景 A）

- 请求 URL：`http://localhost:8080/api/review/aggregate/a-100/summary`
- 请求参数：无（path 参数 `analysisId=a-100`）
- 响应摘要：`code=200`；`data.run.analysisId=a-100`；`data.detailSections` 包含 5 个 section
- 原始返回保存位置：`artifacts/phase9/step3/summary-a-100.json`（占位）

```bash
curl -sS "http://localhost:8080/api/review/aggregate/a-100/summary" | jq '.code, .data.run.analysisId, (.data.detailSections|map(.section))'
```

### 2.2 明细接口证据（场景 B）

- 请求 URL：`http://localhost:8080/api/review/aggregate/a-100/detail`
- 请求参数：`section=pushRecheck&limit=999`
- 响应摘要：`code=200`；`limitApplied=50`；含 `total/truncated`；`pushRecheck[].rechecks` 每项长度 `<=5`
- 原始返回保存位置：`artifacts/phase9/step3/detail-push-limit999.json`（占位）

```bash
curl -sS "http://localhost:8080/api/review/aggregate/a-100/detail?section=pushRecheck&limit=999" \
  | jq '.code, .data.limitApplied, .data.total, .data.truncated, ([.data.pushRecheck[].rechecks|length] | max)'
```

### 2.3 异常语义证据（场景 C）

- 用例描述：非法 `section` 与不存在 `analysisId` 的错误语义校验
- 请求 URL：
  - `http://localhost:8080/api/review/aggregate/a-100/detail?section=unknown`
  - `http://localhost:8080/api/review/aggregate/not-exists/summary`
  - `http://localhost:8080/api/review/aggregate/not-exists/detail?section=missed`
- 响应摘要：
  - 非法 section：`HTTP 400` + `unsupported detail section`
  - 不存在 analysis：`HTTP 404` + `analysis not found`
- 证据位置：`artifacts/phase9/step3/error-semantics.json`（占位）

```bash
curl -i "http://localhost:8080/api/review/aggregate/a-100/detail?section=unknown"
curl -i "http://localhost:8080/api/review/aggregate/not-exists/summary"
curl -i "http://localhost:8080/api/review/aggregate/not-exists/detail?section=missed"
```

---

## 3) 自动化测试证据区（样例）

- 执行命令：
  - `mvn test -Dtest=ReviewAggregateServiceImplTest`
- 结果摘要：
  - `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`（示例）
- 报告路径或截图：
  - `target/surefire-reports/TEST-org.example.trademodel.service.impl.ReviewAggregateServiceImplTest.xml`（以本机构建为准）
- 补充回归命令（如有）：
  - `mvn test -Dtest=ReviewAggregateServiceImplTest#detailLimitIsClampedToDefaultAndMax`

---

## 4) 最终验收结论区（样例）

- 本轮结论：**通过**
- 阻塞项（如有）：
  - 无
- 风险项（如有）：
  - 旧接口仍在兼容期，需在后续里程碑明确下线窗口
- 后续动作：
  - 前端按 contract 执行“先 summary 后 detail”接线，并在下一轮提交联调证据
- 结论确认人：
  - 验收负责人（占位）
- 结论日期：
  - 2026-04-17
