# PHASE: Dashboard Display Status API Curl Result（PR27）

## 1. 阶段目标

本阶段记录 `/api/dashboard/detail` 的本地 API curl 验证结果，补齐 PR #26 中尚未提供的 curl 原始输出验证项。

本 PR 只新增验证记录文档，不修改 Java、不修改 dashboard、不修改 schema、不接 order API、不自动交易。

---

## 2. 本地验证命令

用户在本地执行：

```bash
cd /Users/xuchao/Documents/trade-model-v1

curl -s "http://localhost:8081/api/dashboard/detail?symbol=BTCUSDT" | grep -E "planBoundaryDisplay|executionPlanDisplay|riskActionGuardDisplay|paperObservationDisplay|BACKEND_PENDING|BOUNDARY_PENDING|notTradeInstruction|manualReviewRequired"
```

执行时间：2026-05-14 下午。

---

## 3. 验证结果摘要

用户提供的 curl 输出确认 response JSON 中包含以下四个 display 对象：

- `planBoundaryDisplay`
- `executionPlanDisplay`
- `riskActionGuardDisplay`
- `paperObservationDisplay`

并确认包含以下安全默认状态：

- `BACKEND_PENDING`
- `BOUNDARY_PENDING`
- `manualReviewRequired: true`
- `notTradeInstruction: true`

---

## 4. Display 对象字段验证

### 4.1 PlanBoundaryDisplay

curl 输出确认：

```json
"planBoundaryDisplay": {
  "planBoundaryStatus": "BACKEND_PENDING",
  "planBoundaryStatusLabel": "后端未接入",
  "sourceTraceStatus": "BACKEND_PENDING",
  "backendConnectionStatus": "BACKEND_PENDING",
  "manualReviewRequired": true,
  "notTradeInstruction": true
}
```

结论：

```text
PlanBoundaryDisplay：PASS
```

---

### 4.2 ExecutionPlanDisplay

curl 输出确认：

```json
"executionPlanDisplay": {
  "executionPlanStatus": "BOUNDARY_PENDING",
  "executionPlanStatusLabel": "等待边界接入",
  "executionPlanBoundaryAligned": false,
  "planBoundaryStatus": "BACKEND_PENDING",
  "notExecutableReason": "PLAN_BOUNDARY_BACKEND_PENDING",
  "manualReviewRequired": true,
  "notTradeInstruction": true
}
```

结论：

```text
ExecutionPlanDisplay：PASS
```

---

### 4.3 RiskActionGuardDisplay

curl 输出确认：

```json
"riskActionGuardDisplay": {
  "riskActionGuardStatus": "BACKEND_PENDING",
  "riskActionGuardStatusLabel": "后端未接入",
  "liquidityState": "BACKEND_PENDING",
  "opportunityPushAllowed": false,
  "reverseTradeAllowed": false,
  "newPositionAllowed": false,
  "marketOrderExitAllowed": false,
  "manualRiskReviewRequired": true,
  "notTradeInstruction": true
}
```

结论：

```text
RiskActionGuardDisplay：PASS
```

---

### 4.4 PaperObservationDisplay

curl 输出确认：

```json
"paperObservationDisplay": {
  "paperObservationStatus": "BACKEND_PENDING",
  "paperObservationStatusLabel": "后端未接入",
  "paperObservationAvailable": false,
  "manualReviewEntryAvailable": false,
  "linkedPaperObservationCount": 0,
  "linkedReviewCount": 0,
  "missedOpportunityFlag": false,
  "notRealPosition": true,
  "notTradeInstruction": true,
  "manualReviewRequired": true,
  "backendConnectionStatus": "BACKEND_PENDING"
}
```

结论：

```text
PaperObservationDisplay：PASS
```

---

## 5. 安全边界验证

本次 curl 输出未发现以下字段或语义：

- `orderId`
- `orderRequest`
- `autoExecute`
- `autoTrade`
- 自动下单字段
- 自动开仓字段
- 自动平仓字段
- 自动反手字段

本次 API 输出仍保持：

- `notTradeInstruction = true`
- `manualReviewRequired = true`
- `planBoundaryStatus = BACKEND_PENDING`
- `executionPlanStatus = BOUNDARY_PENDING`
- `riskActionGuardStatus = BACKEND_PENDING`
- `paperObservationStatus = BACKEND_PENDING`

结论：

```text
安全边界：PASS
```

---

## 6. 当前完整 smoke 结论

结合 PR #26 的单测与页面截图结果，以及本 PR 的 curl 输出结果：

| 验收项 | 结果 |
|---|---|
| DashboardDetailResponseVOTest | PASS |
| `/dashboard` 页面打开 | PASS |
| 页面显示 BACKEND_PENDING / INCOMPLETE | PASS |
| `/api/dashboard/detail` 返回四个 display 对象 | PASS |
| API 返回 BACKEND_PENDING / BOUNDARY_PENDING | PASS |
| API 返回 manualReviewRequired / notTradeInstruction | PASS |
| 未发现 order / autoTrade / autoExecute 字段 | PASS |

---

## 7. 后续建议

当前 dashboard display status 链路已完成 smoke 闭环。

下一步建议进入：

```text
PR #28：真实 PlanBoundary adapter 接入前方案
```

该 PR 应先做方案文档，不直接改 service / mapper / schema / RuleEngine。

---

## 8. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_DASHBOARD_DISPLAY_STATUS_API_CURL_RESULT.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无 order API / 自动交易相关改动。
- 如实记录 curl 输出中的 display status 字段。
- 不记录完整交易明细 JSON，只记录 display 验收相关字段。
