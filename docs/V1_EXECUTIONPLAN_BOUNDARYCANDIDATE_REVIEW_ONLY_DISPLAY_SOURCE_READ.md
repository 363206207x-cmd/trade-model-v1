# V1 ExecutionPlan / BoundaryCandidate Review-Only Display Source Read

## 1. Executive Summary

- ExecutionPlan / BoundaryCandidate review-only display continuation 适合作为下一条最小 runtime slice 的 source-read 目标。
- Existing owner path 已存在：`BoundaryCandidateService` / `BoundaryCandidateServiceImpl` / `BoundaryCandidateDTO` 是 BoundaryCandidate owner path；`PlanService` / `PlanServiceImpl` / `ExecutionPlanVO` / `ExecutionPlanDO` / `ExecutionPlanMapper` / `tm_execution_plan` 是 ExecutionPlan owner path；`DashboardController` / `/api/dashboard/detail` / `DefaultPlanBoundaryDisplayAdapter` / `DefaultExecutionPlanDisplayAdapter` 是 dashboard detail display owner path。
- Existing dashboard 展示已存在但仍是 partial：`dashboard.html` 有 `planBoundaryPlaceholderCard` 和 workbench 的 PlanBoundary / ExecutionPlan render path，可见状态、SourceTrace、Backend Connection、ExecutionPlan 状态、Boundary 对齐、不可执行原因与安全文案。
- Dedicated review-only status endpoint / panel 仍缺失。`PlanController` 目前的 `/api/plan/generate` 是生成语义，不适合作为未来只读 status endpoint。
- Existing tests 已存在：`BoundaryCandidateServiceImplTest`、`PlanServiceImplTest`、`DefaultPlanBoundaryDisplayAdapterTest`、`DefaultExecutionPlanDisplayAdapterTest`、`DashboardDetailResponseVOTest`、`DashboardControllerTest` 等可作为后续 design/readiness 的 targeted test 基础。
- 不需要新增 DTO / Validator / Assembler。后续如实现，必须复用 existing owner path，并可用 Map / existing VO 做最小只读 status surface。
- 本 source-read 不接 Push / Candidate generation / Decision generation / Point / Trading，不生成 entry / stop / TP / RR，不生成 final direction。
- 下一步应该进入：`Minimal Review-Only ExecutionPlan / BoundaryCandidate Runtime Wiring Design`，只能设计，不实现。

## 2. Source Read Inventory

| Area | Files/classes found | Existing behavior | Runtime/API connection | Dashboard connection | Gap |
|---|---|---|---|---|---|
| BoundaryCandidate owner path | `BoundaryCandidateService`, `BoundaryCandidateServiceImpl`, `BoundaryCandidateDTO`, `BoundaryEntryDTO`, `BoundaryStopDTO`, `BoundaryTakeProfitLevelDTO`, `BoundaryStatusEnum` | Evaluates boundary candidate shape, SourceTrace completeness, boundary source fields, data quality, and RiskActionGuard blockers; outputs `INCOMPLETE`, `WATCH_ONLY`, or `VALID` with `manualReviewRequired=true` and `notTradeInstruction=true`. | Runtime service exists, but it is not exposed as a dedicated review-only status endpoint. | Indirectly visible through PlanBoundary display adapters and `/api/dashboard/detail`. | `VALID` can be misread as executable unless future status surface explicitly says review-only and not point / not trading. |
| ExecutionPlan owner path | `PlanService`, `PlanServiceImpl`, `ExecutionPlanVO`, `ExecutionPlanDO`, `ExecutionPlanMapper`, `tm_execution_plan` | Builds advisory plans, applies SourceTrace / RiskActionGuard readiness, keeps manual review and not-trade flags. Mapper stores/selects latest plan by analysis id. | `/api/plan/generate` exists but has generation semantics; `/api/dashboard/detail` joins decision/plan display fields through dashboard owner path. | `DefaultExecutionPlanDisplayAdapter` and dashboard workbench display status/reasons. | Need a future safe status endpoint or reuse of dashboard detail that does not call `/api/plan/generate`. |
| Dashboard detail API | `DashboardController.dashboardDetail`, `/api/dashboard/detail` | Builds safe default displays, reads DecisionResult, SourceTrace detail, PlanBoundary display, ExecutionPlan display, RiskActionGuard, PaperObservation, Market mini, Evidence / Score top items. | Existing read path is available and review-only safe when used as detail/status source. | Dashboard JavaScript consumes `planBoundaryDisplay` and `executionPlanDisplay`. | Dedicated compact status endpoint / panel is missing. |
| Dashboard template | `dashboard.html`, `planBoundaryPlaceholderCard`, `renderDisplayStatusCards`, `renderStructuredWorkbench` | Shows PlanBoundary status, SourceTrace, Backend Connection, ExecutionPlan status, Boundary alignment, incomplete reason, and safety copy. | Browser consumes `/api/dashboard/detail`. | Visible as placeholder/status card plus workbench status summary. | Existing copy still says placeholder; future design must decide whether to keep placeholder or add compact review-only status panel. |
| Tests | `BoundaryCandidateServiceImplTest`, `PlanServiceImplTest`, `DefaultPlanBoundaryDisplayAdapterTest`, `DefaultExecutionPlanDisplayAdapterTest`, `DashboardDetailResponseVOTest`, `DashboardControllerTest`, P17/P18 fixture tests | Cover missing SourceTrace, watch-only fallback, missing numeric sources, RiskActionGuard blockers, adapter fail-closed behavior, manual review, not-trade instruction, and no frozen wrapper dependency. | Good targeted coverage for owner path and display adapters. | Existing dashboard/controller tests can be extended later if dashboard status surface is added. | No dedicated ExecutionPlan / BoundaryCandidate status endpoint/panel test yet. |
| Schema / mapper | `schema.sql`, `tm_execution_plan`, `ExecutionPlanMapper`, `DecisionResultMapper` join fields | Execution plan persistence exists with plan mode and legacy text fields. DecisionResult mapper joins latest execution plan into detail read model. | Persistence/read path exists. | Dashboard uses joined detail fields and display adapters. | Legacy field names (`entry_zone`, `stop_loss`, `take_profit_rules`) are sensitive and must not be exposed as actionable values in status surface. |
| Review-only flags | `manualReviewRequired`, `notTradeInstruction`, `EXECUTION_PLAN_REVIEW_ONLY_DISPLAY`, `EXECUTION_PLAN_NOT_EXECUTABLE`, `ENTRY_STOP_TP_RR_NOT_GENERATED` | Owner path and display adapters already force manual review / not trade instruction and add review-only reasons. | Available through display VO and service outputs. | Dashboard copy already says non-trading / manual review. | Future status mapping must normalize flags into explicit fields such as `reviewOnly`, `notTradingSignal`, `notPointSignal`, `notExecutable`. |
| Fail-closed behavior | `DefaultPlanBoundaryDisplayAdapter`, `DefaultExecutionPlanDisplayAdapter`, `DefaultPlanBoundarySourceTraceAdapter`, `BoundaryCandidateServiceImpl`, `PlanServiceImpl` | Missing decision/analysis/source trace/risk context keeps display `BACKEND_PENDING`, `INCOMPLETE`, or `WATCH_ONLY`; ambiguity does not become executable. | Existing display path is fail-closed. | Visible via incomplete reasons / blocking reasons / not executable reason. | Dedicated status values are not yet mapped. |
| Source trace / provenance | `SourceTraceDTO`, `DashboardSourceTraceDetailAdapter`, `DefaultPlanBoundarySourceTraceAdapter`, `DefaultExecutionPlanDisplayAdapter` | SourceTrace drives PlanBoundary / ExecutionPlan readiness and missing-field reasons. | Existing dashboard detail path already reads SourceTrace context when available. | SourceTrace visibility is rendered in dashboard cards/workbench. | Source trace completeness needs a compact future status field. |

## 3. Existing Runtime Flow

```text
Watchlist / MarketQuote / Evidence-Score / DecisionResult completed slices
  -> DecisionService / DecisionResult read model
     (exists; runtime read; dashboard visible; review-only safe)
  -> DashboardController /api/dashboard/detail
     (exists; runtime read API; dashboard visible; review-only safe)
  -> DashboardSourceTraceDetailAdapter
     (exists; partial source trace / runtime context; dashboard visible; review-only safe)
  -> DefaultPlanBoundaryDisplayAdapter / DefaultPlanBoundarySourceTraceAdapter
     (exists; partial PlanBoundary display; dashboard visible; fail-closed)
  -> DefaultExecutionPlanDisplayAdapter
     (exists; partial ExecutionPlan display; dashboard visible; fail-closed)
  -> dashboard.html planBoundaryPlaceholderCard / structured workbench
     (exists; visible; review-only safe, but still placeholder/partial)
```

Additional owner path:

```text
BoundaryCandidateService / BoundaryCandidateDTO
  (exists; runtime service; not directly exposed as safe status API; review-only flags enforced)

PlanService / ExecutionPlanVO / ExecutionPlanMapper / tm_execution_plan
  (exists; runtime/persistence owner; /api/plan/generate has generation semantics and is not a safe status endpoint)
```

## 4. ExecutionPlan / BoundaryCandidate Readiness

- 是否能读取 BoundaryCandidate 状态：partial。`BoundaryCandidateService` can evaluate status, and dashboard display adapters can represent PlanBoundary status, but no dedicated read-only status endpoint currently exposes it.
- 是否能读取 ExecutionPlan 状态：partial/yes。`/api/dashboard/detail` exposes `executionPlanDisplay`, and `DefaultExecutionPlanDisplayAdapter` maps status/reason fields safely.
- 是否能判断缺失 / incomplete / watch-only / fail-closed：yes through existing adapters and tests. Missing DecisionResult, analysis id, source trace, boundary source fields, or RiskActionGuard context degrades to `BACKEND_PENDING`, `INCOMPLETE`, or `WATCH_ONLY`.
- 是否能显示 source trace / source health：partial. SourceTrace detail exists and is displayed, but a compact source-health/status field for this slice is not yet dedicated.
- 是否已有测试：yes for services, display adapters, VO defaults, dashboard/controller paths; missing dedicated status endpoint/panel tests.
- 是否已有 dashboard DOM slot：yes. `planBoundaryPlaceholderCard` and workbench status summary already exist.
- 是否可形成最小 review-only runtime 小闭环：yes, after design/readiness, by reusing dashboard detail/display adapters or adding one minimal read-only status endpoint and minimal dashboard status/copy.

## 5. Boundary Confirmation

- ExecutionPlan / BoundaryCandidate slice 不得生成 Candidate。
- 不得生成新的 Decision。
- 不得生成 Point。
- 不得生成 final direction。
- 不得输出 entry / stop / TP / RR 数值。
- 不得发送 Push。
- 不得接 external channel。
- 不得接 order / execution / auto-trading。
- 不得绕过 Watchlist / MarketQuote / Evidence-Score / DecisionResult 已完成边界。
- 不得把 Display Slots 当候选池。
- `BoundaryCandidate VALID` 只能作为人工复核状态，不等于交易动作、不等于 Point readiness、不等于 final direction。
- `/api/plan/generate` 有生成语义，未来最小 status surface 不应复用它作为只读状态 endpoint。

## 6. Candidate Slice Comparison / Go-NoGo

Decision: **A. GO: Minimal Review-Only ExecutionPlan / BoundaryCandidate Runtime Wiring Design**

GO reasons:

- Existing owner path 足够真实：BoundaryCandidate service/DTO、PlanService/ExecutionPlan、mapper/schema、dashboard detail/display adapters 和 tests 都存在。
- User-visible value 清楚：DecisionResult 之后，用户自然需要知道 PlanBoundary / ExecutionPlan 是否已有只读状态、是否 fail-closed、是否仍非交易。
- Risk 可控但必须 design first：该 slice 靠近 legacy `entry_zone` / `stop_loss` / `take_profit_rules` / readiness 语义，不能直接 implementation。
- 不需要新 DTO / Validator / Assembler。未来 design 应优先复用 existing VO / Map / dashboard detail display models。

Next step:

```text
Minimal Review-Only ExecutionPlan / BoundaryCandidate Runtime Wiring Design
```

Next step must remain design only. It must define owner path, status mapping, fail-closed rules, dashboard/API surface, tests, and forbidden semantics before any implementation readiness gate.

## 7. Rejected Expansion

Temporarily rejected:

- Push external channel
- Candidate generation
- Decision generation
- Point generation
- ExecutionPlan actionable plan output
- real entry / stop / TP / RR values
- final direction
- order / execution / auto-trading
- P359 / P360
- Three AI expansion

These are rejected because current goal is only review-only display continuation over existing owner paths.

## 8. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes. This source-read chooses existing BoundaryCandidate / ExecutionPlan / dashboard display owner paths instead of creating another wrapper family.
- 是否提升 capability level: No, source read only.
- 是否接 service/runtime/dashboard/API: No, source read only. It identifies existing paths and future design candidates.
- 是否符合 #830 审计建议: Yes. It avoids new duplicate skeletons, keeps P359/P360 frozen, and prefers existing owner assets.
