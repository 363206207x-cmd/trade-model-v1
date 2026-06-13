# V1 Recheck Preview / Recheck Status Implementation Readiness Gate

## 1. Scope

- Effective execution baseline: `40a84e3 docs(recheck): design preview status runtime wiring (#1000)`.
- Branch: `recheck-preview-recheck-status-implementation-readiness-gate`.
- Package type: A-risk implementation readiness gate.
- Current capability level: `REVIEW_ONLY_RUNTIME partial`.
- Completed review-only runtime partial slices before this package: 19.
- Capability movement: none.

This package makes a GO / NO-GO decision for a future B-risk minimal implementation of `Recheck preview / recheck status`. It does not implement an endpoint, dashboard behavior, Recheck preview status, Recheck status, Recheck execution, Replay execution, scheduler dispatch, collector trigger, API-client refresh, MarketQuote refresh, PushSnapshot write, dispatch-config write/init/update, Push send, external channel, Candidate generation/ranking/scoring, Decision generation, Point generation, final direction, entry / stop / TP / RR, order / execution, auto-trading, Position Monitor execution, schema/config/pom changes, new DTO / Validator / Assembler / Orchestrator, or a new service/domain/mapper/repository ownership family.

## 2. Readiness Decision

Decision: `GO to B-risk minimal implementation`.

The next implementation may continue only if it is strictly limited to a persisted Recheck log / status contract / dispatch-read evidence projection:

- `PushRecheckController` GET read paths as source inventory only;
- `PushRecheckService` read methods only;
- `PushRecheckLogMapper` persisted-log reads only;
- `PushRecheckStatusContract` review-label mapping only;
- replay-summary counters only;
- ops overview read evidence only;
- dispatch config / audit read evidence only;
- optional backlog count read evidence only.

The future implementation must not call execution or write paths. If any required behavior depends on `recheck(...)`, `replayByDispatch(...)`, scheduler dispatch, MarketQuote refresh, PushSnapshot writes, dispatch config init/update/write, Push send, external channels, Candidate / Decision / Point generation, final direction, entry / stop / TP / RR, order / execution, auto-trading, or Position Monitor execution, the implementation must stop with NO-GO.

## 3. Duplication Decision

Decision: `not duplicate only when scoped to persisted Recheck log / status contract / dispatch-read evidence`.

The slice is not a duplicate of completed modules only under this narrow scope:

| Completed module | Existing coverage | Readiness decision |
|---|---|---|
| Review / Replay result status | Owns `/api/dashboard/review-replay-result-status`, review result availability, ReviewAggregate context, replay summary availability, and no replay execution boundaries. | Future Recheck status is allowed only if it surfaces persisted Recheck log / status-contract / dispatch-read evidence. If it merely restates review result or replay result availability, return `NO-GO: duplicate with Review / Replay result status`. |
| Internal Push preview / notification preview status | Owns `/api/dashboard/internal-push-preview-notification-status`, internal preview DTO / assembler path, preview-only state, no Push send, no external channel, no PushSnapshot write, and no Recheck / Replay execution boundaries. | Future Recheck status is allowed only if it reads persisted Recheck logs and status contract evidence rather than `recheckRequired`, preview blockers, notification preview, no-send policy, or internal push owner assets. If not, return `NO-GO: duplicate with Internal Push preview / notification preview status`. |
| Alert fatigue / notification policy status | Owns MonitorAlert / notification policy / suppression / fatigue read-only status. | Future Recheck status may cite this only as comparison evidence. If it implements notification policy or alert-fatigue health, return `NO-GO: duplicate with Alert fatigue / notification policy status`. |

## 4. Owner Path

Approved future owner path if implementation proceeds:

```text
DashboardController or existing read controller
  -> existing PushRecheck read methods only
  -> PushRecheckLogMapper persisted-log reads
  -> TmPushRecheckLogDO / PushRecheckLogItemVO
  -> PushRecheckStatusContract review-label mapping
  -> PushRecheckReplaySummaryVO counters
  -> PushRecheckOpsOverviewVO read evidence
  -> PushRecheckDispatchConfigService#getCurrentConfig / listRecentAudit
  -> minimal review-only Map projection
```

Allowed read evidence:

- `PushRecheckService#getLatestLog`
- `PushRecheckService#listLogs`
- `PushRecheckService#summarizeReplayByDispatch` as counter-only read evidence
- `PushRecheckService#getOpsOverview` as read evidence only
- `PushRecheckLogMapper.selectByPushId`
- `PushRecheckLogMapper.countByPushId`
- `PushRecheckLogMapper.selectByInstructionId`
- `PushRecheckLogMapper.selectByBatchId`
- `PushRecheckLogMapper.selectRecent`
- `PushRecheckLogMapper.countByStatusInWindow`
- `PushRecheckStatusContract`
- `PushRecheckDispatchConfigService#getCurrentConfig`
- `PushRecheckDispatchConfigService#listRecentAudit`
- `PushSnapshotMapper#countPendingRecheckBacklog`

Forbidden adjacent owner paths:

- `PushRecheckService#recheck(...)`
- `PushRecheckService#replayByDispatch(...)`
- `PushRecheckScheduler#recheckPendingPushesScheduled(...)`
- `PushRecheckScheduler#handleOne(...)`
- `MarketQuoteClient#fetch24hTicker(...)`
- `PushRecheckLogMapper#insert(...)`
- `PushSnapshotMapper#insert(...)`
- `PushSnapshotMapper#updatePushStatus(...)`
- `PushRecheckDispatchConfigService#loadOrInit(...)`
- `PushRecheckDispatchConfigService#updateConfig(...)`
- dispatch config mapper insert/update
- dispatch config audit mapper insert

## 5. Endpoint Decision

The next B-risk implementation may add at most one minimal read-only `Map` endpoint if needed.

Allowed endpoint shape:

```text
GET /api/dashboard/recheck-preview-status?symbol=BTCUSDT
```

The endpoint may also be skipped if existing `/api/push/recheck` GET read paths are judged sufficient. If implemented, the endpoint must read only persisted Recheck log / status contract / dispatch-read evidence and must not create a sendable message, provider payload, executable action, or trading-adjacent output.

## 6. Dashboard Decision

The next B-risk implementation may add one minimal dashboard panel only if needed:

- minimal Recheck preview / Recheck status status panel;
- DOM ids for status values only;
- review-only / manual-review / fail-closed copy;
- not Recheck execution / not Replay execution / not scheduler dispatch / not MarketQuote refresh copy;
- not PushSnapshot write / not dispatch config write copy;
- not Push send / not external channel copy;
- not Candidate / Decision / Point / Trading copy.

Forbidden dashboard additions:

- Recheck trigger button;
- Replay trigger button;
- scheduler dispatch trigger;
- dispatch config update controls;
- Push send or external channel controls;
- sendable message or provider payload;
- Candidate ranking / scoring;
- final direction;
- entry / stop / TP / RR;
- order / execution / auto-trading controls.

## 7. Allowed Implementation Files

If the next package remains B-risk and passes its own scoped checks, it may touch only:

- `src/main/java/org/example/trademodel/controller/DashboardController.java` or an existing related controller, for one minimal read-only `Map` endpoint only;
- `src/main/resources/templates/dashboard.html`, for one minimal status panel / DOM / safety-copy surface only;
- targeted controller/dashboard tests, especially `DashboardControllerTest`, for endpoint safety fields, fail-closed states, duplication boundary, forbidden execution/write/scheduler/refresh/send/candidate/point/trading boundaries, and forbidden executable/action fields absent;
- existing Recheck read-model tests, for tiny owner-path assertions only if needed;
- implementation report docs;
- source-of-truth docs.

## 8. Forbidden Files / Scopes

The next implementation must not touch:

- schema/config/pom;
- new DTO / Validator / Assembler / Orchestrator files;
- new service/domain/mapper/repository ownership families;
- production Java outside the allowed controller-only read projection;
- Recheck execution methods;
- Replay execution methods;
- scheduler dispatch methods;
- collector / API-client refresh paths;
- MarketQuote refresh paths;
- PushSnapshot write paths;
- Recheck log insert paths;
- dispatch config init/update/write paths;
- Push send or external channel paths;
- Candidate / Decision / Point generation paths;
- final direction / entry / stop / TP / RR paths;
- order / execution / auto-trading paths;
- Position Monitor execution paths;
- P359 / P360.

## 9. Required Safety Fields

If implementation proceeds, the endpoint and tests must cover these fields or equivalent negative-only safety semantics:

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

`VALID_EXECUTABLE` may appear only as persisted legacy Recheck status evidence. It must be explained as a review-label/status-contract value, not executable readiness, trading authorization, or order permission.

## 10. Required Status Mapping

| Status | Gate requirement |
|---|---|
| `RECHECK_PREVIEW_REVIEW_ONLY_READY` | Allowed only when persisted latest/list Recheck evidence is readable. |
| `RECHECK_PREVIEW_MISSING_FAIL_CLOSED` | Required when preview owner input, latest log, symbol, push id, or read path is missing. |
| `RECHECK_PREVIEW_PARTIAL_REVIEW_ONLY` | Required when only partial log/counter/status evidence is readable. |
| `RECHECK_STATUS_REVIEW_ONLY_READY` | Allowed only when status contract maps persisted status to review-label evidence. |
| `RECHECK_STATUS_MISSING_FAIL_CLOSED` | Required when persisted status is missing, unknown, unsupported, or ambiguous. |
| `RECHECK_LOG_READ_MODEL_REVIEW_ONLY_READY` | Allowed for mapper/service read methods only. |
| `REPLAY_SUMMARY_COUNTER_REVIEW_ONLY_READY` | Allowed for persisted-log counters only; not replay execution. |
| `DISPATCH_CONFIG_AUDIT_REVIEW_ONLY_READY` | Allowed for `getCurrentConfig` / `listRecentAudit` only; not init/update. |
| `DUPLICATE_REVIEW_REPLAY_STATUS_REVIEW_REQUIRED` | Required if status overlaps completed Review / Replay result status. |
| `DUPLICATE_INTERNAL_PUSH_PREVIEW_REVIEW_REQUIRED` | Required if status overlaps completed Internal Push preview status. |
| `RECHECK_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Required for any need to call `recheck(...)`. |
| `REPLAY_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Required for any need to call `replayByDispatch(...)`. |
| `SCHEDULER_DISPATCH_BOUNDARY_BLOCKED_FAIL_CLOSED` | Required for any scheduler dispatch need. |
| `MARKET_QUOTE_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED` | Required for any `MarketQuoteClient.fetch24hTicker` or live quote refresh need. |
| `PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Required for any PushSnapshot insert/update need. |
| `DISPATCH_CONFIG_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Required for config init/update/write needs. |
| `PUSH_SEND_BOUNDARY_BLOCKED_FAIL_CLOSED` | Required for Push send needs. |
| `EXTERNAL_CHANNEL_BOUNDARY_BLOCKED_FAIL_CLOSED` | Required for provider/external channel needs. |
| `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Required for Candidate generation/ranking/scoring needs. |
| `DECISION_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Required for Decision generation needs. |
| `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED` | Required for Point or setup value needs. |
| `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED` | Required for order/execution/auto-trading needs. |

## 11. Required Tests For Future Implementation

The next implementation must include targeted tests for:

1. endpoint safety fields and required negative boundaries;
2. missing preview owner input fail-closed state;
3. missing latest/log-list evidence fail-closed state;
4. partial Recheck evidence review-only state;
5. status contract review-label evidence, including legacy `VALID_EXECUTABLE` not being treated as executable readiness;
6. replay-summary counters as read-only counters, not `replayByDispatch(...)`;
7. dispatch config/audit read evidence without `loadOrInit(...)` / `updateConfig(...)`;
8. Recheck execution, Replay execution, scheduler dispatch, MarketQuote refresh, PushSnapshot write, dispatch config write, Push send, external channel, Candidate / Decision / Point, entry / stop / TP / RR, order / execution / auto-trading, and Position Monitor boundaries;
9. forbidden executable/action fields absent;
10. dashboard DOM and safety copy if a panel is added.

## 12. NO-GO Conditions

The future implementation must stop with NO-GO if it requires any of the following:

- calling `recheck(...)`;
- calling `replayByDispatch(...)`;
- triggering scheduler dispatch;
- triggering collector / API-client refresh;
- calling `MarketQuoteClient.fetch24hTicker`;
- writing PushSnapshot;
- inserting Recheck logs;
- initializing or updating dispatch config;
- writing dispatch config audit;
- Push send;
- external channel;
- sendable message / provider payload;
- Candidate generation / ranking / scoring;
- Decision generation;
- Point generation;
- final direction;
- entry / stop / TP / RR;
- order / execution / auto-trading;
- Position Monitor execution;
- complete duplication of Review / Replay result status;
- complete duplication of Internal Push preview / notification preview status;
- complete duplication of Alert fatigue / notification policy status;
- new DTO / Validator / Assembler / Orchestrator;
- new service/domain/mapper/repository ownership family;
- schema/config/pom;
- any status that cannot be guaranteed as persisted Recheck log / status contract read-only projection.

## 13. Recheck / Replay Execution Boundary

The approved boundary is:

```text
persisted Recheck log / status contract / dispatch-read status projection only
```

The approved boundary is not:

- Recheck execution;
- Replay execution;
- scheduler dispatch;
- collector trigger;
- API-client refresh;
- MarketQuote refresh;
- PushSnapshot write;
- Recheck log write;
- dispatch config init/update/write;
- Push send;
- external channel;
- sendable message;
- provider payload;
- Candidate generation/ranking/scoring;
- Decision generation;
- Point generation;
- final direction;
- entry / stop / TP / RR;
- order / execution / auto-trading;
- Position Monitor execution.

## 14. Next Allowed Action

Next allowed action after this readiness gate merges:

```text
Minimal Review-Only Recheck Preview / Recheck Status Runtime Wiring Implementation
```

Next branch:

```text
minimal-review-only-recheck-preview-recheck-status-runtime-wiring-implementation
```

The next package is B-risk. It must create a Draft PR and stop for GPT / human review; it must not auto-merge.

## 15. Capability-Level Movement

- Current level: `REVIEW_ONLY_RUNTIME partial`.
- This package raises capability level: No.
- Completed slice count remains: 19.
- Future implementation target, if B-risk implementation is approved and later visually closed: the 20th `REVIEW_ONLY_RUNTIME partial` slice.
- Not production wiring.
- Not Recheck execution.
- Not Replay execution.
- Not Push.
- Not Candidate generation.
- Not Decision generation.
- Not Point generation.
- Not Trading.

## 16. Freeze Rule Compliance

- 是否创建新骨架: No.
- 是否复用 Cursor-era / V1 资产: Yes. It reuses existing PushRecheck controller/service/mapper/status-contract/ops/dispatch/dashboard/test assets.
- 是否减少重复: Yes. It blocks duplicate Review / Replay result status, duplicate Internal Push preview status, and duplicate Alert fatigue / notification policy status.
- 是否提升 capability level: No.
- 是否接 service/runtime/dashboard/API: No in this package; it permits a future B-risk minimal service/runtime/dashboard/API read-only projection.
- 是否符合 #830 审计建议: Yes. It avoids new skeleton owners and narrows implementation to existing owner paths.

## 17. Final Recommendation

Proceed to B-risk minimal implementation only under the bounded owner path above.

The future package must remain a review-only persisted Recheck log / status contract / dispatch-read projection. It must stop immediately if implementation requires execution, replay, scheduler dispatch, quote refresh, PushSnapshot write, dispatch config write, Push send, external channel, Candidate / Decision / Point generation, final direction / entry / stop / TP / RR, order / execution / auto-trading, Position Monitor execution, schema/config/pom, new skeleton owners, or duplicate completed Review / Replay / Internal Push / Alert fatigue status behavior.
