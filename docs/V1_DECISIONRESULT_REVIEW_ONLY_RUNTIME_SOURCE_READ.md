# DecisionResult Review-Only Runtime Source Read

# 1. Executive Summary

DecisionResult review-only dashboard/API status **适合作为下一条最小 runtime slice**，但下一步只能进入 design，不能直接 implementation。

- 是否已有 DecisionResult owner path: Yes。`tm_decision_result` / `DecisionResult` / `DecisionResultMapper` / `DecisionService` / `DecisionServiceImpl` / `DecisionResultVO` 已经形成现有 decision read-model owner path。
- 是否已有 DecisionService: Yes。`DecisionService` 暴露 summary/detail 读路径，`DecisionServiceImpl` 读取最新 decision、按 symbol 读取最新 decision，并补充 quote / position / read-model fallback metadata。
- 是否已有 controller/API: Yes。`DashboardController` 已有 `/api/dashboard/summary` 和 `/api/dashboard/detail`，但没有 dedicated DecisionResult review-only status endpoint。
- 是否已有 dashboard 展示: Yes。`dashboard.html` 已展示 summary tiles、detail workbench、decision shells、AI role output、source trace、PlanBoundary、ExecutionPlan、RiskActionGuard、read-model fallback 等内容；但没有 dedicated `DecisionResult status` panel。
- 是否已有 tests: Yes。`DashboardControllerTest`、`DecisionServiceImplTest`、`DecisionResultMapperLatestPlanIntegrationTest`、dashboard display adapter tests 已覆盖 summary/detail、mapper join、read-model partial/full、source trace、manual review、not trade instruction 等。
- 是否已有 schema / mapper: Yes。`schema.sql` 有 `tm_decision_result`，`DecisionResultMapper` 有 insert、latest summary/detail joined queries、latest decision time、today count、reverse signal count 等读路径。
- 是否已有 review-only / fail-closed / not-trading-signal 边界: Partial。`DashboardDetailResponseVO.withSafeDefaultDisplays()`、SourceTrace / RuntimeKline / PlanBoundary / ExecutionPlan / RiskActionGuard display adapters 和 tests 有 fail-closed / manualReviewRequired / notTradeInstruction；`DecisionServiceImpl` 有 `readModelTruthStatus` / `readModelFallbackReason`。但 DecisionResult 本身还没有 dedicated `reviewOnly=true`、`notTradingSignal=true`、`notCandidateSignal=true`、`notDecisionGeneration=true`、`notPointSignal=true` status surface。
- 是否需要新增 DTO / Validator / Assembler: No。未来最小实现如果进入 readiness gate，优先用 existing `DecisionResultVO` / `DashboardSummaryResponseVO` / `DashboardDetailResponseVO` 或 Map。
- 是否会接 Push / Candidate / Decision generation / Point / Trading: No。本任务只做 source read；未来 slice 也必须保持 read-model status only。
- 下一步应该做什么: **Minimal Review-Only DecisionResult Runtime Wiring Design**。

# 2. Source Read Inventory

| Area | Files/classes found | Existing behavior | Runtime/API connection | Dashboard connection | Gap |
|---|---|---|---|---|---|
| DecisionResult owner path | `DecisionResult`, `DecisionResultVO`, `DecisionResultMapper`, `DecisionService`, `DecisionServiceImpl`, `tm_decision_result` | Reads persisted decision read model; joins latest execution plan and analysis metadata; enriches quote and position fields for display. | `/api/dashboard/summary` and `/api/dashboard/detail` consume `DecisionService`. | Summary list and detail workbench consume decision rows. | No dedicated DecisionResult review-only status mapping yet. |
| DecisionService | `DecisionService`, `DecisionServiceImpl` | `getLatestDecisionResults`, `getLatestDecisionResultBySymbol`, `getLightSystemStatus`, `countOpenPositions`; marks read-model `FULL` / `PARTIAL`. | Yes, through `DashboardController`. | Yes, dashboard receives model attrs and API response data. | Read-model fields can include action-looking text; needs explicit status boundary. |
| controller/API | `DashboardController` | `/dashboard`, `/api/dashboard/summary`, `/api/dashboard/detail`, `/api/dashboard/evidence-score-status`. | Existing summary/detail APIs expose decision data. | Existing dashboard fetches summary/detail. | No `/api/dashboard/decision-result-status` or equivalent dedicated status endpoint. |
| dashboard | `dashboard.html` | Renders decision shells, read model fallback, source trace, AI role results, PlanBoundary / ExecutionPlan / RiskActionGuard sections. | Fetches summary/detail and stores detail decision map. | Yes, user-visible decision detail exists. | No dedicated `decisionResultStatusPanel`; current display mixes decision, execution-plan, AI, and source trace context. |
| tests | `DashboardControllerTest`, `DecisionServiceImplTest`, `DecisionResultMapperLatestPlanIntegrationTest`, dashboard adapter tests | Covers decision core fields, summary/detail APIs, read-model fallback, source trace anchors, manual review and not trade instruction display defaults. | Yes, targeted tests cover existing API paths. | Static/API tests cover dashboard-related response fields. | No dedicated DecisionResult status endpoint/panel test. |
| schema / mapper | `schema.sql`, `DecisionResultMapper` | `tm_decision_result` stores decision fields; mapper joins `tm_execution_plan` and `tm_analysis_run`; reads latest by limit/symbol. | Yes. | Yes, summary/detail API fields are backed by mapper result. | Staleness/incomplete status not normalized as one status enum. |
| ai_role_results if present | `tm_decision_result.ai_role_results`, `DecisionResultVO.aiRoleResults`, dashboard `aiRoleRaw` rendering | AI role text can be displayed when present. | Exposed through existing decision APIs. | Dashboard AI shell and detail rendering use it. | Must stay read-only; not Three AI expansion or final arbiter. |
| review-only flags | `DashboardDetailResponseVO` safe defaults, `SourceTraceDTO`, display adapters, tests | Adjacent display owners force `manualReviewRequired=true` and `notTradeInstruction=true`. | Indirect via `/api/dashboard/detail`. | Visible through detail/workbench display. | DecisionResult status itself lacks explicit review-only flags. |
| fail-closed behavior | `withSafeDefaultDisplays`, `readModelTruthStatus`, `readModelFallbackReason`, SourceTrace missing fields | Missing read-model/source fields degrade to partial/incomplete/safe defaults. | Partial through detail API and adapters. | Dashboard shows fallback/missing status in adjacent sections. | Dedicated missing/stale/incomplete DecisionResult status absent. |
| source trace / provenance if present | `DashboardSourceTraceDetailAdapter`, `SourceTraceDTO`, tests | Uses `DecisionResultVO.decisionId`, `analysisId`, `symbol`, `createTime`, `timeframe`, quote metadata, data quality, multi-timeframe source labels. | Yes via `/api/dashboard/detail`. | Yes through SourceTrace detail UI. | Provenance exists but not summarized as DecisionResult runtime status. |

# 3. Existing Runtime Flow

```text
Watchlist / MarketQuote / Evidence / Score completed slices
  -> DecisionService / DecisionResult owner path
     [exists, runtime yes, dashboard visible yes via summary/detail, review-only safe partial]
  -> DecisionResultMapper / tm_decision_result / tm_execution_plan join / tm_analysis_run metadata
     [exists, runtime yes, dashboard visible through VO, review-only safe partial]
  -> /api/dashboard/summary and /api/dashboard/detail
     [exists, runtime API yes, dashboard visible yes, review-only safe partial]
  -> dashboard.html decision shells / workbench / source trace / display adapters
     [exists, dashboard visible yes, review-only safe partial]
  -> dedicated DecisionResult review-only status
     [missing, runtime status no, dashboard dedicated panel no, review-only status mapping missing]
```

Key boundary: existing DecisionResult rows are **read-model data**. They must not become Candidate generation, new Decision generation, Point generation, final direction, Push, order, execution, or auto-trading.

# 4. DecisionResult Readiness

- 是否能读取 DecisionResult 状态: Partial yes. Existing summary/detail APIs can read latest decisions and detail by symbol, but they do not return a normalized DecisionResult status.
- 是否能判断 DecisionResult 是否可用: Partial yes. `getLatestDecisionResultBySymbol` can return `null`; `DecisionServiceImpl` marks read-model `FULL` / `PARTIAL`; dashboard detail has safe defaults.
- 是否能判断缺失 / stale / incomplete: Missing/incomplete partial yes through null decision, `readModelTruthStatus=PARTIAL`, `readModelFallbackReason=LEGACY_MISSING:*`, SourceTrace missing fields. Stale is not a dedicated DecisionResult freshness status yet.
- 是否能判断 fail-closed: Partial yes through display adapters and safe defaults; no dedicated `DECISIONRESULT_*_FAIL_CLOSED` status surface yet.
- 是否能显示 `ai_role_results`: Yes. Schema, VO, and dashboard render path exist; this must remain read-only and not become Three AI expansion.
- 是否已有测试: Yes. Existing tests cover summary/detail, mapper latest plan join, read-model partial/full, source trace anchors, manual review, not trade instruction, and no point-source completion in source trace.
- 是否已有 dashboard DOM slot: Yes for decision shells / workbench / source trace; No for dedicated `DecisionResult status` panel.

# 5. Boundary Confirmation

- DecisionResult slice 不得生成 Candidate。
- DecisionResult slice 不得生成新的 Decision；只能读取已有 decision read model。
- DecisionResult slice 不得生成 Point。
- DecisionResult slice 不得生成 final direction。
- DecisionResult slice 不得发送 Push。
- DecisionResult slice 不得接外部通道。
- DecisionResult slice 不得接订单/执行。
- DecisionResult slice 不得绕过 Watchlist / MarketQuote / Evidence / Score 边界。
- DecisionResult slice 不得把 Display Slots 当候选池。
- `marketBiasHierarchy`、`tradeType`、`recommendedAction`、`entryZone`、`stopLoss`、`takeProfitRules`、`leverageSuggestion`、`positionSuggestion` 等字段只能作为 existing read-model context；不能作为新状态实现里的交易指令。

# 6. Candidate Slice Comparison / Go-NoGo

Decision: **A. GO: Minimal Review-Only DecisionResult Runtime Wiring Design**。

Reason:

- DecisionResult owner path 真实存在；
- `DecisionService`、mapper、schema、summary/detail API、dashboard display、tests 都存在；
- Dedicated review-only status endpoint / panel 缺失，但这正适合进入 design，而不是直接实现；
- 可以复用 existing owner path，不需要新增 DTO / Validator / Assembler；
- 必须先设计状态映射，避免把 existing read-model fields 误升级为 Candidate、Decision generation、Point 或 Trading signal。

Owner path 候选：

```text
Watchlist / MarketQuote / Evidence / Score completed slices
-> DecisionResultMapper / tm_decision_result
-> DecisionService / DecisionResultVO
-> DashboardController summary/detail API
-> dashboard decision shells / source trace / display adapters
-> future minimal review-only DecisionResult status
```

Dashboard/API 最小状态候选：

- latest decision exists / missing；
- symbol；
- decisionId / analysisId；
- createTime / age if available；
- readModelTruthStatus；
- readModelFallbackReason；
- sourceTrace status / missing fields summary；
- ai_role_results present / missing；
- reviewOnly = true；
- notTradingSignal = true；
- notCandidateSignal = true；
- notDecisionGeneration = true；
- notPointSignal = true；
- failClosed = true when missing / partial / stale / ambiguous。

# 7. Rejected Expansion

暂不做：

- Push external channel；
- Candidate generation；
- Decision generation；
- ExecutionPlan / BoundaryCandidate wiring；
- Point generation；
- order / execution / auto-trading；
- P359 / P360；
- Three AI expansion。

Reason: this source read only confirms whether DecisionResult can become the next review-only status slice. It does not authorize new decision production, AI orchestration, sendable output, point generation, or trading actions.

# 8. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, source read only
- 是否接 service/runtime/dashboard/API: No, source read only
- 是否符合 #830 审计建议: Yes
