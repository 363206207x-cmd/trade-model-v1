# V1 Minimal Review-Only Recheck Preview / Recheck Status Runtime Wiring Design

## 1. Scope

- Effective baseline: `3dfd350 docs(recheck): read preview status source path (#999)`.
- Branch: `minimal-review-only-recheck-preview-recheck-status-runtime-wiring-design`.
- Package type: A-risk design.
- Current capability level: `REVIEW_ONLY_RUNTIME partial`.
- Completed review-only runtime partial slices before this package: 19.
- Capability movement: none.

This package designs the minimal review-only runtime wiring for `Recheck preview / recheck status`. It does not implement an endpoint, dashboard behavior, service/runtime wiring, Recheck execution, Replay execution, scheduler dispatch, collector trigger, API-client refresh, MarketQuote refresh, Push send, external channel, PushSnapshot write, dispatch-config write, Candidate generation/ranking/scoring, Decision generation, Point generation, final direction, entry / stop / TP / RR, order / execution, auto-trading, Position Monitor execution, schema/config/pom changes, new DTO / Validator / Assembler / Orchestrator, or new service/domain/mapper/repository ownership family.

## 2. Design Result

Result: `GO to implementation readiness gate only`.

The source read found enough existing owner assets for a narrowly scoped read-only status design:

1. persisted `tm_push_recheck_log` reads through `PushRecheckLogMapper`;
2. `PushRecheckService#getLatestLog` and `PushRecheckService#listLogs`;
3. `PushRecheckService#summarizeReplayByDispatch` as counter-only summary;
4. `PushRecheckService#getOpsOverview` as read-only operational summary, provided it does not use write/init paths;
5. `PushRecheckStatusContract` as status-label evidence only;
6. `PushRecheckDispatchConfigService#getCurrentConfig` and `listRecentAudit` as config/audit evidence only;
7. `PushSnapshotMapper#countPendingRecheckBacklog` as backlog count evidence only;
8. existing dashboard / review-page / test assets as display and duplication context.

This design does not approve implementation directly. The next package must be an implementation readiness gate that decides GO / NO-GO and confirms whether a minimal dedicated endpoint/panel is needed at all.

## 3. Duplication Decision

Decision: `not duplicate only if scoped to persisted Recheck log / status contract / dispatch-read evidence; NO-GO if it collapses into Review / Replay result status or Internal Push preview status`.

### Review / Replay Result Status

The completed Review / Replay result status slice already owns:

- `/api/dashboard/review-replay-result-status`;
- review result availability;
- ReviewAggregate summary/detail availability;
- replay summary availability as review/replay evidence;
- no replay execution safety fields and dashboard copy.

This Recheck design is distinct only if it focuses on persisted Recheck logs and PushRecheck operational evidence:

- latest Recheck log availability;
- log-list/read-model health;
- Recheck status review-label classification;
- replay-summary counters as persisted-log counters, not execution;
- dispatch config/audit read evidence;
- Recheck execution and scheduler dispatch boundary visibility.

If the future proposal only restates ReviewResult, ReviewAggregate, or generic replay availability, the readiness gate must return `NO-GO: duplicate with Review / Replay result status`.

### Internal Push Preview / Notification Preview Status

The completed Internal Push preview / notification preview status slice already owns:

- `/api/dashboard/internal-push-preview-notification-status`;
- `ReviewOnlyInternalPushPreviewAssembler -> ReviewOnlyInternalPushPreviewDTO`;
- preview-only and no-send boundaries;
- no PushSnapshot write, no Recheck execution, and no Replay execution safety fields.

This Recheck design is distinct only if it reads persisted Recheck log/status evidence after or around a push, rather than re-implementing preview ownership. If the future proposal only reports `recheckRequired`, preview blockers, notification preview, or no-send policy, the readiness gate must return `NO-GO: duplicate with Internal Push preview / notification preview status`.

### Alert Fatigue / Notification Policy Status

The completed Alert fatigue / notification policy status slice owns MonitorAlert / policy status. This Recheck design may reference it only as a comparison boundary. It must not implement notification suppression, alert fatigue, or notification policy health.

## 4. Owner Path

Primary read-only owner path:

```text
PushRecheckController GET read paths
  -> PushRecheckService read methods only
  -> PushRecheckLogMapper persisted-log reads
  -> TmPushRecheckLogDO / PushRecheckLogItemVO
  -> PushRecheckStatusContract review-label mapping
  -> PushRecheckReplaySummaryVO counters
  -> PushRecheckOpsOverviewVO read summary
  -> future readiness-approved Recheck preview / status projection
```

Read-only support evidence:

```text
PushRecheckDispatchConfigService#getCurrentConfig
PushRecheckDispatchConfigService#listRecentAudit
PushSnapshotMapper#countPendingRecheckBacklog
dashboard / review-page display context
existing PushRecheck / dashboard tests
```

Forbidden adjacent paths, for boundary evidence only:

```text
PushRecheckService#recheck(...)
PushRecheckService#replayByDispatch(...)
PushRecheckScheduler#recheckPendingPushesScheduled(...)
PushRecheckScheduler#handleOne(...)
MarketQuoteClient#fetch24hTicker(...)
PushRecheckLogMapper#insert(...)
PushSnapshotMapper#insert(...)
PushSnapshotMapper#updatePushStatus(...)
PushRecheckDispatchConfigService#loadOrInit(...)
PushRecheckDispatchConfigService#updateConfig(...)
dispatch config mapper insert/update
dispatch config audit mapper insert
```

### Existing Asset Reuse Answers

| Question | Design answer |
|---|---|
| Can this become a minimal read-only status? | Yes, only as a persisted-log/status-contract/ops-read projection and only after readiness gate approval. |
| Is there an independent owner path? | Partial but sufficient for readiness: persisted Recheck logs, latest/log-list reads, replay-summary counters, ops overview, status contract, and dispatch config/audit reads. |
| Must it read persisted Recheck logs? | Yes. Persisted Recheck logs are the strongest canonical source and avoid live execution. |
| Must it read latest/log-list reads? | Yes, if implementation proceeds; these are the most direct preview/status evidence. |
| Must it read replay-summary counters only? | Yes. Counters may be read, but `replayByDispatch(...)` must stay forbidden. |
| Must it read ops overview / status contract? | Yes, as read-only summary/label evidence only. |
| Must it read dispatch config/audit evidence only? | Yes. `getCurrentConfig` / `listRecentAudit` are acceptable; `loadOrInit` / `updateConfig` are not. |
| Need new DTO / Validator / Assembler / Orchestrator? | No. Future implementation, if approved, must use `Map` or existing VO/DO/read assets. |
| Need schema/config/pom? | No. Existing schema is sufficient; changes are forbidden. |
| Need new service/domain/mapper/repository ownership family? | No. Existing PushRecheck / PushSnapshot owner assets are sufficient. |

## 5. Endpoint Decision

Default: do not add a dedicated endpoint in this design package.

The implementation readiness gate may allow at most one minimal read-only `Map` endpoint if it proves all of the following:

- the endpoint only reads existing Recheck owner assets;
- the endpoint does not call `recheck(...)`;
- the endpoint does not call `replayByDispatch(...)`;
- the endpoint does not trigger `PushRecheckScheduler`;
- the endpoint does not call `MarketQuoteClient.fetch24hTicker`;
- the endpoint does not write `PushSnapshot`, Recheck log, dispatch config, or dispatch audit;
- the endpoint does not send Push, use external channel, generate Candidate / Decision / Point, expose final direction / entry / stop / TP / RR, or connect trading behavior;
- the endpoint is distinct from completed Review / Replay result status and Internal Push preview status.

Candidate future endpoint, if readiness approves:

```text
GET /api/dashboard/recheck-preview-status?symbol=BTCUSDT
```

An existing `/api/push/recheck` read path may be reused instead if the readiness gate decides a dedicated dashboard endpoint is unnecessary.

## 6. Dashboard Decision

Default: do not add a dashboard panel in this design package.

The implementation readiness gate may allow a minimal dashboard Recheck preview/status panel only if it adds read-only status/copy/DOM and remains distinct from:

- `reviewReplayStatusPanel`;
- `internalPushPreviewNotificationStatusPanel`;
- `alertFatiguePolicyStatusPanel`.

Allowed future panel content:

- Recheck preview/status runtime value;
- latest persisted Recheck log availability;
- persisted log count/list availability;
- replay-summary counter availability;
- dispatch config/audit read evidence;
- status contract / review-label evidence;
- review-only / manual-review / fail-closed copy;
- not Recheck execution / not Replay execution / not scheduler dispatch / not MarketQuote refresh copy;
- not PushSnapshot write / not dispatch config write copy;
- not Push send / not external channel copy;
- not Candidate / Decision / Point / Trading copy.

Forbidden future panel content:

- buttons;
- Recheck trigger;
- Replay trigger;
- scheduler dispatch trigger;
- config update controls;
- Push send / external channel controls;
- sendable message or provider payload;
- Candidate ranking/scoring;
- final direction;
- entry / stop / TP / RR;
- order / execution / auto-trading controls.

## 7. Status Mapping

| Status | Meaning | Fail-closed? | Boundary |
|---|---|---:|---|
| `RECHECK_PREVIEW_REVIEW_ONLY_READY` | Persisted latest/log-list Recheck evidence is readable for status display. | No | Read-only preview/status only. |
| `RECHECK_PREVIEW_MISSING_FAIL_CLOSED` | Required Recheck preview source, push id, symbol, or latest evidence is missing. | Yes | Do not infer execution status. |
| `RECHECK_PREVIEW_PARTIAL_REVIEW_ONLY` | Some Recheck evidence is readable but logs, counters, or review labels are partial. | No | Manual review required. |
| `RECHECK_STATUS_REVIEW_ONLY_READY` | Recheck status contract can map persisted status into review-label evidence. | No | Review label only, not execution authorization. |
| `RECHECK_STATUS_MISSING_FAIL_CLOSED` | Persisted status is missing, unknown, or unsupported. | Yes | Do not invent PASS/WAIT/BLOCKED labels. |
| `RECHECK_LOG_READ_MODEL_REVIEW_ONLY_READY` | `PushRecheckLogMapper` read methods can supply persisted log evidence. | No | Reads only; no insert. |
| `REPLAY_SUMMARY_COUNTER_REVIEW_ONLY_READY` | `summarizeReplayByDispatch` can aggregate existing logs without executing replay. | No | Counter-only; no replay execution. |
| `DISPATCH_CONFIG_AUDIT_REVIEW_ONLY_READY` | Existing config/audit reads can show dispatch-read evidence. | No | No init/update. |
| `DUPLICATE_REVIEW_REPLAY_STATUS_REVIEW_REQUIRED` | Proposed status overlaps completed Review / Replay result status. | Yes | Readiness gate must decide GO/NO-GO. |
| `DUPLICATE_INTERNAL_PUSH_PREVIEW_REVIEW_REQUIRED` | Proposed status overlaps completed Internal Push preview status. | Yes | Readiness gate must decide GO/NO-GO. |
| `RECHECK_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to call `recheck(...)` is blocked. | Yes | No Recheck execution. |
| `REPLAY_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to call `replayByDispatch(...)` is blocked. | Yes | No Replay execution. |
| `SCHEDULER_DISPATCH_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to trigger scheduler dispatch is blocked. | Yes | No scheduler dispatch. |
| `MARKET_QUOTE_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to fetch fresh MarketQuote data is blocked. | Yes | No market quote refresh. |
| `PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to insert/update PushSnapshot is blocked. | Yes | No snapshot write. |
| `DISPATCH_CONFIG_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to init/update dispatch config/audit is blocked. | Yes | No config write. |
| `PUSH_SEND_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to send Push is blocked. | Yes | No Push send. |
| `EXTERNAL_CHANNEL_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need for external channel/provider delivery is blocked. | Yes | No external channel. |
| `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to generate/rank/score Candidate is blocked. | Yes | No Candidate signal. |
| `DECISION_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to generate a Decision is blocked. | Yes | No Decision generation. |
| `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any need to generate Point or numeric setup is blocked. | Yes | No Point / entry / stop / TP / RR. |
| `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any order/execution/auto-trading path is blocked. | Yes | No trading behavior. |

## 8. Required Safety Fields

Future readiness-approved implementation must expose negative/read-only safety semantics only:

| Field | Required value |
|---|---:|
| `reviewOnly` | `true` |
| `manualReviewOnly` | `true` |
| `notRecheckExecution` | `true` |
| `notReplayExecution` | `true` |
| `notSchedulerDispatch` | `true` |
| `notCollectorTrigger` | `true` |
| `notApiClientRefresh` | `true` |
| `notMarketQuoteRefresh` | `true` |
| `notPushSnapshotWrite` | `true` |
| `notDispatchConfigWrite` | `true` |
| `notPushSend` | `true` |
| `notExternalChannel` | `true` |
| `notCandidateSignal` | `true` |
| `notDecisionGeneration` | `true` |
| `notPointSignal` | `true` |
| `notFinalDirection` | `true` |
| `notEntryStopTpRr` | `true` |
| `notTradingSignal` | `true` |
| `notExecutable` | `true` |
| `displaySlotsAreCandidatePool` | `false` |

The field name `VALID_EXECUTABLE` may appear only as legacy persisted status evidence. Future dashboard/API copy must explain it as review-label evidence only, not executable readiness, trading authorization, or order permission.

## 9. Fail-Closed Rules

- Missing push id, symbol, or latest Recheck evidence => `RECHECK_PREVIEW_MISSING_FAIL_CLOSED`.
- Missing or unsupported persisted `recheckStatus` => `RECHECK_STATUS_MISSING_FAIL_CLOSED`.
- Mapper exception, unavailable log read path, or ambiguous owner path => `RECHECK_PREVIEW_MISSING_FAIL_CLOSED` with `failClosed=true`.
- Partial log-list / latest-log / status-contract evidence => `RECHECK_PREVIEW_PARTIAL_REVIEW_ONLY`.
- Missing replay-summary counters when requested => `REPLAY_SUMMARY_COUNTER_REVIEW_ONLY_READY` with zero/empty counters only if produced by read method; otherwise fail closed.
- Missing dispatch config/audit evidence => `DISPATCH_CONFIG_AUDIT_REVIEW_ONLY_READY` only if read path returns a stable empty state; never call init/update to fill gaps.
- Any attempt to call `recheck(...)` => `RECHECK_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED`.
- Any attempt to call `replayByDispatch(...)` => `REPLAY_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED`.
- Any attempt to trigger scheduler dispatch or `MarketQuoteClient.fetch24hTicker` => scheduler or market quote boundary blocked fail-closed.
- Any attempt to write PushSnapshot, recheck log, dispatch config, or audit => corresponding write boundary blocked fail-closed.
- Any attempt to expose Candidate / Decision / Point / final direction / entry / stop / TP / RR / order / execution / auto-trading => corresponding boundary blocked fail-closed.
- Any proposal that merely duplicates Review / Replay result status or Internal Push preview status => duplication status and readiness NO-GO unless a distinct persisted-Recheck owner value is proven.

## 10. Recheck / Replay Execution Boundary

This module is a status projection only.

It must explicitly remain:

- not Recheck execution;
- not Replay execution;
- not scheduler dispatch;
- not collector trigger;
- not API-client refresh;
- not MarketQuote refresh;
- not PushSnapshot write;
- not Recheck log write;
- not dispatch config init/update;
- not Push send;
- not external channel;
- not sendable message;
- not provider payload;
- not Candidate generation/ranking/scoring;
- not Decision generation;
- not Point generation;
- not final direction;
- not entry / stop / TP / RR;
- not order / execution / auto-trading;
- not Position Monitor execution.

`summarizeReplayByDispatch(...)` is acceptable only as a persisted-log counter read. `replayByDispatch(...)` is execution and must remain forbidden. `getOpsOverview(...)` is acceptable only if it reads existing config/audit/log state and does not cause `loadOrInit` or config update behavior.

## 11. Implementation Readiness Gate Checklist

The next package must answer GO / NO-GO before any implementation:

1. Is the proposed status distinct from completed Review / Replay result status?
2. Is the proposed status distinct from completed Internal Push preview / notification preview status?
3. Is Alert fatigue / notification policy only comparison evidence?
4. Is a dedicated endpoint truly needed, or can existing `/api/push/recheck` read paths remain sufficient?
5. If an endpoint is needed, is it one minimal read-only `Map` endpoint?
6. Is a dashboard panel truly needed, or would it duplicate existing panels?
7. If a panel is needed, can it be a minimal read-only status/copy/DOM panel only?
8. Can the owner path read only persisted logs, latest/log-list reads, replay-summary counters, ops overview, status contract, and dispatch config/audit evidence?
9. Can dispatch config be read without `loadOrInit` / `updateConfig`?
10. Are `recheck(...)`, `replayByDispatch(...)`, scheduler dispatch, and `MarketQuoteClient.fetch24hTicker` completely absent?
11. Are PushSnapshot write and Recheck-log insert completely absent?
12. Are all required safety fields present?
13. Are forbidden executable/action fields absent: recheck action, replay action, scheduler action, market quote refresh action, PushSnapshot write action, Push send, external channel action, Candidate ranking/score, final direction, entry, stop, takeProfit, TP, riskReward, RR, order action, execution action, auto-trading action?
14. Are schema/config/pom, new DTO / Validator / Assembler / Orchestrator, and new service/domain/mapper/repository ownership family unnecessary?

## 12. Maximum Future Implementation Files If Readiness Gate Returns GO

The readiness gate may permit only these candidates:

- existing controller, preferably `DashboardController.java` if a dashboard status endpoint is required, or `PushRecheckController.java` only if it reuses existing read path semantics;
- `dashboard.html`: minimal Recheck preview/status panel/copy/DOM only if needed;
- targeted controller/dashboard tests: endpoint safety flags, fail-closed states, duplication boundary, Recheck/Replay execution boundary, scheduler/MarketQuote boundary, PushSnapshot/config write boundary, and forbidden executable/action fields absent;
- existing PushRecheck tests: tiny owner-path assertions only if needed;
- implementation report docs;
- source-of-truth docs.

The readiness gate must forbid:

- new DTO / Validator / Assembler / Orchestrator;
- new service/domain/mapper/repository ownership family;
- schema/config/pom;
- Recheck execution;
- Replay execution;
- scheduler dispatch;
- collector / API-client refresh;
- MarketQuote refresh;
- PushSnapshot write;
- dispatch config init/update;
- Push send;
- external channel;
- Candidate generation/ranking/scoring;
- Decision generation;
- Point generation;
- final direction / entry / stop / TP / RR;
- order / execution / auto-trading;
- Position Monitor execution;
- P359 / P360.

## 13. NO-GO Conditions

The readiness gate must return NO-GO if any future implementation requires:

- calling `recheck(...)`;
- calling `replayByDispatch(...)`;
- triggering scheduler dispatch;
- triggering collector / API-client refresh;
- calling `MarketQuoteClient.fetch24hTicker`;
- writing PushSnapshot;
- inserting Recheck logs;
- initializing/updating dispatch config or dispatch audit;
- Push send;
- external channel;
- sendable message / provider payload;
- Candidate generation/ranking/scoring;
- Decision generation;
- Point generation;
- final direction;
- entry / stop / TP / RR;
- order / execution / auto-trading;
- Position Monitor execution;
- complete duplication of Review / Replay result status;
- complete duplication of Internal Push preview / notification preview status;
- new DTO / Validator / Assembler / Orchestrator;
- new service/domain/mapper/repository ownership family;
- schema/config/pom;
- any status that cannot be guaranteed as persisted Recheck log / status contract read-only projection.

If the only way to answer Recheck preview/status requires execution or replay, the decision must be `NO-GO: requires Recheck / Replay execution`.

## 14. Capability-Level Movement

- Current level: `REVIEW_ONLY_RUNTIME partial`.
- This package raises capability level: No, design only.
- Future implementation target, if readiness later approves: the 20th `REVIEW_ONLY_RUNTIME partial` slice.
- Not production wiring.
- Not Recheck execution.
- Not Replay execution.
- Not Push.
- Not Candidate generation.
- Not Decision generation.
- Not Point generation.
- Not Trading.

## 15. Freeze Rule Compliance

- 是否创建新骨架: No.
- 是否复用 Cursor-era / V1 资产: Yes. The design reuses existing PushRecheck, PushSnapshot, status-contract, ops overview, dispatch config/audit, dashboard, review-page, and test assets.
- 是否减少重复: Yes. The design narrows the module to persisted Recheck owner evidence and requires duplication NO-GO against completed Review / Replay and Internal Push slices.
- 是否提升 capability level: No.
- 是否接 service/runtime/dashboard/API: No. Design only.
- 是否符合 #830 审计建议: Yes. It avoids new skeleton owners and prefers existing service/runtime/dashboard/API owner paths.

## 16. Final Recommendation

Proceed to `Implementation readiness gate for Recheck preview / recheck status`.

The readiness gate may return GO only if the next implementation can remain a persisted Recheck log / status contract / ops-read projection, distinct from completed Review / Replay result status and Internal Push preview status, and without calling Recheck execution, Replay execution, scheduler dispatch, MarketQuote refresh, PushSnapshot write, dispatch config write, Push send, external channel, Candidate / Decision / Point generation, final direction / entry / stop / TP / RR, order / execution, auto-trading, Position Monitor execution, schema/config/pom, or new skeleton owners.

