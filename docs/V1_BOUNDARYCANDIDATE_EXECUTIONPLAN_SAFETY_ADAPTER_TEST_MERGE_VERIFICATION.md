# V1 BoundaryCandidate / ExecutionPlan Safety Adapter Test Merge Verification

## 1. Executive Summary

#846 验证通过。

#846 只改了既有 owner-path tests：

- `BoundaryCandidateServiceImplTest`
- `DefaultExecutionPlanDisplayAdapterTest`

#846 没有修改生产 Java，没有新增 DTO / Validator / Assembler / Orchestrator，没有恢复 P359 / P360，没有新增 runtime candidate wrapper，没有新增 point proposal owner，没有改 dashboard / schema / config / pom，也没有接 service / runtime / push。

验证结果确认：

- `BoundaryCandidateService` / `BoundaryCandidateDTO` owner path 不依赖 frozen point/runtime wrappers。
- `DefaultExecutionPlanDisplayAdapter` / `DefaultPlanBoundaryDisplayAdapter` display owner path 不依赖 frozen point/runtime wrappers。
- `SourceOwnedCandidateIntegrationRuntimeCandidate` 仍然 non-canonical / frozen。
- `ReviewOnlyNumericPointProposal` 和 `ReviewOnlyPointProposal` 仍然 standalone frozen。
- P359 / P360 继续冻结。
- safety semantics 只能被吸收到现有 BoundaryCandidate / ExecutionPlan / dashboard adapter owner path。

当前 capability level 不变化，仍是 `REVIEW_ONLY_RUNTIME partial`，且仅限 PositionSync + Dashboard review-only status slice。本 verification 不等于 Production Wiring，不等于 point generation，不等于完整 ExecutionPlan runtime，不等于交易能力。

下一步建议：`Minimal owner-path safety adapter production merge readiness review`。

## 2. Verification Matrix

| Check | Result | Evidence |
|---|---|---|
| workflow contract | PASS | `bash scripts/check-workflow-contract.sh` -> `WORKFLOW_CONTRACT_OK` |
| compile | PASS | `./mvnw -q -DskipTests compile` |
| test-compile | PASS | `./mvnw -q -DskipTests test-compile` |
| `BoundaryCandidateServiceImplTest` | PASS | `./mvnw -q -Dtest=BoundaryCandidateServiceImplTest test` |
| `BoundaryCandidateDTOTest` | PASS | `./mvnw -q -Dtest=BoundaryCandidateDTOTest test` |
| `DefaultExecutionPlanDisplayAdapterTest` | PASS | `./mvnw -q -Dtest=DefaultExecutionPlanDisplayAdapterTest test` |
| `DefaultPlanBoundaryDisplayAdapterTest` | PASS | `./mvnw -q -Dtest=DefaultPlanBoundaryDisplayAdapterTest test` |
| `StaticNoTradeInstructionGuardTest` | PASS | `./mvnw -q -Dtest=StaticNoTradeInstructionGuardTest test` |
| no production Java change | PASS | forbidden path check over `src/main/java` returned no changed files |
| no dashboard/schema/config/pom change | PASS | forbidden path check over `src/main/resources` and `pom.xml` returned no changed files |
| no new DTO / Validator / Assembler | PASS | changed files are docs plus existing tests only; no production object family was added |
| no P359 / P360 | PASS | no P359/P360 branch revival, no runtime assembler package, no P360 package |
| no frozen wrapper dependency in owner path | PASS | grep over `BoundaryCandidateServiceImpl`, plan-boundary DTOs, and display adapters returned no frozen wrapper dependency |
| no trading semantics | PASS | no production Java or dashboard changes; targeted tests continue to assert no owner-path trade/execution surface |

## 3. Owner-Path Safety Coverage Confirmed

Confirmed owner-path safety coverage:

- BoundaryCandidate owner path does not depend on frozen wrappers.
- ExecutionPlan display owner path does not depend on frozen wrappers.
- `SourceOwnedCandidateIntegrationRuntimeCandidate` remains non-canonical and frozen.
- `ReviewOnlyNumericPointProposal` remains standalone frozen.
- `ReviewOnlyPointProposal` remains standalone frozen.
- P359 / P360 remain frozen.
- Safety semantics may only be absorbed into the existing owner path:

```text
BoundaryCandidateService / BoundaryCandidateDTO
-> PlanService / ExecutionPlanVO / ExecutionPlanDO / ExecutionPlanMapper
-> DecisionResult read model
-> DefaultPlanBoundaryDisplayAdapter / DefaultExecutionPlanDisplayAdapter / DashboardController
```

The verified tests specifically lock:

- `BoundaryCandidateServiceImplTest.ownerPathShouldNotReferenceFrozenPointOrRuntimeWrappers`
- `DefaultExecutionPlanDisplayAdapterTest.displayAdaptersShouldNotBecomePointProposalOrRuntimeCandidateOwners`

Existing tests continue to cover `manualReviewRequired`, `notTradeInstruction`, missing source trace downgrade, missing numeric source downgrade, RiskActionGuard blocking downgrade, display-only `NOT_TRADE_INSTRUCTION`, display-only `ENTRY_STOP_TP_RR_NOT_GENERATED`, and non-executable review-only output.

## 4. Capability Conclusion

The current capability remains `REVIEW_ONLY_RUNTIME partial`.

This verification package does not raise capability level.

It is not Production Wiring.

It is not point generation.

It is not a complete ExecutionPlan runtime.

It is not trading capability.

The value of #846 is reducing duplicate-wrapper risk by keeping Codex-era safety semantics attached to the existing owner path rather than allowing a new runtime candidate or point proposal owner to emerge.

## 5. Next Step Decision

Recommended option: **A. Minimal owner-path safety adapter production merge readiness review**.

Reason:

- #846 proves the tests-only owner-path merge is safe and effective.
- Before any production Java can be touched, the project must decide whether there is an actual production owner-path gap that cannot be handled by tests and display/readiness reason mapping alone.
- A readiness review keeps the freeze rule intact and prevents a quiet slide back into P359/P360, new DTOs, new validators, new assemblers, Three AI, Position Monitor expansion, Push, point generation, or order / execution / auto-trading.

Not B:

- Further test coverage may be useful later, but the required owner-path frozen-wrapper safety assertions now exist and pass.

Not C:

- Choosing a new minimal runtime slice before deciding whether the owner-path safety merge needs a production readiness review would leave the BoundaryCandidate / ExecutionPlan merge track unfinished.

## 6. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: Verification only, confirms REVIEW_ONLY_RUNTIME partial
- 是否接 service/runtime/dashboard/API: No new wiring; verifies tests-only owner-path safety coverage
- 是否符合 #830 审计建议: Yes
