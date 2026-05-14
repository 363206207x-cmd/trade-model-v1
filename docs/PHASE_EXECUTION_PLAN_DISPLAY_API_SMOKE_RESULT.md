# PHASE: ExecutionPlan Display API Smoke Result（PR39）

## 1. 阶段目标

本阶段记录 `ExecutionPlanDisplayAdapter` 接入 `DashboardController` 后的本地 API smoke 验证结果。

本 PR 只新增验证记录文档，不修改 Java、不修改 dashboard、不修改 schema、不接交易执行链路。

---

## 2. 背景

已完成：

- PR #36：ExecutionPlanDisplay 与 PlanBoundaryDisplay 状态映射方案。
- PR #37：新增 fail-closed `ExecutionPlanDisplayAdapter` 与默认实现。
- PR #38：`DashboardController` 接入 `ExecutionPlanDisplayAdapter`。

本阶段验证重点：

- `/api/dashboard/detail` 是否仍能正常返回。
- `executionPlanDisplay` 是否已经出现在 API response 中。
- `executionPlanDisplay` 是否受 `planBoundaryDisplay` 约束。
- PlanBoundary 为 BACKEND_PENDING 时，ExecutionPlan 是否保持 BOUNDARY_PENDING。
- 是否仍保持 `manualReviewRequired=true` 与 `notTradeInstruction=true`。

---

## 3. 本地同步状态

用户本地已确认 `main` 同步到最新远端：

```text
2ac1260 (HEAD -> main, origin/main, origin/HEAD) feat(dashboard): wire ExecutionPlan display adapter
5d29120 feat(plan): add default ExecutionPlan display adapter
dcbb05b docs(plan): define ExecutionPlan display mapping plan
```

结论：

```text
本地 main 已包含 PR #37 / PR #38。
```

---

## 4. API curl 验证

用户本地执行：

```bash
curl -s "http://localhost:8081/api/dashboard/detail?symbol=BTCUSDT" | grep -E "planBoundaryDisplay|executionPlanDisplay|planBoundaryStatus|executionPlanStatus|executionPlanBoundaryAligned|notExecutableReason|BACKEND_PENDING|BOUNDARY_PENDING|INCOMPLETE|DECISION_MISSING|PLAN_BOUNDARY_BACKEND_PENDING|manualReviewRequired|notTradeInstruction"
```

用户提供的输出确认 response 中包含：

```json
"planBoundaryDisplay": {
  "planBoundaryStatus": "BACKEND_PENDING",
  "planBoundaryStatusLabel": "后端未接入",
  "sourceTraceStatus": "BACKEND_PENDING",
  "backendConnectionStatus": "BACKEND_PENDING",
  "incompleteReasons": [],
  "blockingReasons": ["DECISION_MISSING"],
  "manualReviewRequired": true,
  "notTradeInstruction": true,
  "updatedAt": null
}
```

同时确认 response 中包含：

```json
"executionPlanDisplay": {
  "executionPlanStatus": "BOUNDARY_PENDING",
  "executionPlanStatusLabel": "等待边界接入",
  "executionPlanBoundaryAligned": false,
  "planBoundaryStatus": "BACKEND_PENDING",
  "executionPlanSummary": null,
  "notExecutableReason": "PLAN_BOUNDARY_BACKEND_PENDING",
  "incompleteReasons": ["PLAN_BOUNDARY_BACKEND_PENDING"],
  "manualReviewRequired": true,
  "notTradeInstruction": true,
  "updatedAt": null
}
```

结论：

```text
ExecutionPlanDisplay API smoke：PASS
```

---

## 5. 映射行为验证

本次 curl 输出说明：

- 当前 `decision = null`。
- PlanBoundaryDisplay adapter 返回：
  - `planBoundaryStatus = BACKEND_PENDING`
  - `blockingReasons = [DECISION_MISSING]`
- ExecutionPlanDisplay adapter 读取 PlanBoundary 状态后返回：
  - `executionPlanStatus = BOUNDARY_PENDING`
  - `executionPlanBoundaryAligned = false`
  - `planBoundaryStatus = BACKEND_PENDING`
  - `notExecutableReason = PLAN_BOUNDARY_BACKEND_PENDING`
  - `incompleteReasons = [PLAN_BOUNDARY_BACKEND_PENDING]`

该行为符合 PR #36 / PR #37 / PR #38 的映射设计：

```text
PlanBoundary BACKEND_PENDING -> ExecutionPlan BOUNDARY_PENDING
```

---

## 6. 安全边界验证

本次输出未发现：

- 交易执行字段。
- 自动开仓语义。
- 自动平仓语义。
- 自动反手语义。
- 真实 entry / stop / take profit 数值。
- ExecutionPlan 被标记为 READY_REVIEW_ONLY。
- ExecutionPlan 被标记为可自动执行。

当前仍保持：

```text
executionPlanStatus = BOUNDARY_PENDING
executionPlanBoundaryAligned = false
manualReviewRequired = true
notTradeInstruction = true
```

结论：

```text
安全边界：PASS
```

---

## 7. 未完成项

用户尚未提供以下单独测试命令输出：

```bash
./mvnw -Dtest=DefaultExecutionPlanDisplayAdapterTest test
./mvnw -Dtest=DashboardControllerTest test
```

因此本 PR 不声称上述单测已被用户本地单独执行通过。

记录状态：

```text
DefaultExecutionPlanDisplayAdapterTest：未提供单独输出 / 待补充
DashboardControllerTest：本轮未提供单独输出 / 待补充
```

---

## 8. 当前 smoke 总结

| 验收项 | 结果 |
|---|---|
| 本地 main 同步到 PR #38 | PASS |
| `/api/dashboard/detail` 返回 | PASS |
| `planBoundaryDisplay` 可见 | PASS |
| `executionPlanDisplay` 可见 | PASS |
| `planBoundaryStatus = BACKEND_PENDING` | PASS |
| `executionPlanStatus = BOUNDARY_PENDING` | PASS |
| `executionPlanBoundaryAligned = false` | PASS |
| `notExecutableReason = PLAN_BOUNDARY_BACKEND_PENDING` | PASS |
| `manualReviewRequired = true` | PASS |
| `notTradeInstruction = true` | PASS |
| 未生成真实 entry / stop / take profit | PASS |
| DefaultExecutionPlanDisplayAdapterTest 单独输出 | PENDING |
| DashboardControllerTest 单独输出 | PENDING |

---

## 9. 后续建议

下一步建议：

```text
PR #40：DefaultExecutionPlanDisplayAdapterTest 本地单测结果补充记录
```

用户补充单测结果后，再进入：

```text
PR #41：DashboardControllerTest 本地结果补充记录
```

或在同一个 PR 中记录两项测试结果。

---

## 10. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_EXECUTION_PLAN_DISPLAY_API_SMOKE_RESULT.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无交易执行相关改动。
- 如实记录 API curl 中的 `executionPlanDisplay` fail-closed 输出。
- 如实记录单测输出尚未提供。
