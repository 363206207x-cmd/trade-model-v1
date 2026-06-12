# V1 Minimal Review-Only Hot Reset / Event Impact Source Status Runtime Wiring Design

## Scope

This package designs the minimal review-only runtime wiring for
`Hot Reset / Event Impact Source status`.

It is design only. It does not implement an endpoint, dashboard panel, Java
business logic, tests, schema/config/pom changes, Hot Reset execution/write,
event generation, external API refresh, news fetch, scheduler/collector trigger,
Push send, external channel, Recheck/Replay execution, Candidate generation,
Decision generation, Point generation, final direction, entry/stop/TP/RR,
order/execution, auto-trading, Position Monitor execution, P359, or P360.

Effective execution baseline:

- Actual main HEAD: `00cee60 docs(runtime): read hot reset event source path`.
- Source-of-truth baseline lag before this design package is non-blocking.
- Completed `REVIEW_ONLY_RUNTIME partial` slices before this design: `17`.
- Capability level after this package: still `REVIEW_ONLY_RUNTIME partial`.

Design result: `GO_TO_IMPLEMENTATION_READINESS_GATE_ONLY`.

This package does not authorize implementation.

## Source-Read Inputs

The design is based on
`docs/V1_HOT_RESET_EVENT_IMPACT_SOURCE_STATUS_SOURCE_READ.md` and the existing
assets it confirmed:

- `HotResetService`
- `HotResetServiceImpl`
- `HotResetEventDO`
- `HotResetEventMapper`
- `EventImpactInputVO`
- `EvidenceServiceImpl`
- `AssetStateService`
- `RunBaselineServiceImpl`
- `ReviewAggregateServiceImpl`
- `SourceTraceEventSourceOwnershipService`
- `FailClosedSourceTraceEventSourceOwnershipService`
- `SourceTraceEventSourceOwnershipResult`
- `dashboard.html`
- `review-page.js`
- related tests/docs

## Design Answers

| Question | Design answer |
|---|---|
| Reuse existing Hot Reset event read path? | **Yes.** The future status should reuse persisted event reads from `HotResetEventMapper.selectLatestByAnalysisId(...)`, `countByAnalysisId(...)`, `countInWindow(...)`, and `selectTriggerTypeCountsInWindow(...)`. The write method `insert(...)` is forbidden. |
| Reuse Event Impact input/evidence read path? | **Yes, as source semantics only.** `EventImpactInputVO` and `EvidenceServiceImpl` prove that persisted Hot Reset events can be read into event-impact context. Future status may mirror those readable fields, but must not run scoring, evidence generation, event generation, or Decision generation. |
| Reuse SourceTrace event-source ownership fail-closed path? | **Yes.** The future status should surface the existing fail-closed ownership semantics from `FailClosedSourceTraceEventSourceOwnershipService`, especially `INCOMPLETE`, `MISSING_SOURCE`, `REVIEW_ONLY`, and `manualReviewRequired=true`. It must not fabricate source ownership. |
| Reuse run-baseline Hot Reset summary? | **Yes, as aggregate context.** `RunBaselineServiceImpl.buildHotResetSummary(...)` can be referenced for count/window/trigger-type summary when already read by the existing run-baseline owner. It must not become a trigger or refresh path. |
| Reuse review-page / dashboard display context? | **Yes, as display context only.** `review-page.js` `sec-hot-reset`, review aggregate Hot Reset summary, and dashboard `kpi-hot-reset` can inform copy and field naming, but they must not become execution or generation entrypoints. |
| Allow dedicated status endpoint? | **Allowed only if the readiness gate confirms it is needed.** Maximum shape is one minimal read-only `Map` endpoint, for example `GET /api/dashboard/hot-reset-event-impact-source-status?symbol=BTCUSDT`. |
| Allow dashboard status panel? | **Allowed only if readiness gate confirms it is needed.** It must be a minimal read-only status panel and must not change dashboard business logic or add action buttons. |
| How to handle incomplete SourceTrace event-source ownership? | Preserve fail-closed truth. Show `SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE_FAIL_CLOSED` when ownership is incomplete, `eventSource` is missing, or source id/type cannot be proven. Do not synthesize source trace evidence id, event source id, or event source type. |
| Can Hot Reset event be event source evidence? | **Yes, read-only evidence only.** Persisted `HotResetEventDO` fields can show an event source snapshot, count, latest event time, trigger type, trigger value, and reason. They cannot trigger Hot Reset execution. |
| Can Event Impact be impact source status? | **Yes, read-only status only.** Event Impact can describe existing event impact input/evidence status. It cannot generate events, fetch news, call external APIs, or change Score/Decision behavior. |
| How to mark not Hot Reset execution? | Force `notHotResetExecution=true` and never call `HotResetService.executeHotReset(...)`. |
| How to mark not Hot Reset write? | Force `notHotResetWrite=true` and never call `AssetStateService.recordHotResetEvent(...)`, `AssetStateServiceImpl.recordHotResetEvent(...)`, or `HotResetEventMapper.insert(...)`. |
| How to mark not event generation? | Force `notEventGeneration=true`; status may read persisted events only and cannot create new event rows, event evidence, event facts, or macro/news events. |
| How to mark not external API refresh? | Force `notExternalApiRefresh=true`; status may not call market/news/external clients or refresh APIs. |
| How to mark not news fetch? | Force `notNewsFetch=true`; broad macro-news/event-calendar remains deferred. |
| How to mark not scheduler / collector trigger? | Force `notSchedulerTrigger=true` and `notCollectorTrigger=true`; status may not trigger collection or scheduled jobs. |
| How to mark not Push / external channel? | Force `notPushSend=true` and `notExternalChannel=true`; status cannot send or prepare external notifications. |
| How to mark not Recheck / Replay execution? | Force `notRecheckExecution=true` and `notReplayExecution=true`; status cannot call replay/recheck paths. |
| How to mark not Candidate / Decision / Point? | Force `notCandidateSignal=true`, `notDecisionGeneration=true`, and `notPointSignal=true`; status fields cannot become candidate ranking, generated decisions, or point proposals. |
| How to mark not final direction / entry / stop / TP / RR? | Force `notFinalDirection=true` and `notEntryStopTpRr=true`; status cannot expose plan fields. |
| How to mark not order / execution / auto-trading? | Force `notTradingSignal=true` and `notExecutable=true`; no order/execution/auto-trading action is permitted. |
| Need DTO / Validator / Assembler / Orchestrator? | **No.** Existing entities/VOs and a minimal `Map` projection, if approved later, are enough. New skeletons are blocked. |
| Need schema/config/pom? | **No.** Existing `tm_hot_reset_event`, `tm_asset_state`, and current service/controller assets are sufficient for design. |
| Need new service/domain/mapper/repository ownership family? | **No.** Reuse existing owner paths. A new owner family would duplicate the existing Hot Reset event read and SourceTrace fail-closed ownership assets. |
| What must the readiness gate check? | Whether implementation is GO/NO-GO, endpoint necessity, dashboard panel necessity, exact file scope, read-only owner calls, no write/execute/generate/refresh triggers, safety fields, fail-closed precedence, targeted tests, and no new skeleton owners/schema/config/pom. |

## Owner Path

Preferred future read owner path:

```text
existing dashboard/latest analysis context when already available
  -> analysisId
  -> HotResetEventMapper.selectLatestByAnalysisId(analysisId)
  -> HotResetEventMapper.countByAnalysisId(analysisId)
  -> HotResetEventDO read-only event fields
  -> EventImpactInputVO-compatible source status fields
```

Aggregate context path:

```text
SystemController / RunBaselineServiceImpl
  -> HotResetEventMapper.countInWindow(windowMinutes)
  -> HotResetEventMapper.selectTriggerTypeCountsInWindow(windowMinutes)
  -> light system status latest Hot Reset fields
  -> run-baseline Hot Reset summary
```

SourceTrace ownership context:

```text
SourceTraceEventSourceOwnershipService
  -> FailClosedSourceTraceEventSourceOwnershipService
  -> SourceTraceEventSourceOwnershipResult
  -> INCOMPLETE / MISSING_SOURCE / REVIEW_ONLY / manualReviewRequired
```

Review and dashboard display context:

```text
ReviewAggregateServiceImpl.toHotReset(...)
  -> review-page.js sec-hot-reset
dashboard.html
  -> kpi-hot-reset
```

Rejected owner paths:

```text
HotResetService.executeHotReset(...)
HotResetService.shouldTriggerHotReset(...) as a status trigger
AssetStateService.recordHotResetEvent(...)
AssetStateServiceImpl.recordHotResetEvent(...)
HotResetEventMapper.insert(...)
score/event-impact formula changes
news fetch / external API refresh
scheduler / collector trigger
Push send / external channel
replay / recheck execution
Candidate generation / Decision generation / Point generation
final direction / entry / stop / TP / RR
order / execution / auto-trading
Position Monitor execution
new DTO / Validator / Assembler / Orchestrator
new service/domain/mapper/repository ownership family
schema/config/pom changes
P359 / P360
```

## Endpoint Decision

Default: reuse existing read owner paths and display contexts first.

A dedicated endpoint is allowed only after the implementation readiness gate
confirms that dashboard status needs a small projection. If allowed, the maximum
future endpoint is one minimal read-only `Map` endpoint under an existing
controller owner, preferably dashboard-scoped:

```text
GET /api/dashboard/hot-reset-event-impact-source-status?symbol=BTCUSDT
```

Allowed endpoint output categories:

- status identifiers
- safety booleans
- fail-closed / partial booleans
- latest persisted Hot Reset event evidence
- read-only event impact source summary
- SourceTrace event-source ownership status
- run-baseline Hot Reset count/window context
- boundary summaries and reason strings

Forbidden endpoint output categories:

- Hot Reset execution action
- Hot Reset write action
- event generation action
- external API refresh/news fetch action
- scheduler/collector action
- Push send/external channel action
- Recheck/Replay action
- Candidate ranking/score
- generated decision
- Point proposal
- final direction
- entry / stop / take profit / TP / risk-reward / RR
- order / execution / auto-trading action

## Dashboard Design

Existing dashboard `kpi-hot-reset` is only a compact KPI. It does not carry the
full review-only safety contract. A minimal dashboard panel is therefore allowed
only if the readiness gate confirms a dedicated panel is necessary for clear
safety copy.

Proposed future DOM ids if a panel is approved:

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

Required dashboard copy:

- review-only
- manual review only
- fail-closed
- Hot Reset event is read-only event source evidence
- Event Impact is read-only impact source status
- SourceTrace event-source ownership may be incomplete and fail-closed
- not Hot Reset execution
- not Hot Reset write
- not event generation
- not external API refresh
- not news fetch
- not scheduler trigger
- not collector trigger
- not Push send
- not external channel
- not Recheck execution
- not Replay execution
- not Candidate / Decision generation / Point
- not final direction / entry / stop / TP / RR
- not order / execution / auto-trading
- not executable
- Display Slots are not a candidate pool

The dashboard must not add buttons, execution links, write links, refresh links,
news-fetch links, Push links, replay/recheck links, candidate links, point links,
trade links, or Position Monitor actions.

## Status Mapping

| Status | Trigger condition | Review-only meaning | Fail closed |
|---|---|---|---:|
| `HOT_RESET_EVENT_SOURCE_REVIEW_ONLY_READY` | Latest persisted Hot Reset event is readable for the selected analysis/context and safety boundaries are present. | Hot Reset event source evidence can be displayed for manual review. | false |
| `HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED` | No analysis/context exists, no latest persisted event exists, or mapper read returns no row. | Hot Reset event source evidence is missing. | true |
| `HOT_RESET_EVENT_SOURCE_PARTIAL_REVIEW_ONLY` | Persisted event exists but non-executable display fields such as reason, trigger value, event time, or count are incomplete. | Partial Hot Reset event source evidence only. | false |
| `EVENT_IMPACT_SOURCE_REVIEW_ONLY_READY` | Event impact input/evidence context can be derived from persisted Hot Reset event reads. | Event Impact source status is visible as read-only metadata. | false |
| `EVENT_IMPACT_SOURCE_MISSING_FAIL_CLOSED` | Event impact source fields cannot be read without generation, scoring changes, news fetch, or refresh. | Event Impact source status is missing. | true |
| `SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_REVIEW_ONLY_READY` | Existing SourceTrace event-source ownership result is present and complete without fabrication. | Event-source ownership can be displayed as read-only metadata. | false |
| `SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE_FAIL_CLOSED` | Ownership is `INCOMPLETE`, event source is null, missing reason is present, or source id/type is not provable. | Ownership remains incomplete and fail-closed. | true |
| `HOT_RESET_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any implementation would call or imply `HotResetService.executeHotReset(...)`. | Hot Reset execution boundary blocked. | true |
| `HOT_RESET_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any implementation would write Hot Reset state/event rows. | Hot Reset write boundary blocked. | true |
| `EVENT_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any implementation would create event facts, event rows, macro/news events, or event evidence. | Event generation boundary blocked. | true |
| `EXTERNAL_API_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any implementation would call external refresh/API clients. | External API refresh boundary blocked. | true |
| `NEWS_FETCH_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any implementation would fetch news or events. | News fetch boundary blocked. | true |
| `SCHEDULER_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any implementation would trigger scheduler work. | Scheduler trigger boundary blocked. | true |
| `COLLECTOR_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any implementation would trigger collector work. | Collector trigger boundary blocked. | true |
| `PUSH_SEND_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any implementation would send Push or prepare external notification delivery. | Push boundary blocked. | true |
| `RECHECK_REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any implementation would execute recheck or replay. | Recheck/Replay boundary blocked. | true |
| `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any implementation would generate or rank candidates. | Candidate boundary blocked. | true |
| `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any implementation would generate Point, final direction, or entry/stop/TP/RR. | Point boundary blocked. | true |
| `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any implementation would create order/execution/auto-trading behavior. | Trading boundary blocked. | true |

Status precedence:

1. Hot Reset execution/write boundary blocked.
2. Event generation / external API refresh / news fetch boundary blocked.
3. Scheduler / collector boundary blocked.
4. Push / external channel / Recheck / Replay boundary blocked.
5. Candidate / Decision / Point / trading boundary blocked.
6. SourceTrace event-source ownership incomplete fail-closed.
7. Hot Reset event source missing fail-closed.
8. Event Impact source missing fail-closed.
9. Hot Reset event source partial review-only.
10. Hot Reset / Event Impact / SourceTrace read-only ready.

`READY` means "readable as source/status evidence for manual review." It never
means Hot Reset execution, event generation, trade authorization, or action
readiness.

## Safety Fields

Future implementation must expose or display the following safety fields/copy if
a dedicated endpoint/panel is approved:

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

Allowed supporting fields:

- `status`
- `hotResetEventSourceStatus`
- `eventImpactSourceStatus`
- `sourceTraceEventSourceOwnershipStatus`
- `latestEventTime`
- `eventTraceId`
- `eventVersion`
- `triggerType`
- `triggerValue`
- `triggerReasonCode`
- `triggerReasonText`
- `eventFactHit`
- `eventFactCount`
- `eventLatestTime`
- `eventReasonCode`
- `eventTriggerType`
- `eventSourceOwnershipMissingReason`
- `manualReviewRequired`
- `failClosed`
- `partial`
- `reason`

Forbidden fields:

- `hotResetExecutionAction`
- `hotResetWriteAction`
- `eventGenerationAction`
- `externalApiRefreshAction`
- `newsFetchAction`
- `schedulerAction`
- `collectorAction`
- `pushSendAction`
- `externalChannelAction`
- `recheckExecutionAction`
- `replayExecutionAction`
- `candidateRanking`
- `candidateScore`
- `decisionGenerationAction`
- `pointProposal`
- `finalDirection`
- `entry`
- `stop`
- `takeProfit`
- `tp`
- `riskReward`
- `rr`
- `orderAction`
- `executionAction`
- `autoTradingAction`

## Fail-Closed Rules

- Missing selected analysis/context -> `HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED`.
- `HotResetEventMapper` read exception -> `HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED`.
- No latest persisted Hot Reset event -> `HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED`.
- Latest event exists but source fields are incomplete ->
  `HOT_RESET_EVENT_SOURCE_PARTIAL_REVIEW_ONLY`.
- Event Impact source fields cannot be derived from persisted Hot Reset event
  reads -> `EVENT_IMPACT_SOURCE_MISSING_FAIL_CLOSED`.
- SourceTrace ownership result is absent or incomplete ->
  `SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE_FAIL_CLOSED`.
- Missing event-source id/type/evidence id must stay missing; never synthesize
  them from runtime latest price, quote, kline, skeletons, or display slots.
- Any need to call `HotResetService.executeHotReset(...)`, `HotResetEventMapper.insert(...)`,
  `AssetStateService.recordHotResetEvent(...)`, or
  `AssetStateServiceImpl.recordHotResetEvent(...)` -> boundary-blocked
  fail-closed.
- Any need for event generation, external API refresh, news fetch,
  scheduler/collector trigger, Push send, external channel, Recheck/Replay,
  Candidate, Decision generation, Point, final direction, entry/stop/TP/RR,
  order/execution, auto-trading, or Position Monitor execution ->
  boundary-blocked fail-closed.

## Hot Reset / Event Boundary

This design separates three concepts:

1. `Hot Reset event source evidence`: already-persisted `HotResetEventDO`
   records and read-only count/latest summaries.
2. `Event Impact source status`: read-only status derived from existing event
   impact input/evidence semantics over persisted Hot Reset events.
3. `SourceTrace event-source ownership`: current fail-closed ownership result,
   which may stay incomplete and still be valuable as manual-review evidence.

None of those concepts authorize:

- Hot Reset execution
- Hot Reset state writes
- event generation
- external API refresh
- news fetching
- scheduler/collector trigger
- Push send or external channel
- Recheck/Replay execution
- Candidate / Decision / Point generation
- final direction / entry / stop / TP / RR
- order / execution / auto-trading
- Position Monitor execution

## Implementation Readiness Gate Checklist

The next readiness gate must decide:

1. Whether B-risk minimal implementation is GO or NO-GO.
2. Whether the existing persisted Hot Reset event read path is enough.
3. Whether Event Impact fields can be represented without calling scoring or
   generation behavior.
4. Whether SourceTrace event-source ownership can be surfaced as incomplete
   fail-closed without fabrication.
5. Whether run-baseline Hot Reset summary should be a source field, context
   field, or excluded from the minimal endpoint.
6. Whether one dedicated read-only `Map` endpoint is needed.
7. Whether a minimal dashboard panel is needed.
8. Exact allowed implementation files.
9. Required targeted tests for safety fields, fail-closed rules, SourceTrace
   incomplete ownership, dashboard DOM/copy, and forbidden fields absent.
10. Proof that implementation will not call execution/write/generation/refresh
    paths.
11. Proof that no DTO / Validator / Assembler / Orchestrator, schema/config/pom,
    or new service/domain/mapper/repository ownership family is needed.

## Maximum Future Implementation Scope If Allowed

If the readiness gate returns GO, the maximum future implementation scope should
be:

- one existing controller owner path with at most one minimal read-only
  `Map` endpoint, if needed
- `dashboard.html` minimal status panel/copy/DOM only, if needed
- targeted controller/dashboard tests
- optional tiny existing owner-path assertions in existing tests, if needed
- implementation report docs
- source-of-truth docs

Forbidden implementation files/areas:

- schema/config/pom
- new DTO / Validator / Assembler / Orchestrator
- new service/domain/mapper/repository ownership family
- Hot Reset execution/write code
- event generation / score / decision generation code
- external API/news/scheduler/collector code
- Push/Recheck/Replay code
- Candidate / Point / trading code
- Position Monitor execution code
- P359 / P360

## Next Allowed Action

`Implementation readiness gate for Hot Reset / Event Impact Source review-only status`

Suggested next branch:

`hot-reset-event-impact-source-status-implementation-readiness-gate`

## #830 Audit

- New skeleton created: no.
- Cursor-era assets reused: yes, existing Hot Reset event mapper/entity,
  EventImpact input/evidence read semantics, SourceTrace event-source ownership
  fail-closed result, run-baseline summary, review aggregate display, and
  dashboard KPI context.
- Duplication reduced: yes, the design selects existing owner/read paths and
  keeps new owner families blocked.
- Capability level raised: no.
- Service/runtime/dashboard/API connected: no, design only.
- #830 audit alignment: yes, this avoids DTO / Validator / Assembler /
  Orchestrator expansion and keeps P359/P360 frozen.
