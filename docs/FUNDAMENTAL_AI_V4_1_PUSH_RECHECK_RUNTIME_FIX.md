# Fundamental AI v4.1 Push Recheck Runtime Fix

Status: `IMPLEMENTED_PENDING_INDEPENDENT_REAUDIT`

## Root Cause

The pending-recheck mapper used a parameterized H2 `DATEADD` expression. H2
could not infer the interval-unit parameter and the scheduler caught the query
exception, allowing the process to remain alive while scheduled Recheck did no
work.

## Fix

- `PushRecheckScheduler` computes
  `cutoffAt = referenceAt.minusMinutes(minRetryMinutes)` in Java.
- `PushSnapshotMapper.listPendingRecheckNext` compares
  `last_recheck_time <= cutoffAt` using dialect-neutral SQL.
- No H2-only business query or PostgreSQL interval branch was added.
- Scheduler execution now records `SUCCEEDED`, `PARTIAL`, or `FAILED`, trace ID,
  processed/succeeded/failed counts, error class/message and completion time.
- Query failure is observable as `FAILED` and cannot masquerade as success.

## Executed Evidence

| Check | Result |
|---|---|
| H2 mapper cutoff selection | PASS |
| Java-computed scheduler cutoff | PASS |
| H2 scheduled invocation | PASS |
| failure visibility and no downstream call | PASS |
| PostgreSQL 16.15 cutoff query | PASS |
| no `DATEADD`/`INTERVAL` in the mapper query | PASS |

Principal suites:

- `PushSnapshotCutoffMapperIntegrationTest`
- `PushRecheckSchedulerRuntimeContractTest`
- `PostgreSqlDateFunctionVariantGuardTest`
- `ControlledPostgreSqlFlywaySmokeTest`
- existing Push Recheck service/scheduler regression suites

`PUSH_RECHECK_H2_SCHEDULER = PASS`

`PUSH_RECHECK_POSTGRESQL = PASS`

`SCHEDULER_ERROR_VISIBILITY = PASS`
