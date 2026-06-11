# V1 RiskActionGuard Read-Only Status Source Read

## 1. Executive Summary

本包只做 source read（源码读取）和 source-of-truth（事实源）交接，不做 implementation（实现）。

结论：`RiskActionGuard read-only status` 适合作为下一条最小 `REVIEW_ONLY_RUNTIME partial` 设计对象。现有 owner path 已经存在：`DashboardController` 的 `/api/dashboard/detail` 通过 `RiskActionGuardDisplayAdapter` / `DefaultRiskActionGuardDisplayAdapter` 生成 `DashboardDetailResponseVO.RiskActionGuardDisplayVO`，dashboard 已有 `riskActionGuardPlaceholderCard`、`executionPlanRiskGuardValue`、Risk Reminder copy 和主工作台风险复核文案。

现有资产已经具备 fail-closed（失败即关闭）、manual review（人工复核）、not trade instruction（非交易指令）、action flags forced false（动作关闭）等安全语义；但 dedicated RiskActionGuard status endpoint 和独立完整 status panel 仍缺失。下一步只能进入 design，不得直接实现。

不需要新增 DTO / Validator / Assembler / Orchestrator。后续 design 应优先复用既有 `RiskActionGuardDisplayVO`、adapter、dashboard detail read model 和 dashboard DOM/copy。

本包不接 Push，不接 Candidate，不接 Decision generation，不接 Point，不生成 final direction / entry / stop / TP / RR，不接 order / execution / auto-trading，不执行 Position Monitor，不执行 replay / recheck，不继续 P359 / P360。

## 2. Source Read Inventory

| Area | Files/classes found | Existing behavior | Runtime/API connection | Dashboard connection | Gap |
|---|---|---|---|---|---|
| RiskActionGuard display adapter | `src/main/java/org/example/trademodel/service/dashboard/RiskActionGuardDisplayAdapter.java`, `DefaultRiskActionGuardDisplayAdapter.java` | Builds fail-closed dashboard RiskActionGuard state from DecisionResult, PlanBoundary display, ExecutionPlan display, and fallback display. Forces action flags false and manual review true. | Used by `DashboardController.dashboardDetail`. | Feeds `riskActionGuardDisplay` consumed by dashboard JS. | Dedicated compact RiskActionGuard status endpoint missing. |
| RiskActionGuard VO | `DashboardDetailResponseVO.RiskActionGuardDisplayVO` | Fields include `riskActionGuardStatus`, label, advice, blocking reason, liquidity state, stampede/wick flags, action flags, `manualRiskReviewRequired`, `notTradeInstruction`. Defaults are fail-closed. | Returned inside `/api/dashboard/detail`. | Dashboard cards and workbench consume it. | New design may need normalized status fields, but should use Map/existing VO instead of new DTO. |
| DashboardController | `DashboardController.dashboardDetail` | Builds safe default displays, reads decision, source trace, PlanBoundary, ExecutionPlan, RiskActionGuard, PaperObservation, and other status slices. | `/api/dashboard/detail?symbol=...` already exposes `riskActionGuardDisplay`. | Existing dashboard JS attaches `riskActionGuardDisplay`. | No `/api/dashboard/risk-action-guard-status` or equivalent dedicated status endpoint. |
| Dashboard template | `src/main/resources/templates/dashboard.html` | Has `riskActionGuardPlaceholderCard`, `executionPlanRiskGuardValue`, Risk Reminder copy, action-disabled copy, and main workbench focus text. | Uses dashboard detail response, not a dedicated status endpoint. | Visible as placeholder/detail copy, not a full slice status panel. | Dedicated `riskActionGuardStatusPanel` not present. Copy contains action wording that must stay clearly negative/manual-review. |
| Tests | `DefaultRiskActionGuardDisplayAdapterTest`, `DashboardDetailResponseVOTest`, `RuleEngineServiceSourceTraceTest`, `DashboardControllerTest`, `DefaultExecutionPlanDisplayAdapterTest`, P17/P18 fixture tests | Cover missing inputs, plan/execution not ready, high risk, missing liquidity, stampede, wick-only, action flags forced false, and no action methods. | Existing tests validate owner path semantics indirectly. | Dashboard tests use adapter stubs and display path. | No targeted test for a dedicated RiskActionGuard status endpoint/panel because that endpoint/panel does not exist yet. |
| Docs | `PHASE_BACKEND_P182...`, `PHASE_BACKEND_P187...`, `PHASE_RISK_ACTION_GUARD_DISPLAY_*`, `V1_BOUNDARYCANDIDATE_EXECUTIONPLAN_OWNER_SOURCE_READ.md`, selection doc after Missed Archive closure | Existing docs identify RiskActionGuard as display/adapter/fail-closed owner, not production risk action. | Docs record `/api/dashboard/detail` smoke and adapter test history. | Docs record Risk Reminder read-only display closure. | Need current V1 minimal runtime design doc for standalone status mapping. |
| Boundary consumers | `RuleEngineService`, `BoundaryCandidateServiceImpl`, `PlanService`, `DefaultExecutionPlanDisplayAdapter`, `DefaultPaperObservationDisplayAdapter` | Consume RiskActionGuard display as fail-closed blocker; unsafe action flags downgrade or block. | Runtime services exist, but this source-read does not invoke or change them. | Feeds downstream display behavior. | Design must avoid turning guard state into Position Monitor action or executable plan behavior. |

## 3. Existing Owner Path

```text
Completed review-only runtime slices
-> DashboardController /api/dashboard/detail
-> DashboardDetailResponseVO.withSafeDefaultDisplays()
-> PlanBoundaryDisplayAdapter
-> ExecutionPlanDisplayAdapter
-> RiskActionGuardDisplayAdapter / DefaultRiskActionGuardDisplayAdapter
-> DashboardDetailResponseVO.RiskActionGuardDisplayVO
-> dashboard.html riskActionGuardPlaceholderCard / executionPlanRiskGuardValue / workbench focus copy
-> future minimal RiskActionGuard read-only status design
```

Owner path assessment:

- `RiskActionGuardDisplayAdapter`: exists, runtime display adapter, review-only safe.
- `DefaultRiskActionGuardDisplayAdapter`: exists, fail-closed, action flags forced false.
- `RiskActionGuardDisplayVO`: exists, sufficient as current read model carrier.
- `/api/dashboard/detail`: exists and exposes `riskActionGuardDisplay`.
- Dedicated RiskActionGuard status endpoint: missing.
- Dedicated full RiskActionGuard status panel: partial / placeholder only.
- Review-only safety: exists, but future design must normalize wording to avoid action implication.

## 4. Reusable Assets

- `RiskActionGuardDisplayAdapter` and `DefaultRiskActionGuardDisplayAdapter`.
- `DashboardDetailResponseVO.RiskActionGuardDisplayVO`.
- `DashboardController.dashboardDetail` and existing safe default display chain.
- `dashboard.html`:
  - `riskActionGuardPlaceholderCard`
  - `executionPlanRiskGuardValue`
  - `executionPlanNotExecutableReasonValue`
  - Risk Reminder read-only copy
  - action-disabled copy
  - workbench focus text for RiskActionGuard review
- Tests:
  - `DefaultRiskActionGuardDisplayAdapterTest`
  - `DashboardDetailResponseVOTest`
  - `RuleEngineServiceSourceTraceTest`
  - `DashboardControllerTest`
  - `DefaultExecutionPlanDisplayAdapterTest`
  - P17 / P18 fixture tests
- Historical docs for RiskActionGuard display adapter, Risk Reminder display, and ExecutionPlan / BoundaryCandidate owner path.

## 5. Safety Semantics Found

Existing safety semantics are strong enough for design:

- missing decision -> `DECISION_MISSING`, fail-closed.
- PlanBoundary not valid -> `PLAN_BOUNDARY_NOT_VALID`, fail-closed.
- ExecutionPlan not `READY_REVIEW_ONLY` -> `EXECUTION_PLAN_NOT_READY`, fail-closed.
- missing liquidity -> `LIQUIDITY_CONTEXT_MISSING`, fail-closed.
- liquidity deterioration -> review-only manual review.
- stampede -> `STAMPEDE_REVIEW_ONLY`, action blocked.
- wick-only -> `WICK_ONLY_REVIEW_ONLY`, no trend reversal assumption.
- high risk -> manual review only.
- action flags are forced:
  - `opportunityPushAllowed=false`
  - `reverseTradeAllowed=false`
  - `newPositionAllowed=false`
  - `marketOrderExitAllowed=false`
- `manualRiskReviewRequired=true`.
- `notTradeInstruction=true`.
- Dashboard copy says Risk Reminder is read-only, not a trade instruction, does not connect order API, does not trigger auto-trading, and does not generate real points.

Gap: the current VO uses `notTradeInstruction`, not the newer status-slice field name `notTradingSignal`. Future design can map `notTradeInstruction=true` into `notTradingSignal=true` at a Map/status endpoint layer without adding a DTO.

## 6. Action Wording And Boundary Risks

RiskActionGuard naturally contains action-adjacent words. These are the main design risks:

- `riskActionAdvice` may mention reduce, move stop, lower leverage, high risk, liquidity deterioration, stampede, wick-only, reverse, open, or close.
- VO action flags include `opportunityPushAllowed`, `reverseTradeAllowed`, `newPositionAllowed`, and `marketOrderExitAllowed`.
- Dashboard copy includes "自动平仓 / 自动反手 / 自动改止损关闭" and "可考虑减仓 / 移动止损 / 降低杠杆"; these must remain negative/manual-review wording only.
- Boundary consumers use RiskActionGuard to downgrade ExecutionPlan / BoundaryCandidate and must not be treated as executable guard authorization.

Design must explicitly forbid:

- Position Monitor execution or action suggestion expansion.
- close / reverse / move stop / open / execute semantics as executable output.
- Candidate generation or ranking.
- Point generation or numeric point wiring.
- final direction / entry / stop / TP / RR.
- Push send or external channel.
- order / execution / auto-trading.

## 7. Current Gaps

- No dedicated RiskActionGuard read-only status endpoint exists.
- No dedicated full `riskActionGuardStatusPanel` exists.
- Current dashboard has a placeholder card and workbench/detail rendering, not a complete status-slice panel.
- `sourceTraceComplete`, `sourceHealth`, `notTradingSignal`, `notCandidateSignal`, `notDecisionGeneration`, `notPointSignal`, `notExecutable`, and `displaySlotsAreCandidatePool` are not first-class fields on `RiskActionGuardDisplayVO`; design should prefer a minimal Map/status mapping over a new DTO.
- No schema/table change is needed for the minimal status slice.
- No new Validator / Assembler / Orchestrator is justified.
- Position Monitor and action wording risks must be contained by copy and fail-closed status mapping.

## 8. Design Readiness / Go-NoGo

Decision: **GO: Minimal Review-Only RiskActionGuard Read-Only Status Runtime Wiring Design**.

Reason: existing owner path, adapter, VO, dashboard detail API, dashboard placeholder/copy, and targeted tests are sufficient to design a minimal review-only status mapping. The gaps are designable without adding new schema/config/pom, DTO, Validator, Assembler, Orchestrator, Push, Candidate, Decision generation, Point, or trading behavior.

Next allowed action: `Minimal Review-Only RiskActionGuard Read-Only Status Runtime Wiring Design`.

Next branch: `minimal-review-only-riskactionguard-read-only-status-runtime-wiring-design`.

The next package must remain design-only. It must define owner path, status mapping, dashboard/API minimal surface, fail-closed rules, action wording guardrails, test scope, and implementation readiness questions.

## 9. Rejected Expansion

The following are explicitly out of scope:

- RiskActionGuard implementation.
- Position Monitor execution or expansion.
- Push / external channel.
- Candidate generation.
- Decision generation.
- Point generation.
- final direction / entry / stop / TP / RR.
- order / execution / auto-trading.
- replay / recheck execution.
- new DTO / Validator / Assembler / Orchestrator.
- schema/config/pom changes.
- P359 / P360.

## 10. Freeze Rule Compliance

- 是否创建新骨架: No.
- 是否复用 Cursor-era 资产: Yes. Reuses existing dashboard display adapter / VO / dashboard detail owner path.
- 是否减少重复: Yes. Keeps the existing RiskActionGuard display owner path instead of creating another guard wrapper.
- 是否提升 capability level: No, source-read only.
- 是否接 service/runtime/dashboard/API: No, source-read only; it confirms the existing `/api/dashboard/detail` path.
- 是否符合 #830 审计建议: Yes.

## 11. Final Recommendation

可以进入 `Minimal Review-Only RiskActionGuard Read-Only Status Runtime Wiring Design`。下一步只设计，不实现；优先复用 `DefaultRiskActionGuardDisplayAdapter`、`RiskActionGuardDisplayVO`、`DashboardController` `/api/dashboard/detail` 和 dashboard placeholder/copy；明确 fail-closed、review-only、not trading、not candidate、not decision generation、not point、not executable 边界；禁止 Push、Candidate、Decision generation、Point、交易动作、Position Monitor execution、DTO / Validator / Assembler / Orchestrator、P359 / P360。
