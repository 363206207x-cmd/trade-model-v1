# V1 Hot Reset / Event Impact Source Status Source Read

## Scope

This package is a source-read-only package for `Hot Reset / Event Impact Source review-only status`.

It confirms whether the existing codebase has enough reusable owner paths to design a minimal review-only runtime slice later. It does not implement the status, add endpoints, add dashboard behavior, change tests, or touch schema/config/build files.

Effective execution baseline:

- Actual main HEAD used for this package: `e8e9a9f docs(runtime): select hot reset event source status`.
- Source-of-truth baseline lag is non-blocking because actual `main` is clean and synced.
- Completed `REVIEW_ONLY_RUNTIME partial` slices before this source read: 17.
- Capability level after this package: still `REVIEW_ONLY_RUNTIME partial`.

## Source Read Files

- `src/main/java/org/example/trademodel/service/HotResetService.java`
- `src/main/java/org/example/trademodel/service/impl/HotResetServiceImpl.java`
- `src/main/java/org/example/trademodel/entity/HotResetEventDO.java`
- `src/main/java/org/example/trademodel/mapper/HotResetEventMapper.java`
- `src/main/java/org/example/trademodel/vo/EventImpactInputVO.java`
- `src/main/java/org/example/trademodel/service/SourceTraceEventSourceOwnershipService.java`
- `src/main/java/org/example/trademodel/service/impl/FailClosedSourceTraceEventSourceOwnershipService.java`
- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceEventSourceOwnershipResult.java`
- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceEventSourceOwnershipStatusEnum.java`
- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceEventSourceMissingReasonEnum.java`
- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceEventSourceReviewModeEnum.java`
- `src/main/java/org/example/trademodel/service/impl/EvidenceServiceImpl.java`
- `src/main/java/org/example/trademodel/service/impl/AnalysisAssemblerServiceImpl.java`
- `src/main/java/org/example/trademodel/service/AssetStateService.java`
- `src/main/java/org/example/trademodel/service/impl/AssetStateServiceImpl.java`
- `src/main/java/org/example/trademodel/service/impl/RunBaselineServiceImpl.java`
- `src/main/java/org/example/trademodel/service/impl/ReviewAggregateServiceImpl.java`
- `src/main/java/org/example/trademodel/controller/ReviewController.java`
- `src/main/resources/schema.sql`
- `src/main/resources/templates/dashboard.html`
- `src/main/resources/static/js/review-page.js`
- `src/test/java/org/example/trademodel/service/impl/SourceTraceEventSourceOwnershipServiceTest.java`
- `src/test/java/org/example/trademodel/service/impl/DefaultSourceAssemblerTest.java`
- `src/test/java/org/example/trademodel/service/impl/EvidenceServiceImplTest.java`
- `src/test/java/org/example/trademodel/service/impl/ScoreServiceImplTest.java`
- `src/test/java/org/example/trademodel/service/impl/DecisionServiceImplTest.java`
- `src/test/java/org/example/trademodel/service/impl/AssetStateServiceImplTest.java`
- `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`
- `docs/score-8-event-impact-rule-card.md`
- `docs/V1_NEXT_MINIMAL_RUNTIME_SLICE_SELECTION_AFTER_ACCOUNT_RISK_ACCOUNT_EXPOSURE.md`

## Existing Assets

### Hot Reset rule and write-side boundary

`HotResetService` exposes two methods:

- `shouldTriggerHotReset(int confusedScore, boolean multiTimeframeAligned)`
- `executeHotReset(DecisionContext context, DecisionResult currentResult)`

`HotResetServiceImpl` keeps the current minimal rule local and deterministic: hot reset triggers when confused score is at least 40 and multi-timeframe alignment is false. The `executeHotReset` implementation currently returns the existing decision result unchanged, but the service contract and comments are execution-adjacent.

This source read treats `HotResetService` as risk evidence, not as a future read-only status owner. A later status implementation must not call `executeHotReset` and must not trigger `shouldTriggerHotReset` as generation behavior.

### Persisted Hot Reset event read model

`HotResetEventDO` models already-persisted Hot Reset event data:

- `eventId`
- `analysisId`
- `traceId`
- `symbol`
- `triggerType`
- `triggerValue`
- `decisionId`
- `decisionState`
- `confusedScoreSnapshot`
- `multiTimeframeAlignedSnapshot`
- `triggerReasonCode`
- `triggerReasonText`
- `eventVersion`
- `eventTime`
- `preState`
- `postState`
- `createTime`

`HotResetEventMapper` has one write method and several read methods:

- Write-side: `insert(HotResetEventDO row)`.
- Read-side: `selectLatestByAnalysisId(String analysisId)`.
- Read-side: `countByAnalysisId(String analysisId)`.
- Read-side: `countInWindow(int windowMinutes)`.
- Read-side: `selectTriggerTypeCountsInWindow(int windowMinutes)`.

The read-side mapper methods are reusable for future design. The write-side `insert` method is explicitly forbidden for this review-only status track.

### Asset state Hot Reset summary

`schema.sql` contains Hot Reset metadata in `tm_asset_state`:

- `hot_reset_flag`
- `hot_reset_trigger_type`
- `hot_reset_trigger_value`
- `hot_reset_time`
- `pre_reset_state`
- `post_reset_state`

`schema.sql` also defines `tm_hot_reset_event` and marks it as Hot Reset specific, not a generic event platform.

`AssetStateService.findLatestHotResetSnapshot()` is a read path over the latest Hot Reset snapshot. `AssetStateService.recordHotResetEvent(...)` and `AssetStateServiceImpl.recordHotResetEvent(...)` are write-side paths that update `tm_asset_state` and insert into `tm_hot_reset_event` when an analysis id exists. Those write paths are not allowed for future review-only status implementation.

### Event Impact input and evidence path

`EventImpactInputVO` is a local input carrier with:

- `eventFactHit`
- `eventFactCount`
- `eventLatestTime`
- `eventReasonCode`
- `eventTriggerType`
- `eventVersion`
- `eventTraceId`

Its class comment says it only carries calculable and traceable input and does not carry the scoring formula.

`EvidenceServiceImpl.populateEventImpactInputFromHotReset(...)` reads `HotResetEventMapper.selectLatestByAnalysisId(...)` and `countByAnalysisId(...)` for an already-known analysis id, then fills `EventImpactInputVO`. `EvidenceServiceImpl.appendHotResetEventEvidenceIfExists(...)` also reads the latest event and appends neutral event evidence when a persisted Hot Reset event exists.

This is a reusable read-only event-impact source path. It is not event generation, not news fetching, not external API refresh, and not a score formula expansion.

### Event Impact score context

`docs/score-8-event-impact-rule-card.md` and `ScoreServiceImplTest` confirm that Score-8 event impact is a lightweight negative scoring rule over existing event evidence / event impact input. The tests explicitly protect that the rule does not mean the event system is complete and does not change the decision main path.

Future design must keep the status slice out of score calculation and out of Decision generation / Point / final direction behavior.

### SourceTrace event-source ownership

`SourceTraceEventSourceOwnershipService.resolveEventSourceOwnership(...)` exists.

The current implementation is `FailClosedSourceTraceEventSourceOwnershipService`, which always returns a fail-closed missing-source result:

- `ownershipStatus = INCOMPLETE`
- `missingReason = MISSING_SOURCE`
- `reviewMode = REVIEW_ONLY`
- `eventSource = null`
- `manualReviewRequired = true`
- `notTradeInstruction = true`
- `missingFields` contains `eventSource`

`SourceTraceEventSourceOwnershipServiceTest` verifies that:

- Runtime latest price / kline items do not become event ownership.
- Quote latest price does not become event ownership.
- Earlier skeletons do not become event ownership.
- Missing event source and event uncertainty remain review-only.
- The service/result classes do not expose trading/execution method names.

This is reusable fail-closed SourceTrace event-source evidence. It is not a complete event-source id/type/evidence-id owner and must not be presented as ready event-source ownership.

### Runtime and review display context

`RunBaselineServiceImpl.buildHotResetSummary(...)` reads Hot Reset counts in a time window, trigger type counts, and light system status latest Hot Reset fields. It is a reusable aggregate read context but belongs to the broader runtime baseline owner path.

`ReviewAggregateServiceImpl.toHotReset(...)` reads `AssetStateMapper.selectBySymbol(symbol)` and `HotResetEventMapper.selectLatestByAnalysisId(...)` to build the review-page Hot Reset section. The review summary copy distinguishes current symbol row state from per-analysis event timeline and marks `preResetState` / `postResetState` as a state switch boundary, not an instruction.

`ReviewController` exposes review aggregate paths such as `/api/review/aggregate/{analysisId}/detail?section=hotReset`. This is display context only, not the dedicated runtime status owner.

`review-page.js` renders the review section `sec-hot-reset` with current-row and analysis-event fields.

### Dashboard evidence

`dashboard.html` currently has `kpi-hot-reset` and reads Hot Reset summary fields from system status. It renders a compact KPI such as triggered / not triggered. The dashboard already contains comments clarifying that `tm_asset_state` Hot Reset fields are latest current-row semantics, not a per-analysis event stream.

No dedicated dashboard panel or DOM ids were found for `Hot Reset / Event Impact Source review-only status`.

No dedicated dashboard safety copy was found that covers all of:

- review-only
- manual review only
- fail-closed
- not Hot Reset execution
- not Hot Reset write
- not event generation
- not external API refresh
- not news fetch
- not scheduler / collector trigger
- not Push send / external channel
- not Recheck / Replay execution
- not Candidate / Decision generation / Point
- not final direction / entry / stop / TP / RR
- not order / execution / auto-trading
- not Position Monitor execution
- not executable

## Endpoint And Controller Findings

No dedicated runtime status endpoint was found for `Hot Reset / Event Impact Source review-only status`.

Existing controller/display paths are context only:

- `ReviewController` review aggregate detail can display the review-page Hot Reset section.
- System/run-baseline status can summarize Hot Reset activity.
- Dashboard KPI `kpi-hot-reset` can show triggered / not triggered compactly.

Future design may choose whether a minimal dedicated read-only `Map` endpoint is necessary. If it does, it must only read existing owner paths and must not create DTO / Validator / Assembler / Orchestrator or new service/domain/mapper/repository ownership families.

## Reusable Assets

- `HotResetEventMapper.selectLatestByAnalysisId(...)`.
- `HotResetEventMapper.countByAnalysisId(...)`.
- `HotResetEventMapper.countInWindow(...)`.
- `HotResetEventMapper.selectTriggerTypeCountsInWindow(...)`.
- `HotResetEventDO` persisted event fields.
- `EventImpactInputVO` read-only event-impact input fields.
- `EvidenceServiceImpl` existing Hot Reset event read path into event-impact input / neutral evidence.
- `RunBaselineServiceImpl` Hot Reset summary read context.
- `ReviewAggregateServiceImpl` Hot Reset display summary.
- `review-page.js` `sec-hot-reset` display context.
- `dashboard.html` `kpi-hot-reset` compact KPI.
- `SourceTraceEventSourceOwnershipService` interface.
- `FailClosedSourceTraceEventSourceOwnershipService` fail-closed implementation.
- `SourceTraceEventSourceOwnershipResult` safety flags and missing-source semantics.
- Existing source-trace event-source ownership tests.
- Existing evidence / score tests that define event-impact boundaries.

## Gaps

- No dedicated Hot Reset / Event Impact Source review-only runtime status endpoint exists.
- No dedicated dashboard status panel or complete DOM id set exists for this status.
- Existing `kpi-hot-reset` is only a compact KPI and does not carry full review-only / fail-closed / forbidden-boundary copy.
- SourceTrace event-source ownership is intentionally fail-closed and incomplete; it does not provide source trace evidence id, event source id, or event source type as a complete ready owner.
- Hot Reset write-side paths sit nearby in `AnalysisAssemblerServiceImpl` and `AssetStateServiceImpl.recordHotResetEvent(...)`; future design must explicitly avoid them.
- `HotResetEventMapper.insert(...)` exists and is write-side only.
- Event Impact score exists as Score-8 context; future design must avoid score formula changes and must not become Decision generation, Point generation, final direction, entry / stop / TP / RR, or trading semantics.
- No complete safety field contract exists yet for this status.
- No current live browser / screenshot evidence is part of this source-read package.

## Design Risk Notes

- Hot Reset is execution/write-adjacent by name and by existing write path. Future implementation must be read-only and must not call Hot Reset execution/write methods.
- Event Impact can look like event generation or macro/news integration. Future design must stay on existing persisted Hot Reset event reads and must not fetch news, refresh external APIs, trigger scheduler/collector work, or generate events.
- SourceTrace event-source ownership currently fails closed. Future design can surface that fail-closed state, but must not imply complete event-source ownership.
- ReviewAggregate and review-page Hot Reset content are useful display evidence, but not execution or runtime generation entrypoints.
- Broad Macro-news / event calendar remains deferred because it is closer to external API refresh, news fetch, event generation, and scheduler/collector boundaries.

## Source Read Decision

Decision: `GO_TO_DESIGN_ONLY`.

The codebase has enough existing read-only assets to design a minimal Hot Reset / Event Impact Source review-only status. The safest owner candidates are existing persisted Hot Reset event reads plus the existing fail-closed SourceTrace event-source ownership result. Design must decide the final owner path and whether one minimal read-only endpoint / panel is warranted.

This package does not authorize implementation.

## Future Design Questions

- Should future runtime status read from `HotResetEventMapper` directly through an existing controller owner, or should it reuse a broader owner path such as run-baseline / review aggregate context?
- Should `SourceTraceEventSourceOwnershipResult` be shown as a separate fail-closed event-source ownership status?
- Should `EventImpactInputVO` be treated as display source context only, or a status field source?
- What minimal dashboard panel copy is required so Hot Reset source status cannot be mistaken for Hot Reset execution or event generation?
- Can one minimal read-only `Map` endpoint be added without new DTO / Validator / Assembler / Orchestrator and without a new service/domain/mapper/repository owner family?

## Forbidden Boundary Confirmed

This source read did not:

- modify Java business code
- modify tests
- modify dashboard business logic
- modify schema/config/pom
- execute Hot Reset
- write Hot Reset state
- generate events
- fetch news
- refresh external APIs
- trigger scheduler / collector work
- execute Push send
- connect external channels
- execute Recheck / Replay
- generate Candidate / Decision / Point
- generate final direction / entry / stop / TP / RR
- connect order / execution / auto-trading
- execute Position Monitor
- add DTO / Validator / Assembler / Orchestrator
- add service/domain/mapper/repository ownership families
- continue P359 / P360

## Next Allowed Action

`Minimal Review-Only Hot Reset / Event Impact Source Status Runtime Wiring Design`

Suggested next branch:

`minimal-review-only-hot-reset-event-impact-source-status-runtime-wiring-design`

## #830 Audit

- New skeleton created: no.
- Cursor-era assets reused: yes, existing Hot Reset event mapper/entity, EventImpact input, EvidenceService read path, SourceTrace event-source ownership result, run-baseline summary, review aggregate display, dashboard KPI.
- Duplication reduced: yes, this source read selects existing owner/read paths and keeps new owner families blocked.
- Capability level raised: no.
- Service/runtime/dashboard/API connected: no, source-read only.
- #830 audit alignment: yes, this package avoids DTO / Validator / Assembler / Orchestrator expansion and keeps P359/P360 frozen.
