# V1 Data Source Health Dashboard/API Status Runtime Wiring Implementation

## 1. Executive Summary

本包实现了 `Data Source Health dashboard/API status` 的最小 review-only runtime status（只读运行时状态）。

- 新增 endpoint：`GET /api/dashboard/data-source-health-status?symbol=BTCUSDT`
- 新增 dashboard panel：`dataSourceHealthStatusPanel`
- 复用 existing slice-local source-health status surfaces：MarketQuote、Evidence / Score、DecisionResult、ExecutionPlan / BoundaryCandidate、Review / Replay
- 不新增 DTO / Validator / Assembler / Orchestrator
- 不改 schema / config / pom
- 不触发 external API refresh / scheduler / collector / API client
- 不接 Push / external channel
- 不生成 Candidate / Decision generation / Point
- 不输出 final direction / entry / stop / TP / RR
- 不接 order / execution / auto-trading

当前 capability level 仍为 `REVIEW_ONLY_RUNTIME partial`。Data Source Health 仍未形成完整闭环，下一步必须做 runtime wiring verification。

## 2. Implemented Endpoint

| Item | Value |
|---|---|
| Endpoint | `/api/dashboard/data-source-health-status?symbol=BTCUSDT` |
| Method | `GET` |
| Owner path | Existing `DashboardController` review-only status owner path |
| Response shape | `Map<String, Object>` |
| DTO added | No |
| External refresh | No |
| Trading semantics | No |

Implemented fields:

- `status`
- `symbol`
- `sourceHealth`
- `scopedSources`
- `sourceStatuses`
- `okSources`
- `partialSources`
- `staleSources`
- `missingSources`
- `watchOnlySources`
- `blockedSources`
- `reason`
- `message`
- `failClosed`
- `reviewOnly = true`
- `notTradingSignal = true`
- `notCandidateSignal = true`
- `notDecisionGeneration = true`
- `notPointSignal = true`
- `notReplayExecution = true`
- `notExecutable = true`
- `watchlistBounded = true`
- `marketQuoteChecked = true`
- `evidenceScoreChecked = true`
- `decisionResultChecked = true`
- `executionPlanBoundaryChecked = true`
- `reviewReplayChecked = true`
- `externalRefreshTriggered = false`
- `displaySlotsAreCandidatePool = false`

## 3. Status Mapping

| Status | Trigger | Fail-closed | Notes |
|---|---|---:|---|
| `DATA_SOURCE_HEALTH_REVIEW_ONLY_READY` | All scoped source statuses are OK | No | Review-only status only |
| `DATA_SOURCE_HEALTH_PARTIAL_REVIEW_ONLY` | At least one source is PARTIAL and no missing/stale/blocked source exists | Yes | Human review only |
| `DATA_SOURCE_HEALTH_STALE_FAIL_CLOSED` | At least one source is STALE | Yes | No refresh is triggered |
| `DATA_SOURCE_HEALTH_MISSING_FAIL_CLOSED` | At least one source is MISSING | Yes | Missing data is not fabricated |
| `DATA_SOURCE_HEALTH_WATCH_ONLY_REVIEW` | At least one source is WATCH_ONLY and no missing/stale/blocked source exists | Yes | Observation only |
| `DATA_SOURCE_HEALTH_BLOCKED_FAIL_CLOSED` | At least one source is BLOCKED/unknown | Yes | Conservative default |

The implementation intentionally keeps MarketQuote and ExecutionPlan / BoundaryCandidate source surfaces conservative in the aggregate endpoint so this endpoint does not trigger market quote refresh, dashboard detail expansion, scheduler, collector, or API-client reads. Missing or watch-only ambiguity stays fail-closed.

## 4. Dashboard Surface

Added panel:

- DOM id: `dataSourceHealthStatusPanel`
- Status id: `dataSourceHealthRuntimeStatusValue`
- Source health id: `dataSourceHealthSourceHealthValue`
- Scoped sources id: `dataSourceHealthScopedSourcesValue`
- Source buckets:
  - `dataSourceHealthOkSourcesValue`
  - `dataSourceHealthPartialSourcesValue`
  - `dataSourceHealthMissingStaleSourcesValue`
  - `dataSourceHealthWatchBlockedSourcesValue`
- Safety copy:
  - `dataSourceHealthReviewOnlyValue`
  - `dataSourceHealthSignalBoundaryValue`
  - `dataSourceHealthRefreshBoundaryValue`
  - `dataSourceHealthUpstreamValue`

The dashboard copy explicitly says Data Source Health is review-only, not a trading signal, not Candidate, not Decision generation, not Point, not executable, and does not trigger external refresh.

## 5. Test Coverage

Targeted coverage added to `DashboardControllerTest`:

- dashboard template contains `/api/dashboard/data-source-health-status`
- dashboard template contains `dataSourceHealthStatusPanel`
- dashboard template contains expected DOM ids and safety copy
- endpoint returns review-only safety fields
- endpoint returns `failClosed=true` when source ambiguity exists
- endpoint does not expose executable/trading fields such as entry, stop, TP, RR, finalDirection, orderAction, executionAction, Push send state, scheduler/collector/API-client trigger, or auto-trading action

## 6. Boundary Confirmation

- No DTO / Validator / Assembler / Orchestrator added.
- No schema / config / pom changed.
- No external API refresh, scheduler, collector, or API client trigger added.
- No Push or external channel wiring added.
- No Candidate generation added.
- No Decision generation added.
- No Point generation added.
- No final direction, entry, stop, TP, RR, position size, leverage, order action, execution action, or auto-trading action added.
- No replay execution or review result generation added.
- P359 / P360 remain frozen.

## 7. Capability-Level Statement

- Current level: `REVIEW_ONLY_RUNTIME partial`
- This implementation moves Data Source Health toward a review-only runtime slice but does not complete the slice.
- It is not Production Wiring.
- It is not source-health persistence.
- It is not external refresh.
- It is not Push.
- It is not Candidate generation.
- It is not Decision generation.
- It is not Point generation.
- It is not Trading.

## 8. Next Allowed Action

`Minimal Review-Only Data Source Health Dashboard/API Status Runtime Wiring Verification`

The verification package must validate compile/test results, endpoint fields, dashboard DOM/copy, forbidden semantics, source-of-truth alignment, and no external refresh / no trading boundaries.
