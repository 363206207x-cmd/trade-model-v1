# V1 Runtime Readiness / System Guardrail Status Implementation Readiness Gate

## 1. Current Main

- Current merged main: `f6cc925 docs(runtime): design runtime readiness guardrail wiring`
- Current module: `Runtime readiness / system guardrail status`
- Current phase: `Implementation readiness gate`
- Current capability level: `REVIEW_ONLY_RUNTIME partial`
- Completed review-only runtime partial slices: `15`
- Risk: `A`
- Scope: readiness gate docs and source-of-truth updates only.

This package is a readiness decision only. It does not implement runtime readiness / system guardrail status, does not add or change endpoints, does not change dashboard behavior, and does not touch Java business code, tests, schema/config/pom, DTO, Validator, Assembler, Orchestrator, service/domain/mapper/repository ownership, scheduler, collector, API client refresh, external refresh, recovery, repair, restart, auto-fix, executable readiness, trading authorization, Push, external channel, Candidate, Decision generation, Point, final direction, entry/stop/TP/RR, order/execution, auto-trading, Position Monitor execution, replay, recheck, P359, or P360.

## 2. Readiness Decision

Decision: `GO to B-risk minimal implementation`.

Reason:

- Source Read confirmed the existing `SystemController`, `SystemHealthService`, `RunBaselineService`, `RunBaselineVO`, `RuntimeMetricService`, `DashboardSummaryResponseVO`, and dashboard system surfaces are enough to build a small review-only status.
- Design selected the existing system health / run-baseline / runtime metric owner path and rejected any new service/domain/mapper/repository ownership family.
- The implementation can remain review-only by projecting existing read-only runtime status and explicit safety fields.
- All executable readiness, trading authorization, recovery/repair/restart/auto-fix, refresh, scheduler, collector, Candidate, Decision generation, Point, Push, order/execution, and trading semantics can be blocked up front.

The next implementation is B-risk because it may touch controller, dashboard template, and targeted tests, but the allowed scope is narrow and existing-owner-path only.

## 3. Owner Path Decision

Preferred owner path:

```text
SystemController
  -> GET /api/system/run-baseline
  -> RunBaselineService / RunBaselineServiceImpl
  -> SystemHealthService / SystemHealthServiceImpl
  -> RuntimeMetricService.snapshot()
  -> RunBaselineVO
```

Dashboard context owner path:

```text
DashboardController
  -> GET /api/dashboard/summary
  -> DashboardSummaryResponseVO.systemStatus
  -> DashboardSummaryResponseVO.systemHealth
  -> dashboard.html existing system status surfaces
```

`GET /api/system/health` decision:

- It may be used only as static liveness evidence.
- It must not be used as executable readiness.
- It must not be used as trading authorization.
- It must not be used as scheduler, refresh, repair, recovery, restart, auto-fix, order, execution, or auto-trading authorization.

Primary read-only runtime readiness source:

- `GET /api/system/run-baseline` can be the primary read-only runtime readiness owner path.
- It already aggregates system health, PositionSync availability, runtime metrics, alert/data-quality context, recheck counts, and hot-reset history.
- Sensitive recheck and hot-reset context must remain read-only status/history, never execution or repair behavior.

## 4. Dedicated Endpoint Decision

A dedicated endpoint is allowed only if the implementation needs a smaller dashboard-friendly status projection than the broad run-baseline response.

Allowed shape:

```text
GET /api/system/runtime-readiness-guardrail-status?windowMinutes=60
```

Constraints:

- At most one minimal read-only `Map<String,Object>` endpoint.
- Must live under `SystemController`.
- Must project existing `RunBaselineVO`, `SystemHealthService`, and `RuntimeMetricService.snapshot()` data only.
- Must not introduce DTO / Validator / Assembler / Orchestrator.
- Must not introduce service/domain/mapper/repository ownership.
- Must not call `RuntimeMetricService.recordDuration(...)` as new status behavior.
- Must not trigger scheduler, collector, API client refresh, external refresh, recovery, repair, restart, auto-fix, replay, recheck, Push, Candidate, Decision generation, Point, order, execution, auto-trading, or Position Monitor behavior.

## 5. Dashboard Panel Decision

A minimal dashboard guardrail panel is allowed only if it is strictly a status display.

Allowed DOM ids if needed:

- `runtimeReadinessGuardrailStatusPanel`
- `runtimeReadinessStatusValue`
- `systemGuardrailStatusValue`
- `runBaselineStatusValue`
- `runtimeMetricStatusValue`
- `runtimeReadinessBoundaryValue`
- `runtimeReadinessReasonValue`

Required copy:

- review-only
- manual review only
- fail-closed when uncertain
- not executable readiness
- not trading authorization
- not recovery / repair / restart / auto-fix
- not scheduler trigger
- not collector trigger
- not API client refresh
- not external refresh
- not Candidate
- not Decision generation
- not Point
- not final direction
- not entry / stop / TP / RR
- not order / execution / auto-trading
- not executable

Forbidden dashboard behavior:

- no buttons;
- no repair/restart/recovery controls;
- no scheduler trigger controls;
- no refresh controls;
- no Candidate, Point, trading, order, execution, or auto-trading entry points;
- no copy that says "authorized", "ready to execute", "ready to trade", or equivalent.

## 6. Allowed Implementation Files

If the next B-risk implementation proceeds, it may modify only:

- `src/main/java/org/example/trademodel/controller/SystemController.java`
  - only one minimal read-only `Map` endpoint if the implementation needs it;
  - must reuse `RunBaselineService`, `SystemHealthService`, and `RuntimeMetricService.snapshot()` owner data;
  - must not add ownership family or writable behavior.
- `src/main/resources/templates/dashboard.html`
  - only minimal runtime guardrail panel / copy / DOM if needed;
  - must not alter business dashboard behavior beyond read-only status visibility.
- `src/test/java/org/example/trademodel/controller/SystemControllerTest.java` or existing dashboard/system tests
  - must cover endpoint safety fields, fail-closed states, readiness/authorization boundary, and forbidden executable fields absent.
- Existing run-baseline / runtime metric tests
  - only tiny owner-path assertions if required;
  - no expanded business semantics.
- Implementation report docs.
- Source-of-truth docs.

No other Java, mapper, domain, service, repository, schema, config, pom, DTO, Validator, Assembler, or Orchestrator file is allowed by this gate.

## 7. Required Safety Fields

The future endpoint and/or dashboard panel must expose or display these safety fields/copy:

| Field | Required value |
|---|---:|
| `reviewOnly` | `true` |
| `manualReviewOnly` | `true` |
| `notExecutableReadiness` | `true` |
| `notTradingAuthorization` | `true` |
| `notRecoveryRepair` | `true` |
| `notRestartAction` | `true` |
| `notAutoFix` | `true` |
| `notSchedulerTrigger` | `true` |
| `notCollectorTrigger` | `true` |
| `notApiClientRefresh` | `true` |
| `notExternalRefresh` | `true` |
| `notCandidateSignal` | `true` |
| `notDecisionGeneration` | `true` |
| `notPointSignal` | `true` |
| `notFinalDirection` | `true` |
| `notEntryStopTpRr` | `true` |
| `notTradingSignal` | `true` |
| `notExecutable` | `true` |
| `displaySlotsAreCandidatePool` | `false` |

These fields are negative safety assertions. They are allowed and required; they must not be interpreted as positive trading or execution semantics.

## 8. Required Status Mapping

The next implementation must cover these statuses:

| Status | Meaning | Fail-closed rule |
|---|---|---|
| `RUNTIME_READINESS_REVIEW_ONLY_READY` | Runtime readiness status is readable for manual review only. | Downstream action remains closed. |
| `RUNTIME_READINESS_BACKEND_PENDING_FAIL_CLOSED` | Runtime readiness backend cannot answer safely yet. | Yes |
| `RUNTIME_READINESS_MISSING_FAIL_CLOSED` | Required readiness inputs are missing. | Yes |
| `RUNTIME_READINESS_PARTIAL_REVIEW_ONLY` | Runtime readiness is partially readable. | Yes for downstream action |
| `SYSTEM_GUARDRAIL_REVIEW_ONLY_READY` | Guardrail status is readable for manual review only. | Downstream action remains closed. |
| `SYSTEM_GUARDRAIL_DEGRADED_REVIEW_ONLY` | Guardrail state is degraded but readable. | Yes for downstream action |
| `SYSTEM_GUARDRAIL_BLOCKED_FAIL_CLOSED` | Guardrail state is blocked or unsafe. | Yes |
| `RUN_BASELINE_REVIEW_ONLY_READY` | Run-baseline read model is readable. | Downstream action remains closed. |
| `RUN_BASELINE_MISSING_FAIL_CLOSED` | Run-baseline is missing. | Yes |
| `RUNTIME_METRIC_REVIEW_ONLY_READY` | Runtime metric snapshot is readable. | Downstream action remains closed. |
| `RUNTIME_METRIC_MISSING_FAIL_CLOSED` | Runtime metric snapshot is missing or ambiguous. | Yes |
| `EXECUTABLE_READINESS_BOUNDARY_BLOCKED_FAIL_CLOSED` | Executable readiness boundary is blocked. | Yes |
| `TRADING_AUTHORIZATION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Trading authorization boundary is blocked. | Yes |
| `RECOVERY_REPAIR_BOUNDARY_BLOCKED_FAIL_CLOSED` | Recovery / repair boundary is blocked. | Yes |
| `SCHEDULER_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED` | Scheduler trigger boundary is blocked. | Yes |
| `COLLECTOR_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED` | Collector trigger boundary is blocked. | Yes |
| `API_CLIENT_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED` | API client refresh boundary is blocked. | Yes |
| `EXTERNAL_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED` | External refresh boundary is blocked. | Yes |
| `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Candidate boundary is blocked. | Yes |
| `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED` | Point / final direction / entry-stop-TP-RR boundary is blocked. | Yes |
| `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED` | Trading boundary is blocked. | Yes |

Precedence:

1. executable readiness or trading authorization boundary blocked;
2. recovery / repair / restart / auto-fix boundary blocked;
3. scheduler / collector / API client / external refresh boundary blocked;
4. Candidate / Decision generation / Point / final direction / entry-stop-TP-RR / trading boundary blocked;
5. missing or unsafe run-baseline / system health / runtime metric source;
6. degraded guardrail state;
7. partial review-only state;
8. review-only ready.

## 9. Required Tests For B-Risk Implementation

The next implementation must include targeted tests that prove:

- endpoint exists if a dedicated endpoint is added;
- endpoint uses existing read-only owner path only;
- endpoint safety fields are present and correct;
- `/api/system/health` is not treated as executable readiness;
- run-baseline missing maps to `RUN_BASELINE_MISSING_FAIL_CLOSED`;
- runtime metric missing maps to `RUNTIME_METRIC_MISSING_FAIL_CLOSED`;
- backend exception maps to `RUNTIME_READINESS_BACKEND_PENDING_FAIL_CLOSED`;
- degraded system guardrail maps to review-only/fail-closed downstream behavior;
- executable readiness and trading authorization boundaries are blocked;
- recovery / repair / restart / auto-fix boundaries are blocked;
- scheduler / collector / API client / external refresh boundaries are blocked;
- Candidate / Decision generation / Point / final direction / entry-stop-TP-RR / trading boundaries are blocked;
- forbidden executable fields are absent;
- dashboard panel DOM and safety copy exist if the panel is added;
- no schema/config/pom, DTO/Validator/Assembler/Orchestrator, or new owner family is touched.

## 10. Forbidden Files And Behavior

The next implementation must not modify:

- `pom.xml`
- schema or db migration files
- config files
- new DTO classes
- new Validator classes
- new Assembler classes
- new Orchestrator classes
- new service/domain/mapper/repository ownership family

The next implementation must not create or trigger:

- executable readiness;
- trading authorization;
- recovery / repair / restart / auto-fix;
- scheduler trigger;
- collector trigger;
- API client refresh;
- external refresh;
- Candidate generation;
- Decision generation;
- Point generation;
- final direction;
- entry / stop / TP / RR;
- Push send;
- external channel;
- order / execution / auto-trading;
- Position Monitor execution;
- replay / recheck execution;
- missed-opportunity generation/write behavior;
- review result generation;
- paper order;
- simulated execution;
- paper PnL;
- P359 / P360.

## 11. NO-GO Conditions

The next implementation must stop with `NO-GO` if it requires any of the following:

- executable readiness;
- trading authorization;
- recovery / repair / restart / auto-fix;
- scheduler trigger;
- collector trigger;
- API client refresh;
- external refresh;
- Candidate generation;
- Decision generation;
- Point generation;
- final direction;
- entry / stop / TP / RR;
- order / execution / auto-trading;
- Position Monitor execution;
- replay / recheck execution;
- Push send / external channel;
- schema/config/pom;
- new DTO / Validator / Assembler / Orchestrator;
- new service/domain/mapper/repository ownership family;
- treating readiness as executable authorization instead of operational guardrail status.

## 12. Readiness / Authorization Boundary

Allowed interpretation:

- "Runtime readiness" means review-only operational status visibility.
- "System guardrail" means manual-review-only system status and fail-closed boundary state.
- "READY" means the read model is readable, not that any action is authorized.
- "DEGRADED" means status is visible but downstream action remains closed.
- "BLOCKED" means fail-closed and manual review required.

Forbidden interpretation:

- executable readiness;
- trading authorization;
- recovery/repair permission;
- restart/auto-fix permission;
- scheduler/collector/API refresh permission;
- Candidate/Decision/Point readiness;
- final direction / entry / stop / TP / RR readiness;
- order/execution/auto-trading readiness.

If code, field names, status names, or dashboard copy cannot preserve this boundary, implementation is `NO-GO`.

## 13. Next Allowed Action

Next allowed action:

`Minimal Review-Only Runtime Readiness / System Guardrail Status Runtime Wiring Implementation`

Next branch:

`minimal-review-only-runtime-readiness-system-guardrail-status-runtime-wiring-implementation`

Next risk:

`B`

The next package may implement only the allowed minimal review-only runtime wiring described above. It must create a Draft PR and must not auto-merge until B-risk review explicitly approves it.

## 14. Freeze-Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 资产: Yes, the gate reuses existing `SystemController`, `/api/system/run-baseline`, `SystemHealthService`, `RunBaselineService`, `RunBaselineVO`, `RuntimeMetricService.snapshot()`, `DashboardSummaryResponseVO`, and dashboard system surfaces.
- 是否减少重复: Yes, it blocks new DTO / Validator / Assembler / Orchestrator and new service/domain/mapper/repository owner families.
- 是否提升 capability level: No
- 是否接 service/runtime/dashboard/API: No implementation in this package; the next B-risk package may wire an existing service/runtime/dashboard/API review-only status path.
- 是否符合 #830 审计建议: Yes
