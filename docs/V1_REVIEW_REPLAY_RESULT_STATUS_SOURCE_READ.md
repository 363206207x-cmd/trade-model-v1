# V1 Review / Replay Result Status Source Read

## 1. Executive Summary

本任务只做 source read，不实现功能。

结论：`Review / Replay result status` 适合作为第 7 条最小 Review-Only Runtime partial 切片进入 design。现有 `ReviewController` / `ReviewService` / `ReviewResultMapper` / `tm_review_result` / `ReviewAggregateService` / review page / targeted tests 已经形成可复用 owner path；但 dedicated review-only runtime status endpoint 和 dashboard status panel 仍缺失，下一步只能进入 design，不能直接 implementation。

本次确认：

- ReviewResult owner path: exists。
- ReviewService: exists。
- Review controller/API: exists, includes read endpoints and one write endpoint that future status must not call.
- Dashboard / review page display: partial, review page and aggregate/detail assets exist, dashboard runtime status panel missing.
- Tests: exists for ReviewService, ReviewAggregate, replay summary, and review controller read-only paths.
- Schema / mapper: exists via `tm_review_result` and `ReviewResultMapper`.
- Review-only / fail-closed / not-trading-signal boundary: partial; existing read paths are safe, but dedicated status flags are missing.
- Replay assets: partial; summary read assets exist, replay execution endpoint is side-effectful and must be excluded.
- New DTO / Validator / Assembler needed now: No.
- Push / Candidate / Decision generation / Point / Trading touched: No.

下一步：`Minimal Review-Only Review / Replay Result Status Runtime Wiring Design`。下一步仍只能是 design，不是 implementation。

## 2. Source Read Inventory

| Area | Files/classes found | Existing behavior | Runtime/API connection | Dashboard connection | Gap |
|---|---|---|---|---|---|
| ReviewResult owner path | `ReviewResultDO`, `ReviewResultMapper`, `tm_review_result` | Stores review result by `analysis_id` with error type, actual outcome, and adjustment suggestion | Read through mapper/service; write through `ReviewService#saveOrUpdate` | Indirect through review page / aggregate | No dedicated review-only runtime status mapping yet |
| ReviewService | `ReviewService`, `ReviewServiceImpl` | `getStateByAnalysisId` reads review state; `saveOrUpdate` writes review result and rule log | Read path is reusable; write path must be excluded from future status endpoint | Review page consumes state/save endpoints | Future status must use read-only path only |
| ReviewController/API | `ReviewController` | Provides aggregate, summary, detail, state, rule-version logs, and save endpoints | `GET /api/review/state/{analysisId}`, aggregate/summary/detail are read paths; `POST /api/review/save` is write path | Review page JS uses these endpoints | Dedicated `/api/.../review-replay-status` style endpoint missing |
| ReviewAggregate | `ReviewAggregateService`, `ReviewAggregateServiceImpl`, aggregate VO classes | Builds review aggregate over AnalysisRun, DecisionResult, ExecutionPlan, MarketEnvironment, Evidence/Score, PushRecheck logs, ReviewResult, rule logs | Existing read owner path returns aggregate optional by analysis id | Existing review page/detail assets can show aggregate | Dedicated status/fail-closed fields missing |
| Review page / dashboard | `templates/review.html`, `static/js/review-page.js`, dashboard/detail docs | Review page reads aggregate/state and allows manual review save | Uses existing API read and save paths | Review page exists; main dashboard dedicated Review/Replay status panel missing | Future dashboard status panel must be minimal and read-only |
| Replay summary | `PushRecheckService#summarizeReplayByDispatch`, `PushRecheckReplaySummaryVO`, ops overview VO/tests | Reads replay log summary/counters when logs exist | Summary read path can inform a future status if kept read-only | Ops overview and review aggregate expose replay summary fields | Must not execute replay/recheck |
| Replay execution | `PushRecheckController#replay`, `PushRecheckService#replayByDispatch` | Replays by dispatch and creates a replay batch/logs | Side-effectful; not eligible for review-only status | Not a safe dashboard source for this slice | Explicitly forbidden for next design |
| Tests | `ReviewServiceImplTest`, `ReviewAggregateServiceImplTest`, `ReviewAggregateServiceImplEvidenceTopItemsTest`, `ReviewAggregateServiceImplScoreTopItemsTest`, `PushRecheckServiceImplTest`, `SourceTraceEntryReadOnlyReviewControllerTest` | Existing tests cover review persistence/read state, aggregate, top items, replay summary, and read-only source trace review | Reusable as precedent for future targeted tests | Dashboard/template status tests still missing | Future implementation would need endpoint/panel/fail-closed tests |
| Schema / mapper | `schema.sql`, `ReviewResultMapper` | `tm_review_result` and mapper methods exist | No schema change needed for source-read/design | Indirect through review page/aggregate | Future design should avoid schema changes |
| Review-only flags | Existing docs/tests mention review-only/fail-closed/not-trading boundaries | Boundary language exists across prior slices | Not dedicated to Review/Replay status endpoint | Not dedicated to dashboard status panel | Future mapping must add explicit safe flags if implementation later proceeds |
| Source trace / provenance | Review aggregate includes rule logs, market/evidence/score/decision/plan context, replay summaries | Provenance is partial but usable as read context | Existing aggregate/detail path is the owner candidate | Review page detail can display aggregate context | Source trace completeness must be mapped conservatively |

## 3. Existing Runtime Flow

```text
Watchlist / MarketQuote / Evidence-Score / DecisionResult / ExecutionPlan completed slices
  -> ReviewResult / tm_review_result / ReviewResultMapper (exists, runtime read/write owner; read path safe)
  -> ReviewService#getStateByAnalysisId (exists, runtime read; review-only safe when used as GET/read)
  -> ReviewController GET /api/review/state/{analysisId} and aggregate/summary/detail (exists, runtime API; read-only safe)
  -> ReviewAggregateService / ReviewAggregateVO (exists, runtime read aggregate; partial fail-closed via Optional.empty)
  -> review.html / review-page.js (exists, review page visible; dashboard status panel missing)
  -> PushRecheck replay summary read path (partial, read-only summary usable if isolated)
  -> PushRecheck replay execution path (exists but side-effectful; forbidden for this slice)
```

Segment status:

- Completed upstream runtime slices: exists; runtime; dashboard visible; review-only safe.
- ReviewResult owner path: exists; runtime; indirectly visible through review page; safe only through read endpoints.
- Review aggregate path: exists; runtime read; visible through review page/detail; partial fail-closed via missing aggregate.
- Replay summary: partial; runtime read; visible in ops/review aggregate contexts; safe only as summary.
- Replay execution: exists; runtime write/side effect; not review-only safe; must not be called.
- Dedicated Review / Replay status endpoint: missing.
- Dedicated dashboard status panel: missing.

## 4. Review / Replay Readiness

- Can read ReviewResult status: Yes, through `ReviewService#getStateByAnalysisId` and `GET /api/review/state/{analysisId}`.
- Can read Review aggregate status: Yes, through `ReviewAggregateService` and existing review aggregate endpoints.
- Can determine missing/incomplete: Partial. Missing `AnalysisRunDO` returns empty aggregate; missing ReviewResult can be inferred from empty/null review state.
- Can determine fail-closed: Partial. The data path supports conservative missing/partial mapping, but explicit `failClosed` field is not dedicated yet.
- Can show replay result status: Partial. Replay summary exists, but replay execution must be excluded.
- Can show source trace/provenance: Partial. Review aggregate has context and rule logs, but a dedicated source-trace completeness mapping is missing.
- Existing tests: Yes, enough to justify design; not enough for implementation.
- Dashboard DOM slot: Partial. Review page exists; main dashboard status panel is missing.

## 5. Boundary Confirmation

Future Review / Replay status must:

- Not generate Candidate.
- Not generate a new Decision.
- Not generate Point.
- Not output final direction.
- Not output entry / stop / TP / RR.
- Not send Push.
- Not call external channel.
- Not call order / execution / auto-trading.
- Not call replay execution or recheck mutation path.
- Not bypass Watchlist / MarketQuote / Evidence-Score / DecisionResult / ExecutionPlan boundaries.
- Not treat Display Slots as a candidate pool.
- Fail closed when review result, aggregate, replay summary, or source trace is missing/ambiguous.

## 6. Candidate Slice Comparison / Go-NoGo

Decision: **A. GO: Minimal Review-Only Review / Replay Result Status Runtime Wiring Design**.

Owner path candidate:

```text
Completed review-only runtime slices
  -> ReviewResult / ReviewService / ReviewResultMapper / tm_review_result
  -> ReviewAggregateService / review aggregate detail
  -> optional PushRecheck replay summary read path
  -> future minimal Review / Replay status endpoint/dashboard panel
```

Dashboard/API minimal status candidate:

- Review status availability.
- Replay summary availability.
- Review aggregate availability.
- Source trace / provenance completeness.
- Fail-closed status for missing/incomplete/ambiguous data.
- Explicit `reviewOnly=true`.
- Explicit not trading / not candidate / not decision generation / not point safety flags.

Why GO:

- Existing Review owner assets are real and already runtime-readable.
- Existing replay summary assets can be treated as read-only inventory if separated from replay execution.
- Dedicated status endpoint/panel is missing but can be designed conservatively without new owner wrappers.
- Source read found enough tests and owner paths to support a design package.

Why not implementation yet:

- The safe status mapping is not designed.
- The replay summary versus replay execution boundary must be fixed before any code.
- Dedicated endpoint/panel fields and fail-closed rules must be designed first.

## 7. Rejected Expansion

Not doing now:

- Push external channel.
- Replay execution / recheck mutation.
- Candidate generation.
- Decision generation.
- ExecutionPlan / BoundaryCandidate expansion.
- Point generation.
- Entry / stop / TP / RR output.
- Order / execution / auto-trading.
- Automatic rule correction / feedback loop.
- P359 / P360.
- Three AI expansion.

## 8. Freeze Rule Compliance

- 是否创建新骨架: No.
- 是否复用 Cursor-era 资产: Yes, ReviewService / ReviewController / ReviewResultMapper / tm_review_result / ReviewAggregateService / replay summary assets.
- 是否减少重复: Yes, source read anchors the future slice to existing owners instead of creating wrapper owners.
- 是否提升 capability level: No, source read only.
- 是否接 service/runtime/dashboard/API: No, source read only; it only identifies existing assets and future design candidates.
- 是否符合 #830 审计建议: Yes.

## 9. Final Recommendation

可以进入 `Minimal Review-Only Review / Replay Result Status Runtime Wiring Design`。最小设计应复用现有 ReviewResult / ReviewService / ReviewController / ReviewAggregate / replay summary read assets，明确排除 replay execution、Push、Candidate、Decision generation、Point、final direction、entry/stop/TP/RR、order/execution/auto-trading，并保持当前能力层级为 `REVIEW_ONLY_RUNTIME partial`。
