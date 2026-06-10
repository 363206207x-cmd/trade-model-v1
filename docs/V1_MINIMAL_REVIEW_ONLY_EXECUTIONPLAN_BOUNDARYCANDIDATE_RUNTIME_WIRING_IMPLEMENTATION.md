# V1 Minimal Review-Only ExecutionPlan / BoundaryCandidate Runtime Wiring Implementation

## 1. Executive Summary

本包实现最小 ExecutionPlan / BoundaryCandidate review-only runtime status。实现只增加只读状态可见性，不生成 Candidate，不生成新的 Decision，不生成 Point，不输出 final direction / entry / stop / TP / RR，不接 Push / external channel，不接 order / execution / auto-trading。

本包复用既有 `DashboardController`、`/api/dashboard/detail`、`DashboardDetailResponseVO`、`PlanBoundaryDisplayAdapter`、`ExecutionPlanDisplayAdapter`、`RiskActionGuardDisplayAdapter`、`SourceTraceDTO` 和 dashboard display owner path。没有新增 DTO / Validator / Assembler / Orchestrator，没有修改 schema/config/pom。

当前 capability level 保持：`REVIEW_ONLY_RUNTIME partial`。下一允许动作是：`Minimal Review-Only ExecutionPlan / BoundaryCandidate Runtime Wiring Verification`。

## 2. Implemented Surface

| Area | Result | Notes |
|---|---|---|
| Endpoint | `GET /api/dashboard/execution-plan-boundary-status?symbol=BTCUSDT` | Read-only compact status surface over existing dashboard detail owner path. |
| Dashboard panel | `executionPlanBoundaryStatusPanel` | Minimal status/copy/DOM only, placed after DecisionResult status and before the existing workbench detail sections. |
| Tests | `DashboardControllerTest` | Covers endpoint flags, fail-closed missing DecisionResult, forbidden field absence, and dashboard DOM/static copy. |
| Source-of-truth | Updated | Aligns current main baseline to `a84a4aa` and next action to verification. |

## 3. Review-Only Fields

The endpoint returns only review-only status fields:

- `status`
- `symbol`
- `analysisId`
- `planBoundaryStatus`
- `executionPlanStatus`
- `sourceTraceStatus`
- `sourceTraceComplete`
- `sourceHealth`
- `riskActionGuardStatus`
- `notExecutableReason`
- `incompleteReasons`
- `blockingReasons`
- `reason`
- `message`
- `reviewOnly = true`
- `notTradingSignal = true`
- `notCandidateSignal = true`
- `notDecisionGeneration = true`
- `notPointSignal = true`
- `notExecutable = true`
- `watchlistBounded = true`
- `marketQuoteChecked = true`
- `evidenceScoreChecked = true`
- `decisionResultChecked = true`
- `displaySlotsAreCandidatePool = false`
- `failClosed`

The endpoint does not expose candidate ranking, final direction, entry, stop, TP, RR, position size, leverage, order action, Push send state, or auto-trading action fields.

## 4. Status Mapping

| Status | Trigger | Fail-closed | Review-only |
|---|---|---:|---:|
| `EXECUTIONPLAN_BOUNDARY_REVIEW_ONLY_READY` | Existing PlanBoundary / ExecutionPlan displays are readable, source trace has required boundary sources, RiskActionGuard remains manual-review only, and ExecutionPlan status is `READY_REVIEW_ONLY`. | No | Yes |
| `PLAN_BOUNDARY_BACKEND_PENDING_FAIL_CLOSED` | PlanBoundary owner path is missing or `BACKEND_PENDING`. | Yes | Yes |
| `PLAN_BOUNDARY_INCOMPLETE_FAIL_CLOSED` | PlanBoundary or ExecutionPlan display reports incomplete state. | Yes | Yes |
| `PLAN_BOUNDARY_WATCH_ONLY` | PlanBoundary or ExecutionPlan display reports watch-only state. | Yes | Yes |
| `EXECUTIONPLAN_BOUNDARY_PENDING_FAIL_CLOSED` | ExecutionPlan display is missing or still `BOUNDARY_PENDING`. | Yes | Yes |
| `EXECUTIONPLAN_SOURCE_TRACE_PARTIAL` | Source trace is missing or does not have required boundary sources. | Yes | Yes |
| `EXECUTIONPLAN_RISK_GUARD_BLOCKED_FAIL_CLOSED` | RiskActionGuard is missing, pending, or indicates action permission / unsafe blocker. | Yes | Yes |
| `EXECUTIONPLAN_BOUNDARY_BLOCKED_FAIL_CLOSED` | DecisionResult is missing or status is unknown/unsafe. | Yes | Yes |

## 5. Dashboard Behavior

The dashboard panel shows:

- ExecutionPlan / BoundaryCandidate status
- symbol and analysis id
- PlanBoundary status
- ExecutionPlan status
- source trace status and source health
- RiskActionGuard status
- not executable reason
- review-only / not trading copy
- not Candidate / not new Decision generation / not Point copy
- Watchlist / MarketQuote / Evidence-Score / DecisionResult boundary copy
- Display Slots boundary copy

The panel does not add executable buttons, Push buttons, order controls, final direction, entry, stop, TP, RR, position size, leverage, or trading action semantics.

## 6. Fail-Closed Behavior

Default endpoint state is fail-closed. Missing DecisionResult, missing PlanBoundary owner data, partial source trace, pending ExecutionPlan, or unsafe RiskActionGuard all keep `failClosed=true`.

The endpoint sanitizes status reasons before returning compact status values, so numeric boundary source details stay inside the existing owner path and are not exposed as executable trading fields.

## 7. Test Coverage

Targeted coverage in `DashboardControllerTest` verifies:

- endpoint returns `reviewOnly=true`
- endpoint returns `notTradingSignal=true`
- endpoint returns `notCandidateSignal=true`
- endpoint returns `notDecisionGeneration=true`
- endpoint returns `notPointSignal=true`
- endpoint returns `notExecutable=true`
- endpoint returns `displaySlotsAreCandidatePool=false`
- missing DecisionResult fails closed
- forbidden executable fields are absent from endpoint response
- dashboard contains `executionPlanBoundaryStatusPanel`
- dashboard contains `executionPlanBoundaryRuntimeStatusValue`, `planBoundaryStatusValue`, `executionPlanStatusValue`, and `executionPlanBoundarySignalBoundaryValue`
- dashboard contains review-only / not Candidate / not Decision generation / not Point boundary copy

## 8. Boundary Confirmation

- DTO / Validator / Assembler / Orchestrator: not added.
- schema/config/pom: not changed.
- Push / external channel: not connected.
- Candidate generation: not connected.
- Decision generation: not connected.
- Point generation: not connected.
- final direction / entry / stop / TP / RR output: not exposed.
- order / execution / auto-trading: not connected.
- P359 / P360: still frozen.

## 9. Next Step

Proceed to `Minimal Review-Only ExecutionPlan / BoundaryCandidate Runtime Wiring Verification`. Verification must confirm compile/test/API/dashboard/forbidden semantics and must not add new functionality.
