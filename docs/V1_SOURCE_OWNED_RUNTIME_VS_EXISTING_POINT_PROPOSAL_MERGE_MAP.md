# V1 Source-Owned Runtime vs Existing Point Proposal Merge Map

This document is a merge / ownership map only. It does not implement Java, tests, service wiring, runtime wiring, dashboard changes, market provider logic, Push, external channels, point generation, final direction, order execution, or auto-trading.

## 1. Executive Summary

当前 `Source-Owned Runtime / Point Proposal / BoundaryCandidate / ExecutionPlan` 已经存在明显重复：Cursor-before-P1 阶段已有 `BoundaryCandidateService`、`BoundaryCandidateDTO`、`PlanService`、`ExecutionPlan*`、`DecisionResult*` 和 dashboard display adapters；Codex P306-P358 又新增了 `ReviewOnlyPointProposal`、`ReviewOnlyNumericPointProposal`、`SourceTraceNumericSource`、`SourceOwnedCandidateIntegrationSourceBinding`、`SourceOwnedCandidateIntegrationRuntimeCandidate` 等多层 safety wrapper。

第一版 canonical owner 判断如下：

- `BoundaryCandidateService` / `BoundaryCandidateDTO` 更接近 review-only boundary candidate runtime owner。
- `PlanService` / `ExecutionPlanDO` / `ExecutionPlanMapper` / `ExecutionPlanVO` 更接近 review-only execution plan owner。
- `DecisionService` / `DecisionResultMapper` / `DecisionResultVO` 更接近 decision read-model owner。
- `DefaultPlanBoundaryDisplayAdapter`、`DefaultExecutionPlanDisplayAdapter`、`DefaultDashboardSourceTraceDetailAdapter` 等 dashboard adapters 是 dashboard display owner。
- `ReviewOnlyPointProposal`、`ReviewOnlyNumericPointProposal`、`SourceTraceNumericSource`、`SourceOwnedCandidateIntegration*` 当前只能算 safety adapter / wrapper，不应成为 canonical runtime owner。

必须冻结的方向：P359 / P360、Source-Owned Runtime Assembler revival、new point-candidate wrapper、new runtime-candidate wrapper、new DTO / Validator / Assembler / Orchestrator。P359 分支存在但未合并，#829 已关闭未合并，不计入完成进度。本次没有证据证明 revive P359 会减少重复；默认继续冻结。

本任务不提升 capability level。PositionSync runtime slice 继续保持 `REVIEW_ONLY_RUNTIME partial`。本任务的价值是减少重复风险，阻止 point/runtime wrapper 继续增殖。本任务不能生成点位，不能生成 entry / stop / TP / RR，不能生成 final direction。

下一步建议：**Targeted Source Read for BoundaryCandidate / ExecutionPlan owner**。先把现有 `BoundaryCandidate` 和 `ExecutionPlan` owner 的 service、mapper、dashboard display、风险边界读透，再决定是否做 merge design。不要回到 P359/P360。

## 2. Object Inventory

| Object / Family | Files found | Current role | Runtime connection | Dashboard/API connection | Risk | Initial classification |
|---|---|---|---|---|---|---|
| BoundaryCandidate | `src/main/java/org/example/trademodel/dto/planboundary/BoundaryCandidateDTO.java`, `BoundaryEntryDTO`, `BoundaryStopDTO`, `BoundaryTakeProfitLevelDTO`, `BoundarySourceFieldsDTO` | Carries boundary candidate shape, source fields, data quality, manual-review flags, and blocking reasons. | Partial: consumed through `BoundaryCandidateServiceImpl`. | Partial: dashboard adapters and detail VOs reference plan-boundary display. | Contains `entry`, `stop`, `takeProfitLevels`; must remain review-only and non-executable. | Canonical candidate owner candidate; requires targeted source read before merge work. |
| BoundaryCandidateService | `src/main/java/org/example/trademodel/service/BoundaryCandidateService.java`, `src/main/java/org/example/trademodel/service/impl/BoundaryCandidateServiceImpl.java` | Evaluates boundary candidate inputs, fails closed on missing source trace / source fields / Risk Action Guard blockers, otherwise creates `BoundaryCandidateDTO.valid(...)`. | Yes, service exists. | Indirect: feeds display/read-model path. | Can create `VALID`; use only behind manual review and not-trade-instruction boundaries. | Keep as likely canonical review-only boundary candidate owner. |
| ExecutionPlan | `src/main/java/org/example/trademodel/entity/ExecutionPlanDO.java`, `src/main/java/org/example/trademodel/vo/ExecutionPlanVO.java`, `src/main/java/org/example/trademodel/mapper/ExecutionPlanMapper.java` | Existing execution plan read/write/display model. | Partial/yes: mapper/entity/service exist. | Yes/partial: dashboard detail and execution-plan display adapter exist. | Text fields can be misread as executable advice; must stay advisory/review-only. | Keep as canonical review-only execution plan owner. |
| PlanService | `src/main/java/org/example/trademodel/service/PlanService.java`, `src/main/java/org/example/trademodel/service/impl/PlanServiceImpl.java` | Generates advisory `ExecutionPlanVO`, applies source trace readiness and Risk Action Guard readiness. | Yes, service exists. | Yes/partial through dashboard plan display. | Existing text such as leverage / position suggestion must not become executable. | Keep, then targeted source read before any merge. |
| ReviewOnlyPointProposal | `src/main/java/org/example/trademodel/dto/point/ReviewOnlyPointProposalDTO.java`, `ReviewOnlyPointProposalDisplayDTO.java`, `ReviewOnlyPointProposalAssembler.java`, `ReviewOnlyPointProposalDisplayAssembler.java`, `ReviewOnlyPointBoundaryGateDTO.java` | Codex safety wrapper around future point proposal / display gate. | No independent runtime owner. | Display-oriented skeleton only. | Duplicates existing plan/boundary display semantics if expanded. | Safety adapter; freeze by default. |
| NumericPointProposal | `src/main/java/org/example/trademodel/dto/point/ReviewOnlyNumericPointProposalDTO.java`, `NumericPointSafetyValidator.java`, `ReviewOnlyNumericPointProposalAssembler.java`, `SourceOwnedNumericPointCandidateAssembler.java` | Codex numeric point DTO / validator / assembler skeleton for entry / stop / TP / RR. | No service/runtime wiring. | No direct dashboard/API owner. | Highest overlap with `BoundaryCandidateDTO` and existing plan boundary fields. | Safety adapter; freeze and merge later only if it reduces duplication. |
| SourceTrace Numeric Source | `src/main/java/org/example/trademodel/dto/point/SourceTraceNumericSourceContextDTO.java`, `SourceTraceNumericSourceReadModelValidator.java`, `SourceTraceNumericSourceReadModelAssembler.java` | Codex source-trace numeric-source read-model skeleton. | No real source context runtime. | No direct dashboard/API owner. | Duplicates `BoundarySourceFieldsDTO` and dashboard source trace adapters if expanded separately. | Safety adapter; freeze pending source-trace owner read. |
| SourceOwnedCandidateIntegrationSourceBinding | `SourceOwnedCandidateIntegrationSourceBindingDTO`, `SourceOwnedCandidateIntegrationSourceBindingValidator`, `SourceOwnedCandidateIntegrationSourceBindingAssembler` | Explicit source binding DTO / validator / assembler skeleton for future source-owned candidate integration. | No runtime/service wiring. | No dashboard/API owner. | Adds another wrapper around already existing boundary/source/plan contexts. | Safety adapter; freeze. |
| SourceOwnedCandidateIntegrationRuntimeCandidate | `SourceOwnedCandidateIntegrationRuntimeCandidateDTO`, `SourceOwnedCandidateIntegrationRuntimeCandidateValidator` | Runtime candidate status DTO and validator skeleton. | No service/runtime wiring on main. | No dashboard/API owner. | Competes with `BoundaryCandidate`, `DecisionResult`, and dashboard adapters as runtime-candidate owner. | Runtime wrapper; freeze until owner source read proves value. |
| SourceOwnedCandidateIntegrationRuntimeValidator | `src/main/java/org/example/trademodel/validator/point/SourceOwnedCandidateIntegrationRuntimeCandidateValidator.java` | Validates explicit DTO data only. | No runtime read. | No dashboard/API connection. | Useful safety rules, but no ownership of runtime candidate. | Keep as safety rule reference; do not expand. |
| SourceOwnedCandidateIntegrationRuntimeAssembler / P359 branch | `origin/p359:src/main/java/org/example/trademodel/assembler/point/SourceOwnedCandidateIntegrationRuntimeCandidateAssembler.java` | Unmerged assembler that builds runtime DTO and calls validator. | No merged main connection. | No dashboard/API owner. | Would add another wrapper layer before owner consolidation. | P359 remains frozen; #829 closed unmerged. |
| DecisionResult | `src/main/java/org/example/trademodel/entity/DecisionResult.java`, `DecisionResultMapper.java`, `DecisionResultVO.java`, `DecisionBundleVO.java`, `DecisionService.java`, `DecisionServiceImpl.java` | Existing persisted decision read-model and dashboard source anchor. | Yes/partial: mapper/service exist. | Yes: dashboard summary/detail uses decision VO. | Decision text fields must not be treated as structural point truth. | Keep as canonical decision read-model owner. |
| Dashboard plan / point / boundary display adapters | `DefaultPlanBoundaryDisplayAdapter`, `DefaultPlanBoundarySourceTraceAdapter`, `DefaultExecutionPlanDisplayAdapter`, `DefaultDashboardSourceTraceDetailAdapter`, `DefaultDashboardRuntimeKlineContextAdapter`, `DefaultRiskActionGuardDisplayAdapter` | Existing dashboard display mapping layer. | Partial runtime read-model bridge. | Yes: dashboard surface owner. | Can drift if new wrappers bypass them. | Keep as canonical dashboard display owner. |

## 3. Overlap Analysis

| Overlap area | Is duplicate? | Duplicate type | Closer to runtime | Closer to dashboard/API | Safer today | Recommendation |
|---|---|---|---|---|---|---|
| BoundaryCandidate vs NumericPointProposal | Yes. Both model review-only numeric/boundary point fields such as entry, stop, TP and RR/source refs. | Duplicate plus safety wrapper. | `BoundaryCandidateService` / `BoundaryCandidateDTO` because service exists. | `BoundaryCandidate` display adapters are closer than numeric point skeletons. | Numeric skeleton has stronger forced flags; BoundaryCandidate is closer to real owner but needs source-read review. | Keep `BoundaryCandidate` as likely canonical owner; freeze `ReviewOnlyNumericPointProposal` until merge design. |
| ExecutionPlan vs PointProposal | Yes. Both can express proposed plan / point availability to a reviewer. | Duplicate / adapter. | `PlanService` / `ExecutionPlan*` because service/mapper/entity exist. | `DefaultExecutionPlanDisplayAdapter` is closer. | Codex point proposal wrappers are safer as carriers but not wired. | Keep `ExecutionPlan` as canonical review-only execution plan owner; freeze point proposal wrappers. |
| SourceOwnedCandidateIntegrationRuntimeCandidate vs BoundaryCandidate | Yes/unclear. RuntimeCandidate aims to be a future candidate status; BoundaryCandidate already represents a boundary candidate shape. | Runtime wrapper around overlapping candidate concept. | `BoundaryCandidateService` is closer on merged main. | Dashboard boundary adapters are closer. | RuntimeCandidate validator has useful forbidden-semantics rules, but no runtime owner. | Freeze RuntimeCandidate as adapter reference; do not revive P359 until it can merge into BoundaryCandidate owner. |
| SourceTrace Numeric Source vs BoundarySourceFields | Yes. Both describe source ownership for numeric fields. | Safety wrapper / possible adapter. | `BoundarySourceFieldsDTO` is closer to existing boundary candidate. | `DefaultDashboardSourceTraceDetailAdapter` is closer to dashboard. | SourceTraceNumericSource validator is stricter but isolated. | Freeze SourceTrace Numeric Source; targeted read should decide canonical source trace owner. |
| DecisionResult vs RuntimeCandidate / CandidateIntegration | Yes/unclear. DecisionResult is existing persisted read-model; RuntimeCandidate/CandidateIntegration tries to create a new candidate status model. | Wrapper / unclear replacement. | `DecisionService` / mapper / VO are closer. | Dashboard reads `DecisionResultVO`. | Runtime wrappers have safety flags, but no runtime path. | Keep DecisionResult as canonical decision read-model; freeze RuntimeCandidate/CandidateIntegration. |
| Dashboard display adapters vs new runtime candidate wrappers | Yes if wrappers bypass adapters. | Duplicate display path. | Display adapters are the existing bridge to dashboard runtime views. | Display adapters are canonical dashboard surface. | Adapters already encode fail-closed display behavior in several areas. | Keep adapters; future safety wrappers must adapt into them, not replace them. |

## 4. Canonical Owner Decision

| Capability | Canonical Owner | Secondary / Adapter | Frozen Objects | Decision |
|---|---|---|---|---|
| Review-only boundary candidate | `BoundaryCandidateServiceImpl` + `BoundaryCandidateDTO` + plan-boundary dashboard adapters | `ReviewOnlyNumericPointProposalDTO`, `ReviewOnlyPointProposalDTO` as safety adapters only | Source-owned runtime candidate wrappers, P359 assembler | Keep BoundaryCandidate as likely owner; perform targeted source read before merge implementation. |
| Review-only execution plan | `PlanServiceImpl` + `ExecutionPlanDO` / `ExecutionPlanMapper` / `ExecutionPlanVO` + `DefaultExecutionPlanDisplayAdapter` | PointProposal display DTOs as adapter candidates | NumericPointProposal and SourceOwnedRuntimeCandidate as parallel owners | Keep ExecutionPlan; do not create new execution-plan wrapper. |
| Numeric point proposal | `BoundaryCandidateDTO` / `BoundarySourceFieldsDTO` unless targeted source read proves another owner | `ReviewOnlyNumericPointProposalDTO` as safety rule reference | New numeric point DTO/validator/assembler packages | Freeze standalone NumericPointProposal expansion; merge later into canonical boundary owner. |
| Source trace for numeric fields | UNKNOWN - requires targeted source read | `BoundarySourceFieldsDTO`, `SourceTraceDTO`, `DefaultDashboardSourceTraceDetailAdapter`, `SourceTraceNumericSourceContextDTO` | New SourceTrace numeric-source wrappers | Do not choose a new owner yet; source read must inspect existing source trace owner path. |
| Runtime candidate status | UNKNOWN - requires targeted source read | `DecisionResultVO`, `BoundaryCandidateDTO`, dashboard adapters, `SourceOwnedCandidateIntegrationRuntimeCandidateDTO` | P359/P360 and new runtime-candidate wrappers | Freeze SourceOwnedRuntimeCandidate as canonical owner until it proves it reduces duplication. |
| Dashboard display | `DashboardController` + `DashboardDetailResponseVO` + `Default*DisplayAdapter` classes + `dashboard.html` | Codex display DTOs only if they feed existing display adapters | New dashboard runtime wrappers | Keep existing dashboard adapters as canonical display surface. |
| Decision result | `DecisionServiceImpl` + `DecisionResultMapper` + `DecisionResultVO` / `DecisionBundleVO` | RuntimeCandidate / CandidateIntegration can only reference validation/source status | SourceOwned runtime candidate wrapper as replacement | Keep DecisionResult as decision read-model owner; do not replace it with SourceOwned runtime wrappers. |

## 5. P359 / P360 Re-evaluation

- P359 branch exists but is unmerged.
- PR #829 was closed without merge.
- P359 does not count as completed progress.
- P360 is not allowed to start.
- The visible P359 assembler only constructs `SourceOwnedCandidateIntegrationRuntimeCandidateDTO` from explicit input and calls `SourceOwnedCandidateIntegrationRuntimeCandidateValidator`.
- On merged main, it has no service/runtime/dashboard/API owner and does not reduce overlap with `BoundaryCandidate`, `ExecutionPlan`, or `DecisionResult`.

Conclusion: **do not revive P359 by default**. P359 can be reconsidered only if a targeted source read proves that its assembler will merge into an existing owner path and reduce duplicate wrappers. Current evidence points the other way: it would add another runtime-candidate wrapper before canonical ownership is settled.

## 6. Merge / Freeze Recommendations

| Item | Action | Reason | Timing |
|---|---|---|---|
| BoundaryCandidate | keep | Closest existing owner for boundary candidate shape; service and dashboard adapters exist. | Now; targeted source read next. |
| NumericPointProposal | freeze / merge later | Good safety rules, but duplicates BoundaryCandidate and has no runtime/dashboard owner. | Freeze now; merge only after BoundaryCandidate owner source read. |
| ReviewOnlyPointProposal | freeze / adapter later | It is a point proposal display/gate wrapper, not canonical runtime owner. | Freeze now; adapt later only if it feeds existing dashboard display without new ownership. |
| SourceOwnedRuntimeCandidate | freeze / delete later only after owner stability | No merged service/dashboard owner; overlaps BoundaryCandidate and DecisionResult. | Freeze now; deletion only after canonical runtime owner is stable and tests are mapped. |
| ExecutionPlan | keep | Existing service/entity/mapper/VO/display path is closer to runtime and dashboard. | Now; targeted source read next. |
| DecisionResult | keep | Existing persisted decision read model and dashboard source anchor. | Now; do not replace with runtime wrappers. |
| Dashboard display adapters | keep | Canonical display surface for plan, boundary, source trace, kline, and risk guard views. | Now; wrappers must feed adapters, not bypass them. |
| P359 Runtime Assembler | freeze | Unmerged branch adds wrapper layer and does not solve owner duplication. | Freeze by default; revisit only after owner read proves reduction. |

Do not delete duplicated code now. The dependency graph, tests, and dashboard display path still need targeted source reads. Deletion is safe only after a canonical owner is confirmed and adapter/test migration is explicit.

## 7. Next Step Decision

Decision: **A. Targeted Source Read for BoundaryCandidate / ExecutionPlan owner**.

Reason:

- This is the largest overlap zone: existing Cursor-era `BoundaryCandidate` / `ExecutionPlan` already touch service and dashboard, while Codex numeric point / source-owned runtime wrappers are mostly isolated safety layers.
- A merge design would be premature without reading exactly how `BoundaryCandidateServiceImpl`, `PlanServiceImpl`, `ExecutionPlanMapper`, dashboard display adapters, and tests interact.
- It directly supports the freeze rule by choosing canonical ownership before any Java change.

Do not start:

- P359;
- P360;
- new DTO;
- new Validator;
- new Assembler;
- Three AI;
- Position Monitor expansion;
- Push;
- point generation;
- order / execution / auto-trading.

## 8. Capability-Level Statement

PositionSync slice remains `REVIEW_ONLY_RUNTIME partial`.

本包是否提升 level: **No**.

本包仍值得做，因为它阻止 `SourceOwnedRuntimeCandidate`、`NumericPointProposal`、`ReviewOnlyPointProposal`、`BoundaryCandidate`、`ExecutionPlan` 继续各走一套模型，避免再次掉回“包数推进但业务能力不提升”的状态。

This is not Production Wiring.

This is not point generation.

This is not executable entry / stop / TP / RR.

## 9. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No
- 是否接 service/runtime/dashboard/API: No, merge map only
- 是否符合 #830 审计建议: Yes

## 10. Final Recommendation

当前应保留 `BoundaryCandidate`、`ExecutionPlan`、`DecisionResult` 和 dashboard display adapters 作为第一批 canonical owner 候选；应冻结 `SourceOwnedCandidateIntegrationRuntimeCandidate`、`ReviewOnlyNumericPointProposal`、`ReviewOnlyPointProposal`、P359 和 P360。下一步做 `Targeted Source Read for BoundaryCandidate / ExecutionPlan owner`，不是 revive P359/P360。现在不能大删重复代码，因为 owner 依赖、dashboard 展示和测试迁移还没有被逐项确认；先读清楚 owner，再做最小 merge design。
