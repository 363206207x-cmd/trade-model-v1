# PHASE: PlanBoundary Adapter API Smoke Result（PR34）

## 1. 阶段目标

本阶段记录 PlanBoundaryDisplayAdapter 接入 DashboardController 后的本地 API smoke 验证结果。

本 PR 只新增验证记录文档，不修改 Java、不修改 dashboard、不修改 schema、不接交易执行链路。

---

## 2. 背景

已完成：

- PR #31：新增 fail-closed `PlanBoundaryDisplayAdapter` 与默认实现。
- PR #32：`DashboardController` 接入 adapter。
- PR #33：修复 `DashboardControllerTest` 构造函数测试参数。

本阶段验证重点：

- `/api/dashboard/detail` 是否仍能正常返回。
- `planBoundaryDisplay` 是否由 adapter 维持 fail-closed 输出。
- decision 缺失时是否返回 `DECISION_MISSING`。
- 是否仍保持 `manualReviewRequired=true` 与 `notTradeInstruction=true`。

---

## 3. 已验证结果

### 3.1 DashboardControllerTest

用户提供本地终端结果：

```text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

结论：

```text
DashboardControllerTest：PASS
```

说明：

- PR #33 修复有效。
- Controller 构造函数参数已与测试对齐。

---

### 3.2 API curl 验证

用户本地执行：

```bash
curl -s "http://localhost:8081/api/dashboard/detail?symbol=BTCUSDT" | grep -E "planBoundaryDisplay|planBoundaryStatus|sourceTraceStatus|backendConnectionStatus|incompleteReasons|blockingReasons|BACKEND_PENDING|INCOMPLETE|manualReviewRequired|notTradeInstruction|SOURCE_TRACE_PENDING|READ_MODEL_PARTIAL|LEGACY_MISSING"
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

结论：

```text
PlanBoundaryDisplay API smoke：PASS
```

---

## 4. Adapter 行为验证

本次 curl 输出说明：

- 当前 `decision = null`。
- adapter 没有抛异常。
- adapter 返回 `BACKEND_PENDING`。
- adapter 写入 `blockingReasons = [DECISION_MISSING]`。
- adapter 保持 `manualReviewRequired = true`。
- adapter 保持 `notTradeInstruction = true`。

该行为符合 PR #31 / PR #32 的 fail-closed 设计。

---

## 5. 安全边界验证

本次输出未发现：

- 交易执行字段。
- 自动开仓语义。
- 自动平仓语义。
- 自动反手语义。
- 真实 entry / stop / take profit 数值。
- VALID 状态。

当前仍保持：

```text
planBoundaryStatus = BACKEND_PENDING
sourceTraceStatus = BACKEND_PENDING
backendConnectionStatus = BACKEND_PENDING
manualReviewRequired = true
notTradeInstruction = true
```

结论：

```text
安全边界：PASS
```

---

## 6. 未完成项

用户尚未提供以下单独测试命令输出：

```bash
./mvnw -Dtest=DefaultPlanBoundaryDisplayAdapterTest test
```

因此本 PR 不声称该单测已被用户本地单独执行通过。

记录状态：

```text
DefaultPlanBoundaryDisplayAdapterTest：未提供单独输出 / 待补充
```

---

## 7. 当前 smoke 总结

| 验收项 | 结果 |
|---|---|
| DashboardControllerTest | PASS |
| `/api/dashboard/detail` 返回 | PASS |
| `planBoundaryDisplay` 可见 | PASS |
| `planBoundaryStatus = BACKEND_PENDING` | PASS |
| `blockingReasons` 包含 `DECISION_MISSING` | PASS |
| `manualReviewRequired = true` | PASS |
| `notTradeInstruction = true` | PASS |
| 未生成真实 entry / stop / take profit | PASS |
| DefaultPlanBoundaryDisplayAdapterTest 单独输出 | PENDING |

---

## 8. 后续建议

下一步建议：

```text
PR #35：DefaultPlanBoundaryDisplayAdapterTest 本地单测结果补充记录
```

或者，如果用户先补充该单测结果，也可以直接进入：

```text
PR #36：ExecutionPlanDisplay 与 PlanBoundaryDisplay 状态映射方案
```

---

## 9. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_PLAN_BOUNDARY_ADAPTER_API_SMOKE_RESULT.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无交易执行相关改动。
- 如实记录 `DashboardControllerTest` PASS。
- 如实记录 API curl 中的 `planBoundaryDisplay` fail-closed 输出。
- 如实记录 adapter 单测单独输出尚未提供。
