# PHASE_P6_SOURCE_ASSEMBLER_SMALL_SCALE_TEST_PLAN

## 1. Document Purpose

This document defines the P6 small-scale test plan for Source Assembler output and downstream module integration in Trade Model V1.

The goal is to turn the P5 small-scale verification baseline into an executable test plan, while keeping all source usage traceable and fail-closed.

This document is planning guidance only.

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

## 2. Small-Scale Test Steps

P6 tests should use controlled fixtures or approved internal test data only.

No P6 test case may depend on external derivatives APIs.

### 2.1 Fixture Set

Prepare a minimal fixture set covering:

| Fixture | Purpose | Expected Result |
|---|---|---|
| complete source fixture | entry / stop / TP / RR / liquidity / multi-timeframe / event / wick sources all present | Source Assembler can produce complete traceable output |
| missing entry source fixture | entry numeric source absent | `INCOMPLETE` / `WATCH_ONLY` |
| missing stop source fixture | stop numeric source absent | `INCOMPLETE` / `WATCH_ONLY` |
| missing TP source fixture | TP numeric source absent | `INCOMPLETE` / `WATCH_ONLY` |
| missing RR fixture | RR inputs or rule source absent | `INCOMPLETE` / `WATCH_ONLY` |
| missing liquidity fixture | liquidity state source absent | `SAFE_FAIL_CLOSED_ONLY` |
| missing multi-timeframe fixture | multi-timeframe source absent | `WATCH_ONLY` |
| active event blocker fixture | event window blocker present | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |
| unconfirmed wick fixture | wick source exists but confirmation is absent | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |
| degraded data quality fixture | dataQualityScore below threshold or required evidence incomplete | `INCOMPLETE` / `WATCH_ONLY` |

If fixtures simulate OI, Funding, liquidation, leverage, or long / short ratio data, they must be marked as test fixtures and not production external data.

### 2.2 Source Assembler Output Checks

Each Source Assembler output must verify:

- symbol is present
- timeframe is present
- source type is present for each assembled source
- numeric source value is present when required
- source timeframe is present when timeframe affects interpretation
- source reason is present
- source field or source ref is present
- missingFields records absent inputs
- no entry / stop / TP / RR value is generated without traceable source
- no execution or order semantics are emitted

### 2.3 Source Area Checks

| Source Area | Required Test Assertion | Required Fallback When Missing |
|---|---|---|
| entry source | entry price maps to source type, value, timeframe, reason, and ref | `INCOMPLETE` / `WATCH_ONLY` |
| stop source | stop price or invalidation level maps to traceable source | `INCOMPLETE` / `WATCH_ONLY` |
| TP source | each TP level maps to traceable source and ladder evidence | `INCOMPLETE` / `WATCH_ONLY` |
| RR source | RR is derived from entry / stop / TP inputs and rule source | `INCOMPLETE` / `WATCH_ONLY` |
| liquidity source | liquidity stress state has source evidence | `SAFE_FAIL_CLOSED_ONLY` |
| multi-timeframe source | alignment or conflict evidence is explicit | `WATCH_ONLY` |
| event window source | blocker state and source ref are explicit | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |
| wick confirmation source | wick is confirmed or explicitly marked unconfirmed | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |

## 3. Module Integration Verification Steps

P6 should verify that downstream modules consume Source Assembler output safely.

This section defines expected tests and assertions. It does not implement module integration.

### 3.1 ScoreService

| Test Case | Expected Assertion |
|---|---|
| complete source trace present | ScoreService may consume evidence, but output remains non-trading |
| OI / Funding / liquidation / leverage fixture missing | score confidence must be reduced or marked evidence-limited |
| liquidity source missing | liquidity-related score must fail closed or downgrade confidence |
| wick-only unconfirmed | no trend reversal score should be inferred from wick alone |

ScoreService must not convert OI, Funding, liquidation, leverage, or long / short ratio evidence into direct long, short, stop, or reverse instructions.

### 3.2 EvidenceService

| Test Case | Expected Assertion |
|---|---|
| complete source trace present | evidence records preserve source type, value, timeframe, reason, and ref |
| missingFields present | evidence records preserve missing source information |
| derivatives fixture partial | evidence must mark derivatives evidence as partial |
| external API absent | evidence must not claim external derivatives confirmation |

EvidenceService must preserve traceability and missing-source state.

### 3.3 DecisionEngine

| Test Case | Expected Assertion |
|---|---|
| all required sources complete and blockers absent | review-path candidate evaluation may proceed |
| entry / stop / TP source missing | decision remains `INCOMPLETE` / `WATCH_ONLY` |
| RR source missing | decision remains `INCOMPLETE` / `WATCH_ONLY` |
| liquidity source missing | decision must keep risk action fail-closed |
| active event blocker | decision must downgrade to `WATCH_ONLY` or closed-safe state |
| wick-only unconfirmed | no trend reversal or execution decision may be inferred |

DecisionEngine must not infer executable action from SourceTrace completeness.

### 3.4 Push / Recheck / Watchlist

| Test Case | Expected Assertion |
|---|---|
| complete source trace with manual review | push content remains review-only if emitted |
| missing key source | recheck or watchlist preserves fallback state |
| stampede or extreme stress | no new opportunity push |
| active event blocker | opportunity push is suppressed or marked review-only |
| unconfirmed wick | watchlist may track but must not emit execution semantics |

Push, Recheck, and Watchlist must not emit new position, reverse, or order semantics from source evidence.

### 3.5 ExecutionPlan Readiness

| Test Case | Expected Assertion |
|---|---|
| BoundaryCandidate is `VALID` but SourceTrace is incomplete | readiness remains blocked |
| display-only PlanBoundary exists | ExecutionPlan remains non-executable |
| DTO-only valid candidate exists | ExecutionPlan remains non-executable |
| risk context complete but manual review gate absent | readiness remains blocked |
| liquidity source missing | readiness must fail closed |

BoundaryCandidate `VALID` is not ExecutionPlan readiness.

ExecutionPlan readiness requires explicit verified sources, risk context, review gates, and separately approved implementation.

### 3.6 RiskActionGuard

| Test Case | Expected Assertion |
|---|---|
| high risk evidence present | no direct stop-loss action |
| high risk with directional pressure | no direct reverse action |
| wick-only spike | no trend reversal classification without confirmation |
| stampede state | new position, reverse, and opportunity push remain blocked |
| liquidity worsening | no one-shot market exit recommendation from this layer |
| derivatives data missing | fail-closed state remains active |

RiskActionGuard must remain conservative, review-oriented, and non-trading.

## 4. Fallback / Missing Data Trigger Rules

P6 tests must explicitly verify fallback behavior.

| Missing Or Degraded Area | Required Fallback | Test Expectation |
|---|---|---|
| RuntimeKlineContext missing | `INCOMPLETE` | BoundaryCandidate must not become `VALID` |
| OHLCV / kline window missing | `INCOMPLETE` | Source Assembler reports missing runtime context |
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

## 5. Risk Action Guard Boundary Verification

P6 tests must preserve these guardrails:

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

## 6. Recommended Next Steps For P7

P7 should only begin after P6 fixture and test coverage is reviewed.

Recommended P7 actions:

1. Create a P7 verification report for P6 fixture and module integration test results.
2. Decide whether Source Assembler implementation is ready for a narrow Java implementation phase.
3. If implementation is approved, create a minimal implementation checklist before editing code.
4. Keep external derivatives API integration deferred until internal fixtures and fallback behavior are stable.
5. Keep schema and dashboard changes deferred unless a separate approved plan exists.
6. Keep ExecutionPlan non-executable until source completeness, risk context, review gates, and readiness logic are independently verified.

P7 must still avoid:

- external API integration
- automatic execution semantics
- direct long / short / stop / reverse interpretation from derivatives evidence
- deriving BoundaryCandidate `VALID` from missing source data
- deriving executable ExecutionPlan from display-only or DTO-only state

## 7. Current Conclusion

P6 defines the small-scale test plan for Source Assembler output and module integration checks.

The required testing posture is:

- use controlled fixtures only
- verify traceable source completeness
- preserve missingFields
- trigger explicit fallback
- keep BoundaryCandidate `VALID` as review-only candidate state
- keep ExecutionPlan non-executable from missing source, display-only state, or DTO-only state
- keep Risk Action Guard fail-closed and non-trading

This phase remains documentation-only and does not connect external APIs or implement automated trading.
