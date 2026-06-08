# V1 Evidence / Score Review-Only Runtime Source Read

This document records a source-read only pass for the `Evidence / Score review-only runtime status` slice. It does not implement Java, tests, dashboard changes, schema changes, endpoint changes, Push, Candidate, Decision, Point, or trading behavior.

## 1. Executive Summary

Evidence / Score review-only runtime status is suitable as the next minimal runtime slice, with the important constraint that the next step must be design only, not implementation.

Evidence owner path exists. The current path is `tm_evidence_item` -> `EvidenceItemMapper` -> `EvidenceServiceImpl` / `EvidenceService` -> `EvidenceController` and dashboard detail `evidenceTopItems`. Evidence build is available through `POST /api/evidence/build`, and persisted brief reads are available through `EvidenceService.listTopEvidenceBriefByAnalysisId`.

Score owner path exists. The current path is `tm_score_item` -> `ScoreItemMapper` -> `ScoreServiceImpl` / `ScoreService` -> `ScoreController` and dashboard detail `scoreTopItems`. Score build is available through `POST /api/score/build` and `GET /api/score/list`, and persisted brief reads are available through `ScoreService.listTopScoreBriefByAnalysisId`.

Services, controllers/API, dashboard detail output, mappers, schema tables, and tests exist. Dashboard visibility is partial: `/api/dashboard/detail` exposes `evidenceTopItems` and `scoreTopItems`, `dashboard.html` renders them under the detail view, and `review-page.js` renders the same top3 sections with explicit read-only copy for evidence and partial-completion copy for scores. There is not yet a dedicated Evidence / Score runtime status panel or status endpoint.

Review-only / fail-closed / not-trading-signal boundaries are partial. Existing dashboard/source-trace display surfaces have `notTradeInstruction` guardrails, Evidence / Score detail reads return empty lists when `analysisId` is unavailable or service rows are absent, and tests cover the top item read path. However, there is no dedicated Evidence / Score status mapping for missing, incomplete, fail-closed, or not-trading-signal states yet.

No new DTO / Validator / Assembler is required for the next design. Future implementation should reuse existing owner-path objects and may return a minimal map or existing VO only after a separate readiness gate. This source-read does not and must not connect Push, Candidate, Decision, Point, external channel, order, execution, or auto-trading.

Next step: **GO: Minimal Review-Only Evidence / Score Runtime Wiring Design**. That next package must remain design-only.

## 2. Source Read Inventory

| Area | Files/classes found | Existing behavior | Runtime/API connection | Dashboard connection | Gap |
|---|---|---|---|---|---|
| Evidence owner path | `EvidenceService`, `EvidenceServiceImpl`, `EvidenceController`, `EvidenceItemMapper`, `EvidenceItemVO`, `EvidenceBriefVO`, `tm_evidence_item` | Builds evidence from market environment, hot reset events, funding/OI, leverage, volatility, macro, and price-structure heuristics. Reads top3 persisted evidence brief rows by `analysisId`. | `POST /api/evidence/build` exists. Persisted read path exists through service/mapper, not as dedicated status endpoint. | `/api/dashboard/detail` exposes `evidenceTopItems`; `dashboard.html` renders `证据明细（前3条）`; `review-page.js` renders `结构化证据（前3条）` with read-only copy. | No dedicated review-only Evidence status endpoint/panel; no explicit Evidence status mapping yet. |
| Score owner path | `ScoreService`, `ScoreServiceImpl`, `ScoreController`, `ScoreItemMapper`, `ScoreItemVO`, `ScoreBriefVO`, `tm_score_item` | Builds trend, credibility, funding, leverage risk, liquidity quality, sentiment, macro, and event impact score rows. Reads top3 persisted score brief rows by `analysisId`. | `POST /api/score/build` and `GET /api/score/list` exist. Persisted read path exists through service/mapper, not as dedicated status endpoint. | `/api/dashboard/detail` exposes `scoreTopItems`; `dashboard.html` renders `评分明细（前3条）`; `review-page.js` says `tm_score_item` top3 does not mean all eight scores are complete. | No dedicated review-only Score status endpoint/panel; score values can be misread as readiness/ranking without explicit safety copy. |
| Evidence service | `EvidenceServiceImpl` | Generates `EvidenceItemVO` rows and normalizes evidence type/direction/source. `listTopEvidenceBriefByAnalysisId` returns empty when `analysisId` is missing and returns mapper rows otherwise. | Runtime service exists and is used by build controller and dashboard detail read path. | Dashboard detail reads persisted top3 evidence. | Missing explicit status/fail-closed/readiness reason object. |
| Score service | `ScoreServiceImpl` | Generates score rows from `AssetAnalysisVO` / `MarketEnvironmentVO` and returns top3 persisted score briefs. | Runtime service exists and is used by build/list controller and dashboard detail read path. | Dashboard detail reads persisted top3 scores. | Missing explicit status/fail-closed/readiness reason object. |
| Controller/API | `EvidenceController`, `ScoreController`, `DashboardController` | Evidence/Score build APIs exist; dashboard detail API exposes top items. | Existing APIs are build/detail APIs, not status APIs. | Dashboard detail API is the current user-visible read surface. | Need design to decide whether a minimal status endpoint is needed or dashboard detail is enough. |
| Dashboard | `dashboard.html`, `review-page.js` | Shows evidence/score top3 in detail/review sections. | No dedicated Evidence / Score runtime status API call. | Visible in detail after a selected symbol/analysis context. | No global status panel, no explicit not-trading-signal copy around score status. |
| Tests | `EvidenceServiceImplTest`, `ScoreServiceImplTest`, `EvidenceItemMapperIntegrationTest`, `ScoreItemMapperIntegrationTest`, `DashboardControllerTest`, `ReviewAggregateServiceImplEvidenceTopItemsTest`, `ReviewAggregateServiceImplScoreTopItemsTest` | Tests cover service behavior, mapper top3 reads, dashboard detail exposure, and review aggregate top items. | Existing targeted coverage confirms owner-path reads. | Dashboard tests assert `evidenceTopItems` / `scoreTopItems` arrays and empty-list fallback. | No status endpoint/static copy tests yet. |
| Schema / mapper | `schema.sql`, `EvidenceItemMapper`, `ScoreItemMapper` | Tables and mappers exist for persisted Evidence/Score items. | Mapper read/write path exists. | Dashboard reads through service -> mapper. | Schema has persisted rows but no status/audit table for Evidence/Score readiness. |
| Review-only flags | `DashboardDetailResponseVO` safe default displays, dashboard/source-trace tests, review-page copy | Existing surrounding dashboard displays force read-only and `notTradeInstruction` in several adjacent display models. | Indirect guardrails exist. | Evidence/score section is read-only display, but not a dedicated status slice. | Need explicit Evidence/Score status copy: not a trading signal, not candidate ranking, not final direction. |
| Fail-closed behavior | `DashboardController.resolveEvidenceTopItems`, `resolveScoreTopItems` | Missing decision/service/analysisId resolves to empty list. | Existing detail path fails empty, not with explicit status. | Empty hints render as `证据明细暂无` / `评分明细暂无`. | Need design for `EMPTY_FAIL_CLOSED` / `INCOMPLETE` style status before implementation. |
| Source trace / provenance | `EvidenceBriefVO.source`, `EvidenceBriefVO.direction`, `ScoreBriefVO.scoreType`, `ScoreBriefVO.scoreValue`, source-trace display models | Evidence has type/description/direction/source. Score brief has type/value only. | Partial provenance exists. | Detail UI maps evidence source labels and score rows. | Score provenance is partial; source trace must remain partial unless owner fields support it. |

## 3. Existing Runtime Flow

```text
Watchlist / MarketQuote completed slices
  -> analysis / market environment context
  -> EvidenceServiceImpl / EvidenceItemVO
  -> tm_evidence_item / EvidenceItemMapper / EvidenceBriefVO
  -> ScoreServiceImpl / ScoreItemVO
  -> tm_score_item / ScoreItemMapper / ScoreBriefVO
  -> EvidenceController / ScoreController build APIs and DashboardController detail API
  -> dashboard.html / review-page.js top3 evidence and score display
```

Flow status:

- `Watchlist / MarketQuote completed slices`: exists as prior review-only runtime slices. This source-read does not wire Evidence/Score to them.
- `analysis / market environment context`: exists / partial. Evidence and Score services consume `AssetAnalysisVO` and `MarketEnvironmentVO`, but a dedicated runtime status boundary is missing.
- `EvidenceServiceImpl / EvidenceItemVO`: exists, runtime service, review-only safe if displayed as explanatory evidence only.
- `tm_evidence_item / EvidenceItemMapper / EvidenceBriefVO`: exists, persisted read model, dashboard-visible through detail.
- `ScoreServiceImpl / ScoreItemVO`: exists, runtime service, review-only safe only if score is labelled as explanatory status and not ranking/readiness.
- `tm_score_item / ScoreItemMapper / ScoreBriefVO`: exists, persisted read model, dashboard-visible through detail.
- `EvidenceController / ScoreController`: exists, but build/list APIs are not a minimal status endpoint.
- `DashboardController detail`: exists, dashboard visible, partial status surface.
- Dedicated review-only Evidence / Score status mapping: missing.

## 4. Evidence / Score Readiness

Evidence status can be read partially. The owner path can read top persisted evidence rows by `analysisId`, and dashboard detail exposes them. It cannot yet report a dedicated Evidence status such as ready / empty / incomplete / blocked.

Score status can be read partially. The owner path can build scores and read top persisted score rows by `analysisId`, and dashboard detail exposes them. It cannot yet report a dedicated Score status that prevents score values from being read as candidate ranking or trading readiness.

Evidence / Score availability can be inferred from non-empty top3 rows, but availability is not a formal status. Missing `analysisId`, missing decision, missing service, or no rows currently degrades to empty lists.

Data missing / stale / incomplete cannot be fully judged from the current Evidence / Score surface. Evidence has source labels and descriptions; Score top3 has type/value but no freshness, completeness, or source trace field. A future design must avoid pretending those fields are available.

Fail-closed exists only as an empty-read behavior in the dashboard detail path. It should become explicit only in a future minimal status design.

Source trace is partial. Evidence top rows include `source` and `direction`; Score top rows include `scoreType` and `scoreValue` only. Any future source trace display must mark score provenance as partial unless the existing owner path proves more fields are available.

Tests exist for service behavior, mapper top3 reads, dashboard detail exposure, and review aggregate top items. Tests do not yet cover a dedicated Evidence / Score runtime status panel or endpoint.

Dashboard DOM exists for detail-level evidence/score display, not for a status panel. A future design should decide whether to reuse the detail area or add a minimal safe status slot.

## 5. Boundary Confirmation

Evidence / Score slice must not generate Candidate.

Evidence / Score slice must not generate Decision.

Evidence / Score slice must not generate Point.

Evidence / Score slice must not generate final direction.

Evidence / Score slice must not send Push.

Evidence / Score slice must not connect an external channel.

Evidence / Score slice must not connect order / execution / auto-trading.

Evidence / Score slice must not bypass Watchlist / MarketQuote boundaries. If it later reads a symbol or analysis context, it must remain bounded by existing review-only context or explicitly mark the output as dashboard/detail-only.

Evidence / Score slice must not treat Display Slots as candidate pool.

Score values must not be labelled as candidate rank, point readiness, final direction, or trading signal.

## 6. Candidate Slice Comparison / Go-NoGo

Decision: **A. GO: Minimal Review-Only Evidence / Score Runtime Wiring Design**.

Reason:

- The canonical owner path already exists: `EvidenceService` / `EvidenceItemMapper` / `tm_evidence_item` and `ScoreService` / `ScoreItemMapper` / `tm_score_item`.
- User-visible dashboard/detail output already exists through `evidenceTopItems` and `scoreTopItems`.
- Targeted tests already prove the current top-item read path.
- The missing surface is designable as a minimal review-only status, not a reason to create new DTO / Validator / Assembler families.
- The risk is manageable if the next step is design only and explicitly blocks Candidate / Decision / Point / Push / Trading semantics.

Owner path candidate for the next design:

```text
tm_evidence_item / tm_score_item
  -> EvidenceItemMapper / ScoreItemMapper
  -> EvidenceService / ScoreService
  -> DashboardController detail or future minimal status endpoint
  -> dashboard evidence/score review-only status display
```

Dashboard/API minimal status candidates for design only:

- evidence row count / top item availability;
- score row count / top item availability;
- source/provenance partial status;
- `reviewOnly = true`;
- `notTradingSignal = true`;
- empty or missing rows fail closed for candidate/push/point;
- score values are explanatory only, not ranking/readiness.

No-Go is not selected because the controller/service/source evidence is sufficient for design. However, direct implementation remains blocked until a design and readiness gate confirm endpoint, DOM, copy, and tests.

## 7. Rejected Expansion

Push external channel is rejected. Evidence / Score status cannot become sendable output.

Candidate generation is rejected. Evidence / Score status cannot become candidate creation, ranking, or promotion.

DecisionResult wiring is rejected for this slice. Decision read model remains a separate owner path and must not be blended into Evidence / Score status.

ExecutionPlan / BoundaryCandidate wiring is rejected. That track already has its owner-path safety coverage and should not be reopened through score semantics.

Point generation is rejected. No entry, stop, TP, RR, point readiness, final direction, position size, leverage, or executable output is allowed.

Order / execution / auto-trading is rejected.

P359 / P360 remain frozen. P359 was not merged and #829 closed unmerged; P360 is not allowed to start.

Three AI is rejected. Evidence / Score status should use existing Cursor-era owner assets, not a new provider orchestration path.

## 8. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, source read only
- 是否接 service/runtime/dashboard/API: No, source read only
- 是否符合 #830 审计建议: Yes
