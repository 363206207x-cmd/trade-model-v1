# PHASE: RiskActionGuardDisplayAdapter API Smoke Result（PR47）

## 1. 阶段目标

本阶段记录 `RiskActionGuardDisplayAdapter` 接入 `DashboardController` 后的本地 API smoke 验证结果。

本 PR 只新增验证记录文档，不修改 Java、不修改 dashboard、不修改 schema、不接交易执行链路。

---

## 2. 本地同步状态

用户本地已确认 `main` 同步到最新远端：

```text
5a45734 docs(risk): record RiskActionGuard display adapter test result
74a733e fix(risk): treat backend pending liquidity as missing context
9047021 feat(dashboard): wire RiskActionGuard display adapter
c0e3c64 feat(risk): add default RiskActionGuard display adapter
dd5c019 docs(risk): define RiskActionGuard display adapter plan
```

结论：

```text
本地 main 已包含 PR #42 / PR #43 / PR #44 / PR #45 / PR #46。
```

---

## 3. API curl 验证命令

用户本地执行：

```bash
curl -s "http://localhost:8081/api/dashboard/detail?symbol=BTCUSDT" | grep -E "riskActionGuardDisplay|riskActionGuardStatus|riskActionGuardStatusLabel|riskActionBlockingReason|liquidityState|opportunityPushAllowed|reverseTradeAllowed|newPositionAllowed|marketOrderExitAllowed|manualRiskReviewRequired|notTradeInstruction|BACKEND_PENDING|DECISION_MISSING|PLAN_BOUNDARY_NOT_VALID|EXECUTION_PLAN_NOT_READY|LIQUIDITY_CONTEXT_MISSING"
```

说明：

- 端口 8081 已存在运行中的 Java 服务。
- 本 PR 只摘录 display smoke 相关字段，不记录完整交易明细 JSON。

---

## 4. 用户提供的 API 关键输出

本次 response 中 `riskActionGuardDisplay` 为：

```json
"riskActionGuardDisplay": {
  "riskActionGuardStatus": "BACKEND_PENDING",
  "riskActionGuardStatusLabel": "后端未接入",
  "riskActionAdvice": null,
  "riskActionBlockingReason": null,
  "liquidityState": "BACKEND_PENDING",
  "stampedeDetected": false,
  "wickOnlyRisk": false,
  "opportunityPushAllowed": false,
  "reverseTradeAllowed": false,
  "newPositionAllowed": false,
  "marketOrderExitAllowed": false,
  "manualRiskReviewRequired": true,
  "notTradeInstruction": true,
  "updatedAt": null
}
```

结论：

```text
RiskActionGuardDisplay API smoke：PASS
```

---

## 5. 关联 display 状态

本次 response 同时显示：

### 5.1 PlanBoundaryDisplay

```text
planBoundaryStatus = INCOMPLETE
sourceTraceStatus = MISSING
backendConnectionStatus = PARTIAL
incompleteReasons = [READ_MODEL_PARTIAL]
blockingReasons = [LEGACY_MISSING:invalid_condition]
manualReviewRequired = true
notTradeInstruction = true
```

### 5.2 ExecutionPlanDisplay

```text
executionPlanStatus = INCOMPLETE
executionPlanBoundaryAligned = false
planBoundaryStatus = INCOMPLETE
notExecutableReason = PLAN_BOUNDARY_INCOMPLETE
incompleteReasons = [PLAN_BOUNDARY_INCOMPLETE, READ_MODEL_PARTIAL]
manualReviewRequired = true
notTradeInstruction = true
```

说明：

- PlanBoundary 未 VALID。
- ExecutionPlan 未 READY_REVIEW_ONLY。
- RiskActionGuardDisplay 保持 fail-closed 展示状态。

---

## 6. 风险动作安全边界验证

本次 API 输出确认以下 action flags 全部为 false：

```text
opportunityPushAllowed = false
reverseTradeAllowed = false
newPositionAllowed = false
marketOrderExitAllowed = false
```

并确认：

```text
manualRiskReviewRequired = true
notTradeInstruction = true
liquidityState = BACKEND_PENDING
```

结论：

```text
风险动作安全边界：PASS
```

---

## 7. 未发现越界

本次 API smoke 未发现：

- 自动开仓语义。
- 自动平仓语义。
- 自动反手语义。
- 自动机会推送放行。
- 市价一次性退出建议。
- 真实 entry / stop / take profit 数值生成。
- 交易执行链路调用。

---

## 8. 当前完整阶段结论

截至本 PR，RiskActionGuardDisplayAdapter 阶段已完成：

```text
PR #42：RiskActionGuardDisplayAdapter 方案
PR #43：RiskActionGuardDisplayAdapter Java 最小实现
PR #44：DashboardController 接入 RiskActionGuardDisplayAdapter
PR #45：修复 BACKEND_PENDING 流动性上下文判断
PR #46：本地测试结果记录
PR #47：API smoke 验证记录
```

当前链路：

```text
withSafeDefaultDisplays()
↓
PlanBoundaryDisplayAdapter
↓
ExecutionPlanDisplayAdapter
↓
RiskActionGuardDisplayAdapter
↓
/api/dashboard/detail
↓
dashboard display status
```

当前仍然是：

```text
dashboard read-model fail-closed 展示层
```

不是交易执行链路。

---

## 9. 后续建议

下一阶段建议进入：

```text
PaperObservationDisplayAdapter 方案
```

仍建议按当前节奏推进：

1. docs-only 方案。
2. Java 最小实现。
3. DashboardController 接入。
4. 本地 API smoke 验证。

---

## 10. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_RISK_ACTION_GUARD_DISPLAY_API_SMOKE_RESULT.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无交易执行相关改动。
- 如实记录 API curl 中的 `riskActionGuardDisplay` fail-closed 输出。
- 不记录完整交易明细 JSON。
