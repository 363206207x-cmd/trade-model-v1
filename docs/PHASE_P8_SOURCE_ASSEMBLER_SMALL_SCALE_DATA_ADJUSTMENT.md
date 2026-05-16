# PHASE_P8_SOURCE_ASSEMBLER_SMALL_SCALE_DATA_ADJUSTMENT

## 1. Document Purpose

This document defines the P8 baseline for small-scale data verification, module output adjustment, and fallback / Risk Action Guard behavior confirmation for Source Assembler in Trade Model V1.

The goal is to review P7 small-scale data intake results, identify safe adjustment points, and define how module outputs should be tuned without changing execution semantics.

This document is documentation and planning guidance only.

This phase does not:

- modify Java code
- modify configuration
- modify schema
- modify dashboard
- connect Coinglass or any external API
- generate execution plans
- generate automated trading code

BoundaryCandidate `VALID` remains a review candidate.

ExecutionPlan must not become executable from missing source, display-only state, or DTO-only state.

## 2. Small-Scale Data Verification Steps

P8 verification should use the controlled data intake baseline from P7.

No P8 validation may depend on external derivatives APIs.

### 2.1 Data Quality Review

| Review Area | Verification Step | Expected Outcome |
|---|---|---|
| RuntimeKlineContext | confirm OHLCV, kline window, latest price, stale status, and dataQualityScore | missing or stale context remains `INCOMPLETE` |
| entry source | confirm source type, source value, source timeframe, source reason, and source ref | traceable entry can remain reviewable |
| stop source | confirm stop or invalidation source is traceable | untraceable stop remains `INCOMPLETE` / `WATCH_ONLY` |
| TP source | confirm every TP level has source evidence | TP ladder remains review-only |
| RR source | confirm RR derives from entry / stop / TP and rule source | RR is not inferred from missing inputs |
| liquidity source | confirm liquidity stress has explicit source evidence | missing liquidity remains `SAFE_FAIL_CLOSED_ONLY` |
| multi-timeframe source | confirm alignment or conflict evidence | missing source remains `WATCH_ONLY` |
| event window source | confirm blocker state and source ref | uncertain event state downgrades review state |
| wick confirmation source | confirm wick classification and confirmation status | wick alone does not imply trend reversal |
| derivatives fixture data | confirm OI / Funding / liquidation / leverage are fixture-only or explicitly missing | derivatives confidence remains bounded |

### 2.2 Adjustment Eligibility

Only the following adjustment types are allowed in P8 planning:

- wording of review-only module output
- confidence labels for missing or partial evidence
- fallback classification wording
- source trace completeness notes
- test fixture coverage recommendations
- documentation of observed safe-fail behavior

P8 planning must not adjust:

- execution permissions
- order routing
- position opening
- position closing
- reverse position behavior
- production schema
- dashboard API shape
- external API contracts

## 3. Source Assembler Output Data Check And Adjustment Strategy

P8 should identify whether Source Assembler output needs wording or classification adjustments.

It must not invent missing source values.

| Source Area | Data Check | Allowed Adjustment | Forbidden Adjustment |
|---|---|---|---|
| entry source | verify source trace completeness and missingFields | clarify review wording or missing-source reason | forge entry price or infer source |
| stop source | verify stop / invalidation source trace | clarify invalidation-source note | forge stop or automatic stop action |
| TP source | verify TP ladder source trace and each level's evidence | clarify TP traceability and review-only note | treat TP as automatic take-profit order |
| RR source | verify RR derives from entry / stop / TP sources | clarify RR unavailable reason | infer RR when inputs are missing |
| liquidity source | verify stress source and missing-source behavior | clarify fail-closed liquidity state | recommend one-shot market exit |
| multi-timeframe source | verify alignment / conflict evidence | clarify conflict or insufficient convergence | assume convergence without data |
| event window source | verify blocker source and uncertainty state | clarify blocker or review downgrade | allow VALID when blocker source is missing |
| wick confirmation source | verify wick confirmation or unconfirmed state | clarify unconfirmed wick behavior | treat wick as trend reversal |

All Source Assembler output adjustments must preserve:

- traceable source references
- missingFields
- manual review requirements
- not-trade-instruction semantics
- fail-closed fallback behavior

## 4. Module Integration Verification And Adjustment Steps

P8 should review module outputs produced from P7 small-scale data and identify safe adjustment points.

This section defines expected adjustment behavior only and does not implement module wiring.

### 4.1 ScoreService

| Verification Case | Allowed Adjustment | Required Boundary |
|---|---|---|
| complete source data present | tune confidence wording for evidence-backed score | score remains non-trading |
| derivatives fixture data partial | mark score as derivatives-pending or evidence-limited | no direct long / short instruction |
| liquidity source missing | downgrade liquidity confidence or fail closed | no forced exit suggestion |
| wick unconfirmed | reduce trend confidence or mark unconfirmed | no trend reversal inference |

ScoreService must not convert OI, Funding, liquidation, leverage, or long / short ratio evidence into direct long, short, stop, or reverse instructions.

### 4.2 EvidenceService

| Verification Case | Allowed Adjustment | Required Boundary |
|---|---|---|
| complete source trace present | improve source trace display labels | evidence remains factual |
| missingFields present | improve missing-source explanation | no source certainty is invented |
| derivatives fixture partial | mark as fixture-based or partial evidence | no external confirmation claim |
| no external API source | clarify source as internal fixture or missing | no Coinglass-backed claim |

EvidenceService must preserve traceability and missing-source state.

### 4.3 DecisionEngine

| Verification Case | Allowed Adjustment | Required Boundary |
|---|---|---|
| all required sources complete and blockers absent | allow review-path wording refinement | no executable action |
| entry / stop / TP source missing | keep `INCOMPLETE` / `WATCH_ONLY` | no forged boundary |
| RR source missing | keep `INCOMPLETE` / `WATCH_ONLY` | no inferred RR |
| liquidity source missing | keep fail-closed readiness and guard checks | no exit action |
| event blocker active | keep downgrade or closed-safe state | no VALID gating bypass |
| wick unconfirmed | keep non-reversal interpretation | no execution decision |

DecisionEngine must not infer executable action from source completeness alone.

### 4.4 Push / Recheck / Watchlist

| Verification Case | Allowed Adjustment | Required Boundary |
|---|---|---|
| complete source trace with manual review | improve review-only message clarity | no order or execution semantics |
| missing required source | clarify waiting or fallback reason | no opportunity push based on missing source |
| stampede or extreme stress | preserve suppression behavior | no new opportunity push |
| active event blocker | preserve suppression or review-only state | no event-blocked opportunity |
| wick unconfirmed | track as watch-only if needed | no reverse or execution prompt |

Push, Recheck, and Watchlist must not emit new position, reverse, or order semantics from source evidence.

### 4.5 ExecutionPlan Readiness

| Verification Case | Allowed Adjustment | Required Boundary |
|---|---|---|
| BoundaryCandidate is `VALID` but SourceTrace is incomplete | clarify readiness blocked reason | ExecutionPlan remains non-executable |
| display-only PlanBoundary exists | clarify display-only state | no executable readiness |
| DTO-only valid candidate exists | clarify DTO-only limitation | no executable readiness |
| risk context complete but manual review gate absent | clarify review gate missing | readiness remains blocked |
| liquidity source missing | clarify fail-closed liquidity reason | readiness remains blocked |

BoundaryCandidate `VALID` is not ExecutionPlan readiness.

ExecutionPlan readiness requires explicit verified sources, risk context, review gates, and separately approved implementation.

### 4.6 RiskActionGuard

| Verification Case | Allowed Adjustment | Required Boundary |
|---|---|---|
| high risk evidence present | clarify manual-review risk wording | no direct stop-loss action |
| high risk with directional pressure | clarify risk pressure only | no direct reverse action |
| wick-only spike | clarify unconfirmed wick state | no trend reversal classification |
| stampede state | clarify suppression reason | new position, reverse, and opportunity push remain blocked |
| liquidity worsening | clarify staged risk-reduction review note | no one-shot market exit recommendation |
| derivatives data missing | clarify fail-closed state | no inferred risk action |

RiskActionGuard must remain conservative, review-oriented, and non-trading.

## 5. Fallback / Missing Data Verification And Adjustment

P8 must confirm fallback behavior from P7 and identify only safe wording or classification adjustments.

| Missing Or Degraded Area | Required Fallback | Allowed Adjustment |
|---|---|---|
| RuntimeKlineContext missing | `INCOMPLETE` | clarify missing runtime context |
| OHLCV / kline window missing | `INCOMPLETE` | clarify missing kline evidence |
| latest price missing | `INCOMPLETE` | clarify missing latest price |
| dataQualityScore missing or below threshold | `INCOMPLETE` / `WATCH_ONLY` | clarify low confidence |
| entry source missing | `INCOMPLETE` / `WATCH_ONLY` | clarify missing entry source |
| stop source missing | `INCOMPLETE` / `WATCH_ONLY` | clarify missing stop source |
| TP source missing | `INCOMPLETE` / `WATCH_ONLY` | clarify missing TP source |
| RR inputs missing | `INCOMPLETE` / `WATCH_ONLY` | clarify missing RR inputs |
| liquidity source missing | `SAFE_FAIL_CLOSED_ONLY` | clarify fail-closed liquidity state |
| multi-timeframe source missing | `WATCH_ONLY` | clarify no convergence assumption |
| event window blocker missing | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` | clarify conservative event state |
| wick confirmation missing | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` | clarify wick not confirmed |
| OI history missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `WATCH_ONLY` | clarify derivatives pending |
| Funding history missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `WATCH_ONLY` | clarify funding unavailable |
| liquidation cluster missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `SAFE_FAIL_CLOSED_ONLY` | clarify liquidation risk unavailable |
| leverage distribution missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `SAFE_FAIL_CLOSED_ONLY` | clarify leverage risk unavailable |
| long / short ratio missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `WATCH_ONLY` | clarify crowding unavailable |
| source trace adapter disconnected | `SAFE_FAIL_CLOSED_ONLY` | clarify display is not production-backed |
| display-only module without source backing | `SAFE_FAIL_CLOSED_ONLY` | clarify non-executable display state |

Only DTOs and safe defaults may remain `CLOSED` or `CLOSED_BASIC`.

Modules requiring missing production data must remain `INCOMPLETE_RISK_LOGIC`, `CLOSED_BASIC_DERIVATIVES_PENDING`, or `SAFE_FAIL_CLOSED_ONLY`.

## 6. Risk Action Guard Boundary And Non-Trading Constraint Confirmation

P8 verification and adjustment must preserve these guardrails:

- Risk high does not mean direct stop loss.
- Risk high does not mean direct reverse.
- Wick / spike does not mean trend reversal.
- Stampede state blocks new position, reverse, and opportunity push.
- Liquidity worsening does not justify one-shot market exit from this layer.
- Liquidation cluster evidence does not become automatic close.
- Funding / OI / long-short ratio evidence does not become direct long or short signal.
- BoundaryCandidate `VALID` does not mean execution permission.
- ExecutionPlan must not become executable from missing source, display-only state, or DTO-only state.

P8 may tune review wording or confidence labels, but it must not introduce trading semantics.

RiskActionGuard output must remain manual-review oriented and fail-closed until future verified sources and review gates are implemented.

## 7. Recommended Next Steps For P9

P9 should begin only after P8 adjustment notes are reviewed.

Recommended P9 actions:

1. Create a P9 adjustment verification report summarizing P8 findings.
2. Decide whether Source Assembler can enter a narrow implementation checklist phase.
3. Define exact Java files only after adjustment boundaries are approved.
4. Keep external derivatives API integration deferred until fixture behavior and fallback rules are stable.
5. Keep schema and dashboard changes deferred unless a separate approved plan exists.
6. Keep ExecutionPlan non-executable until source completeness, risk context, review gates, and readiness logic are independently verified.

P9 must still avoid:

- external API integration
- automatic execution semantics
- direct long / short / stop / reverse interpretation from derivatives evidence
- deriving BoundaryCandidate `VALID` from missing source data
- deriving executable ExecutionPlan from display-only or DTO-only state

## 8. Current Conclusion

P8 defines the small-scale data verification, module output adjustment, and fallback / Risk Action Guard confirmation baseline.

The required P8 posture is:

- review controlled data only
- avoid external API integration
- adjust wording or confidence labels only
- preserve traceable source references
- preserve missingFields
- keep fallback behavior explicit
- keep BoundaryCandidate `VALID` as review-only candidate state
- keep ExecutionPlan non-executable from missing source, display-only state, or DTO-only state
- keep Risk Action Guard fail-closed and non-trading

This phase remains documentation-only and does not modify source code, configuration, schema, or dashboard.
