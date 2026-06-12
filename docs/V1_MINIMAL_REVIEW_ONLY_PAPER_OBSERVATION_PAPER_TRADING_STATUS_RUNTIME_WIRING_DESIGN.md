# V1 Minimal Review-Only Paper Observation / Paper Trading Status Runtime Wiring Design

## 1. Executive Summary

- Current merged main: `1625b52 docs(paper): read paper observation status source path`
- Current module: `Paper Observation / Paper Trading Status review-only status`
- Current phase: `Design`
- Design result: `GO` to `Implementation readiness gate for Paper Observation / Paper Trading Status review-only status`
- Capability movement: none. Current level remains `REVIEW_ONLY_RUNTIME partial`.

This design keeps Paper Observation / Paper Trading as a review-only observation status. It may show whether the existing paper-observation display state is present, partial, blocked, or manual-review-only, but it must never create paper orders, simulated executions, paper PnL, real positions, Position Monitor actions, candidate signals, point signals, or trading instructions.

Preferred owner path:

```text
DashboardController /api/dashboard/detail
  -> DecisionResult
  -> PlanBoundaryDisplayVO
  -> ExecutionPlanDisplayVO
  -> RiskActionGuardDisplayVO
  -> PaperObservationDisplayAdapter
  -> DefaultPaperObservationDisplayAdapter
  -> DashboardDetailResponseVO.PaperObservationDisplayVO
  -> dashboard.html Paper Observation display area
```

The next readiness gate must first decide whether the existing `/api/dashboard/detail` payload and dashboard paper observation display area are sufficient. A dedicated status endpoint is allowed only if it remains smaller and clearer than reusing dashboard detail directly; if approved, it must be one minimal read-only `Map` endpoint over the existing display owner path.

This design does not add Java business code, tests, dashboard behavior, schema/config/pom, DTO / Validator / Assembler / Orchestrator, paper order, simulated execution, paper PnL, real position monitoring, Position Monitor execution, Candidate generation, Decision generation, Point generation, Push send, external channel, order/execution, auto-trading, replay/recheck, P359, or P360.

## 2. Existing Assets Used By Design

| Asset | Existing role | Design use | Boundary |
|---|---|---|---|
| `PaperObservationDisplayAdapter` | Read-only adapter contract. | Canonical adapter owner for paper observation display state. | Must not create real positions or trading instructions. |
| `DefaultPaperObservationDisplayAdapter` | Fail-closed adapter implementation. | Reuse upstream DecisionResult / PlanBoundary / ExecutionPlan / RiskActionGuard checks and safety flags. | No paper order, no simulated execution, no paper PnL. |
| `DashboardDetailResponseVO.PaperObservationDisplayVO` | Existing nested display VO. | Primary status source; no new DTO family by default. | Counts and flags are display metadata, not executable state. |
| `DashboardController.dashboardDetail` | Existing `/api/dashboard/detail` owner path. | Preferred API read source. | No new generation or write path. |
| `dashboard.html` Paper Observation display | Existing dashboard rendering area. | Preferred visual owner path; readiness decides whether a minimal status panel is needed. | No dashboard trading business logic. |
| Existing adapter / VO / controller tests | Existing safety coverage. | Future readiness gate must require targeted coverage for status mapping and safety fields. | Design package does not edit tests. |
| Historical smoke docs | Existing API/display evidence. | Confirms paper observation display already appears in dashboard detail output. | Historical evidence only; not implementation in this package. |

## 3. Owner Path And Endpoint Decision

Default future implementation path:

```text
GET /api/dashboard/detail
  -> DashboardController
  -> safe default DashboardDetailResponseVO
  -> DecisionResult read context
  -> PlanBoundaryDisplayVO
  -> ExecutionPlanDisplayVO
  -> RiskActionGuardDisplayVO
  -> paperObservationDisplayAdapter.build(...)
  -> PaperObservationDisplayVO
```

Dedicated endpoint decision:

- Preferred design: reuse `/api/dashboard/detail` as the canonical source owner.
- Dedicated endpoint is not implemented by this design package.
- A dedicated endpoint is allowed only if the readiness gate finds a compact runtime status rollup is needed for dashboard/API ergonomics.
- If allowed, endpoint must be a minimal read-only `Map<String, Object>` endpoint in the existing `DashboardController` owner path.
- It must project existing `PaperObservationDisplayVO` / dashboard-detail owner data only.
- It must not create DTO / Validator / Assembler / Orchestrator.
- It must not call paper trading execution, backtest execution, Position Monitor execution, order/execution, Push, external channel, replay/recheck, Candidate generation, Decision generation, or Point generation.

Suggested future endpoint candidate, only if readiness gate approves:

```text
GET /api/dashboard/paper-observation-status?symbol=BTCUSDT
```

Suggested future dashboard surface, only if readiness gate approves:

```text
paperObservationStatusPanel
```

Suggested future DOM ids:

- `paperObservationRuntimeStatusValue`
- `paperObservationSafetyBoundaryValue`
- `paperObservationCountsValue`
- `paperObservationManualReviewValue`
- `paperObservationExecutionBoundaryValue`
- `paperObservationReasonValue`

## 4. Read-Only Status Sources

| Source field | Existing owner | Allowed status meaning | Not allowed |
|---|---|---|---|
| `paperObservationStatus` | `PaperObservationDisplayVO` | Backend pending, manual review required, or readable review-only display status. | Paper order readiness. |
| `paperObservationStatusLabel` | `PaperObservationDisplayVO` | Human-readable display label. | Trading instruction label. |
| `paperObservationAvailable` | `PaperObservationDisplayVO` | Existing display availability flag; should remain non-executable. | Permission to create paper orders. |
| `manualReviewEntryAvailable` | `PaperObservationDisplayVO` | Existing flag that must remain false in the minimal status boundary. | Entry availability or executable entry. |
| `linkedPaperObservationCount` | `PaperObservationDisplayVO` | Read-only count metadata. | Paper PnL or generated paper result. |
| `linkedReviewCount` | `PaperObservationDisplayVO` | Read-only review linkage metadata. | Review result generation. |
| `missedOpportunityFlag` | `PaperObservationDisplayVO` | Context-only flag from existing display. | Missed-opportunity generation/write behavior. |
| `reviewSummary` | `PaperObservationDisplayVO` | Fail-closed or manual-review reason. | New decision, final direction, or execution advice. |
| `notRealPosition` | `PaperObservationDisplayVO` | Must be true; confirms this is not a real position. | Real position monitoring. |
| `notTradeInstruction` | `PaperObservationDisplayVO` | Must be true; confirms this is not a trade instruction. | Trading signal. |
| `manualReviewRequired` | `PaperObservationDisplayVO` | Must be true; confirms human-review-only surface. | Automated action. |
| `backendConnectionStatus` | `PaperObservationDisplayVO` | Backend pending / source health display. | Runtime execution readiness. |
| `updatedAt` | `PaperObservationDisplayVO` | Optional display freshness. | Triggering refresh. |

## 5. Status Mapping

| Status | Trigger condition | Review-only meaning | Fail-closed? |
|---|---|---|---:|
| `PAPER_OBSERVATION_REVIEW_ONLY_READY` | `PaperObservationDisplayVO` is present, upstream DecisionResult / PlanBoundary / ExecutionPlan / RiskActionGuard context is readable, `manualReviewRequired=true`, `notRealPosition=true`, `notTradeInstruction=true`, and no execution boundary is crossed. | Paper observation status is readable for manual review only. | No for display; downstream actions remain closed. |
| `PAPER_OBSERVATION_BACKEND_PENDING_FAIL_CLOSED` | `paperObservationStatus` is blank or `BACKEND_PENDING`, or backend status is ambiguous. | Backend state is not wired enough to trust; keep closed. | Yes |
| `PAPER_OBSERVATION_MISSING_FAIL_CLOSED` | `paperObservationDisplay` is null or unavailable. | No paper observation display source exists. | Yes |
| `PAPER_OBSERVATION_PARTIAL_REVIEW_ONLY` | Counts, summary, or freshness are partial while safety flags remain true. | Partial display metadata is visible, but it remains manual-review-only. | No |
| `NOT_REAL_POSITION_REVIEW_ONLY` | `notRealPosition=true`. | Explicitly not a real position. | No |
| `NOT_TRADE_INSTRUCTION_REVIEW_ONLY` | `notTradeInstruction=true`. | Explicitly not a trade instruction. | No |
| `PAPER_ORDER_BOUNDARY_BLOCKED_FAIL_CLOSED` | Status would need to create, expose, or imply paper order state. | Paper order boundary is blocked. | Yes |
| `SIMULATED_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Status would need simulated execution or backtest execution. | Simulated execution boundary is blocked. | Yes |
| `PAPER_PNL_BOUNDARY_BLOCKED_FAIL_CLOSED` | Status would need paper PnL generation or generated paper trading result. | Paper PnL boundary is blocked. | Yes |
| `POSITION_MONITOR_BOUNDARY_BLOCKED_FAIL_CLOSED` | Status would need real position monitoring or Position Monitor execution. | Position Monitor boundary is blocked. | Yes |
| `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED` | Status would need point generation or point proposal output. | Point boundary is blocked. | Yes |
| `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED` | Status would need final direction, entry / stop / TP / RR, order/execution, or auto-trading. | Trading boundary is blocked. | Yes |

Status precedence:

1. `PAPER_ORDER_BOUNDARY_BLOCKED_FAIL_CLOSED`
2. `SIMULATED_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED`
3. `PAPER_PNL_BOUNDARY_BLOCKED_FAIL_CLOSED`
4. `POSITION_MONITOR_BOUNDARY_BLOCKED_FAIL_CLOSED`
5. `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED`
6. `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED`
7. `PAPER_OBSERVATION_MISSING_FAIL_CLOSED`
8. `PAPER_OBSERVATION_BACKEND_PENDING_FAIL_CLOSED`
9. `PAPER_OBSERVATION_PARTIAL_REVIEW_ONLY`
10. `PAPER_OBSERVATION_REVIEW_ONLY_READY`

`READY` means the observation display is readable. It does not mean paper trading, paper order, simulated execution, real position monitoring, or trading readiness.

## 6. Safety Fields

If a dedicated endpoint is later approved, it must expose the following safety fields:

| Field | Required value | Purpose |
|---|---:|---|
| `reviewOnly` | `true` | Status is for review only. |
| `manualReviewOnly` | `true` | Human review is required. |
| `notRealPosition` | `true` | Status is not a real position. |
| `notTradeInstruction` | `true` | Status is not a trade instruction. |
| `notPaperOrder` | `true` | Status does not create or imply paper orders. |
| `notSimulatedExecution` | `true` | Status does not run simulated execution. |
| `notPaperPnlGeneration` | `true` | Status does not generate paper PnL. |
| `notPositionMonitorExecution` | `true` | Status does not execute Position Monitor behavior. |
| `notCandidateSignal` | `true` | Status is not a candidate signal. |
| `notDecisionGeneration` | `true` | Status does not generate decisions. |
| `notPointSignal` | `true` | Status is not point generation. |
| `notFinalDirection` | `true` | Status does not output final direction. |
| `notEntryStopTpRr` | `true` | Status does not output entry / stop / TP / RR. |
| `notTradingSignal` | `true` | Status is not a trading signal. |
| `notExecutable` | `true` | Status cannot be executed. |
| `displaySlotsAreCandidatePool` | `false` | Display slots remain display-only. |

Suggested supporting fields:

- `status`
- `symbol`
- `paperObservationStatus`
- `paperObservationStatusLabel`
- `linkedPaperObservationCount`
- `linkedReviewCount`
- `missedOpportunityFlag`
- `reviewSummary`
- `backendConnectionStatus`
- `sourceHealth`
- `reason`
- `message`
- `updatedAt`
- `failClosed`

Forbidden response fields:

- paper order action;
- simulated execution action;
- paper PnL;
- generated paper trading result;
- real position state;
- Position Monitor action;
- candidate ranking;
- generated decision;
- point proposal;
- final direction;
- entry;
- stop;
- TP;
- RR;
- order action;
- execution action;
- auto-trading action;
- Push send state;
- external channel state.

## 7. Paper Execution Boundary

The term `Paper Trading` is allowed only as a user-facing label for observation status. It must not imply:

- paper order creation;
- simulated execution;
- backtest execution;
- generated paper trading result;
- paper PnL generation;
- real position monitoring;
- Position Monitor execution;
- final direction;
- entry / stop / TP / RR;
- order / execution / auto-trading.

Safe wording examples:

- "Paper Observation is review-only and not a real position."
- "Paper Trading status means observation readiness only, not paper order execution."
- "Manual review required; no entry / stop / TP / RR or trading instruction is generated."

Unsafe wording examples:

- "paper order ready"
- "simulate execution now"
- "paper PnL generated"
- "open a paper position"
- "entry/stop/TP/RR available"

If copy or field naming cannot keep this boundary clear, future implementation must fail closed with `PAPER_ORDER_BOUNDARY_BLOCKED_FAIL_CLOSED`, `SIMULATED_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED`, or `PAPER_PNL_BOUNDARY_BLOCKED_FAIL_CLOSED`.

## 8. Fail-Closed Rules

Future status must fail closed when:

- `paperObservationDisplay` is missing.
- `paperObservationStatus` is blank or `BACKEND_PENDING`.
- `backendConnectionStatus` is missing or ambiguous.
- DecisionResult context is missing.
- PlanBoundary is not valid.
- ExecutionPlan is not review-only ready.
- RiskActionGuard is blocked.
- `notRealPosition` is not true.
- `notTradeInstruction` is not true.
- `manualReviewRequired` is not true.
- `manualReviewEntryAvailable` becomes true in a way that can be read as executable entry.
- Any status would require paper order, simulated execution, paper PnL, real position monitoring, Position Monitor execution, Candidate generation, Decision generation, Point generation, Push, external channel, final direction, entry / stop / TP / RR, order/execution, auto-trading, replay/recheck, schema/config/pom, or a new DTO / Validator / Assembler / Orchestrator.

Fail-closed still permits showing a visible status label. It only keeps every downstream action implication closed.

## 9. Dashboard / API Surface

Future readiness gate should choose between:

1. **Reuse `/api/dashboard/detail` only**: keep status inside existing Paper Observation display area and strengthen dashboard copy if needed.
2. **Add one minimal read-only Map endpoint and one minimal panel**: allowed only if the readiness gate proves a compact status panel makes the review-only boundary clearer.

If option 2 is approved, future dashboard surface may include:

- `paperObservationStatusPanel`
- `paperObservationRuntimeStatusValue`
- `paperObservationSafetyBoundaryValue`
- `paperObservationCountsValue`
- `paperObservationManualReviewValue`
- `paperObservationExecutionBoundaryValue`
- `paperObservationReasonValue`

Dashboard copy must state:

- review-only;
- manual review only;
- not real position;
- not trade instruction;
- not paper order;
- not simulated execution;
- not paper PnL generation;
- not Position Monitor execution;
- not Candidate;
- not Decision generation;
- not Point;
- not final direction;
- not entry / stop / TP / RR;
- not trading;
- not executable.

Dashboard must not add buttons, controls, or copy for paper order creation, simulated execution, real position monitoring, Position Monitor execution, trade actions, or point/trade proposal output.

## 10. Implementation Readiness Gate Checklist

The next readiness gate must verify:

- whether `/api/dashboard/detail` alone satisfies the minimal runtime status need;
- whether a dedicated endpoint is necessary and still a single thin read-only `Map`;
- whether `DashboardController` can build the status from the existing dashboard-detail owner path;
- whether `PaperObservationDisplayVO` is sufficient without a new DTO;
- whether `dashboard.html` needs only minimal copy / DOM / panel changes;
- exact DOM ids and copy if a panel is needed;
- exact status mapping and precedence;
- targeted tests needed for endpoint or dashboard behavior;
- coverage for safety fields and forbidden executable fields absent;
- no DTO / Validator / Assembler / Orchestrator;
- no schema/config/pom;
- no paper order, simulated execution, paper PnL, real position monitoring, or Position Monitor execution;
- no Candidate generation, Decision generation, Point generation, final direction, entry / stop / TP / RR, Push, external channel, order/execution, auto-trading, replay/recheck, P359, or P360.

## 11. Allowed Future Implementation Files

If the readiness gate returns GO, future B-risk implementation may consider only:

- `src/main/java/org/example/trademodel/controller/DashboardController.java` for one minimal read-only `Map` endpoint if needed;
- `src/main/resources/templates/dashboard.html` for minimal panel / copy / DOM if needed;
- `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java` for targeted endpoint/dashboard tests;
- `src/test/java/org/example/trademodel/service/dashboard/DefaultPaperObservationDisplayAdapterTest.java` only for tiny existing-adapter assertions if needed;
- implementation report docs;
- source-of-truth docs.

Future implementation must not touch:

- schema / config / pom;
- new DTO / Validator / Assembler / Orchestrator;
- service/domain ownership family;
- mapper/schema ownership;
- paper trading execution;
- paper backtest execution;
- paper order;
- paper PnL generation;
- simulated execution;
- real position monitoring;
- Position Monitor execution;
- Push / external channel;
- Candidate generation;
- Decision generation;
- Point generation;
- final direction / entry / stop / TP / RR;
- order / execution / auto-trading;
- replay / recheck;
- P359 / P360.

## 12. Next Task

- Next allowed action: `Implementation readiness gate for Paper Observation / Paper Trading Status review-only status`
- Next branch: `paper-observation-paper-trading-status-implementation-readiness-gate`
- PR risk: `A`
- Allowed changes: readiness-gate docs and source-of-truth updates only.

The later implementation, if the readiness gate returns GO, should be `B-risk` because it may touch Java, dashboard, and targeted tests.

## 13. Final Recommendation

Design result: `GO` to Implementation readiness gate.

Reason:

- The source-read confirmed a real owner path already exists.
- `PaperObservationDisplayVO` already carries the key safety semantics: `notRealPosition`, `notTradeInstruction`, and `manualReviewRequired`.
- `DefaultPaperObservationDisplayAdapter` already fails closed on missing DecisionResult, invalid PlanBoundary, ExecutionPlan not ready, and RiskActionGuard blocked.
- The future design can reuse `/api/dashboard/detail` and the existing dashboard display before considering a thin endpoint.
- No new DTO / Validator / Assembler / Orchestrator, schema/config/pom, paper order, simulated execution, paper PnL, Position Monitor execution, Candidate generation, Decision generation, Point generation, Push, external channel, or trading path is needed.

## 14. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 assets: Yes, it anchors on the existing dashboard detail / PaperObservation display adapter owner path.
- 是否减少重复: Yes, by keeping future work on `PaperObservationDisplayVO` instead of creating a new paper-trading object family.
- 是否提升 capability level: No, design only.
- 是否接 service/runtime/dashboard/API: No implementation; design selects the existing dashboard/API owner path.
- 是否符合 #830 审计建议: Yes.
