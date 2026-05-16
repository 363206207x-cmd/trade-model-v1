# PHASE_P10_SOURCE_ASSEMBLER_SMALL_SCALE_PRODUCTION_VERIFICATION

## 1. Document Purpose

This document defines the P10 small-scale production verification baseline for Source Assembler in Trade Model V1.

The goal is to record the final documentation-level verification plan for Source Assembler output completeness, downstream module consumption, fallback behavior, and Risk Action Guard boundaries before any future implementation checklist is considered.

This phase is documentation-only.

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

## 2. Small-Scale Production Verification Steps

P10 verification should use controlled small-scale production-like snapshots or approved internal fixtures only.

No P10 verification may depend on live external derivatives APIs.

### 2.1 Verification Preconditions

Before marking P10 complete, confirm:

- P9 small-scale verification document is committed
- controlled fixtures or read-only snapshots are available
- simulated OI, Funding, liquidation, leverage, and long / short ratio data are explicitly marked as fixture or snapshot evidence
- missingFields are preserved for every missing source path
- fallback status is recorded for every incomplete input
- no output contains order, execution, close, reverse, or automated trading semantics

### 2.2 Production-Like Verification Matrix

| Verification Case | Required Input | Expected Output |
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
| derivatives evidence partial | OI / Funding / liquidation / leverage partial or missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / conservative fallback |

### 2.3 Verification Record Requirements

Each P10 verification record should include:

- symbol
- timeframe
- fixture or snapshot name
- available source fields
- missing source fields
- fallback result
- module output summary
- Risk Action Guard result
- BoundaryCandidate review state
- ExecutionPlan readiness state
- non-execution confirmation
- notes for future implementation checklist

## 3. Source Assembler Output Completeness Verification

P10 verifies output completeness and traceability only.

P10 must not invent or backfill missing source values.

| Source Area | Completeness Requirement | Required Fallback When Missing |
|---|---|---|
| entry source | source type, value, timeframe, reason, and ref are present | `INCOMPLETE` / `WATCH_ONLY` |
| stop source | stop / invalidation source type, value, timeframe, reason, and ref are present | `INCOMPLETE` / `WATCH_ONLY` |
| TP source | every TP level has source trace and review-only ladder context | `INCOMPLETE` / `WATCH_ONLY` |
| RR source | RR derives from entry / stop / TP and explicit rule source | `INCOMPLETE` / `WATCH_ONLY` |
| liquidity source | liquidity stress state has source evidence and reason | `SAFE_FAIL_CLOSED_ONLY` |
| multi-timeframe source | alignment / conflict evidence is explicit | `WATCH_ONLY` |
| event source | event blocker state and source ref are explicit | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |
| wick source | wick is confirmed or explicitly marked unconfirmed | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |

All Source Assembler outputs must preserve:

- traceable source references
- missingFields
- manual review requirements
- not-trade-instruction semantics
- fail-closed fallback behavior

## 4. Module Integration Verification And Micro-Adjustment

P10 confirms whether module outputs remain safe when consuming Source Assembler data.

This section defines verification expectations only and does not implement module wiring.

### 4.1 ScoreService

| Verification Case | Expected Behavior | Allowed Adjustment |
|---|---|---|
| complete source data present | score may consume evidence for context only | tune confidence wording |
| derivatives evidence partial | score is evidence-limited | clarify derivatives-pending label |
| liquidity source missing | score downgrades or fails closed | clarify liquidity confidence |
| wick unconfirmed | no trend reversal score is inferred | clarify unconfirmed wick label |

ScoreService must not convert OI, Funding, liquidation, leverage, or long / short ratio evidence into direct long, short, stop, or reverse instructions.

### 4.2 EvidenceService

| Verification Case | Expected Behavior | Allowed Adjustment |
|---|---|---|
| complete source trace present | evidence preserves source type, value, timeframe, reason, and ref | improve source trace label |
| missingFields present | evidence preserves missing source information | improve missing-source explanation |
| derivatives evidence partial | evidence marks derivatives context as partial | clarify fixture or snapshot evidence |
| no external API source | evidence does not claim external confirmation | clarify source absence |

EvidenceService must preserve traceability and missing-source state.

### 4.3 DecisionEngine

| Verification Case | Expected Behavior | Allowed Adjustment |
|---|---|---|
| all required sources complete and blockers absent | review-path candidate evaluation may proceed | clarify review-path wording |
| entry / stop / TP source missing | decision remains `INCOMPLETE` / `WATCH_ONLY` | clarify missing boundary source |
| RR source missing | decision remains `INCOMPLETE` / `WATCH_ONLY` | clarify RR unavailable reason |
| liquidity source missing | readiness and guard checks remain fail-closed | clarify liquidity blocker |
| event blocker active | decision downgrades to `WATCH_ONLY` or closed-safe state | clarify event blocker reason |
| wick unconfirmed | no trend reversal or execution decision is inferred | clarify wick status |

DecisionEngine must not infer executable action from source completeness alone.

### 4.4 Push / Recheck / Watchlist

| Verification Case | Expected Behavior | Allowed Adjustment |
|---|---|---|
| complete source trace with manual review | emitted content remains review-only | improve review-only copy |
| missing required source | recheck / watchlist preserves fallback state | clarify waiting reason |
| stampede or extreme stress | new opportunity push remains blocked | clarify suppression reason |
| active event blocker | opportunity push is suppressed or marked review-only | clarify event-blocked message |
| wick unconfirmed | watchlist may track only | clarify non-execution note |

Push, Recheck, and Watchlist must not emit new position, reverse, or order semantics from source evidence.

### 4.5 ExecutionPlan Readiness

| Verification Case | Expected Behavior | Allowed Adjustment |
|---|---|---|
| BoundaryCandidate is `VALID` but SourceTrace is incomplete | readiness remains blocked | clarify missing source blocker |
| display-only PlanBoundary exists | ExecutionPlan remains non-executable | clarify display-only limitation |
| DTO-only valid candidate exists | ExecutionPlan remains non-executable | clarify DTO-only limitation |
| risk context complete but manual review gate absent | readiness remains blocked | clarify review gate blocker |
| liquidity source missing | readiness remains fail-closed | clarify liquidity blocker |

BoundaryCandidate `VALID` is not ExecutionPlan readiness.

ExecutionPlan readiness requires explicit verified sources, risk context, review gates, and separately approved implementation.

### 4.6 RiskActionGuard

| Verification Case | Expected Behavior | Allowed Adjustment |
|---|---|---|
| high risk evidence present | no direct stop-loss action | clarify manual-review risk wording |
| high risk with directional pressure | no direct reverse action | clarify pressure-only state |
| wick-only spike | no trend reversal classification | clarify unconfirmed wick |
| stampede state | new position, reverse, and opportunity push remain blocked | clarify suppression reason |
| liquidity worsening | no one-shot market exit recommendation | clarify staged review wording |
| derivatives data missing | fail-closed state remains active | clarify missing derivatives source |

RiskActionGuard must remain conservative, review-oriented, and non-trading.

## 5. Fallback / Missing Data Verification And Adjustment

P10 must confirm every missing or degraded data path uses explicit fallback.

| Missing Or Degraded Area | Required Fallback | Verification Requirement |
|---|---|---|
| RuntimeKlineContext missing | `INCOMPLETE` | BoundaryCandidate must not become `VALID` |
| OHLCV / kline window missing | `INCOMPLETE` | source output records missing runtime context |
| latest price missing | `INCOMPLETE` | candidate source output remains incomplete |
| dataQualityScore missing or below threshold | `INCOMPLETE` / `WATCH_ONLY` | review confidence remains limited |
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

P10 verification must preserve these guardrails:

- Risk high does not mean direct stop loss.
- Risk high does not mean direct reverse.
- Wick / spike does not mean trend reversal.
- Stampede state blocks new position, reverse, and opportunity push.
- Liquidity worsening does not justify one-shot market exit from this layer.
- Liquidation cluster evidence does not become automatic close.
- Funding / OI / long-short ratio evidence does not become direct long or short signal.
- BoundaryCandidate `VALID` does not mean execution permission.
- ExecutionPlan must not become executable from missing source, display-only state, or DTO-only state.

P10 may record final documentation-level verification, but it must not introduce trading semantics.

RiskActionGuard output must remain manual-review oriented and fail-closed until future verified sources and review gates are implemented.

## 7. Final Conclusion And P10 Completion Marker

P10 completes the documentation-only small-scale production verification planning sequence for Source Assembler.

P10 can be marked complete only when:

- this document is committed
- no source code is modified in this phase
- no configuration, schema, or dashboard is modified in this phase
- no external API is connected
- no execution plan is generated
- no automated trading code is generated
- BoundaryCandidate `VALID` remains review-only
- ExecutionPlan remains non-executable from missing source, display-only state, or DTO-only state
- Risk Action Guard remains fail-closed and non-trading

After P10, the next safe action is not implementation by default.

The next safe action is to create a separate implementation readiness checklist that defines whether Source Assembler may enter a narrow Java implementation phase.

Any future implementation phase must still explicitly defer:

- external API integration
- automatic execution semantics
- direct long / short / stop / reverse interpretation from derivatives evidence
- deriving BoundaryCandidate `VALID` from missing source data
- deriving executable ExecutionPlan from display-only or DTO-only state
