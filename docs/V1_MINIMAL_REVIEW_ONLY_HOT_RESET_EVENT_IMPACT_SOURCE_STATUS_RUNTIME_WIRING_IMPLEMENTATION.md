# V1 Minimal Review-Only Hot Reset / Event Impact Source Status Runtime Wiring Implementation

## Scope

This package implements the minimal review-only runtime wiring for
`Hot Reset / Event Impact Source status`.

It is a B-risk implementation package. It must create a Draft PR and stop for
GPT / human review. It must not auto-merge.

Effective execution baseline:

- Actual main HEAD before this package: `4d4a675 docs(runtime): verify hot reset event readiness`.
- Completed `REVIEW_ONLY_RUNTIME partial` slices before this package: `17`.
- Capability level after this package: still `REVIEW_ONLY_RUNTIME partial`.

## Implemented Endpoint

Endpoint:

```text
GET /api/dashboard/hot-reset-event-impact-source-status?symbol=BTCUSDT
```

Read-only owner path:

```text
DecisionResult.latest.analysisId
  -> HotResetEventMapper.selectLatestByAnalysisId(analysisId)
  -> HotResetEventMapper.countByAnalysisId(analysisId)
  -> SourceTraceEventSourceOwnershipService.resolveEventSourceOwnership(...)
```

The endpoint only reads existing owner assets. It does not call
`HotResetService`, `HotResetServiceImpl`, `AssetStateService.recordHotResetEvent`,
`HotResetEventMapper.insert`, external API refresh, news fetch, scheduler,
collector, Push, Recheck, Replay, Candidate, Decision generation, Point, trading,
or Position Monitor execution paths.

## Dashboard Wiring

The dashboard adds the minimal read-only panel:

- `hotResetEventImpactSourceStatusPanel`
- `hotResetEventSourceStatusValue`
- `eventImpactSourceStatusValue`
- `sourceTraceEventSourceOwnershipValue`
- `hotResetEventCountsValue`
- `hotResetEventLatestValue`
- `hotResetEventBoundaryValue`
- `hotResetExternalBoundaryValue`
- `hotResetSignalBoundaryValue`
- `hotResetEventReasonValue`

The panel copy explicitly states:

- Hot Reset event is read-only event source evidence.
- Event Impact is read-only impact source status.
- SourceTrace event-source ownership remains fail-closed when incomplete.
- No Hot Reset execution or write.
- No event generation.
- No external API refresh, news fetch, scheduler trigger, or collector trigger.
- No Push send, external channel, Recheck execution, or Replay execution.
- No Candidate, Decision generation, Point, final direction, entry/stop/TP/RR,
  order/execution/auto-trading, Position Monitor execution, trading, or
  executable semantics.
- Display Slots are not a candidate pool.

## Safety Fields

The endpoint returns and tests these safety fields:

| Field | Value |
|---|---:|
| `reviewOnly` | `true` |
| `manualReviewOnly` | `true` |
| `notHotResetExecution` | `true` |
| `notHotResetWrite` | `true` |
| `notEventGeneration` | `true` |
| `notExternalApiRefresh` | `true` |
| `notNewsFetch` | `true` |
| `notSchedulerTrigger` | `true` |
| `notCollectorTrigger` | `true` |
| `notPushSend` | `true` |
| `notExternalChannel` | `true` |
| `notRecheckExecution` | `true` |
| `notReplayExecution` | `true` |
| `notCandidateSignal` | `true` |
| `notDecisionGeneration` | `true` |
| `notPointSignal` | `true` |
| `notFinalDirection` | `true` |
| `notEntryStopTpRr` | `true` |
| `notTradingSignal` | `true` |
| `notExecutable` | `true` |
| `displaySlotsAreCandidatePool` | `false` |

## Status Mapping

Implemented status coverage:

- `HOT_RESET_EVENT_SOURCE_REVIEW_ONLY_READY`
- `HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED`
- `HOT_RESET_EVENT_SOURCE_PARTIAL_REVIEW_ONLY`
- `EVENT_IMPACT_SOURCE_REVIEW_ONLY_READY`
- `EVENT_IMPACT_SOURCE_MISSING_FAIL_CLOSED`
- `SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_REVIEW_ONLY_READY`
- `SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE_FAIL_CLOSED`
- `HOT_RESET_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `HOT_RESET_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `EVENT_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `EXTERNAL_API_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `NEWS_FETCH_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `SCHEDULER_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `COLLECTOR_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `RECHECK_REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED`

## Fail-Closed Rules

- Missing `DecisionResult.analysisId` returns
  `HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED`.
- Mapper read exception returns
  `HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED` with blocked source health.
- Missing persisted Hot Reset event returns
  `HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED` and
  `EVENT_IMPACT_SOURCE_MISSING_FAIL_CLOSED`.
- Partial persisted Hot Reset event returns the substatus
  `HOT_RESET_EVENT_SOURCE_PARTIAL_REVIEW_ONLY`.
- SourceTrace event-source ownership is currently incomplete by design and is
  surfaced as `SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE_FAIL_CLOSED`.
- Boundary statuses remain blocked for Hot Reset execution/write, event
  generation, external refresh/news/scheduler/collector, Push,
  Recheck/Replay, Candidate, Point, and Trading.

## Targeted Tests

Added / updated `DashboardControllerTest` coverage for:

- Dashboard DOM ids and safety copy.
- Endpoint safety flags.
- Persisted Hot Reset event read-only evidence.
- SourceTrace event-source incomplete ownership fail-closed behavior.
- Missing Hot Reset event fail-closed behavior.
- Mapper exception fail-closed behavior.
- Partial event substatus behavior.
- Forbidden executable/action fields absent.
- Existing owner path assertion that `HotResetEventMapper.insert(...)` is never
  called by the status endpoint.

## Overreach Check

No schema/config/pom changes were made.

No new DTO / Validator / Assembler / Orchestrator files were added.

No new service/domain/mapper/repository ownership family was added.

No Hot Reset execution/write, event generation, external API refresh, news fetch,
scheduler/collector trigger, Push send, external channel, Recheck/Replay
execution, Candidate generation, Decision generation, Point generation, final
direction, entry/stop/TP/RR, order/execution, auto-trading, Position Monitor
execution, P359, or P360 behavior was added.

## Next Allowed Action

After this B-risk Draft PR is reviewed and merged, the next allowed action is:

```text
Minimal Review-Only Hot Reset / Event Impact Source Status Runtime Wiring Verification
```

That next package must be A-risk verification only.
