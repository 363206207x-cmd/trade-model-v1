# PHASE_BACKEND_P22_SOURCETRACE_TAKEPROFIT_SOURCE_OWNERSHIP_SKELETON_RESULT

## 1. Document Purpose

This document records the BACKEND-P22 SourceTrace TakeProfit Source Ownership Skeleton Pack result.

Issue context:

- `#133 BACKEND-P22 SourceTrace TakeProfit Source Ownership Skeleton Pack`

PR context:

- `#134 BACKEND-P22 trigger: take-profit source ownership skeleton`

Baseline:

- `a38ad85 feat(backend): add stop source ownership skeleton`

BACKEND-P22 creates the third narrow SourceTrace source-family skeleton for take-profit source ownership.

It is intentionally fail-closed and does not generate real TP prices, infer TP from entry ownership, infer TP from stop ownership, or complete SourceTrace.

## 2. Files Changed

Production skeleton files:

- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceTakeProfitSourceOwnershipResult.java`
- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceTakeProfitSourceOwnershipStatusEnum.java`
- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceTakeProfitSourceMissingReasonEnum.java`
- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceTakeProfitSourceReviewModeEnum.java`
- `src/main/java/org/example/trademodel/service/SourceTraceTakeProfitSourceOwnershipService.java`
- `src/main/java/org/example/trademodel/service/impl/FailClosedSourceTraceTakeProfitSourceOwnershipService.java`

Tests:

- `src/test/java/org/example/trademodel/service/impl/SourceTraceTakeProfitSourceOwnershipServiceTest.java`
- `src/test/java/org/example/trademodel/service/impl/DefaultSourceAssemblerTest.java`
- `src/test/java/org/example/trademodel/dto/planboundary/SourceTraceDerivativesRiskContextDTOTest.java`

Documentation:

- `docs/PHASE_BACKEND_P22_SOURCETRACE_TAKEPROFIT_SOURCE_OWNERSHIP_SKELETON_RESULT.md`

## 3. Skeleton Fields

`SourceTraceTakeProfitSourceOwnershipResult` exposes only review-only ownership metadata:

- `symbol`
- `timeframe`
- `ownershipStatus=INCOMPLETE`
- `missingReason=MISSING_SOURCE`
- `reviewMode=REVIEW_ONLY`
- `tpPriceSources=[]`
- `tpSourceType=null`
- `tpSourceTimeframe=null`
- `tpSourceReason=null`
- `tpSourceRef=null`
- `missingFields=[tpPriceSources, tpSourceType, tpSourceTimeframe, tpSourceReason, tpSourceRef]`
- `manualReviewRequired=true`
- `notTradeInstruction=true`

The skeleton does not expose order, execution, close, reverse, or auto-trading methods.

## 4. Fail-Closed Behavior

`FailClosedSourceTraceTakeProfitSourceOwnershipService` always returns:

- `ownershipStatus=INCOMPLETE`
- `missingReason=MISSING_SOURCE`
- `reviewMode=REVIEW_ONLY`
- empty TP price source list,
- null TP source ownership fields,
- `manualReviewRequired=true`,
- `notTradeInstruction=true`.

RuntimeKline metadata may provide symbol/timeframe context, but it is not interpreted as TP ownership.

RuntimeKline `latestPrice` is not copied into `tpPriceSources`.

RuntimeKline `klineItems` are not interpreted as TP source evidence.

The entry source ownership skeleton is not interpreted as TP source evidence.

The stop source ownership skeleton is not interpreted as TP source evidence.

## 5. Guard Assertions Added

BACKEND-P22 adds focused tests proving:

- default TP ownership output is `INCOMPLETE / MISSING_SOURCE / REVIEW_ONLY`,
- TP source fields remain empty/null by default,
- RuntimeKline `latestPrice` does not become TP source,
- RuntimeKline `klineItems` do not become TP source,
- entry source skeleton output does not become TP source,
- stop source skeleton output does not become TP source,
- SourceTrace remains `INCOMPLETE` when TP ownership is still missing,
- SourceTrace `hasRequiredBoundarySources()` remains false by default,
- `manualReviewRequired=true` and `notTradeInstruction=true` remain true.

## 6. Still-Unwired Fields

The following SourceTrace TP ownership fields remain unwired and must not be inferred:

- `tpPriceSources`
- `tpSourceType`
- `tpSourceTimeframe`
- `tpSourceReason`
- `tpSourceRef`

The following broader SourceTrace families also remain outside BACKEND-P22:

- risk-reward source ownership,
- liquidity source ownership,
- multi-timeframe source ownership,
- event source ownership,
- wick source ownership.

Entry and stop ownership remain fail-closed skeletons and do not provide TP ownership.

## 7. Boundary Confirmation

BACKEND-P22 confirms:

- no real TP price generation,
- no inference from RuntimeKline `latestPrice`,
- no inference from RuntimeKline `klineItems`,
- no inference from entry source skeleton,
- no inference from stop source skeleton,
- no SourceTrace completion,
- no BoundaryCandidateService `VALID` production wiring,
- no ExecutionPlan readiness upgrade,
- no external data integration,
- no Coinglass integration,
- no order API,
- no auto-trading,
- no schema change,
- no `dashboard.html` change.

## 8. Risk Action Guard Reminder

Risk Action Guard remains separate from TP ownership.

- High risk does not directly mean close, reverse, or open.
- Wick / pin-bar evidence does not confirm trend reversal.
- Stampede / liquidity stress must block opportunity push and require review.

## 9. Tests Run

Commands:

```bash
./mvnw -q -Dtest=SourceTraceTakeProfitSourceOwnershipServiceTest test
./mvnw -q -Dtest=DefaultSourceAssemblerTest test
./mvnw -q -Dtest=SourceTraceDerivativesRiskContextDTOTest test
./mvnw -q -Dtest=BoundaryCandidateServiceImplTest test
./mvnw -q -Dtest=DefaultExecutionPlanDisplayAdapterTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

Result:

- PASS `SourceTraceTakeProfitSourceOwnershipServiceTest`
- PASS `DefaultSourceAssemblerTest`
- PASS `SourceTraceDerivativesRiskContextDTOTest`
- PASS `BoundaryCandidateServiceImplTest`
- PASS `DefaultExecutionPlanDisplayAdapterTest`
- PASS compile
- PASS test-compile

## 10. Current Conclusion

BACKEND-P22 adds a rollbackable take-profit source ownership skeleton that fails closed by default.

It gives future backend work a narrow contract surface without allowing RuntimeKline visibility, entry skeleton output, stop skeleton output, or kline history to become a TP source or allowing SourceTrace to appear complete.
