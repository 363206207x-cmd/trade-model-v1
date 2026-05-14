# PHASE: DefaultPlanBoundaryDisplayAdapterTest 本地单测结果补充记录（PR35）

## 1. 阶段目标

本阶段补充记录 `DefaultPlanBoundaryDisplayAdapterTest` 的本地单测结果，补齐 PR #34 中标记为待补充的 adapter 单测验证项。

本 PR 只新增验证记录文档，不修改 Java、不修改 dashboard、不修改 schema、不接交易执行链路。

---

## 2. 本地验证命令

用户在本地执行：

```bash
cd /Users/xuchao/Documents/trade-model-v1

./mvnw -Dtest=DefaultPlanBoundaryDisplayAdapterTest test
```

执行时间：2026-05-14 16:36:24 +08:00。

---

## 3. 用户提供的终端结果

```text
Running org.example.trademodel.service.dashboard.DefaultPlanBoundaryDisplayAdapterTest
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

结论：

```text
DefaultPlanBoundaryDisplayAdapterTest：PASS
```

---

## 4. 验证覆盖范围

该测试覆盖了 `DefaultPlanBoundaryDisplayAdapter` 的第一阶段 fail-closed 行为，包括：

- 输入缺失时返回 `BACKEND_PENDING`。
- fallback 对象被保留。
- safety flags 被强制保持：
  - `manualReviewRequired = true`
  - `notTradeInstruction = true`
- analysisId 缺失时返回 `INCOMPLETE`。
- `readModelTruthStatus = PARTIAL` 时返回 `INCOMPLETE`。
- 文本执行计划字段不会被映射为 `VALID`。
- 没有生成真实 entry / stop / take profit 数值。

---

## 5. 与 PR #34 的关系

PR #34 已记录：

- `DashboardControllerTest` PASS。
- `/api/dashboard/detail` curl smoke PASS。
- `planBoundaryDisplay` 在 decision 缺失时返回：
  - `BACKEND_PENDING`
  - `blockingReasons = [DECISION_MISSING]`
  - `manualReviewRequired = true`
  - `notTradeInstruction = true`

但 PR #34 如实标记：

```text
DefaultPlanBoundaryDisplayAdapterTest：未提供单独输出 / 待补充
```

本 PR 正式补齐该项。

---

## 6. 当前完整验证结论

| 验收项 | 结果 |
|---|---|
| DashboardControllerTest | PASS |
| DefaultPlanBoundaryDisplayAdapterTest | PASS |
| `/api/dashboard/detail` 返回 | PASS |
| `planBoundaryDisplay` 可见 | PASS |
| `planBoundaryStatus = BACKEND_PENDING` | PASS |
| `blockingReasons` 包含 `DECISION_MISSING` | PASS |
| `manualReviewRequired = true` | PASS |
| `notTradeInstruction = true` | PASS |
| 未生成真实 entry / stop / take profit | PASS |

---

## 7. 安全边界复核

本阶段验证未发现以下越界：

- 未改 schema。
- 未新增 mapper。
- 未接 RuleEngine。
- 未接 source assembler。
- 未接 BoundaryCandidate 生产链路。
- 未生成真实 entry / stop / take profit。
- 未生成真实 ExecutionPlan。
- 未接任何交易执行链路。

当前 adapter 仍是 dashboard read-model fail-closed 拼装层。

---

## 8. 后续建议

当前 PlanBoundary display adapter 阶段的本地验证已补齐。

下一步建议进入：

```text
PR #36：ExecutionPlanDisplay 与 PlanBoundaryDisplay 状态映射方案
```

该 PR 建议先做 docs-only 方案，不直接改 Java。

---

## 9. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_PLAN_BOUNDARY_ADAPTER_TEST_RESULT.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无交易执行相关改动。
- 如实记录 `DefaultPlanBoundaryDisplayAdapterTest` 本地 PASS。
