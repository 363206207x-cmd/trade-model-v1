# V1 Internal Push Preview / Notification Preview Status Implementation Readiness Gate

## Scope

- Effective baseline: `1e15f39 docs(runtime): design internal push preview status wiring (#993)`
- Branch: `internal-push-preview-notification-preview-status-implementation-readiness-gate`
- Package type: A-risk implementation readiness gate
- Current capability level: `REVIEW_ONLY_RUNTIME partial`
- Completed review-only runtime partial slices before this package: 18
- Capability movement: none

This package only makes a GO / NO-GO decision for a future B-risk minimal implementation of `Internal Push preview / notification preview status`. It does not implement an endpoint, dashboard behavior, Push preview status, notification preview status, Push send, external channel delivery, sendable message rendering, PushSnapshot writes, Recheck / Replay execution, Candidate generation, Decision generation, Point generation, final direction, entry / stop / TP / RR, order / execution, auto-trading, Position Monitor execution, external API refresh, scheduler / collector trigger, schema/config/pom changes, new DTO / Validator / Assembler / Orchestrator, or a new service/domain/mapper/repository ownership family.

## Readiness Decision

Decision: `GO to B-risk minimal implementation`.

The next implementation may continue only because the source read and design found a distinct internal push preview owner path that is not fully covered by the completed Alert fatigue / notification policy status slice:

1. `ReviewOnlyCandidatePreviewGuardDTO`
2. `ReviewOnlyInternalPushPreviewAssembler`
3. `ReviewOnlyInternalPushPreviewDTO`
4. dashboard `internalPushPreviewDisplay`
5. no-op external channel policy assets as disabled-channel evidence only

This is not approval for Push send, notification delivery, external channel wiring, PushSnapshot writes, Recheck / Replay execution, Candidate generation, Decision generation, Point generation, or trading behavior. The future B-risk PR must be Draft and must stop for GPT / human review before merge.

## Duplication Decision

Decision: `not duplicate if scoped to internal push preview owner assets; NO-GO if it repeats Alert fatigue / notification policy`.

The completed Alert fatigue / notification policy status owns MonitorAlert / notification policy health:

- `DashboardController -> MonitorService#getRecentAlerts -> MonitorAlertDO`
- `/api/dashboard/alert-fatigue-policy-status`
- `alertFatiguePolicyStatusPanel`

The allowed implementation scope for this module is different:

- `ReviewOnlyInternalPushPreviewDTO`
- `ReviewOnlyInternalPushPreviewAssembler`
- dashboard `internalPushPreviewDisplay`
- no-op external channel policy evidence

The only shared surface is negative safety copy such as `notPushSend=true` and `notExternalChannel=true`. If implementation needs MonitorAlert notification cooldown/suppression/policy status, or only restates the existing alert fatigue status, it must stop as `NO-GO: duplicate with Alert fatigue / notification policy status`.

## Existing Owner Path Requirement

The implementation must reuse this existing owner path:

```text
ReviewOnlyCandidatePreviewGuardDTO
  -> ReviewOnlyInternalPushPreviewAssembler
  -> ReviewOnlyInternalPushPreviewDTO
  -> dashboard internalPushPreviewDisplay
```

No-op external channel assets may be referenced only as read-only safety evidence:

```text
NoOpOpportunityPushProviderChannelPolicy
NoOpOpportunityPushExternalChannelPolicy
NoOpOpportunityPushMessageEnvelopeAssembler
NoOpOpportunityPushDeliveryPipelinePolicy
NoOpOpportunityPushAuditEnvelopePersistencePort
```

PushSnapshot and PushRecheck paths are risk paths only. They must not become implementation owners.

## Endpoint Decision

Dedicated endpoint: allowed only if implementation proves the dashboard needs a runtime projection.

Maximum endpoint shape:

```text
GET /api/dashboard/internal-push-preview-notification-status?symbol=BTCUSDT
```

Rules:

- at most one minimal read-only `Map` endpoint;
- must reuse the existing internal push preview owner path;
- must not add a new DTO / Validator / Assembler / Orchestrator;
- must not call PushSnapshot write paths;
- must not call PushRecheck / Replay / scheduler paths;
- must not call or prepare external channel delivery;
- must not return a sendable message, channel payload, provider payload, queue state, or delivery action.

## Dashboard Decision

Dashboard panel: allowed only as a minimal read-only status surface under or adjacent to existing `internalPushPreviewDisplay`.

Allowed content:

- DOM ids for internal push preview / notification preview status values;
- `review-only` / `manual review only` / `fail-closed` copy;
- `not Push send` / `not external channel` copy;
- `not PushSnapshot write` / `not Recheck` / `not Replay` copy;
- `not Candidate` / `not Decision generation` / `not Point` copy;
- `not final direction` / `not entry / stop / TP / RR` copy;
- `not order / execution / auto-trading` copy;
- `Display Slots are not candidate pool` copy.

Forbidden content:

- buttons, links, or controls for send, channel selection, queue, delivery, PushSnapshot write, Recheck, Replay, Candidate, Point, order, execution, auto-trading, or Position Monitor execution;
- sendable message rendering;
- external notification provider payloads;
- final direction / entry / stop / TP / RR output.

## Allowed Implementation Files

If the B-risk implementation proceeds, the maximum allowed file candidates are:

- `src/main/java/org/example/trademodel/controller/DashboardController.java`: at most one minimal read-only `Map` endpoint, if needed.
- `src/main/resources/templates/dashboard.html`: minimal internal push preview / notification preview status panel, DOM ids, and safety copy only.
- targeted controller/dashboard tests: endpoint safety flags, fail-closed states, Push send boundary, external channel boundary, PushSnapshot write boundary, forbidden executable/action fields absent, and optional dashboard DOM/copy assertions.
- existing internal push preview tests: tiny existing owner-path assertions only.
- implementation report docs.
- source-of-truth docs.

## Forbidden Files And Ownership

The next implementation must not modify:

- schema/config/pom files;
- new DTO / Validator / Assembler / Orchestrator files;
- new service/domain/mapper/repository ownership family files;
- PushSnapshot write-side owner files as an implementation path;
- PushRecheck / Replay / scheduler execution owner files as an implementation path;
- external channel provider / delivery / queue / persistence wiring as an implementation path.

Reusing existing `ReviewOnlyInternalPushPreviewDTO` and `ReviewOnlyInternalPushPreviewAssembler` is allowed. Creating replacements or parallel wrappers is not allowed.

## Required Safety Fields

If implementation returns a status `Map`, it must include these exact safety values:

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

## Required Status Mapping

| Status | Required meaning |
|---|---|
| `INTERNAL_PUSH_PREVIEW_REVIEW_ONLY_READY` | Existing internal push preview owner path can be read as review-only status. |
| `INTERNAL_PUSH_PREVIEW_MISSING_FAIL_CLOSED` | Preview DTO or preview owner input is missing; do not infer status. |
| `INTERNAL_PUSH_PREVIEW_PARTIAL_REVIEW_ONLY` | Preview owner path is partially readable but remains manual-review-only / fail-closed for blocked fields. |
| `NOTIFICATION_PREVIEW_REVIEW_ONLY_READY` | Notification preview is represented only as disabled/no-op evidence. |
| `NOTIFICATION_PREVIEW_MISSING_FAIL_CLOSED` | Safe disabled/no-op notification evidence is missing; do not infer channel state. |
| `INTERNAL_PUSH_PREVIEW_ASSEMBLER_REVIEW_ONLY_READY` | Existing assembler is the read-only projection owner. |
| `NO_OP_EXTERNAL_CHANNEL_POLICY_REVIEW_ONLY_READY` | No-op external channel policy proves disabled boundary only. |
| `DUPLICATE_ALERT_NOTIFICATION_POLICY_REVIEW_REQUIRED` | Proposed work overlaps completed Alert fatigue / notification policy status and needs NO-GO review. |
| `PUSH_SEND_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any Push send requirement is blocked. |
| `EXTERNAL_CHANNEL_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any Telegram/email/webhook/app/local/external channel requirement is blocked. |
| `PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any PushSnapshot write requirement is blocked. |
| `RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any Recheck execution requirement is blocked. |
| `REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any Replay execution requirement is blocked. |
| `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any Candidate generation/ranking/scoring requirement is blocked. |
| `DECISION_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any Decision generation requirement is blocked. |
| `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any Point / entry / stop / TP / RR requirement is blocked. |
| `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any order / execution / auto-trading requirement is blocked. |

## Fail-Closed Rules

- Missing internal preview owner input or DTO => `INTERNAL_PUSH_PREVIEW_MISSING_FAIL_CLOSED`.
- Blocked preview owner fields => `INTERNAL_PUSH_PREVIEW_PARTIAL_REVIEW_ONLY`.
- Missing no-op external channel evidence => `NOTIFICATION_PREVIEW_MISSING_FAIL_CLOSED`.
- Any sendable message, provider payload, queue state, delivery state, or external notification action => `PUSH_SEND_BOUNDARY_BLOCKED_FAIL_CLOSED` and `EXTERNAL_CHANNEL_BOUNDARY_BLOCKED_FAIL_CLOSED`.
- Any PushSnapshot write call or write intent => `PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED`.
- Any Recheck / Replay / scheduler execution call or trigger => `RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED` or `REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED`.
- Any Candidate / Decision / Point / final direction / entry / stop / TP / RR / order / execution / auto-trading requirement => corresponding boundary blocked fail-closed status.
- Any implementation that merely repeats Alert fatigue / notification policy status => `DUPLICATE_ALERT_NOTIFICATION_POLICY_REVIEW_REQUIRED` and `NO-GO`.

## Required Tests For B-Risk Implementation

The next implementation must add or update targeted tests only within the allowed file scope:

- controller test for endpoint existence and all safety flags, if endpoint is added;
- controller test for missing owner input / missing preview / partial owner path fail-closed states;
- controller test that no sendable message, external channel action, PushSnapshot write action, Recheck action, Replay action, Candidate ranking/score, Decision generation action, Point, final direction, entry, stop, takeProfit, TP, RR, order action, execution action, or auto-trading action is exposed;
- dashboard/template test for DOM ids and safety copy, if dashboard panel is added;
- existing internal push preview assembler / closure tests may receive only tiny owner-path assertions.

Full test suite remains required for the B-risk implementation PR, but this readiness gate does not run or change Java tests.

## NO-GO Conditions

The future implementation must stop as NO-GO if it requires any of the following:

- complete duplication of Alert fatigue / notification policy status;
- Push send;
- external channel;
- sendable message;
- PushSnapshot write;
- Recheck execution;
- Replay execution;
- Candidate generation, ranking, or score;
- Decision generation;
- Point generation;
- final direction;
- entry / stop / TP / RR;
- order / execution / auto-trading;
- Position Monitor execution;
- external API refresh / scheduler / collector trigger;
- schema/config/pom changes;
- new DTO / Validator / Assembler / Orchestrator beyond reusing existing DTO / assembler;
- new service/domain/mapper/repository ownership family;
- any inability to prove the module is only internal push preview / notification preview status and not a push action / external notification action.

## Push Send Boundary

This module may only describe preview status. It must remain:

- not Push send;
- not external channel;
- not Telegram / email / webhook / app notification / local notification;
- not sendable message;
- not provider payload;
- not delivery pipeline;
- not queue;
- not persistence attempt;
- not PushSnapshot write;
- not Recheck execution;
- not Replay execution;
- not Candidate generation;
- not Decision generation;
- not Point generation;
- not final direction / entry / stop / TP / RR;
- not order / execution / auto-trading;
- not Position Monitor execution.

## Next Allowed Action

Next allowed action: `Minimal Review-Only Internal Push Preview / Notification Preview Status Runtime Wiring Implementation`.

Next branch: `minimal-review-only-internal-push-preview-notification-preview-status-runtime-wiring-implementation`.

Risk: `B-risk`. The implementation PR must be Draft and must not auto-merge.

## Overreach Check

No Java business code, tests, dashboard business logic, schema/config/pom, endpoint/panel behavior, Push send, external channel, sendable message, PushSnapshot write, Recheck / Replay execution, Candidate generation, Decision generation, Point generation, final direction, entry / stop / TP / RR, order / execution, auto-trading, Position Monitor execution, external API refresh, scheduler / collector trigger, new DTO / Validator / Assembler / Orchestrator, new service/domain/mapper/repository ownership family, P359, or P360 work is performed in this package.
