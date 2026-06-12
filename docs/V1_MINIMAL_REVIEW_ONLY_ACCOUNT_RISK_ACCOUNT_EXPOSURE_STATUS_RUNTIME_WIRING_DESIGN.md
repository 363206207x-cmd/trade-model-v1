# V1 Minimal Review-Only Account Risk / Account Exposure Status Runtime Wiring Design

## Scope

This package designs the minimal review-only runtime wiring for
`Account risk / account exposure status`.

It is design only. It does not implement an endpoint, dashboard panel, Java
business logic, tests, schema/config/pom changes, write behavior, Push/Recheck
execution, account action, position sizing, trading authorization, Candidate,
Decision generation, Point, final direction, entry/stop/TP/RR, order/execution,
auto-trading, Position Monitor execution, P359, or P360.

The effective execution baseline is
`1d87e1d docs(risk): read account exposure source path`.

## Source-Read Inputs

The design is based on the completed source read in
`docs/V1_ACCOUNT_RISK_ACCOUNT_EXPOSURE_STATUS_SOURCE_READ.md` and the existing
assets it confirmed:

- `tm_account_risk_snapshot`
- `TmAccountRiskSnapshotDO`
- `AccountRiskSnapshotMapper.selectById(...)`
- `AccountRiskSnapshotMapper.selectLatestByAnalysisId(...)`
- `PushSnapshotService`
- `PushRecheckServiceImpl`
- `ReviewAggregateServiceImpl`
- `ReviewAggregateVO.ReviewPushSummary` account-risk fields
- `AnalysisAssemblerServiceImpl`
- `review-page.js`
- `dashboard.html`
- related account-risk tests

## Owner Path Decision

The canonical read owner for the future status is the existing account-risk
snapshot read model:

`tm_account_risk_snapshot -> TmAccountRiskSnapshotDO -> AccountRiskSnapshotMapper`

`AccountRiskSnapshotMapper.selectLatestByAnalysisId(...)` should be the preferred
read source when an `analysisId` is available, because it reads the current
latest account-risk snapshot for that analysis without creating a new snapshot.
`selectById(...)` can be used only for a known historical snapshot reference,
such as review/push aggregate context. Neither read method may be wrapped in a
new service/domain/mapper/repository family.

`ReviewAggregateServiceImpl` and
`ReviewAggregateVO.ReviewPushSummary` are reusable display evidence for
account-risk fields, but they are not an execution path. `review-page.js`
account-risk display is display context only; it must not become a Push/Recheck,
trade, or account action entry.

`PushSnapshotService`, `PushRecheckServiceImpl`, and
`AnalysisAssemblerServiceImpl` are boundary evidence, not safe status owners:

- `PushSnapshotService` writes account-risk snapshots.
- `PushRecheckServiceImpl` performs recheck behavior and writes push/recheck
  status/log rows.
- `AnalysisAssemblerServiceImpl` persists execution-plan account-risk JSON.

The future status must not call those write/execution/persist methods.

## Endpoint Decision

The next implementation should first try to reuse the existing account-risk
snapshot read owner path. A dedicated endpoint is allowed only if the readiness
gate confirms that a dashboard status surface needs one.

If a dedicated endpoint is allowed, it must be at most one minimal read-only
`Map` endpoint, for example a dashboard-scoped status projection keyed by
`analysisId`. The endpoint may expose only:

- status identifiers
- fail-closed / partial flags
- safety booleans
- snapshot evidence fields already present in `TmAccountRiskSnapshotDO`
- boundary summaries

The endpoint must not expose or derive:

- trading authorization
- position sizing
- reduce / close / stop / reverse guidance
- Candidate / Decision generation / Point
- final direction
- entry / stop / TP / RR
- order / execution / auto-trading

No DTO, Validator, Assembler, Orchestrator, schema/config/pom change, or new
service/domain/mapper/repository ownership family is needed for this design.

## Dashboard Design

The dashboard may add a minimal account-risk status panel only if the readiness
gate confirms it is needed. The panel must be status-only and must not change
dashboard business logic.

Proposed minimal DOM ids for the future implementation:

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
- riskAllowed is read-only evidence, not trading authorization
- exposure / leverage / risk score are read-only status, not position sizing
- not account-risk write
- not PushSnapshot write
- not Push send
- not Recheck execution
- not reduce / close / stop / reverse guidance
- not Candidate / Decision generation / Point
- not final direction / entry / stop / TP / RR
- not order / execution / auto-trading
- Display Slots are not a candidate pool

The dashboard must not add buttons, send actions, recheck actions, sizing
actions, account actions, trading actions, Position Monitor actions, or
execution-adjacent links.

## Status Mapping

| Status | Meaning | Fail closed |
|---|---|---:|
| `ACCOUNT_RISK_STATUS_REVIEW_ONLY_READY` | Latest account-risk snapshot is readable and safety boundaries are present. | false |
| `ACCOUNT_RISK_STATUS_BACKEND_PENDING_FAIL_CLOSED` | Mapper/read path throws or cannot be trusted. | true |
| `ACCOUNT_RISK_STATUS_MISSING_FAIL_CLOSED` | No analysis id or no readable snapshot is available. | true |
| `ACCOUNT_RISK_STATUS_PARTIAL_REVIEW_ONLY` | Snapshot exists but some non-executable display fields are absent. | false |
| `ACCOUNT_EXPOSURE_REVIEW_ONLY_READY` | `positionExposure` / `maxAllowedExposure` are available as display-only evidence. | false |
| `ACCOUNT_EXPOSURE_MISSING_FAIL_CLOSED` | Exposure evidence is absent and cannot be summarized safely. | true |
| `RISK_ALLOWED_READ_ONLY_EVIDENCE` | `riskAllowed` is shown only as historical/read-only evidence. | false |
| `ACCOUNT_RISK_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any account-risk write path is requested or detected. | true |
| `PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any PushSnapshot write behavior is requested or detected. | true |
| `PUSH_SEND_BOUNDARY_BLOCKED_FAIL_CLOSED` | Push send or external notification behavior is requested or detected. | true |
| `RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED` | Recheck execution behavior is requested or detected. | true |
| `TRADING_AUTHORIZATION_BOUNDARY_BLOCKED_FAIL_CLOSED` | `riskAllowed` is treated as authorization instead of evidence. | true |
| `POSITION_SIZING_BOUNDARY_BLOCKED_FAIL_CLOSED` | Exposure/leverage/risk score is interpreted as sizing guidance. | true |
| `REDUCE_CLOSE_STOP_REVERSE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Reduce/close/stop/reverse wording becomes action guidance. | true |
| `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Candidate signal/generation/ranking behavior is requested or detected. | true |
| `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED` | Point/final-direction/entry-stop-TP-RR behavior is requested or detected. | true |
| `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED` | Order/execution/auto-trading behavior is requested or detected. | true |

## Safety Fields

The future status contract must force these values:

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

## Fail-Closed Rules

- Mapper/read exception -> `ACCOUNT_RISK_STATUS_BACKEND_PENDING_FAIL_CLOSED`.
- Missing analysis id or missing latest snapshot ->
  `ACCOUNT_RISK_STATUS_MISSING_FAIL_CLOSED`.
- Snapshot exists but optional display values are incomplete ->
  `ACCOUNT_RISK_STATUS_PARTIAL_REVIEW_ONLY`.
- Missing exposure evidence -> `ACCOUNT_EXPOSURE_MISSING_FAIL_CLOSED`.
- `riskAllowed=false` remains account-risk evidence only. It may explain risk
  state, but it must not create account action or trading authorization.
- `riskAllowed=true` remains account-risk evidence only. It must not be rendered
  as executable readiness, trading authorization, or permission to size/open a
  position.
- Any attempted account-risk write, PushSnapshot write, Push send, Recheck
  execution, trading authorization, position sizing, reduce/close/stop/reverse
  guidance, Candidate, Decision generation, Point, final direction,
  entry/stop/TP/RR, order/execution, auto-trading, or Position Monitor execution
  forces a boundary-blocked fail-closed status.

## Implementation Readiness Gate Checklist

The next readiness gate must decide:

1. Whether implementation is GO or NO-GO.
2. Whether the existing account-risk snapshot read owner path is sufficient.
3. Whether a dedicated status endpoint is truly needed.
4. Whether one minimal read-only `Map` endpoint is enough if an endpoint is
   needed.
5. Whether a minimal dashboard account-risk status panel is needed.
6. Which exact files can change in the B-risk implementation.
7. Which targeted tests must prove safety fields, fail-closed rules, forbidden
   fields absent, and dashboard safety copy.
8. Whether any implementation would require DTO / Validator / Assembler /
   Orchestrator, schema/config/pom, or new service/domain/mapper/repository
   ownership. If yes, implementation is NO-GO.

## Maximum Future Implementation Scope If Allowed

If the readiness gate returns GO, the maximum future implementation scope should
be:

- one existing controller owner path with at most one minimal read-only `Map`
  endpoint, if needed
- `dashboard.html` minimal account-risk status panel/copy/DOM, if needed
- targeted controller/dashboard tests
- optional tiny existing owner-path assertion in existing account-risk tests
- implementation report docs
- source-of-truth docs

It must not modify schema/config/pom, add DTO / Validator / Assembler /
Orchestrator, add service/domain/mapper/repository ownership, or alter write,
Push, Recheck, Position Monitor, Candidate, Decision, Point, or trading behavior.

## No-Go Conditions

Implementation must stop if it requires:

- account-risk snapshot write
- PushSnapshot write
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
- schema/config/pom
- new DTO / Validator / Assembler / Orchestrator
- new service/domain/mapper/repository ownership family
- P359 / P360

## Next Allowed Action

`Implementation readiness gate for Account risk / account exposure status`

Suggested next branch:
`account-risk-account-exposure-status-implementation-readiness-gate`
