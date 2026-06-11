# V1 Minimal Review-Only SourceTrace / RuntimeKline / DataQuality / MultiTimeframe Status Runtime Wiring Design

## 1. Executive Summary

本包只做 Design（设计），不做 Implementation（实现）。设计结论是：`SourceTrace / RuntimeKline / DataQuality / MultiTimeframe aggregate review-only status` 可以进入下一步 Implementation readiness gate（实现前就绪门），但未来实现必须保持最小、只读、fail-closed（失败关闭），并优先复用现有 `/api/dashboard/detail` owner path（归属路径）。

最小 owner path（归属路径）应以现有 `DashboardController` / `DashboardDetailResponseVO` / `DefaultDashboardSourceTraceDetailAdapter` / `DefaultDashboardRuntimeKlineContextAdapter` / `PersistedOhlcvQueryServiceImpl` / `RuntimeKlineContextAssemblyServiceImpl` 为数据来源。未来是否需要 dedicated read-only status endpoint（独立只读状态接口）由 readiness gate 决定；如果需要，最多只能新增一个 thin Map endpoint（薄 Map 接口），例如 `GET /api/dashboard/source-runtime-data-quality-status?symbol=BTCUSDT`，并且只聚合现有只读 owner data。

本设计不需要新增 DTO / Validator / Assembler / Orchestrator，不需要 schema/config/pom，不触发 scheduler / collector / API client refresh，不生成 Candidate / Decision generation / Point / final direction / entry / stop / TP / RR，也不连接 Push / external channel / order / execution / auto-trading。

下一允许动作：`Implementation readiness gate for SourceTrace / RuntimeKline / DataQuality / MultiTimeframe aggregate review-only status`。

## 2. Existing Assets / Owner Path

| Asset | Existing role | Design use | Boundary |
|---|---|---|---|
| `DashboardController` | Existing `/api/dashboard/detail` owner. | Reuse as primary dashboard/API read owner. | No mutation, generation, refresh, or trading side effect. |
| `DashboardDetailResponseVO` | Carries `sourceTrace`, `runtimeKlineContext`, `derivativesRiskContext`, and adjacent display objects. | Use as existing detail payload source for status mapping. | Do not add a new DTO family in design. |
| `DefaultDashboardSourceTraceDetailAdapter` | Builds review-only SourceTrace diagnostic metadata. | SourceTrace readiness, missing fields, fallback status, manual-review copy. | Missing SourceTrace must fail closed; no generated source binding. |
| `DefaultDashboardRuntimeKlineContextAdapter` | Reads persisted OHLCV readiness and returns RuntimeKline diagnostics. | RuntimeKline readiness and persisted OHLCV status source. | No external refresh or live provider call. |
| `PersistedOhlcvQueryServiceImpl` / `PersistedOhlcvBarMapper` | DB read-only latest closed window path. | Persisted OHLCV readiness source. | No schema change and no ingestion trigger. |
| `RuntimeKlineContextAssemblyServiceImpl` | Converts persisted OHLCV readiness to safe runtime kline context. | Fail-closed behavior for stale / partial / missing / invalid windows. | Point-adjacent fields cannot become positive executable fields. |
| `dashboard.html` diagnostics | Existing SourceTrace / RuntimeKline diagnostic cards and safety copy. | Future status panel may reuse the visual language and copy. | No dashboard trading business logic. |
| Existing tests | Dashboard, SourceTrace, RuntimeKline, OHLCV readiness tests. | Future readiness gate must require targeted endpoint/dashboard tests. | Design package does not edit tests. |

## 3. Owner Path Decision

Preferred future implementation path:

```text
GET /api/dashboard/detail
  -> DashboardController
  -> DashboardDetailResponseVO.sourceTrace
  -> DashboardDetailResponseVO.runtimeKlineContext
  -> DefaultDashboardSourceTraceDetailAdapter
  -> DefaultDashboardRuntimeKlineContextAdapter
  -> PersistedOhlcvQueryServiceImpl / PersistedOhlcvBarMapper
  -> dashboard.html existing diagnostic surfaces
```

Dedicated endpoint decision:

- Default design preference: reuse `/api/dashboard/detail` as source owner.
- Dedicated endpoint is allowed only if readiness gate confirms dashboard/API ergonomics need a compact status rollup.
- If allowed, endpoint must be a minimal read-only `Map<String, Object>` endpoint.
- Endpoint must not create DTO / Validator / Assembler / Orchestrator.
- Endpoint must not call scheduler, collector, external provider, API client refresh, or any write path.
- Endpoint must not expose executable field families as positive output.

Suggested future endpoint candidate:

```text
GET /api/dashboard/source-runtime-data-quality-status?symbol=BTCUSDT
```

Suggested future dashboard panel candidate:

```text
sourceRuntimeDataQualityStatusPanel
```

Suggested future DOM ids:

- `sourceRuntimeStatusValue`
- `sourceTraceReadinessValue`
- `runtimeKlineReadinessValue`
- `persistedOhlcvReadinessValue`
- `dataQualityStatusValue`
- `multiTimeframeStatusValue`
- `sourceRuntimeRefreshBoundaryValue`
- `sourceRuntimeSignalBoundaryValue`
- `sourceRuntimeReasonValue`

## 4. Read-Only Status Sources

| Area | Read-only source | Allowed status meaning | Not allowed |
|---|---|---|---|
| SourceTrace | `SourceTraceDTO.fallbackStatus`, `missingFields`, `blockingReasons`, `reviewMode`, `manualReviewRequired`, `notTradeInstruction` | Whether SourceTrace metadata is ready, partial, missing, or blocked. | Generating source binding, point input, final direction, or candidate evidence. |
| RuntimeKlineContext | `RuntimeKlineContextDTO.readinessStatus`, `fallbackStatus`, `staleReasonCode`, safe compact kline metadata | Whether runtime kline context is readable, missing, stale, or partial. | Treating latest price / close / kline values as entry / stop / TP / RR. |
| Persisted OHLCV | `PersistedOhlcvReadinessStatus`, `PersistedOhlcvStaleReasonCode` | Whether stored OHLCV data is fresh, stale, partial, missing, unknown, or invalid. | Triggering ingestion, refresh, live provider reads, scheduler, or collector. |
| DataQuality | `DecisionResultVO.dataQualityScore` through `SourceTraceDTO.dataQualityScore` | Partial diagnostic metadata only. | Claiming a completed DataQuality scoring capability or using frozen source-binding skeletons. |
| MultiTimeframe | `DecisionResultVO.multiTfConvergence` through `SourceTraceDTO.multiTimeframeSource` | Partial timeframe alignment / conflict / missing metadata only. | Producing direction, point, candidate rank, or 5m/15m/1h/4h trading conclusion. |

## 5. Status Mapping

| Status | Input signal | Fail-closed? | Display meaning |
|---|---|---:|---|
| `SOURCE_RUNTIME_STATUS_REVIEW_ONLY_READY` | SourceTrace and RuntimeKline are both present enough to render review-only diagnostics. | No | The aggregate status is readable for manual review only. |
| `SOURCE_TRACE_MISSING_FAIL_CLOSED` | `sourceTrace` missing, null, or cannot be trusted. | Yes | SourceTrace is unavailable; no downstream interpretation is allowed. |
| `SOURCE_TRACE_PARTIAL_REVIEW_ONLY` | SourceTrace exists with missing fields / blocking reasons / `INCOMPLETE`. | No | Partial SourceTrace metadata is visible, but remains manual-review-only. |
| `RUNTIME_KLINE_CONTEXT_READY_REVIEW_ONLY` | RuntimeKline context exists and readiness indicates safe diagnostic rendering. | No | RuntimeKline diagnostics are readable, not executable. |
| `RUNTIME_KLINE_CONTEXT_MISSING_FAIL_CLOSED` | RuntimeKline context missing, null, or unavailable. | Yes | RuntimeKline status cannot be trusted; fail closed. |
| `PERSISTED_OHLCV_READY_REVIEW_ONLY` | Persisted OHLCV readiness is `FRESH`. | No | Stored OHLCV window is fresh enough for diagnostics only. |
| `PERSISTED_OHLCV_STALE_REVIEW_ONLY` | Persisted OHLCV readiness is `STALE`, `PARTIAL`, `UNKNOWN`, or `INVALID`. | No | OHLCV is not fully fresh; show stale reason without refresh. |
| `PERSISTED_OHLCV_MISSING_FAIL_CLOSED` | No persisted OHLCV window or no service result. | Yes | OHLCV readiness is missing; no generated fallback. |
| `DATA_QUALITY_PARTIAL_REVIEW_ONLY` | `dataQualityScore` exists as metadata. | No | DataQuality is partial diagnostic metadata only. |
| `DATA_QUALITY_BLOCKED_FAIL_CLOSED` | DataQuality metadata is absent while required for aggregate confidence. | Yes | Do not infer data quality; show blocked / unknown. |
| `MULTITIMEFRAME_ALIGNMENT_REVIEW_ONLY` | MultiTimeframe convergence metadata indicates alignment. | No | Alignment is visible as metadata only. |
| `MULTITIMEFRAME_CONFLICT_REVIEW_ONLY` | MultiTimeframe metadata indicates conflict / divergence. | No | Conflict is visible as warning metadata only. |
| `MULTITIMEFRAME_MISSING_FAIL_CLOSED` | MultiTimeframe metadata missing when required for aggregate interpretation. | Yes | Do not infer timeframe agreement. |
| `REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED` | Status evaluation would require scheduler / collector / API client / external refresh. | Yes | Refresh is blocked; status remains read-only. |
| `GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Status evaluation would require Candidate / Decision generation / Point / source-binding generation. | Yes | Generation is blocked; display only existing owner data. |

## 6. Safety Fields

Future status output must include the safety fields below when a dedicated endpoint is approved:

| Field | Required value | Purpose |
|---|---:|---|
| `reviewOnly` | `true` | The surface is manual review only. |
| `notCandidateSignal` | `true` | It is not Candidate generation or ranking. |
| `notDecisionGeneration` | `true` | It does not generate a new decision. |
| `notPointSignal` | `true` | It does not generate points. |
| `notFinalDirection` | `true` | It does not output direction. |
| `notEntryStopTpRr` | `true` | It does not output entry / stop / TP / RR. |
| `notTradingSignal` | `true` | It is not a trading signal. |
| `notExecutable` | `true` | It cannot be executed. |
| `notSchedulerTrigger` | `true` | It does not trigger schedulers. |
| `notCollectorTrigger` | `true` | It does not trigger collectors. |
| `notApiClientRefresh` | `true` | It does not call API-client refresh. |
| `notExternalRefresh` | `true` | It does not refresh external data. |
| `notSourceBindingGeneration` | `true` | It does not generate source-binding objects. |
| `displaySlotsAreCandidatePool` | `false` | Dashboard display slots are not a candidate pool. |

## 7. Fail-Closed Rules

- Missing `sourceTrace` => `SOURCE_TRACE_MISSING_FAIL_CLOSED`.
- SourceTrace present but incomplete => `SOURCE_TRACE_PARTIAL_REVIEW_ONLY`.
- Missing `runtimeKlineContext` => `RUNTIME_KLINE_CONTEXT_MISSING_FAIL_CLOSED`.
- RuntimeKline present but non-fresh / stale / partial / unknown => read-only degraded state, never refresh.
- Missing persisted OHLCV readiness => `PERSISTED_OHLCV_MISSING_FAIL_CLOSED`.
- Stale persisted OHLCV readiness => `PERSISTED_OHLCV_STALE_REVIEW_ONLY`.
- Missing DataQuality metadata => `DATA_QUALITY_BLOCKED_FAIL_CLOSED` if the aggregate would otherwise imply quality confidence.
- DataQuality metadata present => `DATA_QUALITY_PARTIAL_REVIEW_ONLY`; it is not a completed score capability.
- Missing MultiTimeframe metadata => `MULTITIMEFRAME_MISSING_FAIL_CLOSED` if the aggregate would otherwise imply timeframe agreement.
- MultiTimeframe conflict => `MULTITIMEFRAME_CONFLICT_REVIEW_ONLY`; it is warning metadata, not direction.
- Any need for scheduler / collector / API client / external refresh => `REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED`.
- Any need for Candidate / Decision generation / Point / source-binding generation => `GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED`.

## 8. Refresh / Generation Boundary

Future implementation must not:

- trigger `PushRecheckScheduler`, `AnalysisSchedulerService`, `MarketDataScheduler`, `RealMarketDataFetcherService`, collector, provider, external API, or dashboard-triggered refresh;
- revive frozen source-binding skeleton families as runtime owner paths;
- create or update SourceTrace / RuntimeKline / DataQuality / MultiTimeframe data;
- generate Candidate, ranking, Decision, Point, final direction, entry, stop, TP, RR, order action, execution action, or auto-trading action;
- send Push or call external channel;
- execute Position Monitor, replay, or recheck;
- use dashboard display slots as a candidate pool.

The status surface may only say what existing read-only owner assets currently know.

## 9. Frozen Source-Binding Boundary

The following families are duplicate-risk / history-only for this slice:

- `RuntimeKlineContextSourceBindingDTO` and related validators / assemblers;
- `DataQualityContextSourceBindingDTO` and related validators / assemblers;
- `MultiTimeframeContextSourceBindingDTO` and related validators / assemblers;
- `SourceTraceNumericSourceContextDTO` / numeric source read-model skeletons;
- `SourceOwnedCandidateIntegration*` runtime/source-binding skeletons.

They may be mentioned as historical evidence, but future implementation must not import them, extend them, or treat them as the active owner path. The active owner path is the dashboard detail read path and its existing adapters.

## 10. Dashboard / API Design

Future readiness gate should decide between:

1. **Reuse only `/api/dashboard/detail`**: no dedicated endpoint; dashboard copy consolidates the existing SourceTrace / RuntimeKline diagnostics.
2. **Add one minimal read-only Map endpoint**: one small status rollup over existing detail/adapters, with no DTO family.

If option 2 is allowed, the endpoint should return only:

- `status`
- `symbol`
- `sourceTraceAvailable`
- `runtimeKlineContextAvailable`
- `persistedOhlcvReadiness`
- `dataQualityAvailable`
- `multiTimeframeAvailable`
- `sourceHealth`
- `reason`
- `message`
- required safety fields listed in Section 6
- `failClosed`

It must not return:

- candidate ranking;
- final direction;
- entry / stop / TP / RR;
- position size;
- leverage;
- order action;
- execution action;
- push send state;
- refresh action.

## 11. Implementation Readiness Gate Checklist

The next readiness gate must verify:

- whether `/api/dashboard/detail` alone can satisfy the user-visible status need;
- whether a dedicated Map endpoint is necessary and still smaller than dashboard-detail-only reuse;
- whether `DashboardController` can assemble the status without service ownership expansion;
- whether `dashboard.html` can show a minimal panel without business logic;
- whether tests can cover status mapping, safety fields, fail-closed, missing SourceTrace, missing RuntimeKline, persisted OHLCV stale/missing, DataQuality partial, MultiTimeframe conflict/missing, forbidden field absence, and no refresh/generation;
- whether no DTO / Validator / Assembler / Orchestrator is required;
- whether no schema/config/pom is required;
- whether no scheduler / collector / API client / external refresh is required;
- whether frozen source-binding skeletons remain unused.

## 12. Allowed Future Implementation Files

If the readiness gate returns GO, future B-risk implementation may only consider:

- `src/main/java/org/example/trademodel/controller/DashboardController.java` for a minimal read-only `Map` endpoint if needed;
- `src/main/resources/templates/dashboard.html` for a minimal status panel / copy / DOM if needed;
- `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java` for targeted endpoint/dashboard tests;
- the implementation report document;
- source-of-truth documents.

Future implementation must not add DTO / Validator / Assembler / Orchestrator, service/domain ownership family, mapper/schema/config/pom, scheduler, collector, provider, external API client, Push, Candidate, Decision generation, Point, order, execution, or auto-trading.

## 13. Final Recommendation

Design result: **GO to Implementation readiness gate**.

Reason:

- The existing owner path is real, read-only, and dashboard-visible.
- SourceTrace and RuntimeKline already have adapters with review-only / fail-closed behavior.
- Persisted OHLCV readiness already has a DB read-only path.
- DataQuality and MultiTimeframe can be shown only as partial metadata, not as completed owner paths.
- Frozen source-binding skeletons can be explicitly excluded, reducing duplicate surface.

Next allowed action:

- `Implementation readiness gate for SourceTrace / RuntimeKline / DataQuality / MultiTimeframe aggregate review-only status`

Next branch:

- `sourcetrace-runtimekline-dataquality-multitimeframe-status-implementation-readiness-gate`

Risk:

- `A` for readiness-gate docs/source-of-truth only.

## 14. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / existing assets: Yes
- 是否减少重复: Yes, by selecting `/api/dashboard/detail` and existing adapters instead of frozen source-binding wrappers
- 是否提升 capability level: No, design only
- 是否接 service/runtime/dashboard/API: No implementation; design selects existing owner path
- 是否符合 #830 审计建议: Yes
