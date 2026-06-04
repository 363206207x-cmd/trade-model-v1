# V1 BoundaryCandidate / ExecutionPlan Owner Source Read

## 1. Executive Summary

本次只读 source read 结论很直接：

- `BoundaryCandidate` owner 路径真实存在：`BoundaryCandidateService` / `BoundaryCandidateServiceImpl` / `BoundaryCandidateDTO` / `BoundaryEntryDTO` / `BoundaryStopDTO` / `BoundaryTakeProfitLevelDTO` / `BoundarySourceFieldsDTO` 已经形成 review-only boundary candidate 的 Cursor-era owner。
- `ExecutionPlan` owner 路径真实存在：`PlanService` / `PlanServiceImpl` / `ExecutionPlanDO` / `ExecutionPlanMapper` / `ExecutionPlanVO` / `tm_execution_plan` / `DefaultExecutionPlanDisplayAdapter` 已经形成 review-only execution plan 的 owner。
- `DecisionResult` 是 canonical decision read-model owner，不应该承担 BoundaryCandidate、ExecutionPlan、PointProposal 或 SourceOwned runtime candidate 的 owner 角色。它通过 mapper join 暴露 plan/read-model 字段，但不应该覆盖 plan/boundary owner。
- dashboard adapters 是 canonical display owner：`DashboardController` 通过 `DashboardSourceTraceDetailAdapter`、`PlanBoundaryDisplayAdapter`、`ExecutionPlanDisplayAdapter`、`RiskActionGuardDisplayAdapter` 生成 detail read model，而不是直接让新的 point/runtime wrapper 绕过展示边界。
- `NumericPointProposal` / `ReviewOnlyNumericPointProposal` / `SourceOwnedCandidateIntegrationRuntimeCandidate` 继续冻结。它们目前更像 Codex-era safety wrappers 或 skeleton families，不是 runtime/display canonical owner。
- 不允许恢复 P359 / P360。没有证据显示 P359 会减少重复；它更可能增加一个 runtime candidate wrapper。
- 下一步应进入 `Minimal merge design for BoundaryCandidate / ExecutionPlan owner + safety adapters`，明确如何把 Codex safety adapters 合并到现有 owner path，而不是继续造新 DTO / Validator / Assembler。

本任务不提升 capability level。PositionSync slice 仍是 `REVIEW_ONLY_RUNTIME partial`。本任务不能生成点位，只为后续 merge design 做 source read。

## 2. BoundaryCandidate Source Read

| Item | Found? | Role | Runtime/service connection | Dashboard/display connection | Safety fields | Gap |
|---|---|---|---|---|---|---|
| `BoundaryCandidateService` | Yes | Boundary candidate service interface. | Exposes overloads for explicit `SourceTraceDTO`, `RuntimeKlineContextDTO`, `DerivativesRiskContextDTO`, and RiskActionGuard display input. | Not a display class by itself. | Service contract is source-explicit. | No direct dashboard endpoint owner. |
| `BoundaryCandidateServiceImpl` | Yes | Canonical service implementation for evaluating boundary candidates. | Uses `DefaultSourceAssembler` for runtime source contexts; returns fallback on missing source trace, missing numeric sources, bad data quality, or RiskActionGuard block. | Indirect; dashboard adapters currently display boundary status separately. | Forces `manualReviewRequired=true`, `notTradeInstruction=true`; accumulates blocking reasons; returns `INCOMPLETE` / `WATCH_ONLY` when unsafe. | A `VALID` service result still must not be treated as executable output. |
| `BoundaryCandidateDTO` | Yes | Boundary candidate carrier. | Produced by service implementation. | Not directly displayed without adapter. | Fields include status, source fields, data quality, `manualReviewRequired`, `notTradeInstruction`, blocking reasons. | It contains `entry`, `stop`, and `takeProfitLevels`; those must stay review-only and source-owned. |
| `BoundaryEntryDTO` | Yes | Entry boundary source carrier. | Used by `BoundaryCandidateDTO.valid(...)` and service validation. | Not direct display owner. | Has numeric source type/value/timeframe/reason. | Numeric value fields can be misread if surfaced outside display adapters. |
| `BoundaryStopDTO` | Yes | Stop boundary source carrier. | Used by `BoundaryCandidateDTO.valid(...)` and service validation. | Not direct display owner. | Has numeric source type/value/timeframe/reason. | Same display risk as entry fields. |
| `BoundaryTakeProfitLevelDTO` | Yes | Take-profit level source carrier. | Used by candidate service and tests. | Not direct display owner. | Includes level, price, rr, source refs, partial/allocation ratios, reason. | Must not become executable TP/RR output. |
| `BoundarySourceFieldsDTO` | Yes | Source identity and evidence refs for boundary fields. | Required by service for a valid candidate. | Not direct display owner. | Tracks entry/stop/take-profit source field, RR rule, data source, data quality, evidence refs. | Needs merge design to map Codex source wrappers into this object or adapter path. |
| Factory/status methods | Partial | `BoundaryCandidateDTO.valid(...)` exists; service builds fallback DTOs by setters. `BoundaryStatusEnum` covers `VALID`, `INCOMPLETE`, `WATCH_ONLY`, `INVALID`. | Service creates fallback statuses based on blockers. | Display adapter treats unsafe/positive statuses carefully. | Missing/unsafe source states force fallback; tests cover incomplete/watch-only/valid. | No explicit `incomplete(...)` / `watchOnly(...)` / `invalid(...)` static factory on DTO. |
| `manualReviewRequired` | Yes | Safety flag on DTO and display VOs. | Service forces true. | Display adapters force true. | Blocks executable interpretation. | Must remain enforced if wrappers feed this path. |
| `notTradeInstruction` | Yes | Safety flag on DTO and display VOs. | Service forces true. | Display adapters force true. | Blocks instruction interpretation. | Must remain enforced if wrappers feed this path. |
| Blocking reasons | Yes | Explain incomplete/watch-only/fail-closed conditions. | Service collects missing source trace, missing boundary sources, RiskActionGuard, stampede, wick-only, liquidity, and unsafe action flags. | Display adapters inherit or produce reasons. | Fail-closed traceability exists. | Merge design should preserve reasons, not replace them with wrapper-only reasons. |
| `dataQualityScore` | Yes | Required candidate quality field. | Missing score blocks candidate. | Can be displayed through safe status/reason surfaces. | Prevents silent pass on missing quality. | Still needs canonical source mapping from upstream data-quality contexts. |
| Source trace fields | Yes | `SourceTraceDTO` and `BoundarySourceFieldsDTO` are key dependencies. | Service requires source trace completeness. | `DefaultDashboardSourceTraceDetailAdapter` / `DefaultPlanBoundarySourceTraceAdapter` expose read-only source trace status. | Runtime-kline-only visibility does not upgrade readiness. | SourceTrace owner remains a dependency, but not a reason to revive P359. |

Source evidence:

- `BoundaryCandidateServiceImplTest` covers missing source trace -> `INCOMPLETE`, watch-only fallback -> `WATCH_ONLY`, missing numeric source value -> `INCOMPLETE`, complete sources -> `VALID`, runtime-kline-only source trace does not upgrade validity, and RiskActionGuard stampede/liquidity failures -> `WATCH_ONLY`.
- `BoundaryCandidateServiceImpl` checks SourceTrace required boundary sources, boundary source fields, data quality score, RiskActionGuard blocked flags, stampede, wick-only risk, liquidity state, and unsafe action flags.

## 3. ExecutionPlan Source Read

| Item | Found? | Role | Runtime/service connection | Mapper/schema connection | Dashboard/display connection | Gap |
|---|---|---|---|---|---|---|
| `PlanService` | Yes | Execution plan service interface. | Generates `ExecutionPlanVO` from decision bundle, score, market environment, asset analysis, optional SourceTrace and RiskActionGuard display. | Not mapper itself. | Output can be consumed by display layers. | Service output still has advisory text fields that can be misread. |
| `PlanServiceImpl` | Yes | Canonical service implementation for review-only execution plan generation. | Defaults to observe/advisory, applies SourceTrace readiness and RiskActionGuard readiness. | Not persistence by itself. | Indirect via VO/mapper/dashboard adapters. | `leverageSuggestion="1-5x"` and `positionSuggestion` are legacy text risks; adapters must not turn them into instructions. |
| `ExecutionPlanDO` | Yes | Persistence entity for execution plan table. | Stores generated plan fields. | Maps to `tm_execution_plan`. | Joined into decision read model. | Field names `entry_zone`, `stop_loss`, `take_profit_rules`, leverage/position suggestion are potentially misleading unless display-gated. |
| `ExecutionPlanMapper` | Yes | MyBatis persistence owner. | Inserts/selects latest plan by analysis id. | Uses `tm_execution_plan`. | `DecisionResultMapper` joins latest plan. | Not a runtime safety gate. |
| `ExecutionPlanVO` | Yes | Service/read model VO. | Contains readiness status, source trace status, plan mode, safety flags, RiskActionGuard status. | Not direct schema object. | Dashboard display adapter consumes plan/boundary state. | Contains advisory fields and suggestion text; must remain behind review-only display. |
| `DefaultExecutionPlanDisplayAdapter` | Yes | Canonical dashboard execution plan display owner. | Uses decision, plan boundary display, optional SourceTrace, and RiskActionGuard display. | Not persistence. | Injected into `DashboardController.dashboardDetail`. | Should be the only future dashboard execution-plan surface. |
| `tm_execution_plan` | Yes | Schema table exists. | Stores execution plans. | `schema.sql` creates it. | Joined by `DecisionResultMapper`. | Persistence alone does not mean executable advice. |
| Misread risk | Yes | Legacy plan text can look actionable. | `PlanServiceImpl` sets default advisory fields. | `DecisionResultMapper` joins plan text into `DecisionResultVO`. | `DefaultExecutionPlanDisplayAdapter` intentionally maps only status/reason and guardrails. | Merge design must preserve display adapter boundary. |

Important source behavior:

- `DefaultExecutionPlanDisplayAdapter` states it maps only status/reason fields and never produces entry/stop/take-profit values.
- It returns `READY_REVIEW_ONLY` only when boundary is valid, SourceTrace required sources are present, RiskActionGuard is safe, and safety flags remain forced.
- It adds guardrail reasons including `EXECUTION_PLAN_REVIEW_ONLY_DISPLAY`, `EXECUTION_PLAN_NOT_EXECUTABLE`, `NOT_TRADE_INSTRUCTION`, and `ENTRY_STOP_TP_RR_NOT_GENERATED`.
- `DefaultExecutionPlanDisplayAdapterTest` covers missing boundary, incomplete boundary, watch-only boundary, sourceTrace missing, sourceTrace safe-fail-closed, complete sourceTrace -> `READY_REVIEW_ONLY`, runtime-kline-only sourceTrace not upgrading readiness, high risk/stampede fallback, and forced safety flags.

## 4. DecisionResult Source Read

`DecisionResult` / `DecisionResultVO` / `DecisionService` / `DecisionResultMapper` are the canonical decision read-model owner.

They should retain these roles:

- Latest decision list/detail read model for dashboard/API.
- Join point for analysis, decision, latest execution plan, data quality, latest quote, and open position annotations.
- Aggregated status surface for dashboard summary/detail.

They should not take these roles:

- Boundary candidate owner.
- Execution plan owner.
- Point proposal owner.
- Source-owned runtime candidate owner.
- Trading action owner.
- Final direction owner.

Important source observations:

- `DecisionResultMapper.findLatestDecisionResultsJoined(...)` and `findLatestDecisionResultBySymbolJoined(...)` join latest `tm_execution_plan` fields into `DecisionResultVO`.
- `DecisionServiceImpl` enriches decision read model with market quote and open position annotations, and marks fallback/read-model partial states.
- `DecisionResultVO` contains plan fields (`recommendedAction`, `planMode`, `entryZone`, `stopLoss`, `takeProfitRules`, `leverageSuggestion`, `positionSuggestion`) and position fields. That makes it a broad read model, not a safe canonical point owner.
- `DecisionResultMapper.countOpenSymbolsWithReverseSignal()` counts reverse-signal situations against open positions. That is a read/count query, not a reverse action.

Conclusion: DecisionResult should remain a dashboard/API read-model aggregator. It may carry plan text, but future merge work must route boundary/plan safety through `BoundaryCandidate` / `ExecutionPlan` / dashboard adapters rather than letting DecisionResult become a point proposal.

## 5. Dashboard Adapter Source Read

Dashboard display owner path exists and is already wired:

```text
DashboardController.dashboardDetail
  -> DashboardSourceTraceDetailAdapter
  -> PlanBoundaryDisplayAdapter
  -> ExecutionPlanDisplayAdapter
  -> RiskActionGuardDisplayAdapter
  -> PaperObservationDisplayAdapter
  -> DashboardDetailResponseVO
```

Observed adapters:

- `DefaultDashboardSourceTraceDetailAdapter`: builds read-only SourceTrace / RuntimeKline / derivatives-risk context. It exposes missing runtime kline context and missing boundary sources instead of fabricating values.
- `DefaultPlanBoundarySourceTraceAdapter`: fail-closed source trace readiness adapter for plan boundary display. It marks missing SourceTrace / BoundaryCandidateDTO / RuntimeKlineContextDTO instead of upgrading status.
- `DefaultPlanBoundaryDisplayAdapter`: fail-closed display adapter for PlanBoundary status. It maps safe status/reason fields only and never produces entry/stop/take-profit values.
- `DefaultExecutionPlanDisplayAdapter`: fail-closed display adapter for ExecutionPlan status. It maps only status/reason fields and never produces entry/stop/take-profit values.
- `DefaultRiskActionGuardDisplayAdapter`: fail-closed dashboard display adapter for risk action state. It forces opportunity push, reverse trade, new position, and market order exit flags to false.
- `DefaultDashboardRuntimeKlineContextAdapter`: builds unavailable or readiness-derived runtime kline context safely; manual review and not-trade flags remain true.

Dashboard adapters should be treated as canonical display owner. Codex wrappers, if ever used, must feed the existing owner path or be merged into it. They must not create parallel dashboard display paths.

Current display conflict risk:

- `DecisionResultVO` exposes plan text fields.
- `BoundaryCandidateDTO` can carry numeric-looking fields.
- Codex point/runtime wrappers also carry review-only candidate/status fields.
- If a future implementation bypasses `DefaultPlanBoundaryDisplayAdapter` / `DefaultExecutionPlanDisplayAdapter`, the same logical capability may appear through several inconsistent dashboard paths.

## 6. Owner Decision

| Capability | Owner decision | Evidence | Frozen / Adapter | Next action |
|---|---|---|---|---|
| Review-only boundary candidate | Keep `BoundaryCandidateService` / `BoundaryCandidateServiceImpl` / `BoundaryCandidateDTO` family as canonical owner. | Service + DTO + targeted tests exist; source trace, data quality, RiskActionGuard, blocking reasons, and safety flags are enforced. | `ReviewOnlyNumericPointProposal`, `SourceOwnedCandidateIntegrationRuntimeCandidate`, and source-owned integration wrappers stay frozen as non-owner wrappers. | Minimal merge design should map Codex safety evidence into this owner path or explicitly leave it frozen. |
| Review-only execution plan | Keep `PlanService` / `ExecutionPlanVO` / `ExecutionPlanDO` / `ExecutionPlanMapper` plus `DefaultExecutionPlanDisplayAdapter` as canonical owner. | Service, persistence, schema, mapper join, VO, and display adapter exist. Adapter is fail-closed and review-only. | Codex point proposal wrappers must not become execution plan owners. | Minimal merge design should protect legacy plan text from executable interpretation. |
| Decision read model | Keep `DecisionService` / `DecisionResultMapper` / `DecisionResultVO` as decision read-model owner only. | Joined query and service enrichment exist; dashboard summary/detail consume this read model. | It is not point proposal, boundary candidate, final direction, or trade-action owner. | Merge design should define which fields are read-only context vs owner output. |
| Dashboard display | Keep dashboard adapters and `DashboardDetailResponseVO` as display owner. | `DashboardController.dashboardDetail` injects and invokes source trace, plan boundary, execution plan, risk guard, and paper observation adapters. | New runtime candidate wrappers must not bypass adapters. | Future safe display work should feed adapters, not create new display path. |
| Numeric point proposal | Freeze. | `ReviewOnlyNumericPointProposalDTO`, `NumericPointSafetyValidator`, and assembler skeletons exist, but no service/dashboard runtime owner path is proven. | Safety adapter only if merge design proves reuse. | Do not expand until canonical owner mapping is designed. |
| Source-owned runtime candidate | Freeze. | `SourceOwnedCandidateIntegrationRuntimeCandidateDTO` and validator exist, but P359 assembler is unmerged and not counted. No service/dashboard runtime path. | Wrapper candidate, not canonical owner. | Keep P359/P360 frozen; do not revive by default. |

## 7. P359 / P360 Decision

- P359 remains frozen.
- P360 remains forbidden.
- P359 branch exists but was not merged; PR #829 was closed unmerged, so it does not count as progress.
- This source read found no evidence that P359 reduces duplication. The opposite risk remains: it would add a parallel `SourceOwnedCandidateIntegrationRuntimeCandidate` assembly path beside existing `BoundaryCandidate`, `ExecutionPlan`, `DecisionResult`, and dashboard adapter owners.
- Therefore P359/P360 must not be restored unless a later merge design proves that the code deletes/reuses wrapper surface and feeds canonical owners rather than adding another family.

## 8. Next Step Decision

Recommended option: **A. Minimal merge design for BoundaryCandidate / ExecutionPlan owner + safety adapters**.

Reason:

- BoundaryCandidate owner path is real.
- ExecutionPlan owner path is real.
- DecisionResult read-model owner is real.
- Dashboard display owner path is real.
- Source-owned runtime / numeric point wrappers are still frozen and should not expand.
- The project now needs a merge design that decides how existing Codex safety adapters either map into the Cursor-era owner path or stay frozen.

Not recommended:

- **B. Further targeted source read for SourceTrace / dashboard source trace owner**: useful later, but not the immediate blocker for owner merge design because SourceTrace dependency and display adapter boundaries are already visible enough for a design pass.
- **C. Next Minimal Runtime Slice Selection**: too early; there is still unresolved duplicate point/runtime skeleton risk.

Do not recommend P359, P360, new DTO, new Validator, new Assembler, Three AI, Position Monitor expansion, Push, point generation, order, execution, or auto-trading.

## 9. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No
- 是否接 service/runtime/dashboard/API: No, source read only
- 是否符合 #830 审计建议: Yes

## 10. Final Recommendation

明确结论：`BoundaryCandidateService` / `BoundaryCandidateDTO` 是 review-only boundary candidate owner；`PlanService` / `ExecutionPlanVO` / `ExecutionPlanMapper` / `DefaultExecutionPlanDisplayAdapter` 是 review-only execution plan owner；`DecisionResult` 只保留 decision read-model owner；dashboard adapters 是 display owner。`NumericPointProposal`、`ReviewOnlyPointProposal`、`SourceOwnedCandidateIntegrationRuntimeCandidate` 和 P359/P360 继续冻结。下一步做 `Minimal merge design for BoundaryCandidate / ExecutionPlan owner + safety adapters`，不是恢复 P359/P360。现在不能删除重复代码，因为 canonical merge path 还没设计稳定，贸然删除会把已有 read-model/display owner 和 Codex safety wrappers 的关系切断。
