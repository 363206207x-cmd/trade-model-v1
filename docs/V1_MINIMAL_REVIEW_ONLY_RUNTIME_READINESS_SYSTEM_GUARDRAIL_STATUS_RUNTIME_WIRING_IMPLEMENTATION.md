# V1 Minimal Review-Only Runtime Readiness / System Guardrail Status Runtime Wiring Implementation

## Current Merged Main

- Current merged main at package start: `5389af8 docs(runtime): verify runtime readiness implementation gate`
- Current module: `Runtime readiness / system guardrail status`
- Current phase: `Implementation`
- Risk: `B`
- Capability level: `REVIEW_ONLY_RUNTIME partial`
- Completed review-only runtime partial slices before this package: `15`

## Implementation Result

Result: implemented the minimal review-only runtime wiring over the existing SystemController / run-baseline / system health / runtime metric owner path.

This package adds one thin read-only status projection and one minimal dashboard guardrail panel. It does not add a DTO, Validator, Assembler, Orchestrator, service, domain, mapper, repository, schema, config, or pom ownership family.

## Implemented Endpoint / Owner Path

- Endpoint: `GET /api/system/runtime-readiness-guardrail-status?windowMinutes=60`
- Owner path reused:
  - `SystemController`
  - `/api/system/run-baseline`
  - `RunBaselineService`
  - `RunBaselineVO`
  - `SystemHealthService`
  - `RuntimeMetricService.snapshot()`

The endpoint only calls `runBaselineService.getRunBaseline(...)`. `/api/system/health` remains static liveness only and is not used as executable readiness.

## Dashboard Panel / DOM

Added minimal dashboard status surface:

- `runtimeReadinessGuardrailStatusPanel`
- `runtimeReadinessStatusValue`
- `systemGuardrailStatusValue`
- `runBaselineStatusValue`
- `runtimeMetricStatusValue`
- `runtimeReadinessSourceHealthValue`
- `runtimeReadinessFailClosedValue`
- `runtimeReadinessReviewOnlyValue`
- `runtimeReadinessBoundaryValue`
- `runtimeReadinessSignalBoundaryValue`
- `runtimeReadinessReasonValue`

Dashboard copy explicitly states review-only, manual review only, fail-closed, not executable readiness, not trading authorization, not recovery / repair / restart / auto-fix, not scheduler trigger, not collector trigger, not API client refresh, not external refresh, not candidate, not decision generation, not point, not final direction, not entry / stop / TP / RR, not trading, not executable, and Display Slots are not a candidate pool.

## Status Mapping

Primary statuses:

- `RUNTIME_READINESS_REVIEW_ONLY_READY`
- `RUNTIME_READINESS_BACKEND_PENDING_FAIL_CLOSED`
- `RUNTIME_READINESS_MISSING_FAIL_CLOSED`
- `RUNTIME_READINESS_PARTIAL_REVIEW_ONLY`
- `SYSTEM_GUARDRAIL_REVIEW_ONLY_READY`
- `SYSTEM_GUARDRAIL_DEGRADED_REVIEW_ONLY`
- `SYSTEM_GUARDRAIL_BLOCKED_FAIL_CLOSED`
- `RUN_BASELINE_REVIEW_ONLY_READY`
- `RUN_BASELINE_MISSING_FAIL_CLOSED`
- `RUNTIME_METRIC_REVIEW_ONLY_READY`
- `RUNTIME_METRIC_MISSING_FAIL_CLOSED`

Boundary statuses:

- `EXECUTABLE_READINESS_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `TRADING_AUTHORIZATION_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `RECOVERY_REPAIR_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `SCHEDULER_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `COLLECTOR_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `API_CLIENT_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `EXTERNAL_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED`

## Safety Fields

The endpoint returns these safety fields as fixed review-only guardrails:

- `reviewOnly=true`
- `manualReviewOnly=true`
- `notExecutableReadiness=true`
- `notTradingAuthorization=true`
- `notRecoveryRepair=true`
- `notRestartAction=true`
- `notAutoFix=true`
- `notSchedulerTrigger=true`
- `notCollectorTrigger=true`
- `notApiClientRefresh=true`
- `notExternalRefresh=true`
- `notCandidateSignal=true`
- `notDecisionGeneration=true`
- `notPointSignal=true`
- `notFinalDirection=true`
- `notEntryStopTpRr=true`
- `notTradingSignal=true`
- `notExecutable=true`
- `displaySlotsAreCandidatePool=false`

## Fail-Closed Rules

- Run-baseline read exception returns `RUNTIME_READINESS_BACKEND_PENDING_FAIL_CLOSED`.
- Missing `RunBaselineVO` returns `RUN_BASELINE_MISSING_FAIL_CLOSED`.
- Missing `SystemHealthSnapshot` returns `RUNTIME_READINESS_MISSING_FAIL_CLOSED`.
- Missing runtime metric snapshot returns `RUNTIME_METRIC_MISSING_FAIL_CLOSED`.
- Database not `UP` or scheduler observation not `RUNNING` returns `SYSTEM_GUARDRAIL_DEGRADED_REVIEW_ONLY` with downstream action fail-closed.
- Complete run-baseline + system health + runtime metric read model returns `RUNTIME_READINESS_REVIEW_ONLY_READY` with `failClosed=false`, but still only for manual review.

## Readiness / Authorization Boundary

This implementation does not:

- generate executable readiness
- generate trading authorization
- execute recovery / repair / restart / auto-fix
- trigger scheduler
- trigger collector
- trigger API client refresh
- trigger external refresh
- generate Candidate
- generate Decision
- generate Point
- generate final direction
- generate entry / stop / TP / RR
- send Push
- call external channel
- place order
- execute trade
- perform auto-trading
- execute Position Monitor behavior
- execute replay / recheck

## Targeted Tests

Targeted coverage added or updated:

- `SystemControllerTest`
  - endpoint ready status
  - static liveness endpoint remains not executable readiness
  - missing run-baseline fail-closed
  - backend exception fail-closed
  - degraded system guardrail review-only fail-closed
  - missing runtime metrics fail-closed
  - forbidden executable / refresh / candidate / point / trading fields absent
- `DashboardControllerTest`
  - `runtimeReadinessGuardrailStatusPanel` DOM ids
  - status constants
  - readiness / authorization / refresh / generation boundary copy

## Source Of Truth Handoff

After this implementation package is merged, the next allowed action is:

`Minimal Review-Only Runtime Readiness / System Guardrail Status Runtime Wiring Verification`

Next branch:

`runtime-readiness-system-guardrail-status-runtime-wiring-verification`

The completed slice count remains `15`. Runtime readiness / system guardrail status is not a completed small closure until verification and visual closure are completed and merged.

## Overreach Status

No overreach:

- no schema/config/pom
- no new DTO / Validator / Assembler / Orchestrator
- no new service/domain/mapper/repository ownership family
- no executable readiness
- no trading authorization
- no recovery / repair / restart / auto-fix
- no scheduler trigger
- no collector trigger
- no API client refresh
- no external refresh
- no Candidate generation
- no Decision generation
- no Point generation
- no final direction / entry / stop / TP / RR
- no Push send or external channel
- no order / execution / auto-trading
- no Position Monitor execution
- no replay / recheck
- no P359 / P360
