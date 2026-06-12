# V1 Account Risk / Account Exposure Status Implementation Readiness Gate

## Scope

This package is an implementation readiness gate for
`Account risk / account exposure status`.

It is readiness only. It does not implement an endpoint, dashboard panel, Java
business logic, tests, schema/config/pom changes, account-risk write behavior,
PushSnapshot write behavior, Push send, Recheck execution, trading
authorization, position sizing, reduce/close/stop/reverse guidance, Candidate,
Decision generation, Point, final direction, entry/stop/TP/RR, order/execution,
auto-trading, Position Monitor execution, replay/recheck execution, P359, or
P360.

The effective execution baseline is
`400e604 docs(risk): design account exposure runtime wiring`.

## Readiness Decision

GO to B-risk minimal implementation.

The next implementation may proceed only as a review-only status wiring package
over the existing account-risk snapshot read owner path. It must create a Draft
PR and stop for B-risk review. It must not auto-merge.

The implementation remains within `REVIEW_ONLY_RUNTIME partial`; it does not
raise V1 into production account-risk action, trading authorization, or
execution readiness.

## Owner Path Decision

The preferred owner path is:

`tm_account_risk_snapshot -> TmAccountRiskSnapshotDO -> AccountRiskSnapshotMapper.selectLatestByAnalysisId(...)`

Readiness judgment:

- `AccountRiskSnapshotMapper.selectLatestByAnalysisId(...)` is the primary
  runtime read source when an `analysisId` is available.
- `AccountRiskSnapshotMapper.selectById(...)` is historical snapshot lookup only
  and must not become the main runtime owner path.
- `ReviewAggregateServiceImpl`, `ReviewAggregateVO.ReviewPushSummary`, and
  `review-page.js` are display context only. They may inform copy and field
  naming, but they must not become an execution, Push, Recheck, or account
  action entry.
- `PushSnapshotService`, `PushRecheckServiceImpl`, and
  `AnalysisAssemblerServiceImpl` remain boundary evidence only. Future
  implementation must not call write, recheck, or persistence behavior from
  those paths.

No new DTO, Validator, Assembler, Orchestrator, service, domain, mapper, or
repository ownership family is allowed.

## Endpoint Readiness

A dedicated account risk / exposure status endpoint is allowed only if the
implementation cannot reuse an existing controller projection cleanly.

If an endpoint is added, it must be at most one minimal read-only `Map` endpoint
in an existing controller owner path. It may read existing account-risk snapshot
evidence and return status identifiers, fail-closed / partial flags, safety
booleans, display-safe snapshot fields, and boundary summaries.

The endpoint must not:

- write account-risk snapshots
- write PushSnapshot data
- send Push or use an external channel
- execute Recheck
- emit trading authorization
- emit position sizing
- emit reduce / close / stop / reverse guidance
- generate Candidate, Decision, or Point behavior
- emit final direction, entry, stop, TP, or RR
- trigger order, execution, auto-trading, or Position Monitor execution

## Dashboard Readiness

A minimal dashboard account-risk / exposure status panel is allowed if the
implementation keeps it status-only.

Allowed dashboard work is limited to DOM ids and safety copy such as:

- `accountRiskExposureStatusPanel`
- `accountRiskRuntimeStatusValue`
- `accountRiskAllowedEvidenceValue`
- `accountRiskExposureValue`
- `accountRiskSnapshotSourceValue`
- `accountRiskWriteBoundaryValue`
- `accountRiskPushRecheckBoundaryValue`
- `accountRiskSignalBoundaryValue`
- `accountRiskReasonValue`

Required copy:

- review-only
- manual review only
- fail-closed
- `riskAllowed` is read-only evidence, not trading authorization
- exposure / leverage / risk score are read-only status, not position sizing
- not account-risk write
- not PushSnapshot write
- not Push send
- not Recheck execution
- not trading authorization
- not position sizing
- not reduce / close / stop / reverse guidance
- not Candidate / Decision generation / Point
- not final direction / entry / stop / TP / RR
- not order / execution / auto-trading
- Display Slots are not a candidate pool

The dashboard must not add buttons, execution links, Push/Recheck actions,
account action controls, sizing controls, order controls, trading controls, or
Position Monitor action surfaces.

## Allowed Implementation Files

If the next B-risk implementation proceeds, the maximum file scope is:

- Existing controller owner path, such as `DashboardController.java` or an
  already-related controller: at most one minimal read-only `Map` endpoint if
  needed.
- `dashboard.html`: only a minimal account risk / exposure status panel, DOM
  ids, and safety copy.
- Targeted controller/dashboard tests: endpoint safety flags, fail-closed
  states, account action boundary, forbidden executable fields absent, and
  dashboard DOM / safety copy.
- Existing account-risk or review aggregate tests: only tiny owner-path read
  assertions if needed, without expanding business semantics.
- Implementation report docs.
- Source-of-truth docs.

## Forbidden Files And Scope

The next implementation remains NO-GO for:

- schema/config/pom changes
- new DTO / Validator / Assembler / Orchestrator
- new service/domain/mapper/repository ownership family
- account-risk snapshot write behavior
- PushSnapshot write behavior
- Push send or external channel
- Recheck execution
- trading authorization
- position sizing
- reduce / close / stop / reverse guidance
- Candidate generation
- Decision generation
- Point generation
- final direction
- entry / stop / TP / RR
- order / execution / auto-trading
- Position Monitor execution
- replay / recheck execution
- P359 / P360

## Required Safety Fields

The future implementation must force these safety fields:

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

## Required Status Mapping

The future implementation must cover:

- `ACCOUNT_RISK_STATUS_REVIEW_ONLY_READY`
- `ACCOUNT_RISK_STATUS_BACKEND_PENDING_FAIL_CLOSED`
- `ACCOUNT_RISK_STATUS_MISSING_FAIL_CLOSED`
- `ACCOUNT_RISK_STATUS_PARTIAL_REVIEW_ONLY`
- `ACCOUNT_EXPOSURE_REVIEW_ONLY_READY`
- `ACCOUNT_EXPOSURE_MISSING_FAIL_CLOSED`
- `RISK_ALLOWED_READ_ONLY_EVIDENCE`
- `ACCOUNT_RISK_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `PUSH_SEND_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `TRADING_AUTHORIZATION_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `POSITION_SIZING_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `REDUCE_CLOSE_STOP_REVERSE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED`

## Required Tests For Implementation

The next implementation must include targeted tests proving:

- endpoint safety flags are present and true
- missing read path / missing snapshot fail closed
- partial snapshot remains review-only and non-executable
- `riskAllowed` is read-only evidence only
- exposure / leverage / risk score are read-only status only
- account-risk write and PushSnapshot write boundaries are blocked
- Push send and Recheck execution boundaries are blocked
- trading authorization and position sizing fields are absent
- reduce / close / stop / reverse guidance is absent as executable guidance
- Candidate / Decision generation / Point / final direction / entry-stop-TP-RR
  fields are absent
- order / execution / auto-trading fields are absent
- dashboard DOM ids and safety copy are present if the panel is added
- implementation does not call `insert(...)`,
  `PushSnapshotService.ensureAccountRiskSnapshot(...)`,
  `PushSnapshotService.insertAuthoritativeSnapshot(...)`,
  `PushRecheckServiceImpl.recheck(...)`, or
  `AnalysisAssemblerServiceImpl.saveToDatabase(...)`

## NO-GO Conditions

Implementation must stop if it requires any of the following:

- account-risk write
- PushSnapshot write
- Push send
- Recheck execution
- trading authorization
- position sizing
- reduce / close / stop / reverse guidance
- Candidate generation
- Decision generation
- Point generation
- final direction
- entry / stop / TP / RR
- order / execution / auto-trading
- Position Monitor execution
- replay / recheck execution
- schema/config/pom changes
- new DTO / Validator / Assembler / Orchestrator
- new service/domain/mapper/repository ownership family
- inability to guarantee `riskAllowed` is read-only evidence
- inability to guarantee exposure / leverage / risk score are read-only status
- inability to guarantee this module is account risk / exposure status only,
  not account action or trade execution

## Account Action Boundary

`riskAllowed`, exposure, leverage, and risk score are evidence values only.

They may explain account-risk status to a human reviewer. They must not become:

- executable readiness
- trading authorization
- position sizing
- reduce / close / stop / reverse guidance
- account action
- order intent
- execution intent
- auto-trading intent

Any future implementation that cannot preserve this boundary is NO-GO.

## Source-Of-Truth Handoff

Current Mainline:

- `400e604 docs(risk): design account exposure runtime wiring`

Current Block:

- `Minimal Review-Only Account Risk / Account Exposure Status Runtime Wiring Implementation`

Capability Movement:

- None from this readiness package. The project remains
  `REVIEW_ONLY_RUNTIME partial` with 16 completed partial runtime slices.

User-visible Output:

- Readiness decision is GO to B-risk minimal implementation.
- Future implementation must create a Draft PR and stop for review.

Overreach Boundary:

- No Java business code, tests, dashboard business logic, schema/config/pom,
  endpoint/panel implementation, account-risk write, PushSnapshot write, Push
  send, Recheck execution, trading authorization, position sizing, action
  guidance, Candidate, Decision generation, Point, final direction,
  entry/stop/TP/RR, order/execution, auto-trading, Position Monitor execution,
  replay/recheck, P359, or P360 was changed by this package.

## Next Allowed Action

`Minimal Review-Only Account Risk / Account Exposure Status Runtime Wiring Implementation`

Suggested next branch:
`minimal-review-only-account-risk-account-exposure-status-runtime-wiring-implementation`

Future implementation risk:
`B`
