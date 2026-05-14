# PHASE: DashboardControllerTest 本地结果补充记录（PR41）

## 1. 阶段目标

本阶段补充记录 `DashboardControllerTest` 的本地测试结果，补齐 PR #39 中标记为待补充的 controller 测试验证项。

本 PR 只新增验证记录文档，不修改 Java、不修改 dashboard、不修改 schema、不接交易执行链路。

---

## 2. 本地验证命令

用户在本地执行：

```bash
cd /Users/xuchao/Documents/trade-model-v1

./mvnw -Dtest=DashboardControllerTest test
```

执行时间：2026-05-14 17:13:13 +08:00。

---

## 3. 用户提供的终端结果

```text
Running org.example.trademodel.controller.DashboardControllerTest
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

结论：

```text
DashboardControllerTest：PASS
```

---

## 4. 验证覆盖范围

该测试覆盖了 dashboard controller 的关键接口行为，包括：

- `/api/dashboard/summary` 基础字段输出。
- `/api/dashboard/detail` 决策核心字段输出。
- `/api/dashboard/detail` marketEnvironmentMini fallback 输出。
- `/api/dashboard/detail` evidenceTopItems / scoreTopItems 输出。
- blank symbol / missing symbol 的 bad request 行为。
- `/api/dashboard/detail` 不输出 Deprecation header。
- `/api/dashboard/refresh` legacy contract 与 Deprecation / Link header。
- Controller 构造函数已正确接收：
  - `PlanBoundaryDisplayAdapter`
  - `ExecutionPlanDisplayAdapter`

---

## 5. 与 PR #39 / PR #40 的关系

PR #39 已记录：

- `/api/dashboard/detail` curl smoke PASS。
- `executionPlanDisplay` 返回：
  - `executionPlanStatus = BOUNDARY_PENDING`
  - `executionPlanBoundaryAligned = false`
  - `planBoundaryStatus = BACKEND_PENDING`
  - `notExecutableReason = PLAN_BOUNDARY_BACKEND_PENDING`
  - `manualReviewRequired = true`
  - `notTradeInstruction = true`

PR #40 已记录：

- `DefaultExecutionPlanDisplayAdapterTest` PASS。
- 9 tests passed。

PR #39 如实标记：

```text
DashboardControllerTest：本轮未提供单独输出 / 待补充
```

本 PR 正式补齐该项。

---

## 6. 当前完整验证结论

| 验收项 | 结果 |
|---|---|
| DefaultExecutionPlanDisplayAdapterTest | PASS |
| DashboardControllerTest | PASS |
| `/api/dashboard/detail` 返回 | PASS |
| `planBoundaryDisplay` 可见 | PASS |
| `executionPlanDisplay` 可见 | PASS |
| `executionPlanStatus = BOUNDARY_PENDING` | PASS |
| `executionPlanBoundaryAligned = false` | PASS |
| `notExecutableReason = PLAN_BOUNDARY_BACKEND_PENDING` | PASS |
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
- 未生成真实 entry / stop / take profit。
- 未生成真实交易执行计划。
- 未接任何交易执行链路。

当前 PlanBoundary / ExecutionPlan display adapter 链路仍是 dashboard read-model fail-closed 拼装层。

---

## 8. 当前阶段结论

截至本 PR：

```text
PlanBoundaryDisplayAdapter + ExecutionPlanDisplayAdapter + DashboardController 接入 + API smoke + 单测验证 已完成闭环。
```

当前完成链路：

```text
DashboardDetailResponseVO display models
↓
PlanBoundaryDisplayAdapter fail-closed
↓
ExecutionPlanDisplayAdapter gated by PlanBoundary
↓
DashboardController detail 接入
↓
API curl smoke PASS
↓
Adapter 单测 PASS
↓
Controller 单测 PASS
```

---

## 9. 后续建议

下一阶段建议进入：

```text
RiskActionGuardDisplayAdapter 方案 / Java 最小实现
```

建议仍按当前节奏推进：

1. 先做 docs-only 方案。
2. 再做 Java 最小实现。
3. 再接 DashboardController。
4. 最后本地 smoke 验证。

---

## 10. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_DASHBOARD_CONTROLLER_TEST_RESULT.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无交易执行相关改动。
- 如实记录 `DashboardControllerTest` 本地 PASS。
