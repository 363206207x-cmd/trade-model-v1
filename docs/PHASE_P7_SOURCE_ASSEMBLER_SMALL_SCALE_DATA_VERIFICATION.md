# PHASE_P7_SOURCE_ASSEMBLER_SMALL_SCALE_DATA_VERIFICATION

## 1. Document Purpose

This document defines the P7 small-scale data intake and verification baseline for Source Assembler in Trade Model V1.

The goal is to verify how small-scale production-like data can be introduced into Source Assembler validation without connecting external APIs, changing runtime code, or producing executable trading behavior.

This document is a documentation baseline only.

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

## 2. Data Intake Checklist And Notes

P7 data intake must use controlled, read-only, small-scale data sources.

Allowed data sources for P7 verification:

- approved internal fixtures
- sanitized production-like snapshots
- read-only exported market structure samples
- manually prepared SourceTrace samples
- manually prepared DerivativesRiskContext samples
- controlled missing-data fixtures

Disallowed data sources for P7:

- direct Coinglass integration
- direct exchange derivatives API integration
- live external API polling
- production schema changes
- dashboard wiring
- automated trading signals

### 2.1 Small-Scale Data Intake Checklist

| Data Area | Required Data | Intake Rule | Missing Data Result |
|---|---|---|---|
| RuntimeKlineContext | OHLCV, kline window, latest price, stale status, data quality | Use read-only fixture or snapshot | `INCOMPLETE` |
| entry source | numeric source value, source type, source timeframe, source reason, source ref | Must be traceable before candidate review | `INCOMPLETE` / `WATCH_ONLY` |
| stop source | stop or invalidation source value, source type, source timeframe, source reason, source ref | Must be traceable and not hardcoded | `INCOMPLETE` / `WATCH_ONLY` |
| TP source | TP ladder source values, source type, source timeframe, source reason, source ref | Each TP level must be traceable | `INCOMPLETE` / `WATCH_ONLY` |
| RR source | RR inputs and rule reference | Must derive from entry / stop / TP source data | `INCOMPLETE` / `WATCH_ONLY` |
| liquidity source | liquidity state, liquidity stress reason, source ref | Must be explicit for readiness and guard checks | `SAFE_FAIL_CLOSED_ONLY` |
| multi-timeframe source | alignment, conflict, or convergence evidence | Must not assume convergence when missing | `WATCH_ONLY` |
| event window source | blocker list and source ref | Must block or downgrade review when uncertain | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |
| wick confirmation source | wick classification and confirmation status | Must not imply trend reversal without confirmation | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |
| OI history sample | open interest history fixture or missing marker | Fixture only; no external API | `CLOSED_BASIC_DERIVATIVES_PENDING` / `WATCH_ONLY` |
| Funding history sample | funding history fixture or missing marker | Fixture only; no external API | `CLOSED_BASIC_DERIVATIVES_PENDING` / `WATCH_ONLY` |
| liquidation cluster sample | liquidation level fixture or missing marker | Fixture only; no external API | `CLOSED_BASIC_DERIVATIVES_PENDING` / `SAFE_FAIL_CLOSED_ONLY` |
| leverage distribution sample | leverage distribution fixture or missing marker | Fixture only; no external API | `CLOSED_BASIC_DERIVATIVES_PENDING` / `SAFE_FAIL_CLOSED_ONLY` |

Any fixture that simulates OI, Funding, liquidation, leverage, or long / short ratio data must clearly mark the evidence as simulated or fixture-based.

No fixture may be interpreted as production external confirmation.

## 3. Source Assembler Output Data Verification Steps

P7 verifies that Source Assembler output remains traceable, explicit, and fail-closed.

### 3.1 Common Output Requirements

Every Source Assembler output must verify:

- symbol is present
- timeframe is present
- source type is present
- source value is present when numeric source is required
- source timeframe is present when source timeframe affects interpretation
- source reason is present
- source field or source ref is present
- missingFields records absent inputs
- dataQualityScore is present or explicitly missing
- no numeric value is generated without traceable source evidence
- no execution or automated trading semantics are emitted

### 3.2 Source Area Verification

| Source Area | Verification Step | Required Result |
|---|---|---|
| entry source | Verify entry source maps to source type, value, timeframe, reason, and ref | entry can be reviewed only when traceable |
| stop source | Verify stop or invalidation source maps to source type, value, timeframe, reason, and ref | stop can be reviewed only when traceable |
| TP source | Verify every TP level has traceable source data | TP ladder remains review-only |
| RR source | Verify RR derives from entry / stop / TP and rule source | RR must not be inferred when inputs are missing |
| liquidity source | Verify liquidity stress state has explicit evidence | missing liquidity blocks readiness and guard actions |
| multi-timeframe source | Verify alignment or conflict evidence is explicit | missing source keeps review conservative |
| event window source | Verify blocker state is explicit | blocker must downgrade or prevent VALID gating |
| wick confirmation source | Verify wick is confirmed or marked unconfirmed | wick alone must not imply trend reversal |

## 4. Module Integration Verification Steps

P7 verifies module consumption of Source Assembler data with small-scale fixtures.

This section defines verification expectations only and does not implement module wiring.

### 4.1 ScoreService

| Verification Case | Expected Behavior |
|---|---|
| complete source data present | ScoreService may consume evidence for scoring context only |
| OI / Funding / liquidation / leverage fixture missing | score confidence is reduced or evidence-limited |
| liquidity source missing | liquidity-related scoring fails closed or downgrades confidence |
| wick unconfirmed | no trend reversal score is inferred |

ScoreService must not convert derivatives evidence into direct long, short, stop, or reverse instructions.

### 4.2 EvidenceService

| Verification Case | Expected Behavior |
|---|---|
| complete source trace present | evidence preserves source type, value, timeframe, reason, and ref |
| missingFields present | evidence preserves missing source information |
| derivatives fixture partial | evidence marks derivatives context as partial |
| no external API source | evidence must not claim external derivatives confirmation |

EvidenceService must preserve traceability and missing-source state.

### 4.3 DecisionEngine

| Verification Case | Expected Behavior |
|---|---|
| all required sources complete and blockers absent | review-path candidate evaluation may proceed |
| entry / stop / TP source missing | decision remains `INCOMPLETE` / `WATCH_ONLY` |
| RR source missing | decision remains `INCOMPLETE` / `WATCH_ONLY` |
| liquidity source missing | decision remains fail-closed for readiness and guard checks |
| event blocker active | decision downgrades to `WATCH_ONLY` or closed-safe state |
| wick unconfirmed | no trend reversal or execution decision is inferred |

DecisionEngine must not infer executable action from source completeness alone.

### 4.4 Push / Recheck / Watchlist

| Verification Case | Expected Behavior |
|---|---|
| complete source trace with manual review | push content remains review-only if emitted |
| missing required source | recheck or watchlist preserves fallback state |
| stampede or extreme stress | new opportunity push remains blocked |
| active event blocker | opportunity push is suppressed or marked review-only |
| wick unconfirmed | watchlist may track but must not emit execution semantics |

Push, Recheck, and Watchlist must not emit new position, reverse, or order semantics from source evidence.

### 4.5 ExecutionPlan Readiness

| Verification Case | Expected Behavior |
|---|---|
| BoundaryCandidate is `VALID` but SourceTrace is incomplete | readiness remains blocked |
| display-only PlanBoundary exists | ExecutionPlan remains non-executable |
| DTO-only valid candidate exists | ExecutionPlan remains non-executable |
| risk context complete but manual review gate absent | readiness remains blocked |
| liquidity source missing | readiness fails closed |

BoundaryCandidate `VALID` is not ExecutionPlan readiness.

ExecutionPlan readiness requires explicit verified sources, risk context, review gates, and separately approved implementation.

### 4.6 RiskActionGuard

| Verification Case | Expected Behavior |
|---|---|
| high risk evidence present | no direct stop-loss action |
| high risk with directional pressure | no direct reverse action |
| wick-only spike | no trend reversal classification without confirmation |
| stampede state | new position, reverse, and opportunity push remain blocked |
| liquidity worsening | no one-shot market exit recommendation from this layer |
| derivatives data missing | fail-closed state remains active |

RiskActionGuard must remain conservative, review-oriented, and non-trading.

## 5. Fallback / Missing Data Trigger Verification

P7 data verification must explicitly confirm that missing or degraded data triggers the correct fallback.

| Missing Or Degraded Area | Required Fallback | Verification Expectation |
|---|---|---|
| RuntimeKlineContext missing | `INCOMPLETE` | BoundaryCandidate must not become `VALID` |
| OHLCV / kline window missing | `INCOMPLETE` | source output records missing runtime context |
| latest price missing | `INCOMPLETE` | candidate source output remains incomplete |
| dataQualityScore missing or below threshold | `INCOMPLETE` / `WATCH_ONLY` | review confidence remains limited |
| entry source missing | `INCOMPLETE` / `WATCH_ONLY` | no entry value is forged |
| stop source missing | `INCOMPLETE` / `WATCH_ONLY` | no stop or invalidation value is forged |
| TP source missing | `INCOMPLETE` / `WATCH_ONLY` | no TP ladder is forged |
| RR inputs missing | `INCOMPLETE` / `WATCH_ONLY` | RR is not inferred without entry / stop / TP |
| liquidity source missing | `SAFE_FAIL_CLOSED_ONLY` | RiskActionGuard and ExecutionPlan readiness stay blocked |
| multi-timeframe source missing | `WATCH_ONLY` | convergence is not assumed |
| event window blocker missing | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` | VALID gating remains conservative |
| wick confirmation missing | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` | wick is not treated as trend reversal |
| OI history missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `WATCH_ONLY` | derivatives confidence remains limited |
| Funding history missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `WATCH_ONLY` | funding is not converted into direction |
| liquidation cluster missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `SAFE_FAIL_CLOSED_ONLY` | liquidation risk is not converted into close or reverse |
| leverage distribution missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `SAFE_FAIL_CLOSED_ONLY` | leverage stress is not assumed |
| long / short ratio missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `WATCH_ONLY` | crowding direction is not inferred |
| source trace adapter disconnected | `SAFE_FAIL_CLOSED_ONLY` | display remains safe but not production-backed |
| display-only module without source backing | `SAFE_FAIL_CLOSED_ONLY` | no executable state is derived |

Only DTOs and safe defaults may remain `CLOSED` or `CLOSED_BASIC`.

Modules requiring missing production data must remain `INCOMPLETE_RISK_LOGIC`, `CLOSED_BASIC_DERIVATIVES_PENDING`, or `SAFE_FAIL_CLOSED_ONLY`.

## 6. Risk Action Guard Boundary And Non-Trading Constraints

P7 verification must preserve these guardrails:

- Risk high does not mean direct stop loss.
- Risk high does not mean direct reverse.
- Wick / spike does not mean trend reversal.
- Stampede state blocks new position, reverse, and opportunity push.
- Liquidity worsening does not justify one-shot market exit from this layer.
- Liquidation cluster evidence does not become automatic close.
- Funding / OI / long-short ratio evidence does not become direct long or short signal.
- BoundaryCandidate `VALID` does not mean execution permission.
- ExecutionPlan must not become executable from missing source, display-only state, or DTO-only state.

RiskActionGuard output must remain manual-review oriented and fail-closed until future verified sources and review gates are implemented.

## 7. Recommended Next Steps For P8

P8 should only begin after P7 small-scale data verification records are reviewed.

Recommended P8 actions:

1. Create a P8 verification report for the P7 data intake and module integration checks.
2. Decide whether Source Assembler implementation can enter a narrow implementation checklist phase.
3. Define a minimal Java implementation scope only after fixtures and fallback behavior are stable.
4. Keep external derivatives API integration deferred until internal data verification is complete.
5. Keep schema and dashboard changes deferred unless a separate approved plan exists.
6. Keep ExecutionPlan non-executable until source completeness, risk context, review gates, and readiness logic are independently verified.

P8 must still avoid:

- external API integration
- automatic execution semantics
- direct long / short / stop / reverse interpretation from derivatives evidence
- deriving BoundaryCandidate `VALID` from missing source data
- deriving executable ExecutionPlan from display-only or DTO-only state

## 8. Current Conclusion

P7 defines the small-scale data intake and verification baseline for Source Assembler and downstream module checks.

The required verification posture is:

- use controlled read-only data only
- avoid external API integration
- verify traceable source completeness
- preserve missingFields
- trigger explicit fallback
- keep BoundaryCandidate `VALID` as review-only candidate state
- keep ExecutionPlan non-executable from missing source, display-only state, or DTO-only state
- keep Risk Action Guard fail-closed and non-trading

This phase remains documentation-only and does not modify source code, configuration, schema, or dashboard.
