# V1 Paper Observation / Paper Trading Status Implementation Readiness Gate

## 1. Current Merged Main

- Current merged main: `0560ba2 docs(paper): design paper observation status runtime wiring`
- Current module: `Paper Observation / Paper Trading Status review-only status`
- Current phase: `Implementation readiness gate`
- Risk level: `A`
- Capability movement: none. The project remains `REVIEW_ONLY_RUNTIME partial`.
- Completed review-only runtime partial slices: 13.

This package is a readiness decision only. It does not implement a new endpoint, dashboard panel, paper order, simulated execution, paper PnL, real position monitoring, Position Monitor behavior, Candidate generation, Decision generation, Point generation, Push, external channel, order/execution, auto-trading, P359, or P360.

## 2. Readiness Decision

Readiness decision: `GO` to B-risk minimal implementation.

Reason:

- The source read confirmed a real owner path: `/api/dashboard/detail` -> `DashboardController` -> `PaperObservationDisplayAdapter` -> `DefaultPaperObservationDisplayAdapter` -> `DashboardDetailResponseVO.PaperObservationDisplayVO`.
- The design confirmed that `PaperObservationDisplayVO` already carries core review-only safety semantics: `notRealPosition`, `notTradeInstruction`, and `manualReviewRequired`.
- Existing adapter behavior already fails closed for missing DecisionResult, invalid PlanBoundary, ExecutionPlan not ready, and RiskActionGuard blocked.
- A minimal implementation can remain limited to projecting existing display owner data and adding explicit safety copy / tests.
- No new DTO / Validator / Assembler / Orchestrator, schema/config/pom, paper order, simulated execution, paper PnL, real position monitoring, Position Monitor execution, Candidate generation, Decision generation, Point generation, Push, external channel, order/execution, auto-trading, replay/recheck, P359, or P360 is needed.

## 3. Owner Path Decision

Preferred owner path remains:

```text
GET /api/dashboard/detail
  -> DashboardController
  -> DecisionResult read context
  -> PlanBoundaryDisplayVO
  -> ExecutionPlanDisplayVO
  -> RiskActionGuardDisplayVO
  -> PaperObservationDisplayAdapter
  -> DefaultPaperObservationDisplayAdapter
  -> DashboardDetailResponseVO.PaperObservationDisplayVO
  -> dashboard.html Paper Observation display area
```

Dedicated endpoint decision:

- Reuse `/api/dashboard/detail` first as the canonical source owner.
- A dedicated read-only status endpoint is allowed only if it is a thin projection over the same owner path and makes the dashboard/API boundary clearer.
- If added, it must be at most one minimal read-only `Map<String, Object>` endpoint in `DashboardController`.
- Suggested endpoint, only if needed: `GET /api/dashboard/paper-observation-status?symbol=BTCUSDT`.
- The endpoint must not call or create any paper execution, paper order, paper PnL, real position, Position Monitor, Candidate, Decision generation, Point, Push, external channel, order/execution, or auto-trading path.

Dashboard panel decision:

- Reuse the existing Paper Observation display area first.
- A minimal status panel is allowed only if it contains status / copy / DOM and does not change business logic.
- Suggested panel id, only if needed: `paperObservationStatusPanel`.
- Suggested DOM ids:
  - `paperObservationRuntimeStatusValue`
  - `paperObservationSafetyBoundaryValue`
  - `paperObservationCountsValue`
  - `paperObservationManualReviewValue`
  - `paperObservationExecutionBoundaryValue`
  - `paperObservationReasonValue`

## 4. Allowed Implementation Files

The next B-risk implementation may change only these files if needed:

- `src/main/java/org/example/trademodel/controller/DashboardController.java`
  - Only for one minimal read-only `Map` endpoint if the implementation uses a dedicated endpoint.
  - Must only project existing dashboard detail / PaperObservation display owner data.
- `src/main/resources/templates/dashboard.html`
  - Only for minimal status panel / copy / DOM.
  - Must display review-only, manual-review-only, not-real-position, not-trade-instruction, not-paper-order, not-simulated-execution, not-paper-PnL, not-entry-stop-TP-RR, not trading, and not executable safety copy.
- `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`
  - Required targeted endpoint/dashboard tests if an endpoint or panel is added.
- `src/test/java/org/example/trademodel/service/dashboard/DefaultPaperObservationDisplayAdapterTest.java`
  - Optional tiny existing-owner-path assertions only.
  - Must not expand business semantics.
- Implementation report docs.
- Source-of-truth docs.

## 5. Forbidden Files And Scope

The next implementation must not touch:

- schema/config/pom;
- new DTO / Validator / Assembler / Orchestrator;
- new service/domain/mapper/repository ownership family;
- paper trading execution;
- paper backtest execution;
- paper order;
- paper PnL generation;
- simulated execution;
- real position monitoring;
- Position Monitor execution;
- Candidate generation;
- Decision generation;
- Point generation;
- final direction;
- entry / stop / TP / RR;
- Push send;
- external channel;
- order / execution / auto-trading;
- replay / recheck;
- P359 / P360.

## 6. Required Safety Fields

If a dedicated endpoint is added, response fields must include:

| Field | Required value | Meaning |
|---|---:|---|
| `reviewOnly` | `true` | Review-only status. |
| `manualReviewOnly` | `true` | Human review is required. |
| `notRealPosition` | `true` | Not a real position. |
| `notTradeInstruction` | `true` | Not a trade instruction. |
| `notPaperOrder` | `true` | Not a paper order. |
| `notSimulatedExecution` | `true` | Does not run simulated execution. |
| `notPaperPnlGeneration` | `true` | Does not generate paper PnL. |
| `notPositionMonitorExecution` | `true` | Does not execute Position Monitor behavior. |
| `notCandidateSignal` | `true` | Not a Candidate signal. |
| `notDecisionGeneration` | `true` | Does not generate a Decision. |
| `notPointSignal` | `true` | Not Point generation. |
| `notFinalDirection` | `true` | Does not output final direction. |
| `notEntryStopTpRr` | `true` | Does not output entry / stop / TP / RR. |
| `notTradingSignal` | `true` | Not a trading signal. |
| `notExecutable` | `true` | Not executable. |
| `displaySlotsAreCandidatePool` | `false` | Display slots are not a Candidate pool. |

## 7. Required Status Mapping

The next implementation must cover these statuses:

- `PAPER_OBSERVATION_REVIEW_ONLY_READY`
- `PAPER_OBSERVATION_BACKEND_PENDING_FAIL_CLOSED`
- `PAPER_OBSERVATION_MISSING_FAIL_CLOSED`
- `PAPER_OBSERVATION_PARTIAL_REVIEW_ONLY`
- `NOT_REAL_POSITION_REVIEW_ONLY`
- `NOT_TRADE_INSTRUCTION_REVIEW_ONLY`
- `PAPER_ORDER_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `SIMULATED_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `PAPER_PNL_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `POSITION_MONITOR_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED`

The `READY` state means the observation display is readable for manual review. It does not mean paper trading, paper order, simulated execution, paper PnL, real position monitoring, Position Monitor execution, point generation, or trading readiness.

## 8. Required Tests

The next B-risk implementation must add or update targeted tests for:

- endpoint safety flags if a dedicated endpoint is added;
- fail-closed states for missing or backend-pending paper observation display;
- existing owner path reuse from dashboard detail / `PaperObservationDisplayAdapter`;
- paper execution boundary:
  - no paper order;
  - no simulated execution;
  - no paper PnL generation;
  - no real position monitoring;
  - no Position Monitor execution;
- forbidden executable fields absent:
  - no final direction;
  - no entry / stop / TP / RR;
  - no order / execution / auto-trading;
  - no Candidate generation;
  - no Decision generation;
  - no Point generation;
  - no Push send / external channel;
- dashboard panel / DOM / safety copy if a dashboard panel is added.

## 9. NO-GO Conditions

The next implementation must stop as `NO-GO` if it requires any of the following:

- paper order;
- simulated execution;
- paper PnL generation;
- real position monitoring;
- Position Monitor execution;
- entry / stop / TP / RR;
- final direction;
- Candidate generation;
- Decision generation;
- Point generation;
- order / execution / auto-trading;
- Push send / external channel;
- schema/config/pom;
- new DTO / Validator / Assembler / Orchestrator;
- a new service/domain/mapper/repository ownership family;
- treating Paper Trading as execution behavior rather than observation copy.

## 10. Paper Execution Boundary

`Paper Trading` may appear only as observation copy. It must never be interpreted as:

- paper order creation;
- simulated execution;
- paper backtest execution;
- generated paper trading result;
- paper PnL generation;
- real position monitoring;
- Position Monitor execution;
- final direction;
- entry / stop / TP / RR;
- order / execution / auto-trading.

Safe wording:

- `Paper Observation status is review-only.`
- `Paper Trading status means observation readiness only, not paper order execution.`
- `Manual review required; no entry / stop / TP / RR or trading instruction is generated.`

Unsafe wording:

- `paper order ready`;
- `simulate execution`;
- `paper PnL generated`;
- `open a paper position`;
- `entry/stop/TP/RR available`.

Any unsafe wording must map to a blocked fail-closed status.

## 11. Next Allowed Action

- Next allowed action: `Minimal Review-Only Paper Observation / Paper Trading Status Runtime Wiring Implementation`
- Next branch: `minimal-review-only-paper-observation-paper-trading-status-runtime-wiring-implementation`
- Next risk: `B`
- Allowed next changes: minimal review-only endpoint/dashboard/status implementation if needed, targeted tests, implementation docs, and source-of-truth updates only.

The next package must create a Draft PR and stop for B-risk review. It must not auto-merge.

## 12. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 assets: Yes, the gate reuses the dashboard detail / PaperObservation display adapter owner path.
- 是否减少重复: Yes, it forbids new DTO / Validator / Assembler / Orchestrator and keeps `PaperObservationDisplayVO` as the status source.
- 是否提升 capability level: No.
- 是否接 service/runtime/dashboard/API: No implementation in this package; it authorizes a narrow future review-only dashboard/API implementation gate.
- 是否符合 #830 审计建议: Yes.
