# Minimal Review-Only RiskActionGuard Read-Only Status Runtime Wiring Implementation

## 1. Executive Summary

本包实现了 RiskActionGuard read-only status（风险动作守卫只读状态）的最小运行时接线。

- Current merged main baseline: `c7fb97e docs(risk): verify implementation readiness (#932)`
- Implemented endpoint: `GET /api/dashboard/risk-action-guard-status?symbol=BTCUSDT`
- Implemented dashboard panel: `riskActionGuardStatusPanel`
- Implementation risk: B-risk
- Capability movement: none; overall level remains `REVIEW_ONLY_RUNTIME partial`
- Completed slice count: still 10; RiskActionGuard 仍需 verification 和 visual closure 后才能成为完整闭环

本实现只读取现有 Dashboard / DecisionResult / PlanBoundary / ExecutionPlan / RiskActionGuard display owner path，不生成新的 Decision，不生成 Candidate，不生成 Point，不输出 final direction / entry / stop / TP / RR，不接 Push，不接 order/execution/auto-trading。

## 2. Reused Existing Assets

| Asset | Reused? | Notes |
|---|---:|---|
| `DashboardController` | Yes | 新增一个最小只读 `Map` endpoint；不新增 controller family。 |
| `DecisionService#getLatestDecisionResultBySymbol` | Yes | 只读读取 latest decision read model；缺失时 fail-closed。 |
| `PlanBoundaryDisplayAdapter` | Yes | 复用已有 plan boundary display owner path。 |
| `ExecutionPlanDisplayAdapter` | Yes | 复用已有 execution plan display owner path。 |
| `RiskActionGuardDisplayAdapter` | Yes | 复用已有 RiskActionGuard display owner path。 |
| `DefaultRiskActionGuardDisplayAdapter` | Yes | 保持现有 adapter 语义；不扩展业务动作。 |
| `RiskActionGuardDisplayVO` | Yes | 作为 read-only status source。 |
| `dashboard.html` | Yes | 新增最小 panel/copy/DOM，不做大改版。 |
| `DashboardControllerTest` | Yes | 新增 targeted endpoint/dashboard/safety tests。 |

## 3. Endpoint Behavior

Endpoint: `GET /api/dashboard/risk-action-guard-status?symbol=BTCUSDT`

Purpose: return RiskActionGuard read-only runtime status for dashboard display.

The endpoint returns only status and safety metadata:

- `status`
- `symbol`
- `analysisId`
- `riskActionGuardStatus`
- `riskActionGuardStatusLabel`
- `riskActionAdviceSummary`
- `riskActionBlockingReason`
- `liquidityState`
- `stampedeDetected`
- `wickOnlyRisk`
- `manualRiskReviewRequired`
- `actionFlagsAllFalse`
- `planBoundaryStatus`
- `executionPlanStatus`
- `sourceTraceComplete`
- `sourceHealth`
- `reason`
- `message`
- `reviewOnly=true`
- `manualReviewOnly=true`
- `notTradingSignal=true`
- `notCandidateSignal=true`
- `notDecisionGeneration=true`
- `notPointSignal=true`
- `notExecutable=true`
- `notPositionMonitorExecution=true`
- `notExecutionPlanGeneration=true`
- `notBoundaryCandidateGeneration=true`
- `externalRefreshTriggered=false`
- `displaySlotsAreCandidatePool=false`
- `failClosed`

The endpoint does not expose:

- `candidateRanking`
- `candidateScore`
- `finalDirection`
- `entry`
- `stop`
- `takeProfit`
- `tp`
- `riskReward`
- `rr`
- `positionSize`
- `leverage`
- `orderAction`
- `executionAction`
- `pushSendState`
- `autoTradingAction`

## 4. Dashboard Behavior

新增 dashboard panel:

- `riskActionGuardStatusPanel`
- `riskActionGuardRuntimeStatusValue`
- `riskActionGuardSymbolValue`
- `riskActionGuardAnalysisIdValue`
- `riskActionGuardStatusValue`
- `riskActionGuardLiquidityValue`
- `riskActionGuardManualReviewValue`
- `riskActionGuardActionFlagsValue`
- `riskActionGuardAdviceValue`
- `riskActionGuardSourceHealthValue`
- `riskActionGuardFailClosedValue`
- `riskActionGuardReviewOnlyValue`
- `riskActionGuardSignalBoundaryValue`
- `riskActionGuardActionBoundaryValue`
- `riskActionGuardUpstreamValue`
- `riskActionGuardReasonValue`

The panel displays only review-only/manual-review status. It explicitly states that reduce / close / reverse / move stop / open / execute wording is guardrail/manual-review copy only and cannot become executable action, order action, or trading signal.

## 5. Status Mapping

| Status | Fail-closed? | Trigger |
|---|---:|---|
| `RISK_ACTION_GUARD_REVIEW_ONLY_READY` | No | Existing RiskActionGuard display is available, action flags are false, and no unsafe action wording is present. |
| `BACKEND_PENDING_FAIL_CLOSED` | Yes | RiskActionGuard display is missing or backend display path is pending. |
| `DECISION_MISSING_FAIL_CLOSED` | Yes | Latest DecisionResult read model is missing. |
| `PLAN_BOUNDARY_FAIL_CLOSED` | Yes | PlanBoundary display reports invalid or fail-closed state. |
| `EXECUTION_PLAN_NOT_READY_FAIL_CLOSED` | Yes | ExecutionPlan display is missing/not ready/fail-closed. |
| `LIQUIDITY_CONTEXT_MISSING_FAIL_CLOSED` | Yes | Liquidity context is missing or unknown. |
| `LIQUIDITY_DETERIORATION_REVIEW_ONLY` | Yes | Liquidity deterioration is visible; display remains review-only. |
| `STAMPEDE_REVIEW_ONLY_FAIL_CLOSED` | Yes | Stampede risk is visible. |
| `WICK_ONLY_REVIEW_ONLY_FAIL_CLOSED` | Yes | Wick-only risk is visible. |
| `HIGH_RISK_REVIEW_ONLY` | Yes | High-risk manual review status is visible. |
| `ACTION_FLAGS_BLOCKED_FAIL_CLOSED` | Yes | Any action flag is true. |
| `ACTION_WORDING_BLOCKED_FAIL_CLOSED` | Yes | Action wording appears without safe guardrail/manual-review/blocked context. |

## 6. Action Wording Guardrails

The following words are blocked as executable semantics:

- reduce
- close
- reverse
- move stop
- open
- execute

They may appear only as guardrail/manual-review copy. The implementation summarizes unsafe advice as `manual-review-only action wording hidden` instead of exposing executable-looking action text.

## 7. Tests

Targeted tests added in `DashboardControllerTest` cover:

- endpoint returns `reviewOnly=true`
- endpoint returns `manualReviewOnly=true`
- endpoint returns all not-trading / not-candidate / not-decision-generation / not-point / not-executable safety flags
- missing DecisionResult returns `DECISION_MISSING_FAIL_CLOSED`
- action flags true returns `ACTION_FLAGS_BLOCKED_FAIL_CLOSED`
- unsafe action wording returns `ACTION_WORDING_BLOCKED_FAIL_CLOSED`
- forbidden executable fields are absent from the endpoint response
- dashboard template contains `riskActionGuardStatusPanel`
- dashboard template contains required DOM ids and safety copy

## 8. Boundary Confirmation

- No new DTO / Validator / Assembler / Orchestrator.
- No schema/config/pom change.
- No new service/domain ownership family.
- No Push or external channel.
- No Candidate generation.
- No Decision generation.
- No Point generation.
- No final direction / entry / stop / TP / RR.
- No order / execution / auto-trading.
- No Position Monitor execution.
- No replay / recheck execution.
- No P359 / P360 continuation.

## 9. Next Step

Next allowed action:

`Minimal Review-Only RiskActionGuard Read-Only Status Runtime Wiring Verification`

That package must be A-risk verification docs/source-of-truth only unless a verification defect is found and explicitly scoped.
