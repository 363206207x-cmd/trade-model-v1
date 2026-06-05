# V1 BoundaryCandidate / ExecutionPlan Safety Adapter Implementation Readiness Gate

This readiness gate follows #842, #843, and #844. It is documentation only.

It does not add or modify Java, tests, dashboard code, service wiring, runtime wiring, schema, config, endpoint, DTO, Validator, Assembler, Orchestrator, point generation, Push, external channel, order execution, or auto-trading.

## 1. Executive Summary

结论：允许进入一个很窄的 `GO: Minimal owner-path safety adapter test/merge implementation`。

这个 GO 只允许未来实现把 Codex-era safety semantics 吸收到既有 owner path 中：

```text
BoundaryCandidateService / BoundaryCandidateDTO
-> PlanService / ExecutionPlanVO / ExecutionPlanDO / ExecutionPlanMapper
-> DecisionResult read model
-> DefaultPlanBoundaryDisplayAdapter / DefaultExecutionPlanDisplayAdapter / DashboardController
```

如果未来 implementation 需要新增 DTO / Validator / Assembler / Orchestrator、新 runtime candidate wrapper、恢复 P359/P360、绕过 dashboard adapters、生成 entry / stop / TP / RR，或者改变 dashboard display path，则立即 NO-GO。

最小 implementation 只能优先改 existing owner-path tests；如确有必要，才允许在既有 `BoundaryCandidateServiceImpl` validation path 或既有 `DefaultExecutionPlanDisplayAdapter` / `DefaultPlanBoundaryDisplayAdapter` 内吸收 safety semantics。它不需要新增 DTO / Validator / Assembler，不需要恢复 P359/P360，不需要新增 runtime candidate wrapper，不会生成点位，不会提升本包 capability level。

下一步应该做：`Minimal owner-path safety adapter test/merge implementation`。

## 2. Existing Owner-Path Test Coverage

| Owner path | Existing tests found | What safety semantics already covered | Gap |
|---|---|---|---|
| `BoundaryCandidateServiceImpl` / `BoundaryCandidateDTO` | `BoundaryCandidateServiceImplTest`, `BoundaryCandidateDTOTest`, `P17LocalFixtureFailClosedTest`, `P18DerivativesRiskContextFixtureExtensionTest` | Missing SourceTrace -> `INCOMPLETE`; SourceTrace watch-only fallback -> `WATCH_ONLY`; missing numeric source value -> `INCOMPLETE`; complete sources -> `VALID`; RuntimeKline-only visibility does not upgrade validity; RiskActionGuard stampede / liquidity missing / wick-only risk / action flag -> `WATCH_ONLY`; `manualReviewRequired=true`; `notTradeInstruction=true`; service does not expose trading execution methods. | Existing owner path has numeric-looking fields, so future changes must keep them review-only and adapter-gated. Wrapper-specific reason labels are not yet fully mapped into owner-path reason names. |
| `BoundaryCandidateDTO` factory and carrier | `BoundaryCandidateDTOTest` | `valid(...)` requires symbol, timeframe, entry, stop, take-profit list, source fields, and data quality score; it forces manual review and not-trade flags; it defensively copies take-profit levels. | DTO currently has setters, so future work should avoid creating a new wrapper and should add tests around existing safety behavior before any service-path change. |
| `PlanServiceImpl` / `ExecutionPlanVO` | `PlanServiceImplTest`, `RuleEngineServiceSourceTraceTest`, `P17LocalFixtureFailClosedTest`, `P18DerivativesRiskContextFixtureExtensionTest` | No SourceTrace -> advisory `INCOMPLETE`; incomplete SourceTrace -> `INCOMPLETE`; complete SourceTrace -> `READINESS_READY_REVIEW_ONLY`; RiskActionGuard stampede / liquidity missing / wick-only / action flag -> watch-only; `manualReviewRequired=true`; `notTradeInstruction=true`. | Legacy plan text and suggestion fields can be misread. Future merge must not turn plan text into executable advice. |
| `DefaultExecutionPlanDisplayAdapter` | `DefaultExecutionPlanDisplayAdapterTest` | Missing boundary -> `BOUNDARY_PENDING`; incomplete boundary -> `INCOMPLETE`; watch-only boundary -> `WATCH_ONLY`; valid boundary plus missing SourceTrace -> `INCOMPLETE`; safe-fail-closed SourceTrace -> `WATCH_ONLY`; complete SourceTrace -> `READY_REVIEW_ONLY`; RiskActionGuard high risk / stampede / liquidity missing / wick-only -> `WATCH_ONLY`; guardrails include `EXECUTION_PLAN_REVIEW_ONLY_DISPLAY`, `EXECUTION_PLAN_NOT_EXECUTABLE`, `NOT_TRADE_INSTRUCTION`, `ENTRY_STOP_TP_RR_NOT_GENERATED`; adapter exposes no trading action / automation / valid factory surface. | It is the strongest current display owner. Future safety semantics should feed this adapter instead of bypassing it. |
| `DefaultPlanBoundaryDisplayAdapter` / source trace display adapters | `DefaultPlanBoundaryDisplayAdapterTest`, `DefaultPlanBoundarySourceTraceAdapterTest`, `DefaultDashboardSourceTraceDetailAdapterTest`, `DefaultRiskActionGuardDisplayAdapterTest` | Display adapters already encode fail-closed display behavior for plan boundary, source trace detail, source trace readiness, and risk guard status. | Some Codex wrapper labels may need a one-to-one display reason mapping later. That mapping should be absorbed into adapters, not into a new display path. |
| Dashboard/controller tests | `DashboardControllerTest`, `DashboardDetailResponseVOTest`, `StaticNoTradeInstructionGuardTest` | Dashboard surface has existing static and controller guard tests around no-trade-instruction style safety. | This readiness gate does not authorize dashboard changes. If future implementation touches dashboard display, it must remain a small owner-path display adapter change, not expansion. |

## 3. Safety Semantics Readiness

| Safety semantic | Already covered in owner path? | Source wrapper where similar semantic exists | Need implementation? | Allowed target |
|---|---|---|---|---|
| `manualReviewRequired=true` | Yes. Covered in `BoundaryCandidateDTO`, `BoundaryCandidateServiceImpl`, `PlanServiceImpl`, and display adapter tests. | `ReviewOnlyPointProposal`, `ReviewOnlyNumericPointProposal`, Source-Owned runtime wrappers. | No new object needed. May add owner-path regression tests if a merge touches this. | Existing owner-path tests and existing DTO/service/display path. |
| `notTradeInstruction=true` | Yes. Covered across BoundaryCandidate, ExecutionPlan, and display adapters. | Same Codex wrappers. | No new object needed. | Existing owner-path tests and display adapter guardrails. |
| fail-closed | Partial but sufficient for GO. Owner path uses `INCOMPLETE`, `WATCH_ONLY`, `SAFE_FAIL_CLOSED_ONLY`, and explicit display not-executable reasons. | `NumericPointSafetyValidator`, Source-Owned runtime validator. | Minimal implementation may add reason mapping tests. It must not add a new validator. | `BoundaryCandidateServiceImpl` validation path or `DefaultExecutionPlanDisplayAdapter` reason mapping, only if necessary. |
| incomplete-safe | Yes. Missing SourceTrace, missing numeric source, missing SourceTrace fields, RuntimeKline-only visibility, and incomplete display states are already covered. | `ReviewOnlyNumericPointProposalDTO`, SourceTrace numeric source wrappers. | No new object needed. | Existing service/display owner tests. |
| missing source trace | Yes. `BoundaryCandidateServiceImplTest`, `PlanServiceImplTest`, and `DefaultExecutionPlanDisplayAdapterTest`. | SourceTrace numeric source wrappers. | No implementation needed unless label mapping is added. | Existing SourceTrace / BoundaryCandidate / ExecutionPlan owner path. |
| missing numeric source | Yes for owner path. `BoundaryCandidateServiceImplTest` covers missing entry numeric source value and `BoundaryCandidateDTOTest` covers required valid factory inputs. | SourceTrace numeric source wrappers. | Optional targeted owner-path test only. | `BoundaryCandidateServiceImplTest` or `BoundaryCandidateDTOTest`. |
| dataQualityScore missing/low | Partial. `BoundaryCandidateDTOTest` requires dataQualityScore for valid factory; P17/P18 fixture tests cover data-quality downgrade cases in source trace context. | DataQualityContext source binding and numeric safety wrappers. | If future merge claims data-quality absorption, add owner-path tests first. | `BoundaryCandidateServiceImplTest`, `P18DerivativesRiskContextFixtureExtensionTest`, or display adapter tests. |
| RiskActionGuard blocking | Yes. Boundary service, PlanService, RuleEngine, and display adapter tests cover stampede, liquidity missing, wick-only risk, high risk, and unsafe action flags. | RiskActionGuard source binding and Source-Owned runtime wrappers. | No new object needed. | Existing `RiskActionGuardDisplayVO` consumption in owner path. |
| source trace completeness | Yes. SourceTrace readiness and fallback status already drive BoundaryCandidate / ExecutionPlan display state. | SourceTrace numeric source wrappers. | Optional mapping tests only. | `BoundaryCandidateServiceImplTest`, `RuleEngineServiceSourceTraceTest`, `DefaultExecutionPlanDisplayAdapterTest`. |
| non-executable entry / stop / TP / RR guardrail | Yes at display level. `DefaultExecutionPlanDisplayAdapterTest` asserts `ENTRY_STOP_TP_RR_NOT_GENERATED`; service tests keep manual review and not-trade flags. | `ReviewOnlyNumericPointProposalDTO`, `NumericPointSafetyValidator`. | May need targeted tests if wrapper semantics are merged into display reasons. | Existing display adapters and tests. |
| display-only readiness reason | Yes. Execution plan display adapter already maps safety states to not-executable reasons and incomplete reasons. | Source-Owned runtime candidate status wrappers. | Reason mapping may be implemented only inside existing display owner path. | `DefaultExecutionPlanDisplayAdapter` / `DefaultPlanBoundaryDisplayAdapter`. |
| forbidden execution semantics | Partial but sufficient for GO. Existing owner-path tests check no execution methods and display not-executable guardrails; Codex wrappers have deeper forbidden semantic tests. | `NumericPointSafetyValidator`, Source-Owned runtime validator. | Future minimal implementation may add tests to owner-path static/source guard only. It must not import the wrapper validator as a new canonical gate. | Existing owner tests or static no-trade guard tests. |

## 4. Minimal Implementation Scope If GO

The GO scope is deliberately narrow.

Allowed candidate targets:

- existing tests under the BoundaryCandidate / ExecutionPlan display owner path;
- existing `BoundaryCandidateServiceImpl` validation path, only if a missing safety reason cannot be represented by tests alone;
- existing `DefaultExecutionPlanDisplayAdapter` or `DefaultPlanBoundaryDisplayAdapter`, only if a display-only readiness reason needs mapping;
- docs/source-of-truth status updates.

Preferred implementation order:

1. Add or update targeted owner-path tests first.
2. If tests prove a small gap, make the smallest possible change in the existing owner path.
3. Keep `BoundaryCandidateService` / `BoundaryCandidateDTO` and `PlanService` / `ExecutionPlanVO` as owners.
4. Keep `DecisionResult` as read-model aggregation only.
5. Keep dashboard adapters as display owners.

Not allowed:

- new DTO;
- new Validator;
- new Assembler;
- new Orchestrator;
- new runtime candidate wrapper;
- P359;
- P360;
- new dashboard path;
- new endpoint;
- schema/config/pom changes;
- Push;
- AI;
- Position Monitor expansion;
- point generation;
- order execution;
- auto-trading.

## 5. No-Go Conditions

Future implementation must stop if it needs any of the following:

- a new DTO / Validator / Assembler / Orchestrator;
- revival of P359 or P360;
- a new runtime candidate owner;
- generated entry / stop / TP / RR;
- a changed dashboard display path;
- a large service rewrite;
- deletion of duplicate code before owner-path tests are stable;
- a new endpoint, schema, config, or pom change;
- bypassing `BoundaryCandidate` / `ExecutionPlan` / dashboard adapter owner path.

## 6. Go / No-Go Decision

Decision: `GO: Minimal owner-path safety adapter test/merge implementation`.

Reason:

- The canonical owner path is real and already has service, DTO, VO, mapper, read model, and dashboard adapter coverage.
- Existing tests already cover the core safety semantics: manual review, not-trade instruction, incomplete-safe fallback, watch-only fallback, SourceTrace completeness, RiskActionGuard blocking, and non-executable display guardrails.
- The useful Codex-era wrapper semantics can be absorbed as reason labels, guardrail tests, or display-only readiness semantics inside the owner path.
- No evidence shows that P359/P360 or a new runtime candidate wrapper would reduce duplication. They remain frozen.

This GO does not authorize product-level point generation. It authorizes only a minimal implementation package that proves the existing owner path can absorb safety semantics without expanding object families.

## 7. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, readiness gate only
- 是否接 service/runtime/dashboard/API: No, readiness only
- 是否符合 #830 审计建议: Yes

## 8. Final Recommendation

明确结论：允许进入 `Minimal owner-path safety adapter test/merge implementation`，但只能在既有 `BoundaryCandidateServiceImpl` / `BoundaryCandidateDTO` / `PlanService` / `ExecutionPlanVO` / dashboard display adapter owner path 和既有测试体系内做最小吸收。禁止新增 DTO / Validator / Assembler / runtime candidate wrapper，禁止恢复 P359/P360，禁止生成点位，禁止绕过 dashboard adapters。现在也不能删除重复代码，因为 owner-path 吸收关系和回归测试还没有完成，直接删除会破坏已有 safety wrapper 资产和现有 runtime/display owner 的映射。
