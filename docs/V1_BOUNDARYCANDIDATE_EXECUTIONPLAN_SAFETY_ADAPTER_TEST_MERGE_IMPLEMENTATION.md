# V1 BoundaryCandidate / ExecutionPlan Safety Adapter Test Merge Implementation

## 1. Executive Summary

This package performs a tests-first, owner-path-only safety adapter merge.

It does not add or modify production Java. It does not add DTO, Validator, Assembler, Orchestrator, runtime candidate wrapper, point proposal owner, dashboard path, endpoint, schema, config, pom, service wiring, runtime wiring, push, external channel, point generation, or trading behavior.

The implementation strengthens existing owner-path tests so future safety semantics remain absorbed into:

```text
BoundaryCandidateService / BoundaryCandidateDTO
-> PlanService / ExecutionPlanVO / ExecutionPlanDO / ExecutionPlanMapper
-> DecisionResult read model
-> DefaultPlanBoundaryDisplayAdapter / DefaultExecutionPlanDisplayAdapter / DashboardController
```

It keeps `SourceOwnedCandidateIntegrationRuntimeCandidate`, `ReviewOnlyNumericPointProposal`, `ReviewOnlyPointProposal`, `NumericPointSafetyValidator`, and P359 / P360 frozen from acting as standalone owners.

## 2. Tests Added / Strengthened

| Test | Safety lock |
|---|---|
| `BoundaryCandidateServiceImplTest.ownerPathShouldNotReferenceFrozenPointOrRuntimeWrappers` | Confirms the BoundaryCandidate owner path does not depend on frozen point/runtime wrapper names. |
| `DefaultExecutionPlanDisplayAdapterTest.displayAdaptersShouldNotBecomePointProposalOrRuntimeCandidateOwners` | Confirms execution-plan and plan-boundary display adapters do not become parallel point/runtime candidate owners. |

Existing owner-path tests already cover:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- missing source trace downgrade
- missing numeric source downgrade
- RiskActionGuard blocking downgrade
- display-only `NOT_TRADE_INSTRUCTION`
- display-only `ENTRY_STOP_TP_RR_NOT_GENERATED`
- non-executable review-only output

## 3. Frozen Objects

The following remain frozen:

- P359
- P360
- `SourceOwnedCandidateIntegrationRuntimeCandidate` from becoming a canonical owner
- `ReviewOnlyNumericPointProposal` as a standalone owner
- `ReviewOnlyPointProposal` as a standalone owner
- new DTO / Validator / Assembler / Orchestrator families
- Push / external channel
- point generation
- order / execution / auto-trading

## 4. Capability Statement

The current capability remains `REVIEW_ONLY_RUNTIME partial`, limited to the PositionSync + Dashboard review-only status slice.

This package does not raise capability level. It reduces duplication risk by locking the existing BoundaryCandidate / ExecutionPlan owner path against frozen wrapper ownership.

## 5. Next Required Action

The next required action is:

```text
Minimal Owner-Path Safety Adapter Test/Merge Verification
```

Do not continue P359 or start P360.
