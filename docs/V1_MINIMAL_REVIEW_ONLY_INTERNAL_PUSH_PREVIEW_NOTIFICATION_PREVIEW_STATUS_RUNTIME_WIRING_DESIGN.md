# V1 Minimal Review-Only Internal Push Preview / Notification Preview Status Runtime Wiring Design

## Scope

- Effective baseline: `a06dbb1 docs(runtime): read internal push preview source path (#992)`
- Branch: `minimal-review-only-internal-push-preview-notification-preview-status-runtime-wiring-design`
- Package type: A-risk design
- Current capability level: `REVIEW_ONLY_RUNTIME partial`
- Completed review-only runtime partial slices before this package: 18
- Capability movement: none

This package designs the minimal review-only runtime wiring for `Internal Push preview / notification preview status`. It does not implement an endpoint, dashboard behavior, Push send, external channel delivery, PushSnapshot writes, Recheck / Replay execution, Candidate generation, Decision generation, Point generation, final direction, entry / stop / TP / RR, order / execution, auto-trading, Position Monitor execution, scheduler / collector / external API refresh, schema/config/pom changes, new DTO / Validator / Assembler / Orchestrator, or new service/domain/mapper/repository ownership family.

## Design Result

Result: `GO to implementation readiness gate only`.

The next package may create an implementation readiness gate because the source read found a distinct owner path for the internal push preview surface:

1. `ReviewOnlyCandidatePreviewGuardDTO`
2. `ReviewOnlyInternalPushPreviewAssembler`
3. `ReviewOnlyInternalPushPreviewDTO`
4. dashboard `internalPushPreviewDisplay`
5. no-op external channel policy assets as boundary evidence only
6. P302-P305 closure docs and related tests

The design does not approve implementation directly. The readiness gate must still decide whether a runtime endpoint or dashboard status panel is necessary, whether the existing DTO/assembler are sufficient, and whether the proposal remains distinct from the completed Alert fatigue / notification policy status slice.

## Duplication Decision

Decision: `not duplicate if scoped to internal push preview owner assets; NO-GO if collapsed into generic notification policy`.

The completed Alert fatigue / notification policy status owns the MonitorAlert / notification policy read path:

- `DashboardController -> MonitorService#getRecentAlerts -> MonitorAlertDO`
- `/api/dashboard/alert-fatigue-policy-status`
- `alertFatiguePolicyStatusPanel`

This design owns a different review-only preview path:

- `ReviewOnlyInternalPushPreviewDTO`
- `ReviewOnlyInternalPushPreviewAssembler`
- dashboard `internalPushPreviewDisplay`
- no-op external channel policy evidence
- P302-P305 internal push preview closure docs

Overlap exists only in negative safety semantics such as `notPushSend=true` and `notExternalChannel=true`. The future readiness gate must return `NO-GO` if the proposed implementation repeats alert fatigue / notification policy status, notification cooldown, notification suppression, MonitorAlert state, or generic notification policy health. It may continue only if the implementation remains a projection of internal push preview ownership and disabled-channel boundary evidence.

## Owner Path

Primary owner path:

```text
ReviewOnlyCandidatePreviewGuardDTO
  -> ReviewOnlyInternalPushPreviewAssembler
  -> ReviewOnlyInternalPushPreviewDTO
  -> dashboard internalPushPreviewDisplay
```

Safety boundary evidence:

```text
NoOpOpportunityPushProviderChannelPolicy
  -> NoOpOpportunityPushExternalChannelPolicy
  -> NoOpOpportunityPushMessageEnvelopeAssembler
  -> NoOpOpportunityPushDeliveryPipelinePolicy
  -> NoOpOpportunityPushAuditEnvelopePersistencePort
```

Forbidden adjacent paths, for risk explanation only:

```text
PushSnapshotService.insertAuthoritativeSnapshot(...)
PushSnapshotMapper.insert(...)
PushRecheckServiceImpl.recheck(...)
PushRecheckServiceImpl.replayByDispatch(...)
PushRecheckScheduler.scanAndRecheck(...)
PushRecheckController write / trigger / replay / config endpoints
```

### Existing Asset Reuse Answers

| Question | Design answer |
|---|---|
| Reuse `ReviewOnlyInternalPushPreviewDTO`? | Yes. It is the existing review-only preview carrier and already forces `reviewOnly`, `notTradeInstruction`, `manualReviewRequired`, `recheckRequired`, and `riskActionGuardRequired`. |
| Reuse `ReviewOnlyInternalPushPreviewAssembler`? | Yes. It is the existing owner-path projection from candidate preview guard to internal push preview, including missing/blocked fail-closed behavior. |
| Reuse dashboard `internalPushPreviewDisplay`? | Yes. It is the canonical dashboard display context. Prefer extending this display only if the readiness gate approves a tiny read-only status panel/DOM projection. |
| Reuse no-op external channel policy? | Yes, as boundary evidence only. It proves external channel / provider / envelope / delivery / queue / persistence stay disabled; it is not a permission to wire any external channel. |
| Use PushSnapshot / PushRecheck as implementation path? | No. They are execution-adjacent risk paths, not status owners. |

## Endpoint Decision

Default: do not add a dedicated endpoint in this design package.

Future readiness gate may allow at most one minimal read-only `Map` endpoint if it proves all of the following:

- the endpoint only projects existing internal push preview owner assets;
- the endpoint does not create a new DTO / Validator / Assembler / Orchestrator;
- the endpoint does not call PushSnapshot write paths;
- the endpoint does not call Recheck / Replay execution paths;
- the endpoint does not call external channel, provider delivery, Telegram, email, webhook, app notification, local notification, scheduler, collector, API client refresh, or external API refresh;
- the endpoint does not expose sendable message, order, execution, readiness upgrade, point generation, final direction, entry / stop / TP / RR, or trading authorization.

Candidate future endpoint name, if readiness approves:

```text
GET /api/dashboard/internal-push-preview-notification-status?symbol=BTCUSDT
```

The endpoint must return a minimal read-only `Map`, not a new DTO.

## Dashboard Decision

Default: reuse existing `internalPushPreviewDisplay`.

Future readiness gate may allow a minimal dashboard status panel only if it remains under the internal push preview display context and only adds:

- DOM ids for read-only status values;
- review-only / manual-review / fail-closed copy;
- no Push send / no external channel copy;
- no PushSnapshot write / no Recheck / no Replay copy;
- no Candidate / Decision generation / Point copy;
- no final direction / entry / stop / TP / RR copy;
- no order / execution / auto-trading copy;
- no Position Monitor execution copy;
- Display Slots are not candidate pool copy.

It must not add buttons, links, send actions, notification channel controls, recheck/replay controls, readiness/point controls, order controls, execution controls, auto-trading controls, or any action entry.

## Notification Preview Read Model Gap

Current state: a dynamic notification preview read model is not available.

The only safe notification-preview evidence is the disabled/no-op external channel family and the dashboard display-gate copy. Therefore future implementation may expose `notification preview unavailable / disabled no-op / review-only` status, but it must not render a sendable message, queued notification, delivery payload, Telegram/email/webhook/app/local notification preview, or external provider channel state as executable behavior.

## Status Mapping

| Status | Meaning | Fail-closed? | Boundary |
|---|---|---:|---|
| `INTERNAL_PUSH_PREVIEW_REVIEW_ONLY_READY` | Existing internal push preview DTO/assembler can produce review-only preview status. | No | Read-only preview evidence only. |
| `INTERNAL_PUSH_PREVIEW_MISSING_FAIL_CLOSED` | Preview guard input or preview DTO is missing. | Yes | Do not invent preview status. |
| `INTERNAL_PUSH_PREVIEW_PARTIAL_REVIEW_ONLY` | Some preview owner fields are readable but blockers/risk blockers remain. | No | Partial status only; manual review required. |
| `NOTIFICATION_PREVIEW_REVIEW_ONLY_READY` | Notification preview is represented only as disabled/no-op boundary evidence. | No | No sendable message or delivery payload. |
| `NOTIFICATION_PREVIEW_MISSING_FAIL_CLOSED` | No safe disabled/no-op notification preview evidence is available. | Yes | Do not infer notification channel state. |
| `INTERNAL_PUSH_PREVIEW_ASSEMBLER_REVIEW_ONLY_READY` | Existing assembler is available as the projection owner. | No | Reuse only; do not create another assembler. |
| `NO_OP_EXTERNAL_CHANNEL_POLICY_REVIEW_ONLY_READY` | No-op channel policy confirms channel is disabled and not sendable. | No | Boundary evidence only. |
| `DUPLICATE_ALERT_NOTIFICATION_POLICY_REVIEW_REQUIRED` | Proposed status overlaps completed Alert fatigue / notification policy status. | Yes | Readiness gate must decide GO/NO-GO. |
| `PUSH_SEND_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to send a push is blocked. | Yes | No Push send. |
| `EXTERNAL_CHANNEL_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need for Telegram/email/webhook/app/local/external channel is blocked. | Yes | No external channel. |
| `PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to write PushSnapshot is blocked. | Yes | No snapshot writes. |
| `RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to execute Recheck is blocked. | Yes | No Recheck execution. |
| `REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to execute Replay is blocked. | Yes | No Replay execution. |
| `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to generate/rank Candidate is blocked. | Yes | No Candidate signal. |
| `DECISION_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to generate Decision is blocked. | Yes | No Decision generation. |
| `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to generate Point or entry/stop/TP/RR is blocked. | Yes | No point proposal or numeric trade setup. |
| `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any order/execution/auto-trading path is blocked. | Yes | No trading behavior. |

## Required Safety Fields

Future readiness-approved implementation must expose only negative/read-only safety semantics:

| Field | Required value |
|---|---:|
| `reviewOnly` | `true` |
| `manualReviewOnly` | `true` |
| `notPushSend` | `true` |
| `notExternalChannel` | `true` |
| `notPushSnapshotWrite` | `true` |
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

## Fail-Closed Rules

- Missing `ReviewOnlyInternalPushPreviewDTO` or missing preview-guard input => `INTERNAL_PUSH_PREVIEW_MISSING_FAIL_CLOSED`.
- Blocked/fail-closed preview DTO => `INTERNAL_PUSH_PREVIEW_PARTIAL_REVIEW_ONLY` or the existing `BLOCKED_FAIL_CLOSED` reason must remain visible.
- Missing no-op external channel evidence => `NOTIFICATION_PREVIEW_MISSING_FAIL_CLOSED`.
- Any attempt to infer sendable notification content => `EXTERNAL_CHANNEL_BOUNDARY_BLOCKED_FAIL_CLOSED`.
- Any attempt to call `PushSnapshotService.insertAuthoritativeSnapshot(...)`, `PushSnapshotMapper.insert(...)`, or PushSnapshot write behavior => `PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED`.
- Any attempt to call `PushRecheckServiceImpl.recheck(...)`, `PushRecheckServiceImpl.replayByDispatch(...)`, `PushRecheckScheduler`, or PushRecheck write/update behavior => `RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED` or `REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED`.
- Any attempt to expose `VALID_EXECUTABLE` as readiness or send authorization => `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED`.
- Any attempt to generate Candidate / Decision / Point / final direction / entry / stop / TP / RR / order / execution / auto-trading => corresponding boundary blocked fail-closed status.
- Any implementation that duplicates Alert fatigue / notification policy owner path rather than internal push preview owner path => `DUPLICATE_ALERT_NOTIFICATION_POLICY_REVIEW_REQUIRED` and readiness NO-GO unless a distinct preview-owner value is proven.

## Push Send Boundary

The module is a preview status only.

It must explicitly remain:

- not Push send;
- not external channel;
- not Telegram;
- not email;
- not webhook;
- not app notification;
- not local notification;
- not sendable message;
- not message rendering for delivery;
- not provider selection;
- not delivery pipeline;
- not queue;
- not persistence attempt;
- not PushSnapshot write;
- not Recheck execution;
- not Replay execution;
- not readiness upgrade;
- not Candidate generation;
- not Decision generation;
- not Point generation;
- not final direction / entry / stop / TP / RR;
- not order / execution / auto-trading;
- not Position Monitor execution.

PushSnapshot and PushRecheck may be named only as risk paths in design/readiness/test assertions. They must not be used as the runtime owner path for this status.

## Implementation Readiness Gate Checklist

The next package must answer GO / NO-GO before any implementation:

1. Is `ReviewOnlyInternalPushPreviewDTO` sufficient, with no new DTO?
2. Is `ReviewOnlyInternalPushPreviewAssembler` sufficient, with no new assembler?
3. Can the owner path avoid PushSnapshot writes and PushRecheck execution entirely?
4. Is the status distinct from completed Alert fatigue / notification policy status?
5. Is the endpoint needed at all, or can existing dashboard display context remain static?
6. If endpoint is needed, is it one minimal read-only `Map` endpoint only?
7. If dashboard panel is needed, can it reuse `internalPushPreviewDisplay` and add only status/copy/DOM?
8. Are dynamic notification preview gaps represented as disabled/no-op / missing fail-closed rather than fabricated sendable messages?
9. Are all required safety fields present?
10. Are forbidden fields absent: sendable message, external channel action, PushSnapshot write action, Recheck action, Replay action, Candidate ranking/score, final direction, entry, stop, takeProfit, TP, RR, order action, execution action, auto-trading action?
11. Are schema/config/pom and new service/domain/mapper/repository ownership family changes unnecessary?
12. Are targeted tests limited to endpoint safety flags, fail-closed states, Push send boundary, duplication boundary, forbidden executable/action fields absent, and optional dashboard DOM/copy assertions?

## Maximum Future Implementation Files If Readiness Gate Returns GO

The readiness gate may permit only these candidates:

- `DashboardController.java`: at most one minimal read-only `Map` endpoint, if needed.
- `dashboard.html`: minimal status/copy/DOM under internal push preview display context, if needed.
- Targeted controller/dashboard tests: only if an endpoint or panel is added.
- Existing internal push preview assembler / closure tests: tiny owner-path assertions only, if needed.
- implementation report docs.
- source-of-truth docs.

The readiness gate must forbid:

- new DTO / Validator / Assembler / Orchestrator;
- new service/domain/mapper/repository ownership family;
- schema/config/pom;
- Push send;
- external channel;
- PushSnapshot write;
- Recheck / Replay execution;
- Candidate generation;
- Decision generation;
- Point generation;
- final direction / entry / stop / TP / RR;
- order / execution / auto-trading;
- Position Monitor execution;
- external API refresh / scheduler / collector;
- P359 / P360.

## Next Allowed Action

Next allowed action: `Implementation readiness gate for Internal Push preview / notification preview status`.

Next branch: `internal-push-preview-notification-preview-status-implementation-readiness-gate`.

Allowed next package scope:

- readiness gate docs
- source-of-truth docs

Forbidden next package scope:

- Java business code
- tests
- dashboard business logic
- schema/config/pom
- endpoint implementation
- panel implementation
- Push send
- external channel
- PushSnapshot write
- Recheck / Replay execution
- Candidate / Decision / Point generation
- final direction / entry / stop / TP / RR
- order / execution / auto-trading
- Position Monitor execution
- external API refresh / scheduler / collector
- new DTO / Validator / Assembler / Orchestrator
- new service/domain/mapper/repository ownership family
- P359 / P360

## Overreach Check

No Java business code, tests, dashboard business logic, schema/config/pom, runtime endpoint, panel behavior, Push send, external channel, PushSnapshot write, Recheck/Replay execution, Candidate/Decision/Point generation, final direction, entry/stop/TP/RR, order/execution/auto-trading, Position Monitor execution, scheduler/collector/external refresh, new skeleton owner, P359, or P360 work is performed in this package.
