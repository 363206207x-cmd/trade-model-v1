# V1 Account Risk / Account Exposure Status Source Read

## Scope

This package is source-read only for the next minimal review-only runtime slice:
`Account risk / account exposure status`.

It does not implement an endpoint, dashboard panel, write path, trading authorization,
position sizing, or any account-risk behavior. The effective execution baseline is
`4cf1af5 docs(runtime): select account risk exposure next slice`.

## Files Read

- `src/main/resources/schema.sql`
- `src/main/java/org/example/trademodel/entity/TmAccountRiskSnapshotDO.java`
- `src/main/java/org/example/trademodel/entity/AccountRiskSnapshotDO.java`
- `src/main/java/org/example/trademodel/mapper/AccountRiskSnapshotMapper.java`
- `src/main/java/org/example/trademodel/service/PushSnapshotService.java`
- `src/main/java/org/example/trademodel/service/impl/PushRecheckServiceImpl.java`
- `src/main/java/org/example/trademodel/service/impl/ReviewAggregateServiceImpl.java`
- `src/main/java/org/example/trademodel/vo/ReviewAggregateVO.java`
- `src/main/java/org/example/trademodel/service/impl/AnalysisAssemblerServiceImpl.java`
- `src/main/resources/static/js/review-page.js`
- `src/main/resources/templates/dashboard.html`
- `src/test/java/org/example/trademodel/service/impl/PushRecheckServiceImplTest.java`
- `src/test/java/org/example/trademodel/service/impl/AnalysisAssemblerServiceImplAccountRiskJsonTest.java`
- `src/test/java/org/example/trademodel/service/impl/ReviewAggregateServiceImplEvidenceTopItemsTest.java`
- `src/test/java/org/example/trademodel/service/impl/ReviewAggregateServiceImplMarketEnvironmentTest.java`
- `src/test/java/org/example/trademodel/service/impl/ReviewAggregateServiceImplScoreTopItemsTest.java`

## Owner Path Findings

### Account Risk Snapshot Read Model

`tm_account_risk_snapshot` already exists in `schema.sql` and is backed by
`TmAccountRiskSnapshotDO` plus `AccountRiskSnapshotMapper`. The table contains a
compact account-risk read model:

- `analysis_id`
- `symbol`
- `risk_level_snapshot`
- `risk_allowed`
- `risk_reason_code`
- `risk_reason_text`
- `position_exposure`
- `max_allowed_exposure`
- `snapshot_source`
- `snapshot_version`
- `source_note`
- `trace_id`
- `create_time`

`AccountRiskSnapshotMapper` exposes `selectById(Long id)` and
`selectLatestByAnalysisId(String analysisId)`, which are viable read-only sources
for a future status slice. The same mapper also exposes `insert(...)`, so any
future implementation must explicitly stay on read methods only.

`AccountRiskSnapshotDO` is a separate legacy-looking carrier with broader score
fields such as `totalRiskScore`, `correlationRisk`, `var95`, and
`maxDrawdownEstimate`. No current owner path was found that makes it the canonical
runtime status source. The canonical near-term source is `TmAccountRiskSnapshotDO`.

### Write-Side Boundary

`PushSnapshotService` owns the write-side account-risk snapshot flow. It creates
`TmAccountRiskSnapshotDO` records through `ensureAccountRiskSnapshot(...)` and
`insertAuthoritativeSnapshot(...)`, and parses exposure from execution-plan
position suggestions. This path also emits risk reason codes such as:

- `DECISION_NOT_WORTH_OPENING`
- `EXPOSURE_UNKNOWN_FALLBACK`
- `EXPOSURE_LIMIT_EXCEEDED`
- `ACCOUNT_RISK_ALLOWED`

This service is not safe for a review-only status implementation because it writes
account risk snapshots and sits inside push-snapshot ownership.

### Push / Recheck Boundary

`PushRecheckServiceImpl` reads account-risk snapshots through
`accountRiskSnapshotMapper.selectById(...)` when rechecking a push snapshot. It
copies `riskAllowed` into `currentAccountRiskAllowed` and updates recheck logs and
push status. It can classify `RISK_BLOCKED`, `VALID_WAITING`, or
`VALID_EXECUTABLE`.

This is useful source evidence, but it is not a safe owner path for the status
slice. It performs recheck execution and writes status/log rows. A future account
risk status must not call `recheck(...)`, must not update push status, and must not
surface executable status as authorization.

### Review Aggregate Read Path

`ReviewAggregateServiceImpl` reads account risk through the existing push/recheck
aggregate path. `ReviewAggregateVO.ReviewPushSummary` already carries:

- `accountRiskSnapshotId`
- `accountRiskAllowed`
- `riskLevelSnapshot`
- `riskReasonCode`
- `riskReasonText`
- `positionExposure`
- `maxAllowedExposure`
- `snapshotSource`
- `snapshotVersion`

This is a reusable read-only aggregate path for design consideration. The future
status must avoid nearby execution-plan fields such as entry, stop, take-profit,
leverage, trigger, and other point/trading semantics.

### Analysis Assembler Boundary

`AnalysisAssemblerServiceImpl` builds an execution account-risk JSON payload from
the latest account-risk snapshot and writes it into execution-plan persistence
during `saveToDatabase(...)`. The JSON contains seven read fields:

- `riskAllowed`
- `riskReasonCode`
- `riskReasonText`
- `positionExposure`
- `maxAllowedExposure`
- `snapshotSource`
- `snapshotVersion`

Although this confirms a stable field set, the assembler is a write/persist path.
Future status design must not call assembler save behavior and must not generate
execution-plan JSON.

### Dashboard / Review UI

`review-page.js` already displays account-risk fields in the push/recheck detail
area and explains that `currentAccountRiskAllowed` comes from the account risk
snapshot referenced by the push snapshot. This is review-page evidence, but it is
tightly coupled to push/recheck history.

`dashboard.html` does not currently contain a dedicated account risk / account
exposure status panel. Existing dashboard review-only status panels provide copy
and DOM patterns, but there is no account-risk-specific runtime status surface yet.

## Reusable Assets

- `tm_account_risk_snapshot` as the existing table and read model.
- `TmAccountRiskSnapshotDO` as the canonical account-risk snapshot entity.
- `AccountRiskSnapshotMapper.selectById(...)` and
  `selectLatestByAnalysisId(...)` as existing read methods.
- `ReviewAggregateServiceImpl` and `ReviewAggregateVO.ReviewPushSummary` as
  existing aggregate read evidence for account-risk fields.
- `review-page.js` account-risk display copy and field naming as UI evidence.
- `PushRecheckServiceImplTest` and
  `AnalysisAssemblerServiceImplAccountRiskJsonTest` as boundary tests showing how
  risk snapshots are read, written, and interpreted today.

## Gaps

- No dedicated dashboard account-risk / account-exposure status panel exists.
- No dedicated review-only account-risk status endpoint exists.
- No account-risk-specific safety field contract exists yet, such as
  `notAccountRiskWrite`, `notPushSnapshotWrite`, `notTradingAuthorization`, or
  `notPositionSizing`.
- Existing `PushRecheckServiceImpl` treats a missing account risk snapshot as not
  blocking recheck execution in one test path. Future review-only status should
  fail closed or mark partial instead of implying executable readiness.
- `positionExposure` and `maxAllowedExposure` are readable snapshot values, but
  they can be misread as position sizing guidance unless the status copy is
  explicit.
- Existing account-risk evidence is close to Push/Recheck and execution-plan
  persistence; the future design must keep those boundaries blocked.

## Design Risk Notes

- Do not call `PushSnapshotService.ensureAccountRiskSnapshot(...)` or
  `insertAuthoritativeSnapshot(...)`; those are write-side paths.
- Do not call `PushRecheckServiceImpl.recheck(...)`; it executes recheck behavior
  and writes status/log rows.
- Do not call `AnalysisAssemblerServiceImpl.saveToDatabase(...)`; it is a
  persist/generation path.
- Treat `riskAllowed` as read-only evidence, never as trading authorization or
  executable readiness.
- Treat `positionExposure` and `maxAllowedExposure` as snapshot display values,
  never as position sizing, reduce/close/stop/reverse guidance, or order sizing.
- Avoid entry, stop, take-profit, RR, leverage, final direction, candidate
  ranking, point generation, and trading fields in any future status response.

## Candidate Status Mapping For Design

The next design package should consider these statuses:

- `ACCOUNT_RISK_SNAPSHOT_REVIEW_ONLY_READY`
- `ACCOUNT_RISK_SNAPSHOT_MISSING_FAIL_CLOSED`
- `ACCOUNT_RISK_SNAPSHOT_PARTIAL_REVIEW_ONLY`
- `ACCOUNT_EXPOSURE_REVIEW_ONLY`
- `ACCOUNT_RISK_NOT_ALLOWED_REVIEW_ONLY_FAIL_CLOSED`
- `ACCOUNT_RISK_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `PUSHSNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `PUSH_SEND_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `RECHECK_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `TRADING_AUTHORIZATION_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `POSITION_SIZING_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `ACTION_GUIDANCE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED`

## Candidate Safety Fields For Design

- `reviewOnly=true`
- `manualReviewOnly=true`
- `notAccountRiskWrite=true`
- `notPushSnapshotWrite=true`
- `notPushSend=true`
- `notRecheckExecution=true`
- `notTradingAuthorization=true`
- `notPositionSizing=true`
- `notReduceCloseStopReverseGuidance=true`
- `notCandidateSignal=true`
- `notDecisionGeneration=true`
- `notPointSignal=true`
- `notFinalDirection=true`
- `notEntryStopTpRr=true`
- `notTradingSignal=true`
- `notExecutable=true`
- `displaySlotsAreCandidatePool=false`

## Next Allowed Action

`Minimal Review-Only Account Risk / Account Exposure Status Runtime Wiring Design`

Suggested next branch:
`minimal-review-only-account-risk-account-exposure-status-runtime-wiring-design`
