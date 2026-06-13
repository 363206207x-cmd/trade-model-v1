# V1 Recheck Preview / Recheck Status Source Read

## 1. Executive Summary

This package is source-read only. It does not design, implement, test, or wire
new runtime behavior.

Result: **GO to Design only** for `Recheck preview / recheck status`, with a
strict duplication and execution-boundary review.

The repository has real Push/Recheck owner assets:

- `PushRecheckController`
- `PushRecheckService` / `PushRecheckServiceImpl`
- `PushRecheckScheduler`
- `PushRecheckStatusContract`
- `PushRecheckDispatchConfigService` / implementation
- `PushRecheckLogMapper`
- `PushSnapshotMapper`
- `TmPushRecheckLogDO`
- `PushRecheckLogItemVO`
- `PushRecheckReplaySummaryVO`
- `PushRecheckOpsOverviewVO`
- review aggregate / review page display context
- existing push/recheck/replay/dashboard tests

The same area also contains side-effectful Recheck execution, Replay execution,
scheduler dispatch, MarketQuote refresh, PushSnapshot update, recheck-log write,
and dispatch-config write paths. Those paths are forbidden for any later
review-only runtime slice.

The safe future design question is narrow:

Can a status-only slice read persisted PushRecheck logs, replay-summary
counters, backlog/config/audit status, and review labels without calling
Recheck execution, Replay execution, scheduler dispatch, Push send, external
channel, PushSnapshot write, Candidate/Decision/Point generation, final
direction/entry/stop/TP/RR, order/execution, auto-trading, or Position Monitor
execution?

## 2. Effective Baseline

- User-provided actual main HEAD: `701a154 docs(runtime): select recheck preview source read (#998)`.
- Completed `REVIEW_ONLY_RUNTIME partial` slices before this package: 19.
- Current capability level remains `REVIEW_ONLY_RUNTIME partial`.
- Package risk: A-risk.
- Allowed changes in this package: source-read docs and source-of-truth docs.
- Forbidden changes in this package: Java business code, tests, dashboard
  business logic, schema/config/pom, DTO/Validator/Assembler/Orchestrator,
  service/domain/mapper/repository ownership family, endpoint/panel behavior,
  Recheck execution, Replay execution, scheduler/collector/API refresh, Push
  send, external channel, PushSnapshot write, Candidate/Decision/Point
  generation, final direction/entry/stop/TP/RR, order/execution/auto-trading,
  Position Monitor execution, P359, P360, and capability-level promotion.

## 3. Source Read Files

| Area | Files / assets read | Finding | Reusable for future design? | Risk |
|---|---|---|---|---|
| Controller | `src/main/java/org/example/trademodel/controller/PushRecheckController.java` | Existing `/api/push/recheck` controller has mixed read and execution endpoints. Read endpoints include dispatch config, config audit, replay summary, ops overview, latest log, and log list. Write/execution endpoints include recheck, replay, and dispatch-config update. | Yes, but only read endpoints / path inventory. | High if future design calls `POST` recheck/replay/config update. |
| Service contract | `PushRecheckService.java` | Read methods exist: `getLatestLog`, `listLogs`, `summarizeReplayByDispatch`, `getOpsOverview`. Execution methods also exist: `recheck`, `replayByDispatch`. | Yes, read methods only. | High if execution methods are used. |
| Service implementation | `PushRecheckServiceImpl.java` | `recheck` writes a recheck log and updates PushSnapshot status. `replayByDispatch` re-runs recheck over historical logs. Summary and ops overview read persisted logs/config/audit. | Partial. Summary/overview are candidates; execution/write methods are forbidden. | High due transaction/write paths and `VALID_EXECUTABLE` wording. |
| Scheduler | `PushRecheckScheduler.java` | Scheduled dispatch reads pending snapshots, calls MarketQuote client, then calls `pushRecheckService.recheck`. | No for implementation path; yes as forbidden boundary evidence. | High: scheduler trigger, API refresh, recheck execution. |
| Dispatch config | `PushRecheckDispatchConfigService.java`, `PushRecheckDispatchConfigServiceImpl.java`, config/audit mappers | `getCurrentConfig` and `listRecentAudit` are read-only. `loadOrInit` inserts missing defaults; `updateConfig` writes config and audit. | Partial. Read methods may inform status; write/init methods forbidden. | Medium/high because naive config access can write. |
| Recheck log owner | `PushRecheckLogMapper.java`, `TmPushRecheckLogDO.java`, `PushRecheckLogItemVO.java` | Persisted logs have read methods by push/instruction/batch/recent/status-window and a write `insert`. | Yes, read methods are the strongest owner-path candidates. | High if `insert` or execution status mutation is used. |
| PushSnapshot owner | `PushSnapshotMapper.java`, `TmPushSnapshotDO.java`, schema `tm_push_snapshot` | Snapshot read methods exist, but write/update and trading-adjacent JSON fields also exist. | Partial, only status/backlog reads if design proves safe. | High: PushSnapshot write and entry/stop JSON exposure risk. |
| Status contract | `PushRecheckStatusContract.java`, `RecheckStatusEnum.java` | Existing labels map recheck status to review tags and push status. `VALID_EXECUTABLE` exists but is not a scheduler-pending status in the contract. | Yes as read-only label evidence only. | Medium: wording can be misread as trading authorization. |
| Replay summary | `PushRecheckReplaySummaryVO.java`, `PushRecheckServiceImpl#summarizeReplayByDispatch` | Summary reads existing logs and aggregates counts/status/error. It does not execute replay by itself. | Yes, if named status-only and separated from `replayByDispatch`. | Medium: "replay" name can drift into execution. |
| Ops overview | `PushRecheckOpsOverviewVO.java`, `PushRecheckServiceImpl#getOpsOverview` | Builds read model over config, audit, latest replay summary, and recent logs. | Partial, if no config write/init path is used. | Medium/high due adjacency to dispatch configuration. |
| Dashboard controller context | `DashboardController.java` | Completed `/api/dashboard/review-replay-result-status`, `/api/dashboard/internal-push-preview-notification-status`, and alert fatigue status already expose adjacent review-only boundaries. No dedicated recheck-preview status endpoint found. | Yes as duplication/boundary context. | Medium: duplicates completed slices if scoped too broadly. |
| Dashboard template | `src/main/resources/templates/dashboard.html` | Existing `reviewReplayStatusPanel` and `internalPushPreviewNotificationStatusPanel` exist. No dedicated `recheckPreviewStatusPanel` found. | Yes as display-pattern evidence. | Medium: future panel must not repeat completed Review/Replay or Internal Push panels. |
| Review page | `src/main/resources/static/js/review-page.js` | Review page has Push/Recheck display context and renders logs/status fields. It also exposes raw PushSnapshot JSON fields such as entry/stop/invalidation context. | Display context only. | High if future status exposes trading-adjacent fields. |
| Schema | `src/main/resources/schema.sql` | `tm_push_snapshot`, `tm_push_recheck_log`, `tm_push_recheck_dispatch_config`, and dispatch config audit tables exist. | Yes, read inventory only. | Schema/config changes are forbidden. |
| Tests | `PushRecheckControllerTest`, `PushRecheckServiceImplTest`, `PushRecheckStatusContractTest`, `PushRecheckDispatchConfigServiceImplTest`, `DashboardControllerTest`, `P17LocalFixtureFailClosedTest`, `WatchlistLowFrequencyScanSchedulerTest` | Tests cover ops overview, status contract, execution/write behavior, replay summary, config writes, dashboard boundaries, and fail-closed review-label evidence. | Yes as evidence and future targeted-test precedent. | Future implementation would need new targeted tests; this package adds none. |
| Prior docs | `V1_REVIEW_REPLAY_RESULT_STATUS_SOURCE_READ.md`, `V1_NEXT_MINIMAL_RUNTIME_SLICE_SELECTION_AFTER_INTERNAL_PUSH_PREVIEW_NOTIFICATION_PREVIEW.md` | Existing docs identify Review/Replay result status as completed and select this source-read target. | Yes as duplication and selection context. | Avoid duplicate Review/Replay result status. |

## 4. Existing API / Controller Paths

Existing read paths under `/api/push/recheck`:

- `GET /api/push/recheck/dispatch/config`
- `GET /api/push/recheck/dispatch/config/audit`
- `GET /api/push/recheck/replay/summary`
- `GET /api/push/recheck/ops/overview`
- `GET /api/push/recheck/{pushId}/latest`
- `GET /api/push/recheck/{pushId}/logs`

Existing forbidden write/execution paths under `/api/push/recheck`:

- `POST /api/push/recheck/{pushId}` calls real recheck execution.
- `POST /api/push/recheck/replay` calls real replay-by-dispatch execution.
- `POST /api/push/recheck/dispatch/config` updates dispatch config and audit.

Future design may decide whether a dedicated dashboard status endpoint is needed,
but this source read does not authorize one.

## 5. Read Model Availability

| Read model question | Source-read answer |
|---|---|
| Existing recheck preview read model? | Partial. Persisted logs, latest log, log list, ops overview, replay summary, status contract, and review-page display context exist. No dedicated review-only `Recheck preview status` model was found. |
| Existing recheck status read model? | Partial. `TmPushRecheckLogDO`, `PushRecheckLogItemVO`, `PushRecheckReplaySummaryVO`, and `PushRecheckOpsOverviewVO` can describe persisted status, but they are adjacent to execution/writes. |
| Existing dashboard placeholder / panel / DOM id? | Partial. Review/Replay and Internal Push panels exist; no dedicated Recheck preview/status panel was found. |
| Existing API endpoint / controller path? | Yes, `/api/push/recheck` exists with read and write/execution endpoints. No dedicated dashboard status endpoint was found. |
| Existing review-only / manual review / fail-closed semantics? | Partial. Prior dashboard status endpoints and tests carry review-only/fail-closed boundaries. Recheck-specific dedicated safety flags are missing. |
| Existing no-op / disabled execution policy? | Partial. `PushRecheckStatusContract` and tests show positive labels remain review labels and not scheduler-pending states, but no dedicated no-op Recheck preview policy was found. |
| Need new DTO / Validator / Assembler / Orchestrator for design? | Default no. Source read found existing VO/DO/read assets enough for design inventory; future design must prefer `Map` / existing owner assets if it proceeds. |
| Need schema/config/pom? | No. Existing schema is sufficient for read inventory; schema/config/pom changes are forbidden. |
| Need new service/domain/mapper/repository ownership family? | No. Existing PushRecheck / PushSnapshot owner assets are sufficient for design inventory. |

## 6. Reusable Assets

Reusable owner/read candidates for a future design:

- `PushRecheckLogMapper.selectByPushId`
- `PushRecheckLogMapper.countByPushId`
- `PushRecheckLogMapper.selectByInstructionId`
- `PushRecheckLogMapper.selectByBatchId`
- `PushRecheckLogMapper.selectRecent`
- `PushRecheckLogMapper.countByStatusInWindow`
- `PushRecheckService.getLatestLog`
- `PushRecheckService.listLogs`
- `PushRecheckService.summarizeReplayByDispatch`
- `PushRecheckService.getOpsOverview`
- `PushRecheckStatusContract` status-to-review-label mapping
- `PushRecheckDispatchConfigService.getCurrentConfig`
- `PushRecheckDispatchConfigService.listRecentAudit`
- `PushSnapshotMapper.countPendingRecheckBacklog`
- existing `reviewReplayStatusPanel` and `internalPushPreviewNotificationStatusPanel` as adjacency/duplication context
- review-page Push/Recheck display as context-only evidence
- targeted tests as future coverage precedent

Reusable assets that are **not** safe as implementation paths:

- `PushRecheckService.recheck`
- `PushRecheckService.replayByDispatch`
- `PushRecheckScheduler.recheckPendingPushesScheduled`
- `PushRecheckScheduler.handleOne`
- `MarketQuoteClient.fetch24hTicker`
- `PushRecheckLogMapper.insert`
- `PushSnapshotMapper.updatePushStatus`
- `PushSnapshotMapper.insert`
- `PushRecheckDispatchConfigService.loadOrInit`
- `PushRecheckDispatchConfigService.updateConfig`
- dispatch-config mapper `insert` / `updateValue`
- dispatch-config audit mapper `insert`

## 7. Gaps

- No dedicated `Recheck preview / recheck status` dashboard endpoint was found.
- No dedicated `Recheck preview / recheck status` dashboard panel / DOM id was found.
- No dedicated recheck-preview safety-field contract exists yet.
- No dedicated fail-closed status map exists for missing latest log, missing
  dispatch summary, mapper exception, partial blockers, or scheduler-disabled
  status.
- Existing read and write paths live close together; design must prevent accidental
  execution or config initialization writes.
- `VALID_EXECUTABLE` exists as a persisted/status-contract label, but future
  status must not present it as executable readiness, trading authorization, or
  action permission.
- Review-page display contains PushSnapshot fields that can be trading-adjacent;
  future status must avoid entry/stop/TP/RR, order, execution, and sizing
  semantics.

## 8. Duplication Check

### Review / Replay Result Status

Potential duplication: high if the future slice only reports review result or
replay summary availability.

Existing completed slice already covers:

- `/api/dashboard/review-replay-result-status`
- review result availability
- replay summary availability
- review aggregate/source health
- no replay execution safety flags

Distinct possible gap:

- persisted PushRecheck log/backlog/dispatch-read status
- latest recheck log availability
- recheck-status review-label classification
- Recheck execution and scheduler boundary visibility

Design must return NO-GO if the future module merely duplicates Review / Replay
result status.

### Internal Push Preview / Notification Preview Status

Potential duplication: medium/high if the future slice only describes
`recheckRequired`, preview blockers, no-send, or notification-preview status.

Existing completed slice already covers:

- `/api/dashboard/internal-push-preview-notification-status`
- internal push preview owner assets
- no Push send / no external channel
- no PushSnapshot write
- no Recheck / Replay execution boundary

Distinct possible gap:

- persisted Recheck log/read status after push preview exists
- Recheck dispatch/read status as operational evidence
- latest log and replay-summary counters as read-only status

Design must not re-implement internal push preview or notification policy.

### Alert Fatigue / Notification Policy Status

Potential duplication: medium if the future slice focuses on notification policy,
suppression, or alert fatigue rather than Recheck status.

Existing completed slice already covers notification policy and alert-fatigue
boundaries. Recheck source read may reference it only as duplication/boundary
evidence.

## 9. Forbidden Boundary Findings

Future design must treat the following as blocked fail-closed boundaries:

- Recheck execution
- Replay execution
- scheduler trigger
- collector trigger
- API client refresh
- external API refresh
- Push send
- external channel
- sendable message / provider payload
- PushSnapshot write
- recheck-log write
- dispatch-config write / init write
- Candidate generation / ranking / scoring
- Decision generation
- Point generation
- final direction
- entry / stop / TP / RR
- order / execution / auto-trading
- Position Monitor execution
- missed-opportunity generation/write
- review result generation
- paper order / simulated execution / paper PnL
- executable readiness / trading authorization
- position sizing / reduce / close / stop / reverse guidance
- external AI call / Three AI provider orchestration / final arbiter
- P359 / P360

## 10. Design Risk Notes

Next design may continue only if it can keep the module as status-only:

- use persisted reads and existing read owner assets only;
- classify `VALID_EXECUTABLE` as review-label evidence, not authorization;
- avoid `loadOrInit` config behavior because it can write defaults;
- avoid `replayByDispatch` because it executes rechecks and writes replay logs;
- avoid scheduler owner methods that trigger MarketQuote refresh or execution;
- avoid PushSnapshot fields that expose entry/stop/invalidation/trading action
  semantics;
- add no new DTO / Validator / Assembler / Orchestrator unless the design proves
  existing assets can be reused without creating a new ownership family;
- add no schema/config/pom;
- add no service/domain/mapper/repository family.

If design cannot isolate a distinct Recheck read-only status from completed
Review/Replay or Internal Push slices, it must return NO-GO for duplication.

## 11. Source Read Decision

Decision: **GO to Minimal Review-Only Recheck Preview / Recheck Status Runtime
Wiring Design only**.

This is not an implementation readiness decision. It does not authorize a new
endpoint, dashboard panel, Java business-code change, test change, schema/config
change, DTO/Validator/Assembler/Orchestrator, or owner-family creation.

## 12. Next Allowed Action

Next allowed action:

`Minimal Review-Only Recheck Preview / Recheck Status Runtime Wiring Design`

Next branch:

`minimal-review-only-recheck-preview-recheck-status-runtime-wiring-design`

Design must explicitly decide whether this slice is distinct enough from:

- completed Review / Replay result status;
- completed Internal Push preview / notification preview status;
- completed Alert fatigue / notification policy status.

## 13. Freeze Rule Compliance

- 是否创建新骨架: No.
- 是否复用 Cursor-era / V1 资产: Yes. Existing PushRecheck, PushSnapshot,
  replay-summary, dashboard, review-page, schema, and test assets are inventoried.
- 是否减少重复: Yes. The source read narrows the future candidate to existing
  owner paths and explicitly requires duplication review.
- 是否提升 capability level: No.
- 是否接 service/runtime/dashboard/API: No. Source read only; no runtime wiring.
- 是否符合 #830 审计建议: Yes. It avoids new skeleton owners and prefers existing
  runtime/dashboard/API owner assets.

## 14. Overreach Status

No overreach in this package:

- no Java business code changed;
- no tests changed;
- no dashboard business logic changed;
- no schema/config/pom changed;
- no DTO / Validator / Assembler / Orchestrator added;
- no service/domain/mapper/repository ownership family added;
- no Recheck execution;
- no Replay execution;
- no scheduler / collector / API client refresh;
- no Push send / external channel;
- no PushSnapshot write;
- no Candidate / Decision / Point generation;
- no final direction / entry / stop / TP / RR;
- no order / execution / auto-trading;
- no Position Monitor execution;
- no capability-level promotion;
- no P359 / P360 continuation.
