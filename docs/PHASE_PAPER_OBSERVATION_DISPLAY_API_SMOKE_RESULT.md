# PHASE: PaperObservationDisplayAdapter API Smoke Result（PR51）

## 1. 阶段目标

本阶段记录 `PaperObservationDisplayAdapter` 接入 `DashboardController` 后的本地 API smoke 验证结果。

本 PR 只新增验证记录文档，不修改 Java、不修改 dashboard、不修改 schema、不接交易执行链路。

---

## 2. 背景

已完成：

- PR #48：PaperObservationDisplayAdapter 方案。
- PR #49：PaperObservationDisplayAdapter Java 最小实现。
- PR #50：DashboardController 接入 PaperObservationDisplayAdapter。

本阶段验证重点：

- `/api/dashboard/detail` 是否仍能正常返回。
- `paperObservationDisplay` 是否已经出现在 API response 中。
- `paperObservationDisplay` 是否保持 fail-closed。
- 纸面观察是否仍明确不是实盘持仓、不是交易指令。

---

## 3. API curl 验证命令

用户本地执行：

```bash
curl -s "http://localhost:8081/api/dashboard/detail?symbol=BTCUSDT" | grep -E "paperObservationDisplay|paperObservationStatus|paperObservationStatusLabel|paperObservationAvailable|manualReviewEntryAvailable|linkedPaperObservationCount|linkedReviewCount|missedOpportunityFlag|reviewSummary|notRealPosition|notTradeInstruction|manualReviewRequired|backendConnectionStatus|BACKEND_PENDING|DECISION_MISSING|PLAN_BOUNDARY_NOT_VALID|EXECUTION_PLAN_NOT_READY|RISK_ACTION_GUARD_BLOCKED|AVAILABLE_REVIEW_ONLY"
```

说明：

- 端口 8081 已存在运行中的 Java 服务。
- 本 PR 只摘录 display smoke 相关字段，不记录完整交易明细 JSON。

---

## 4. 用户提供的 API 关键输出

本次 response 中 `paperObservationDisplay` 为：

```json
"paperObservationDisplay": {
  "paperObservationStatus": "BACKEND_PENDING",
  "paperObservationStatusLabel": "后端未接入",
  "paperObservationAvailable": false,
  "manualReviewEntryAvailable": false,
  "linkedPaperObservationCount": 0,
  "linkedReviewCount": 0,
  "missedOpportunityFlag": false,
  "reviewSummary": "DECISION_MISSING",
  "notRealPosition": true,
  "notTradeInstruction": true,
  "manualReviewRequired": true,
  "backendConnectionStatus": "BACKEND_PENDING",
  "updatedAt": null
}
```

结论：

```text
PaperObservationDisplay API smoke：PASS
```

---

## 5. 关联 display 状态

本次 response 同时显示：

### 5.1 PlanBoundaryDisplay

```text
planBoundaryStatus = BACKEND_PENDING
blockingReasons = [DECISION_MISSING]
manualReviewRequired = true
notTradeInstruction = true
```

### 5.2 ExecutionPlanDisplay

```text
executionPlanStatus = BOUNDARY_PENDING
executionPlanBoundaryAligned = false
notExecutableReason = PLAN_BOUNDARY_BACKEND_PENDING
manualReviewRequired = true
notTradeInstruction = true
```

### 5.3 RiskActionGuardDisplay

```text
riskActionGuardStatus = BACKEND_PENDING
riskActionBlockingReason = DECISION_MISSING
opportunityPushAllowed = false
reverseTradeAllowed = false
newPositionAllowed = false
marketOrderExitAllowed = false
manualRiskReviewRequired = true
notTradeInstruction = true
```

说明：

- decision 当前为 null。
- 上游 display 均保持 fail-closed。
- PaperObservationDisplay 根据缺失上下文保持 `BACKEND_PENDING`，并记录 `DECISION_MISSING`。

---

## 6. 纸面观察安全边界验证

本次 API 输出确认：

```text
paperObservationAvailable = false
manualReviewEntryAvailable = false
notRealPosition = true
notTradeInstruction = true
manualReviewRequired = true
backendConnectionStatus = BACKEND_PENDING
```

结论：

```text
纸面观察安全边界：PASS
```

---

## 7. 未发现越界

本次 API smoke 未发现：

- 纸面观察被包装为实盘持仓。
- 纸面观察被包装为交易指令。
- 纸面观察入口被自动放开。
- 真实持仓创建语义。
- 真实交易执行语义。
- 真实 entry / stop / take profit 数值生成。

---

## 8. 当前完整阶段结论

截至本 PR，PaperObservationDisplayAdapter 阶段已完成：

```text
PR #48：PaperObservationDisplayAdapter 方案
PR #49：PaperObservationDisplayAdapter Java 最小实现
PR #50：DashboardController 接入 PaperObservationDisplayAdapter
PR #51：API smoke 验证记录
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
PaperObservationDisplayAdapter
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

## 9. 未完成项

用户本轮未提供以下单独测试命令完整输出：

```bash
./mvnw -Dtest=DefaultPaperObservationDisplayAdapterTest test
./mvnw -Dtest=DashboardControllerTest test
```

因此本 PR 不声称上述单测在本轮已单独执行通过。

---

## 10. 后续建议

下一步建议：

```text
PR #52：DefaultPaperObservationDisplayAdapterTest 本地单测结果补充记录
```

用户补充单测结果后，可再补：

```text
PR #53：DashboardControllerTest 本地结果补充记录
```

---

## 11. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_PAPER_OBSERVATION_DISPLAY_API_SMOKE_RESULT.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无交易执行相关改动。
- 如实记录 API curl 中的 `paperObservationDisplay` fail-closed 输出。
- 不记录完整交易明细 JSON。
