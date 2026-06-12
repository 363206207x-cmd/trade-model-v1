# V1 Account Risk / Account Exposure Status Runtime Wiring Verification

## Scope

This package verifies the B-risk implementation merged as
`d3a79da feat(risk): show account exposure review-only status`.

It is verification only. It does not implement endpoint behavior, dashboard
behavior, Java business code, tests, schema/config/pom changes, account-risk
write behavior, PushSnapshot write behavior, Push send, Recheck execution,
trading authorization, position sizing, reduce/close/stop/reverse guidance,
Candidate generation, Decision generation, Point generation, final direction,
entry/stop/TP/RR, order/execution, auto-trading, Position Monitor execution,
replay/recheck execution, P359, or P360.

## Verification Result

PASS.

The Account risk / account exposure status runtime wiring matches the
review-only boundary required for `REVIEW_ONLY_RUNTIME partial`.

This verification keeps the completed slice count at 16. Account risk /
account exposure status still requires Visual Verification / Closure before it
can count as the 17th completed Review-Only Runtime partial slice.

## Endpoint And Owner Path Evidence

Verified endpoint:

- `GET /api/dashboard/account-risk-exposure-status?symbol=BTCUSDT`

Verified read-only owner path:

- The endpoint reads the latest `DecisionResult` only to derive `analysisId`.
- Runtime owner read uses
  `AccountRiskSnapshotMapper.selectLatestByAnalysisId(...)`.
- `AccountRiskSnapshotMapper.selectById(...)` remains historical snapshot read
  only and is not the runtime owner path.

Read-only boundary:

- no `tm_account_risk_snapshot` write
- no PushSnapshot write
- no Push send
- no Recheck execution
- no trading authorization
- no position sizing
- no reduce / close / stop / reverse guidance
- no Candidate / Decision generation / Point
- no final direction / entry / stop / TP / RR
- no order / execution / auto-trading

## Dashboard Evidence

Verified dashboard panel and DOM:

- `accountRiskExposureStatusPanel`
- `accountRiskRuntimeStatusValue`
- `accountRiskAllowedEvidenceValue`
- `accountRiskExposureValue`
- `accountRiskSnapshotSourceValue`
- `accountRiskReviewOnlyValue`
- `accountRiskWriteBoundaryValue`
- `accountRiskPushRecheckBoundaryValue`
- `accountRiskSignalBoundaryValue`
- `accountRiskReasonValue`

Verified dashboard copy:

- `riskAllowed` is read-only evidence only.
- `riskAllowed` is not trading authorization.
- `positionExposure` / `maxAllowedExposure` are read-only status only.
- Exposure is not position sizing.
- No reduce / close / stop / reverse guidance is generated.
- The panel states review-only, manual review only, fail-closed, not
  account-risk write, not PushSnapshot write, not Push send, not Recheck
  execution, not candidate, not decision generation, not point, not final
  direction, not entry/stop/TP/RR, not trading, not executable, and Display
  Slots are not a candidate pool.

## Safety Fields Verified

The endpoint safety fields are present and covered by targeted test evidence:

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

## Fail-Closed And Review-Only States Verified

Verified by code inspection, targeted tests, and documentation evidence:

- missing decision / `analysisId` ->
  `ACCOUNT_RISK_STATUS_MISSING_FAIL_CLOSED`
- mapper exception -> `ACCOUNT_RISK_STATUS_BACKEND_PENDING_FAIL_CLOSED`
- missing account risk snapshot ->
  `ACCOUNT_RISK_STATUS_MISSING_FAIL_CLOSED`
- missing exposure -> `ACCOUNT_EXPOSURE_MISSING_FAIL_CLOSED`
- partial risk evidence -> `ACCOUNT_RISK_STATUS_PARTIAL_REVIEW_ONLY`
- `riskAllowed` -> `RISK_ALLOWED_READ_ONLY_EVIDENCE`
- blocked account action boundary ->
  `ACCOUNT_RISK_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- blocked PushSnapshot write boundary ->
  `PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- blocked Push send boundary -> `PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED`
- blocked Recheck boundary -> `RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED`
- blocked trading authorization boundary ->
  `TRADING_AUTHORIZATION_BOUNDARY_BLOCKED_FAIL_CLOSED`
- blocked position sizing boundary ->
  `POSITION_SIZING_BOUNDARY_BLOCKED_FAIL_CLOSED`
- blocked reduce / close / stop / reverse boundary ->
  `REDUCE_CLOSE_STOP_REVERSE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- blocked Candidate boundary -> `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- blocked Point boundary -> `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED`
- blocked Trading boundary -> `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED`

## Forbidden Semantics Classification

Forbidden-semantics search found only negative guardrail copy, blocked boundary
status identifiers, and explicit `doesNotExist()` assertions for forbidden
fields.

No positive executable fields were verified for:

- `accountRiskWriteAction`
- `pushSnapshotWriteAction`
- `pushSend` / `pushSendState`
- `recheckExecutionAction`
- `tradingAuthorization`
- `positionSize` / `leverage` / `positionSizing`
- `reduceGuidance` / `closeGuidance` / `stopGuidance` / `reverseGuidance`
- `candidateRanking` / `candidateScore`
- `finalDirection`
- `entry` / `stop` / `takeProfit` / `tp` / `riskReward` / `rr`
- `orderAction` / `executionAction` / `autoTradingAction`

## Checks

- `bash scripts/v1-state.sh` passed with clean local main and user handoff
  confirming PR #975 merged at `d3a79da`.
- `bash scripts/v1-auto.sh next` passed; source-of-truth lag is expected and
  handled by this package.
- `bash scripts/codex-next-task.sh` passed for this verification handoff.
- `bash scripts/check-workflow-contract.sh` passed.
- `./mvnw -q -Dtest=DashboardControllerTest test` passed.
- `./mvnw -q test` passed.
- Endpoint / owner-path evidence grep passed.
- Dashboard DOM / safety-copy evidence grep passed.
- Forbidden-semantics grep was classified as negative-only evidence.

## Forbidden Scope Check

This package changes only verification and source-of-truth docs. It does not
change Java business code, tests, dashboard business logic, schema/config/pom,
DTO / Validator / Assembler / Orchestrator, or service/domain/mapper/repository
ownership families.

It does not write account-risk snapshots or PushSnapshot data, send Push,
execute Recheck, emit trading authorization, emit position sizing, emit reduce /
close / stop / reverse guidance, generate Candidate / Decision / Point, emit
final direction / entry / stop / TP / RR, or trigger order/execution/trading.

## Next Allowed Action

Next allowed action:

`Account Risk / Account Exposure Status Visual Verification / Closure`

Next branch:

`account-risk-account-exposure-status-visual-verification-closure`

The next package remains A-risk and may modify only visual closure docs and
source-of-truth docs. It must verify dashboard visual/copy evidence and must
not claim Account risk / account exposure status as the 17th completed
Review-Only Runtime partial slice until visual closure is complete and merged.
