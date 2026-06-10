# Next Minimal Runtime Slice Selection After Review / Replay Closure

## 1. Executive Summary

当前 merged main 是 `91613bb chore(workflow): fix one-command runner CI parsing`。Review / Replay result status visual closure 已在 `5da301b` 完成并合并；`91613bb` 之后的 one-command runner hotfix 只修复工作流，不改变业务能力。

当前已完成 7 个 `REVIEW_ONLY_RUNTIME partial` 小闭环：

1. PositionSync + Dashboard review-only status
2. Watchlist + RuleConfig + Dashboard/API review-only status
3. MarketQuote freshness / fallback / dashboard API status
4. Evidence / Score review-only runtime status
5. DecisionResult review-only dashboard/API status
6. ExecutionPlan / BoundaryCandidate review-only runtime status
7. Review / Replay result status

下一条最小 runtime slice 推荐：`Data Source Health dashboard/API status`。

选择它的原因是：已有 `DataSourceHealthDO`、多处 `sourceHealth` 字段、DashboardController/MarketController 的局部 source-health 状态、dashboard DOM 展示和测试断言；但目前缺少一个聚合的、只读的 Data Source Health owner/source-read 结论。下一步先做 source read，能最大化复用已有 Cursor-era/V1 资产，同时避免直接进入 Push、Candidate、Decision generation、Point、Position Monitor action、Three AI 或交易执行。

本 selection 包不实现功能，不提升 capability level。下一允许动作是：`Source Read for Data Source Health dashboard/API status`。

## 2. Current Completed Runtime Slices

| Completed slice | Level | Boundary |
|---|---|---|
| PositionSync + Dashboard review-only status | REVIEW_ONLY_RUNTIME partial | Dashboard shows provider/fallback/simulated/freshness/open-position count only; not trading. |
| Watchlist + RuleConfig + Dashboard/API review-only status | REVIEW_ONLY_RUNTIME partial | Watchlist Pool is visible as boundary; Display Slots are not a candidate pool. |
| MarketQuote freshness / fallback / dashboard API status | REVIEW_ONLY_RUNTIME partial | Quote source/freshness/fallback/source health visible; dashboard-only sample is not a candidate universe. |
| Evidence / Score review-only runtime status | REVIEW_ONLY_RUNTIME partial | Evidence/Score availability/count/top summary visible; not Candidate, Decision, Point, or trading. |
| DecisionResult review-only dashboard/API status | REVIEW_ONLY_RUNTIME partial | Existing DecisionResult read model is visible; not new Decision generation. |
| ExecutionPlan / BoundaryCandidate review-only runtime status | REVIEW_ONLY_RUNTIME partial | Existing plan/boundary display is visible; not executable and not point generation. |
| Review / Replay result status | REVIEW_ONLY_RUNTIME partial | Existing Review/Replay status is visible; no replay execution and no review result generation. |

All seven slices are review-only. They are not Production Wiring, not Push, not Candidate generation, not Decision generation, not Point generation, and not Trading.

## 3. Candidate Next Slices Considered

| Candidate slice | Existing assets | User-visible value | Runtime/API readiness | Risk | Duplicate risk | Recommendation |
|---|---|---|---|---|---|---|
| Data Source Health dashboard/API status | `DataSourceHealthDO`; repeated `sourceHealth` fields in MarketQuote, Evidence / Score, DecisionResult, ExecutionPlan / BoundaryCandidate, Review / Replay; dashboard source-health labels/tests | High. Users can understand why upstream slices are OK, partial, stale, missing, or blocked. | Medium. Local source-health values exist, but dedicated aggregate owner/API must be source-read. | Low-medium. It is status-only if kept review-only. | Medium. Must avoid duplicating completed slice-specific panels. | Select for source read. |
| SourceTrace / Provenance aggregate status | Source trace fields and copy exist across several completed slices. | Medium-high. Could explain provenance completeness. | Medium. Existing fields are scattered. | Low-medium. Safe if read-only. | Medium-high. It overlaps Data Source Health and slice-specific source trace. | Defer and possibly include as a Data Source Health source-read sub-question. |
| Internal Push preview status only | Historical internal push preview/recheck assets and dashboard display gate exist. | Medium. Could make internal preview state visible. | Medium. Assets exist, but are close to Push/Recheck. | High. Easy to slide toward external channel/send semantics. | High. Could revive frozen Candidate/Push wrapper paths. | Reject for now. |
| Position Monitor manual-input source read | PositionSync, monitor service/docs, monitor alerts, and manual position concepts exist. | Medium. Position visibility is useful. | Medium. Existing foundations need careful source read. | High. Can be misread as close/reverse/open action suggestion. | Medium. PositionSync already completed; expansion risk is real. | Reject for now. |
| Three AI / multi-agent status read-only inventory | `ai_role_results`, DecisionResult AI role summary, AI conflict/read-model assets. | Medium. Could clarify AI role availability. | Low-medium. Real provider orchestration is not authorized. | High. Can be misread as final arbiter or final decision. | High. Likely creates provider/orchestration wrapper pressure. | Reject for now. |
| RiskActionGuard read-only source health/status | Historical RiskActionGuard display/source-binding docs and ExecutionPlan risk fields exist. | Medium. Risk boundary visibility is useful. | Medium-low. Owner path may overlap ExecutionPlan/Position Monitor. | Medium-high. Can drift into action guard decisions. | Medium. RiskActionGuard skeleton/source-binding history is large. | Defer until Data Source Health source read clarifies aggregate health boundaries. |
| Review / Replay follow-up expansion | Review / Replay endpoint/panel just completed. | Low for next step. Current slice is already closed. | High, but already done. | Medium-high if expanded beyond status. | High. Would overwork the just-closed slice. | Reject; already completed as slice 7. |

## 4. Selected Next Slice

Selected slice: `Data Source Health dashboard/API status`.

Owner path candidates for the source-read package:

- `src/main/java/org/example/trademodel/entity/DataSourceHealthDO.java`
- existing `sourceHealth` fields in `MarketController`
- existing `sourceHealth` fields in `DashboardController`
- dashboard source-health DOM/copy in `src/main/resources/templates/dashboard.html`
- existing `DashboardControllerTest` / `MarketControllerTest` source-health assertions
- existing source trace / provenance docs where source health is partial, missing, stale, or blocked

The source-read package must answer whether these assets can form a small review-only aggregate status without introducing a new DTO / Validator / Assembler / Orchestrator, without schema/config/pom changes, and without changing completed slice-specific owner paths.

This is the natural next step after Review / Replay because seven completed panels now each expose local health/safety status. A dedicated source read can determine whether there is an existing canonical source-health owner or whether the status should remain distributed.

## 5. Why Not The Others

- Push external channel / Internal Push preview: not selected because external channel and send semantics remain disabled, and Push/Recheck is too close to delivery behavior.
- Three AI / multi-agent: not selected because provider orchestration, final-arbiter semantics, budget/cache/fallback, and final decision risk are out of scope.
- Point generation: not selected because point generation, final direction, entry, stop, TP, RR, and executable proposal fields remain frozen.
- ExecutionPlan / BoundaryCandidate continuation: not selected because this became the sixth completed review-only runtime slice.
- Review / Replay continuation: not selected because this became the seventh completed review-only runtime slice.
- Position Monitor expansion: not selected because it can be misread as close/reverse/open/moving-stop action suggestion.
- P359 / P360: not selected because P359 was closed unmerged and P360 remains disallowed by the freeze rule.

## 6. Next Step Decision

A. GO: Source Read for Data Source Health dashboard/API status

The next package must remain source-read only. It may read source and docs, inventory existing assets, and update source-of-truth docs. It must not implement a new endpoint, dashboard panel, service, schema, or tests.

Next package name: `Source Read for Data Source Health dashboard/API status`.

Recommended branch: `data-source-health-dashboard-status-source-read`.

## 7. Forbidden Scope

The next source-read package and this selection package must not:

- modify Java business code;
- modify tests;
- modify dashboard business logic;
- modify schema/config/pom;
- connect Push or external channel;
- generate Candidate;
- generate a new Decision;
- generate Point;
- output final direction, entry, stop, TP, RR, position size, leverage, or order action;
- connect order/execution/auto-trading;
- create DTO / Validator / Assembler / Orchestrator;
- continue P359 or P360;
- claim Production Wiring or a capability-level increase.

## 8. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 资产: Yes
- 是否减少重复: Yes, by selecting a source read before any aggregate source-health implementation.
- 是否提升 capability level: No, selection only
- 是否接 service/runtime/dashboard/API: No, selection only
- 是否符合 #830 审计建议: Yes

## 9. Final Recommendation

可以进入 `Source Read for Data Source Health dashboard/API status`。最小下一步只允许读取并盘点 existing source-health owner/API/dashboard/test assets，禁止实现、禁止 Push、禁止 Candidate、禁止 Decision generation、禁止 Point、禁止交易动作、禁止 P359/P360；当前能力层级仍是 `REVIEW_ONLY_RUNTIME partial`。
