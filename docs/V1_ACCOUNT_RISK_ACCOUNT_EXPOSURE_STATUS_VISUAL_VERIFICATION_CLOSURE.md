# V1 Account Risk / Account Exposure Status Visual Verification / Closure

## Scope

This package closes visual verification for `Account risk / account exposure
status`.

It is visual closure documentation only. It does not implement endpoint behavior,
dashboard behavior, Java business code, tests, schema/config/pom changes,
account-risk write behavior, PushSnapshot write behavior, Push send, Recheck
execution, trading authorization, position sizing, reduce / close / stop /
reverse guidance, Candidate generation, Decision generation, Point generation,
final direction, entry / stop / TP / RR, order / execution / auto-trading,
Position Monitor execution, replay / recheck execution, P359, or P360.

## Visual Closure Result

PASS.

Account risk / account exposure status has enough dashboard template, endpoint,
test, and verification-document evidence to close the review-only visual slice.

This closure marks `Account risk / account exposure status` as the 17th completed
`REVIEW_ONLY_RUNTIME partial` slice after this package is merged.

## Visual Evidence

Environment-limited evidence only.

No live browser or screenshot was captured in this package, and this document
does not claim live UI success. Visual closure is based on dashboard template
DOM/copy, targeted `DashboardControllerTest` template assertions, endpoint/test
evidence, and the completed runtime wiring verification record.

Verified dashboard panel and DOM ids:

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
  execution, not trading authorization, not position sizing, not reduce / close
  / stop / reverse guidance, not candidate, not decision generation, not point,
  not final direction, not entry / stop / TP / RR, not trading, not executable,
  and Display Slots are not a candidate pool.

## Endpoint And Dashboard Evidence

The visual evidence aligns with runtime wiring verification:

- Endpoint: `GET /api/dashboard/account-risk-exposure-status?symbol=BTCUSDT`
- Runtime owner path:
  - latest `DecisionResult` -> `analysisId`
  - `AccountRiskSnapshotMapper.selectLatestByAnalysisId(...)`
  - `AccountRiskSnapshotMapper.selectById(...)` remains historical snapshot read
    only and is not the runtime owner path
- Template evidence: `dashboard.html` includes the panel DOM ids and safety copy.
- Test evidence: `DashboardControllerTest` covers dashboard DOM/copy, endpoint
  safety fields, owner-path assertions, fail-closed states, account action
  boundary, and forbidden executable/action fields absent.
- Verification evidence:
  `docs/V1_ACCOUNT_RISK_ACCOUNT_EXPOSURE_STATUS_RUNTIME_WIRING_VERIFICATION.md`.

## Account Action Boundary Visual Evidence

The panel copy and endpoint/test evidence keep account-risk visibility as status
only.

Confirmed negative account action boundaries:

- no account-risk write
- no PushSnapshot write
- no Push send
- no Recheck execution
- no trading authorization
- no position sizing
- no reduce / close / stop / reverse guidance
- no Candidate generation
- no Decision generation
- no Point generation
- no final direction / entry / stop / TP / RR
- no Push / external channel
- no order / execution / auto-trading
- no Position Monitor execution
- no replay / recheck execution

## Forbidden Scope Check

This package changes only visual closure and source-of-truth documentation.

No Java business code, tests, dashboard business logic, schema/config/pom,
DTO / Validator / Assembler / Orchestrator, service/domain/mapper/repository
ownership family, endpoint behavior, or dashboard behavior is changed.

It does not write account-risk snapshots or PushSnapshot data, send Push, execute
Recheck, emit trading authorization, emit position sizing, emit reduce / close /
stop / reverse guidance, generate Candidate / Decision / Point, emit final
direction / entry / stop / TP / RR, or trigger order / execution / trading.

## Completed Slice Count

Completed Review-Only Runtime partial slices after this package is merged:

17.

The capability level remains `REVIEW_ONLY_RUNTIME partial`.

## Next Allowed Action

Next allowed action:

`Next minimal runtime slice selection after Account Risk / Account Exposure Status closure`

Next branch:

`next-minimal-runtime-slice-selection-after-account-risk-account-exposure`

The next package is A-risk selection only unless a later source-read package
explicitly scopes a different risk.

## #830 Duplicate Skeleton Freeze Audit

- New skeleton created: no.
- Cursor-era assets reused: yes, the closure validates the existing account-risk
  snapshot owner path and dashboard/API status surface.
- Duplication reduced: yes, the slice closes around the existing account-risk
  read owner instead of creating a parallel owner.
- Capability uplift: no; closure completes a partial slice but does not raise the
  global capability level.
- Service / runtime / dashboard / API wiring: yes, verified as already wired by
  the implementation and verification packages.
- #830 audit fit: yes, this package closes an existing runtime/dashboard/API
  review-only path without new duplicate skeletons.
