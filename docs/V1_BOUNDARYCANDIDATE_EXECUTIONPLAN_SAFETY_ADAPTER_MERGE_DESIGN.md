# V1 BoundaryCandidate / ExecutionPlan Safety Adapter Merge Design

本文件是 #842 merge map 和 #843 owner source read 之后的最小 merge design。

它只设计如何把 Codex-era safety adapter / wrapper 中有价值的规则吸收到现有 Cursor-era owner path 中。不新增 Java，不修改测试，不改 dashboard，不接 service/runtime，不恢复 P359/P360，不生成点位。

## 1. Executive Summary

本任务只做 merge design，不实现。

未来 merge 的目标不是继续扩张 `ReviewOnlyPointProposal`、`ReviewOnlyNumericPointProposal`、`SourceOwnedCandidateIntegrationRuntimeCandidate` 或 P359/P360，而是把其中可复用的安全语义吸收到现有 owner path：

- `BoundaryCandidateService` / `BoundaryCandidateDTO` 继续作为 review-only boundary candidate owner；
- `PlanService` / `ExecutionPlanVO` / `ExecutionPlanDO` / `ExecutionPlanMapper` 继续作为 review-only execution plan owner；
- `DecisionResult` 继续作为 decision read-model owner；
- `DefaultPlanBoundaryDisplayAdapter` / `DefaultExecutionPlanDisplayAdapter` / dashboard adapters 继续作为 display owner。

可以保留的 adapter 语义包括：`manualReviewRequired`、`notTradeInstruction`、fail-closed、incomplete-safe、source trace completeness、numeric source ownership labels、blocking reasons、display-only readiness reasons，以及不生成 executable output 的测试约束。

必须冻结的 wrapper 包括：P359、P360、`SourceOwnedCandidateIntegrationRuntimeCandidate` 作为 canonical owner、`ReviewOnlyNumericPointProposal` 作为 standalone owner、`ReviewOnlyPointProposal` 作为 standalone owner，以及任何新的 DTO / Validator / Assembler family。

不允许恢复 P359/P360。#843 没有证明 P359 会减少重复；它更可能在 `BoundaryCandidate`、`ExecutionPlan`、`DecisionResult`、dashboard adapters 旁边新增一条 parallel runtime candidate owner。

本任务不提升 capability level。PositionSync slice 仍为 `REVIEW_ONLY_RUNTIME partial`。本任务不生成点位，不删除代码，不改 Java。下一步应进入 `Minimal implementation readiness gate for owner-path safety adapter merge`，先验证是否能只通过现有 owner path 和现有测试面吸收安全语义。

## 2. Owner Path To Preserve

必须固定并保留的 owner path：

```text
BoundaryCandidateService / BoundaryCandidateDTO
  -> PlanService / ExecutionPlanVO / ExecutionPlanDO / ExecutionPlanMapper
  -> DecisionResult read model
  -> DefaultPlanBoundaryDisplayAdapter / DefaultExecutionPlanDisplayAdapter / DashboardController
```

未来任何 safety adapter 都必须 feed into owner path。

不允许绕过 dashboard adapters。Dashboard display owner 必须继续由 `DefaultPlanBoundaryDisplayAdapter`、`DefaultExecutionPlanDisplayAdapter` 以及相关 dashboard detail adapters 承担。

不允许新增 parallel runtime candidate owner。`SourceOwnedCandidateIntegrationRuntimeCandidate` 不能成为新的 canonical runtime candidate path。

不允许新增 parallel point proposal owner。`ReviewOnlyPointProposal` 和 `ReviewOnlyNumericPointProposal` 不能继续作为 standalone owner 扩张。

`DecisionResult` 只能继续作为 decision read-model aggregation。它可以承载最新 decision / plan / status 的读取视图，但不能变成 point owner、runtime candidate owner、final direction owner 或 trade action owner。

## 3. Safety Adapter Inventory

| Adapter / Wrapper | Useful safety semantics | Duplicate risk | Merge target | Keep / freeze / merge-later |
|---|---|---|---|---|
| `ReviewOnlyPointProposal` | Review-only point availability, unavailable / blocked / incomplete reasons, `manualReviewRequired`, `notTradeInstruction`, non-executable display wording. | High. It overlaps existing plan boundary and execution plan display owners. | `DefaultPlanBoundaryDisplayAdapter` / `DefaultExecutionPlanDisplayAdapter` display reasons. | Freeze as standalone owner; merge-later as display semantics only. |
| `ReviewOnlyNumericPointProposal` | Nullable numeric fields, incomplete / degraded / blocked states, forced safety flags, non-executable point semantics. | High. It overlaps `BoundaryCandidateDTO` numeric boundary fields and `BoundaryCandidateService` validation. | `BoundaryCandidateService` / `BoundaryCandidateDTO` existing validation path. | Freeze as standalone owner; merge-later only if readiness gate proves direct owner-path absorption. |
| `NumericPointSafetyValidator` | Forbidden executable semantics, required refs, fail-closed / incomplete / degraded checks, safety flags. | High if kept as parallel validator. Existing owner path already has boundary status, blocking reasons, source trace, data quality, and RiskActionGuard handling. | Existing `BoundaryCandidateService` validation rules and targeted owner-path tests. | Merge-later as rule source; do not keep as canonical runtime validator. |
| `SourceTraceNumericSource` | Numeric source identity, source owner labels, freshness / completeness semantics, source confidence labels. | Medium-high. It overlaps `BoundarySourceFieldsDTO` and source trace display adapters. | `BoundarySourceFieldsDTO` / source trace display adapters. | Freeze standalone source owner; merge-later as source field labels and completeness reasons. |
| `SourceOwnedCandidateIntegrationSourceBinding` | Upstream source completeness, trust, review-only, incomplete-safe, fail-closed flags. | High. It wraps source ownership but is not connected to existing runtime owner path. | Boundary / execution readiness reasons only after owner-path gate. | Freeze; reuse only as safety vocabulary. |
| `SourceOwnedCandidateIntegrationRuntimeCandidate` | Runtime candidate status, incomplete / blocked / degraded / review-only distinctions, safety flags. | Very high. It competes with `BoundaryCandidate`, `ExecutionPlan`, and dashboard adapters as runtime/display owner. | Display/readiness reasons inside existing owner path, not as object owner. | Freeze as canonical owner. |
| `SourceOwnedCandidateIntegrationRuntimeCandidateValidator` | Safety flag enforcement, forbidden semantics checks, blocked/incomplete/degraded/review-only validation result. | High if revived independently. It has no service/dashboard runtime owner path. | Owner-path tests and fail-closed validation rules. | Merge-later as test/rule reference only. |
| P359 Runtime Assembler | Explicit-input assembly discipline and mandatory validator invocation. | Very high. It would create a new parallel assembler path beside the existing owners. | None until readiness gate proves it reduces duplication. | Freeze. Do not revive by default. |

## 4. Minimal Merge Design

Future merge must be absorption into existing owners, not wrapper expansion.

- Numeric safety rules can only merge into `BoundaryCandidateService` / `BoundaryCandidateDTO` or the existing boundary validation path.
- Source trace numeric semantics can only merge into `BoundarySourceFieldsDTO` / source trace display adapters.
- Runtime candidate status semantics can only merge as display/readiness reasons. They must not become a new canonical runtime candidate object.
- PointProposal display semantics can only feed `DefaultPlanBoundaryDisplayAdapter` / `DefaultExecutionPlanDisplayAdapter`.
- `manualReviewRequired` and `notTradeInstruction` must remain true in any future owner-path output.
- entry / stop / TP / RR must not be represented as executable output.
- `DecisionResult` remains read-model aggregation. It must not become point owner, runtime candidate owner, final direction owner, or trade action owner.

The safest future merge sequence is:

1. Run an implementation readiness gate that checks exact owner-path test coverage, changed-file limits, status mapping, and no-new-object proof.
2. If GO, add or adjust only tests around existing owner path first, where useful.
3. If implementation is allowed, modify only existing owner-path code needed to absorb a safety rule. Do not add a new DTO / Validator / Assembler.
4. Keep dashboard display through existing adapters.
5. Only after owner path is stable and tests map old wrapper semantics to owner behavior, consider deprecating duplicate wrappers.

Concrete mapping guidance:

| Safety semantics | Allowed merge location | Not allowed |
|---|---|---|
| Missing source trace / numeric source reason | `BoundarySourceFieldsDTO`, `BoundaryCandidateService` blocking reasons, source trace display adapter | New SourceTrace numeric owner object |
| Incomplete numeric boundary state | `BoundaryCandidateDTO.status=INCOMPLETE` or existing boundary display readiness | New numeric point proposal runtime owner |
| Fail-closed blocked state | Existing boundary blocking reasons, execution plan display guardrails | New runtime candidate validator as canonical gate |
| Degraded / watch-only state | Existing `WATCH_ONLY` / display readiness wording after readiness gate confirms mapping | Silent upgrade to valid review-only status |
| Non-executable safety labels | Existing dashboard display adapters and owner-path output fields | New point/candidate wrapper family |
| Forbidden execution semantics checks | Existing targeted tests / owner-path validation rules | Separate P359/P360 assembler chain |

Any future implementation must preserve the owner-path invariant:

```text
Codex safety rule -> existing owner path -> existing dashboard adapter -> review-only display
```

It must not become:

```text
Codex safety rule -> new runtime candidate wrapper -> new assembler -> new display path
```

## 5. What Stays Frozen

The following remain frozen:

- P359
- P360
- `SourceOwnedCandidateIntegrationRuntimeCandidate` as canonical owner
- `ReviewOnlyNumericPointProposal` as standalone owner
- `ReviewOnlyPointProposal` as standalone owner
- new DTO / Validator / Assembler families
- Three AI
- Position Monitor expansion
- Push
- point generation
- order / execution / auto-trading

No future package may thaw them by saying it is only "review-only" unless it proves direct duplicate reduction and owner-path absorption.

## 6. What Can Be Reused Later

The following can be reused later:

- safety flags;
- blocking reasons;
- source trace completeness rules;
- numeric source ownership labels;
- fail-closed validation;
- display-only readiness reasons;
- tests that check non-executable behavior.

These are reusable only as absorbed semantics inside owner path. They cannot continue as standalone wrapper families.

Allowed reuse examples:

- Add a missing source trace completeness reason to `BoundaryCandidateService` if readiness gate proves it is absent.
- Map source ownership labels into `BoundarySourceFieldsDTO` or dashboard source trace display.
- Extend existing display adapters with clearer blocked/incomplete reason copy.
- Preserve tests that assert no executable entry / stop / TP / RR / order / execution semantics.

Disallowed reuse examples:

- Revive P359 to assemble `SourceOwnedCandidateIntegrationRuntimeCandidate`.
- Add a new `PointProposal` owner beside `BoundaryCandidateDTO`.
- Add a new validator family when an existing service/display adapter can enforce or display the rule.
- Add a dashboard path that bypasses `DefaultPlanBoundaryDisplayAdapter` or `DefaultExecutionPlanDisplayAdapter`.

## 7. Delete / Deprecation Policy

Do not delete duplicate code now.

Deletion must wait until:

- owner path is stable;
- tests prove wrapper semantics are preserved in owner path;
- dashboard/API behavior is not regressed;
- no safety test is removed without equivalent owner-path coverage;
- deprecation targets are documented.

The policy is:

1. Freeze duplicate wrappers.
2. Merge or adapt useful safety semantics into owner path.
3. Add or preserve tests that prove non-executable behavior.
4. Deprecate or delete only after the owner path is stable.

Do not delete code just to make the tree look cleaner. That would trade a duplication problem for a safety-regression problem.

## 8. Next Step Decision

Recommended next step:

**A. Minimal implementation readiness gate for owner-path safety adapter merge**

Reason:

- #843 already confirmed the owner candidates exist and are more canonical than the Codex wrapper families.
- The next risk is implementation scope creep. A readiness gate can decide whether a minimal owner-path merge is possible without new DTO / Validator / Assembler work.
- It forces the next package to prove changed-file limits, test targets, no-new-object compliance, and exact rule mapping before any Java change.

Not B yet:

- Further SourceTrace owner read may be useful later, but #843 already found enough to design owner-path absorption at the boundary/plan layer.

Not C yet:

- Choosing the next runtime slice before closing the owner-path merge readiness would leave duplicate point/runtime wrappers unresolved.

Do not recommend P359, P360, new DTO, new Validator, new Assembler, Three AI, Position Monitor expansion, Push, point generation, order, execution, or auto-trading.

## 9. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No
- 是否接 service/runtime/dashboard/API: No, design only
- 是否符合 #830 审计建议: Yes

## 10. Final Recommendation

明确结论：保留 `BoundaryCandidateService` / `BoundaryCandidateDTO` -> `PlanService` / `ExecutionPlanVO/DO/Mapper` -> `DecisionResult` -> dashboard display adapters 这条 owner path；冻结 `ReviewOnlyPointProposal`、`ReviewOnlyNumericPointProposal`、`SourceOwnedCandidateIntegrationRuntimeCandidate`、P359/P360 和新的 DTO/Validator/Assembler 扩张；把 safety flags、blocking reasons、source trace completeness、fail-closed、non-executable tests 吸收到 owner path。下一步做 `Minimal implementation readiness gate for owner-path safety adapter merge`。这不是 P359/P360，因为目标是减少 parallel wrapper，而不是再开一条 runtime candidate chain。现在不能删除重复代码，因为 owner-path merge 还没有测试覆盖证明，贸然删除会破坏已有 safety evidence。
