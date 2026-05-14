# PHASE: PlanBoundarySourceTraceAdapter 本地单测结果记录（PR61）

## 1. 阶段目标

本阶段记录 `DefaultPlanBoundarySourceTraceAdapterTest` 的本地单测结果，补齐 PR #60 后的验证记录。

本 PR 只新增验证记录文档，不修改 Java、不修改 dashboard、不修改 schema、不接交易执行链路。

---

## 2. 本地验证命令

用户在本地执行：

```bash
cd /Users/xuchao/Documents/trade-model-v1

./mvnw -Dtest=DefaultPlanBoundarySourceTraceAdapterTest test
```

执行时间：2026-05-14 22:33:02 +08:00。

---

## 3. 用户提供的终端结果

```text
Running org.example.trademodel.service.dashboard.DefaultPlanBoundarySourceTraceAdapterTest
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

结论：

```text
DefaultPlanBoundarySourceTraceAdapterTest：PASS
```

---

## 4. 验证覆盖范围

该测试覆盖了 `DefaultPlanBoundarySourceTraceAdapter` 的第一阶段 fail-closed readiness 行为，包括：

- 输入全为空时返回 `BACKEND_PENDING`。
- fallback 对象被保留。
- safety flags 被强制保持：
  - `manualReviewRequired = true`
  - `notTradeInstruction = true`
- decision 缺失时写入 `DECISION_MISSING`。
- decision 存在但 source trace 输入不可用时返回 `INCOMPLETE`。
- source trace 缺失时写入：
  - `SOURCE_TRACE_INPUT_NOT_AVAILABLE`
  - `BOUNDARY_CANDIDATE_DTO_MISSING`
  - `RUNTIME_KLINE_CONTEXT_DTO_MISSING`
- `readModelTruthStatus = PARTIAL` 时保留 `READ_MODEL_PARTIAL` 与 fallback reason。
- 文本 `entryZone / stopLoss / takeProfitRules / executionPlanSummary` 不会触发 VALID。
- reason lists 为空时会被初始化。

---

## 5. 安全边界复核

本阶段验证未发现以下越界：

- 未接 Controller。
- 未改 `DefaultPlanBoundaryDisplayAdapter`。
- 未改 dashboard。
- 未改 schema。
- 未新增 mapper。
- 未接 RuleEngine。
- 未接 source assembler。
- 未引用不存在的 `BoundaryCandidateDTO / RuntimeKlineContextDTO`。
- 未生成真实 entry / stop / take profit。
- 未返回 VALID。
- 未接 order API。
- 未自动交易。

当前 `PlanBoundarySourceTraceAdapter` 仍是 source trace readiness / fail-closed 层。

---

## 6. 当前阶段结论

截至本 PR：

```text
PlanBoundarySourceTraceAdapter Java 最小实现 + 本地单测验证 已完成闭环。
```

当前链路状态：

```text
PlanBoundarySourceTraceAdapter
↓
DefaultPlanBoundarySourceTraceAdapter
↓
DefaultPlanBoundarySourceTraceAdapterTest
↓
本地单测 PASS
```

但尚未接入：

```text
DefaultPlanBoundaryDisplayAdapter
DashboardController
/api/dashboard/detail
```

---

## 7. 后续建议

下一步建议进入：

```text
PR #62：PlanBoundaryDisplayAdapter 调用 source trace adapter 的方案
```

该 PR 应继续 docs-only，明确如何把 `DefaultPlanBoundaryDisplayAdapter` 与 `PlanBoundarySourceTraceAdapter` 组合起来，并保持：

```text
不返回 VALID
不生成 entry / stop / TP
不接 order API
```

---

## 8. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_PLAN_BOUNDARY_SOURCE_TRACE_ADAPTER_TEST_RESULT.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无交易执行相关改动。
- 如实记录 `DefaultPlanBoundarySourceTraceAdapterTest` 本地 PASS。
