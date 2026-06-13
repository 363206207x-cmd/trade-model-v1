# V1 Minimal Review-Only Internal Push Preview / Notification Preview Status Runtime Wiring Implementation

## Result

This B-risk implementation package wires the minimal Internal Push preview /
notification preview status surface as review-only runtime visibility.

- PR mode: Draft PR required; no auto-merge.
- Effective baseline: `f32c276 docs(runtime): gate internal push preview readiness (#994)`.
- Capability movement: none. V1 remains `REVIEW_ONLY_RUNTIME partial`.
- Completed slice count: still 18 until follow-up verification and visual closure
  packages are merged.
- Next allowed action after this Draft PR is reviewed and merged:
  `Minimal Review-Only Internal Push Preview / Notification Preview Status Runtime Wiring Verification`.

## Implemented Endpoint / Owner Path

- Endpoint: `GET /api/dashboard/internal-push-preview-notification-status?symbol=BTCUSDT`
- Controller owner: `DashboardController`
- Runtime owner path:
  - reuse `ReviewOnlyCandidatePreviewGuardDTO` as the existing upstream owner
    contract reference
  - reuse `ReviewOnlyInternalPushPreviewAssembler`
  - reuse `ReviewOnlyInternalPushPreviewDTO`
  - project into dashboard `internalPushPreviewDisplay`
  - surface no-op external channel policy evidence as disabled-channel status

The endpoint returns a simple `Map` status projection. It deliberately starts
fail-closed when no live `ReviewOnlyCandidatePreviewGuardDTO` owner input is
available, rather than fabricating a notification preview.

The endpoint does not introduce DTO, Validator, Assembler, Orchestrator,
service, domain, mapper, repository, schema, config, or pom ownership.

## Dashboard Panel / DOM

The dashboard adds the minimal read-only panel inside the existing
`internalPushPreviewDisplay` area:

- `internalPushPreviewNotificationStatusPanel`
- `internalPushPreviewRuntimeStatusValue`
- `internalPushPreviewSymbolValue`
- `internalPushPreviewSourceHealthValue`
- `internalPushPreviewStatusValue`
- `internalPushNotificationPreviewStatusValue`
- `internalPushPreviewAssemblerValue`
- `internalPushExternalChannelPolicyValue`
- `internalPushPreviewReviewOnlyValue`
- `internalPushSendBoundaryValue`
- `internalPushSnapshotRecheckBoundaryValue`
- `internalPushSignalBoundaryValue`
- `internalPushPreviewReasonValue`

The panel copy states that the surface is review-only, manual-review-only,
fail-closed, and preview-only. It also states that it is not Push send, not an
external channel, not a sendable message, not provider payload, not
PushSnapshot write, not Recheck execution, not Replay execution, not Candidate,
not Decision generation, not Point, not final direction, not entry/stop/TP/RR,
not order/execution/auto-trading, not Position Monitor execution, not trading,
not executable, and that Display Slots are not a candidate pool.

No button, send entry, external-channel entry, write entry, Recheck entry,
Replay entry, Candidate entry, Point entry, or trading entry was added.

## Safety Fields

The endpoint and dashboard status contract expose these safety fields:

- `reviewOnly=true`
- `manualReviewOnly=true`
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
- `notTradingSignal=true`
- `notExecutable=true`
- `displaySlotsAreCandidatePool=false`

Additional endpoint guard evidence remains negative-only:

- `notSendableMessage=true`
- `notProviderPayload=true`
- `notPositionMonitorExecution=true`
- `notExternalApiRefresh=true`
- `notSchedulerTrigger=true`
- `notCollectorTrigger=true`

## Status Mapping / Fail-Closed Rules

- `INTERNAL_PUSH_PREVIEW_REVIEW_ONLY_READY`: existing internal push preview
  owner DTO is readable and not blocked.
- `INTERNAL_PUSH_PREVIEW_MISSING_FAIL_CLOSED`: owner input / DTO / assembler
  read path is missing or unavailable.
- `INTERNAL_PUSH_PREVIEW_PARTIAL_REVIEW_ONLY`: preview owner path is readable
  with risk blockers that remain manual-review evidence only.
- `NOTIFICATION_PREVIEW_REVIEW_ONLY_READY`: notification preview is status-only
  evidence, not a sendable notification.
- `NOTIFICATION_PREVIEW_MISSING_FAIL_CLOSED`: notification preview evidence is
  missing and must not be fabricated.
- `INTERNAL_PUSH_PREVIEW_ASSEMBLER_REVIEW_ONLY_READY`: existing assembler can
  produce a read-only DTO projection.
- `NO_OP_EXTERNAL_CHANNEL_POLICY_REVIEW_ONLY_READY`: external channel remains
  disabled/no-op evidence only.
- `DUPLICATE_ALERT_NOTIFICATION_POLICY_REVIEW_REQUIRED`: Alert fatigue /
  notification policy is only a duplication comparison, not the owner path.
- `PUSH_SEND_BOUNDARY_BLOCKED_FAIL_CLOSED`: Push send stays blocked.
- `EXTERNAL_CHANNEL_BOUNDARY_BLOCKED_FAIL_CLOSED`: external channel stays
  blocked.
- `PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED`: PushSnapshot write stays
  blocked.
- `RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED`: Recheck execution stays blocked.
- `REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED`: Replay execution stays blocked.
- `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED`: Candidate generation stays blocked.
- `DECISION_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED`: Decision generation stays
  blocked.
- `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED`: Point generation stays blocked.
- `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED`: trading/order/execution stays blocked.

## Push Send Boundary

This implementation is internal preview / notification preview status only.

It does not send Push, connect an external channel, render a sendable message,
render provider payload, write PushSnapshot data, execute Recheck, execute
Replay, generate Candidate / Decision / Point, produce final direction,
produce entry / stop / TP / RR, or touch order / execution / auto-trading.

PushSnapshot, PushRecheck, Replay, scheduler, and external-channel ownership
remain forbidden-boundary evidence only and are not implementation paths.

## Targeted Tests

`DashboardControllerTest` adds coverage for:

- dashboard panel DOM ids and safety copy;
- endpoint safety flags;
- fail-closed missing owner input state;
- internal push preview assembler / DTO owner-path projection;
- no-op external channel policy status;
- Push send boundary;
- external channel boundary;
- PushSnapshot write boundary;
- Recheck / Replay boundary;
- Candidate / Decision generation / Point / Trading boundary;
- forbidden executable / action fields absent.

## Forbidden Scope Check

No changes were made to:

- schema / config / pom
- new DTO / Validator / Assembler / Orchestrator files
- service / domain / mapper / repository ownership family
- Push send behavior
- external channel behavior
- sendable message or provider payload behavior
- PushSnapshot write behavior
- Recheck / Replay execution
- Candidate generation
- Decision generation
- Point generation
- final direction
- entry / stop / TP / RR
- order / execution / auto-trading
- Position Monitor execution
- external API refresh / scheduler / collector trigger
- P359 / P360

## #830 Duplicate Skeleton Freeze Audit

- New skeleton created: no.
- Existing V1 assets reused: yes, `ReviewOnlyInternalPushPreviewDTO`,
  `ReviewOnlyInternalPushPreviewAssembler`, dashboard
  `internalPushPreviewDisplay`, and no-op external channel policy evidence.
- Duplication reduced: yes, the status surface wires existing internal push
  preview assets instead of creating a parallel notification-policy owner.
- Alert fatigue duplicate risk: bounded as comparison-only evidence.
- Capability uplift: no.
- Service / runtime / dashboard / API wiring: yes, one minimal review-only
  existing-owner runtime/dashboard/API status path.
- #830 audit fit: yes, this package wires existing assets and does not expand
  duplicate DTO / Validator / Assembler / Orchestrator / service ownership.

## GPT / Human Review Summary

Review focus for the Draft PR:

- Confirm the endpoint is a status projection only and does not expose a
  sendable message or provider payload.
- Confirm `ReviewOnlyInternalPushPreviewAssembler` is reused directly and no
  new owner family is introduced.
- Confirm the fail-closed default for missing owner input is intentional.
- Confirm dashboard copy does not create a Push send / external channel entry.
- Confirm tests cover safety fields, boundaries, and forbidden fields absent.

## Next Allowed Action

After this B-risk Draft PR is reviewed and merged, the next allowed action is:

```text
Minimal Review-Only Internal Push Preview / Notification Preview Status Runtime Wiring Verification
```

That next package must be A-risk verification docs/source-of-truth only.
