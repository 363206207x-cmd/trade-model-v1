# PHASE_P3_SOURCE_ASSEMBLER_VERIFICATION_PLAN

## 1. Document Purpose

This document defines the P3 implementation and verification plan for the Source Assembler stage in Trade Model V1.

It is based on:

- `docs/PHASE_V1_MODULE_CLOSURE_REBASE.md`
- `docs/PHASE_DERIVATIVES_RISK_CONTEXT_PLAN.md`
- `docs/PHASE_SOURCE_ASSEMBLER_PLAN.md`

Purpose:

- guide a future minimal Source Assembler implementation
- define verification rules for entry / stop / TP / RR / liquidity / multi-timeframe / event window / wick sources
- define module integration verification strategy
- define fallback behavior when sources are missing
- preserve Risk Action Guard boundaries
- prevent DTO-only or display-only state from becoming executable

This stage does not:

- modify Java code
- modify tests
- modify schema
- modify dashboard
- connect Coinglass
- connect any external derivatives API
- generate execution plans
- generate order or automatic trading logic

This document is a verification plan only.

Baseline status:

- FORMAL BASELINE

## 2. Source Assembler Implementation Guidance

### 2.1 Implementation Scope

Future Source Assembler implementation should be minimal and read-only.

It may assemble:

- `SourceTrace`
- missing source metadata
- source references
- fallback status hints

It must not:

- create orders
- call order API
- close positions
- reverse positions
- produce execution commands
- generate production entry / stop / TP values without traceable sources
- infer missing source reasons
- infer missing source timeframes

### 2.2 Suggested Components

| Component | Responsibility | Required Output | Missing Fallback |
|---|---|---|---|
| `SourceAssembler` | Coordinates all sub-assemblers | complete or fallback-ready SourceTrace | aggregate missing fields |
| `EntrySourceAssembler` | Builds entry numeric source | entry value / type / reason / refs | `INCOMPLETE` / `WATCH_ONLY` |
| `StopSourceAssembler` | Builds stop numeric source | stop value / type / reason / refs | `INCOMPLETE` / `WATCH_ONLY` |
| `TakeProfitSourceAssembler` | Builds TP ladder source | TP values / type / reason / refs | `INCOMPLETE` / `WATCH_ONLY` |
| `RiskRewardSourceAssembler` | Builds RR source | RR value / rule ref | `INCOMPLETE` / `WATCH_ONLY` |
| `LiquiditySourceAssembler` | Builds liquidity source | liquidity state / reason / refs | `SAFE_FAIL_CLOSED_ONLY` |
| `MultiTimeframeSourceAssembler` | Builds multi-timeframe confirmation | alignment / conflict source | `INCOMPLETE` / `WATCH_ONLY` |
| `EventWindowSourceAssembler` | Builds event blocker source | active event blockers | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |
| `WickConfirmationSourceAssembler` | Builds wick source | wick-only risk confirmation | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |

### 2.3 Field Verification Checklist

| Field Group | Required Verification | Failure Handling |
|---|---|---|
| Entry source | value, type, timeframe, reason, source ref are traceable | `INCOMPLETE` / `WATCH_ONLY` |
| Stop source | value, type, timeframe, reason, source ref are traceable | `INCOMPLETE` / `WATCH_ONLY` |
| TP source | each TP level has traceable value, type, reason, source ref | `INCOMPLETE` / `WATCH_ONLY` |
| RR source | RR derives only from traceable entry / stop / TP | `INCOMPLETE` / `WATCH_ONLY` |
| Liquidity source | liquidity state and reason are present | `SAFE_FAIL_CLOSED_ONLY` |
| Multi-timeframe source | confirmation or conflict is explicit | `INCOMPLETE` / `WATCH_ONLY` |
| Event window source | blocker source is explicit | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |
| Wick confirmation source | wick-only source is explicit | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |
| Safety defaults | manual review and non-trading markers are true | fail verification |
| Missing fields | all missing sources are recorded | fail verification |

## 3. Module Integration Verification Strategy

### 3.1 ScoreService

Verification goal:

- ScoreService may consume derivatives risk context only as risk evidence.
- Missing OI / Funding / liquidation / leverage sources must not inflate scores.

Required checks:

- OI history missing -> derivatives pending marker
- Funding history missing -> derivatives pending marker
- liquidation cluster missing -> no strong risk conclusion
- leverage distribution missing -> no leverage-pressure conclusion
- current OI and current funding remain weak evidence only

Must not verify:

- Funding as long signal
- Funding as short signal
- OI as long signal
- OI as short signal
- liquidation as stop or reverse signal

### 3.2 EvidenceService

Verification goal:

- EvidenceService emits derivatives evidence only when source-backed.
- Missing derivatives sources become explicit missing evidence, not inferred conclusions.

Required checks:

- OI history evidence includes source ref when available
- Funding history evidence includes source ref when available
- liquidation evidence requires cluster source
- leverage evidence requires leverage distribution source
- liquidity evidence requires liquidity source
- weak evidence is labeled as weak or derivatives pending

### 3.3 DecisionEngine

Verification goal:

- DecisionEngine can use derivatives context to reduce confidence or require review.
- DecisionEngine must not convert derivatives evidence into direct trade direction.

Required checks:

- missing derivatives context sets risk completeness false
- missing critical risk source can downgrade to review / watch-only state
- complete risk context can add review reasons, not execution commands

Forbidden outcomes:

- direct long from Funding
- direct short from Funding
- reverse from liquidation
- trend reversal from wick

### 3.4 Push / Recheck / Watchlist

Verification goal:

- Push, Recheck, and Watchlist consume SourceTrace only as safety gates.

Required checks:

- missing entry / stop / TP source blocks opportunity push
- missing liquidity source fail-closes risk action
- missing event blocker keeps WATCH_ONLY or fail-closed
- missing wick confirmation keeps WATCH_ONLY or fail-closed
- missing derivatives context prevents executable recheck state

Forbidden outcomes:

- opportunity push from OI alone
- opportunity push from Funding alone
- reverse trade from liquidation cluster
- new position from incomplete SourceTrace

### 3.5 ExecutionPlan Readiness

Verification goal:

- ExecutionPlan readiness remains review-only until PlanBoundary, SourceTrace, and risk context are complete.

Required checks:

- missing SourceTrace -> not executable
- missing DerivativesRiskContext -> not executable
- missing liquidity source -> not executable
- DTO-only VALID -> not executable
- display-only VALID -> not executable

Allowed outcome:

- review-only readiness with manual review required

Forbidden outcome:

- executable state from DTO-only or display-only data

### 3.6 RiskActionGuard

Verification goal:

- RiskActionGuard preserves fail-closed behavior for missing risk sources.

Required checks:

- missing liquidity source -> `SAFE_FAIL_CLOSED_ONLY`
- missing liquidation source -> `SAFE_FAIL_CLOSED_ONLY`
- missing leverage source -> `SAFE_FAIL_CLOSED_ONLY`
- stampede unknown -> no new position / no reverse / no opportunity push
- high risk -> manual review, not direct execution

Forbidden outcomes:

- high risk -> direct stop loss
- high risk -> direct reverse trade
- wick -> trend reversal
- liquidation cluster -> automatic close
- liquidity worsening -> one-shot market exit

## 4. Fallback / Missing Data Handling Rules

| Missing Source | Affected Area | Required Fallback | Verification Expectation |
|---|---|---|---|
| RuntimeKlineContext | BoundaryCandidateService | `INCOMPLETE` | VALID must not be produced |
| entry source | SourceTrace / BoundaryCandidateService | `INCOMPLETE` / `WATCH_ONLY` | entry must remain missing |
| stop source | SourceTrace / BoundaryCandidateService | `INCOMPLETE` / `WATCH_ONLY` | stop must remain missing |
| TP source | SourceTrace / BoundaryCandidateService | `INCOMPLETE` / `WATCH_ONLY` | TP ladder must remain missing |
| RR source | SourceTrace / ExecutionPlan readiness | `INCOMPLETE` / `WATCH_ONLY` | RR must not be inferred |
| liquidity source | RiskActionGuard / ExecutionPlan readiness | `SAFE_FAIL_CLOSED_ONLY` | all trading action flags false |
| multi-timeframe source | BoundaryCandidateService / DecisionEngine | `INCOMPLETE` / `WATCH_ONLY` | no assumed convergence |
| event window source | BoundaryCandidateService / Push / Watchlist | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` | no event-safe assumption |
| wick confirmation source | BoundaryCandidateService / RiskActionGuard | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` | no trend reversal inference |
| OI history | Score / Evidence / Decision | `CLOSED_BASIC_DERIVATIVES_PENDING` | current OI remains weak evidence |
| Funding history | Score / Evidence / Decision | `CLOSED_BASIC_DERIVATIVES_PENDING` | last funding remains weak evidence |
| liquidation cluster | Evidence / RiskActionGuard | `SAFE_FAIL_CLOSED_ONLY` | no automatic exit |
| leverage distribution | Score / RiskActionGuard | `SAFE_FAIL_CLOSED_ONLY` | no automatic leverage action |

Global verification rules:

- missing fields must be explicit
- missing fields must not be silently converted to neutral truth
- SourceTrace completeness must be verified before BoundaryCandidate `VALID`
- DerivativesRiskContext completeness must be verified before ExecutionPlan readiness
- display-only state must never become executable
- DTO-only state must never become executable
- safety defaults must remain enabled

## 5. Risk Action Guard Usage Constraints

All future implementation and verification must preserve:

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

Safety flags must remain:

- `manualReviewRequired = true`
- `notTradeInstruction = true`

RiskActionGuard must not:

- call order API
- create orders
- close positions
- reverse positions
- emit automatic trading commands

## 6. Recommended Next Steps (P4)

1. Create Source Assembler implementation checklist.
2. Perform read-only scope confirmation of existing DTOs, services, adapters, and tests.
3. Decide whether `DerivativesRiskContext` and `SourceTrace` should be DTOs, interfaces, or internal service models.
4. Define unit tests for each missing-source fallback path.
5. Implement minimal Source Assembler skeleton only after checklist closure.
6. Keep external API integration deferred.
7. Keep BoundaryCandidateService VALID integration deferred until Source Assembler verification is complete.
8. Keep ExecutionPlan readiness deferred until PlanBoundary and RiskActionGuard gates are verified.
9. Keep order API and automatic trading out of scope.

## 7. Current Conclusion

P3 defines how a future Source Assembler implementation should be verified before module integration.

The verification baseline is:

- Source Assembler must assemble only traceable sources.
- Missing sources must produce explicit fallback.
- BoundaryCandidate `VALID` must not be inferred from missing source data.
- ExecutionPlan must not become executable from display-only or DTO-only state.
- RiskActionGuard must remain fail-closed.
- No external derivatives API is connected in this phase.
- No execution plan or automatic trading code is generated in this phase.
