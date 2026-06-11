# V1 Minimal Review-Only Missed Opportunity / Review Archive Status Runtime Wiring Design

## 1. Executive Summary

This package is design only. It does not implement Java business code, tests, dashboard business logic, schema/config/pom changes, endpoint wiring, external API refresh, scheduler/collector/API client triggers, Push, Candidate generation, Decision generation, Point generation, order/execution, auto-trading, DTO, Validator, Assembler, Orchestrator, replay/recheck execution, missed-opportunity generation/write behavior, review result generation, P359, or P360.

Minimal future target: expose a review-only Missed Opportunity / Review Archive status that tells the user whether missed-opportunity archive rows are readable, whether the reason view parses, whether the row links back into ReviewAggregate/review archive context, and why the status is empty, partial, blocked, or fail-closed.

Design conclusion: the minimal owner path should stay on the existing MissedOpportunity / ReviewAggregate / dashboard-count assets. The existing write path is real but must remain outside this status surface.

Owner path:

```text
tm_missed_opportunity / MissedOpportunityDO
  -> MissedOpportunityMapper read/query/count methods
  -> MissedOpportunityServiceImpl read/query/count methods only
  -> MissedOpportunityController GET /api/missed-opportunity/query
  -> MissedReasonViewParser reasonView / parseStatus
  -> ReviewAggregateServiceImpl missed detail / review archive linkage
  -> LightSystemStatusVO.missedValidOpportunityCount / dashboard missedOpportunityCount()
  -> future minimal review-only Missed Opportunity / Review Archive status API/dashboard surface
```

Rejected owner paths for the minimal slice:

```text
recordFromAuthoritativeAnalysisIfEligible / save / mapper insert
AnalysisAssemblerServiceImpl missed-opportunity recording path
POST /api/review/save or review result generation
replay / recheck mutation paths
new MissedOpportunity DTO / Validator / Assembler / Orchestrator family
Push / Candidate / Decision generation / Point / trading paths
```

Next step: `Minimal Review-Only Missed Opportunity / Review Archive Status Runtime Wiring Implementation Readiness Gate`.

## 2. Owner Path To Preserve

Fixed future owner boundary:

```text
tm_missed_opportunity
  -> MissedOpportunityDO(missedId, decisionId, analysisId, symbol, bizDate, reasonJson, ruleVersion, traceId, createTime)
  -> MissedOpportunityMapper.selectByMissedId / listByAnalysisId / listByBizDate / listByQuery / countByBizDate
  -> MissedOpportunityServiceImpl.findByMissedId / listByAnalysisId / listByBizDate / query / countByBizDate
  -> MissedOpportunityController GET /api/missed-opportunity/query
  -> MissedReasonViewParser.parse(reasonJson)
  -> ReviewAggregateServiceImpl.toMissedList / ReviewAggregateVO missed section
  -> review-page.js #sec-missed existing archive display
  -> dashboard existing missedOpportunityCount() count signal
```

Rules:

- Future implementation must reuse `MissedOpportunityDO`, `MissedOpportunityMapper`, `MissedOpportunityServiceImpl`, `MissedOpportunityController`, `MissedReasonViewParser`, `ReviewAggregateServiceImpl`, and the existing dashboard count signal.
- Future status must use read/query/count methods only.
- `recordFromAuthoritativeAnalysisIfEligible`, `save`, and mapper `insert` are generation/write paths and must not be called by status code.
- ReviewAggregate is archive linkage context only. It must not become review-result generation, replay execution, recheck execution, or rule auto-correction.
- Existing review page `sec-missed` may remain the detailed archive table. The minimal dashboard surface should be status-level only, not a second full raw table.
- Display Slots remain display-only. Missed Opportunity / Review Archive status is not a Candidate pool, Decision generator, Point source, Push trigger, or trading authorization.

## 3. Minimal Status Mapping

Allowed future statuses:

| Status | Trigger condition | Dashboard/API copy intent | Candidate/Decision/Point/Push allowed? | Review-only? | Fail-closed? |
|---|---|---|---|---|---|
| `MISSED_ARCHIVE_REVIEW_ONLY_READY` | Read path is available, count/query is readable, at least one scoped missed row exists, reason parse status is `OK`, and review/archive linkage is present or not required for the selected view. | Missed archive is readable for manual review only. | No | Yes | No for display; still no downstream action |
| `MISSED_ARCHIVE_COUNT_ONLY_PARTIAL` | Dashboard daily count is readable, but latest row/detail, reason parse status, or aggregate linkage is not available through the selected status input. | Count signal exists, but archive detail is partial. | No | Yes | Yes for downstream action |
| `MISSED_ARCHIVE_EMPTY_FAIL_CLOSED` | Query/count returns no rows for the selected `analysisId`, `symbol`, `bizDate`, or default daily scope. | No missed archive row is proven; keep status empty and fail-closed. | No | Yes | Yes |
| `MISSED_REASON_EMPTY_OR_PARSE_PARTIAL` | Row exists but `reasonJson` is blank or parser returns `EMPTY_REASON_JSON`. | Reason explanation is missing; display only with partial source-health. | No | Yes | Yes for downstream action |
| `MISSED_REASON_PARSE_FAILED_FAIL_CLOSED` | Row exists but parser returns `PARSE_FAILED` or reason view is contradictory/unsupported. | Reason explanation could not be parsed; keep status fail-closed. | No | Yes | Yes |
| `MISSED_ARCHIVE_LINKAGE_PARTIAL` | Missed row exists but `analysisId`, ReviewAggregate missed section, rule version, trace id, or review page linkage is missing/partial. | Archive row is readable, but review linkage/source trace is partial. | No | Yes | Yes for downstream action |
| `MISSED_ARCHIVE_QUERY_UNAVAILABLE_FAIL_CLOSED` | MissedOpportunity read/query/count owner path is unavailable or cannot safely answer the selected status. | Missed archive status cannot be proven from read owners. | No | Yes | Yes |
| `MISSED_ARCHIVE_BLOCKED_FAIL_CLOSED` | Answering would require missed-opportunity generation/write behavior, review save/generation, replay/recheck execution, external refresh, scheduler/collector/API-client trigger, Push, Candidate generation, Decision generation, Point generation, order/execution, auto-trading, or new skeleton ownership. | Status is blocked; no runtime action is allowed. | No | Yes | Yes |

Status precedence:

1. `MISSED_ARCHIVE_BLOCKED_FAIL_CLOSED`
2. `MISSED_ARCHIVE_QUERY_UNAVAILABLE_FAIL_CLOSED`
3. `MISSED_REASON_PARSE_FAILED_FAIL_CLOSED`
4. `MISSED_ARCHIVE_EMPTY_FAIL_CLOSED`
5. `MISSED_REASON_EMPTY_OR_PARSE_PARTIAL`
6. `MISSED_ARCHIVE_LINKAGE_PARTIAL`
7. `MISSED_ARCHIVE_COUNT_ONLY_PARTIAL`
8. `MISSED_ARCHIVE_REVIEW_ONLY_READY`

`READY` means readable for display only. It never permits missed-opportunity generation, review result generation, replay/recheck execution, Candidate generation, Decision generation, Point generation, Push send, external channel, order/execution, or auto-trading.

## 4. Minimal Future Fields

Allowed future fields:

- `status`
- `analysisId`
- `symbol`
- `bizDate`
- `missedCount`
- `todayMissedCount`
- `latestMissedId`
- `latestCreateTime`
- `latestRuleVersion`
- `traceIdPresent`
- `reasonViewAvailable`
- `reasonParseStatus`
- `archiveLinked`
- `reviewAggregateMissedAvailable`
- `queryAvailable`
- `countAvailable`
- `sourceHealth`
- `sourceRef`
- `reason`
- `message`
- `failClosed`
- `reviewOnly = true`
- `notTradingSignal = true`
- `notCandidateSignal = true`
- `notDecisionGeneration = true`
- `notPointSignal = true`
- `notReplayExecution = true`
- `notRecheckExecution = true`
- `notMissedOpportunityGeneration = true`
- `notReviewResultGeneration = true`
- `notExecutable = true`
- `displaySlotsAreCandidatePool = false`

Forbidden future fields:

- candidate ranking
- generated decision
- final direction
- entry
- stop
- TP
- RR
- position size
- leverage
- order action
- Push send state
- external channel state
- replay execution action
- recheck execution action
- missed-opportunity generation action
- review result generation action
- rule auto-correction action

The implementation readiness gate should prefer a minimal `Map` or existing object projection. A new DTO / Validator / Assembler / Orchestrator family is not allowed for this minimal slice.

## 5. Dashboard/API Surface

Readiness gate should evaluate two safe implementation shapes:

1. Reuse existing `GET /api/missed-opportunity/query` for row details plus existing dashboard `missedOpportunityCount()` for the first status display.
2. Allow one minimal read-only status endpoint only if the existing query/count surfaces cannot safely carry status, source-health, and fail-closed reasons, for example:

```text
GET /api/missed-opportunity/review-archive-status?analysisId=...&symbol=...&bizDate=...
```

Preferred direction: keep detailed archive rows on the existing query/review-page path and allow at most one minimal read-only status endpoint after readiness approval. The endpoint should live on the existing MissedOpportunity owner path unless the readiness gate proves a `DashboardController` status endpoint is safer for dashboard aggregation.

The dashboard/API surface may show:

- overall missed archive status;
- today/scoped missed count;
- latest/archive availability;
- reason parse status;
- review aggregate linkage status;
- source-health / source-ref summary;
- fail-closed reason;
- review-only label;
- not trading / not candidate / not decision generation / not point labels;
- not replay / not recheck / not generation labels;
- Display Slots boundary label.

The surface must not add missed-opportunity record buttons, review save, replay/recheck buttons, Push actions, Candidate ranking, Decision generation, Point generation, final direction, entry/stop/TP/RR, order, execution, or auto-trading.

## 6. Generation / Review / Replay Boundary

Missed Opportunity archive status is a read-side visibility layer.

Future implementation must not call:

- `MissedOpportunityService#recordFromAuthoritativeAnalysisIfEligible`
- `MissedOpportunityService#save`
- `MissedOpportunityMapper#insert`
- `POST /api/review/save`
- replay execution endpoints
- recheck mutation paths
- Push send paths
- Candidate generation paths
- Decision generation paths
- Point generation paths
- order/execution/trading paths

If status cannot be determined without any of those calls, the required status is `MISSED_ARCHIVE_BLOCKED_FAIL_CLOSED`.

## 7. Fail-Closed Rules

Future status must fail closed when:

- no missed row is proven for the selected scope;
- the MissedOpportunity query/count path is unavailable;
- `analysisId`, `symbol`, or `bizDate` input is ambiguous or unsupported;
- `reasonJson` is blank, malformed, unsupported, or contradictory;
- parser status is `PARSE_FAILED`;
- ReviewAggregate linkage is required but unavailable;
- rule version, trace id, or source ref is required by readiness criteria but missing;
- answering would require missed-opportunity generation/write behavior;
- answering would require review result generation, review save, replay execution, or recheck mutation;
- answering would require external API refresh, scheduler/collector/API-client trigger, Push, Candidate generation, Decision generation, Point generation, order/execution, or auto-trading;
- answering would require a new DTO / Validator / Assembler / Orchestrator owner;
- any display could be mistaken as trading advice or executable rule/action intent.

Fail-closed means display can remain visible, but no downstream Candidate / Decision / Point / Push / Trading implication is allowed.

## 8. Minimal Future Implementation Boundary

If readiness gate returns GO, future implementation must stay within:

- existing `MissedOpportunityDO`;
- existing `MissedOpportunityMapper` read/query/count methods;
- existing `MissedOpportunityServiceImpl` read/query/count methods;
- existing `MissedOpportunityController` read path;
- existing `MissedReasonViewParser`;
- existing `ReviewAggregateServiceImpl` missed detail linkage;
- existing `LightSystemStatusVO.missedValidOpportunityCount` and dashboard count signal;
- optional minimal read-only status endpoint after readiness approval;
- optional minimal dashboard status/copy/DOM after readiness approval;
- targeted controller/dashboard/service tests only if implementation touches endpoint/dashboard behavior;
- source-of-truth docs.

Future implementation must not:

- add DTO / Validator / Assembler / Orchestrator;
- add schema/config/pom changes;
- add a new missed-opportunity mapper/service/persistence owner;
- call missed-opportunity generation/write behavior;
- call review save, review result generation, replay execution, or recheck mutation;
- trigger external API refresh, scheduler, collector, or API client reads;
- connect Push or external channels;
- generate Candidate;
- generate a new Decision;
- generate Point;
- output final direction / entry / stop / TP / RR;
- connect order / execution / auto-trading;
- continue P359 or start P360.

## 9. Readiness Checklist

The next readiness gate must answer:

- Is existing `GET /api/missed-opportunity/query` plus `missedOpportunityCount()` sufficient for the first status, or is one minimal read-only status endpoint needed?
- If a status endpoint is needed, should it live in `MissedOpportunityController` or `DashboardController`?
- Can implementation use a minimal `Map` / existing object projection without a new DTO?
- Which scoped inputs are allowed first: default today, `analysisId`, `symbol`, `bizDate`, or `missedId`?
- Can latest row, count, parse status, and aggregate linkage be derived without generation/write calls?
- Can missing rows and parser failures fail closed with clear dashboard copy?
- Can review page `sec-missed` remain the detailed table while dashboard stays status-level?
- Does dashboard already have a safe insertion neighborhood near the existing missed count / review status surfaces?
- What targeted tests are sufficient if endpoint/dashboard implementation proceeds?
- Can forbidden semantics remain absent from changed business files and visible UI copy?

## 10. Capability-Level Movement

- Current level: `REVIEW_ONLY_RUNTIME partial`.
- This package raises capability level: No, design only.
- Future minimal implementation target: keep `REVIEW_ONLY_RUNTIME partial` by adding review-only visibility over existing MissedOpportunity / ReviewAggregate / dashboard-count assets.
- It is not Production Wiring.
- It is not missed-opportunity generation.
- It is not review result generation.
- It is not replay/recheck execution.
- It is not Push.
- It is not Candidate generation.
- It is not Decision generation.
- It is not Point generation.
- It is not Trading.

## 11. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, design only
- 是否接 service/runtime/dashboard/API: No, design only; future readiness may authorize minimal review-only MissedOpportunity/dashboard status wiring
- 是否符合 #830 审计建议: Yes

## 12. Final Recommendation

GO to `Minimal Review-Only Missed Opportunity / Review Archive Status Runtime Wiring Implementation Readiness Gate`.

The readiness gate should decide whether existing query/count surfaces are enough or one minimal read-only status endpoint is justified, while preserving the read-only owner path and blocking missed-opportunity generation/write behavior, review result generation, replay/recheck execution, Push, Candidate generation, Decision generation, Point generation, final direction, entry/stop/TP/RR, order/execution, auto-trading, new DTO / Validator / Assembler / Orchestrator, P359, and P360.
