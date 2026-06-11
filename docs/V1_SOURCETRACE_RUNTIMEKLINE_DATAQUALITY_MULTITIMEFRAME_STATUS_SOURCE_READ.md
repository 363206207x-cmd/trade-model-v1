# V1 SourceTrace / RuntimeKline / DataQuality / MultiTimeframe Status Source Read

## 1. Executive Summary

本包只做 Source Read（源码读取），不做实现。结论是：`SourceTrace / RuntimeKline / DataQuality / MultiTimeframe aggregate review-only status` 适合作为下一条最小 Review-Only Runtime partial（只读运行时部分完成）小闭环进入 Design（设计）。

当前可复用的 owner path（归属路径）已经存在：

- `GET /api/dashboard/detail` 通过 `DashboardController` 输出 `DashboardDetailResponseVO`；
- `DashboardDetailResponseVO` 已携带 `sourceTrace`、`runtimeKlineContext`、`derivativesRiskContext`；
- `DefaultDashboardSourceTraceDetailAdapter` 已生成只读、fail-closed 的 SourceTrace 诊断；
- `DefaultDashboardRuntimeKlineContextAdapter` 已读取 persisted OHLCV readiness（持久化 K线就绪状态）并生成 RuntimeKline 诊断；
- `PersistedOhlcvQueryServiceImpl` / `PersistedOhlcvBarMapper` 已提供 DB read-only（数据库只读）窗口读取；
- `RuntimeKlineContextAssemblyServiceImpl` 已把非 FRESH / 不安全 K线窗口 fail-closed；
- `dashboard.html` 已有 SourceTrace / RuntimeKline 诊断展示与 safety copy（安全文案）。

缺口也很明确：当前没有一个专门聚合 SourceTrace / RuntimeKline / DataQuality / MultiTimeframe 的 dashboard/API status endpoint 或 status panel。DataQuality 与 MultiTimeframe 主要来自 `DecisionResultVO` 的 `dataQualityScore` / `multiTfConvergence` 元数据，以及一些冻结的历史 source-binding skeleton；这些不能被误当成新的运行时 owner 或 Point/Candidate 能力。

下一允许动作是：`Minimal Review-Only SourceTrace / RuntimeKline / DataQuality / MultiTimeframe Aggregate Runtime Wiring Design`。下一步仍只能设计，不允许实现。

## 2. Source Read Inventory

| Area | Files/classes found | Existing behavior | Runtime/API/dashboard connection | Gap |
|---|---|---|---|---|
| SourceTrace dashboard detail owner | `src/main/java/org/example/trademodel/controller/DashboardController.java`, `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java`, `src/main/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapter.java` | Builds review-only SourceTrace metadata with `fallbackStatus=INCOMPLETE`, `reviewMode=REVIEW_ONLY`, `manualReviewRequired=true`, `notTradeInstruction=true`. | Existing `/api/dashboard/detail` returns `sourceTrace`; dashboard renders it in diagnostic cards. | No dedicated aggregate status endpoint/panel yet. |
| SourceTrace entry read-only seam | `SourceTraceEntryReadOnlyReviewController`, `SourceTraceEntryReadOnlyDisplayMapper`, `SourceTraceEntryReadOnlyCompletionAssembler`, related tests | Historical read-only seam for SourceTrace entry completion; explicitly unwired from readiness, dashboard mutation, order, automation, and external data. | Useful as prior safety evidence, not the aggregate dashboard owner path. | High duplicate-risk if treated as the active owner; keep context-only. |
| RuntimeKline dashboard detail owner | `DefaultDashboardRuntimeKlineContextAdapter`, `RuntimeKlineContextDTO`, `RuntimeKlineItemDTO`, `RuntimeKlineContextAssemblyServiceImpl` | Reads persisted OHLCV readiness when available. Non-FRESH, missing, stale, partial, invalid, or unsafe context fails closed. | Existing `/api/dashboard/detail` returns `runtimeKlineContext`; dashboard renders persisted OHLCV readiness and compact kline diagnostics. | `RuntimeKlineContextDTO` contains point-adjacent fields, so future status must omit executable field exposure. |
| Persisted OHLCV read model | `PersistedOhlcvQueryServiceImpl`, `PersistedOhlcvBarMapper`, `PersistedOhlcvReadinessResult`, `PersistedOhlcvReadinessStatus`, `PersistedOhlcvStaleReasonCode`, `tm_persisted_ohlcv_bar` | DB read-only latest closed window query; readiness can be `FRESH`, `STALE`, `PARTIAL`, `MISSING`, `UNKNOWN`, or `INVALID`. | Used by `DefaultDashboardRuntimeKlineContextAdapter` when present. | Do not modify schema; no external refresh or ingestion trigger is allowed. |
| DataQuality signals | `SourceTraceDTO.dataQualityScore`, `DecisionResultVO.dataQualityScore`, dashboard source-trace rendering/tests | Existing dashboard detail can surface `dataQualityScore` as metadata from DecisionResult. | Partial runtime metadata in existing dashboard detail. | No unified DataQuality status mapping; source-binding skeletons remain frozen/history-only. |
| MultiTimeframe signals | `SourceTraceDTO.multiTimeframeSource`, `DecisionResultVO.multiTfConvergence`, dashboard source-trace rendering/tests | Existing dashboard detail can surface multi-timeframe convergence metadata from DecisionResult. | Partial runtime metadata in existing dashboard detail. | No 5m / 15m / 1h / 4h aggregate status owner found in active runtime path. |
| Dashboard diagnostics | `src/main/resources/templates/dashboard.html` | Existing `runtimeKlineContextCard`, `renderSourceTraceVisibility`, `renderRuntimeKlineContextVisibility`, `runtimeKlineReadinessLabel`, and source-trace copy emphasize review-only / not trade instruction / not entry source. | Visible diagnostic area exists in dashboard detail path. | No dedicated aggregate status panel or DOM id for the selected slice yet. |
| Tests | `DashboardControllerTest`, `DefaultDashboardSourceTraceDetailAdapterTest`, `DefaultDashboardRuntimeKlineContextAdapterTest`, `RuntimeKlineContextAssemblyServiceImplTest`, `PersistedOhlcvQueryServiceImplTest` | Tests cover SourceTrace incompleteness, RuntimeKline fail-closed behavior, readiness metadata, and no entry/stop/TP/RR population in unsafe states. | Existing targeted tests can guide future implementation test scope. | Future design must require endpoint/panel safety tests if implementation proceeds. |
| Scheduler / collector / API refresh boundary | `PushRecheckScheduler`, `AnalysisSchedulerService`, `RealMarketDataFetcherService`, dashboard auto-refresh JS | External refresh / scheduler paths exist elsewhere in the project. | They are not part of the safe SourceTrace / RuntimeKline read-only owner path. | Future design must forbid triggering these paths. |
| Frozen source-binding skeletons | `RuntimeKlineContextSourceBindingDTO`, `DataQualityContextSourceBindingDTO`, `MultiTimeframeContextSourceBindingDTO`, validators/assemblers/tests under point/source-binding packages | Historical #830/P3xx source-binding skeleton family. | Not active dashboard/API owner path. | Do not extend or revive without explicit ownership proof. |

## 3. Existing Read-Only Runtime Flow

```text
Dashboard detail request
  -> DashboardController#getDetail (/api/dashboard/detail)
     exists / runtime yes / dashboard visible yes / review-only safe partial

  -> DashboardDetailResponseVO.sourceTrace
     exists / runtime metadata yes / dashboard visible yes / review-only safe partial

  -> DefaultDashboardSourceTraceDetailAdapter
     exists / runtime adapter yes / dashboard visible through detail / fail-closed yes

  -> DefaultDashboardRuntimeKlineContextAdapter
     exists / runtime adapter yes / dashboard visible through detail / fail-closed yes

  -> PersistedOhlcvQueryServiceImpl + PersistedOhlcvBarMapper
     exists / DB read-only yes / no external refresh / no scheduler trigger

  -> RuntimeKlineContextAssemblyServiceImpl
     exists / FRESH-only safe compact kline metadata / unsafe windows fail-closed

  -> dashboard.html SourceTrace / RuntimeKline diagnostic cards
     exists / dashboard visible yes / review-only copy present

  -> Dedicated aggregate status endpoint/panel
     missing / design required / no implementation in this package
```

## 4. Readiness Signals Found

| Signal | Existing source | What it can safely mean | Risk |
|---|---|---|---|
| Persisted OHLCV readiness | `PersistedOhlcvReadinessStatus` | Kline read model can be fresh/stale/partial/missing/unknown/invalid. | FRESH must not imply entry / stop / TP / RR readiness. |
| Stale reason | `PersistedOhlcvStaleReasonCode` | Explains missing/partial/stale/invalid kline windows. | Do not trigger refresh to fix it. |
| SourceTrace fallback | `SourceTraceDTO.fallbackStatus`, `missingFields`, `blockingReasons` | Explains incomplete SourceTrace and missing boundary inputs. | Do not turn missing fields into generated sources. |
| RuntimeKline fallback | `RuntimeKlineContextDTO.fallbackStatus`, `readinessStatus`, `staleReasonCode` | Explains why RuntimeKline remains unavailable or partial. | Point-adjacent DTO fields must remain omitted/negative-only in future status. |
| DataQuality metadata | `DecisionResultVO.dataQualityScore` -> `SourceTraceDTO.dataQualityScore` | Review-only diagnostic metadata. | Not a completed data-quality scoring capability. |
| MultiTimeframe metadata | `DecisionResultVO.multiTfConvergence` -> `SourceTraceDTO.multiTimeframeSource` | Review-only convergence metadata. | Not a 5m/15m/1h/4h aggregate owner. |

## 5. Dashboard / API Evidence

- API: `/api/dashboard/detail` already returns `sourceTrace` and `runtimeKlineContext`.
- Dashboard: `runtimeKlineContextCard` and render functions show SourceTrace / RuntimeKline visibility diagnostics.
- Copy: dashboard text says SourceTrace is read-only, not a trade instruction, requires manual review, and `INCOMPLETE` / `BLOCKED` do not mean a trading opportunity.
- Copy: RuntimeKline readiness is described as persisted OHLCV diagnostic metadata only; `FRESH` does not complete SourceTrace.
- Copy: latest price and kline items are not entry / stop / TP / RR sources.

This is enough to design a minimal status layer, but not enough to claim a new completed runtime slice yet.

## 6. Gaps

- No dedicated aggregate status endpoint exists for `SourceTrace / RuntimeKline / DataQuality / MultiTimeframe`.
- No dedicated dashboard status panel / DOM ids exist for this selected slice.
- DataQuality and MultiTimeframe are partial metadata, not full owner paths.
- Existing source-binding DTO / Validator / Assembler families are frozen duplicate-risk assets.
- External refresh / scheduler / collector / API client paths exist elsewhere and must be explicitly excluded.
- `RuntimeKlineContextDTO` includes point-adjacent fields, so future status must avoid exposing executable field names as positive output.

## 7. Boundary Confirmation

This source-read package does not:

- implement an endpoint or dashboard panel;
- trigger scheduler / collector / API client refresh;
- generate Candidate;
- generate Decision;
- generate Point;
- output final direction;
- output entry / stop / TP / RR;
- send Push;
- use external channels;
- connect order / execution / auto-trading;
- add DTO / Validator / Assembler / Orchestrator;
- revive P359 / P360.

Future design must keep these boundaries explicit.

## 8. Go / No-Go

Result: **GO to Design**.

Reason:

- Existing owner path is real and already dashboard-visible through `/api/dashboard/detail`.
- Existing adapters and tests already encode review-only / fail-closed behavior.
- A minimal future status surface can be designed as a thin read-only aggregate over existing owner assets.
- No schema/config/pom, DTO/Validator/Assembler/Orchestrator, external refresh, Candidate, Decision generation, Point, Push, or trading path is needed for design.

Next allowed action:

- `Minimal Review-Only SourceTrace / RuntimeKline / DataQuality / MultiTimeframe Aggregate Runtime Wiring Design`

Next branch:

- `minimal-review-only-sourcetrace-runtimekline-dataquality-multitimeframe-status-runtime-wiring-design`

Risk:

- `A` for design docs/source-of-truth only.

## 9. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / existing assets: Yes
- 是否减少重复: Yes, by selecting the existing dashboard detail owner path instead of frozen source-binding wrappers
- 是否提升 capability level: No, source read only
- 是否接 service/runtime/dashboard/API: No implementation; source read confirms existing owner path
- 是否符合 #830 审计建议: Yes
