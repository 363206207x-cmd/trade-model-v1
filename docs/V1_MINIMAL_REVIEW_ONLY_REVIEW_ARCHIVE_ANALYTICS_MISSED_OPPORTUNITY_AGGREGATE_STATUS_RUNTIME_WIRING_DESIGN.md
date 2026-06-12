# V1 Minimal Review-Only Review Archive Analytics / Missed Opportunity Aggregate Status Runtime Wiring Design

## 1. Executive Summary

This package is design only. It does not implement Java business code, tests, dashboard business logic, schema/config/pom changes, endpoint behavior, dashboard panel behavior, Push, Candidate generation, Decision generation, Point generation, final direction, entry/stop/TP/RR, order/execution, auto-trading, DTO, Validator, Assembler, Orchestrator, replay execution, recheck execution, missed-opportunity generation/write behavior, review result generation, P359, or P360.

- Current merged main: `f5e7092 docs(review): read review archive aggregate source path`
- Current module: `Review Archive Analytics / Missed Opportunity Aggregate Status`
- Current phase: `Design`
- Completed review-only runtime partial slices: 14
- Capability movement: none. The project remains `REVIEW_ONLY_RUNTIME partial`.

Design result: **GO to implementation readiness gate only**.

The future minimal runtime wiring should primarily reuse the existing owner path:

```text
tm_missed_opportunity / MissedOpportunityDO
  -> MissedOpportunityMapper read/query/count methods
  -> MissedOpportunityService read/query/count methods
  -> MissedOpportunityController GET /api/missed-opportunity/review-archive-status
  -> MissedReasonViewParser reason parse/source-health status
  -> ReviewAggregateService missed summary/detail metadata as context only
  -> review-page.js #sec-missed read-only archive table
  -> dashboard.html #missedArchiveStatusPanel
```

No new owner family is justified. The design should extend the completed Missed Opportunity / Review Archive status slice with archive analytics / aggregate read-only metadata, not duplicate it.

## 2. Design Answers

| Question | Design answer |
|---|---|
| Reuse existing `/api/missed-opportunity/review-archive-status` owner path? | **Yes.** This is the primary owner path. It already carries count/detail/source-health/safety flags and is safer than a new ownership family. |
| Need dedicated aggregate analytics endpoint? | **Default no.** If readiness gate proves the existing endpoint cannot safely carry aggregate metadata, allow at most one thin read-only `Map` endpoint or a narrow extension of the existing status endpoint. |
| Reuse `missedArchiveStatusPanel` or add new panel? | **Reuse/extend `missedArchiveStatusPanel` first.** A separate tiny aggregate row/panel is allowed only if extending the panel would make the completed base status ambiguous. |
| Safe read-only aggregate sources? | scoped missed count, today count, latest missed row, reason parse status, source health, `analysisId`, `symbol`, `bizDate`, trace/linkage presence, `ReviewAggregateSummaryVO.detailSections[missed].total`, `ReviewAggregateDetailVO.missed` availability. |
| How to mark not missed-opportunity generation? | Return `notMissedOpportunityGeneration=true`; readiness must prove no call to `recordFromAuthoritativeAnalysisIfEligible`, `save`, or mapper `insert`. |
| How to mark not missed-opportunity write? | Return `notMissedOpportunityWrite=true`; status must use read/query/count methods only. |
| How to mark not review result generation? | Return `notReviewResultGeneration=true`; `POST /api/review/save` and `ReviewService#saveOrUpdate` remain forbidden write paths. |
| How to mark not replay execution? | Return `notReplayExecution=true`; ReviewAggregate replay context is read-only context and cannot trigger replay. |
| How to mark not recheck execution? | Return `notRecheckExecution=true`; Push/Recheck aggregate assets are context-only and cannot trigger recheck. |
| How to mark not Push/external channel? | Return `notPushSend=true` and `notExternalChannel=true`; no notification send or external channel field is allowed. |
| How to mark not Candidate/Decision/Point? | Return `notCandidateSignal=true`, `notDecisionGeneration=true`, and `notPointSignal=true`; aggregate counts must not become opportunity ranking or point proposal input. |
| How to mark not final direction / entry / stop / TP / RR? | Return `notFinalDirection=true` and `notEntryStopTpRr=true`; plan/action fields from ReviewAggregate are not part of this status output. |
| How to keep analytics/status, not generation/execution? | The status may say archive rows/counts are readable, missing, stale, partial, or blocked. It may not create rows, save reviews, replay/recheck, send Push, rank candidates, generate points, or produce trading instructions. |
| Readiness gate focus? | Verify endpoint/panel necessity, exact allowed files, read/write split, aggregate-only field list, fail-closed mapping, targeted tests, and forbidden-call proof. |
| Future implementation max scope? | Existing `MissedOpportunityController` read path or existing dashboard owner path, `dashboard.html` minimal status/copy/DOM, targeted tests, implementation report, and source-of-truth docs. |
| Need DTO/Validator/Assembler/Orchestrator? | **No.** Use existing objects or a minimal `Map` projection only. |
| Need schema/config/pom? | **No.** Existing `tm_missed_opportunity` and ReviewAggregate read metadata are enough. |
| Need new service/domain/mapper/repository family? | **No.** That would duplicate existing owners and violate the freeze rule. |

## 3. Owner Path

Primary owner path:

```text
MissedOpportunityController
  -> GET /api/missed-opportunity/review-archive-status
  -> MissedOpportunityService.countByBizDate / query / findByMissedId
  -> MissedReasonViewParser.parse
  -> existing status Map projection
  -> dashboard missedArchiveStatusPanel
```

Aggregate context owner path:

```text
ReviewAggregateService
  -> getAggregateSummaryByAnalysisId(analysisId)
  -> ReviewAggregateSummaryVO.detailSections(section=missed,total=...)
  -> getAggregateDetailByAnalysisId(analysisId, "missed", limit)
  -> ReviewAggregateDetailVO.missed
  -> review-page.js #sec-missed
```

The aggregate context path is read-only context. It must not expose plan fields, Push/Recheck action fields, replay/recheck controls, review save behavior, or any trading-adjacent output as positive status.

Rejected owner paths:

```text
MissedOpportunityService.recordFromAuthoritativeAnalysisIfEligible
MissedOpportunityService.save
MissedOpportunityMapper.insert
ReviewController POST /api/review/save
ReviewService.saveOrUpdate
Replay execution paths
Recheck mutation paths
Push send / external channel paths
Candidate / Decision generation / Point generation paths
new analytics DTO / service / mapper / repository family
```

## 4. Status Mapping

| Status | Trigger | Display intent | Fail-closed? |
|---|---|---|---:|
| `REVIEW_ARCHIVE_AGGREGATE_REVIEW_ONLY_READY` | Existing status/read owners and missed aggregate metadata are readable for the selected scope. | Archive aggregate is readable for manual review only. | No for display; yes for downstream action |
| `REVIEW_ARCHIVE_AGGREGATE_BACKEND_PENDING_FAIL_CLOSED` | Existing read owner throws, is unavailable, or cannot safely answer. | Backend/read owner pending; keep status fail-closed. | Yes |
| `REVIEW_ARCHIVE_AGGREGATE_MISSING_FAIL_CLOSED` | No archive/aggregate row or count is proven for the selected scope. | Aggregate is missing; do not infer outcome. | Yes |
| `REVIEW_ARCHIVE_AGGREGATE_PARTIAL_REVIEW_ONLY` | Count exists but detail, parse status, source health, or aggregate linkage is partial. | Partial aggregate visibility only. | Yes for downstream action |
| `MISSED_OPPORTUNITY_COUNT_REVIEW_ONLY` | MissedOpportunity count is readable. | Count is visible for review; not a candidate pool. | No for display; yes for downstream action |
| `REVIEW_ARCHIVE_COUNT_REVIEW_ONLY` | ReviewAggregate missed section total is readable. | Archive section count is visible for review. | No for display; yes for downstream action |
| `MISSED_OPPORTUNITY_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Answering would require missed-opportunity generation. | Generation boundary blocked. | Yes |
| `MISSED_OPPORTUNITY_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Answering would require saving/inserting missed rows. | Write boundary blocked. | Yes |
| `REVIEW_RESULT_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Answering would require review save/result generation. | Review result generation boundary blocked. | Yes |
| `REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED` | Answering would require replay execution. | Replay boundary blocked. | Yes |
| `RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED` | Answering would require recheck execution/mutation. | Recheck boundary blocked. | Yes |
| `PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED` | Answering would require Push send or external channel. | Push/external boundary blocked. | Yes |
| `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Answering would generate or rank candidates. | Candidate boundary blocked. | Yes |
| `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED` | Answering would generate point/final direction/entry-stop-TP-RR. | Point boundary blocked. | Yes |
| `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED` | Answering would imply order/execution/auto-trading. | Trading boundary blocked. | Yes |

Precedence:

1. Any generation/write/execution boundary blocked status.
2. Backend pending / unavailable owner.
3. Missing aggregate/count.
4. Partial aggregate.
5. Count-only review-only.
6. Aggregate review-only ready.

`READY` means visible for manual review only. It does not authorize missed-opportunity generation, missed-opportunity write, review result generation, replay/recheck execution, Push, Candidate generation, Decision generation, Point generation, final direction, entry/stop/TP/RR, order/execution, or auto-trading.

## 5. Safety Fields

Future implementation must return or display these fields/copy:

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
- `failClosed` according to status

Allowed aggregate/status fields:

- `status`
- `analysisId`
- `symbol`
- `bizDate`
- `missedCount`
- `todayMissedCount`
- `reviewArchiveCount`
- `latestMissedId`
- `latestCreateTime`
- `latestRuleVersion`
- `reasonParseStatus`
- `reasonViewAvailable`
- `sourceHealth`
- `traceIdPresent`
- `archiveLinked`
- `reviewAggregateMissedAvailable`
- `aggregateSectionAvailable`
- `aggregateSectionTotal`
- `staleState`
- `sourceRef`
- `reason`
- `message`

Forbidden status fields:

- candidate ranking or candidate score
- generated decision or final direction
- entry, stop, take profit, TP, RR
- position size or leverage
- order action or execution action
- Push send state or external channel state
- replay execution action
- recheck execution action
- missed-opportunity generation action
- missed-opportunity write action
- review-result generation action
- rule auto-correction action
- paper order, simulated execution, or paper PnL

## 6. Dashboard / API Surface

Preferred dashboard design:

- Reuse or minimally extend `missedArchiveStatusPanel`.
- Keep `review-page.js#sec-missed` as the detailed archive table.
- Keep dashboard as status/aggregate visibility, not a second raw archive table.
- If aggregate metadata needs a distinct display, add only a tiny aggregate row/section under the existing panel.

Preferred API design:

```text
GET /api/missed-opportunity/review-archive-status?analysisId=...&symbol=...&bizDate=...
```

Use the existing endpoint first. A separate endpoint is not needed by default. If readiness gate permits a separate endpoint, it must be a thin read-only `Map` projection and must reuse existing MissedOpportunity / ReviewAggregate read owners.

## 7. Fail-Closed Rules

Fail closed when:

- MissedOpportunity read/count owner is unavailable.
- ReviewAggregate missed metadata is required but absent.
- No missed/archive rows or counts are proven for the selected scope.
- `analysisId`, `symbol`, or `bizDate` is ambiguous and cannot be normalized safely.
- `reasonJson` is blank or parse status is `PARSE_FAILED` and the status depends on reason health.
- `traceId`, rule version, or aggregate section metadata is required but missing.
- The only way to answer is to call missed-opportunity generation/write paths.
- The only way to answer is to call review save/result generation.
- The only way to answer is to run replay or recheck.
- The only way to answer is to send Push or use an external channel.
- The only way to answer is to generate Candidate, Decision, Point, final direction, entry/stop/TP/RR, order/execution, or auto-trading semantics.
- The implementation would require new DTO / Validator / Assembler / Orchestrator, schema/config/pom, or a new service/domain/mapper/repository ownership family.

Fail-closed still permits a visible status panel, but only as a blocked/manual-review indicator.

## 8. Generation / Execution Boundary

This slice is archive analytics / aggregate status only.

The future implementation must not call:

- `recordFromAuthoritativeAnalysisIfEligible`
- `MissedOpportunityService#save`
- `MissedOpportunityMapper#insert`
- `POST /api/review/save`
- `ReviewService#saveOrUpdate`
- replay execution paths
- recheck mutation paths
- Push send paths
- external channel paths
- Candidate generation paths
- Decision generation paths
- Point generation paths
- order/execution/trading paths

If any of these are needed, readiness gate must return NO-GO.

## 9. Implementation Readiness Gate Questions

The next readiness gate must verify:

1. Whether extending `GET /api/missed-opportunity/review-archive-status` is sufficient.
2. Whether any separate aggregate endpoint is truly needed.
3. Whether `missedArchiveStatusPanel` can carry aggregate fields without confusing the completed base status.
4. Exact allowed files for implementation.
5. Whether aggregate fields can be derived from read-only `MissedOpportunityService` and `ReviewAggregateService` methods.
6. Whether all write/generation methods are excluded by construction and by tests.
7. Whether stale/missing/partial states can be represented without fabricating archive facts.
8. Whether targeted tests can cover safety fields, fail-closed states, and forbidden field absence.
9. Whether no DTO / Validator / Assembler / Orchestrator, schema/config/pom, or new ownership family is needed.
10. NO-GO conditions for generation, write, replay/recheck, Push, Candidate, Decision, Point, trading, or new owner paths.

## 10. Future Implementation Scope If Readiness Is GO

Maximum allowed future implementation scope:

- `src/main/java/org/example/trademodel/controller/MissedOpportunityController.java`, only to extend the existing read-only status endpoint or add one thin read-only `Map` endpoint if readiness approves.
- `src/main/resources/templates/dashboard.html`, only to add minimal aggregate status/copy/DOM under or near `missedArchiveStatusPanel`.
- targeted controller/dashboard tests for endpoint safety fields, fail-closed states, generation/write/replay/recheck boundaries, and forbidden field absence.
- implementation report docs.
- source-of-truth docs.

Still forbidden:

- Java business code outside the existing owner path;
- unrelated tests;
- dashboard business logic beyond minimal status/copy/DOM;
- schema/config/pom;
- DTO / Validator / Assembler / Orchestrator;
- new service/domain/mapper/repository ownership family;
- missed-opportunity generation/write behavior;
- review result generation;
- replay/recheck execution;
- Push send / external channel;
- Candidate / Decision generation / Point;
- final direction / entry / stop / TP / RR;
- order / execution / auto-trading;
- Position Monitor execution;
- paper order / simulated execution / paper PnL;
- P359 / P360.

## 11. Capability Movement

- Current level: `REVIEW_ONLY_RUNTIME partial`.
- This package raises capability level: No, design only.
- Completed review-only runtime partial slices remain 14.
- Future implementation, if approved, remains a review-only visibility slice and does not become Production Wiring.

## 12. Freeze Rule Compliance

- 是否创建新骨架: No.
- 是否复用 Cursor-era / V1 资产: Yes, MissedOpportunity read/status owner path, ReviewAggregate missed metadata, review page, dashboard panel, and existing tests.
- 是否减少重复: Yes, by choosing existing owners and rejecting a new aggregate analytics owner family.
- 是否提升 capability level: No, design only.
- 是否接 service/runtime/dashboard/API: No in this package; future readiness may authorize minimal review-only wiring over existing service/API/dashboard owners.
- 是否符合 #830 审计建议: Yes.

## 13. Next Allowed Action

- Next allowed action: `Implementation readiness gate for Review Archive Analytics / Missed Opportunity Aggregate Status`
- Next branch: `review-archive-analytics-missed-opportunity-aggregate-status-implementation-readiness-gate`
- Next risk: `A`
- Expected implementation risk after a GO readiness gate: `B`
