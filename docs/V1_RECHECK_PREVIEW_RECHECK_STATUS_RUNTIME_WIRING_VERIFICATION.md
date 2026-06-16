# V1 Recheck Preview / Recheck Status Runtime Wiring Verification

## Verification Result

This A-risk verification package confirms that the merged Recheck preview /
Recheck status runtime wiring remains a review-only projection.

- Effective execution baseline: `3299671 feat(recheck): show preview status review-only`.
- Verified module: `Recheck preview / recheck status`.
- Verified endpoint: `GET /api/dashboard/recheck-preview-status`.
- Completed slice count: still `19`. This verification does not complete the
  20th slice until the visual verification / closure package is merged.
- Capability movement: none. V1 remains `REVIEW_ONLY_RUNTIME partial`.
- Verification result: PASS.

## Endpoint / Owner Path Evidence

`DashboardController` exposes `GET /api/dashboard/recheck-preview-status` as a
minimal read-only `Map` endpoint. The endpoint only reads:

- `PushRecheckService#getLatestLog(...)`
- `PushRecheckService#getOpsOverview(...)`
- persisted Recheck log evidence through the existing service / mapper owner
  path
- `PushRecheckStatusContract` status-label evidence
- replay-summary counters inside ops overview
- dispatch config / audit read evidence inside ops overview

The endpoint projects persisted Recheck log / status contract / dispatch-read
evidence only. It does not call `recheck(...)`, `replayByDispatch(...)`,
scheduler dispatch, collector trigger, API-client refresh,
`MarketQuoteClient.fetch24hTicker`, PushSnapshot write, dispatch config write,
Push send, external channel, Candidate / Decision / Point generation, final
direction, entry / stop / TP / RR, order / execution / auto-trading, or
Position Monitor execution.

Forbidden-call scan evidence:

```text
rg "pushRecheckService\.(recheck|replayByDispatch)|fetch24hTicker|updatePushStatus|loadOrInit\(|updateConfig\(|\.insert\(" \
  src/main/java/org/example/trademodel/controller/DashboardController.java \
  src/main/resources/templates/dashboard.html
```

Result: no matches.

## Dashboard Panel / DOM Evidence

`dashboard.html` contains the review-only panel and required display slots:

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

Dashboard copy explicitly states review-only, manual review only, fail-closed,
not Recheck execution, not Replay execution, not scheduler dispatch, not
collector trigger, not API client refresh, not MarketQuote refresh, not
PushSnapshot write, not dispatch config write, not Push send, not external
channel, not candidate, not decision generation, not point, not final
direction, not entry / stop / TP / RR, not order / execution / auto-trading,
not trading, not executable, and `Display Slots 不是候选池`.

No button, execution entry, replay entry, scheduler entry, refresh entry,
Push entry, external-channel entry, Candidate entry, Point entry, trading
entry, executable payload, or provider payload is present.

## Safety Fields Verified

`DashboardControllerTest` verifies these endpoint safety fields:

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

## Fail-Closed / Review-Only States Verified

Test and source evidence cover:

- missing persisted Recheck evidence:
  `RECHECK_PREVIEW_MISSING_FAIL_CLOSED`
- read-path exception:
  `RECHECK_STATUS_MISSING_FAIL_CLOSED`
- partial read-only state:
  `RECHECK_PREVIEW_PARTIAL_REVIEW_ONLY`
- status contract as historical label only:
  `persisted status label only; not executable readiness or trading authorization`
- replay-summary counters as counters only:
  `REPLAY_SUMMARY_COUNTER_REVIEW_ONLY_READY`
- dispatch config / audit read evidence only:
  `DISPATCH_CONFIG_AUDIT_REVIEW_ONLY_READY`
- duplicate Review / Replay result status review required:
  `DUPLICATE_REVIEW_REPLAY_STATUS_REVIEW_REQUIRED`
- duplicate Internal Push preview review required:
  `DUPLICATE_INTERNAL_PUSH_PREVIEW_REVIEW_REQUIRED`

Boundary states remain fail-closed:

- `RECHECK_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `REPLAY_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `SCHEDULER_DISPATCH_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `COLLECTOR_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `API_CLIENT_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `MARKET_QUOTE_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `DISPATCH_CONFIG_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `PUSH_SEND_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `EXTERNAL_CHANNEL_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `DECISION_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED`

## Forbidden Semantics Verified Absent

`DashboardControllerTest` verifies that the endpoint does not expose:

- `recheckExecutionAction`
- `replayExecutionAction`
- `schedulerAction`
- `marketQuoteRefreshAction`
- `pushSnapshotWriteAction`
- `dispatchConfigWriteAction`
- `pushSend`
- `pushSendState`
- `externalChannelAction`
- `candidateRanking`
- `candidateScore`
- `decisionGenerationAction`
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
- `positionMonitorExecutionAction`
- `executablePayload`
- `providerPayload`

## Checks

- `./mvnw -q -Dtest=DashboardControllerTest test`: PASS.
- `./mvnw -q test`: PASS.
- Forbidden-call scan against `DashboardController.java` and `dashboard.html`:
  PASS, no matches.
- `git diff --check`: PASS.
- `bash scripts/check-workflow-contract.sh`: PASS.

## Forbidden Scope Check

This verification package changes docs / source-of-truth only. It does not
modify Java business code, tests, dashboard business logic, schema, config,
pom, DTO, Validator, Assembler, Orchestrator, service, domain, mapper,
repository ownership family, endpoint behavior, dashboard behavior, Recheck
execution, Replay execution, scheduler dispatch, collector/API-client refresh,
MarketQuote refresh, PushSnapshot write, dispatch config write, Push send,
external channel, Candidate generation/ranking/scoring, Decision generation,
Point generation, final direction, entry / stop / TP / RR, order / execution /
auto-trading, Position Monitor execution, P359, or P360.

## Next Allowed Action

After this A-risk verification package is merged, the next allowed action is:

`Recheck Preview / Recheck Status Visual Verification / Closure`

Suggested next branch:

`recheck-preview-recheck-status-visual-verification-closure`
