# PHASE_P18_DERIVATIVES_RISK_CONTEXT_FIXTURE_EXTENSION_RESULT

## 1. Result Object

This document records the expected result of the P18 DerivativesRiskContext / SourceTrace Fixture Extension Pack.

The focused test class is:

- `src/test/java/org/example/trademodel/service/P18DerivativesRiskContextFixtureExtensionTest.java`

The fixture catalog is:

- `src/test/resources/planboundary/p18-derivatives-risk-fixture-extension-cases.csv`

## 2. Verification Commands

Focused commands for P18:

```bash
./mvnw -q -Dtest=P18DerivativesRiskContextFixtureExtensionTest test
./mvnw -q -Dtest=P17LocalFixtureFailClosedTest,P18DerivativesRiskContextFixtureExtensionTest test
./mvnw -q -Dtest=BoundaryCandidateServiceImplTest,DefaultSourceAssemblerTest,DefaultExecutionPlanDisplayAdapterTest,RuleEngineServiceSourceTraceTest,SourceTraceDerivativesRiskContextDTOTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

## 3. Expected Result Matrix

| Fixture Group | Expected Result |
|---|---|
| complete derivatives risk context | SourceTrace complete, BoundaryCandidate may become `VALID`, still review-only |
| missing OI / Funding / liquidation / leverage / long-short | SourceTrace `WATCH_ONLY`, BoundaryCandidate `WATCH_ONLY`, ExecutionPlan `WATCH_ONLY` |
| stale OI / Funding | SourceTrace `WATCH_ONLY`, no executable confidence |
| extreme Funding / OI / liquidation / leverage / long-short | SourceTrace `WATCH_ONLY`, no direct trade action |
| missing or worsening liquidity stress | SourceTrace `SAFE_FAIL_CLOSED_ONLY`, candidate/display `WATCH_ONLY` |
| stampede-like stress | RiskActionGuard blocks new entry, reverse, and opportunity push |
| conflicting derivatives signals | review-only `WATCH_ONLY` |
| data quality downgrade | review-only `WATCH_ONLY` |

## 4. Safety Assertions

P18 verifies that:

- no Coinglass API or external API is added
- no order API is added
- no auto-trading is added
- `VALID` remains manual-review / not-trade-instruction
- missing derivatives risk context cannot silently become complete
- stale or abnormal derivatives risk context stays review-only
- liquidity stress can fail closed without generating an execution instruction
- ExecutionPlan readiness remains review-only or fallback
- RuleEngine defaults remain advisory with `canExecute=false`

## 5. Non-Goals Confirmed

P18 does not add:

- P19 work
- API keys
- external HTTP clients
- live exchange / live derivatives data calls
- dashboard UI changes
- schema changes
- real ExecutionPlan execution
- order placement
- automated trading

## 6. Current Conclusion

P18 is a rollbackable derivatives-risk fixture extension package. It deepens local coverage for OI, Funding, liquidation, leverage, long-short, liquidity stress, data-quality downgrade, and conflicting risk signals while preserving review-only and fail-closed behavior.
