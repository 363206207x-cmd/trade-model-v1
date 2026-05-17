# PHASE_P17_LOCAL_FIXTURE_FAIL_CLOSED_TEST_PACK

## 1. Document Purpose

This document defines the P17 local fixture fail-closed test pack for Trade Model V1.

P17 turns the P16 paper-backtest conclusions into focused local fixture checks and tests. It does not start P18.

## 2. P17 Scope

P17 covers only local fixture verification for:

- complete SourceTrace review-only candidate flow
- missing SourceTrace fallback
- missing derivatives risk context fallback
- RiskActionGuard high-risk / stampede / liquidity-missing / wick-only fallback
- ExecutionPlan readiness review-only behavior
- Push / Recheck / Watchlist positive naming review-only semantics

P17 does not connect Coinglass, order API, or any external derivatives API.

## 3. Files Added In This Package

- `src/test/resources/planboundary/p17-local-fixture-fail-closed-cases.csv`
- `src/test/java/org/example/trademodel/service/P17LocalFixtureFailClosedTest.java`
- `docs/PHASE_P17_LOCAL_FIXTURE_FAIL_CLOSED_TEST_PACK.md`
- `docs/PHASE_P17_LOCAL_FIXTURE_FAIL_CLOSED_TEST_RESULT.md`

The existing PR trigger file remains unchanged.

## 4. Local Fixture Coverage

| Fixture | SourceTrace | DerivativesRiskContext | RiskActionGuard | Expected BoundaryCandidate | Expected ExecutionPlan | Push/Recheck/Watchlist |
|---|---|---|---|---|---|---|
| complete-review-only | complete | complete | manual-review safe | `VALID` review candidate | `READY_REVIEW_ONLY` | review label only |
| missing-source-trace | missing entry source | complete | safe | `INCOMPLETE` | `INCOMPLETE` | no push opportunity |
| missing-derivatives-context | complete runtime source | missing risk context | safe | `WATCH_ONLY` | `WATCH_ONLY` | no executable confidence |
| stampede-risk | complete | complete | stampede detected | `WATCH_ONLY` | `WATCH_ONLY` | opportunity push blocked |
| liquidity-missing | complete | complete | liquidity pending | `WATCH_ONLY` | `WATCH_ONLY` | fail closed |
| wick-only-risk | complete | complete | wick-only detected | `WATCH_ONLY` | `WATCH_ONLY` | no trend-reversal inference |

## 5. Required Safety Assertions

Every fixture must preserve:

- `manualReviewRequired = true`
- `notTradeInstruction = true`
- no order placement
- no position close
- no reverse position
- no auto-trading
- no external API dependency

`VALID` means review candidate only. It does not mean executable order readiness.

## 6. Fail-Closed Rules

Missing or incomplete source evidence must map to existing project fallback vocabulary:

| Missing Area | Required Behavior |
|---|---|
| SourceTrace missing | `INCOMPLETE` |
| entry / stop / TP / RR source missing | `INCOMPLETE` |
| derivatives risk context missing | `WATCH_ONLY` |
| liquidity source missing | `SAFE_FAIL_CLOSED_ONLY` at SourceTrace, `WATCH_ONLY` at candidate/display |
| event / multi-timeframe / wick source missing | `WATCH_ONLY` |
| RiskActionGuard high risk or action flag | `WATCH_ONLY` / review-only |

## 7. Risk Action Guard Boundary

P17 keeps the established Risk Action Guard boundary:

- high risk does not directly mean stop-loss
- high risk does not directly mean reverse position
- wick / pin-bar does not mean confirmed trend reversal
- stampede / liquidity-stress blocks new entry, reverse, and opportunity push
- Funding / OI / liquidation / leverage / long-short ratio do not directly generate trade actions

## 8. Current Conclusion

P17 is a focused local fixture fail-closed package. It verifies that the existing SourceTrace, DerivativesRiskContext, BoundaryCandidateService, ExecutionPlan readiness, RuleEngine, and Push/Recheck naming contracts remain review-only and fail-closed under local fixtures.
