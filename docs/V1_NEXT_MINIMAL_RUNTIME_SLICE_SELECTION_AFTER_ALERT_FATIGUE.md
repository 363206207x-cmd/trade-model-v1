# Next Minimal Runtime Slice Selection After Alert Fatigue Closure

## 1. Executive Summary

Current merged main（当前已合并主线）: `a9ec1c9 docs(alerts): record notification policy visual closure`

Completed slices（已完成小闭环）: 12 Review-Only Runtime partial（只读运行时部分完成） slices.

Selected next slice（最终选择）: `SourceTrace / RuntimeKline / DataQuality / MultiTimeframe aggregate review-only status`.

Next allowed action（下一允许动作）: `Source Read for SourceTrace / RuntimeKline / DataQuality / MultiTimeframe aggregate review-only status`.

Next branch（下一分支）: `source-trace-runtime-kline-data-quality-status-source-read`.

Risk level（风险等级）: A, source-read docs and source-of-truth updates only.

Capability movement（能力变化）: none. The project remains `REVIEW_ONLY_RUNTIME partial`; this package does not add runtime behavior, endpoint behavior, dashboard behavior, Push, Candidate generation, Decision generation, Point generation, order/execution, auto-trading, or schema/config/pom changes.

## 2. Completed Runtime Slices

1. `PositionSync + Dashboard review-only status`: `REVIEW_ONLY_RUNTIME partial`
2. `Watchlist + RuleConfig + Dashboard/API review-only status`: `REVIEW_ONLY_RUNTIME partial`
3. `MarketQuote freshness / fallback / dashboard API status`: `REVIEW_ONLY_RUNTIME partial`
4. `Evidence / Score review-only runtime status`: `REVIEW_ONLY_RUNTIME partial`
5. `DecisionResult review-only dashboard/API status`: `REVIEW_ONLY_RUNTIME partial`
6. `ExecutionPlan / BoundaryCandidate review-only runtime status`: `REVIEW_ONLY_RUNTIME partial`
7. `Review / Replay result status`: `REVIEW_ONLY_RUNTIME partial`
8. `Data Source Health dashboard/API status`: `REVIEW_ONLY_RUNTIME partial`
9. `RuleConfig runtime audit / rule explainability`: `REVIEW_ONLY_RUNTIME partial`
10. `Missed Opportunity / Review Archive status`: `REVIEW_ONLY_RUNTIME partial`
11. `RiskActionGuard read-only status`: `REVIEW_ONLY_RUNTIME partial`
12. `Alert fatigue / notification policy status`: `REVIEW_ONLY_RUNTIME partial`

All completed slices are review-only. None of them is Production Wiring（生产接线）, Push send（推送发送）, Candidate generation（候选生成）, Decision generation（决策生成）, Point generation（点位生成）, final direction / entry / stop / TP / RR（最终方向 / 入场 / 止损 / 止盈 / 盈亏比）, order/execution（订单 / 执行）, or auto-trading（自动交易）.

## 3. Candidate Next Slices Considered

| Candidate slice | Existing owner path / assets | User-visible value | Risk | Recommendation |
|---|---|---|---|---|
| Position Monitor manual-input / monitor status | Legacy PositionSync / monitor foundations exist, plus dashboard guardrail copy. | Could expose manual position observation status. | High execution-adjacent wording risk: close, reverse, open, move stop, monitor action, and auto-close boundaries need more isolation. | Not selected now. Keep frozen until a narrower source read can prove it is only manual-input status and not Position Monitor execution. |
| Internal Push preview / recheck status | Internal push preview dashboard copy and historical P302-P305 assets exist. | Could show preview/recheck boundary status. | Push and recheck are explicitly sensitive; external-channel and recheck-execution boundaries are easy to confuse after the just-closed alert policy slice. | Not selected now. Avoid back-to-back Push/recheck-adjacent work. |
| Candidate preview / ranking status | Candidate attention / preview / source-owned candidate skeleton history exists. | Could show candidate preview health. | Directly adjacent to Candidate generation and ranking; duplicate DTO/Validator/Assembler skeleton history is large after #830. | Not selected. Too likely to revive frozen Candidate/runtime wrapper surface. |
| Three AI / AI conflict status | `ai_role_results` and DecisionResult AI-role display fields exist. | Could show role-result availability. | Real provider orchestration, conflict arbitration, fallback, and final challenge chain are not authorized. | Not selected. Keep as read-only inventory only until provider boundaries are separately approved. |
| SourceTrace / RuntimeKline / DataQuality / MultiTimeframe aggregate | Existing dashboard detail surfaces already show SourceTrace and RuntimeKline diagnostics; `DefaultDashboardRuntimeKlineContextAdapter`, `PersistedOhlcvQueryService`, persisted OHLCV readiness DTOs, SourceTrace read-only display mapper, and historic DataQuality / MultiTimeframe source-binding assets exist. | High: user can see source / kline / data-quality readiness boundaries before any point or trading semantics. | Medium if it drifts into numeric point generation; low for source-read-only selection because current step only inventories existing owner paths. | Selected. It is the safest next source-read target with visible diagnostics and no need to start Push, Candidate, Point, or trading. |
| Account risk / system health / macro-news status | `SystemHealthService` and some account-risk/macro-news history exist. | Could show operational context. | Account risk is position/exposure adjacent; macro/news owner path appears less cohesive than SourceTrace/RuntimeKline. | Not selected now. May be considered after source/data-quality readiness is clarified. |
| Paper Observation / paper trading status | Dashboard display adapter traces exist. | Could show observation-only status. | The phrase paper trading can be mistaken for trading simulation/execution. | Not selected. Needs a stricter naming/source read before any runtime slice. |
| Runtime readiness / system guardrail status | Review-only readiness gate assets exist. | Could provide a broad guardrail summary. | "Readiness" can be misread as executable readiness; it overlaps with Push/Point gate history. | Not selected now. Too broad for the next minimal closure. |

## 4. Selected Next Slice

The selected next slice is `SourceTrace / RuntimeKline / DataQuality / MultiTimeframe aggregate review-only status`.

Why this slice now（为什么现在选它）:

- It has visible, existing dashboard diagnostic surfaces: SourceTrace read-only visibility, RuntimeKlineContext runtime diagnostics, persisted OHLCV readiness, missing-field summaries, and explicit non-trade copy.
- It has concrete owner-path assets to inventory before design: `DefaultDashboardRuntimeKlineContextAdapter`, `PersistedOhlcvQueryService`, `PersistedOhlcvQueryServiceImpl`, persisted OHLCV readiness result/status DTOs, `SourceTraceEntryReadOnlyDisplayMapper`, and dashboard detail rendering.
- It can be kept source-read-only first. The next package does not need Java edits, tests, dashboard edits, schema, config, pom, new DTO, Validator, Assembler, or Orchestrator.
- It is a natural safety step before any future Point, Candidate, Position Monitor, or Push work because it clarifies whether runtime market/source evidence is complete, partial, stale, missing, or fail-closed.
- It directly supports #830 by choosing an existing runtime/dashboard diagnostic path instead of adding another wrapper family.

The selected slice must not be interpreted as SourceTrace completion, RuntimeKline completion, DataQuality score completion, MultiTimeframe agreement, numeric point generation, candidate generation, executable readiness, or trading authorization.

## 5. Why Not The Others

- Push / Alert preview and internal Push/recheck preview remain blocked as next step because they sit next to external channel and recheck execution risk.
- Candidate preview / ranking is rejected because it can revive Candidate generation and the frozen source-owned candidate wrapper chain.
- Position Monitor is rejected for now because manual monitor status still risks becoming close/reverse/open/move-stop action guidance.
- Three AI is rejected because no real three-provider orchestration or conflict arbitration boundary is authorized.
- Account risk / system health / macro-news is rejected because account risk is exposure/action-adjacent and macro/news owner paths are less immediately visible than RuntimeKline/SourceTrace.
- Paper Observation / paper trading is rejected because the name itself can imply simulated trading behavior.
- Runtime readiness / system guardrail status is rejected because it is broader and easier to confuse with executable readiness.

## 6. Source Read Task Definition

Next task:

`Source Read for SourceTrace / RuntimeKline / DataQuality / MultiTimeframe aggregate review-only status`

Branch:

`source-trace-runtime-kline-data-quality-status-source-read`

Scope:

- read SourceTrace / RuntimeKline / DataQuality / MultiTimeframe owner assets;
- inventory dashboard/API surfaces already present;
- confirm status fields, fail-closed fields, manual-review fields, source health, freshness, missing-fields, stale-reason, and source-trace provenance assets;
- record whether future design can reuse existing owner paths without new DTO / Validator / Assembler / Orchestrator;
- record all boundaries against Point, Candidate, Push, Decision generation, final direction, entry / stop / TP / RR, order/execution, and auto-trading.

## 7. Forbidden Scope

This package and the next Source Read must not:

- modify Java business code;
- modify tests;
- modify dashboard business logic;
- modify schema/config/pom;
- add endpoint or panel behavior;
- trigger external API refresh, scheduler, collector, or API client refresh;
- send Push or use external channels;
- trigger recheck execution;
- generate Candidate, Decision, Point, final direction, entry, stop, TP, RR, order action, execution action, or auto-trading action;
- add DTO / Validator / Assembler / Orchestrator;
- continue P359 or P360;
- claim capability movement beyond `REVIEW_ONLY_RUNTIME partial`.

## 8. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 资产: Yes
- 是否减少重复: Yes, by selecting an existing dashboard/runtime diagnostic owner path instead of adding wrapper families.
- 是否提升 capability level: No, selection only.
- 是否接 service/runtime/dashboard/API: No, selection only; future source read will inspect existing service/runtime/dashboard/API assets.
- 是否符合 #830 审计建议: Yes
