# V1 Missed Opportunity / Review Archive Status Source Read

## 1. Executive Summary

This package is source-read only. It does not implement runtime wiring.

Conclusion: **GO to design only** for `Minimal Review-Only Missed Opportunity / Review Archive Status Runtime Wiring Design`.

Existing owner assets are real and reusable:

- `MissedOpportunityController` exposes read-only `GET /api/missed-opportunity/query`.
- `MissedOpportunityServiceImpl` has read/query/count methods, plus guarded write/record methods that must be excluded from any future status path.
- `MissedOpportunityMapper`, `MissedOpportunityDO`, and `tm_missed_opportunity` provide the persistence owner.
- `MissedReasonViewParser` and `MissedReasonViewVO` provide a parse-status layer for `reason_json`.
- `ReviewAggregateServiceImpl` reads missed rows by `analysisId`, adds `missed` detail metadata, and renders Missed context in review closure/deviation fields.
- `review-page.js` already renders a read-only `sec-missed` archive section.
- `LightSystemStatusVO.missedValidOpportunityCount`, `DecisionServiceImpl#getLightSystemStatus`, and dashboard `missedOpportunityCount()` expose a partial daily count on the dashboard.

The source path can support a minimal review-only status design, but not direct implementation yet. The missing piece is a dedicated status mapping and dashboard/API boundary that separates read-only archive visibility from missed-opportunity generation/write behavior and review result generation.

Capability level remains `REVIEW_ONLY_RUNTIME partial`. This package does not raise capability level and is not Production Wiring.

## 2. Source Inventory

| Area | Files/classes found | Existing behavior | Runtime/API/dashboard connection | Gap |
|---|---|---|---|---|
| Missed Opportunity API | `MissedOpportunityController` | `GET /api/missed-opportunity/query` accepts `analysisId`, `symbol`, `bizDate`, `missedId`, and `limit`; returns `MissedOpportunityQueryItemVO` with parsed `reasonView`. | Existing read API. | No dedicated status/fail-closed endpoint or dashboard panel. |
| Missed Opportunity service | `MissedOpportunityService`, `MissedOpportunityServiceImpl` | Read methods: `findByMissedId`, `listByDecisionId`, `listBySymbol`, `listByAnalysisId`, `listByBizDate`, `query`, `countByBizDate`. Write paths: `recordFromAuthoritativeAnalysisIfEligible` and `save`. | Read service exists; write/generation behavior exists and must be excluded. | Future status design must use read methods only and explicitly block record/write behavior. |
| Mapper/schema owner | `MissedOpportunityMapper`, `MissedOpportunityDO`, `tm_missed_opportunity` | Table has `missed_id`, `decision_id`, `analysis_id`, `symbol`, `biz_date`, `reason_json`, `rule_version`, `trace_id`, `create_time`; mapper provides insert and read queries. | Existing persistence owner; no schema change needed for design. | Status mapping must treat missing rows as fail-closed/empty, not fabricate archive facts. |
| Reason view parser | `MissedReasonViewParser`, `MissedReasonViewVO` | Parses `reason_json` into `version`, `rule`, `whyMissed`, `facts`, `refs`, and `parseStatus` (`OK`, `EMPTY_REASON_JSON`, `PARSE_FAILED`). | Existing read-side explanation layer. | Future status can use parse status/source-health, but must not add a new parser/validator skeleton. |
| Review aggregate | `ReviewAggregateServiceImpl`, `ReviewAggregateVO`, `ReviewAggregateSummaryVO`, `ReviewAggregateDetailVO` | Reads `missedOpportunityMapper.listByAnalysisId`, sets aggregate `missed`, detail section `missed`, deviation signals/source tags, next focus, key facts, and Hot Reset relation hints. | Existing review/archive read owner path. | Existing Review / Replay status endpoint is broader; it does not expose Missed Opportunity archive status as a dedicated dashboard/API slice. |
| Review page | `review.html`, `review-page.js` | Read-only region renders `sec-missed`; table shows missed id, analysis id, decision id, symbol, biz date, rule version, trace id, `reasonView`, raw `reasonJson`, and create time. | Existing user-visible archive surface. | Main dashboard has no dedicated Missed Opportunity / Review Archive status panel. |
| Dashboard summary count | `LightSystemStatusVO`, `DecisionServiceImpl`, `dashboard.html` | `missedValidOpportunityCount` counts today by `tm_missed_opportunity.biz_date`; dashboard `missedOpportunityCount()` displays "可复盘" count or `暂无`. | Existing partial dashboard signal. | Count-only signal lacks latest row, parse/source-health, aggregate linkage, and fail-closed reason. |
| Tests | `MissedOpportunityServiceImplTest`, `MissedReasonViewParserTest`, `DecisionServiceImplTest`, ReviewAggregate tests | Existing tests cover record eligibility/write skip/query normalization, reason parser states, dashboard system status count, and aggregate construction. | Enough precedent for a future design and targeted implementation tests. | No dedicated `MissedOpportunityControllerTest` or dedicated dashboard status tests yet. |

## 3. Existing Runtime Flow

```text
Authoritative analysis path
  -> MissedOpportunityServiceImpl#recordFromAuthoritativeAnalysisIfEligible
  -> tm_missed_opportunity insert when existing guarded rule says eligible
  -> MissedOpportunityMapper
  -> MissedOpportunityController GET /api/missed-opportunity/query
  -> MissedReasonViewParser reasonView

Review archive path
  -> ReviewAggregateServiceImpl#getAggregateByAnalysisId / #getAggregateDetailByAnalysisId
  -> missedOpportunityMapper.listByAnalysisId
  -> ReviewAggregateVO.missed / ReviewAggregateDetailVO.missed
  -> review-page.js renderMissed() in #sec-missed

Dashboard partial count path
  -> DecisionServiceImpl#getLightSystemStatus
  -> missedOpportunityMapper.countByBizDate(LocalDate.now())
  -> LightSystemStatusVO.missedValidOpportunityCount
  -> dashboard.html missedOpportunityCount()
  -> KPI "今日可复盘机会"
```

Safe read path exists. Unsafe-for-this-slice write/generation path also exists and must stay out of scope.

## 4. Review-Only Readiness

- Can read missed rows by `analysisId`: Yes, through `MissedOpportunityMapper#listByAnalysisId`, `MissedOpportunityService#listByAnalysisId`, and ReviewAggregate detail.
- Can query archive rows by `missedId`, `analysisId`, `symbol`, or `bizDate`: Yes, through `GET /api/missed-opportunity/query`.
- Can show parsed reason status: Yes, `MissedReasonViewParser` exposes `parseStatus`.
- Can show today's count on dashboard: Partial, `missedValidOpportunityCount` exists.
- Can show review/archive linkage: Partial, review page `sec-missed` exists, but main dashboard lacks dedicated status.
- Can fail closed: Partial. Missing rows and parser failures are representable, but no dedicated status mapping exists.
- Can avoid missed-opportunity generation/write behavior: Yes, if the future status design uses only read methods and explicitly forbids `recordFromAuthoritativeAnalysisIfEligible`, `save`, and mapper `insert`.
- Can avoid replay/recheck execution and review result generation: Yes, if the design stays within MissedOpportunity read path and ReviewAggregate read path, and does not call `POST /api/review/save` or Push/Recheck mutation paths.
- New DTO / Validator / Assembler needed now: No.

## 5. Owner Path Decision

Canonical owner path for future design:

```text
MissedOpportunityMapper / tm_missed_opportunity
  -> MissedOpportunityService read/query/count methods only
  -> MissedOpportunityController GET /api/missed-opportunity/query
  -> MissedReasonViewParser reasonView / parseStatus
  -> ReviewAggregateServiceImpl missed detail / review archive linkage
  -> dashboard summary count as existing partial signal
  -> future minimal review-only Missed Opportunity / Review Archive status API/dashboard surface
```

Do not create a new owner wrapper. Do not move ownership to ReviewResult generation, replay/recheck execution, Push, Candidate, Decision generation, Point, or trading paths.

## 6. Design Gaps

A future design package must define:

- status mapping for readable, missing, parse-failed, archive-linked, count-only, source-trace-partial, and blocked/fail-closed states;
- whether an existing endpoint can be reused or whether one minimal read-only dashboard status endpoint is justified;
- whether dashboard should show only daily count plus latest/archive-link status, not raw table behavior;
- fail-closed reason rules for no missed rows, missing analysis id, parser failure, missing aggregate, and owner-path unavailable;
- source-health fields from `reasonView.parseStatus`, aggregate linkage, count availability, and query availability;
- strict forbidden-call list: `recordFromAuthoritativeAnalysisIfEligible`, `save`, mapper `insert`, `POST /api/review/save`, replay/recheck mutation, Push send, Candidate generation, Decision generation, Point generation, order/execution/auto-trading;
- minimal future targeted tests if implementation later proceeds.

## 7. Boundary Confirmation

This source-read package does not:

- modify Java business code;
- modify tests;
- modify dashboard business logic;
- modify schema/config/pom;
- implement an endpoint or dashboard panel;
- call or add missed-opportunity generation/write behavior;
- call or add review result generation;
- call replay or recheck execution;
- connect Push or external channel;
- generate Candidate;
- generate Decision;
- generate Point;
- output final direction, entry, stop, TP, RR, position size, leverage, order action, execution action, or auto-trading action;
- add DTO / Validator / Assembler / Orchestrator;
- continue P359 or start P360.

Forbidden terms found during source grep are existing business paths, historical docs/tests, point/readiness skeletons, or negative guardrail copy. They are not new behavior in this package.

## 8. Go / No-Go

Decision: **GO to design only**.

Why GO:

- Existing Missed Opportunity read/API/service/mapper/schema assets are real.
- Existing ReviewAggregate archive linkage and review page display are real.
- Existing dashboard count provides partial user-visible status.
- Existing reason parser can support source-health/fail-closed status.
- The future design can reuse existing owners and avoid new DTO / Validator / Assembler.

Why not implementation:

- Dedicated review-only status mapping is not designed.
- Main dashboard does not yet have a dedicated Missed Opportunity / Review Archive status panel.
- Safe boundary between read-only status and existing record/write behavior must be fixed first.
- Targeted endpoint/dashboard/fail-closed/forbidden semantics tests are not scoped yet.

Next allowed action: `Minimal Review-Only Missed Opportunity / Review Archive Status Runtime Wiring Design`.

## 9. Checks Run

- `bash scripts/v1-session-bootstrap.sh`
- `bash scripts/v1-state.sh`
- `bash scripts/check-workflow-contract.sh`
- `bash scripts/codex-next-task.sh`
- targeted source inventory grep for `MissedOpportunityController`, `MissedOpportunityServiceImpl`, `MissedOpportunityMapper`, `tm_missed_opportunity`, `ReviewAggregateServiceImpl`, and dashboard `missedOpportunityCount`
- source file reads for controller/service/mapper/schema/review aggregate/review page/dashboard/tests
- forbidden semantics grep over source paths

Final diff and forbidden-path checks are recorded in the package handoff after edits.

## 10. Freeze Rule Compliance

- 是否创建新骨架: No.
- 是否复用 Cursor-era / V1 资产: Yes, `MissedOpportunityController`, `MissedOpportunityServiceImpl`, `MissedOpportunityMapper`, `tm_missed_opportunity`, `MissedReasonViewParser`, `ReviewAggregateServiceImpl`, review page, and dashboard count assets.
- 是否减少重复: Yes, source read anchors future design to existing owners instead of creating wrapper owners.
- 是否提升 capability level: No, source read only.
- 是否接 service/runtime/dashboard/API: No new wiring; source read inventories existing service/API/dashboard/read assets.
- 是否符合 #830 审计建议: Yes.
