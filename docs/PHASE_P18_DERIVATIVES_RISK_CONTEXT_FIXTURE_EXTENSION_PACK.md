# PHASE_P18_DERIVATIVES_RISK_CONTEXT_FIXTURE_EXTENSION_PACK

## 1. Document Purpose

This document defines the P18 DerivativesRiskContext / SourceTrace Fixture Extension Pack for Trade Model V1.

P18 extends the P17 local fail-closed fixture package with deeper derivatives-risk context scenarios. It remains a local fixture and focused-test package only.

## 2. P18 Scope

P18 covers local fixtures for:

- OI / open interest present, missing, stale, and abnormal
- Funding present, missing, stale, and extreme
- liquidation cluster present, missing, and abnormal concentration
- leverage distribution present, missing, and high-risk skew
- long-short ratio present, missing, and extreme crowding
- liquidity stress present, missing, worsening, and stampede-like
- derivatives-risk data quality score downgrade
- SourceTrace linkage from derivatives risk context to BoundaryCandidate and ExecutionPlan readiness

P18 does not start P19 and does not connect Coinglass or any external API.

## 3. Files Added In This Package

- `docs/PHASE_P18_DERIVATIVES_RISK_CONTEXT_FIXTURE_EXTENSION_PACK.md`
- `docs/PHASE_P18_DERIVATIVES_RISK_CONTEXT_FIXTURE_EXTENSION_RESULT.md`
- `src/test/java/org/example/trademodel/service/P18DerivativesRiskContextFixtureExtensionTest.java`
- `src/test/resources/planboundary/p18-derivatives-risk-fixture-extension-cases.csv`

No Codex Cloud trigger artifact is included in the final P18 package.

## 4. Fixture Scenario Matrix

| Fixture | Derivatives Risk Condition | Expected SourceTrace | Expected BoundaryCandidate | Expected ExecutionPlan | Expected Safety |
|---|---|---|---|---|---|
| complete-derivatives-risk | all required local fields present | complete | `VALID` review candidate | `READY_REVIEW_ONLY` | no executable permission |
| missing-oi-history | OI history absent | `WATCH_ONLY` | `WATCH_ONLY` | `WATCH_ONLY` | missing risk source blocks confidence |
| stale-oi-history | OI history stale marker | `WATCH_ONLY` | `WATCH_ONLY` | `WATCH_ONLY` | stale source remains review-only |
| missing-funding-history | Funding history absent | `WATCH_ONLY` | `WATCH_ONLY` | `WATCH_ONLY` | no executable confidence |
| extreme-funding | Funding extreme marker | `WATCH_ONLY` | `WATCH_ONLY` | `WATCH_ONLY` | no direct trade action |
| missing-liquidation-cluster | liquidation cluster absent | `WATCH_ONLY` | `WATCH_ONLY` | `WATCH_ONLY` | no executable confidence |
| abnormal-liquidation-concentration | abnormal concentration marker | `WATCH_ONLY` | `WATCH_ONLY` | `WATCH_ONLY` | no direct stop/reverse action |
| missing-leverage-distribution | leverage distribution absent | `WATCH_ONLY` | `WATCH_ONLY` | `WATCH_ONLY` | no executable confidence |
| high-leverage-skew | high-risk leverage skew marker | `WATCH_ONLY` | `WATCH_ONLY` | `WATCH_ONLY` | no direct action |
| missing-long-short-ratio | long-short ratio absent | `WATCH_ONLY` | `WATCH_ONLY` | `WATCH_ONLY` | no executable confidence |
| extreme-long-short-crowding | extreme crowding marker | `WATCH_ONLY` | `WATCH_ONLY` | `WATCH_ONLY` | no direct trade action |
| liquidity-stress-missing | liquidity stress source absent | `SAFE_FAIL_CLOSED_ONLY` | `WATCH_ONLY` | `WATCH_ONLY` | fail closed |
| liquidity-stress-worsening | worsening liquidity marker | `SAFE_FAIL_CLOSED_ONLY` | `WATCH_ONLY` | `WATCH_ONLY` | fail closed |
| stampede-like-stress | stampede-like stress marker | `SAFE_FAIL_CLOSED_ONLY` plus guard block | `WATCH_ONLY` | `WATCH_ONLY` | opportunity push / new entry blocked |
| conflicting-derivatives-signals | risk signals conflict | `WATCH_ONLY` | `WATCH_ONLY` | `WATCH_ONLY` | review-only |
| data-quality-downgrade | risk data quality below threshold | `WATCH_ONLY` | `WATCH_ONLY` | `WATCH_ONLY` | review-only |

## 5. Required Safety Assertions

Every P18 fixture must preserve:

- `manualReviewRequired = true`
- `notTradeInstruction = true`
- `BoundaryCandidateDTO.valid(...)` can only represent a review candidate
- ExecutionPlan readiness remains review-only or fallback
- RuleEngine output remains advisory and `canExecute=false`
- Push / Recheck / Watchlist naming remains review-only

## 6. Fail-Closed Rules

P18 uses the existing project vocabulary:

| Risk Context State | Required Fallback |
|---|---|
| missing structural boundary source | `INCOMPLETE` |
| missing or stale OI / Funding / liquidation / leverage / long-short source | `WATCH_ONLY` |
| abnormal OI / Funding / liquidation / leverage / long-short signal | `WATCH_ONLY` |
| missing or worsening liquidity stress source | `SAFE_FAIL_CLOSED_ONLY` at SourceTrace, `WATCH_ONLY` at candidate/display |
| stampede-like stress | `SAFE_FAIL_CLOSED_ONLY` plus RiskActionGuard review-only block |
| conflicting derivatives signals | `WATCH_ONLY` |
| data quality downgrade | `WATCH_ONLY` |

## 7. Risk Action Guard Boundary

P18 keeps the Risk Action Guard boundary:

- high risk does not directly mean stop-loss
- high risk does not directly mean reverse position
- strong reversal does not mean direct reverse entry
- wick / pin-bar does not mean confirmed trend reversal
- stampede / liquidity stress blocks new entry, reverse, and opportunity push
- Funding / OI / liquidation / leverage / long-short ratio do not directly generate trade actions

## 8. Current Conclusion

P18 extends local derivatives-risk fixture coverage without production integration. It verifies that missing, stale, abnormal, conflicting, or liquidity-stressed derivatives-risk context fails closed or remains review-only before any future Coinglass or external API work.
