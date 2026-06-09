# Minimal Review-Only DecisionResult Runtime Wiring Implementation

# 1. Executive Summary

本包实现了最小 DecisionResult review-only dashboard/API status。

实现内容：

- 新增只读 endpoint: `GET /api/dashboard/decision-result-status?symbol=BTCUSDT`
- 新增 dashboard 最小 panel: `decisionResultStatusPanel`
- 扩展 `DashboardControllerTest`，覆盖 ready、missing fail-closed、read-model partial、source trace partial、ai_role_results partial、禁止字段不暴露、dashboard copy/DOM
- 更新 source-of-truth docs

本包复用 existing `tm_decision_result` / `DecisionResultMapper` / `DecisionService` / `DecisionResultVO` / `DashboardController` / dashboard assets。没有新增 DTO / Validator / Assembler / Orchestrator，没有修改 schema / config / pom。

Endpoint 只读，只读取 existing DecisionResult read model，不触发新决策，不生成 Candidate，不生成 Point，不发送 Push，不接 order / execution / auto-trading。

Capability level 仍为 `REVIEW_ONLY_RUNTIME partial`。本包让 DecisionResult slice 具备最小 review-only runtime/API/dashboard 可见性，但仍需后续 verification / smoke 才能闭环确认。

# 2. Reused Existing Assets

| Asset | Reused? | Role |
|---|---:|---|
| `DecisionService.getLatestDecisionResultBySymbol` | Yes | DecisionResult read-model owner path |
| `DecisionResultVO` | Yes | Existing read model; status endpoint只读取安全字段 |
| `DashboardController` | Yes | Existing dashboard/API owner |
| `dashboard.html` | Yes | Existing dashboard runtime display surface |
| `DashboardControllerTest` | Yes | Targeted API/dashboard coverage |
| `tm_decision_result` / mapper path | Yes | Existing persisted owner path，未改 schema |

# 3. Endpoint

Endpoint:

```text
GET /api/dashboard/decision-result-status?symbol=BTCUSDT
```

返回字段：

- `status`
- `symbol`
- `analysisId`
- `decisionAvailable`
- `decisionStatus`
- `confidence`
- `aiRoleResultsAvailable`
- `aiRoleResultsSummary`
- `sourceTraceComplete`
- `sourceHealth`
- `reason`
- `message`
- `reviewOnly = true`
- `notTradingSignal = true`
- `notCandidateSignal = true`
- `notDecisionGeneration = true`
- `notPointSignal = true`
- `watchlistBounded = true`
- `marketQuoteChecked = true`
- `evidenceScoreChecked = true`
- `displaySlotsAreCandidatePool = false`
- `failClosed`

Endpoint 不返回：

- candidate ranking
- executable final direction
- entry
- stop
- TP
- RR
- position size
- leverage
- order action
- Push send state
- auto-trading action

# 4. Status Mapping

| Status | Trigger | Fail-closed |
|---|---|---:|
| `DECISIONRESULT_REVIEW_ONLY_READY` | DecisionResult exists, createTime/readModel/source trace/ai role availability are sufficient | No |
| `DECISIONRESULT_MISSING_FAIL_CLOSED` | No DecisionResult exists for symbol | Yes |
| `DECISIONRESULT_READ_MODEL_PARTIAL` | `readModelTruthStatus` is not `FULL` or fallback reason exists | Yes |
| `DECISIONRESULT_SOURCE_TRACE_PARTIAL` | required read-model anchors are incomplete | Yes |
| `DECISIONRESULT_AI_ROLE_PARTIAL` | `ai_role_results` missing | Yes |
| `DECISIONRESULT_STALE_OR_UNKNOWN_FAIL_CLOSED` | createTime or read-model completeness unknown | Yes |
| `DECISIONRESULT_BLOCKED_FAIL_CLOSED` | default blocked status before owner-path read succeeds | Yes |

# 5. Dashboard Panel

Dashboard panel:

```text
decisionResultStatusPanel
```

显示：

- DecisionResult status
- symbol / analysisId
- decisionAvailable / confidence
- ai_role_results availability / summary
- source trace / source health
- review-only label
- not trading / not candidate / not decision generation / not point label
- Watchlist / MarketQuote / Evidence-Score boundary label
- Display Slots is not candidate pool copy

Dashboard panel 只展示状态，不展示 entry / stop / TP / RR，不展示 final direction，不提供 order / Push 操作按钮。

# 6. Fail-Closed Behavior

DecisionResult 缺失、read model partial、source trace partial、ai_role_results partial、createTime/read-model completeness unknown 时，status 均 fail-closed。

Fail-closed 只影响 review-only status：候选、决策生成、点位、Push、交易动作全部保持关闭。

# 7. Tests

新增 / 更新 targeted tests：

- `decisionResultStatusEndpointReturnsReviewOnlyReadyStatus`
- `decisionResultStatusEndpointFailsClosedWhenDecisionResultMissing`
- `decisionResultStatusEndpointMarksReadModelPartialFailClosed`
- `decisionResultStatusEndpointMarksSourceTracePartialFailClosed`
- `decisionResultStatusEndpointMarksAiRolePartialFailClosed`
- `decisionResultStatusEndpointDoesNotExposeExecutableCandidateDecisionPointOrTradingFields`
- `dashboardTemplateShowsReviewOnlyDecisionResultRuntimeStatusMapping`

已先行通过：

```text
./mvnw -q -Dtest=DashboardControllerTest test
```

# 8. Forbidden Semantics

本包没有接入：

- Push
- external channel
- Candidate
- Decision generation
- Point
- final direction
- entry / stop / TP / RR
- position size
- leverage
- order / execution / auto-trading
- P359 / P360

# 9. Capability-Level Statement

当前 level: `REVIEW_ONLY_RUNTIME partial`。

本包是否提升 capability level: Not yet as completed project state; implementation branch adds DecisionResult review-only API/dashboard visibility, but closure requires verification.

未来 verification 通过后，DecisionResult slice 可标记为 `REVIEW_ONLY_RUNTIME partial`。

仍不等于 Production Wiring。

仍不等于 Push。

仍不等于 Candidate generation。

仍不等于 Decision generation。

仍不等于 Point generation。

仍不等于 Trading。

# 10. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, implementation pending verification
- 是否接 service/runtime/dashboard/API: Yes, minimal review-only API/dashboard status only
- 是否符合 #830 审计建议: Yes

# 11. Next Required Action

下一步：`Minimal Review-Only DecisionResult Runtime Wiring Verification`。

Verification 必须确认 workflow contract、compile、test-compile、full test、API smoke、dashboard smoke、forbidden path、forbidden semantics 全部通过后，才能把 DecisionResult slice 作为 review-only runtime 小闭环收口。
