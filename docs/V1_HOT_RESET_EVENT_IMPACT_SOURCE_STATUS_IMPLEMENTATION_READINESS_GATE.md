# V1 Hot Reset / Event Impact Source Status Implementation Readiness Gate

## Scope

This package is the implementation readiness gate for
`Hot Reset / Event Impact Source review-only status`.

It is readiness-gate documentation only. It does not implement an endpoint,
dashboard panel, Java business logic, tests, schema/config/pom changes, Hot Reset
execution/write behavior, event generation, external API refresh, news fetch,
scheduler/collector trigger, Push send, external channel, Recheck/Replay
execution, Candidate generation, Decision generation, Point generation, final
direction, entry/stop/TP/RR, order/execution, auto-trading, Position Monitor
execution, P359, or P360.

Effective execution baseline:

- Actual main HEAD: `761af58 docs(runtime): design hot reset event source status`.
- Source-of-truth baseline lag before this package is non-blocking.
- Completed `REVIEW_ONLY_RUNTIME partial` slices before this gate: `17`.
- Capability level after this package: still `REVIEW_ONLY_RUNTIME partial`.

Readiness decision: `GO_TO_B_RISK_MINIMAL_IMPLEMENTATION`.

The next implementation package is B-risk. It must create a Draft PR and stop
for GPT / human review before merge.

## Readiness Inputs

This gate is based on:

- `docs/V1_HOT_RESET_EVENT_IMPACT_SOURCE_STATUS_SOURCE_READ.md`
- `docs/V1_MINIMAL_REVIEW_ONLY_HOT_RESET_EVENT_IMPACT_SOURCE_STATUS_RUNTIME_WIRING_DESIGN.md`
- existing `HotResetEventMapper` persisted-event read methods
- existing `EventImpactInputVO` and `EvidenceServiceImpl` read semantics
- existing SourceTrace event-source ownership fail-closed path
- existing run-baseline Hot Reset summary context
- existing review-page and dashboard display context

## GO / NO-GO Judgement

| Required judgement | Decision |
|---|---|
| Allow next B-risk implementation? | **GO**, but only for a minimal review-only status wiring package with Draft PR and manual/GPT review before merge. |
| Reuse `HotResetEventMapper` persisted-event reads? | **Required.** Future implementation must prefer existing read methods such as `selectLatestByAnalysisId(...)`, `countByAnalysisId(...)`, `countInWindow(...)`, and `selectTriggerTypeCountsInWindow(...)`. |
| Reuse `EventImpactInputVO` and `EvidenceServiceImpl` read semantics? | **Required as read semantics only.** They may support source-status projection but must not run scoring, evidence generation, event generation, or Decision generation. |
| Reuse SourceTrace event-source ownership fail-closed path? | **Required.** Existing incomplete ownership must be surfaced honestly as fail-closed and manual-review-only. |
| Reuse run-baseline Hot Reset summary? | **Allowed as aggregate context only.** It must not become a trigger, refresh, scheduler, or collector path. |
| Reuse review aggregate, `review-page.js`, and dashboard display context? | **Allowed as display context only.** They cannot become execution, write, generation, replay/recheck, Push, Candidate, Point, or trading entrypoints. |
| Allow dedicated status endpoint? | **Allowed if needed.** Maximum is one minimal read-only `Map` endpoint under an existing controller owner, preferably `GET /api/dashboard/hot-reset-event-impact-source-status?symbol=BTCUSDT`. |
| Allow dashboard status panel? | **Allowed if needed.** It must be a minimal read-only status panel with DOM ids and safety copy only. No dashboard business logic or action entrypoints. |
| SourceTrace ownership incomplete / fail-closed handling? | **Must stay fail-closed.** Implementation must not fabricate source trace evidence id, event source id, event source type, or ownership readiness. |
| Hot Reset event meaning? | **Read-only event source evidence only.** Persisted `HotResetEventDO` data may be displayed as evidence; it cannot trigger Hot Reset execution. |
| Event Impact meaning? | **Read-only impact source status only.** It cannot create events, fetch news, refresh external APIs, or alter score/decision behavior. |

## Required Owner Path

Preferred minimal implementation owner path:

```text
existing dashboard/latest analysis context when already available
  -> analysisId
  -> HotResetEventMapper.selectLatestByAnalysisId(analysisId)
  -> HotResetEventMapper.countByAnalysisId(analysisId)
  -> HotResetEventDO read-only event fields
  -> EventImpactInputVO-compatible source status fields
```

Allowed aggregate context:

```text
SystemController / RunBaselineServiceImpl
  -> HotResetEventMapper.countInWindow(windowMinutes)
  -> HotResetEventMapper.selectTriggerTypeCountsInWindow(windowMinutes)
  -> light system status latest Hot Reset fields
  -> run-baseline Hot Reset summary
```

Required SourceTrace boundary:

```text
SourceTraceEventSourceOwnershipService
  -> FailClosedSourceTraceEventSourceOwnershipService
  -> SourceTraceEventSourceOwnershipResult
  -> INCOMPLETE / MISSING_SOURCE / REVIEW_ONLY / manualReviewRequired
```

Display context only:

```text
ReviewAggregateServiceImpl.toHotReset(...)
  -> review-page.js sec-hot-reset
dashboard.html
  -> kpi-hot-reset
```

## Allowed Implementation Files

If the next implementation proceeds, the maximum allowed file set is:

- `src/main/java/org/example/trademodel/controller/DashboardController.java` or an existing related controller: only one minimal read-only `Map` endpoint if necessary.
- `src/main/resources/templates/dashboard.html`: only a minimal Hot Reset / Event Impact Source status panel, DOM ids, and safety copy.
- Targeted controller/dashboard tests such as `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`: only endpoint safety fields, fail-closed states, Hot Reset / Event Impact boundary, forbidden executable fields absent, and dashboard DOM/copy if panel is added.
- Existing Hot Reset / Event Impact / SourceTrace tests: only tiny existing owner-path assertions if necessary, without expanding business semantics.
- Future implementation report docs.
- Source-of-truth docs.

Proposed endpoint if needed:

```text
GET /api/dashboard/hot-reset-event-impact-source-status?symbol=BTCUSDT
```

Proposed dashboard DOM ids if needed:

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

## Forbidden Files And Areas

The next implementation must not modify or create:

- schema/config/pom files.
- new DTO / Validator / Assembler / Orchestrator files.
- new service/domain/mapper/repository ownership families.
- Hot Reset service execution or write paths such as `HotResetService`, `HotResetServiceImpl`, `AssetStateService.recordHotResetEvent(...)`, `AssetStateServiceImpl.recordHotResetEvent(...)`, or `HotResetEventMapper.insert(...)`.
- Event generation, Score, Decision generation, Candidate, Point, final direction, entry/stop/TP/RR, order/execution, auto-trading, Position Monitor, Push, external channel, Recheck, Replay, external API refresh, news fetch, scheduler, or collector code.
- P359 or P360 assets.

Read-only references to existing owner paths are allowed only from the bounded
status endpoint/panel implementation.

## Required Safety Fields

Any dedicated endpoint or panel must expose/test/display these safety fields:

| Field | Required value |
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

## Required Status Mapping

| Status | Required meaning |
|---|---|
| `HOT_RESET_EVENT_SOURCE_REVIEW_ONLY_READY` | Persisted Hot Reset event source evidence is readable for manual review only. |
| `HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED` | Selected analysis/context or persisted Hot Reset event source evidence is missing. |
| `HOT_RESET_EVENT_SOURCE_PARTIAL_REVIEW_ONLY` | Persisted event exists but non-executable display fields are incomplete. |
| `EVENT_IMPACT_SOURCE_REVIEW_ONLY_READY` | Event Impact source status can be derived from existing persisted-event read semantics. |
| `EVENT_IMPACT_SOURCE_MISSING_FAIL_CLOSED` | Event Impact source status cannot be read without generation/refresh/scoring changes. |
| `SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_REVIEW_ONLY_READY` | SourceTrace event-source ownership is provably readable without fabrication. |
| `SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE_FAIL_CLOSED` | SourceTrace event-source ownership is incomplete, missing, or unprovable. |
| `HOT_RESET_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to call or imply Hot Reset execution blocks implementation. |
| `HOT_RESET_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to write Hot Reset state/event rows blocks implementation. |
| `EVENT_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to create events, facts, macro/news events, or event evidence blocks implementation. |
| `EXTERNAL_API_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any external API/client refresh need blocks implementation. |
| `NEWS_FETCH_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any news fetch need blocks implementation. |
| `SCHEDULER_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any scheduler trigger need blocks implementation. |
| `COLLECTOR_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any collector trigger need blocks implementation. |
| `PUSH_SEND_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any Push send or external notification delivery need blocks implementation. |
| `RECHECK_REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any Recheck or Replay execution need blocks implementation. |
| `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any Candidate generation/ranking need blocks implementation. |
| `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any Point, final direction, entry/stop/TP/RR need blocks implementation. |
| `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any order/execution/auto-trading need blocks implementation. |

Fail-closed precedence for implementation:

1. Hot Reset execution/write boundary.
2. Event generation, external API refresh, or news fetch boundary.
3. Scheduler or collector boundary.
4. Push, external channel, Recheck, or Replay boundary.
5. Candidate, Decision generation, Point, final direction, entry/stop/TP/RR, or trading boundary.
6. SourceTrace event-source ownership incomplete fail-closed.
7. Hot Reset event source missing fail-closed.
8. Event Impact source missing fail-closed.
9. Partial read-only source evidence.
10. Ready read-only source evidence.

`READY` means readable source/status evidence for manual review. It never means
Hot Reset execution, event generation, executable readiness, trade authorization,
or any trading action.

## Required Targeted Tests For Next Implementation

The B-risk implementation must include targeted tests for:

- endpoint exists, if added, and returns a `Map` only.
- endpoint safety fields have the exact required values.
- missing latest analysis/context fails closed.
- `HotResetEventMapper` read exception fails closed.
- missing persisted Hot Reset event fails closed.
- partial persisted event maps to `HOT_RESET_EVENT_SOURCE_PARTIAL_REVIEW_ONLY`.
- missing Event Impact source maps to `EVENT_IMPACT_SOURCE_MISSING_FAIL_CLOSED`.
- incomplete SourceTrace event-source ownership maps to `SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE_FAIL_CLOSED` and does not fabricate source id/type/evidence id.
- Hot Reset execution/write boundaries stay blocked and do not call `HotResetService.executeHotReset(...)`, `AssetStateService.recordHotResetEvent(...)`, `AssetStateServiceImpl.recordHotResetEvent(...)`, or `HotResetEventMapper.insert(...)`.
- event generation, external API refresh, news fetch, scheduler/collector, Push, external channel, Recheck/Replay, Candidate, Decision generation, Point, final direction, entry/stop/TP/RR, order/execution, auto-trading, and Position Monitor boundaries stay blocked.
- forbidden executable/action fields are absent from the endpoint response.
- dashboard panel DOM ids and safety copy exist, if a panel is added.
- existing owner path assertions in Hot Reset / Event Impact / SourceTrace tests, only if needed.

The B-risk implementation must run the targeted controller/dashboard tests and
the full `./mvnw -q test` suite before PR review.

## NO-GO Conditions

Implementation must stop with NO-GO if it requires any of the following:

- Hot Reset execution.
- Hot Reset write.
- event generation.
- external API refresh.
- news fetch.
- scheduler trigger.
- collector trigger.
- Push send.
- external channel.
- Recheck execution.
- Replay execution.
- Candidate generation.
- Decision generation.
- Point generation.
- final direction.
- entry / stop / TP / RR.
- order / execution / auto-trading.
- Position Monitor execution.
- schema/config/pom changes.
- new DTO / Validator / Assembler / Orchestrator.
- new service/domain/mapper/repository ownership family.
- treating Hot Reset event as anything beyond read-only event source evidence.
- treating Event Impact as anything beyond read-only impact source status.
- treating incomplete SourceTrace event-source ownership as ready ownership.
- treating this module as event action, trading action, executable readiness, or trade authorization.

## Hot Reset / Event Boundary

Allowed:

- read already-persisted Hot Reset event rows.
- read Hot Reset event counts and trigger-type counts.
- mirror Event Impact input/evidence read semantics as source status.
- surface SourceTrace event-source ownership as ready only when complete, otherwise fail-closed.
- display manual-review-only status/copy.

Forbidden:

- execute Hot Reset.
- write Hot Reset state or event rows.
- generate events or event facts.
- fetch news or refresh external APIs.
- trigger scheduler/collector work.
- send Push or external channel notifications.
- execute Recheck/Replay.
- generate Candidate, Decision, Point, final direction, entry/stop/TP/RR, order, execution, auto-trading, or Position Monitor actions.

## Readiness Result

`GO_TO_B_RISK_MINIMAL_IMPLEMENTATION`

The next implementation may proceed only within the file and behavior limits in
this gate. It must remain `REVIEW_ONLY_RUNTIME partial`, create a Draft PR, and
stop for GPT / human review before merge.

## Next Allowed Action

`Minimal Review-Only Hot Reset / Event Impact Source Status Runtime Wiring Implementation`

Suggested next branch:

`minimal-review-only-hot-reset-event-impact-source-status-runtime-wiring-implementation`

## #830 Audit

- New skeleton created: no.
- Cursor-era assets reused: yes, existing Hot Reset event mapper/entity,
  EventImpact input/evidence read semantics, SourceTrace event-source ownership
  fail-closed result, run-baseline summary, review aggregate display, and
  dashboard KPI context.
- Duplication reduced: yes, this gate selects existing owner/read paths and
  keeps new owner families blocked.
- Capability level raised: no.
- Service/runtime/dashboard/API connected: no, readiness gate only.
- #830 audit alignment: yes, this avoids DTO / Validator / Assembler /
  Orchestrator expansion and keeps P359/P360 frozen.
