# Minimal Review-Only Account Risk / Account Exposure Status Runtime Wiring Implementation

## Result

This B-risk implementation package wires the minimal Account risk / account exposure status surface as review-only runtime visibility.

- PR mode: Draft PR required; no auto-merge.
- Capability movement: none. V1 remains `REVIEW_ONLY_RUNTIME partial`.
- Completed slice count: still 16 until the follow-up verification and visual closure packages are merged.
- Next allowed action: `Minimal Review-Only Account Risk / Account Exposure Status Runtime Wiring Verification`.

## Implemented Endpoint / Owner Path

- Endpoint: `GET /api/dashboard/account-risk-exposure-status?symbol=BTCUSDT`
- Controller owner: `DashboardController`
- Runtime owner path:
  - read latest `DecisionResult` only to obtain `analysisId`
  - read `TmAccountRiskSnapshotDO` through `AccountRiskSnapshotMapper.selectLatestByAnalysisId(analysisId)`
  - `AccountRiskSnapshotMapper.selectById(...)` remains historical snapshot read context only and is not used as the runtime owner path
- Display context:
  - `ReviewAggregate` / review-page account-risk display evidence remains context only
  - no execution or action entry is introduced from review-page display context

The endpoint returns a simple `Map` status projection and does not introduce DTO, Validator, Assembler, Orchestrator, service, domain, mapper, repository, schema, config, or pom ownership.

## Dashboard Panel / DOM

The dashboard adds the minimal read-only panel:

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

The panel copy states that `riskAllowed` is read-only evidence, not trading authorization, and that exposure / risk score are read-only status, not position sizing.

## Safety Fields

The endpoint and dashboard status contract expose these safety fields:

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

## Status Mapping / Fail-Closed Rules

- `ACCOUNT_RISK_STATUS_REVIEW_ONLY_READY`: latest account-risk snapshot is readable and has enough status/exposure evidence.
- `ACCOUNT_RISK_STATUS_BACKEND_PENDING_FAIL_CLOSED`: read path throws.
- `ACCOUNT_RISK_STATUS_MISSING_FAIL_CLOSED`: no DecisionResult, missing analysis id, or no latest snapshot.
- `ACCOUNT_RISK_STATUS_PARTIAL_REVIEW_ONLY`: snapshot exists but core risk evidence is partial.
- `ACCOUNT_EXPOSURE_REVIEW_ONLY_READY`: exposure fields are readable.
- `ACCOUNT_EXPOSURE_MISSING_FAIL_CLOSED`: exposure fields are missing.
- `RISK_ALLOWED_READ_ONLY_EVIDENCE`: `riskAllowed` is present only as read-only evidence.
- `ACCOUNT_RISK_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED`: account-risk writes stay blocked.
- `PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED`: PushSnapshot writes stay blocked.
- `PUSH_SEND_BOUNDARY_BLOCKED_FAIL_CLOSED`: Push send stays blocked.
- `RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED`: Recheck execution stays blocked.
- `TRADING_AUTHORIZATION_BOUNDARY_BLOCKED_FAIL_CLOSED`: trading authorization stays blocked.
- `POSITION_SIZING_BOUNDARY_BLOCKED_FAIL_CLOSED`: position sizing stays blocked.
- `REDUCE_CLOSE_STOP_REVERSE_BOUNDARY_BLOCKED_FAIL_CLOSED`: reduce / close / stop / reverse guidance stays blocked.
- `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED`: Candidate generation stays blocked.
- `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED`: Point generation stays blocked.
- `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED`: trading / order / execution stays blocked.

## Account Action Boundary

This implementation is account risk / exposure status only.

It does not write account-risk snapshots, write PushSnapshot data, send Push, execute Recheck, generate trading authorization, produce position sizing, produce reduce / close / stop / reverse guidance, generate Candidate / Decision / Point, produce final direction, produce entry / stop / TP / RR, or touch order / execution / auto-trading.

## Targeted Tests

`DashboardControllerTest` adds coverage for:

- endpoint safety flags
- fail-closed missing snapshot
- fail-closed read-path exception
- fail-closed missing exposure
- owner path assertion: `selectLatestByAnalysisId(...)` is used
- owner path assertion: `selectById(...)` is not used as runtime owner path
- forbidden executable / action fields absent
- dashboard DOM ids and safety copy

## Forbidden Scope Check

No changes were made to:

- schema / config / pom
- DTO / Validator / Assembler / Orchestrator
- service / domain / mapper / repository ownership family
- account-risk write behavior
- PushSnapshot write behavior
- Push send / external channel
- replay / recheck execution
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
- P359 / P360

## #830 Duplicate Skeleton Freeze Audit

- New skeleton created: no.
- Cursor-era assets reused: yes, `tm_account_risk_snapshot`, `TmAccountRiskSnapshotDO`, and `AccountRiskSnapshotMapper.selectLatestByAnalysisId(...)`.
- Duplication reduced: yes, the status surface wires an existing account-risk read model instead of creating a parallel owner.
- Capability uplift: no.
- Service / runtime / dashboard / API wiring: yes, a minimal review-only existing-owner runtime/dashboard/API status path.
- #830 audit fit: yes, this package wires existing assets and does not expand duplicate DTO / Validator / Assembler / Orchestrator / service ownership.
