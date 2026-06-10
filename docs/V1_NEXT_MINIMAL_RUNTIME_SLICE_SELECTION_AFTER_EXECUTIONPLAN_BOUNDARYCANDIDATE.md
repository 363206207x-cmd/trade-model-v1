# V1 Next Minimal Runtime Slice Selection After ExecutionPlan / BoundaryCandidate

## 1. Executive Summary

Current merged main（当前已合并主线）: `d907719 docs(wiring): record executionplan boundarycandidate visual closure`.

Completed slices（已完成切片）= 6:

1. `PositionSync + Dashboard review-only status`: `REVIEW_ONLY_RUNTIME partial`
2. `Watchlist + RuleConfig + Dashboard/API review-only status`: `REVIEW_ONLY_RUNTIME partial`
3. `MarketQuote freshness / fallback / dashboard API status`: `REVIEW_ONLY_RUNTIME partial`
4. `Evidence / Score review-only runtime status`: `REVIEW_ONLY_RUNTIME partial`
5. `DecisionResult review-only dashboard/API status`: `REVIEW_ONLY_RUNTIME partial`
6. `ExecutionPlan / BoundaryCandidate review-only runtime status`: `REVIEW_ONLY_RUNTIME partial`

ExecutionPlan / BoundaryCandidate implementation, verification, and visual closure are completed and merged. It is no longer the active in-progress module.

Selected next slice（最终选择的下一个切片）: **Review / Replay result status（复盘 / 回放结果状态）**.

Why this slice now（为什么现在选它）: after Watchlist, MarketQuote, Evidence / Score, DecisionResult, and ExecutionPlan / BoundaryCandidate are visible as review-only runtime states, the next safest user-visible step is to read the existing Review / Replay owner path and decide whether review result availability, replay summary, source health, and fail-closed status can become a minimal dashboard/API status. Source-read evidence already shows `ReviewService`, `ReviewController`, `ReviewResultMapper`, `tm_review_result`, `ReviewAggregateService`, and replay summary assets exist.

This package is selection only. It does not change Java, tests, dashboard business logic, schema/config/pom, Push, Candidate, Decision generation, Point, final direction, entry/stop/TP/RR, order/execution, auto-trading, DTO/Validator/Assembler, P359, or P360.

Allowed next action（下一允许动作）: **Source Read for Review / Replay result status**.

Risk level（风险等级）: `A`.

Capability movement（能力变化）: none; still `REVIEW_ONLY_RUNTIME partial`.

## 2. Candidate Next Slices Considered

| Candidate next slice | Existing assets | User-visible value | Runtime/API readiness | Risk | Duplicate risk | Recommendation |
|---|---|---|---|---|---|---|
| Review / Replay result status | `ReviewService`, `ReviewController`, `ReviewResultMapper`, `ReviewResultDO`, `tm_review_result`, `ReviewAggregateService`, `PushRecheckReplaySummaryVO`, existing tests | High. Shows whether the human review / replay feedback loop has usable read-only state after ExecutionPlan / BoundaryCandidate. | Medium. Review read/write and aggregate owner assets exist; replay is present but must be kept read-only and not confused with Push/Recheck execution. | Medium. Replay language can drift toward Push/Recheck execution unless scoped tightly. | Medium. Must separate ReviewResult, ReviewAggregate, and PushRecheck replay owners. | **Select** for source read only. |
| Data Source Health dashboard status | `DataSourceHealthDO`, repeated `sourceHealth` fields in MarketQuote, Evidence / Score, DecisionResult, ExecutionPlan / BoundaryCandidate, dashboard labels/tests | Medium. Can explain source health across completed panels. | Medium. Local `sourceHealth` is already visible; dedicated aggregate owner/API is unclear. | Low-medium. Mostly status display. | Medium-high. Could duplicate sourceHealth already shown in four completed slices. | Defer until source-health aggregation need is clearer. |
| Internal Push preview status only | Existing internal push preview assembler, dashboard `internalPushPreviewDisplay`, no-op/external-channel disabled copy, tests | Medium. Already visible as internal preview. | Medium. Existing UI/assets exist. | High. The word Push容易被误读为 external channel or sendable message. | Medium. Existing P302-P305 already closed an internal preview path. | Defer; do not reopen Push-adjacent work now. |
| Position Monitor manual-input source read | PositionSync, MonitorService / MonitorAlert, manual position docs/assets, historical Position Monitor read-only display packs | Medium. Useful for position visibility. | Medium. Existing foundations exist. | High. Can drift toward close/reverse/open action suggestion. | Medium. PositionSync slice is already complete; monitor expansion needs a strict gate. | Defer; not after ExecutionPlan closure. |
| Three AI / multi-agent status read-only inventory | `ai_role_results`, `DecisionEngineService`, `AiRoleEnum`, AI conflict docs/assets | Medium. Could clarify AI role availability. | Low-medium. Real provider orchestration is not authorized. | High. Can be misread as final arbiter or final decision. | High. Likely creates provider/orchestration wrapper pressure. | Reject for now. |
| Point / numeric proposal display continuation | Numeric point plans, DTO/validator/assembler skeletons, SourceTrace numeric source docs | Medium. Would be visible but risky. | Low-medium. Many skeletons exist, but no safe runtime owner selected after #830 freeze. | High. Too close to point generation and entry/stop/TP/RR. | High. Revives frozen skeleton family. | Reject. |
| Candidate generation / ranking status | Candidate attention / preview skeletons and dashboard internal preview history | Medium. Candidate visibility can be useful. | Low for current safety target. | High. Directly touches Candidate generation/ranking semantics. | High. Frozen by #830 and current boundary. | Reject. |

## 3. Selected Next Slice

Selected next slice: **Review / Replay result status**.

Owner path候选:

```text
ReviewResult / tm_review_result
  -> ReviewResultMapper
  -> ReviewService / ReviewController
  -> ReviewAggregateService / dashboard detail if present
  -> PushRecheck replay summary only as read-only source-read inventory
  -> future minimal Review / Replay result status
```

This is the maximum safe next step because:

- It follows the user review chain after the sixth completed slice, instead of going back into ExecutionPlan implementation/verification/visual closure.
- It reuses existing Cursor-era / V1 old assets instead of creating a new DTO / Validator / Assembler / Orchestrator family.
- It can be made user-visible as review result availability, replay summary availability, source health, and fail-closed status.
- It can remain strictly review-only and non-executable.
- It can be source-read first before any design or implementation decision.

The source-read package must confirm:

- whether `ReviewResult` owner path is canonical;
- whether replay summary belongs in this slice or must stay separate;
- whether existing controller/API/dashboard surfaces can be reused;
- whether source trace / source health and fail-closed states exist;
- whether the status can be shown without Push, Candidate, Decision generation, Point, final direction, entry/stop/TP/RR, order/execution, or auto-trading semantics.

## 4. Why Not The Others

- Data Source Health dashboard status: defer because `sourceHealth` is already visible in MarketQuote, Evidence / Score, DecisionResult, and ExecutionPlan / BoundaryCandidate. A separate aggregate may be useful later, but source-health-only work has higher duplication risk right now.
- Internal Push preview status only: defer because Push language is high-risk and P302-P305 already closed an internal preview path. Any future work must not imply external send.
- Position Monitor manual-input source read: defer because it can be misread as close/reverse/open action suggestion. PositionSync is already complete, but Position Monitor expansion needs a narrower future gate.
- Three AI / multi-agent status read-only inventory: reject for now because provider orchestration, budget/cache/fallback, and final-arbiter semantics are out of scope.
- Point / numeric proposal display continuation: reject because it is too close to point generation and entry/stop/TP/RR.
- Candidate generation / ranking status: reject because Candidate generation/ranking is explicitly forbidden in this package.

## 5. Forbidden Scope

The next source-read package must not:

- change Java business code;
- change tests;
- change dashboard business logic;
- change schema/config/pom;
- connect Push or external channel;
- generate Candidate;
- generate new Decision;
- generate Point;
- generate final direction;
- generate entry / stop / TP / RR;
- connect order / execution / auto-trading;
- add DTO / Validator / Assembler / Orchestrator;
- continue P359 / P360;
- implement Review / Replay runtime wiring.

## 6. Source-Read Boundary

The next package is **Source Read for Review / Replay result status**.

Allowed scope:

- read existing Review / Replay source files;
- document owner path and gaps;
- compare ReviewResult, ReviewAggregate, and PushRecheck replay assets;
- decide GO/NO-GO to design;
- update source-of-truth only.

Not allowed:

- implementation;
- endpoint/dashboard changes;
- replay execution;
- Push send;
- feedback auto-correction;
- candidate/decision/point/trading semantics.

## 7. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 旧资产: Yes
- 是否减少重复: Yes, by selecting existing Review / Replay owner assets before any new surface.
- 是否提升 capability level: No, selection only
- 是否接 service/runtime/dashboard/API: No, selection only
- 是否符合 #830 审计建议: Yes
