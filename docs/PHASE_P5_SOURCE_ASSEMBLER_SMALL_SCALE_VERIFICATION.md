# PHASE_P5_SOURCE_ASSEMBLER_SMALL_SCALE_VERIFICATION

## 1. Document Purpose

This document defines the P5 small-scale production verification baseline for Source Assembler and downstream module integration in Trade Model V1.

The goal is to verify, in a controlled test environment, whether Source Assembler outputs are structurally complete, traceable, and safely consumed by dependent modules.

This document is planning and verification guidance only.

This phase does not:

- modify Java code
- modify configuration
- modify schema
- modify dashboard
- connect Coinglass or any external API
- generate execution plans
- generate automated trading code

BoundaryCandidate `VALID` remains a review candidate.

ExecutionPlan must not become executable from display-only, DTO-only, or incomplete source state.

## 2. Source Assembler Small-Scale Production Verification Steps

Small-scale verification should use controlled test data or approved internal fixtures only.

The verification objective is to prove that Source Assembler can assemble traceable sources without inventing missing values.

### 2.1 Verification Scope

| Source Area | Required Output | Verification Method | Required Fallback When Missing |
|---|---|---|---|
| entry source | entry price, source type, source value, source timeframe, source reason, source ref | Verify every numeric entry value maps to an explicit source trace | `INCOMPLETE` / `WATCH_ONLY` |
| stop source | stop price, invalidation reference, source type, source value, source timeframe, source reason, source ref | Verify stop is derived from traceable structure, not hardcoded | `INCOMPLETE` / `WATCH_ONLY` |
| TP source | TP ladder, TP price source, source type, source value, source timeframe, source reason, source ref | Verify each TP level has traceable numeric source | `INCOMPLETE` / `WATCH_ONLY` |
| RR source | RR input values and rule reference | Verify RR is derived from entry / stop / TP sources | `INCOMPLETE` / `WATCH_ONLY` |
| liquidity source | liquidity state, liquidity stress reason, source ref | Verify liquidity state has source evidence | `SAFE_FAIL_CLOSED_ONLY` |
| multi-timeframe source | timeframe alignment / conflict evidence | Verify higher and lower timeframe evidence is explicit | `WATCH_ONLY` |
| event window source | active blocker list and source ref | Verify event window blocker is explicit before allowing VALID | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |
| wick confirmation source | wick event classification and confirmation source | Verify wick is not treated as trend reversal without confirmation | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |

### 2.2 Small-Scale Data Set Requirements

The P5 test data set should include:

- one complete source case where entry / stop / TP / RR / liquidity / multi-timeframe / event / wick sources are all traceable
- one missing entry source case
- one missing stop source case
- one missing TP source case
- one missing RR input case
- one missing liquidity source case
- one missing multi-timeframe source case
- one active event window blocker case
- one wick-only case without trend confirmation
- one degraded data quality case

No case may rely on external derivatives APIs during P5.

If a fixture simulates OI, Funding, liquidation, or leverage evidence, the fixture must clearly mark the data as test evidence and not production external data.

### 2.3 Source Integrity Checks

Each assembled source must be checked for:

- non-empty source type
- non-null source value when numeric source is required
- source timeframe when timeframe affects interpretation
- source reason
- source field or source ref
- missing field list when input is unavailable
- no generated value when source evidence is absent

Source Assembler must fail closed when required source evidence is missing.

## 3. Module Integration Test Verification Steps

P5 verification should confirm that downstream modules consume Source Assembler output safely.

This document does not implement those integrations. It defines the expected verification checks.

### 3.1 ScoreService

| Check | Expected Result |
|---|---|
| Complete derivatives and source trace context is present | Score may consume risk context as evidence, not as trade instruction |
| OI / Funding / liquidation / leverage evidence is missing | Score must mark missing evidence and avoid overconfident risk scoring |
| liquidity source is missing | Score must downgrade confidence or fail closed according to risk policy |
| wick-only source is present without confirmation | Score must not treat wick as confirmed trend reversal |

ScoreService must not convert Funding, OI, or liquidation evidence directly into long / short / stop / reverse instructions.

### 3.2 EvidenceService

| Check | Expected Result |
|---|---|
| SourceTrace contains complete entry / stop / TP / RR sources | Evidence record can reference traceable numeric source fields |
| SourceTrace contains missingFields | Evidence record must preserve missing evidence information |
| DerivativesRiskContext is partial | Evidence must mark derivatives evidence as partial or unavailable |
| external API source is absent | Evidence must not claim external derivatives confirmation |

EvidenceService should preserve source trace, not invent source certainty.

### 3.3 DecisionEngine

| Check | Expected Result |
|---|---|
| SourceTrace complete and blockers absent | DecisionEngine may allow review-path candidate evaluation |
| entry / stop / TP / RR source missing | DecisionEngine must keep result `INCOMPLETE` / `WATCH_ONLY` |
| liquidity source missing or stress unknown | DecisionEngine must keep risk action fail-closed |
| event blocker active | DecisionEngine must downgrade to `WATCH_ONLY` or closed-safe state |
| wick-only case without confirmation | DecisionEngine must not treat wick as trend reversal |

DecisionEngine must not infer executable action from SourceTrace completeness alone.

### 3.4 Push / Recheck / Watchlist

| Check | Expected Result |
|---|---|
| SourceTrace complete but manual review required | Push may show review-only information if policy allows |
| stampede / extreme stress state present | No new opportunity push |
| missing key source fields | Recheck / Watchlist should preserve waiting or fallback state |
| event window blocker active | Push opportunity should be suppressed or marked review-only |
| wick-only unconfirmed | Watchlist may track, but must not emit execution semantics |

Push, Recheck, and Watchlist must preserve manual review and non-execution semantics.

### 3.5 ExecutionPlan Readiness

| Check | Expected Result |
|---|---|
| BoundaryCandidate is `VALID` but SourceTrace incomplete | ExecutionPlan readiness must remain blocked |
| display-only PlanBoundary exists | ExecutionPlan must not become executable |
| DTO-only valid candidate exists | ExecutionPlan must not become executable |
| risk context complete but review gate missing | ExecutionPlan readiness must remain review-blocked |
| liquidity source missing | ExecutionPlan readiness must fail closed |

BoundaryCandidate `VALID` is not ExecutionPlan readiness.

ExecutionPlan readiness requires explicit verified sources, risk context, review gates, and future implementation approval.

### 3.6 RiskActionGuard

| Check | Expected Result |
|---|---|
| high risk detected | RiskActionGuard must not directly produce stop-loss action |
| high risk detected with directional pressure | RiskActionGuard must not directly produce reverse action |
| wick-only spike detected | RiskActionGuard must not classify it as trend reversal without confirmation |
| stampede state detected | New position, reverse, and opportunity push must remain blocked |
| liquidity worsening detected | No one-shot market exit recommendation from this layer |
| derivatives data missing | RiskActionGuard must remain fail-closed |

RiskActionGuard must remain conservative until verified data sources and review gates are complete.

## 4. Fallback / Missing Data Trigger And Verification

P5 must explicitly verify that missing fields trigger the correct fallback state.

| Missing Or Degraded Area | Required Fallback | Verification Expectation |
|---|---|---|
| RuntimeKlineContext missing | `INCOMPLETE` | BoundaryCandidate must not be promoted to `VALID` |
| OHLCV / kline window missing | `INCOMPLETE` | Source Assembler must report missing runtime source |
| entry source missing | `INCOMPLETE` / `WATCH_ONLY` | No entry value may be forged |
| stop source missing | `INCOMPLETE` / `WATCH_ONLY` | No stop value or invalidation level may be forged |
| TP source missing | `INCOMPLETE` / `WATCH_ONLY` | No TP ladder may be forged |
| RR inputs missing | `INCOMPLETE` / `WATCH_ONLY` | RR must not be inferred without entry / stop / TP inputs |
| liquidity source missing | `SAFE_FAIL_CLOSED_ONLY` | RiskActionGuard and ExecutionPlan readiness remain blocked |
| multi-timeframe source missing | `WATCH_ONLY` | No convergence assumption may be made |
| event window blocker missing | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` | VALID gating must remain conservative |
| wick confirmation source missing | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` | Wick cannot be treated as trend reversal |
| OI history missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `WATCH_ONLY` | Derivatives risk confidence must be limited |
| Funding history missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `WATCH_ONLY` | Funding signal must not become directional action |
| liquidation cluster missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `SAFE_FAIL_CLOSED_ONLY` | Liquidation risk must not become automatic close or reverse |
| leverage distribution missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `SAFE_FAIL_CLOSED_ONLY` | Leverage stress must not be assumed |
| long / short ratio missing | `CLOSED_BASIC_DERIVATIVES_PENDING` / `WATCH_ONLY` | Directional crowding must not be inferred |
| source trace adapter disconnected | `SAFE_FAIL_CLOSED_ONLY` | Display can remain safe, but not production-backed |
| display-only module without source backing | `SAFE_FAIL_CLOSED_ONLY` | No executable state may be derived |

Only DTOs and safe defaults may remain `CLOSED` or `CLOSED_BASIC`.

Modules requiring missing production data must remain `INCOMPLETE_RISK_LOGIC`, `CLOSED_BASIC_DERIVATIVES_PENDING`, or `SAFE_FAIL_CLOSED_ONLY`.

## 5. Risk Action Guard Boundary Verification

P5 verification must preserve the following rules:

- Risk high does not mean direct stop loss.
- Risk high does not mean direct reverse.
- Wick / spike does not mean trend reversal.
- Stampede state blocks new position, reverse, and opportunity push.
- Liquidity worsening does not justify one-shot market exit from this layer.
- Liquidation cluster evidence does not become automatic close.
- Funding / OI / long-short ratio evidence does not become direct long / short signal.
- BoundaryCandidate `VALID` does not mean execution permission.
- ExecutionPlan must not become executable from display-only or DTO-only state.

RiskActionGuard output must remain review-oriented and fail-closed unless future verified implementation explicitly changes the gate.

## 6. Recommended Next Steps For P6

P6 should only begin after P5 verification records are complete.

Recommended P6 actions:

1. Create a controlled fixture set for DerivativesRiskContext and SourceTrace.
2. Add unit tests for Source Assembler complete-source and missing-source cases.
3. Add module integration tests for ScoreService, EvidenceService, DecisionEngine, Push / Recheck / Watchlist, ExecutionPlan readiness, and RiskActionGuard.
4. Record verification results in a dedicated P6 verification document.
5. Keep external derivatives API integration deferred until contracts, fixtures, and fallback tests are stable.
6. Keep ExecutionPlan non-executable until explicit readiness gates and manual review rules are implemented and verified.

P6 must still avoid:

- external API integration
- schema or dashboard changes without a separate approved plan
- automatic execution semantics
- direct long / short / stop / reverse interpretation from derivatives evidence

## 7. Current Conclusion

P5 defines the small-scale production verification baseline for Source Assembler and module integration testing.

This document does not implement Source Assembler and does not connect external derivatives APIs.

The required verification posture is:

- assemble only traceable sources
- preserve missing fields
- trigger explicit fallback
- keep BoundaryCandidate `VALID` as review-only candidate state
- keep ExecutionPlan non-executable from display-only or DTO-only state
- keep Risk Action Guard fail-closed and non-trading

The next phase should create controlled fixtures and executable verification tests without connecting external APIs.
