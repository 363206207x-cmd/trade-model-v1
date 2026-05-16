# PHASE_P4_SOURCE_ASSEMBLER_EXECUTION_VERIFICATION

## 1. Document Purpose

This document defines the P4 Source Assembler execution verification baseline for Trade Model V1.

It is based on:

- `docs/PHASE_DERIVATIVES_RISK_CONTEXT_PLAN.md`
- `docs/PHASE_SOURCE_ASSEMBLER_PLAN.md`
- `docs/PHASE_P3_SOURCE_ASSEMBLER_VERIFICATION_PLAN.md`

Purpose:

- provide a verification template for a future Source Assembler implementation
- define execution-style verification steps for source assembly
- define module integration verification records
- define missing-source fallback validation
- preserve Risk Action Guard and non-trading semantics

This stage does not:

- modify Java code
- modify tests
- modify schema
- modify dashboard
- connect Coinglass
- connect any external derivatives API
- generate execution plans
- generate order or automatic trading logic

This document is a verification baseline only.

Baseline status:

- FORMAL BASELINE

## 2. Source Assembler Verification Checklist

### 2.1 Source Assembly Coverage

| Source Area | Required Assembly Result | Required Verification | Missing Fallback |
|---|---|---|---|
| entry source | entry value, type, timeframe, reason, source ref | all fields traceable and non-forged | `INCOMPLETE` / `WATCH_ONLY` |
| stop source | stop value, type, timeframe, reason, source ref | all fields traceable and non-forged | `INCOMPLETE` / `WATCH_ONLY` |
| TP source | TP ladder values, type, timeframe, reason, source refs | every TP level traceable and non-forged | `INCOMPLETE` / `WATCH_ONLY` |
| RR source | RR value and rule ref | derived only from entry / stop / TP sources | `INCOMPLETE` / `WATCH_ONLY` |
| liquidity source | liquidity state, reason, source ref | source-backed liquidity state | `SAFE_FAIL_CLOSED_ONLY` |
| multi-timeframe source | alignment or conflict source | explicit timeframe source and reason | `INCOMPLETE` / `WATCH_ONLY` |
| event window source | active blockers and source refs | explicit event source | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |
| wick confirmation source | wick-only risk source and reason | explicit wick source, no reversal inference | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |

### 2.2 Required Safety Fields

| Field | Expected Value | Verification |
|---|---|---|
| `manualReviewRequired` | `true` | Must not be disabled by Source Assembler |
| `notTradeInstruction` | `true` | Must not be disabled by Source Assembler |
| `missingFields` | explicit list | Must include every missing source |
| `sourceRefs` | source-backed refs or empty with missing marker | Must not include fabricated refs |

### 2.3 Non-Forging Verification

Future verification must confirm Source Assembler does not forge:

- entry price
- stop price
- TP price
- RR
- source timeframe
- source reason
- source reference
- liquidity state
- event blocker
- wick confirmation

If any value cannot be traced, it must remain missing and trigger fallback.

## 3. Module Integration Execution Verification Steps

### 3.1 ScoreService Verification

Goal:

- Verify ScoreService consumes derivatives context only as risk evidence.

Execution verification steps:

1. Provide complete OI history and verify score can reference it as source-backed evidence.
2. Remove OI history and verify derivatives score remains basic / pending.
3. Provide complete Funding history and verify score can reference it as source-backed evidence.
4. Remove Funding history and verify last funding remains weak evidence only.
5. Remove liquidation / leverage / liquidity sources and verify no strong risk conclusion is produced.

Expected result:

- ScoreService never converts Funding, OI, or liquidation into long / short / stop / reverse instructions.

### 3.2 EvidenceService Verification

Goal:

- Verify EvidenceService emits derivatives evidence only when source-backed.

Execution verification steps:

1. Verify OI history evidence requires OI history source.
2. Verify Funding history evidence requires Funding history source.
3. Verify liquidation evidence requires liquidation cluster source.
4. Verify leverage evidence requires leverage distribution source.
5. Verify missing derivatives source becomes missing evidence or weak evidence.

Expected result:

- Evidence remains explanatory and non-trading.

### 3.3 DecisionEngine Verification

Goal:

- Verify DecisionEngine uses derivatives context only for confidence, review, and fallback decisions.

Execution verification steps:

1. Provide complete derivatives context and verify it may add review reasons.
2. Remove derivatives context and verify derivatives risk completeness is false.
3. Remove critical risk source and verify decision can be downgraded to review / WATCH_ONLY.
4. Verify no direct long / short / stop / reverse decision is derived from Funding, OI, liquidation, or wick source.

Expected result:

- Decision remains advisory and review-oriented when derivatives risk is incomplete.

### 3.4 Push / Recheck / Watchlist Verification

Goal:

- Verify Push, Recheck, and Watchlist use SourceTrace only as safety gates.

Execution verification steps:

1. Remove entry source and verify opportunity push is blocked.
2. Remove stop source and verify opportunity push is blocked.
3. Remove TP source and verify opportunity push is blocked.
4. Remove liquidity source and verify risk gate is fail-closed.
5. Remove event window source and verify WATCH_ONLY / SAFE_FAIL_CLOSED_ONLY.
6. Remove wick confirmation source and verify WATCH_ONLY / SAFE_FAIL_CLOSED_ONLY.
7. Verify missing derivatives context prevents executable recheck state.

Expected result:

- No opportunity push, reverse trade, or new position is created from incomplete SourceTrace.

### 3.5 ExecutionPlan Readiness Verification

Goal:

- Verify ExecutionPlan readiness remains non-executable unless PlanBoundary, SourceTrace, derivatives risk context, and RiskActionGuard gates are complete.

Execution verification steps:

1. Verify DTO-only BoundaryCandidate `VALID` does not produce executable readiness.
2. Verify display-only PlanBoundary `VALID` does not produce executable readiness.
3. Remove SourceTrace and verify ExecutionPlan remains not executable.
4. Remove DerivativesRiskContext and verify ExecutionPlan remains not executable.
5. Remove liquidity source and verify ExecutionPlan remains not executable.
6. Verify review-only readiness keeps manual review required.

Expected result:

- ExecutionPlan does not become executable from missing, DTO-only, or display-only source state.

### 3.6 RiskActionGuard Verification

Goal:

- Verify RiskActionGuard remains fail-closed under missing or high-risk source states.

Execution verification steps:

1. Remove liquidity source and verify `SAFE_FAIL_CLOSED_ONLY`.
2. Remove liquidation source and verify `SAFE_FAIL_CLOSED_ONLY`.
3. Remove leverage source and verify `SAFE_FAIL_CLOSED_ONLY`.
4. Mark stampede unknown and verify no new position / no reverse / no opportunity push.
5. Mark high risk and verify manual review, not direct stop loss.
6. Mark wick-only risk and verify no trend reversal inference.
7. Mark liquidity worsening and verify no one-shot market exit.

Expected result:

- RiskActionGuard never emits automatic trading actions.

## 4. Fallback / Missing Data Handling Verification

| Missing Source | Expected Fallback | Must Verify |
|---|---|---|
| RuntimeKlineContext | `INCOMPLETE` | BoundaryCandidate `VALID` not produced |
| entry source | `INCOMPLETE` / `WATCH_ONLY` | entry remains missing and non-forged |
| stop source | `INCOMPLETE` / `WATCH_ONLY` | stop remains missing and non-forged |
| TP source | `INCOMPLETE` / `WATCH_ONLY` | TP ladder remains missing and non-forged |
| RR source | `INCOMPLETE` / `WATCH_ONLY` | RR not inferred |
| liquidity source | `SAFE_FAIL_CLOSED_ONLY` | all trading action flags false |
| multi-timeframe source | `INCOMPLETE` / `WATCH_ONLY` | no assumed convergence |
| event window source | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` | no event-safe assumption |
| wick confirmation source | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` | no trend reversal inference |
| OI history | `CLOSED_BASIC_DERIVATIVES_PENDING` | current OI remains weak evidence |
| Funding history | `CLOSED_BASIC_DERIVATIVES_PENDING` | last funding remains weak evidence |
| liquidation cluster | `SAFE_FAIL_CLOSED_ONLY` | no automatic exit |
| leverage distribution | `SAFE_FAIL_CLOSED_ONLY` | no automatic leverage action |

Global verification requirements:

- every missing source is recorded in `missingFields`
- missing source is not silently converted to neutral truth
- BoundaryCandidate `VALID` is not inferred from missing source data
- ExecutionPlan is not executable from DTO-only state
- ExecutionPlan is not executable from display-only state
- RiskActionGuard remains fail-closed when critical risk source is missing

## 5. Risk Action Guard Verification

All P4 verification must preserve:

- high risk does not imply direct stop loss
- high risk does not imply direct reverse trade
- wick does not imply trend reversal
- stampede state forbids new positions
- stampede state forbids reverse trades
- stampede state forbids opportunity pushes
- liquidity worsening should not trigger one-shot market exit
- liquidation cluster should not trigger automatic close
- Funding should not imply long or short
- OI should not imply long or short

Safety flags must remain true:

- `manualReviewRequired`
- `notTradeInstruction`

Forbidden outputs:

- order instruction
- automatic order
- automatic close
- automatic reverse
- automatic market exit
- executable plan from missing source

## 6. Recommended Next Steps (P5)

1. Create Source Assembler implementation checklist.
2. Perform read-only scope confirmation of existing DTOs, services, adapters, and tests.
3. Decide final object form for `SourceTrace` and `DerivativesRiskContext`.
4. Define unit test cases for every missing-source fallback row in this document.
5. Implement only minimal Source Assembler skeleton after checklist closure.
6. Verify Source Assembler in isolation before module integration.
7. Keep BoundaryCandidateService VALID integration deferred until Source Assembler verification passes.
8. Keep ExecutionPlan readiness deferred until RiskActionGuard and SourceTrace verification passes.
9. Keep external API integration, order API, and automatic trading out of scope.

## 7. Current Conclusion

P4 defines the execution verification template for Source Assembler and module integration.

This stage does not implement Source Assembler.

This stage does not connect external derivatives APIs.

This stage does not generate execution plans or automatic trading logic.

BoundaryCandidate `VALID` remains a review candidate state.

ExecutionPlan remains non-executable unless future verified sources, risk context, RiskActionGuard, and manual review gates are complete.
