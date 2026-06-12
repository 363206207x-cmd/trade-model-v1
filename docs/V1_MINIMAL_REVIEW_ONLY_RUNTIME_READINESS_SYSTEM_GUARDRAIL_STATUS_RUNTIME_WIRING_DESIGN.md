# V1 Minimal Review-Only Runtime Readiness / System Guardrail Status Runtime Wiring Design

## 1. Executive Summary

- Current merged main: `c5aba1a docs(runtime): read runtime readiness guardrail source path`
- Current module: `Runtime readiness / system guardrail status`
- Current phase: `Design`
- Completed review-only runtime partial slices: `15`
- Current capability level: `REVIEW_ONLY_RUNTIME partial`
- Capability movement: none.

This package is design only. It does not implement Java business code, tests, dashboard business logic, schema/config/pom changes, endpoint behavior, dashboard panel behavior, scheduler trigger, collector trigger, API client refresh, external refresh, recovery, repair, restart, auto-fix, executable readiness, trading authorization, Push send, external channel, Candidate generation, Decision generation, Point generation, final direction, entry/stop/TP/RR, order/execution, auto-trading, DTO, Validator, Assembler, Orchestrator, new service/domain/mapper/repository ownership family, replay, recheck, P359, or P360.

Design result: **GO to implementation readiness gate only**.

The future minimal runtime wiring should primarily reuse the existing owner path:

```text
SystemController
  -> GET /api/system/run-baseline
  -> RunBaselineService / RunBaselineServiceImpl
  -> SystemHealthService / SystemHealthServiceImpl
  -> RuntimeMetricService.snapshot()
  -> RunBaselineVO
  -> DashboardSummaryResponseVO.systemStatus / systemHealth
  -> dashboard.html existing system status surfaces
```

`GET /api/system/health` may be referenced only as static liveness evidence. It must not be treated as executable readiness, trading authorization, scheduler authorization, repair authorization, or production readiness.

## 2. Design Answers

| Question | Design answer |
|---|---|
| Reuse existing system health / run-baseline / runtime metric owner path? | **Yes.** `SystemController`, `SystemHealthService`, `RunBaselineService`, `RunBaselineVO`, `RuntimeMetricService.snapshot()`, and dashboard summary/system surfaces are the canonical source path. |
| Need a dedicated runtime readiness / system guardrail endpoint? | **Default no.** Start from existing `/api/system/run-baseline` and dashboard summary/system health data. A dedicated endpoint is allowed only if the readiness gate proves a smaller status rollup is clearer than broad run-baseline output. |
| If dedicated endpoint is needed, what shape is allowed? | At most one minimal read-only `Map<String,Object>` endpoint under the existing `SystemController` owner path, for example `GET /api/system/runtime-readiness-guardrail-status`. It must project existing read-only owner data only. |
| Can `/api/system/health` be used as readiness? | **No.** It is static liveness text only. It can support a liveness indicator, but it cannot imply executable readiness or trading authorization. |
| Can `/api/system/run-baseline` be used as run-baseline read model? | **Yes.** It is the preferred aggregate source because it already carries system health, PositionSync availability, runtime metrics, alert/data-quality summaries, recheck counts, and hot reset context. |
| Which `RuntimeMetricService` fields are safe? | `snapshot()` output only: metric name, `lastDurationMs`, `avgDurationMs`, `sampleCount`, plus run-baseline `hasSamples`, `totalSampleCount`, and `sampleBoundaryDetail`. `recordDuration(...)` is not part of this future status path. |
| Reuse dashboard system surfaces or add guardrail panel? | Reuse current runtime pill, sidebar system card, KPI cards, and helper copy first. A minimal `runtimeReadinessGuardrailStatusPanel` is allowed only if readiness gate finds existing surfaces too scattered for explicit safety fields. |
| How to mark not executable readiness? | Require `notExecutableReadiness=true`, `notExecutable=true`, and copy that `READY` means manual review of operational status only. |
| How to mark not trading authorization? | Require `notTradingAuthorization=true`, `notTradingSignal=true`, `notFinalDirection=true`, and `notEntryStopTpRr=true`; no authorization language may be used. |
| How to mark not recovery / repair / restart / auto-fix? | Require `notRecoveryRepair=true`, `notRestartAction=true`, and `notAutoFix=true`; hot reset and health context remain historical/status-only. |
| How to mark not scheduler / collector / API refresh / external refresh? | Require `notSchedulerTrigger=true`, `notCollectorTrigger=true`, `notApiClientRefresh=true`, and `notExternalRefresh=true`; scheduler status is observation only. |
| How to mark not Candidate / Decision / Point? | Require `notCandidateSignal=true`, `notDecisionGeneration=true`, and `notPointSignal=true`; readiness/guardrail data cannot be scored or ranked as opportunity signals. |
| How to mark not final direction / entry / stop / TP / RR? | Require `notFinalDirection=true` and `notEntryStopTpRr=true`; status may not expose trade plan fields. |
| How to mark not order / execution / auto-trading? | Require `notExecutable=true` and explicit dashboard copy that no order, execution, or auto-trading action is present. |
| How to keep readiness from becoming authorization? | Use `reviewOnly=true`, `manualReviewOnly=true`, and "operational guardrail status" wording. Status values may describe readable/degraded/blocked/missing only, never "authorized", "allowed to trade", or "ready to execute". |
| Readiness gate focus? | Endpoint necessity, dashboard panel necessity, exact read-only source fields, safety fields, fail-closed precedence, targeted tests, and proof that no trigger/write/generation/authorization path is needed. |
| Future implementation max scope? | Existing `SystemController` read-only status projection if approved, `dashboard.html` minimal panel/copy/DOM if approved, targeted tests, implementation report, and source-of-truth docs. |
| Need DTO / Validator / Assembler / Orchestrator? | **No.** Existing VO and Map projections are sufficient for the minimal review-only design. |
| Need schema/config/pom? | **No.** Existing runtime health/baseline/metric read assets are enough. |
| Need new service/domain/mapper/repository owner family? | **No.** That would duplicate existing `SystemController` / `RunBaselineService` / `SystemHealthService` owners and violate the freeze rule. |

## 3. Owner Path

Preferred source owner path:

```text
GET /api/system/run-baseline
  -> SystemController.runBaseline(windowMinutes)
  -> RunBaselineService.getRunBaseline(windowMinutes)
  -> SystemHealthService.getSystemHealth()
  -> PositionSyncService.getPositionSyncStatus()
  -> RuntimeMetricService.snapshot()
  -> MonitorAlertMapper / AnalysisRunMapper / PushRecheckLogMapper / HotResetEventMapper read counts
  -> RunBaselineVO
```

Dashboard context owner path:

```text
GET /api/dashboard/summary
  -> DashboardController.summary()
  -> DashboardSummaryResponseVO.systemStatus
  -> DashboardSummaryResponseVO.systemHealth
  -> dashboard.html runtime pill / sidebar system status / KPI cards
```

Static liveness context:

```text
GET /api/system/health
  -> static ApiResponse<String> liveness text
```

Rejected owner paths:

```text
PositionSyncService.syncPositions()
RuntimeMetricService.recordDuration(...) as a new status behavior
any scheduler start/trigger path
any collector trigger path
any API client refresh path
any external refresh path
any recovery / repair / restart / auto-fix path
any trading authorization path
Candidate / Decision generation / Point generation paths
final direction / entry / stop / TP / RR paths
order / execution / auto-trading paths
new DTO / Validator / Assembler / Orchestrator
new service / domain / mapper / repository ownership family
```

## 4. Status Mapping

| Status | Trigger condition | Review-only meaning | Fail-closed? |
|---|---|---|---:|
| `RUNTIME_READINESS_REVIEW_ONLY_READY` | Run-baseline read model, system health, and metric snapshot are readable with no blocked boundary. | Operational readiness status is visible for manual review only. | No for display; yes for downstream action |
| `RUNTIME_READINESS_BACKEND_PENDING_FAIL_CLOSED` | System/run-baseline owner is unavailable, throws, or cannot answer safely. | Runtime readiness backend is pending; keep closed. | Yes |
| `RUNTIME_READINESS_MISSING_FAIL_CLOSED` | Required runtime readiness inputs are absent. | Readiness cannot be inferred. | Yes |
| `RUNTIME_READINESS_PARTIAL_REVIEW_ONLY` | Some sources are readable but metrics, PositionSync, alert/data-quality, recheck, or hot reset context is partial. | Partial operational status only. | Yes for downstream action |
| `SYSTEM_GUARDRAIL_REVIEW_ONLY_READY` | Database, scheduler observation, and run-baseline guardrail context are readable. | Guardrail state is visible for manual review. | No for display; yes for downstream action |
| `SYSTEM_GUARDRAIL_DEGRADED_REVIEW_ONLY` | Database `DOWN`/`ERROR`, scheduler `STALE`/`NO_RECENT_ACTIVITY`, stale PositionSync, high alert/data-quality risk, or missing metric samples are observed but readable. | Guardrail is degraded and review-only. | Yes for downstream action |
| `SYSTEM_GUARDRAIL_BLOCKED_FAIL_CLOSED` | Any required guardrail source is missing, unsafe, or only answerable by a forbidden action. | Guardrail blocked. | Yes |
| `RUN_BASELINE_REVIEW_ONLY_READY` | `RunBaselineVO` is present and generated for a safe window. | Run-baseline is readable. | No for display; yes for downstream action |
| `RUN_BASELINE_MISSING_FAIL_CLOSED` | Run-baseline is null, unavailable, or cannot be assembled safely. | Baseline missing. | Yes |
| `RUNTIME_METRIC_REVIEW_ONLY_READY` | `RuntimeMetricService.snapshot()` returns metrics or an explicit empty-sample boundary. | Runtime metric snapshot is visible. | No for display; yes for downstream action |
| `RUNTIME_METRIC_MISSING_FAIL_CLOSED` | Metrics are required but snapshot is unavailable or ambiguous. | Runtime metric visibility missing. | Yes |
| `EXECUTABLE_READINESS_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any status would imply executable readiness. | Executable readiness boundary blocked. | Yes |
| `TRADING_AUTHORIZATION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any status would imply trading authorization. | Trading authorization boundary blocked. | Yes |
| `RECOVERY_REPAIR_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any status would require recovery, repair, restart, or auto-fix. | Recovery/repair boundary blocked. | Yes |
| `SCHEDULER_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any status would trigger or authorize scheduler execution. | Scheduler trigger boundary blocked. | Yes |
| `COLLECTOR_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any status would trigger collector behavior. | Collector trigger boundary blocked. | Yes |
| `API_CLIENT_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any status would call an API client refresh. | API client refresh boundary blocked. | Yes |
| `EXTERNAL_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any status would perform external refresh. | External refresh boundary blocked. | Yes |
| `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any status would produce candidate signal/ranking. | Candidate boundary blocked. | Yes |
| `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any status would produce Point/final direction/entry-stop-TP-RR. | Point boundary blocked. | Yes |
| `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any status would produce order/execution/auto-trading semantics. | Trading boundary blocked. | Yes |

Status precedence:

1. Executable/trading authorization boundary blocked.
2. Recovery/repair/restart/auto-fix boundary blocked.
3. Scheduler/collector/API-client/external-refresh boundary blocked.
4. Candidate/Decision/Point/trading boundary blocked.
5. System guardrail blocked.
6. Runtime readiness backend pending or missing.
7. Run-baseline / runtime metric missing.
8. System guardrail degraded.
9. Runtime readiness partial.
10. Review-only ready.

`READY` means "readable for manual review of operational guardrails." It never means executable readiness, trading authorization, recovery permission, scheduler permission, or order readiness.

## 5. Safety Fields

Future implementation must expose or display the following safety fields/copy if a dedicated endpoint/panel is approved:

| Field | Required value | Purpose |
|---|---:|---|
| `reviewOnly` | `true` | Status is for review only. |
| `manualReviewOnly` | `true` | Human review is required. |
| `notExecutableReadiness` | `true` | Readiness is not executable readiness. |
| `notTradingAuthorization` | `true` | Readiness is not permission to trade. |
| `notRecoveryRepair` | `true` | Status does not run recovery or repair. |
| `notRestartAction` | `true` | Status does not restart anything. |
| `notAutoFix` | `true` | Status does not auto-fix anything. |
| `notSchedulerTrigger` | `true` | Status does not trigger scheduler work. |
| `notCollectorTrigger` | `true` | Status does not trigger collectors. |
| `notApiClientRefresh` | `true` | Status does not refresh through API clients. |
| `notExternalRefresh` | `true` | Status does not call external refresh. |
| `notCandidateSignal` | `true` | Status is not a candidate signal. |
| `notDecisionGeneration` | `true` | Status does not generate decisions. |
| `notPointSignal` | `true` | Status is not Point generation. |
| `notFinalDirection` | `true` | Status does not output final direction. |
| `notEntryStopTpRr` | `true` | Status does not output entry / stop / TP / RR. |
| `notTradingSignal` | `true` | Status is not a trading signal. |
| `notExecutable` | `true` | Status cannot be executed. |
| `displaySlotsAreCandidatePool` | `false` | Display slots remain display-only. |

Allowed supporting fields:

- `status`
- `runtimeStatus`
- `guardrailStatus`
- `generatedAt`
- `windowMinutes`
- `systemHealth.databaseStatus`
- `systemHealth.databaseStatusDetail`
- `systemHealth.schedulerStatus`
- `systemHealth.schedulerStatusDetail`
- `positionSync.availabilityStatus`
- `positionSync.availabilityDetail`
- `performance.hasSamples`
- `performance.totalSampleCount`
- `performance.sampleBoundaryDetail`
- alert/data-quality counts and ratios as read-only context
- recheck counts as read-only context only
- hot reset counts/latest context as read-only context only
- `reason`
- `message`
- `failClosed`

Forbidden fields:

- executable readiness action
- trading authorization action
- recovery action
- repair action
- restart action
- auto-fix action
- scheduler trigger action
- collector trigger action
- API client refresh action
- external refresh action
- candidate ranking or score
- generated decision
- point proposal
- final direction
- entry
- stop
- take profit / TP
- risk-reward / RR
- order action
- execution action
- auto-trading action
- Push send state
- external channel state

## 6. Dashboard / API Surface

Preferred API design:

```text
GET /api/system/run-baseline?windowMinutes=60
```

Use existing run-baseline first. If readiness gate determines this endpoint is too broad for dashboard ergonomics, allow at most:

```text
GET /api/system/runtime-readiness-guardrail-status?windowMinutes=60
```

The optional endpoint must be a thin read-only `Map` projection over existing `RunBaselineVO`, `SystemHealthService`, and `RuntimeMetricService.snapshot()` data.

Preferred dashboard design:

- Reuse existing runtime pill (`runtimeDot`, `runtimeText`).
- Reuse existing sidebar system status (`sidebarSystemStatus`).
- Reuse existing KPI/system surfaces (`cardRisk`, `cardDataQuality`, `cardHotReset`) as context.
- Keep current workbench `Runtime readiness` wording only if copy clarifies that it is review-only operational readiness.

Optional future dashboard panel, only if readiness gate approves:

```text
runtimeReadinessGuardrailStatusPanel
runtimeReadinessStatusValue
systemGuardrailStatusValue
runBaselineStatusValue
runtimeMetricStatusValue
runtimeReadinessBoundaryValue
runtimeReadinessReasonValue
```

Dashboard copy must say that the status is review-only, manual-review-only, fail-closed when uncertain, not executable readiness, not trading authorization, not recovery/repair/restart/auto-fix, not scheduler/collector/API-client/external refresh, not Candidate, not Decision generation, not Point, not final direction, not entry/stop/TP/RR, not order/execution/auto-trading, and not executable.

## 7. Fail-Closed Rules

Fail closed when:

- `RunBaselineVO` is missing or cannot be assembled safely.
- `SystemHealthService.getSystemHealth()` is missing, null, or throws.
- Database status is missing, `DOWN`, or `ERROR` and the status cannot safely explain the state.
- Scheduler status is missing, `STALE`, or `NO_RECENT_ACTIVITY` and the status cannot safely explain the state.
- PositionSync availability is missing or `UNKNOWN` when needed for guardrail confidence.
- Runtime metric snapshot is unavailable when required by the status.
- Alert/data-quality/read-model context is unavailable and the status depends on it.
- Recheck or hot reset context would be interpreted as executable recovery, restart, or recheck capability.
- The only way to answer is to trigger scheduler, collector, API client refresh, or external refresh.
- The only way to answer is to run recovery, repair, restart, or auto-fix.
- The only way to answer is to produce executable readiness or trading authorization.
- The only way to answer is Candidate generation, Decision generation, Point generation, final direction, entry/stop/TP/RR, Push, external channel, order/execution, or auto-trading.
- The implementation would require DTO / Validator / Assembler / Orchestrator, schema/config/pom, or a new service/domain/mapper/repository ownership family.

Fail-closed still permits a visible status panel, but only as a blocked/manual-review indicator.

## 8. Readiness / Authorization Boundary

This slice is operational guardrail visibility only.

Allowed wording:

- "Runtime readiness is review-only and for manual review."
- "System guardrail is degraded; fail-closed for downstream action."
- "Run-baseline is readable; no scheduler or refresh is triggered."
- "No executable readiness or trading authorization is produced."

Forbidden wording:

- "system is authorized to trade"
- "ready to execute"
- "scheduler can run now"
- "auto repair available"
- "restart now"
- "candidate ready"
- "point ready"
- "entry/stop/TP/RR available"
- "order ready"

If copy or field names cannot keep readiness separate from authorization, future readiness gate must return NO-GO.

## 9. Implementation Readiness Gate Questions

The next readiness gate must verify:

1. Whether `/api/system/run-baseline` is sufficient as the API source.
2. Whether a dedicated `GET /api/system/runtime-readiness-guardrail-status` endpoint is truly needed.
3. Whether the dashboard can reuse current system surfaces or needs one minimal `runtimeReadinessGuardrailStatusPanel`.
4. Exact allowed files for a future B-risk implementation.
5. Exact fields from `RunBaselineVO`, `SystemHealthService`, and `RuntimeMetricService.snapshot()` that may be projected.
6. Status precedence and fail-closed mapping for missing/degraded/partial sources.
7. Proof that `RuntimeMetricService.recordDuration(...)` is not introduced as new status behavior.
8. Proof that scheduler/collector/API-client/external refresh paths are not called.
9. Proof that recovery/repair/restart/auto-fix paths are not called.
10. Proof that readiness copy cannot be read as executable readiness or trading authorization.
11. Required targeted tests for endpoint/panel safety fields, fail-closed states, and forbidden field absence.
12. Whether no DTO / Validator / Assembler / Orchestrator, schema/config/pom, or new service/domain/mapper/repository ownership family is needed.
13. NO-GO conditions for refresh, recovery/repair, executable readiness, trading authorization, Candidate, Decision, Point, Push, order/execution, auto-trading, P359, or P360.

## 10. Allowed Future Implementation Scope

If readiness gate returns GO, the next B-risk implementation may allow only:

- `SystemController.java`: one minimal read-only `Map` endpoint if needed.
- `dashboard.html`: minimal status panel/copy/DOM if needed.
- Targeted tests for endpoint safety fields, fail-closed states, dashboard DOM/copy, and forbidden executable fields absent.
- Implementation report docs.
- Source-of-truth updates.

No other Java owner family is justified by this design.

## 11. Forbidden Scope

Future implementation must not:

- change schema/config/pom;
- add DTO / Validator / Assembler / Orchestrator;
- add service/domain/mapper/repository ownership family;
- trigger scheduler / collector / API client refresh / external refresh;
- execute recovery / repair / restart / auto-fix;
- generate executable readiness;
- generate trading authorization;
- generate Candidate / Decision / Point;
- generate final direction / entry / stop / TP / RR;
- send Push or use external channel;
- create order / execution / auto-trading behavior;
- execute Position Monitor;
- execute replay / recheck;
- continue P359 / P360.

## 12. Next Allowed Action

Next allowed action:

`Implementation readiness gate for Runtime readiness / system guardrail status`

Next branch:

`runtime-readiness-system-guardrail-status-implementation-readiness-gate`

Next risk:

`A`

The next package must remain readiness gate docs and source-of-truth updates only. It must not implement runtime readiness / system guardrail status.

## 13. Freeze-Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 资产: Yes, `SystemController`, `SystemHealthService`, `RunBaselineService`, `RunBaselineVO`, `RuntimeMetricService`, dashboard summary/system surfaces, and existing tests are the selected assets.
- 是否减少重复: Yes, by selecting existing system/runtime owner paths instead of creating a new readiness owner family.
- 是否提升 capability level: No
- 是否接 service/runtime/dashboard/API: No implementation in this package; design selects existing service/runtime/dashboard/API owner paths for the future readiness gate.
- 是否符合 #830 审计建议: Yes
