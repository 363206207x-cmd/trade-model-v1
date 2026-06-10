# V1 ExecutionPlan / BoundaryCandidate Runtime Wiring Implementation Readiness Gate

## 1. Executive Summary

本任务是 implementation readiness gate，不是 implementation。

结论：**GO 到 Minimal Review-Only ExecutionPlan / BoundaryCandidate Runtime Wiring Implementation**。

GO 的原因是 #873-#876 已经完成 DecisionResult review-only dashboard/API 小闭环，#872-#874 之后的 source-read / design 又确认 ExecutionPlan / BoundaryCandidate 的现有 owner path 足够清楚：`DashboardController` 的 `/api/dashboard/detail`、`DefaultPlanBoundaryDisplayAdapter`、`DefaultExecutionPlanDisplayAdapter`、`DashboardSourceTraceDetailAdapter`、`RiskActionGuardDisplayAdapter`、`BoundaryCandidateService`、`PlanService`、`ExecutionPlanVO/DO/Mapper` 和 `tm_execution_plan` 都已经存在。

最小 implementation 允许做的事情很窄：复用 `/api/dashboard/detail`，或在确有必要时新增一个 compact read-only status endpoint；复用现有 display adapters / VO 字段；在 `dashboard.html` 只加最小 status/copy/DOM；添加 targeted controller/dashboard/display tests；更新 source-of-truth docs。

最小 implementation 禁止新增 DTO / Validator / Assembler / Orchestrator，禁止改 schema/config/pom，禁止使用 `/api/plan/generate` 做 status endpoint，禁止接 Push / Candidate / Decision generation / Point / Trading，禁止输出 final direction、entry / stop / TP / RR、position size、leverage、order action，禁止继续 P359/P360。

当前 capability level 不提升，仍是 `REVIEW_ONLY_RUNTIME partial`。下一允许动作是 `Minimal Review-Only ExecutionPlan / BoundaryCandidate Runtime Wiring Implementation`。

## 2. Readiness Gate Result

**A. GO: Minimal Review-Only ExecutionPlan / BoundaryCandidate Runtime Wiring Implementation**

Implementation 仍必须是 review-only，并且必须复用 existing owner path：

```text
Watchlist / MarketQuote / Evidence-Score / DecisionResult completed slices
  -> DecisionService / DecisionResult read model
  -> DashboardController /api/dashboard/detail
  -> DashboardSourceTraceDetailAdapter
  -> DefaultPlanBoundaryDisplayAdapter / PlanBoundaryDisplayVO
  -> DefaultExecutionPlanDisplayAdapter / ExecutionPlanDisplayVO
  -> dashboard PlanBoundary / ExecutionPlan review-only display status
```

Underlying owner assets remain:

- `BoundaryCandidateService` / `BoundaryCandidateDTO` / `BoundaryStatusEnum`
- `PlanService` / `ExecutionPlanVO` / `ExecutionPlanDO` / `ExecutionPlanMapper`
- `tm_execution_plan`
- `DashboardDetailResponseVO.PlanBoundaryDisplayVO`
- `DashboardDetailResponseVO.ExecutionPlanDisplayVO`

GO guardrails:

- 不得新增 DTO / Validator / Assembler / Orchestrator。
- 不得改 schema/config/pom。
- 不得调用 `/api/plan/generate` 作为 status endpoint，因为它是 generation semantics。
- 不得接 Push / Candidate / Decision generation / Point / Trading。
- 不得让 `BoundaryCandidate VALID` 或 `READY_REVIEW_ONLY` 被解释成可执行交易动作。

## 3. Required Future Implementation Boundary

如果进入 implementation，未来最小实现只允许：

- 新增或复用一个最小 ExecutionPlan / BoundaryCandidate review-only status endpoint。
- 优先复用 `/api/dashboard/detail?symbol=BTCUSDT`；若需要 compact status surface，最多新增一个类似 `/api/dashboard/execution-plan-boundary-status?symbol=BTCUSDT` 的 GET endpoint。
- 新增或复用一个最小 dashboard status panel，可优先复用 `planBoundaryPlaceholderCard`。
- 显示 `status` / `symbol` / `analysisId` / `planBoundaryStatus` / `executionPlanStatus` / `sourceTraceStatus` / `sourceHealth` / `riskActionGuardStatus` / `notExecutableReason` / `incompleteReasons` / `blockingReasons` / `failClosed`。
- 明确 `reviewOnly=true`。
- 明确 `notTradingSignal=true`。
- 明确 `notCandidateSignal=true`。
- 明确 `notDecisionGeneration=true`。
- 明确 `notPointSignal=true`。
- 明确 `notExecutable=true`。
- 明确 `watchlistBounded=true`。
- 明确 `marketQuoteChecked=true`。
- 明确 `evidenceScoreChecked=true`。
- 明确 `decisionResultChecked=true`。
- 明确 `displaySlotsAreCandidatePool=false`。

未来 implementation 不允许：

- 生成 Candidate。
- 生成新的 Decision。
- 生成 Point。
- 生成 final direction。
- 输出 entry / stop / TP / RR。
- 输出 position size。
- 输出 leverage。
- 接 Push external channel。
- 接 order / execution / auto-trading。
- 新增 DTO / Validator / Assembler / Orchestrator。
- 修改 schema/config/pom。
- 继续 P359/P360。

## 4. Status Mapping Readiness

| Status | Existing asset can judge? | Source | Gap | Implementation allowed? | Fail-closed? |
|---|---:|---|---|---:|---:|
| `EXECUTIONPLAN_BOUNDARY_REVIEW_ONLY_READY` | Yes | `PlanBoundaryDisplayVO`, `ExecutionPlanDisplayVO`, `DefaultExecutionPlanDisplayAdapter`, `DashboardController.dashboardDetail` | Needs compact surface copy so ready cannot mean executable. | Yes | No, but still not executable |
| `PLAN_BOUNDARY_BACKEND_PENDING_FAIL_CLOSED` | Yes | Safe defaults from `DashboardDetailResponseVO.withSafeDefaultDisplays()` and `DefaultPlanBoundaryDisplayAdapter` | Needs normalized status in endpoint/panel. | Yes | Yes |
| `PLAN_BOUNDARY_INCOMPLETE_FAIL_CLOSED` | Yes | `PlanBoundaryDisplayVO.incompleteReasons`, SourceTrace adapter, DecisionResult read-model partial checks | Needs clear dashboard/API copy. | Yes | Yes |
| `PLAN_BOUNDARY_WATCH_ONLY` | Yes | `PlanBoundaryDisplayVO.planBoundaryStatus`, `BoundaryStatusEnum.WATCH_ONLY`, display adapter reasons | Must stay manual-review only. | Yes | Yes |
| `EXECUTIONPLAN_BOUNDARY_PENDING_FAIL_CLOSED` | Yes | `ExecutionPlanDisplayVO.executionPlanStatus`, `notExecutableReason` | Needs compact status mapping if endpoint added. | Yes | Yes |
| `EXECUTIONPLAN_SOURCE_TRACE_PARTIAL` | Yes | `SourceTraceDTO`, `DashboardSourceTraceDetailAdapter`, `ExecutionPlanDisplayVO.incompleteReasons` | Source-health label is partial and may need derived copy. | Yes | Yes |
| `EXECUTIONPLAN_RISK_GUARD_BLOCKED_FAIL_CLOSED` | Yes | `RiskActionGuardDisplayVO`, `DefaultExecutionPlanDisplayAdapter.resolveRiskActionGuardReason` behavior | Needs display that blocked means no Push/order/execution. | Yes | Yes |
| `EXECUTIONPLAN_BOUNDARY_BLOCKED_FAIL_CLOSED` | Yes | Unknown/unsafe status fallback in display adapters and guardrail reasons | Must catch ambiguity and forbidden executable terms. | Yes | Yes |

`READY_REVIEW_ONLY` and `BoundaryCandidate VALID` are implementable only as read-only display states. They must not become final direction, point readiness, order intent, or Production Wiring.

## 5. Existing Asset Readiness

| Asset | Exists? | Reusable? | Needs new DTO? | Needs schema change? | Risk | Decision |
|---|---:|---:|---:|---:|---|---|
| `DashboardController` `/api/dashboard/detail` | Yes | Yes | No | No | Broad detail response may be heavier than compact status. | Reuse first; add compact GET only if needed. |
| `PlanBoundaryDisplayVO` | Yes | Yes | No | No | Status labels need normalized safety copy. | Reuse. |
| `ExecutionPlanDisplayVO` | Yes | Yes | No | No | `READY_REVIEW_ONLY` can be misread without safety labels. | Reuse with explicit not-executable fields. |
| `DefaultPlanBoundaryDisplayAdapter` | Yes | Yes | No | No | `VALID` must remain unsafe unless display adapter degrades/annotates it. | Reuse as owner path. |
| `DefaultExecutionPlanDisplayAdapter` | Yes | Yes | No | No | Near entry/stop/take-profit language; must expose status only. | Reuse as owner path. |
| `DashboardSourceTraceDetailAdapter` | Yes | Yes | No | No | Source-health may need derived status label. | Reuse. |
| `RiskActionGuardDisplayAdapter` | Yes | Yes | No | No | Must not authorize action flags. | Reuse and keep blocked/fail-closed. |
| `BoundaryCandidateService` / `BoundaryCandidateDTO` | Yes | Yes | No | No | DTO contains numeric-looking fields; do not expose as action values. | Reuse only as underlying owner, not as new status DTO. |
| `PlanService` / `ExecutionPlanVO` | Yes | Yes | No | No | `/api/plan/generate` has generation semantics; do not call for status. | Reuse existing read/display data, not generation endpoint. |
| `ExecutionPlanMapper` / `tm_execution_plan` | Yes | Yes | No | No | Legacy columns have actionable names. | Reuse via existing read model only; no schema change. |
| Dashboard template/assets | Yes | Yes | No | No | Existing `planBoundaryPlaceholderCard` says placeholder. | Safe slot exists; allow minimal copy/status update only. |
| Tests | Yes | Yes | No | No | No dedicated compact endpoint/panel tests yet. | Add targeted tests only in implementation. |
| Source trace/provenance | Yes | Yes | No | No | Completeness can be partial. | Use fail-closed mapping. |
| Fail-closed flags | Yes | Yes | No | No | Must stay visible. | Preserve. |
| Review-only flags | Yes | Yes | No | No | Must be normalized for status endpoint/panel. | Preserve and expose explicitly. |

## 6. Test Readiness

Future implementation must include minimal targeted coverage for:

- controller/API smoke test for the status surface, either `/api/dashboard/detail` reuse assertions or the new compact endpoint;
- dashboard template/model/status panel label test if `dashboard.html` is touched;
- status mapping test for all allowed status values;
- missing DecisionResult / missing PlanBoundary fail-closed test;
- SourceTrace partial test;
- RiskActionGuard blocked test;
- forbidden semantics test or grep check;
- no Push / Candidate / Decision generation / Point / Trading check;
- no DTO / Validator / Assembler check.

Implementation checks must include compile, test-compile, targeted controller/dashboard/display tests, `git diff --check`, forbidden path check, and forbidden semantics grep.

## 7. Boundary With Existing Completed Slices

Future implementation must not bypass:

- Watchlist / RuleConfig boundary.
- MarketQuote freshness/fallback/source-health boundary.
- Evidence / Score review-only status boundary.
- DecisionResult review-only read-model boundary.
- Display Slots boundary.
- SourceTrace / source-health boundary.
- RiskActionGuard fail-closed boundary.
- Ambiguity must fail closed.

If any upstream state is missing, stale, fallback ambiguous, partial, blocked, or unsafe, the ExecutionPlan / BoundaryCandidate status must remain fail-closed and not executable.

## 8. Explicit No-Overreach Confirmation

- 是否接 Push：No
- 是否接 external channel：No
- 是否生成 Candidate：No
- 是否生成新的 Decision：No
- 是否生成 Point：No
- 是否生成 final direction：No
- 是否输出 entry / stop / TP / RR：No
- 是否输出 position size / leverage action：No
- 是否接 order / execution / auto-trading：No
- 是否继续 P359/P360：No
- 是否新增 DTO / Validator / Assembler：No
- 是否改 schema/config/pom：No
- 是否提升 capability level：No, readiness only

## 9. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes. This gate keeps the future implementation on the existing BoundaryCandidate / ExecutionPlan / dashboard display owner path instead of adding a new wrapper family.
- 是否提升 capability level: No, readiness gate only.
- 是否接 service/runtime/dashboard/API: No, readiness only.
- 是否符合 #830 审计建议: Yes

## 10. Final Recommendation

明确结论：**GO 到 Minimal Review-Only ExecutionPlan / BoundaryCandidate Runtime Wiring Implementation**。

下一步 implementation 的最小边界是：复用 existing dashboard/detail/display adapters and owner assets；最多新增一个 compact read-only status endpoint；最多新增一个 minimal dashboard status/copy/DOM update；添加 targeted tests；更新 source-of-truth。合并前必须通过 workflow contract、compile、test-compile、targeted tests、diff checks、forbidden path check、forbidden semantics grep，并证明没有 DTO / Validator / Assembler、schema/config/pom、Push、Candidate、Decision generation、Point、final direction、entry / stop / TP / RR、order/execution/auto-trading、P359/P360 越界。

它仍是 `REVIEW_ONLY_RUNTIME partial`，因为它只允许展示只读状态，不是 Production Wiring，不是 Push，不是 Candidate generation，不是 Decision generation，不是 Point generation，也不是 Trading。
