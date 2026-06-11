# V1 Missed Opportunity / Review Archive Status Runtime Wiring Implementation Readiness Gate

## 1. Executive Summary

Decision: **GO** to `Minimal Review-Only Missed Opportunity / Review Archive Status Runtime Wiring Implementation`.

This package is readiness-gate only. It does not implement Java business code, tests, dashboard business logic, schema/config/pom changes, endpoint wiring, external API refresh, scheduler/collector/API client triggers, Push, Candidate generation, Decision generation, Point generation, final direction, entry/stop/TP/RR, order/execution, auto-trading, DTO, Validator, Assembler, Orchestrator, replay execution, recheck execution, missed-opportunity generation/write behavior, review result generation, P359, or P360.

GO is narrow:

- reuse the existing MissedOpportunity / ReviewAggregate / dashboard missed-count owner path;
- use read/query/count methods only;
- allow at most one minimal read-only status endpoint if implementation proceeds;
- allow at most one minimal dashboard status/copy/DOM surface if implementation proceeds;
- require targeted controller/dashboard tests in the implementation package;
- keep missed-opportunity generation/write behavior, review result generation, replay/recheck execution, Push, Candidate generation, Decision generation, Point, and Trading blocked;
- require no new DTO / Validator / Assembler / Orchestrator.

Current capability level does not move. The project remains `REVIEW_ONLY_RUNTIME partial`, and completed review-only runtime partial slices remain 9.

## 2. Current Baseline

- Current merged main baseline: `74556d4 docs(missed): fix design handoff to readiness gate`
- Current module: `Missed Opportunity / Review Archive status`
- Current phase: `Implementation readiness gate`
- Risk level: `A` for this docs-only readiness gate
- Next implementation risk: `B`, because implementation may touch existing controller/dashboard/test files

## 3. Source-Read And Design Summary

`docs/V1_MISSED_OPPORTUNITY_REVIEW_ARCHIVE_STATUS_SOURCE_READ.md` confirmed reusable owner assets:

```text
tm_missed_opportunity / MissedOpportunityDO
  -> MissedOpportunityMapper read/query/count methods
  -> MissedOpportunityServiceImpl read/query/count methods
  -> MissedOpportunityController GET /api/missed-opportunity/query
  -> MissedReasonViewParser reasonView / parseStatus
  -> ReviewAggregateServiceImpl missed detail / review archive linkage
  -> review-page.js #sec-missed archive display
  -> LightSystemStatusVO.missedValidOpportunityCount / dashboard missedOpportunityCount()
```

`docs/V1_MINIMAL_REVIEW_ONLY_MISSED_OPPORTUNITY_REVIEW_ARCHIVE_STATUS_RUNTIME_WIRING_DESIGN.md` fixed the future minimal owner path and rejected these paths for the minimal slice:

```text
recordFromAuthoritativeAnalysisIfEligible / save / mapper insert
AnalysisAssemblerServiceImpl missed-opportunity recording path
POST /api/review/save or review result generation
replay / recheck mutation paths
new MissedOpportunity DTO / Validator / Assembler / Orchestrator family
Push / Candidate / Decision generation / Point / trading paths
```

The design allows a minimal read-only status surface only if it stays on existing read owners and fail-closes when status cannot be proven without generation/write behavior.

## 4. Readiness Questions

| Gate question | Decision | Reason |
|---|---|---|
| Is an existing owner path present? | **Yes** | `MissedOpportunityController`, `MissedOpportunityServiceImpl`, `MissedOpportunityMapper`, `tm_missed_opportunity`, `MissedReasonViewParser`, `ReviewAggregateServiceImpl`, review page, and dashboard count assets exist. |
| Can implementation use read-only methods only? | **Yes** | Query/count/read methods exist. Write methods are clearly separable and can be excluded. |
| Is one minimal status endpoint allowed? | **Yes, if needed** | Existing query endpoint returns rows, but a dashboard status needs status/fail-closed/source-health flags. One minimal read-only endpoint is acceptable after this gate. |
| Should the endpoint live in existing owner paths? | **Yes** | Prefer `MissedOpportunityController` if the endpoint is archive-specific; `DashboardController` is acceptable only if implementation proves dashboard aggregation is safer. |
| Can implementation avoid new DTO / Validator / Assembler? | **Yes** | Use a minimal `Map` response or existing object projection. |
| Can dashboard show minimal status safely? | **Yes** | The existing dashboard already has `missedOpportunityCount()` and review-only panel patterns. |
| Can missing rows fail closed? | **Yes** | Empty result/count states can map to `MISSED_ARCHIVE_EMPTY_FAIL_CLOSED`. |
| Can parser failures fail closed? | **Yes** | `MissedReasonViewParser` already exposes `EMPTY_REASON_JSON` and `PARSE_FAILED`. |
| Can ReviewAggregate linkage remain read-only context? | **Yes** | Use `ReviewAggregateServiceImpl` missed section as archive linkage evidence only; do not generate review results. |
| Does implementation need schema/config/pom changes? | **No** | Existing table/mapper/read paths are sufficient for a minimal status. |

## 5. Allowed Future Implementation Scope

If this readiness gate is merged, the next implementation package may change only:

- existing MissedOpportunity or dashboard status controller path, to add one minimal read-only status endpoint such as `/api/missed-opportunity/review-archive-status`;
- `src/main/resources/templates/dashboard.html`, to add one small Missed Opportunity / Review Archive status panel or status copy/DOM near existing review/missed-count surfaces;
- targeted controller/dashboard/service tests for read-only status, fail-closed states, parser status, and forbidden field absence;
- implementation report documentation;
- source-of-truth documents.

Allowed future status fields:

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
- `reviewOnly=true`
- `notTradingSignal=true`
- `notCandidateSignal=true`
- `notDecisionGeneration=true`
- `notPointSignal=true`
- `notReplayExecution=true`
- `notRecheckExecution=true`
- `notMissedOpportunityGeneration=true`
- `notReviewResultGeneration=true`
- `notExecutable=true`
- `displaySlotsAreCandidatePool=false`

## 6. Forbidden Future Implementation Scope

The next implementation must not:

- add DTO / Validator / Assembler / Orchestrator;
- add schema/config/pom changes;
- add a new missed-opportunity mapper/service/persistence owner;
- call `recordFromAuthoritativeAnalysisIfEligible`, `save`, or mapper `insert`;
- call `POST /api/review/save`;
- trigger replay execution or recheck mutation;
- trigger external API refresh, scheduler, collector, or API client calls;
- connect Push or external channels;
- generate Candidate;
- generate Decision;
- generate Point;
- output final direction;
- output entry / stop / TP / RR;
- output position size, leverage, order action, or executable action;
- connect order / execution / auto-trading;
- continue P359 or start P360;
- treat Display Slots as a candidate pool.

## 7. Status Mapping Readiness

| Status | Implementable with existing assets? | Data source | Gap | Allowed to implement? | Fail-closed? |
|---|---:|---|---|---:|---:|
| `MISSED_ARCHIVE_REVIEW_ONLY_READY` | Yes | Query/count rows, parser `OK`, optional aggregate linkage | Requires minimal status projection | Yes | No for display; no downstream action |
| `MISSED_ARCHIVE_COUNT_ONLY_PARTIAL` | Yes | `missedValidOpportunityCount` / `countByBizDate` | Detail/linkage may be absent | Yes | Yes for downstream action |
| `MISSED_ARCHIVE_EMPTY_FAIL_CLOSED` | Yes | Empty query/count | None | Yes | Yes |
| `MISSED_REASON_EMPTY_OR_PARSE_PARTIAL` | Yes | `MissedReasonViewParser.EMPTY_REASON_JSON` | None | Yes | Yes for downstream action |
| `MISSED_REASON_PARSE_FAILED_FAIL_CLOSED` | Yes | `MissedReasonViewParser.PARSE_FAILED` | None | Yes | Yes |
| `MISSED_ARCHIVE_LINKAGE_PARTIAL` | Yes | `analysisId`, `traceId`, ReviewAggregate missed section | Linkage may be missing | Yes | Yes for downstream action |
| `MISSED_ARCHIVE_QUERY_UNAVAILABLE_FAIL_CLOSED` | Yes | Exceptions/unavailable read owner | Needs controller/test mapping | Yes | Yes |
| `MISSED_ARCHIVE_BLOCKED_FAIL_CLOSED` | Yes | Forbidden-call boundary | Needs explicit guard/copy | Yes | Yes |

## 8. Required Future Checks

The implementation package must run:

- `bash scripts/check-workflow-contract.sh`
- `bash scripts/v1-state.sh`
- `bash scripts/codex-next-task.sh`
- `bash scripts/v1-auto.sh next`
- compile / test-compile if Java is touched
- targeted controller test for the new read-only status endpoint
- targeted dashboard/controller/template test if dashboard is touched
- grep/readiness check for MissedOpportunity read/write split
- forbidden semantics grep
- forbidden path check
- `git diff --check`

## 9. Readiness Result

Result: **GO**.

GO rationale:

- Existing MissedOpportunity read/API/service/mapper/schema assets are real and reusable.
- Existing `MissedReasonViewParser` gives parser status needed for source-health/fail-closed mapping.
- Existing ReviewAggregate missed section and review page archive display provide read-only archive linkage context.
- Existing dashboard `missedOpportunityCount()` gives a safe insertion neighborhood.
- A minimal endpoint/panel can be implemented as read-only status without new DTO / Validator / Assembler, schema/config/pom, Push, Candidate generation, Decision generation, Point, or trading.

NO-GO conditions for the next package:

- it requires schema/config/pom changes;
- it requires a new DTO / Validator / Assembler / Orchestrator;
- it requires a new MissedOpportunity mapper/service/persistence owner;
- it calls missed-opportunity generation/write behavior;
- it calls review save, review result generation, replay execution, or recheck mutation;
- it triggers external refresh, scheduler, collector, or API client calls;
- it generates Candidate, Decision, Point, final direction, entry / stop / TP / RR, Push, order/execution, or auto-trading.

## 10. Capability Movement

- Current level: `REVIEW_ONLY_RUNTIME partial`.
- This package raises capability level: No, readiness gate only.
- Completed review-only runtime partial slices remain 9.
- Missed Opportunity / Review Archive status is not implemented by this package.
- Future implementation target: remain `REVIEW_ONLY_RUNTIME partial`, not Production Wiring.

## 11. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 资产: Yes
- 是否减少重复: Yes, by forcing reuse of existing MissedOpportunity / ReviewAggregate / dashboard-count owner assets
- 是否提升 capability level: No, readiness gate only
- 是否接 service/runtime/dashboard/API: No in this package; future implementation may minimally connect existing service/API/dashboard review-only status
- 是否符合 #830 审计建议: Yes

## 12. Next Allowed Action

Next allowed action: **Minimal Review-Only Missed Opportunity / Review Archive Status Runtime Wiring Implementation**.

Next implementation risk: **B**.

The next package may add only one minimal read-only status endpoint, minimal dashboard status/copy/DOM, targeted tests, implementation docs, and source-of-truth updates over existing MissedOpportunity / ReviewAggregate / dashboard-count assets. It must not add DTO / Validator / Assembler, schema/config/pom, Push, Candidate generation, Decision generation, Point generation, final direction, entry/stop/TP/RR, order/execution, auto-trading, replay/recheck execution, review result generation, missed-opportunity generation/write behavior, P359, or P360.
