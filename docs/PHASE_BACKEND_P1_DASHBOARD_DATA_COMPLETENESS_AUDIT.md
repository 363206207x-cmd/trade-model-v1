# PHASE_BACKEND_P1_DASHBOARD_DATA_COMPLETENESS_AUDIT

## 1. Audit Purpose

This document records the BACKEND-P1 read-only audit for dashboard data completeness.

The audit reviews the current dashboard backend, DTO contracts, display adapters, local fixture packs, and homepage consumption paths before any backend implementation package is allowed to fill new dashboard fields.

This package is documentation-only.

Hard boundaries:

- Do not modify `dashboard.html`.
- Do not modify Java production code.
- Do not modify tests.
- Do not modify schema, config, or dashboard assets.
- Do not add external data integrations.
- Do not add Coinglass integration.
- Do not add order API integration.
- Do not add auto-trading.
- Do not fabricate dashboard fields.
- Keep `BoundaryCandidate VALID` as manual-review / not-trade-instruction.
- Missing `SourceTrace` or derivatives-risk context must fail closed.

## 2. Repository Context

| Item | Value |
|---|---|
| Target PR | #80 |
| Target issue | #79 |
| Target branch | `codex/backend-p1-dashboard-data-audit` |
| Issue baseline | `da321e0 docs(dashboard): freeze current stable homepage` |
| PR branch audit start | `e63f96c chore(backend): create BACKEND-P1 cloud trigger entry` |
| Final package type | Read-only audit plus documentation |
| Temporary trigger artifact | Removed from final package |

## 3. Files Inspected

| Area | Files inspected |
|---|---|
| Dashboard API | `src/main/java/org/example/trademodel/controller/DashboardController.java`, `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java` |
| Dashboard frontend | `src/main/resources/templates/dashboard.html` |
| PlanBoundary display | `DefaultPlanBoundaryDisplayAdapter.java`, `DefaultPlanBoundarySourceTraceAdapter.java`, `PlanBoundaryDisplayAdapter.java`, `PlanBoundarySourceTraceAdapter.java` |
| ExecutionPlan display | `DefaultExecutionPlanDisplayAdapter.java`, `ExecutionPlanDisplayAdapter.java`, `PlanServiceImpl.java`, `ExecutionPlanVO.java` |
| RiskActionGuard display | `DefaultRiskActionGuardDisplayAdapter.java`, `RiskActionGuardDisplayAdapter.java` |
| Paper observation display | `DefaultPaperObservationDisplayAdapter.java`, `PaperObservationDisplayAdapter.java` |
| Source contracts | `SourceTraceDTO.java`, `DerivativesRiskContextDTO.java`, `RuntimeKlineContextDTO.java`, `SourceTraceFallbackStatusEnum.java`, `SourceCompletenessContract.java` |
| Boundary candidate | `BoundaryCandidateDTO.java`, `BoundaryEntryDTO.java`, `BoundaryStopDTO.java`, `BoundaryTakeProfitLevelDTO.java`, `BoundarySourceFieldsDTO.java`, `BoundaryCandidateServiceImpl.java`, `DefaultSourceAssembler.java` |
| Position read model | `DecisionResultVO.java`, `DecisionServiceImpl.java`, `RealPositionVO.java`, `RealPositionMapper.java` |
| Push / Recheck naming | `RecheckStatusEnum.java`, `PushRecheckStatusContract.java` |
| Tests as evidence | `DashboardControllerTest.java`, `DashboardDetailResponseVOTest.java`, `DefaultPlanBoundarySourceTraceAdapterTest.java`, `DefaultPlanBoundaryDisplayAdapterTest.java`, `DefaultExecutionPlanDisplayAdapterTest.java`, `DefaultRiskActionGuardDisplayAdapterTest.java`, `DefaultPaperObservationDisplayAdapterTest.java`, `BoundaryCandidateServiceImplTest.java`, `P17LocalFixtureFailClosedTest.java`, `P18DerivativesRiskContextFixtureExtensionTest.java`, `PlanServiceImplTest.java`, `RuleEngineServiceSourceTraceTest.java` |
| Prior docs | `PHASE_P17_LOCAL_FIXTURE_FAIL_CLOSED_TEST_PACK.md`, `PHASE_P17_LOCAL_FIXTURE_FAIL_CLOSED_TEST_RESULT.md`, `PHASE_P18_DERIVATIVES_RISK_CONTEXT_FIXTURE_EXTENSION_PACK.md`, `PHASE_P18_DERIVATIVES_RISK_CONTEXT_FIXTURE_EXTENSION_RESULT.md`, `PHASE_HOME_FREEZE_DASHBOARD_CURRENT_STABLE_HOMEPAGE.md`, `PHASE_PLAN_BOUNDARY_EXECUTIONPLAN_BACKEND_FIELD_INTEGRATION_PLAN.md` |

## 4. Dashboard Detail Current Data Flow

`DashboardController#dashboardDetail` currently:

1. Creates `DashboardDetailResponseVO.withSafeDefaultDisplays()`.
2. Loads the latest `DecisionResultVO` by symbol.
3. Builds `PlanBoundaryDisplayVO` through `PlanBoundaryDisplayAdapter`.
4. Builds `ExecutionPlanDisplayVO` through the simple `ExecutionPlanDisplayAdapter` overload.
5. Builds `RiskActionGuardDisplayVO` from decision, PlanBoundary display, and ExecutionPlan display.
6. Builds `PaperObservationDisplayVO`.
7. Adds `marketEnvironmentMini`, `evidenceTopItems`, and `scoreTopItems`.

Important audit finding:

- `DefaultExecutionPlanDisplayAdapter` has overloads that can consume `SourceTraceDTO` and `RiskActionGuardDisplayVO`.
- The dashboard controller currently calls the simple overload and does not pass a production `SourceTraceDTO`.
- `DefaultPlanBoundarySourceTraceAdapter` intentionally returns `INCOMPLETE / MISSING` and records missing BoundaryCandidate / RuntimeKlineContext source trace inputs.
- Therefore `/api/dashboard/detail` is safe and fail-closed, but it is not yet a complete production SourceTrace-backed dashboard.

## 5. Completeness Classification Legend

| Label | Meaning |
|---|---|
| `REAL` | Backed by current service / read model data and exposed to dashboard. |
| `PARTIAL` | Structure or partial data exists, but production completeness is not proven. |
| `PLACEHOLDER` | Safe display placeholder or frontend empty state. |
| `MISSING` | Required production data path is absent from dashboard detail. |
| `FIXTURE_ONLY` | Covered by local tests or docs but not wired into dashboard production response. |
| `SAFE_FAIL_CLOSED` | Intentionally blocks execution semantics and remains manual-review only. |

## 6. Module Completeness Matrix

| Module | Current dashboard status | Classification | Risk if implemented directly | Required next package |
|---|---|---|---|---|
| PlanBoundary display object | `PlanBoundaryDisplayVO` exists with `planBoundaryStatus`, label, source trace status, backend connection status, incomplete reasons, blocking reasons, `manualReviewRequired`, `notTradeInstruction`. | `SAFE_FAIL_CLOSED` / `PARTIAL` | Treating display status as production BoundaryCandidate can bypass source trace. | BACKEND-P2 should wire real source trace only after explicit contract checks. |
| PlanBoundary source trace adapter | `DefaultPlanBoundarySourceTraceAdapter` returns `INCOMPLETE`, `MISSING`, `PARTIAL`, and blocking reasons for missing source trace input. | `SAFE_FAIL_CLOSED` | Replacing it with optimistic `VALID` would fabricate boundary completeness. | Add production adapter only when source trace and runtime kline context reach dashboard detail. |
| BoundaryCandidateDTO / service | DTO, valid factory, `BoundaryCandidateServiceImpl`, and fallback checks exist. | `PARTIAL` | Service-level VALID could be mistaken for dashboard production readiness. | Keep VALID as candidate only; require dashboard wiring audit before exposing as display truth. |
| RuntimeKlineContext / SourceAssembler | DTO and `DefaultSourceAssembler` exist and can assemble `SourceTraceDTO` from runtime and derivatives context. | `PARTIAL` / `FIXTURE_ONLY` for dashboard | Dashboard detail does not currently pass assembled source trace into display adapters. | Add a controller/service integration package after field contract review. |
| DerivativesRiskContextDTO | DTO exists with OI, funding, liquidation, leverage, long/short, liquidity, event, wick fields. | `PARTIAL` / `FIXTURE_ONLY` | DTO presence can be mistaken for live derivatives data. | Keep external data integration out until missing-data fallbacks are tested. |
| SourceTraceDTO | Has required boundary source checks for entry, stop, TP, RR, liquidity, multi-timeframe, event, wick. | `PARTIAL` / `FIXTURE_ONLY` for dashboard | Complete source trace in tests must not imply `/api/dashboard/detail` completeness. | Wire only through explicit backend field integration package. |
| ExecutionPlan display | `ExecutionPlanDisplayVO` exists; default is `BOUNDARY_PENDING`; adapter remains review-only and has SourceTrace/RiskGuard overloads. | `SAFE_FAIL_CLOSED` / `PARTIAL` | Dashboard controller currently does not use the stricter overload. | BACKEND-P2/P3 should pass `SourceTraceDTO` and RiskActionGuard state explicitly if exposed. |
| PlanService / ExecutionPlanVO | `PlanServiceImpl` supports SourceTrace and RiskActionGuard readiness, but generates advisory plan semantics. | `SAFE_FAIL_CLOSED` / `PARTIAL` | `READY_REVIEW_ONLY` could be misread as executable if UI/backend naming is careless. | Preserve advisory-only language and explicit non-executable reason. |
| RiskActionGuard display | Adapter enforces all action flags false, liquidity pending default, manual risk review true, not-trade-instruction true. | `SAFE_FAIL_CLOSED` | If future liquidity state is populated without source provenance, push/reverse/new-position flags could be abused. | Add source-backed liquidity/stampede/wick checks before changing status. |
| Paper observation display | Adapter remains fail-closed, not real position, not trade instruction, manual review required. | `SAFE_FAIL_CLOSED` | Paper observation could be confused with real position if copy changes. | Keep labels review-only and not-real-position. |
| Open position monitoring | `DecisionServiceImpl` enriches decision rows from `RealPositionMapper`; dashboard consumes `hasOpenPosition`, `positionSide`, `avgOpenPrice`, `positionOpenTime`, `positionQuantity`, `markPrice`, `unrealizedPnlPct`, `positionStatus`. | `REAL` / `PARTIAL` | Position display is real read-model enrichment but not an execution instruction. | Keep direction/risk display read-only; do not infer close/reverse. |
| AI role convergence | `DecisionResultVO` exposes `aiRoleResults`, `aiPlanMode`, `aiConflictLevel`, `aiConflictScore`, `confusedScore`; dashboard renders structured and raw fallback states. | `REAL` / `PARTIAL` | AI convergence could be mistaken for source trace completeness. | Keep AI state separate from PlanBoundary and RiskActionGuard gates. |
| MarketEnvironment mini | Snapshot path and real quote heuristic fallback exist; placeholder fallback exists. | `REAL` / `PARTIAL` | Heuristic market environment is not derivatives risk context. | Label heuristic and placeholder sources clearly. |
| Evidence / score lists | Dashboard detail exposes top evidence and score items by analysis id. | `REAL` when analysis id exists; otherwise safe empty. | Evidence/score cannot replace missing SourceTrace. | Keep as supporting context only. |
| Push / Recheck naming | `VALID_EXECUTABLE`, `RECHECK_VALID_EXECUTABLE`, `PASS`, `successCount`, and `executionStatus` naming exists in related modules. | `PARTIAL` with naming risk | Names can imply execution even when system is review-only. | Follow P11A correction themes before surfacing as action authority. |
| Dashboard frontend consumption | Homepage consumes display objects and has explicit non-trading / fail-closed copy. | `PLACEHOLDER` / `SAFE_FAIL_CLOSED` | Frontend labels can drift if backend starts returning optimistic statuses. | Keep copy aligned with backend non-goals. |

## 7. PlanBoundary Field Audit

| Field | Current source | Dashboard classification | Notes |
|---|---|---|---|
| `planBoundaryStatus` | `DefaultPlanBoundaryDisplayAdapter` and source trace adapter fallback. | `SAFE_FAIL_CLOSED` | Defaults to `BACKEND_PENDING` or `INCOMPLETE`, not production VALID. |
| `planBoundaryStatusLabel` | Display adapter labels. | `SAFE_FAIL_CLOSED` | Human-readable status only. |
| `sourceTraceStatus` | `DefaultPlanBoundarySourceTraceAdapter`. | `MISSING` / `SAFE_FAIL_CLOSED` | Currently marks `MISSING` when decision exists. |
| `backendConnectionStatus` | Display adapter. | `PARTIAL` / `BACKEND_PENDING` | Does not mean source trace production is connected. |
| Entry readiness | Not exposed as real numeric field in dashboard detail display object. | `MISSING` | Current frontend states it must not be forged. |
| Stop readiness | Not exposed as real numeric field in dashboard detail display object. | `MISSING` | Requires source-backed assembly. |
| TP readiness | Not exposed as real numeric field in dashboard detail display object. | `MISSING` | Requires source-backed assembly. |
| `manualReviewRequired` | Display defaults and adapter enforcement. | `REAL` safety default | Must remain true. |
| `notTradeInstruction` | Display defaults and adapter enforcement. | `REAL` safety default | Must remain true. |

## 8. ExecutionPlan Field Audit

| Field | Current source | Dashboard classification | Notes |
|---|---|---|---|
| `executionPlanStatus` | `DefaultExecutionPlanDisplayAdapter`. | `SAFE_FAIL_CLOSED` / `PARTIAL` | Defaults to `BOUNDARY_PENDING`; can become `READY_REVIEW_ONLY`, never execution. |
| `executionPlanStatusLabel` | Adapter label. | `SAFE_FAIL_CLOSED` | UI label only. |
| `executionPlanBoundaryAligned` | Adapter boolean. | `PARTIAL` | True only in review-only branch, not an execution gate. |
| `notExecutableReason` | Adapter and VO defaults. | `REAL` safety field | Defaults to pending or manual review reason. |
| `manualReviewRequired` | Adapter enforcement. | `REAL` safety default | Always true in adapter. |
| `notTradeInstruction` | Adapter enforcement. | `REAL` safety default | Always true in adapter. |
| Readiness from SourceTrace | Adapter overload and `PlanServiceImpl` support it. | `PARTIAL` / `NOT WIRED TO DETAIL` | Controller currently does not pass `SourceTraceDTO` to execution adapter. |
| Actual execution plan | Not generated by dashboard detail. | `MISSING` by design | Must remain deferred. |

## 9. RiskActionGuard Field Audit

| Field | Current source | Dashboard classification | Notes |
|---|---|---|---|
| `riskActionGuardStatus` | `DefaultRiskActionGuardDisplayAdapter`. | `SAFE_FAIL_CLOSED` | Defaults to `BACKEND_PENDING`; manual review if upstream gates pass. |
| `riskActionGuardStatusLabel` | Adapter label. | `SAFE_FAIL_CLOSED` | Display only. |
| `liquidityState` | Display default unless future data populates it. | `MISSING` / `SAFE_FAIL_CLOSED` | Defaults `BACKEND_PENDING`. |
| `opportunityPushAllowed` | Adapter always sets false. | `REAL` safety flag | No push authorization. |
| `reverseTradeAllowed` | Adapter always sets false. | `REAL` safety flag | No reverse authorization. |
| `newPositionAllowed` | Adapter always sets false. | `REAL` safety flag | No new position authorization. |
| `marketOrderExitAllowed` | Adapter always sets false. | `REAL` safety flag | No market exit authorization. |
| `manualRiskReviewRequired` | Adapter always sets true. | `REAL` safety default | Must remain true. |
| `notTradeInstruction` | Adapter always sets true. | `REAL` safety default | Must remain true. |

## 10. Open Position Monitoring Audit

| Field | Current source | Dashboard classification | Notes |
|---|---|---|---|
| `hasOpenPosition` | `DecisionServiceImpl` enriches from `RealPositionMapper.findOpenPositions()`. | `REAL` when position table is available; false fallback otherwise | Absence is treated as no observed open position, not a business close conclusion. |
| `positionStatus` | Real position read model enrichment. | `REAL` / `PARTIAL` | Dashboard checks `OPEN` and side/open price before showing active position. |
| `positionSide` | Real position read model enrichment. | `REAL` / `PARTIAL` | Display-only; cannot imply reverse trade. |
| `avgOpenPrice` | Real position read model enrichment. | `REAL` / `PARTIAL` | Display-only. |
| `positionOpenTime` | Real position read model enrichment. | `REAL` / `PARTIAL` | Display-only. |
| `positionQuantity` | Real position read model enrichment. | `REAL` / `PARTIAL` | Display-only. |
| `markPrice` | Real position read model enrichment or latest price fallback in frontend display. | `REAL` / `PARTIAL` | Not a liquidation or order trigger. |
| `unrealizedPnlPct` | Real position read model enrichment. | `REAL` / `PARTIAL` | Risk display only; high risk does not mean direct close. |

## 11. AI Role Convergence Audit

| Field | Current source | Dashboard classification | Notes |
|---|---|---|---|
| `aiRoleResults` | `DecisionResultVO` read model. | `REAL` / `PARTIAL` | Dashboard renders raw role output when present. |
| `aiPlanMode` | `DecisionResultVO` read model. | `REAL` / `PARTIAL` | Does not bypass SourceTrace. |
| `aiConflictLevel` | `DecisionResultVO` read model. | `REAL` / `PARTIAL` | Used for review focus; not a trading gate. |
| `aiConflictScore` | `DecisionResultVO` read model. | `REAL` / `PARTIAL` | Review-only display context. |
| `confusedScore` | `DecisionResultVO` read model and system status count path. | `REAL` / `PARTIAL` | Does not imply invalidation or reverse action. |

## 12. SourceTrace / DerivativesRiskContext Audit

| Contract area | Current status | Dashboard reachability | Notes |
|---|---|---|---|
| Entry source | DTO and SourceAssembler support exists. | Not directly exposed on `/api/dashboard/detail`. | Missing entry source must remain `INCOMPLETE` / `WATCH_ONLY`. |
| Stop source | DTO and SourceAssembler support exists. | Not directly exposed on `/api/dashboard/detail`. | Missing stop source must remain fallback. |
| TP source | DTO and SourceAssembler support exists. | Not directly exposed on `/api/dashboard/detail`. | Missing TP source must remain fallback. |
| RR source | DTO and SourceAssembler support exists. | Not directly exposed on `/api/dashboard/detail`. | RR cannot be inferred without entry / stop / TP. |
| Liquidity source | DTO field exists. | RiskActionGuard display defaults to pending. | Missing liquidity must fail closed. |
| Multi-timeframe source | DTO field exists. | Not directly exposed. | Missing MTF context must not infer convergence. |
| Event source | DTO field exists. | Not directly exposed. | Missing event blocker must keep review-only. |
| Wick source | DTO field exists. | Not directly exposed. | Wick-only cannot be interpreted as trend reversal. |
| OI / funding / liquidation / leverage / long-short | `DerivativesRiskContextDTO` fields exist. | Not production wired to dashboard detail. | Current coverage is contract/test-level, not live external data integration. |

## 13. Frontend Consumption Audit

| Frontend area | Consumed backend fields | Classification | Notes |
|---|---|---|---|
| Module status board | Static UI status copy. | `PLACEHOLDER` | States PlanBoundary and Entry / Stop / TP are not fully connected. |
| Display status cards | `planBoundaryDisplay`, `executionPlanDisplay`, `riskActionGuardDisplay`, `paperObservationDisplay`. | `SAFE_FAIL_CLOSED` | Copy says non-trading, manual review, no entry/stop/TP generation. |
| Main workbench | PlanBoundary, SourceTrace, ExecutionPlan, RiskActionGuard, market environment, decision fields. | `PARTIAL` / `SAFE_FAIL_CLOSED` | Clear safety line: VALID is review candidate and execution readiness is not auto-execution. |
| Position section | `DecisionResultVO` position fields. | `REAL` / `PARTIAL` | Uses explicit real-position fields, not `isAdopted`. |
| AI section | AI role and conflict fields. | `REAL` / `PARTIAL` | Fallback copy says AI missing must not infer opportunity or trade action. |
| Execution summary detail | Legacy decision execution text. | `PARTIAL` | Text-only summary is not structured ExecutionPlan. |

## 14. Directly Missing Or Incomplete Data

| Missing / incomplete data | Affected modules | Required fallback |
|---|---|---|
| Production SourceTrace assembly into dashboard detail | PlanBoundary display, ExecutionPlan display, RiskActionGuard readiness, main workbench | `INCOMPLETE` / `WATCH_ONLY` / `SAFE_FAIL_CLOSED` |
| RuntimeKlineContext reachability in dashboard detail | PlanBoundary SourceTrace adapter, BoundaryCandidate display readiness | `INCOMPLETE` |
| DerivativesRiskContext live data | RiskActionGuard, SourceTrace, ExecutionPlan readiness | `SAFE_FAIL_CLOSED` / `WATCH_ONLY` |
| Coinglass / external derivatives data | OI, funding, liquidation, leverage, long-short, liquidity stress | Not integrated; do not infer trading action |
| Event window blocker | RiskActionGuard, ExecutionPlan readiness | `WATCH_ONLY` / `SAFE_FAIL_CLOSED` |
| Wick confirmation source | RiskActionGuard, BoundaryCandidate validation | `WATCH_ONLY` / `SAFE_FAIL_CLOSED` |
| Structured entry / stop / TP dashboard fields | PlanBoundary display, frontend status cards | `INCOMPLETE`; do not fabricate |

## 15. Risks If Implemented Without This Audit

- Returning `planBoundaryStatus = VALID` without passing complete source trace into dashboard detail would fabricate completeness.
- Treating `READY_REVIEW_ONLY` as executable would violate the current ExecutionPlan safety contract.
- Populating liquidity or derivatives fields without source metadata would weaken RiskActionGuard fail-closed behavior.
- Showing text-only `entryZone`, `stopLoss`, or `takeProfitRules` as structured boundary sources would bypass SourceTrace.
- Interpreting `VALID_EXECUTABLE`, `RECHECK_VALID_EXECUTABLE`, `valid=true`, `PASS`, `successCount`, or `executionStatus` as execution authorization would conflict with P11A.
- Treating open position risk as direct close / reverse signal would violate Risk Action Guard.

## 16. Recommended Next Implementation Package

Recommended BACKEND-P2 scope:

1. Add a controller/service read-only assembly boundary for `SourceTraceDTO` and `DerivativesRiskContextDTO` into dashboard detail.
2. Keep all missing sources as `INCOMPLETE`, `WATCH_ONLY`, or `SAFE_FAIL_CLOSED_ONLY`.
3. Pass `SourceTraceDTO` into the stricter `DefaultExecutionPlanDisplayAdapter` overload.
4. Preserve `manualReviewRequired = true` and `notTradeInstruction = true`.
5. Add dashboard API tests for fail-closed response shape.
6. Do not modify `dashboard.html` until backend field truth is stable.

Recommended non-scope for BACKEND-P2:

- No external API or Coinglass integration.
- No order API.
- No auto-trading.
- No schema migration unless a separate reviewed package explicitly requires it.
- No production trading action flags.

## 17. Explicit Non-Goals For BACKEND-P1

This package does not:

- Implement BACKEND-P2.
- Implement P19.
- Modify `dashboard.html`.
- Modify Java production code.
- Modify Java tests.
- Modify schema.
- Modify config.
- Add external data integration.
- Add Coinglass integration.
- Add order API integration.
- Add automated trading.
- Generate execution plans.
- Declare dashboard data completeness complete.

## 18. Verification

Commands used for read-only audit:

- `rg --files`
- `rg`
- `sed -n`
- `git status --short`
- `git log --oneline`

Tests:

- Not run. This package is documentation-only and intentionally does not modify Java, templates, schema, config, or tests.

## 19. Current Conclusion

BACKEND-P1 confirms that the dashboard is currently safe and fail-closed, but not yet complete for production SourceTrace-backed PlanBoundary / ExecutionPlan / RiskActionGuard data.

The current implementation has strong DTO, fixture, adapter, and safety-default foundations. The main gap is dashboard detail reachability: the production `/api/dashboard/detail` path does not yet assemble and pass full `SourceTraceDTO` / `DerivativesRiskContextDTO` into the display stack.

Therefore:

- PlanBoundary display remains `INCOMPLETE` / `MISSING` / `BACKEND_PENDING` rather than production VALID.
- ExecutionPlan display remains review-only / advisory and must not become executable.
- RiskActionGuard remains fail-closed, with action flags false.
- Open position monitoring fields are available through the decision read model enrichment, but remain display-only.
- AI role convergence is review context only and cannot bypass source trace.

BACKEND-P2 should be a separate, reviewed implementation package after this audit baseline is merged.
