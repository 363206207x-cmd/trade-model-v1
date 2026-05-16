# PHASE_P9_SOURCE_ASSEMBLER_SMALL_SCALE_VERIFICATION

## 1. Document Purpose

This document defines the P9 execution baseline for small-scale Source Assembler data verification, module output micro-adjustment, fallback behavior confirmation, and Risk Action Guard boundary review in Trade Model V1.

The goal is to turn the P8 adjustment baseline into a concrete verification record template while keeping the system documentation-only, review-only, and non-executable.

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

## 2. Small-Scale Data Verification Execution Steps

P9 verification should execute against controlled fixtures or approved read-only snapshots only.

No P9 verification may depend on live external derivatives APIs.

### 2.1 Execution Preconditions

Before recording P9 verification, confirm:

- P8 small-scale data adjustment document is committed
- controlled fixture or read-only snapshot set is available
- fixture data clearly marks simulated OI, Funding, liquidation, leverage, and long / short ratio evidence
- source trace fields are present or explicitly marked missing
- fallback labels are expected for every missing data path
- no source path emits order, execution, close, reverse, or auto-trading semantics

### 2.2 Verification Execution Matrix

| Case | Verification Input | Expected Output |
|---|---|---|
| complete source case | entry / stop / TP / RR / liquidity / multi-timeframe / event / wick sources present | review-only complete SourceTrace |
| missing entry source | entry source absent or incomplete | `INCOMPLETE` / `WATCH_ONLY` |
| missing stop source | stop source absent or incomplete | `INCOMPLETE` / `WATCH_ONLY` |
| missing TP source | TP source absent or incomplete | `INCOMPLETE` / `WATCH_ONLY` |
| missing RR input | RR rule or required inputs absent | `INCOMPLETE` / `WATCH_ONLY` |
| missing liquidity source | liquidity state source absent | `SAFE_FAIL_CLOSED_ONLY` |
| missing multi-timeframe source | timeframe alignment evidence absent | `WATCH_ONLY` |
| active event blocker | event blocker present or uncertain | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |
| unconfirmed wick | wick exists without confirmation | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |
| derivatives fixture partial | OI / Funding / liquidation / leverage partial or missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / conservative fallback |

### 2.3 Execution Output Record

Each P9 verification record should capture:

- fixture name
- symbol
- timeframe
- available source fields
- missing source fields
- fallback result
- module output summary
- Risk Action Guard result
- whether BoundaryCandidate remains review-only
- whether ExecutionPlan remains non-executable
- notes for safe wording or confidence adjustment

## 3. Source Assembler Output Check And Micro-Adjustment Strategy

P9 may identify wording or confidence-label adjustments only.

P9 must not invent or backfill missing source values.

| Source Area | Verification Check | Allowed Micro-Adjustment | Required Boundary |
|---|---|---|---|
| entry source | source type, value, timeframe, reason, and ref are present | clarify missing entry source wording | no forged entry |
| stop source | stop / invalidation source is traceable | clarify invalidation evidence wording | no automatic stop action |
| TP source | each TP level has source trace | clarify TP review-only ladder wording | no automatic take-profit order |
| RR source | RR derives from entry / stop / TP and rule source | clarify RR unavailable reason | no inferred RR |
| liquidity source | liquidity stress has evidence | clarify fail-closed liquidity state | no one-shot exit recommendation |
| multi-timeframe source | alignment / conflict evidence is explicit | clarify conflict or insufficient convergence | no assumed convergence |
| event window source | blocker state is explicit | clarify event downgrade reason | no VALID gating bypass |
| wick confirmation source | wick is confirmed or marked unconfirmed | clarify unconfirmed wick state | no trend reversal inference |

All Source Assembler output must preserve:

- traceable source references
- missingFields
- manual review requirements
- not-trade-instruction semantics
- fail-closed fallback behavior

## 4. Module Integration Verification And Micro-Adjustment Steps

P9 checks module outputs against P8 adjustment rules.

This section defines verification expectations only and does not implement module wiring.

### 4.1 ScoreService

| Verification Case | Expected Behavior | Allowed Micro-Adjustment |
|---|---|---|
| complete source data present | score may consume evidence for context only | tune confidence wording |
| derivatives fixture partial | score is evidence-limited | clarify derivatives-pending label |
| liquidity source missing | score downgrades or fails closed | clarify liquidity confidence |
| wick unconfirmed | no trend reversal score is inferred | clarify unconfirmed wick label |

ScoreService must not convert OI, Funding, liquidation, leverage, or long / short ratio evidence into direct long, short, stop, or reverse instructions.

### 4.2 EvidenceService

| Verification Case | Expected Behavior | Allowed Micro-Adjustment |
|---|---|---|
| complete source trace present | evidence preserves source type, value, timeframe, reason, and ref | improve source trace label |
| missingFields present | evidence preserves missing source information | improve missing-source explanation |
| derivatives fixture partial | evidence marks derivatives context as partial | clarify fixture-based evidence |
| no external API source | evidence does not claim external confirmation | clarify source absence |

EvidenceService must preserve traceability and missing-source state.

### 4.3 DecisionEngine

| Verification Case | Expected Behavior | Allowed Micro-Adjustment |
|---|---|---|
| all required sources complete and blockers absent | review-path candidate evaluation may proceed | clarify review-path wording |
| entry / stop / TP source missing | decision remains `INCOMPLETE` / `WATCH_ONLY` | clarify missing boundary source |
| RR source missing | decision remains `INCOMPLETE` / `WATCH_ONLY` | clarify RR unavailable reason |
| liquidity source missing | readiness and guard checks remain fail-closed | clarify liquidity blocker |
| event blocker active | decision downgrades to `WATCH_ONLY` or closed-safe state | clarify event blocker reason |
| wick unconfirmed | no trend reversal or execution decision is inferred | clarify wick status |

DecisionEngine must not infer executable action from source completeness alone.

### 4.4 Push / Recheck / Watchlist

| Verification Case | Expected Behavior | Allowed Micro-Adjustment |
|---|---|---|
| complete source trace with manual review | emitted content remains review-only | improve review-only copy |
| missing required source | recheck / watchlist preserves fallback state | clarify waiting reason |
| stampede or extreme stress | new opportunity push remains blocked | clarify suppression reason |
| active event blocker | opportunity push is suppressed or marked review-only | clarify event-blocked message |
| wick unconfirmed | watchlist may track only | clarify non-execution note |

Push, Recheck, and Watchlist must not emit new position, reverse, or order semantics from source evidence.

### 4.5 ExecutionPlan Readiness

| Verification Case | Expected Behavior | Allowed Micro-Adjustment |
|---|---|---|
| BoundaryCandidate is `VALID` but SourceTrace is incomplete | readiness remains blocked | clarify missing source blocker |
| display-only PlanBoundary exists | ExecutionPlan remains non-executable | clarify display-only limitation |
| DTO-only valid candidate exists | ExecutionPlan remains non-executable | clarify DTO-only limitation |
| risk context complete but manual review gate absent | readiness remains blocked | clarify review gate blocker |
| liquidity source missing | readiness remains fail-closed | clarify liquidity blocker |

BoundaryCandidate `VALID` is not ExecutionPlan readiness.

ExecutionPlan readiness requires explicit verified sources, risk context, review gates, and separately approved implementation.

### 4.6 RiskActionGuard

| Verification Case | Expected Behavior | Allowed Micro-Adjustment |
|---|---|---|
| high risk evidence present | no direct stop-loss action | clarify manual-review risk wording |
| high risk with directional pressure | no direct reverse action | clarify pressure-only state |
| wick-only spike | no trend reversal classification | clarify unconfirmed wick |
| stampede state | new position, reverse, and opportunity push remain blocked | clarify suppression reason |
| liquidity worsening | no one-shot market exit recommendation | clarify staged review wording |
| derivatives data missing | fail-closed state remains active | clarify missing derivatives source |

RiskActionGuard must remain conservative, review-oriented, and non-trading.

## 5. Fallback / Missing Data Verification And Adjustment Rules

P9 must confirm fallback behavior and record any safe wording or confidence adjustment.

| Missing Or Degraded Area | Required Fallback | Adjustment Rule |
|---|---|---|
| RuntimeKlineContext missing | `INCOMPLETE` | clarify missing runtime context only |
| OHLCV / kline window missing | `INCOMPLETE` | clarify missing kline evidence only |
| latest price missing | `INCOMPLETE` | clarify missing latest price only |
| dataQualityScore missing or below threshold | `INCOMPLETE` / `WATCH_ONLY` | clarify low confidence only |
| entry source missing | `INCOMPLETE` / `WATCH_ONLY` | no entry value may be forged |
| stop source missing | `INCOMPLETE` / `WATCH_ONLY` | no stop or invalidation value may be forged |
| TP source missing | `INCOMPLETE` / `WATCH_ONLY` | no TP ladder may be forged |
| RR inputs missing | `INCOMPLETE` / `WATCH_ONLY` | RR must not be inferred |
| liquidity source missing | `SAFE_FAIL_CLOSED_ONLY` | readiness and guard checks remain blocked |
| multi-timeframe source missing | `WATCH_ONLY` | convergence must not be assumed |
| event window blocker missing | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` | VALID gating remains conservative |
| wick confirmation missing | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` | wick must not imply trend reversal |
| OI history missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `WATCH_ONLY` | derivatives confidence remains bounded |
| Funding history missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `WATCH_ONLY` | funding must not become direction |
| liquidation cluster missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `SAFE_FAIL_CLOSED_ONLY` | liquidation risk must not become close or reverse |
| leverage distribution missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `SAFE_FAIL_CLOSED_ONLY` | leverage stress must not be assumed |
| long / short ratio missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `WATCH_ONLY` | crowding direction must not be inferred |
| source trace adapter disconnected | `SAFE_FAIL_CLOSED_ONLY` | display remains safe but not production-backed |
| display-only module without source backing | `SAFE_FAIL_CLOSED_ONLY` | no executable state may be derived |

Only DTOs and safe defaults may remain `CLOSED` or `CLOSED_BASIC`.

Modules requiring missing production data must remain `INCOMPLETE_RISK_LOGIC`, `CLOSED_BASIC_DERIVATIVES_PENDING`, or `SAFE_FAIL_CLOSED_ONLY`.

## 6. Risk Action Guard Boundary And Non-Trading Constraints

P9 verification must preserve these guardrails:

- Risk high does not mean direct stop loss.
- Risk high does not mean direct reverse.
- Wick / spike does not mean trend reversal.
- Stampede state blocks new position, reverse, and opportunity push.
- Liquidity worsening does not justify one-shot market exit from this layer.
- Liquidation cluster evidence does not become automatic close.
- Funding / OI / long-short ratio evidence does not become direct long or short signal.
- BoundaryCandidate `VALID` does not mean execution permission.
- ExecutionPlan must not become executable from missing source, display-only state, or DTO-only state.

P9 may record verification results and safe wording recommendations, but it must not introduce trading semantics.

RiskActionGuard output must remain manual-review oriented and fail-closed until future verified sources and review gates are implemented.

## 7. Recommended Next Steps For P10

P10 should begin only after P9 verification records are reviewed.

Recommended P10 actions:

1. Create a P10 implementation readiness checklist for Source Assembler.
2. Decide whether narrow Java implementation scope is allowed.
3. Identify exact files only after implementation boundaries are approved.
4. Keep external derivatives API integration deferred until internal verification is stable.
5. Keep schema and dashboard changes deferred unless a separate approved plan exists.
6. Keep ExecutionPlan non-executable until source completeness, risk context, review gates, and readiness logic are independently verified.

P10 must still avoid:

- external API integration
- automatic execution semantics
- direct long / short / stop / reverse interpretation from derivatives evidence
- deriving BoundaryCandidate `VALID` from missing source data
- deriving executable ExecutionPlan from display-only or DTO-only state

## 8. Current Conclusion

P9 defines the small-scale verification execution baseline for Source Assembler output, module output micro-adjustment, fallback confirmation, and Risk Action Guard boundary review.

The required P9 posture is:

- execute against controlled fixtures or read-only snapshots only
- avoid external API integration
- record verification outcomes
- tune wording or confidence labels only
- preserve traceable source references
- preserve missingFields
- keep fallback behavior explicit
- keep BoundaryCandidate `VALID` as review-only candidate state
- keep ExecutionPlan non-executable from missing source, display-only state, or DTO-only state
- keep Risk Action Guard fail-closed and non-trading

This phase remains documentation-only and does not modify source code, configuration, schema, or dashboard.
