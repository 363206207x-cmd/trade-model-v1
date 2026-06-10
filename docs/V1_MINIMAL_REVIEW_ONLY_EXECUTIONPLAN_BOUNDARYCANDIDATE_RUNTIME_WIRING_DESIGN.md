# V1 Minimal Review-Only ExecutionPlan / BoundaryCandidate Runtime Wiring Design

## 1. Executive Summary

本任务只设计，不实现。

ExecutionPlan / BoundaryCandidate runtime status 的最小目标是：复用已有 `DashboardController` / `/api/dashboard/detail` / `DefaultPlanBoundaryDisplayAdapter` / `DefaultExecutionPlanDisplayAdapter` / `BoundaryCandidateService` / `PlanService` owner path，让用户能看到 PlanBoundary / ExecutionPlan 是否可读、是否仍 fail-closed、SourceTrace / RiskActionGuard 是否完整，以及该输出仍然只是 review-only display continuation。

Owner path 是：

```text
Watchlist / MarketQuote / Evidence-Score / DecisionResult completed slices
  -> DecisionService / DecisionResult read model
  -> DashboardController /api/dashboard/detail
  -> DashboardSourceTraceDetailAdapter
  -> DefaultPlanBoundaryDisplayAdapter / PlanBoundaryDisplayVO
  -> DefaultExecutionPlanDisplayAdapter / ExecutionPlanDisplayVO
  -> dashboard PlanBoundary / ExecutionPlan review-only display status

BoundaryCandidateService / BoundaryCandidateDTO
  and PlanService / ExecutionPlanVO / ExecutionPlanDO / ExecutionPlanMapper / tm_execution_plan
  remain the underlying owner assets, but the future status surface must not call /api/plan/generate.
```

不需要新增 DTO / Validator / Assembler。是否需要新增 endpoint 由下一步 readiness gate 判断；若已有 `/api/dashboard/detail` 足够，应优先复用，若不够才允许设计一个最小只读 status endpoint。无需改 schema。不得接 Push / Candidate / Decision generation / Point / Trading，不得生成候选 / 点位 / 方向，不得输出 entry / stop / TP / RR、position size、leverage、order action。

下一步应该进入：`Minimal Review-Only ExecutionPlan / BoundaryCandidate Runtime Wiring Implementation Readiness Gate`。

## 2. Owner Path To Preserve

Future implementation must preserve this owner path:

```text
Watchlist / MarketQuote / Evidence-Score / DecisionResult completed slices
  -> DecisionService / DecisionResult read model
     (exists; runtime read; dashboard visible; review-only safe)
  -> DashboardController /api/dashboard/detail
     (exists; read-only dashboard API; dashboard visible; review-only safe)
  -> DashboardSourceTraceDetailAdapter
     (exists; partial SourceTrace / RuntimeKline / derivatives-risk metadata; review-only safe)
  -> DefaultPlanBoundaryDisplayAdapter / DefaultPlanBoundarySourceTraceAdapter
     (exists; PlanBoundary display; fail-closed; manualReviewRequired=true; notTradeInstruction=true)
  -> DefaultExecutionPlanDisplayAdapter
     (exists; ExecutionPlan display; fail-closed; no entry/stop/take-profit value generation)
  -> dashboard planBoundaryPlaceholderCard / structured workbench
     (exists; visible; review-only display continuation)
```

Underlying owner assets:

- `BoundaryCandidateService` / `BoundaryCandidateServiceImpl` / `BoundaryCandidateDTO` / `BoundaryStatusEnum`
- `PlanService` / `PlanServiceImpl` / `ExecutionPlanVO` / `ExecutionPlanDO` / `ExecutionPlanMapper`
- `tm_execution_plan`
- `DashboardDetailResponseVO.PlanBoundaryDisplayVO`
- `DashboardDetailResponseVO.ExecutionPlanDisplayVO`

Future implementation must not bypass the existing BoundaryCandidate / ExecutionPlan / dashboard display owner path. It must not create a new ExecutionPlan / BoundaryCandidate wrapper owner. It must not use `/api/plan/generate` as a status endpoint because that endpoint has generation semantics. It must not directly connect Push / Candidate / Decision generation / Point. It must not treat Display Slots as a candidate pool. It must obey the completed Watchlist / MarketQuote / Evidence-Score / DecisionResult boundaries.

## 3. Minimal Future Status Mapping

Allowed future review-only statuses:

| Status | Trigger condition | Dashboard/API copy | Candidate / Decision / Point / Push allowed? | Review-only? | Fail-closed? |
|---|---|---|---|---:|---:|
| `EXECUTIONPLAN_BOUNDARY_REVIEW_ONLY_READY` | `PlanBoundaryDisplay` and `ExecutionPlanDisplay` are readable, source trace is complete enough for display, RiskActionGuard does not expose action permission, and display remains `manualReviewRequired=true` / `notTradeInstruction=true`. | `ExecutionPlan / BoundaryCandidate 只读状态可见；仅人工复核，不是交易信号，不生成点位或执行动作。` | No | Yes | No, but still not executable |
| `PLAN_BOUNDARY_BACKEND_PENDING_FAIL_CLOSED` | Missing DecisionResult, missing analysis id, missing PlanBoundary display, or backend connection pending. | `PlanBoundary 后端状态未齐；fail-closed，仅展示等待接入状态。` | No | Yes | Yes |
| `PLAN_BOUNDARY_INCOMPLETE_FAIL_CLOSED` | PlanBoundary display reports `INCOMPLETE`, SourceTrace missing/partial, read model partial, or text-only plan narrative without complete display source. | `PlanBoundary 信息不完整；不生成候选、不生成点位、不输出交易动作。` | No | Yes | Yes |
| `PLAN_BOUNDARY_WATCH_ONLY` | PlanBoundary display reports `WATCH_ONLY` or source trace fallback requires watch-only review. | `PlanBoundary 仅观察；只能人工复核，不进入候选/点位/执行链路。` | No | Yes | Yes |
| `EXECUTIONPLAN_BOUNDARY_PENDING_FAIL_CLOSED` | ExecutionPlan display reports `BOUNDARY_PENDING`, no aligned PlanBoundary, or `PLAN_BOUNDARY_BACKEND_PENDING`. | `ExecutionPlan 等待边界状态；fail-closed，不生成执行计划动作。` | No | Yes | Yes |
| `EXECUTIONPLAN_SOURCE_TRACE_PARTIAL` | ExecutionPlan display reports incomplete/watch-only due to SourceTrace missing/incomplete/safe-fail-closed. | `ExecutionPlan SourceTrace 不完整；只显示状态，不输出 entry / stop / TP / RR。` | No | Yes | Yes |
| `EXECUTIONPLAN_RISK_GUARD_BLOCKED_FAIL_CLOSED` | RiskActionGuard is missing, pending, high-risk, liquidity incomplete, stampede/wick-only risk, or exposes blocked action flags. | `RiskActionGuard 阻断；ExecutionPlan 只读 fail-closed，不连接 Push、order 或执行动作。` | No | Yes | Yes |
| `EXECUTIONPLAN_BOUNDARY_BLOCKED_FAIL_CLOSED` | Unknown/unsafe status, unsafe `VALID` interpretation, forbidden executable semantics, or any ambiguity in upstream boundary. | `ExecutionPlan / BoundaryCandidate 状态不安全；fail-closed，必须人工复核。` | No | Yes | Yes |

`READY_REVIEW_ONLY` and `BoundaryCandidate VALID` must remain review-only display states. They are not final direction, not point readiness, not an order instruction, and not Production Wiring.

## 4. Minimal Future Fields

Allowed future fields:

- `status`
- `symbol`
- `analysisId`
- `planBoundaryStatus`
- `planBoundaryStatusLabel`
- `executionPlanStatus`
- `executionPlanStatusLabel`
- `executionPlanBoundaryAligned`
- `sourceTraceStatus`
- `backendConnectionStatus`
- `sourceTraceComplete`
- `sourceHealth`
- `riskActionGuardStatus`
- `riskActionGuardBlockingReason`
- `incompleteReasons`
- `blockingReasons`
- `notExecutableReason`
- `manualReviewRequired`
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
- `reason`
- `message`

Forbidden future fields:

- candidate ranking
- final direction
- entry
- stop
- TP
- RR
- position size
- leverage
- order action
- Push send state
- auto-trading action

Legacy owner assets may contain historical names such as `entry_zone`, `stop_loss`, `take_profit_rules`, `leverage_suggestion`, or `position_suggestion`, but the future status surface must not expose those fields as actionable values.

## 5. Dashboard/API Minimal Surface

Future minimal API surface:

- Prefer reuse of `/api/dashboard/detail?symbol=BTCUSDT` if its `planBoundaryDisplay`, `executionPlanDisplay`, `riskActionGuardDisplay`, and `sourceTrace` fields are enough for a status view.
- If readiness gate finds a compact endpoint necessary, allow only one minimal read-only endpoint such as `/api/dashboard/execution-plan-boundary-status?symbol=BTCUSDT`.
- The endpoint may return `Map` or existing VO-derived fields; it must not create a new DTO.
- The endpoint must not call `/api/plan/generate`.
- The endpoint must not generate a new BoundaryCandidate, ExecutionPlan, DecisionResult, Candidate, Point, Push payload, or trading action.

Future minimal dashboard surface:

- Reuse `planBoundaryPlaceholderCard` or add a minimal status/copy/DOM area only if readiness gate confirms the safe slot.
- Show PlanBoundary status.
- Show ExecutionPlan status.
- Show SourceTrace / backend connection / source health.
- Show RiskActionGuard status and blocking reason if available.
- Show incomplete / blocking / not executable reason.
- Show review-only label.
- Show not trading / not candidate / not decision generation / not point / not executable label.
- Show Watchlist / MarketQuote / Evidence-Score / DecisionResult boundary label.
- Show Display Slots are not candidate pool.

The dashboard must not add a complex execution card, all-market scan, point proposal UI, Push action, order button, or any copy that implies executable trade advice.

## 6. Watchlist / MarketQuote / Evidence-Score / DecisionResult Boundary

ExecutionPlan / BoundaryCandidate status must not bypass:

- Watchlist Pool / RuleConfig boundary.
- MarketQuote freshness / fallback / source-health boundary.
- Evidence / Score review-only availability boundary.
- DecisionResult review-only read-model boundary.
- Display Slots boundary.

If any upstream state is missing, stale, fallback ambiguous, partial, or unsafe, the status must fail closed. Display Slots are homepage display positions only; they are not the candidate universe. Assets outside Watchlist Pool must not enter candidate / push / point / execution chains from this slice.

## 7. Minimal Future Implementation Boundary

If the next readiness gate is GO, future minimal implementation must be limited to:

- Reuse `DashboardController` / `/api/dashboard/detail` whenever possible.
- Optionally add one compact read-only status endpoint only after readiness gate.
- Reuse `DefaultPlanBoundaryDisplayAdapter`, `DefaultExecutionPlanDisplayAdapter`, `DashboardSourceTraceDetailAdapter`, existing VO fields, and existing mapper/schema assets.
- Optionally add minimal dashboard status/copy only after readiness gate.
- Add targeted controller/dashboard/display tests only after readiness gate.
- Update source-of-truth docs.

Future implementation must not:

- add DTO / Validator / Assembler / Orchestrator;
- modify schema / config / pom;
- use `/api/plan/generate` for status;
- connect Push;
- connect Candidate generation;
- connect Decision generation;
- connect Point generation;
- output entry / stop / TP / RR;
- output final direction;
- output position size / leverage as action;
- connect order / execution / auto-trading;
- revive P359 / P360.

## 8. Readiness Checklist

The next readiness gate must check:

- Whether `/api/dashboard/detail` already provides enough fields for the minimal status.
- Whether a new compact endpoint is truly needed.
- Whether endpoint response can use `Map` / existing VO fields without new DTO.
- Whether `PlanBoundaryDisplayVO` and `ExecutionPlanDisplayVO` fields are sufficient.
- Whether SourceTrace / source health fields are sufficient.
- Whether RiskActionGuard fields are sufficient.
- Whether `planBoundaryPlaceholderCard` is the safe dashboard DOM slot.
- Whether existing tests cover fail-closed mapping, source trace partial, risk guard blocked, and no executable semantics.
- Whether targeted tests can be added without broad dashboard redesign.
- Whether forbidden terms appear only in guardrails/historical docs/tests, not in new implementation output.
- Whether Push / Candidate / Decision generation / Point / Trading remain unconnected.

## 9. Capability-Level Movement

- 当前 level: `REVIEW_ONLY_RUNTIME partial`
- 本包是否提升 level: No, design only.
- 未来最小 ExecutionPlan / BoundaryCandidate implementation 目标：`REVIEW_ONLY_RUNTIME partial` for ExecutionPlan / BoundaryCandidate display continuation.
- 不等于 Production Wiring.
- 不等于 Push.
- 不等于 Candidate generation.
- 不等于 Decision generation.
- 不等于 Point generation.
- 不等于 Trading.

## 10. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes. The design routes through existing BoundaryCandidate / ExecutionPlan / dashboard display owners instead of adding another wrapper family.
- 是否提升 capability level: No, design only.
- 是否接 service/runtime/dashboard/API: No, design only.
- 是否符合 #830 审计建议: Yes

## 11. Final Recommendation

可以进入 implementation readiness gate。最小实现大概只允许复用 `/api/dashboard/detail` 或新增一个 compact read-only status endpoint、复用 existing display adapters / VO fields、增加最小 dashboard status/copy 和 targeted tests。禁止新增 DTO / Validator / Assembler，禁止使用 `/api/plan/generate` 作为 status endpoint，禁止 Push、Candidate、Decision generation、Point、entry / stop / TP / RR、final direction、order / execution / auto-trading，禁止继续 P359/P360。它不是 Push，因为不发送外部或内部推送；不是 Candidate，因为不生成或排序候选；不是 Decision generation，因为只读取既有 DecisionResult/display path；不是 Point，因为不输出数值点位；不是 P359/P360，因为不新增 runtime candidate wrapper 或 point proposal family。
