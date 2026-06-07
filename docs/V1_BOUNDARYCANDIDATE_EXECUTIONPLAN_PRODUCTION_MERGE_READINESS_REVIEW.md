# V1 BoundaryCandidate / ExecutionPlan Production Merge Readiness Review

This review follows #846 and #847. It is documentation only.

It does not add or modify production Java, tests, DTOs, Validators, Assemblers, Orchestrators, dashboard code, schema, config, pom, service wiring, runtime wiring, Push, external channel, point generation, order execution, or auto-trading.

## 1. Executive Summary

结论：**NO-GO production change**。

当前不需要生产 Java 改动。#846 已经用 tests-first 的方式锁住主要风险：`BoundaryCandidateServiceImplTest` 确认 BoundaryCandidate owner path 不依赖 frozen point/runtime wrappers，`DefaultExecutionPlanDisplayAdapterTest` 确认 execution-plan / plan-boundary display adapters 不会变成 point proposal 或 runtime candidate owner。#847 又验证了这些测试、编译、forbidden path 和 frozen wrapper dependency 检查。

如果后续真的发现生产缺口，最小改动也只能在既有 owner path 内做：`BoundaryCandidateServiceImpl`、`DefaultExecutionPlanDisplayAdapter` 或 `DefaultPlanBoundaryDisplayAdapter` 的极小 reason/status mapping。但本次 source read 没发现必须改这些生产文件的明确缺口，所以现在不允许为了“推进”硬改生产代码。

已有 tests-first owner-path safety lock。当前不允许新增 DTO / Validator / Assembler / Orchestrator，不允许恢复 P359 / P360，不允许新增 runtime candidate wrapper，不允许新增 point proposal owner，不允许生成 entry / stop / TP / RR，不允许生成 final direction，也不允许接 order / execution / auto-trading。

当前 capability level 不提升，仍是 `REVIEW_ONLY_RUNTIME partial`，且用户可见 runtime 能力仍只确认到 PositionSync + Dashboard review-only status。下一步应转向 `Next Minimal Runtime Slice Selection`，而不是继续在 BoundaryCandidate / ExecutionPlan owner path 上制造生产改动。

## 2. Existing Test Lock Review

| Safety requirement | Existing test coverage | Production gap found? | Decision |
|---|---|---|---|
| owner path 不依赖 frozen wrappers | `BoundaryCandidateServiceImplTest.ownerPathShouldNotReferenceFrozenPointOrRuntimeWrappers` reads `BoundaryCandidateServiceImpl` and plan-boundary DTO sources and asserts absence of `SourceOwnedCandidateIntegrationRuntimeCandidate`, `ReviewOnlyNumericPointProposal`, `ReviewOnlyPointProposal`, `NumericPointSafetyValidator`, and `SourceTraceNumericSourceContextDTO`. | No. Grep over production owner path returned no frozen wrapper dependency. | Keep tests. No production change. |
| display adapters 不成为 point proposal / runtime candidate owner | `DefaultExecutionPlanDisplayAdapterTest.displayAdaptersShouldNotBecomePointProposalOrRuntimeCandidateOwners` reads `DefaultExecutionPlanDisplayAdapter` and `DefaultPlanBoundaryDisplayAdapter` and asserts absence of frozen owner names. | No. Display adapters do not import or reference frozen wrappers. | Keep tests. No production change. |
| `manualReviewRequired=true` | `BoundaryCandidateServiceImplTest` asserts true across incomplete, watch-only, valid, and RiskActionGuard fallback cases; `DefaultExecutionPlanDisplayAdapterTest` asserts display mappings force true. | No. Production service/display adapters already enforce the flag. | No production change. |
| `notTradeInstruction=true` | `BoundaryCandidateServiceImplTest` asserts true across service outputs; `DefaultExecutionPlanDisplayAdapterTest.assertReviewOnlyGuardrails` asserts display not-trade behavior. | No. Production service/display adapters already enforce the flag. | No production change. |
| missing SourceTrace downgrade | `evaluateBoundaryCandidateReturnsIncompleteWhenSourceTraceMissing`, `shouldKeepExecutionPlanIncompleteWhenSourceTraceIsMissingForValidBoundary`, and source trace fallback tests cover `INCOMPLETE` / `WATCH_ONLY`. | No. Existing production paths downgrade missing or unsafe SourceTrace. | No production change. |
| missing numeric source downgrade | `evaluateBoundaryCandidateReturnsIncompleteWhenBoundarySourcesAreMissing`; `BoundaryCandidateDTOTest` requires valid factory inputs including source fields and data quality. | No. Existing service validation blocks missing numeric source values. | No production change. |
| RiskActionGuard blocking | `BoundaryCandidateServiceImplTest` covers stampede, liquidity missing, wick-only risk, and unsafe action flag -> `WATCH_ONLY`; `DefaultExecutionPlanDisplayAdapterTest` covers high risk, stampede, liquidity missing, and wick-only fallback. | No. Existing owner path already downgrades blocked risk states. | No production change. |
| `NOT_TRADE_INSTRUCTION` display reason | `DefaultExecutionPlanDisplayAdapterTest.assertReviewOnlyGuardrails` requires `NOT_TRADE_INSTRUCTION`. | No. Adapter already adds the display reason. | No production change. |
| `ENTRY_STOP_TP_RR_NOT_GENERATED` display reason | `DefaultExecutionPlanDisplayAdapterTest.assertReviewOnlyGuardrails` requires `ENTRY_STOP_TP_RR_NOT_GENERATED`. | No. Adapter already adds the display reason. | No production change. |
| no order / execution / auto-trading | `BoundaryCandidateServiceImplTest.serviceShouldNotExposeTradingExecutionMethods`, `DefaultExecutionPlanDisplayAdapterTest.adapterShouldNotExposeTradingActionAutomationOrValidFactorySurface`, and #847 forbidden semantics grep cover owner path safety. | No clear production gap. Forbidden terms seen elsewhere are mostly frozen wrapper tests and historical docs, not new owner-path dependencies. | No production change. |

## 3. Production Gap Assessment

No existing production owner-path safety semantic gap was found in this review.

`BoundaryCandidateServiceImpl` already:

- downgrades missing `SourceTrace` to `INCOMPLETE`;
- downgrades SourceTrace fallback / missing fields;
- blocks missing entry / stop / TP / RR numeric source fields;
- blocks missing `dataQualityScore`;
- downgrades RiskActionGuard backend-pending, stampede, wick-only, liquidity-missing, and unsafe action flags;
- forces `manualReviewRequired=true` and `notTradeInstruction=true` on valid and fallback candidates.

`DefaultExecutionPlanDisplayAdapter` already:

- keeps missing boundary as `BOUNDARY_PENDING`;
- maps incomplete / watch-only / invalid boundary status safely;
- requires SourceTrace for valid boundary readiness;
- downgrades missing, incomplete, watch-only, and safe-fail-closed SourceTrace;
- downgrades unsafe RiskActionGuard states;
- adds display guardrails `EXECUTION_PLAN_REVIEW_ONLY_DISPLAY`, `EXECUTION_PLAN_NOT_EXECUTABLE`, `NOT_TRADE_INSTRUCTION`, and `ENTRY_STOP_TP_RR_NOT_GENERATED`;
- forces `manualReviewRequired=true` and `notTradeInstruction=true`.

`DefaultPlanBoundaryDisplayAdapter` already:

- routes display through the existing source trace adapter;
- forces `manualReviewRequired=true` and `notTradeInstruction=true`;
- treats unsafe `VALID` read-only candidate status as incomplete unless the source trace/display path proves safety;
- adds `REVIEW_MODE:REVIEW_ONLY` and `NOT_TRADE_INSTRUCTION` display reasons.

Therefore:

- No mandatory `BoundaryCandidateServiceImpl` production change was found.
- No mandatory `DefaultExecutionPlanDisplayAdapter` production change was found.
- No mandatory `DefaultPlanBoundaryDisplayAdapter` production change was found.
- This is tests-first coverage plus existing production owner-path behavior, not a reason to modify production code.

Because there is no clear production gap, production changes are **NO-GO**.

## 4. GO / NO-GO Decision

Decision: **B. NO-GO: No production change needed; move to next minimal runtime slice selection or further source read**.

Reason:

- #846 already locks the duplicate-wrapper risk in existing owner-path tests.
- #847 already verifies those tests and confirms no production Java, no new DTO / Validator / Assembler, no P359/P360, and no frozen-wrapper dependency in owner path.
- This review found no production safety semantic that cannot already be represented by existing tests and existing owner-path behavior.
- A production edit now would be performative churn, not capability movement.

Recommended next step: `Next Minimal Runtime Slice Selection`.

Do not hard-code a production Java change just to continue the BoundaryCandidate / ExecutionPlan track. Do not revive P359 or P360. Do not add a new DTO, Validator, Assembler, runtime candidate wrapper, point proposal owner, dashboard path, endpoint, schema, config, Push, point generation, order execution, or auto-trading.

## 5. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, readiness review only
- 是否接 service/runtime/dashboard/API: No, review only
- 是否符合 #830 审计建议: Yes

## 6. Next Step Recommendation

Recommended option: **A. Next Minimal Runtime Slice Selection**.

Reason:

- The BoundaryCandidate / ExecutionPlan owner-path safety adapter track has reached a safe stopping point: owner path identified, merge design done, readiness gate done, tests-first safety lock added, verification done, and production change review returns NO-GO.
- Continuing to search for a production edit in this track would recreate the package-count trap that #830 warned about.
- The project should now choose the next smallest existing Cursor-era service/runtime/dashboard/API slice that can move toward visible `REVIEW_ONLY_RUNTIME` without adding skeleton families.

Not B yet:

- Further SourceTrace owner read can be useful later, but this review did not find it necessary before selecting the next minimal runtime slice.

Not C:

- `Minimal production owner-path safety adapter merge` is allowed only if a clear production gap is found. None was found.

Do not recommend P359, P360, new DTO, new Validator, new Assembler, Three AI, Position Monitor expansion, Push, point generation, order, execution, or auto-trading.
