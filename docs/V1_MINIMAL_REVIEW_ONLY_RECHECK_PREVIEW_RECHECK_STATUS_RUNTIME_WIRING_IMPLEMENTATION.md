# V1 Minimal Review-Only Recheck Preview / Recheck Status Runtime Wiring Implementation

## Result

This B-risk implementation package wires the minimal Recheck preview / Recheck
status surface as review-only runtime visibility.

- PR mode: Draft PR required; no auto-merge.
- Effective baseline: `833b2f6 docs(recheck): gate preview status implementation readiness (#1001)`.
- Capability movement: none. V1 remains `REVIEW_ONLY_RUNTIME partial`.
- Completed slice count: still 19 until follow-up verification and visual
  closure packages are merged.
- Next allowed action after this Draft PR is reviewed and merged:
  `Minimal Review-Only Recheck Preview / Recheck Status Runtime Wiring Verification`.

## Implemented Endpoint / Owner Path

- Endpoint: `GET /api/dashboard/recheck-preview-status`
- Optional query evidence: `pushId`, `dispatchBatchId`,
  `dispatchInstructionId`.
- Controller owner: `DashboardController`
- Runtime owner path:
  - `PushRecheckService#getLatestLog(...)`
  - `PushRecheckService#getOpsOverview(...)`
  - persisted `PushRecheckLogMapper` reads through the service owner path
  - `PushRecheckStatusContract` status-label evidence
  - replay-summary counters inside `PushRecheckOpsOverviewVO`
  - dispatch config / audit read evidence inside `PushRecheckOpsOverviewVO`
  - dashboard `recheckPreviewStatusPanel`

The endpoint returns a simple `Map` status projection. It starts fail-closed
when persisted Recheck log / status-contract / dispatch-read evidence is
missing or unavailable. It never calls `recheck(...)`, `replayByDispatch(...)`,
scheduler dispatch, `MarketQuoteClient.fetch24hTicker(...)`, PushSnapshot
writes, dispatch-config writes, Push send, external channel, Candidate /
Decision / Point generation, final direction, entry / stop / TP / RR, order /
execution, auto-trading, or Position Monitor execution.

The endpoint does not introduce DTO, Validator, Assembler, Orchestrator,
service, domain, mapper, repository, schema, config, or pom ownership.

## Dashboard Panel / DOM

The dashboard adds one minimal read-only panel:

- `recheckPreviewStatusPanel`
- `recheckPreviewRuntimeStatusValue`
- `recheckPreviewPushIdValue`
- `recheckPreviewSourceHealthValue`
- `recheckPreviewStatusValue`
- `recheckStatusReadModelValue`
- `recheckLatestLogValue`
- `recheckReplaySummaryValue`
- `recheckDispatchAuditValue`
- `recheckPreviewReviewOnlyValue`
- `recheckExecutionBoundaryValue`
- `recheckReplaySchedulerBoundaryValue`
- `recheckPushSnapshotBoundaryValue`
- `recheckSignalTradingBoundaryValue`
- `recheckPreviewReasonValue`

The panel copy states that the surface is review-only, manual-review-only,
fail-closed, and limited to persisted Recheck log / status contract /
dispatch-read evidence. It also states not Recheck execution, not Replay
execution, not scheduler dispatch, not collector trigger, not API client
refresh, not MarketQuote refresh, not PushSnapshot write, not dispatch config
write, not Push send, not external channel, not Candidate, not Decision
generation, not Point, not final direction, not entry / stop / TP / RR, not
order / execution / auto-trading, not trading, not executable, and that
Display Slots are not a candidate pool.

No button, Recheck trigger, Replay trigger, scheduler dispatch trigger,
refresh entry, Push entry, external-channel entry, Candidate entry, Point
entry, trading entry, executable payload, or provider payload was added.

## Safety Fields

The endpoint and dashboard status contract expose these safety fields:

- `reviewOnly=true`
- `manualReviewOnly=true`
- `notRecheckExecution=true`
- `notReplayExecution=true`
- `notSchedulerDispatch=true`
- `notCollectorTrigger=true`
- `notApiClientRefresh=true`
- `notMarketQuoteRefresh=true`
- `notPushSnapshotWrite=true`
- `notDispatchConfigWrite=true`
- `notPushSend=true`
- `notExternalChannel=true`
- `notCandidateSignal=true`
- `notDecisionGeneration=true`
- `notPointSignal=true`
- `notFinalDirection=true`
- `notEntryStopTpRr=true`
- `notTradingSignal=true`
- `notExecutable=true`
- `displaySlotsAreCandidatePool=false`

## Status Mapping / Fail-Closed Rules

- `RECHECK_PREVIEW_REVIEW_ONLY_READY`: persisted log, replay-summary
  counters, and dispatch config / audit read evidence are all visible.
- `RECHECK_PREVIEW_MISSING_FAIL_CLOSED`: persisted Recheck owner evidence is
  missing.
- `RECHECK_PREVIEW_PARTIAL_REVIEW_ONLY`: some Recheck owner evidence is
  readable, but the projection is incomplete.
- `RECHECK_STATUS_REVIEW_ONLY_READY`: persisted Recheck status label is
  readable as historical evidence only.
- `RECHECK_STATUS_MISSING_FAIL_CLOSED`: persisted Recheck status is missing or
  the read path is unavailable.
- `RECHECK_LOG_READ_MODEL_REVIEW_ONLY_READY`: persisted Recheck log read model
  is readable.
- `REPLAY_SUMMARY_COUNTER_REVIEW_ONLY_READY`: replay-summary counters are
  readable as counters only, not Replay execution.
- `DISPATCH_CONFIG_AUDIT_REVIEW_ONLY_READY`: dispatch config / audit evidence
  is readable as evidence only, not config write or scheduler dispatch.
- `DUPLICATE_REVIEW_REPLAY_STATUS_REVIEW_REQUIRED`: Review / Replay result
  status remains a duplication boundary, not the owner path.
- `DUPLICATE_INTERNAL_PUSH_PREVIEW_REVIEW_REQUIRED`: Internal Push preview
  status remains a duplication boundary, not the owner path.
- `RECHECK_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED`: Recheck execution stays
  blocked.
- `REPLAY_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED`: Replay execution stays
  blocked.
- `SCHEDULER_DISPATCH_BOUNDARY_BLOCKED_FAIL_CLOSED`: scheduler dispatch stays
  blocked.
- `API_CLIENT_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED`: API client refresh stays
  blocked.
- `MARKET_QUOTE_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED`: MarketQuote refresh
  stays blocked.
- `PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED`: PushSnapshot write stays
  blocked.
- `DISPATCH_CONFIG_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED`: dispatch config write
  stays blocked.
- `PUSH_SEND_BOUNDARY_BLOCKED_FAIL_CLOSED`: Push send stays blocked.
- `EXTERNAL_CHANNEL_BOUNDARY_BLOCKED_FAIL_CLOSED`: external channel stays
  blocked.
- `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED`: Candidate generation / ranking /
  scoring stays blocked.
- `DECISION_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED`: Decision generation stays
  blocked.
- `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED`: Point generation stays blocked.
- `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED`: order / execution / auto-trading
  stays blocked.

## Recheck Execution Boundary

This implementation is Recheck preview / Recheck status only.

It does not execute Recheck, execute Replay, dispatch scheduler work, trigger a
collector, refresh API-client data, refresh MarketQuote data, write
PushSnapshot data, write dispatch config, send Push, connect external channels,
generate Candidate / Decision / Point output, produce final direction, produce
entry / stop / TP / RR, or touch order / execution / auto-trading.

The `PushRecheckStatusContract` value is exposed only as persisted status-label
evidence. Even `RECHECK_VALID_EXECUTABLE` remains historical label evidence,
not executable readiness, trading authorization, or an execution instruction.

## Targeted Tests

`DashboardControllerTest` adds coverage for:

- dashboard panel DOM ids and safety copy;
- endpoint safety fields;
- ready projection from persisted log plus ops overview;
- missing owner evidence fail-closed state;
- read-path exception fail-closed state;
- partial read-only state;
- duplicate Review / Replay and Internal Push preview boundaries;
- Recheck / Replay boundary;
- scheduler dispatch boundary;
- API client / MarketQuote refresh boundary;
- PushSnapshot write boundary;
- dispatch config write boundary;
- Push send / external channel boundary;
- Candidate / Decision / Point / Trading boundary;
- forbidden executable / action fields absent.

## Forbidden Scope Check

No changes were made to:

- schema / config / pom
- new DTO / Validator / Assembler / Orchestrator files
- service / domain / mapper / repository ownership family
- Recheck execution behavior
- Replay execution behavior
- scheduler dispatch behavior
- collector / API client refresh behavior
- MarketQuote refresh behavior
- PushSnapshot write behavior
- dispatch config write/init/update behavior
- Push send behavior
- external channel behavior
- Candidate generation / ranking / scoring
- Decision generation
- Point generation
- final direction
- entry / stop / TP / RR
- order / execution / auto-trading
- Position Monitor execution
- P359 / P360

## #830 Duplicate Skeleton Freeze Audit

- New skeleton created: no.
- Existing V1 assets reused: yes, `PushRecheckService#getLatestLog`,
  `PushRecheckService#getOpsOverview`, `PushRecheckLogMapper` read path,
  `PushRecheckStatusContract`, replay-summary counters, ops overview, and
  dispatch config / audit read evidence.
- Duplicate reduction: yes, the projection is limited to persisted Recheck log
  / status-contract / dispatch-read evidence and treats Review / Replay result
  status, Internal Push preview status, and Alert fatigue / notification
  policy status as duplication boundaries.
- Capability movement: none.
- Runtime/dashboard/API wiring: yes, one minimal read-only endpoint and one
  minimal dashboard status panel.
- #830 audit posture: compliant with duplicate skeleton freeze because it
  wires existing owner paths without new owner families.
