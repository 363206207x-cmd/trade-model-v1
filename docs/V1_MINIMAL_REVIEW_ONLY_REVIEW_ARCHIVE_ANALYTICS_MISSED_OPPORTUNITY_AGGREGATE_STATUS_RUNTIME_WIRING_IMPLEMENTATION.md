# V1 Minimal Review-Only Review Archive Analytics / Missed Opportunity Aggregate Status Runtime Wiring Implementation

## Current Merged Main

- Current merged main at package start: `b6f29ac docs(review): verify review archive aggregate readiness`
- Current module: `Review Archive Analytics / Missed Opportunity Aggregate Status`
- Current phase: `Implementation`
- Risk: `B`
- Capability level: `REVIEW_ONLY_RUNTIME partial`
- Completed review-only runtime partial slices before this package: `14`

## Implementation Result

Result: implemented the minimal review-only runtime wiring over the existing Missed Opportunity / Review Archive owner path.

This package reuses the existing `GET /api/missed-opportunity/review-archive-status` endpoint and `missedArchiveStatusPanel` dashboard surface. It does not add a new DTO, Validator, Assembler, Orchestrator, service, domain, mapper, repository, schema, config, or pom ownership family.

## Implemented Endpoint / Owner Path

- Endpoint: `GET /api/missed-opportunity/review-archive-status`
- Owner path reused:
  - `MissedOpportunityController`
  - `MissedOpportunityService#countByBizDate`
  - `MissedOpportunityService#findByMissedId`
  - `MissedOpportunityService#query`
  - `tm_missed_opportunity` read model
  - `MissedReasonViewParser`
- Write-side methods remain excluded from the status path:
  - `MissedOpportunityService#save`
  - `MissedOpportunityService#recordFromAuthoritativeAnalysisIfEligible`

## Dashboard Panel / DOM

Reused and tightened:

- `missedArchiveStatusPanel`
- `missedArchiveRuntimeStatusValue`
- `missedArchiveScopeValue`
- `missedArchiveCountValue`
- `missedArchiveLatestValue`
- `missedArchiveReasonParseValue`
- `missedArchiveSourceHealthValue`
- `missedArchiveReviewOnlyValue`
- `missedArchiveManualReviewValue`
- `missedArchiveSignalBoundaryValue`
- `missedArchiveGenerationBoundaryValue`
- `missedArchiveWritePushBoundaryValue`
- `missedArchiveExecutionBoundaryValue`
- `missedArchiveUpstreamValue`
- `missedArchiveReasonValue`

Dashboard copy explicitly states review-only, manual review only, no missed-opportunity generation, no missed-opportunity write, no review result generation, no replay / recheck execution, no Push send, no external channel, no Candidate, no Decision generation, no Point, no final direction, no entry / stop / TP / RR, no order / execution / auto-trading, and Display Slots are not a candidate pool.

## Status Mapping

Primary aggregate statuses:

- `REVIEW_ARCHIVE_AGGREGATE_REVIEW_ONLY_READY`
- `REVIEW_ARCHIVE_AGGREGATE_BACKEND_PENDING_FAIL_CLOSED`
- `REVIEW_ARCHIVE_AGGREGATE_MISSING_FAIL_CLOSED`
- `REVIEW_ARCHIVE_AGGREGATE_PARTIAL_REVIEW_ONLY`

Count statuses:

- `MISSED_OPPORTUNITY_COUNT_REVIEW_ONLY`
- `REVIEW_ARCHIVE_COUNT_REVIEW_ONLY`

Boundary statuses:

- `MISSED_OPPORTUNITY_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `MISSED_OPPORTUNITY_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `REVIEW_RESULT_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED`

## Safety Fields

The endpoint returns these safety fields as fixed review-only guardrails:

- `reviewOnly=true`
- `manualReviewOnly=true`
- `notMissedOpportunityGeneration=true`
- `notMissedOpportunityWrite=true`
- `notReviewResultGeneration=true`
- `notReplayExecution=true`
- `notRecheckExecution=true`
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

## Fail-Closed Rules

- Read exception on existing MissedOpportunity read owner path returns `REVIEW_ARCHIVE_AGGREGATE_BACKEND_PENDING_FAIL_CLOSED`.
- Missing archive row returns `REVIEW_ARCHIVE_AGGREGATE_MISSING_FAIL_CLOSED`.
- Count-only data returns `REVIEW_ARCHIVE_AGGREGATE_PARTIAL_REVIEW_ONLY`.
- Reason JSON parse failure returns `REVIEW_ARCHIVE_AGGREGATE_PARTIAL_REVIEW_ONLY` with `failClosed=true`.
- Missing archive linkage or trace id returns `REVIEW_ARCHIVE_AGGREGATE_PARTIAL_REVIEW_ONLY` with `failClosed=true`.
- Complete existing read model returns `REVIEW_ARCHIVE_AGGREGATE_REVIEW_ONLY_READY` with `failClosed=false`.

## Generation / Execution Boundary

This implementation does not:

- generate missed opportunity
- write missed opportunity
- generate review result
- execute replay
- execute recheck
- send Push
- call external channel
- generate Candidate
- generate Decision
- generate Point
- generate final direction
- generate entry / stop / TP / RR
- place order
- execute trade
- perform auto-trading
- execute Position Monitor behavior

## Targeted Tests

Targeted coverage added or updated:

- `MissedOpportunityControllerTest`
  - endpoint ready status
  - missing archive fail-closed
  - count-only partial
  - read-path exception fail-closed
  - reason parse fail-closed
  - forbidden executable / generation / write / Push fields absent
- `DashboardControllerTest`
  - `missedArchiveStatusPanel` DOM ids
  - aggregate status constants
  - generation / write / replay / recheck / Push / Candidate / Point / trading boundary copy

## Source Of Truth Handoff

After this implementation package is merged, the next allowed action is:

`Minimal Review-Only Review Archive Analytics / Missed Opportunity Aggregate Status Runtime Wiring Verification`

Next branch:

`review-archive-analytics-missed-opportunity-aggregate-status-runtime-wiring-verification`

The completed slice count remains `14`. Review Archive Analytics / Missed Opportunity Aggregate Status is not a completed small closure until verification and visual closure are completed and merged.

## Overreach Status

No overreach:

- no schema/config/pom
- no new DTO / Validator / Assembler / Orchestrator
- no new service/domain/mapper/repository ownership family
- no missed-opportunity generation/write behavior
- no review result generation
- no replay/recheck execution
- no Push send or external channel
- no Candidate generation
- no Decision generation
- no Point generation
- no final direction / entry / stop / TP / RR
- no order / execution / auto-trading
- no Position Monitor execution
- no P359 / P360
