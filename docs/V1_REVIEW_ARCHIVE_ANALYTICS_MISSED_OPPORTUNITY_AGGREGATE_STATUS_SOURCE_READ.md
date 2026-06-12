# V1 Review Archive Analytics / Missed Opportunity Aggregate Status Source Read

## 1. Current Merged Main

- Current merged main: `2f535cf docs(runtime): select next slice after paper observation closure`
- Current module: `Review Archive Analytics / Missed Opportunity Aggregate Status`
- Current phase: `Source Read`
- Risk level: `A`
- Completed review-only runtime partial slices: 14
- Capability movement: none. The project remains `REVIEW_ONLY_RUNTIME partial`.

This package is source-read only. It records existing owner paths and risks for a future design. It does not implement archive analytics, aggregate status, endpoint behavior, dashboard behavior, missed-opportunity generation/write behavior, review result generation, replay/recheck execution, Push, Candidate generation, Decision generation, Point generation, order/execution, or trading.

## 2. Source Read Files

| Area | Files read | Findings |
|---|---|---|
| Existing missed archive status API | `src/main/java/org/example/trademodel/controller/MissedOpportunityController.java` | Existing `GET /api/missed-opportunity/query` and `GET /api/missed-opportunity/review-archive-status` read `MissedOpportunityService` only. The status endpoint already exposes review-only, fail-closed, not Candidate, not Decision generation, not Point, not replay/recheck, not missed-opportunity generation, not review-result generation, and not executable safety flags. |
| Missed Opportunity service | `src/main/java/org/example/trademodel/service/MissedOpportunityService.java`, `src/main/java/org/example/trademodel/service/impl/MissedOpportunityServiceImpl.java` | Read methods exist: `findByMissedId`, `listByDecisionId`, `listBySymbol`, `listByAnalysisId`, `listByBizDate`, `query`, and `countByBizDate`. Write/generation methods also exist: `recordFromAuthoritativeAnalysisIfEligible` and `save`; future design must forbid them. |
| Missed Opportunity mapper/schema owner | `src/main/java/org/example/trademodel/mapper/MissedOpportunityMapper.java`, `src/main/java/org/example/trademodel/entity/MissedOpportunityDO.java`, `src/main/resources/schema.sql` | Existing table owner is `tm_missed_opportunity`. Read queries support by missed id, decision id, symbol, analysis id, biz date, scoped query, and biz-date count. The mapper also has `insert`, which is forbidden for this slice. |
| Reason parse layer | `src/main/java/org/example/trademodel/service/MissedReasonViewParser.java`, `src/main/java/org/example/trademodel/vo/MissedOpportunityQueryItemVO.java`, `src/main/java/org/example/trademodel/vo/MissedReasonViewVO.java` | Existing parser maps `reason_json` to `OK`, `EMPTY_REASON_JSON`, or `PARSE_FAILED` without throwing. This can support read-only source health, parse status, partial, and fail-closed states. |
| Review archive aggregate | `src/main/java/org/example/trademodel/service/ReviewAggregateService.java`, `src/main/java/org/example/trademodel/service/impl/ReviewAggregateServiceImpl.java`, `src/main/java/org/example/trademodel/vo/ReviewAggregateVO.java`, `src/main/java/org/example/trademodel/vo/ReviewAggregateSummaryVO.java`, `src/main/java/org/example/trademodel/vo/ReviewAggregateDetailVO.java` | Existing aggregate owner path reads `missedOpportunityMapper.listByAnalysisId`, sets `ReviewAggregateVO.missed`, exposes `ReviewAggregateDetailVO.missed`, and adds `DetailSectionMeta(section=missed,total=...)`. It is useful as read-only linkage evidence, but the aggregate also contains Push/Recheck and plan fields, so future design must avoid exposing execution-adjacent fields as status output. |
| Review controller/page | `src/main/java/org/example/trademodel/controller/ReviewController.java`, `src/main/resources/static/js/review-page.js` | `GET /api/review/aggregate/{analysisId}`, `GET /api/review/aggregate/{analysisId}/summary`, and `GET /api/review/aggregate/{analysisId}/detail?section=missed` are read paths. `POST /api/review/save` is a write/review-result path and must be forbidden. `review-page.js` renders read-only `sec-missed` archive rows. |
| Dashboard owner surface | `src/main/java/org/example/trademodel/controller/DashboardController.java`, `src/main/resources/templates/dashboard.html` | Existing dashboard has `missedArchiveStatusPanel`, fetches `/api/missed-opportunity/review-archive-status`, shows scoped/today count, latest row, reason parse/source health, review-only copy, generation/replay boundary copy, and Display Slots not candidate pool copy. |
| Existing tests | `src/test/java/org/example/trademodel/controller/MissedOpportunityControllerTest.java`, `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`, `src/test/java/org/example/trademodel/service/impl/MissedOpportunityServiceImplTest.java`, `src/test/java/org/example/trademodel/service/impl/ReviewAggregateServiceImplTest.java`, `src/test/java/org/example/trademodel/service/impl/ReviewAggregateServiceImplEvidenceTopItemsTest.java`, `src/test/java/org/example/trademodel/service/impl/ReviewAggregateServiceImplScoreTopItemsTest.java`, `src/test/java/org/example/trademodel/service/impl/ReviewAggregateServiceImplMarketEnvironmentTest.java` | Existing tests cover read-only archive endpoint safety fields, empty/count-only/parse-failure states, forbidden field absence, dashboard DOM endpoint reference, missed write eligibility, query normalization, aggregate detail section behavior, and aggregate top-item assembly. |
| Prior records | `docs/V1_MISSED_OPPORTUNITY_REVIEW_ARCHIVE_STATUS_SOURCE_READ.md`, `docs/V1_MINIMAL_REVIEW_ONLY_MISSED_OPPORTUNITY_REVIEW_ARCHIVE_STATUS_RUNTIME_WIRING_DESIGN.md`, `docs/V1_MISSED_OPPORTUNITY_REVIEW_ARCHIVE_STATUS_RUNTIME_WIRING_IMPLEMENTATION_READINESS_GATE.md`, `docs/V1_MINIMAL_REVIEW_ONLY_MISSED_OPPORTUNITY_REVIEW_ARCHIVE_STATUS_RUNTIME_WIRING_IMPLEMENTATION.md`, `docs/V1_MISSED_OPPORTUNITY_REVIEW_ARCHIVE_STATUS_RUNTIME_WIRING_VERIFICATION.md`, `docs/V1_MISSED_OPPORTUNITY_REVIEW_ARCHIVE_STATUS_VISUAL_VERIFICATION_CLOSURE.md` | The base Missed Opportunity / Review Archive status slice is already closed as the 10th review-only runtime partial slice. This package must treat analytics/aggregate as a second source-read pass and avoid duplicating the completed owner path. |

## 3. Reusable Assets

| Reusable asset | Reuse decision |
|---|---|
| `GET /api/missed-opportunity/review-archive-status` | Existing status path can be the primary owner candidate for future design. It already carries safety flags and count/detail/readiness metadata. |
| `GET /api/missed-opportunity/query` | Existing row query path for drill-down context; future status should not turn this into generation/write behavior. |
| `MissedOpportunityService` read methods | Reusable for count and scoped read status. Future design must forbid `recordFromAuthoritativeAnalysisIfEligible` and `save`. |
| `MissedOpportunityMapper` read queries and `tm_missed_opportunity` | Reusable as canonical persistence read owner. Future design must forbid mapper `insert`. |
| `MissedReasonViewParser` | Reusable as source health / parse status evidence. No new validator/parser skeleton is needed. |
| `ReviewAggregateService#getAggregateSummaryByAnalysisId` | Reusable for aggregate availability and `detailSections` missed total, if the future design scopes it to read-only aggregate metadata. |
| `ReviewAggregateService#getAggregateDetailByAnalysisId(..., "missed", ...)` | Reusable for read-only missed section linkage. It must not expose plan, Push/Recheck, or action fields as positive status output. |
| `review-page.js#renderMissed` / `#sec-missed` | Existing read-only review archive surface for detailed table display. |
| `dashboard.html#missedArchiveStatusPanel` | Existing dashboard status panel and safety copy. Future analytics design should decide whether it extends this panel or adds a tiny aggregate section; no implementation in this package. |

## 4. Existing Aggregate / Count Sources

The following count/status sources already exist:

- `MissedOpportunityMapper#countByBizDate(LocalDate)` for daily count.
- `MissedOpportunityMapper#listByAnalysisId(String)` for per-analysis archive linkage.
- `MissedOpportunityMapper#listByQuery(...)` for scoped read by analysis, symbol, and date.
- `MissedOpportunityController#reviewArchiveStatus(...)` for scoped count, latest row, parse status, trace/linkage, source health, and safety flags.
- `ReviewAggregateSummaryVO.detailSections` with `section=missed` and missed total.
- `ReviewAggregateDetailVO.missed` for read-only lazy loaded missed rows.
- dashboard `missedArchiveStatusPanel` and `missedOpportunityCount(...)` count visibility.

Potential future aggregate states can be designed from these existing sources without schema/config/pom changes and without new DTO / Validator / Assembler / Orchestrator.

## 5. Current Gaps

| Gap | Impact | Design note |
|---|---|---|
| Analytics aggregate is not separately designed | Existing Missed Archive status is complete, but a broader "archive analytics / aggregate" status has no separate mapping yet. | Design must prove the new slice is not a duplicate of the completed status panel. |
| Stale state is not explicit | `tm_missed_opportunity` has `biz_date` and `create_time`, but no explicit freshness/staleness field. | Future design can define date-based read-only stale/unknown status only if it avoids fabricating facts. |
| ReviewAggregate includes execution-adjacent fields | Aggregate VO includes plan, Push/Recheck, and closure summaries. | Future design should use only missed counts/section metadata and not expose plan or Push/Recheck fields as actionable output. |
| Review result write path exists nearby | `ReviewController#saveReview` and `ReviewService#saveOrUpdate` are write paths. | Future design must forbid review result generation/write behavior. |
| Missed write path exists nearby | `MissedOpportunityServiceImpl#recordFromAuthoritativeAnalysisIfEligible`, `save`, and mapper `insert` write rows. | Future design must forbid generation/write and use only reads. |
| Replay/recheck and Push assets exist in ReviewAggregate context | Aggregate reads Push/Recheck rows for review pages. | Future design must mark those assets context-only and never trigger replay/recheck execution, Push send, or external channel. |

## 6. Boundary Risks

This source read found these risks for future design:

- missed-opportunity generation/write behavior via `recordFromAuthoritativeAnalysisIfEligible`, `save`, and mapper `insert`;
- review result generation/write behavior via `POST /api/review/save`;
- replay/recheck execution drift through Push/Recheck aggregate context;
- Push send / external channel drift if aggregate sections are treated as notification behavior;
- Candidate / Decision generation / Point drift if missed opportunities are interpreted as new opportunities to rank or generate;
- final direction / entry / stop / TP / RR drift if aggregate plan fields are surfaced;
- duplicate owner drift if a new analytics DTO/service/mapper family is created instead of reusing existing owner paths.

All of the above must be forbidden in the next design.

## 7. Source-Read Conclusion

Source-read result: **GO to design only**.

Why GO:

- A concrete read owner path already exists for missed archive status.
- ReviewAggregate already supplies missed section count/detail metadata.
- The dashboard already has a safe `missedArchiveStatusPanel`.
- Existing tests already cover the base endpoint, safety flags, fail-closed states, and forbidden field absence.
- Future design can reuse current owners without new DTO / Validator / Assembler / Orchestrator, schema/config/pom, or a new service/domain/mapper/repository family.

Why not implementation now:

- The aggregate analytics/status mapping is not yet designed.
- The future design must decide whether to extend the existing missed archive panel/status or define a separate minimal aggregate status.
- Stale/missing/archive count semantics need explicit fail-closed rules.
- ReviewAggregate contains execution-adjacent fields that must be excluded from any positive status output.

## 8. Next Design Questions

The design package must answer:

1. Whether the future slice should reuse `GET /api/missed-opportunity/review-archive-status`, `GET /api/review/aggregate/{analysisId}/summary`, or at most one thin read-only Map endpoint.
2. Whether dashboard should extend `missedArchiveStatusPanel` or add a separate tiny aggregate status row/panel.
3. Which aggregate fields are safe: daily count, scoped count, `detailSections[missed].total`, latest missed id, parse status, source health, archive linkage, and missing/stale/partial state.
4. Which aggregate fields are unsafe: plan fields, Push/Recheck action-like fields, review save/write fields, generated opportunity signals, and point/trading fields.
5. Fail-closed rules for missing archive rows, missing aggregate, missing analysis id, parse failure, count-only partial state, stale/unknown date state, and owner-path exceptions.
6. Required targeted tests if implementation later proceeds.

## 9. Forbidden Scope For Next Package

The next design package must still forbid:

- Java business code changes;
- tests;
- dashboard business logic;
- schema/config/pom;
- endpoint/panel implementation;
- new DTO / Validator / Assembler / Orchestrator;
- new service/domain/mapper/repository ownership family;
- missed-opportunity generation/write behavior;
- review result generation/write behavior;
- replay execution;
- recheck execution;
- Push send;
- external channel;
- Candidate generation;
- Decision generation;
- Point generation;
- final direction / entry / stop / TP / RR;
- order / execution / auto-trading;
- Position Monitor execution;
- paper order / simulated execution / paper PnL;
- P359 / P360.

## 10. Next Allowed Action

- Next allowed action: `Minimal Review-Only Review Archive Analytics / Missed Opportunity Aggregate Status Runtime Wiring Design`
- Next branch: `minimal-review-only-review-archive-analytics-missed-opportunity-aggregate-status-runtime-wiring-design`
- Next risk: `A`
- Allowed changes next: design docs and source-of-truth updates only.

## 11. Freeze Rule Compliance

- 是否创建新骨架: No.
- 是否复用 Cursor-era / V1 资产: Yes, `MissedOpportunityController`, `MissedOpportunityService`, `MissedOpportunityMapper`, `tm_missed_opportunity`, `MissedReasonViewParser`, `ReviewAggregateService`, review page, dashboard status panel, and existing tests.
- 是否减少重复: Yes, by anchoring future analytics/status design to existing Missed Opportunity / ReviewAggregate owners instead of new wrappers.
- 是否提升 capability level: No, source-read only.
- 是否接 service/runtime/dashboard/API: No new wiring; source read inventories existing service/API/dashboard/read assets.
- 是否符合 #830 审计建议: Yes.
