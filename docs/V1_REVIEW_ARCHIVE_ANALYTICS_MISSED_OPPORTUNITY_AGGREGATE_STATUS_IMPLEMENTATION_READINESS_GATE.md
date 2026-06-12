# V1 Review Archive Analytics / Missed Opportunity Aggregate Status Implementation Readiness Gate

## Current Merged Main

- Effective execution baseline: `91332b7 docs(review): design review archive aggregate runtime wiring`
- Current module: `Review Archive Analytics / Missed Opportunity Aggregate Status`
- Current phase: `Implementation readiness gate`
- Current capability level: `REVIEW_ONLY_RUNTIME partial`
- Completed review-only runtime partial slices: 14
- Package risk: A

## Source-Read Summary

Source Read confirmed an existing read-only owner path for review archive and missed opportunity visibility:

- `MissedOpportunityController`
- `MissedOpportunityService` read/query/count methods
- `MissedOpportunityServiceImpl` read/query/count methods
- `MissedOpportunityMapper` read/query/count methods
- `/api/missed-opportunity/query`
- `/api/missed-opportunity/review-archive-status`
- `ReviewAggregateService` missed section metadata
- dashboard `missedArchiveStatusPanel`
- review page missed section
- existing missed archive / fail-closed / review-only tests

The source read also confirmed write and generation paths exist nearby, but they are not valid owner paths for this slice.

## Design Summary

The design selects the existing `/api/missed-opportunity/review-archive-status` owner path and dashboard `missedArchiveStatusPanel` as the preferred minimal runtime surface. The slice must remain archive analytics / aggregate status only.

The design explicitly rejects missed-opportunity generation, missed-opportunity write behavior, review result generation, replay execution, recheck execution, Push send, external channel, Candidate generation, Decision generation, Point generation, final direction, entry / stop / TP / RR, order / execution, auto-trading, Position Monitor execution, new DTO / Validator / Assembler / Orchestrator, new service/domain/mapper/repository ownership family, schema/config/pom, P359, and P360.

## Existing Assets Found

- `src/main/java/org/example/trademodel/controller/MissedOpportunityController.java`
  - Existing read-only `GET /api/missed-opportunity/review-archive-status`.
  - Existing `GET /api/missed-opportunity/query` read path.
- `src/main/java/org/example/trademodel/service/MissedOpportunityService.java`
  - Existing read/query/count contract.
- `src/main/java/org/example/trademodel/service/impl/MissedOpportunityServiceImpl.java`
  - Existing read/query/count implementation.
  - Adjacent `save` path is write-side and forbidden for this slice.
- `src/main/java/org/example/trademodel/mapper/MissedOpportunityMapper.java`
  - Existing read/query/count SQL owner path.
  - Adjacent `insert` path is write-side and forbidden for this slice.
- `src/main/java/org/example/trademodel/service/ReviewAggregateService.java`
  - Existing aggregate summary/detail read path.
- `src/main/java/org/example/trademodel/service/impl/ReviewAggregateServiceImpl.java`
  - Existing missed section metadata and missed detail read path.
- `src/main/resources/templates/dashboard.html`
  - Existing `missedArchiveStatusPanel` and missed archive DOM ids.
- `src/test/java/org/example/trademodel/controller/MissedOpportunityControllerTest.java`
  - Existing endpoint review-only / fail-closed safety coverage.
- `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`
  - Existing dashboard DOM / safety copy coverage.

## Missing Assets / Gaps

- Current status endpoint already carries many review-only and generation-boundary fields, but the next implementation should make the aggregate analytics safety contract explicit for:
  - `manualReviewOnly`
  - `notMissedOpportunityWrite`
  - `notPushSend`
  - `notExternalChannel`
  - `notFinalDirection`
  - `notEntryStopTpRr`
- Aggregate analytics wording must avoid implying a new generator, scorer, replay, recheck, Push, Candidate, Point, or trading decision.
- Dashboard copy must keep archive count and missed opportunity count as review-only aggregate status, not a recommendation or execution signal.

## Readiness Decision

GO to B-risk minimal implementation.

The next package may proceed only because the existing read-only owner path is sufficient and the remaining work can be limited to endpoint/status field completion, dashboard copy/DOM completion, targeted tests, implementation docs, and source-of-truth updates.

## Owner Path Decision

Preferred owner path:

- Reuse `GET /api/missed-opportunity/review-archive-status`.
- Reuse `missedArchiveStatusPanel`.
- Reuse `MissedOpportunityService` read/query/count methods.
- Reuse `ReviewAggregateService` missed summary/detail metadata only as read-only context.

Dedicated aggregate analytics endpoint:

- Default decision: not needed.
- If implementation proves a separate surface is necessary, it may only be one minimal read-only `Map` endpoint in the existing controller owner family.
- It must not introduce DTO / Validator / Assembler / Orchestrator or a new service/domain/mapper/repository ownership family.

## Allowed Implementation Files

The next B-risk implementation may modify only:

- `src/main/java/org/example/trademodel/controller/MissedOpportunityController.java`
  - Only to extend the existing read-only status output or, if unavoidable, add one minimal read-only `Map` endpoint.
  - It may call only existing read/query/count owner paths.
- `src/main/resources/templates/dashboard.html`
  - Only minimal missed archive aggregate status panel/copy/DOM updates.
  - Prefer the existing `missedArchiveStatusPanel`.
- `src/test/java/org/example/trademodel/controller/MissedOpportunityControllerTest.java`
  - Endpoint safety flags, fail-closed status, generation/write/replay/recheck/Push/Candidate/Point/trading boundary, forbidden executable fields absent.
- `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`
  - Dashboard DOM and safety copy if dashboard copy/DOM changes.
- Existing missed opportunity / review aggregate tests
  - Only tiny owner-path assertions when needed.
  - No business semantic expansion.
- `docs/V1_MINIMAL_REVIEW_ONLY_REVIEW_ARCHIVE_ANALYTICS_MISSED_OPPORTUNITY_AGGREGATE_STATUS_RUNTIME_WIRING_IMPLEMENTATION.md`
- Source-of-truth docs.

## Forbidden Files And Ownership

Forbidden for the next implementation:

- Schema / config / pom changes.
- New DTO / Validator / Assembler / Orchestrator.
- New service / domain / mapper / repository ownership family.
- Any write-side implementation path such as `MissedOpportunityServiceImpl#save` or `MissedOpportunityMapper#insert`.
- Any review result generation owner path.
- Replay or recheck execution owner paths.
- Push send or external channel owner paths.
- Candidate / Decision generation / Point / Trading owner paths.
- Position Monitor execution owner paths.
- P359 / P360.

## Required Safety Fields

The next implementation must expose or preserve these safety fields:

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

## Required Status Mapping

The next implementation must cover:

- `REVIEW_ARCHIVE_AGGREGATE_REVIEW_ONLY_READY`
- `REVIEW_ARCHIVE_AGGREGATE_BACKEND_PENDING_FAIL_CLOSED`
- `REVIEW_ARCHIVE_AGGREGATE_MISSING_FAIL_CLOSED`
- `REVIEW_ARCHIVE_AGGREGATE_PARTIAL_REVIEW_ONLY`
- `MISSED_OPPORTUNITY_COUNT_REVIEW_ONLY`
- `REVIEW_ARCHIVE_COUNT_REVIEW_ONLY`
- `MISSED_OPPORTUNITY_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `MISSED_OPPORTUNITY_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `REVIEW_RESULT_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED`

## Fail-Closed Rules

The next implementation must fail closed when:

- The existing status endpoint cannot read its required source.
- Missed opportunity count cannot be read.
- Review archive aggregate context cannot be read.
- Existing owner path returns null / missing / inconsistent data.
- Aggregate status cannot distinguish read-only archive visibility from generation or write behavior.
- Any write, generation, replay, recheck, Push, Candidate, Point, final direction, entry / stop / TP / RR, order/execution, or trading semantics would be required.

Read-only empty counts may be represented as review-only empty status only when the read model itself is available.

## Generation / Execution Boundary

Allowed:

- Count existing missed opportunity rows.
- Read existing missed opportunity archive status.
- Read existing review aggregate missed section metadata.
- Display review-only aggregate status and safety copy.

Forbidden:

- Missed-opportunity generation.
- Missed-opportunity write behavior.
- Review result generation.
- Replay execution.
- Recheck execution.
- Push send or external channel.
- Candidate generation.
- Decision generation.
- Point generation.
- Final direction, entry, stop, TP, RR output.
- Order, execution, auto-trading.
- Position Monitor execution.

## Required Tests For Next Implementation

The next implementation must include targeted tests for:

- Endpoint safety fields.
- Fail-closed status when read/count/aggregate data is missing or unavailable.
- Read-only empty count behavior.
- Generation/write/replay/recheck/Push/Candidate/Point/trading boundaries.
- Forbidden executable fields absent.
- Dashboard `missedArchiveStatusPanel` DOM / copy / safety copy when dashboard changes.
- No call path to write-side missed opportunity save/insert behavior.
- No call path to review result generation, replay execution, recheck execution, Push, Candidate, Point, final direction, entry / stop / TP / RR, order/execution, or trading.

## NO-GO Conditions

The next implementation is NO-GO if it requires:

- Missed-opportunity generation.
- Missed-opportunity write.
- Review result generation.
- Replay execution.
- Recheck execution.
- Push send / external channel.
- Candidate generation.
- Decision generation.
- Point generation.
- Final direction.
- Entry / stop / TP / RR.
- Order / execution / auto-trading.
- Position Monitor execution.
- Schema / config / pom changes.
- New DTO / Validator / Assembler / Orchestrator.
- New service / domain / mapper / repository ownership family.
- Any behavior that makes this archive analytics / aggregate status package a generation or execution package.

## Risk Level

Next implementation risk: B.

Reason: the next package may touch controller/dashboard/test files, but only within existing owner paths and only for review-only status wiring.

## Next Allowed Action

`Minimal Review-Only Review Archive Analytics / Missed Opportunity Aggregate Status Runtime Wiring Implementation`

## Next Branch

`minimal-review-only-review-archive-analytics-missed-opportunity-aggregate-status-runtime-wiring-implementation`

## Freeze Compliance

- 是否创建新骨架：否。
- 是否复用 Cursor-era / V1 资产：是，复用 MissedOpportunity / ReviewAggregate / dashboard missed archive owner path。
- 是否减少重复：是，明确继续复用现有 `/api/missed-opportunity/review-archive-status`，不新增 owner family。
- 是否提升 capability level：否，仍为 `REVIEW_ONLY_RUNTIME partial`。
- 是否接 service/runtime/dashboard/API：本包不接；下一包仅允许最小只读 status wiring。
- 是否符合 #830：是，避免 DTO / Validator / Assembler / Orchestrator / duplicated owner expansion。
