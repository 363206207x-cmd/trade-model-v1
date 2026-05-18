# PHASE_BACKEND_P23_SOURCETRACE_RISKREWARD_SOURCE_OWNERSHIP_SKELETON_RESULT

## 1. Document Purpose

This document records the BACKEND-P23 SourceTrace RiskReward Source Ownership Skeleton Pack result.

Issue context:

- `#135 BACKEND-P23 SourceTrace RiskReward Source Ownership Skeleton Pack`

PR context:

- `#136 BACKEND-P23 trigger: RR skeleton`

Baseline:

- `2b8c05e feat(backend): add take-profit source ownership skeleton`

BACKEND-P23 creates the fourth narrow SourceTrace source-family skeleton for risk-reward source ownership.

It is intentionally fail-closed and does not calculate real RR values, infer RR from entry ownership, infer RR from stop ownership, infer RR from TP ownership, or complete SourceTrace.

## 2. Files Changed

Production skeleton files:

- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceRiskRewardSourceOwnershipResult.java`
- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceRiskRewardSourceOwnershipStatusEnum.java`
- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceRiskRewardSourceMissingReasonEnum.java`
- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceRiskRewardSourceReviewModeEnum.java`
- `src/main/java/org/example/trademodel/service/SourceTraceRiskRewardSourceOwnershipService.java`
- `src/main/java/org/example/trademodel/service/impl/FailClosedSourceTraceRiskRewardSourceOwnershipService.java`

Tests:

- `src/test/java/org/example/trademodel/service/impl/SourceTraceRiskRewardSourceOwnershipServiceTest.java`
- `src/test/java/org/example/trademodel/service/impl/DefaultSourceAssemblerTest.java`
- `src/test/java/org/example/trademodel/dto/planboundary/SourceTraceDerivativesRiskContextDTOTest.java`

Documentation:

- `docs/PHASE_BACKEND_P23_SOURCETRACE_RISKREWARD_SOURCE_OWNERSHIP_SKELETON_RESULT.md`

## 3. Skeleton Fields

`SourceTraceRiskRewardSourceOwnershipResult` exposes only review-only ownership metadata:

- `symbol`
- `timeframe`
- `ownershipStatus=INCOMPLETE`
- `missingReason=MISSING_SOURCE`
- `reviewMode=REVIEW_ONLY`
- `rrSource=null`
- `rrRuleRef=null`
- `missingFields=[rrSource, rrRuleRef]`
- `manualReviewRequired=true`
- `notTradeInstruction=true`

The skeleton does not expose order, execution, close, reverse, or auto-trading methods.

## 4. Fail-Closed Behavior

`FailClosedSourceTraceRiskRewardSourceOwnershipService` always returns:

- `ownershipStatus=INCOMPLETE`
- `missingReason=MISSING_SOURCE`
- `reviewMode=REVIEW_ONLY`
- null RR source ownership fields,
- `manualReviewRequired=true`,
- `notTradeInstruction=true`.

RuntimeKline metadata may provide symbol/timeframe context, but it is not interpreted as RR ownership.

RuntimeKline `latestPrice` is not used to calculate `rrSource`.

RuntimeKline `klineItems` are not interpreted as RR source evidence.

The entry source ownership skeleton is not interpreted as RR source evidence.

The stop source ownership skeleton is not interpreted as RR source evidence.

The TP source ownership skeleton is not interpreted as RR source evidence.

## 5. Guard Assertions Added

BACKEND-P23 adds focused tests proving:

- default RR ownership output is `INCOMPLETE / MISSING_SOURCE / REVIEW_ONLY`,
- RR source fields remain null by default,
- RuntimeKline `latestPrice` does not become RR source,
- RuntimeKline `klineItems` do not become RR source,
- entry source skeleton output does not become RR source,
- stop source skeleton output does not become RR source,
- TP source skeleton output does not become RR source,
- SourceTrace remains `INCOMPLETE` when RR ownership is still missing,
- SourceTrace `hasRequiredBoundarySources()` remains false by default,
- `manualReviewRequired=true` and `notTradeInstruction=true` remain true.

## 6. Still-Unwired Fields

The following SourceTrace RR ownership fields remain unwired and must not be inferred:

- `rrSource`
- `rrRuleRef`

The following broader SourceTrace families also remain outside BACKEND-P23:

- liquidity source ownership,
- multi-timeframe source ownership,
- event source ownership,
- wick source ownership.

Entry, stop, and TP ownership remain fail-closed skeletons and do not provide RR ownership.

## 7. Boundary Confirmation

BACKEND-P23 confirms:

- no real RR calculation,
- no inference from RuntimeKline `latestPrice`,
- no inference from RuntimeKline `klineItems`,
- no inference from entry source skeleton,
- no inference from stop source skeleton,
- no inference from TP source skeleton,
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

Risk Action Guard remains separate from RR ownership.

- High risk does not directly mean close, reverse, or open.
- Wick / pin-bar evidence does not confirm trend reversal.
- Stampede / liquidity stress must block opportunity push and require review.

## 9. Tests Run

Commands:

```bash
./mvnw -q -Dtest=SourceTraceRiskRewardSourceOwnershipServiceTest test
./mvnw -q -Dtest=DefaultSourceAssemblerTest test
./mvnw -q -Dtest=SourceTraceDerivativesRiskContextDTOTest test
./mvnw -q -Dtest=BoundaryCandidateServiceImplTest test
./mvnw -q -Dtest=DefaultExecutionPlanDisplayAdapterTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

Result:

- PASS `./mvnw -q -Dtest=SourceTraceRiskRewardSourceOwnershipServiceTest test`
- PASS `./mvnw -q -Dtest=DefaultSourceAssemblerTest test`
- PASS `./mvnw -q -Dtest=SourceTraceDerivativesRiskContextDTOTest test`
- PASS `./mvnw -q -Dtest=BoundaryCandidateServiceImplTest test`
- PASS `./mvnw -q -Dtest=DefaultExecutionPlanDisplayAdapterTest test`
- PASS `./mvnw -q -DskipTests compile`
- PASS `./mvnw -q -DskipTests test-compile`

## 10. Current Conclusion

BACKEND-P23 adds a rollbackable risk-reward source ownership skeleton that fails closed by default.

It gives future backend work a narrow contract surface without allowing RuntimeKline visibility, entry skeleton output, stop skeleton output, TP skeleton output, or kline history to become an RR source or allowing SourceTrace to appear complete.
