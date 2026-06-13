# V1 Internal Push Preview / Notification Preview Status Source Read

## Scope

- Baseline: `ea84df3 docs(runtime): select internal push preview source read (#991)`
- Branch: `internal-push-preview-notification-preview-status-source-read`
- Package type: A-risk source read
- Current capability level: `REVIEW_ONLY_RUNTIME partial`
- Completed review-only runtime partial slices before this package: 18
- Capability movement: none

This package only reads source and records whether `Internal Push preview / notification preview status` can become a future minimal review-only runtime slice. It does not implement a status endpoint, dashboard behavior, Push send, external channel delivery, PushSnapshot write behavior, Recheck / Replay execution, Candidate / Decision / Point generation, final direction, entry / stop / TP / RR, order / execution, auto-trading, Position Monitor execution, scheduler / collector / external API refresh, or any new owner family.

## Source Read Result

Result: `GO to design only`.

The source read found a real review-only internal push preview asset family:

- `ReviewOnlyInternalPushPreviewDTO`
- `ReviewOnlyInternalPushPreviewAssembler`
- internal push preview assembler tests
- Candidate / Push review-only MVP closure tests
- dashboard `internalPushPreviewDisplay`
- historical P302 / P303 / P304 / P305 closure docs
- no-op external channel policy assets

The source read also found execution-adjacent PushSnapshot / PushRecheck assets that must not become the status owner without a future readiness gate. Those assets contain writes, scheduler-driven recheck behavior, replay behavior, external market quote fetch, and execution-like status names such as `VALID_EXECUTABLE`.

## Source Read Files

| Area | Files / assets read | Finding |
|---|---|---|
| Internal Push preview DTO | `src/main/java/org/example/trademodel/dto/push/ReviewOnlyInternalPushPreviewDTO.java` | Existing DTO is reusable. It carries `reviewOnly=true`, `notTradeInstruction=true`, `manualReviewRequired=true`, `recheckRequired=true`, `riskActionGuardRequired=true`, `failClosed`, `blocked`, reasons, and allowed next step. It has no send, order, execution, entry, stop, TP, RR, external channel, or notification-send field. |
| Internal Push preview assembler | `src/main/java/org/example/trademodel/service/push/ReviewOnlyInternalPushPreviewAssembler.java` | Existing assembler is reusable as a read-only semantic source. It maps missing / blocked preview guard input to `BLOCKED_FAIL_CLOSED` and safe input to `REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK`. It is not Spring-wired and does not call external providers. |
| Internal Push preview tests | `src/test/java/org/example/trademodel/service/push/ReviewOnlyInternalPushPreviewAssemblerTest.java` | Existing tests cover review-only flags, fail-closed behavior, defensive copies, no Spring context dependency, and absence of MarketQuote / Binance / external / send / notification / order / execution / entry-stop-TP-RR behavior. |
| Candidate / Push closure tests | `src/test/java/org/example/trademodel/service/push/CandidatePushReviewOnlyMvpClosureTest.java` | Existing test proves the review-only chain reaches `ReviewOnlyInternalPushPreviewDTO` and dashboard display gate without external channel, sendable message, readiness, point, order, execution, auto-trading, entry, stop, TP, RR, or Display Slots as candidate pool. |
| Dashboard display gate | `src/main/resources/templates/dashboard.html` `internalPushPreviewDisplay` section | Existing static dashboard display gate states review-only preview, not trade instruction, manual review required, recheck required, Risk Action Guard required, external channel disabled, no Telegram / email / webhook / app / local notification, no readiness / point / entry / stop / TP / RR, no external channel message, no sendable message, no order, no execution, no auto-trading. |
| Historical closure docs | `docs/PHASE_P302_INTERNAL_PUSH_PREVIEW_RECHECK_HANDOFF_REVIEW_ONLY_CLOSURE.md`, `docs/PHASE_P303_PUSH_PREVIEW_BEFORE_EXTERNAL_CHANNEL_CLOSURE.md`, `docs/PHASE_P304_DASHBOARD_INTERNAL_PUSH_PREVIEW_DISPLAY_GATE_CLOSURE.md`, `docs/PHASE_P305_CANDIDATE_PUSH_REVIEW_ONLY_MVP_CLOSURE.md` | Existing docs already freeze the internal push preview as review-only, manual-review-required, fail-closed, and before external channel. They explicitly leave external send, readiness, point, order, execution, and entry-stop-TP-RR out of scope. |
| No-op external channel policy | `src/main/java/org/example/trademodel/service/watchlistscan/NoOpOpportunityPushExternalChannelPolicy.java` and related no-op channel / provider / envelope / delivery / audit policy assets | Existing no-op family is strong safety evidence. It keeps external channel disabled and returns no-op evidence, but it is not permission to connect Telegram, email, webhook, app notification, local notification, provider delivery, queue, or persistence. |
| No-op channel tests | `src/test/java/org/example/trademodel/service/watchlistscan/NoOpOpportunityPushExternalChannelPolicyTest.java` and related no-op tests | Existing tests assert disabled/no-op external channel semantics and no provider/live-send behavior. |
| Review-only opportunity push | `src/main/java/org/example/trademodel/service/watchlistscan/DefaultOpportunityPushRule.java`, `src/main/java/org/example/trademodel/dto/watchlistscan/OpportunityPushDTO.java` | Existing review-only DTO/rule marks manual review, not trade instruction, external push not sent, readiness not upgraded, trading action not created, and entry-stop-TP-RR not generated. This is adjacent evidence, not a runtime notification preview status owner yet. |
| PushSnapshot write-side | `src/main/java/org/example/trademodel/service/PushSnapshotService.java`, `src/main/java/org/example/trademodel/mapper/PushSnapshotMapper.java`, `src/main/java/org/example/trademodel/entity/TmPushSnapshotDO.java`, `src/main/resources/schema.sql` | Existing PushSnapshot family contains write behavior (`insertAuthoritativeSnapshot`, mapper `insert`) and execution-adjacent fields such as trigger price, entry zone, stop zone, execution feasibility, and account risk snapshot linkage. Future design must not use it as a write path and must not surface executable guidance. |
| PushRecheck execution side | `src/main/java/org/example/trademodel/service/impl/PushRecheckServiceImpl.java`, `src/main/java/org/example/trademodel/controller/PushRecheckController.java`, `src/main/java/org/example/trademodel/service/PushRecheckScheduler.java`, `src/main/java/org/example/trademodel/service/PushRecheckStatusContract.java` | Existing Recheck assets include transactional recheck writes, replay, scheduler-triggered market quote fetch, push status updates, and status names such as `VALID_EXECUTABLE`. They are forbidden for this status except as risk evidence. |
| Push / Recheck tests | `src/test/java/org/example/trademodel/service/impl/PushRecheckServiceImplTest.java`, `src/test/java/org/example/trademodel/controller/PushRecheckControllerTest.java`, `src/test/java/org/example/trademodel/service/PushRecheckStatusContractTest.java` | Existing tests prove write/update/replay/dispatch/config behavior exists and must remain outside a review-only preview status. |
| Alert fatigue duplicate check | `docs/V1_ALERT_FATIGUE_NOTIFICATION_POLICY_STATUS_*` and source-of-truth entries | Existing Alert fatigue / notification policy status covers MonitorAlert notification policy, not internal push preview. There is partial overlap on "not Push send / not external channel"; future design must NO-GO if it collapses into generic notification policy status rather than internal push preview ownership. |

## Existing Owner Path Candidates

Primary reusable semantic owner path:

1. `ReviewOnlyCandidatePreviewGuardDTO`
2. `ReviewOnlyInternalPushPreviewAssembler`
3. `ReviewOnlyInternalPushPreviewDTO`
4. dashboard `internalPushPreviewDisplay`
5. P302-P305 review-only closure docs and tests

Safety boundary evidence:

1. no-op external channel policy family
2. no-op provider / envelope / delivery / audit policy assets
3. no-op external channel tests

Execution-adjacent assets that must not become owner paths without a future GO:

1. `PushSnapshotService.insertAuthoritativeSnapshot(...)`
2. `PushSnapshotMapper.insert(...)`
3. `PushRecheckServiceImpl.recheck(...)`
4. `PushRecheckServiceImpl.replayByDispatch(...)`
5. `PushRecheckScheduler.scanAndRecheck(...)`
6. `PushRecheckController` write / trigger / replay / config endpoints

## Existing Dashboard / API Evidence

Dashboard:

- Existing display area: `internalPushPreviewDisplay`
- Existing copy: review-only preview, not trade instruction, manual review required, recheck required, Risk Action Guard required, external channel disabled, no Telegram / email / webhook / app / local notification, no readiness / point / entry / stop / TP / RR, no external channel message, no sendable message, no order, no execution, no auto-trading.
- Missing: dynamic status panel/value DOM ids dedicated to internal push preview / notification preview status.

API:

- No dedicated runtime status endpoint for internal push preview / notification preview status was found.
- Existing DTO/assembler are plain Java review-only assets, not a controller/runtime endpoint today.
- PushRecheck controller endpoints exist, but they include execution/write/replay/config paths and must not be reused as the preview status owner in this source-read package.

## Existing Safety Semantics

Already present:

- `reviewOnly=true`
- `notTradeInstruction=true`
- `manualReviewRequired=true`
- `failClosed` / `blocked`
- `recheckRequired=true`
- `riskActionGuardRequired=true`
- external channel disabled / no-op semantics
- no sendable message / no external channel message
- no readiness / point / entry / stop / TP / RR in the dashboard display gate
- no order / execution / auto-trading in the dashboard display gate
- Watchlist Pool is the candidate source, not Display Slots

Missing as explicit runtime status fields today:

- `notPushSend=true`
- `notExternalChannel=true`
- `notPushSnapshotWrite=true`
- `notRecheckExecution=true`
- `notReplayExecution=true`
- `notCandidateSignal=true`
- `notDecisionGeneration=true`
- `notPointSignal=true`
- `notFinalDirection=true`
- `notEntryStopTpRr=true`
- `notOrderExecutionAutoTrading=true`
- `notPositionMonitorExecution=true`
- `notExternalApiRefresh=true`
- `notSchedulerTrigger=true`
- `notCollectorTrigger=true`
- `notExecutable=true`
- `displaySlotsAreCandidatePool=false`

Those missing fields should be considered design candidates only. This source-read package does not add them.

## Risk Inventory

| Risk | Evidence | Source-read decision |
|---|---|---|
| Push send | Dashboard says no Telegram / email / webhook / app / local notification and no external channel message; no-op policies disable channel. | Must remain forbidden. |
| External channel | No-op external channel DTO/policy assets exist but are disabled-only. | May be cited as safety evidence only, not as a channel integration. |
| PushSnapshot write | `PushSnapshotService.insertAuthoritativeSnapshot(...)` and `PushSnapshotMapper.insert(...)` write snapshots. | Forbidden for future status unless explicitly excluded by design/readiness. |
| Recheck execution | `PushRecheckServiceImpl.recheck(...)` writes logs and updates push status. | Forbidden. |
| Replay execution | `PushRecheckServiceImpl.replayByDispatch(...)` invokes recheck from historical dispatch data. | Forbidden. |
| Scheduler / collector / external refresh | `PushRecheckScheduler` fetches market quote data and invokes recheck. | Forbidden. |
| Candidate / Decision / Point generation | Existing preview chain is downstream of candidate preview guard and upstream of readiness/point, but tests block point/trading behavior. | Future status must not generate or rank candidates, decisions, or points. |
| Final direction / entry / stop / TP / RR | PushSnapshot entity has trigger/entry/stop/execution feasibility fields; dashboard display gate says no readiness/point/entry/stop/TP/RR. | Future status must not surface these as actionable guidance. |
| Order / execution / auto-trading | Dashboard display gate and tests forbid order/execution/auto-trading. | Forbidden. |
| Position Monitor execution | No owner path requires Position Monitor. | Forbidden. |

## Reusable Assets

- Existing `ReviewOnlyInternalPushPreviewDTO` can carry review-only preview status semantics.
- Existing `ReviewOnlyInternalPushPreviewAssembler` can map preview-guard input to review-only or fail-closed preview state.
- Existing tests already cover many source-read safety requirements and negative dependencies.
- Existing dashboard `internalPushPreviewDisplay` gives a static, explicit safety-copy anchor.
- Existing no-op external channel policy family gives a strong disabled-channel boundary.
- Historical P302-P305 docs define the handoff before external channel and Candidate / Push review-only closure.

## Gaps

- No dedicated runtime status endpoint for `Internal Push preview / notification preview status`.
- No dynamic dashboard status panel/value DOM ids for this status beyond the existing static `internalPushPreviewDisplay`.
- No notification preview read model distinct from no-op external channel and display-gate copy.
- No runtime status test coverage for endpoint safety flags because no endpoint exists.
- No explicit runtime safety-field map for `notPushSend`, `notExternalChannel`, `notPushSnapshotWrite`, `notRecheckExecution`, `notReplayExecution`, `notCandidateSignal`, `notDecisionGeneration`, `notPointSignal`, `notFinalDirection`, `notEntryStopTpRr`, `notTradingSignal`, `notExecutable`, and `displaySlotsAreCandidatePool=false`.
- PushSnapshot and PushRecheck assets exist but include write/execution/replay/scheduler/external-refresh risks and cannot be treated as status owners without a future narrowly scoped design/readiness decision.

## Duplication Check

Duplication risk: partial overlap, not a full duplicate today.

The completed Alert fatigue / notification policy status slice owns MonitorAlert notification policy status and no-Push/no-external-channel boundaries. Internal Push preview / notification preview is different because it has a separate existing owner set:

- `ReviewOnlyInternalPushPreviewDTO`
- `ReviewOnlyInternalPushPreviewAssembler`
- `internalPushPreviewDisplay`
- Candidate / Push review-only closure tests and docs
- no-op external channel policy assets

Future design must return `NO-GO` if the proposed status merely repeats Alert fatigue / notification policy status. It may continue only if it remains a distinct internal push preview / notification preview read-only projection over the existing preview assets and disabled-channel boundary.

## Design Risk Notes

- The safest next step is design only.
- Default future path should reuse existing preview DTO/assembler/dashboard/no-op-channel assets.
- Default future path should not introduce new DTO / Validator / Assembler / Orchestrator. `ReviewOnlyInternalPushPreviewDTO` and `ReviewOnlyInternalPushPreviewAssembler` already exist.
- Default future path should not introduce schema/config/pom changes.
- Default future path should not introduce service/domain/mapper/repository ownership family.
- If a future endpoint is considered, it must be a minimal read-only `Map` endpoint and must not call `PushSnapshotService` write methods, `PushRecheckServiceImpl.recheck`, `PushRecheckServiceImpl.replayByDispatch`, scheduler paths, external provider refresh, or external channel delivery.
- If future design cannot keep Push send, external channel, PushSnapshot write, Recheck/Replay execution, Candidate/Decision/Point generation, final direction, entry/stop/TP/RR, order/execution/auto-trading, and Position Monitor execution out of scope, the readiness gate must return NO-GO.

## Next Allowed Action

Next allowed action: `Minimal Review-Only Internal Push Preview / Notification Preview Status Runtime Wiring Design`.

Next branch: `minimal-review-only-internal-push-preview-notification-preview-status-runtime-wiring-design`.

Allowed next package scope:

- design docs
- source-of-truth docs

Forbidden next package scope:

- Java business code
- tests
- dashboard business logic
- schema/config/pom
- Push send
- external channel
- PushSnapshot write
- Recheck / Replay execution
- Candidate / Decision / Point generation
- final direction / entry / stop / TP / RR
- order / execution / auto-trading
- Position Monitor execution
- external API refresh / scheduler / collector
- new DTO / Validator / Assembler / Orchestrator unless existing assets are explicitly reused and no new skeleton is created
- new service/domain/mapper/repository ownership family
- P359 / P360

## Overreach Check

No Java business code, tests, dashboard business logic, schema/config/pom, runtime endpoint, panel behavior, Push send, external channel, PushSnapshot write, Recheck/Replay execution, Candidate/Decision/Point generation, final direction, entry/stop/TP/RR, order/execution/auto-trading, Position Monitor execution, scheduler/collector/external refresh, new skeleton owner, P359, or P360 work is performed in this package.
