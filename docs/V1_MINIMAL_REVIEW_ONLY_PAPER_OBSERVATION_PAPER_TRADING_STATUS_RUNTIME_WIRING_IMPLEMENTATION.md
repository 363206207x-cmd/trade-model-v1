# Minimal Review-Only Paper Observation / Paper Trading Status Runtime Wiring Implementation

## 1. Executive Summary

This B-risk implementation adds a minimal review-only status surface for `Paper Observation / Paper Trading Status review-only status`.

Implemented surface:

- Endpoint: `GET /api/dashboard/paper-observation-status?symbol=BTCUSDT`
- Dashboard panel: `paperObservationStatusPanel`
- Owner path: existing dashboard detail / `PaperObservationDisplayAdapter` owner path
- Capability movement: none, still `REVIEW_ONLY_RUNTIME partial`

The endpoint is read-only. It reuses the existing dashboard detail owner path and existing `DashboardDetailResponseVO.PaperObservationDisplayVO` produced by `PaperObservationDisplayAdapter`. It does not create paper orders, run simulated execution, generate paper PnL, perform real position monitoring, execute Position Monitor behavior, generate Candidate / Decision generation / Point, output final direction / entry / stop / TP / RR, send Push, call an external channel, place order / execution / auto-trading actions, or continue P359/P360.

## 2. Reused Existing Assets

| Asset | Reused? | Notes |
|---|---:|---|
| `DashboardController` | Yes | Adds one thin read-only Map endpoint. |
| `/api/dashboard/detail` owner path | Yes | The status endpoint calls the existing detail path and derives status from its read model. |
| `PaperObservationDisplayAdapter` | Yes | Existing adapter remains the Paper Observation owner boundary. |
| `DefaultPaperObservationDisplayAdapter` | Yes | Existing fail-closed/default/manual-review behavior remains unchanged. |
| `DashboardDetailResponseVO.PaperObservationDisplayVO` | Yes | Provides the read-only status source fields. |
| `dashboard.html` | Yes | Adds one minimal status panel and safety copy. |
| `DashboardControllerTest` | Yes | Adds targeted endpoint/template assertions. |

No DTO, Validator, Assembler, Orchestrator, schema, config, pom, service, domain, mapper, or repository ownership family was added.

## 3. Implemented Endpoint

| Endpoint | Method | Purpose | Paper order? | Simulated execution? | Trading semantics? |
|---|---|---|---:|---:|---:|
| `/api/dashboard/paper-observation-status?symbol=BTCUSDT` | GET | Review-only Paper Observation / Paper Trading status | No | No | No |

Allowed response fields include status, symbol, analysis id, Paper Observation owner status, linked observation/review counts, backend status, review summary, safety fields, fail-closed, and boundary statuses.

Forbidden response fields are not exposed: Candidate ranking, final direction, entry, stop, TP, RR, paper order action, simulated execution action, paper PnL, real position action, Position Monitor action, order action, execution action, Push send state, or auto-trading action.

## 4. Dashboard Panel

Dashboard panel:

- `paperObservationStatusPanel`

DOM ids:

- `paperObservationRuntimeStatusValue`
- `paperObservationSymbolValue`
- `paperObservationAnalysisIdValue`
- `paperObservationStatusValue`
- `paperObservationBackendValue`
- `paperObservationCountsValue`
- `paperObservationManualReviewValue`
- `paperObservationFailClosedValue`
- `paperObservationReviewSummaryValue`
- `paperObservationReviewOnlyValue`
- `paperObservationExecutionBoundaryValue`
- `paperObservationSignalBoundaryValue`
- `paperObservationReasonValue`

The panel is a status surface only. It displays review-only, manual-review-only, fail-closed, not real position, not trade instruction, not paper order, not simulated execution, not paper PnL generation, not Position Monitor execution, not Candidate, not Decision generation, not Point, not final direction, not entry / stop / TP / RR, not trading, not executable, and Display Slots not candidate-pool copy.

## 5. Status Mapping

Implemented statuses:

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

Fail-closed rules:

- Missing DecisionResult fails closed.
- Missing Paper Observation display owner data fails closed.
- Missing `notRealPosition=true` fails closed.
- Missing `notTradeInstruction=true` fails closed.
- Any owner-path exposure of paper observation availability or manual review surface availability fails closed.
- Incomplete owner-path safety flags fail closed as partial review-only.
- Backend-pending owner state fails closed.

Review-only ready rule:

- `paperObservationStatus=MANUAL_REVIEW_REQUIRED`
- `reviewSummary=AVAILABLE_REVIEW_ONLY`
- owner safety flags are all safe
- no paper observation entry is available
- no manual review surface is available

## 6. Safety Fields

The endpoint returns:

- `reviewOnly=true`
- `manualReviewOnly=true`
- `notRealPosition=true`
- `notTradeInstruction=true`
- `notPaperOrder=true`
- `notSimulatedExecution=true`
- `notPaperPnlGeneration=true`
- `notPositionMonitorExecution=true`
- `notCandidateSignal=true`
- `notDecisionGeneration=true`
- `notPointSignal=true`
- `notFinalDirection=true`
- `notEntryStopTpRr=true`
- `notTradingSignal=true`
- `notExecutable=true`
- `displaySlotsAreCandidatePool=false`

## 7. Tests

Targeted tests were added to `DashboardControllerTest`:

- Dashboard template contains `paperObservationStatusPanel`, required DOM ids, status constants, and safety copy.
- Endpoint returns review-only ready status and required safety fields.
- Missing DecisionResult fails closed.
- Unsafe owner-path paper execution availability fails closed.
- Forbidden executable, paper order, simulated execution, paper PnL, real position, Position Monitor, Candidate, final direction, entry / stop / TP / RR, Push, order, execution, and auto-trading fields are absent.

Checks run during implementation:

- `./mvnw -q -DskipTests compile` PASS
- `./mvnw -q -DskipTests test-compile` PASS
- `./mvnw -q -Dtest=DashboardControllerTest test` PASS
- `./mvnw -q test` PASS

## 8. Forbidden Scope Confirmation

This package did not add:

- DTO / Validator / Assembler / Orchestrator
- service / domain / mapper / repository ownership family
- schema/config/pom changes
- paper order
- simulated execution
- paper PnL generation
- real position monitoring
- Position Monitor execution
- Candidate generation
- Decision generation
- Point generation
- final direction
- entry / stop / TP / RR
- Push send / external channel
- order / execution / auto-trading
- replay / recheck
- P359 / P360

## 9. Current Mainline

- Current Mainline: `3a281e4 docs(paper): verify paper observation implementation readiness`
- Current Block: `Minimal Review-Only Paper Observation / Paper Trading Status Runtime Wiring Implementation`
- Capability Movement: none
- User-visible Output: one review-only endpoint and one dashboard status panel
- Overreach Boundary: no paper order, simulated execution, paper PnL, real position monitoring, Position Monitor execution, Candidate / Decision generation / Point, final direction / entry / stop / TP / RR, Push, external channel, order / execution / auto-trading, DTO / Validator / Assembler / Orchestrator, schema/config/pom, P359, or P360

## 10. Next Allowed Action

Next allowed action:

`Minimal Review-Only Paper Observation / Paper Trading Status Runtime Wiring Verification`

The next package is A-risk verification docs/source-of-truth only after this B-risk implementation PR is reviewed and merged.
