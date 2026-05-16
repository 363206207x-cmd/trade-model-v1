# PHASE_SOURCE_ASSEMBLER_PLAN

## 1. Document Purpose

This document defines the P2 Source Assembler and module integration plan for Trade Model V1.

It is based on:

- `docs/PHASE_V1_MODULE_CLOSURE_REBASE.md`
- `docs/PHASE_DERIVATIVES_RISK_CONTEXT_PLAN.md`

Purpose:

- define the Source Assembler module structure
- define mapping from available inputs into `SourceTrace`
- define how modules should consume `DerivativesRiskContext` and `SourceTrace`
- define fallback behavior when required sources are missing
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

This document is a design baseline only.

Baseline status:

- FORMAL BASELINE

## 2. Source Assembler Module Structure And Field Mapping

### 2.1 Source Assembler Responsibility

`SourceAssembler` should be a read-only assembly layer that converts traceable market, structure, event, and risk inputs into a `SourceTrace` object.

It must not:

- invent entry price
- invent stop price
- invent TP price
- invent RR
- infer missing source timeframe
- infer missing source reason
- produce order instructions
- produce automatic trading actions

If a required source is missing, the assembler must record missing fields and return a fallback-ready SourceTrace.

### 2.2 Proposed Sub-Assemblers

| Sub-Assembler | Output Area | Required Inputs | Missing Fallback |
|---|---|---|---|
| `EntrySourceAssembler` | entry numeric source | support / resistance / pullback / structure source | `INCOMPLETE` / `WATCH_ONLY` |
| `StopSourceAssembler` | stop numeric source | swing high / swing low / invalidation level / buffer rule | `INCOMPLETE` / `WATCH_ONLY` |
| `TakeProfitSourceAssembler` | TP numeric source | RR ladder / resistance / support / liquidity levels | `INCOMPLETE` / `WATCH_ONLY` |
| `RiskRewardSourceAssembler` | RR numeric source | entry / stop / TP sources | `INCOMPLETE` / `WATCH_ONLY` |
| `LiquiditySourceAssembler` | liquidity state source | liquidity stress / spread / depth / slippage source | `SAFE_FAIL_CLOSED_ONLY` |
| `MultiTimeframeSourceAssembler` | multi-timeframe confirmation source | higher / lower timeframe structure | `INCOMPLETE` / `WATCH_ONLY` |
| `EventWindowSourceAssembler` | event window blocker | scheduled event / hot reset / market event source | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |
| `WickConfirmationSourceAssembler` | wick confirmation source | spike / wick / sweep / confirmation evidence | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |

### 2.3 SourceTrace Field Mapping

| SourceTrace Field | Assembler | Mapping Rule | Missing Behavior |
|---|---|---|---|
| `entryPriceSource` | EntrySourceAssembler | Use traceable entry source value only | `null`, mark `entrySourceMissing` |
| `entrySourceType` | EntrySourceAssembler | Use explicit source type such as `support`, `pullback`, `structure_level` | mark missing when absent |
| `entrySourceReason` | EntrySourceAssembler | Use human-reviewable source reason | mark missing when absent |
| `stopPriceSource` | StopSourceAssembler | Use traceable stop / invalidation source value only | `null`, mark `stopSourceMissing` |
| `stopSourceType` | StopSourceAssembler | Use explicit source type such as `swing_low`, `swing_high`, `structure_invalidated` | mark missing when absent |
| `stopSourceReason` | StopSourceAssembler | Use human-reviewable stop reason | mark missing when absent |
| `tpPriceSources` | TakeProfitSourceAssembler | Use traceable TP ladder values only | empty list, mark `tpSourceMissing` |
| `tpSourceType` | TakeProfitSourceAssembler | Use explicit source type such as `rr_ladder`, `resistance`, `liquidity_level` | mark missing when absent |
| `tpSourceReason` | TakeProfitSourceAssembler | Use human-reviewable TP reason | mark missing when absent |
| `rrSource` | RiskRewardSourceAssembler | Calculate only from traceable entry / stop / TP sources | `null`, mark `rrSourceMissing` |
| `rrRuleRef` | RiskRewardSourceAssembler | Reference the applied RR rule | mark missing when absent |
| `liquiditySource` | LiquiditySourceAssembler | Use liquidity stress source only | `SAFE_FAIL_CLOSED_ONLY` |
| `multiTimeframeSource` | MultiTimeframeSourceAssembler | Use explicit multi-timeframe confirmation / conflict source | `INCOMPLETE` / `WATCH_ONLY` |
| `eventSource` | EventWindowSourceAssembler | Use event window blocker source | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |
| `wickSource` | WickConfirmationSourceAssembler | Use wick-only confirmation source | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |
| `sourceRefs` | All assemblers | Preserve evidence / decision / source references | empty list with missing marker |
| `missingFields` | All assemblers | Aggregate all missing required fields | drives fallback |
| `manualReviewRequired` | SourceAssembler | Always set true | must not be disabled |
| `notTradeInstruction` | SourceAssembler | Always set true | must not be disabled |

### 2.4 DerivativesRiskContext Mapping

| DerivativesRiskContext Field | Source / Dependency | Module Use | Missing Behavior |
|---|---|---|---|
| `openInterestHistory` | OI history source | Score / Evidence / Decision context | weak evidence only, derivatives pending |
| `openInterestDelta` | OI current and previous snapshot | Score / Evidence / crowding label | null when unavailable |
| `fundingHistory` | Funding history source | Score / Evidence / sentiment / macro | weak evidence only when missing |
| `lastFundingRate` | Current funding source | weak explanatory evidence | not a long / short signal |
| `liquidationCluster` | liquidation cluster source | RiskActionGuard / Evidence / Dashboard | `SAFE_FAIL_CLOSED_ONLY` when missing |
| `leverageDistribution` | leverage source | leverage pressure evaluation | `SAFE_FAIL_CLOSED_ONLY` when missing |
| `longShortRatio` | long / short ratio source | sentiment / crowding evidence | weak evidence only |
| `liquidityStress` | liquidity source | RiskActionGuard / ExecutionPlan readiness | `SAFE_FAIL_CLOSED_ONLY` when missing |
| `eventWindowBlockers` | event source | BoundaryCandidate / Push / Watchlist | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |
| `wickConfirmationSources` | wick source | BoundaryCandidate / RiskActionGuard | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` |
| `missingFields` | all sources | fallback driver | must be propagated |

## 3. Module Integration Strategy

### 3.1 ScoreService

ScoreService may consume `DerivativesRiskContext` only as risk evidence input.

Allowed use:

- lower confidence when derivatives risk sources are missing
- label derivatives-related scores as pending or weak evidence
- use complete OI / Funding history when available
- avoid inflating score from placeholder or current-only values

Forbidden use:

- interpreting Funding as long / short instruction
- interpreting OI as long / short instruction
- interpreting liquidation as stop / reverse instruction

Fallback:

- missing OI history -> `CLOSED_BASIC_DERIVATIVES_PENDING`
- missing Funding history -> `CLOSED_BASIC_DERIVATIVES_PENDING`
- missing liquidity / liquidation / leverage source -> fail closed for risk-action consumers

### 3.2 EvidenceService

EvidenceService may emit derivatives evidence only when the source is explicit and traceable.

Allowed evidence types:

- OI history evidence
- Funding history evidence
- liquidation cluster evidence
- leverage distribution evidence
- liquidity stress evidence
- event window evidence
- wick confirmation evidence

Required labels:

- source-backed
- weak evidence when partial
- missing source when unavailable
- not a trading instruction

Fallback:

- missing source -> record missing evidence, do not infer risk conclusion

### 3.3 DecisionEngine

DecisionEngine may consume derivatives risk context as one part of decision context.

Allowed use:

- reduce confidence
- increase review requirements
- downgrade worth-opening
- add review reasons
- trigger WATCH_ONLY when critical sources are missing

Forbidden use:

- directly opening long from positive Funding
- directly opening short from negative Funding
- reversing from liquidation cluster
- treating wick as trend reversal

Fallback:

- missing derivatives context -> basic decision may remain available, but derivatives risk completeness must be false

### 3.4 Push / Recheck / Watchlist

Push / Recheck / Watchlist may consume SourceTrace and DerivativesRiskContext only as safety gates.

Allowed use:

- block opportunity push when liquidity / liquidation / leverage source is missing and required
- downgrade to waiting when event or wick confirmation is missing
- require manual review for derivatives risk gaps
- preserve existing recheck status semantics

Forbidden use:

- using OI / Funding / liquidation to create new opportunity push automatically
- generating reverse trade
- generating new position
- turning missing source into valid executable state

Fallback:

- missing high-risk source -> `SAFE_FAIL_CLOSED_ONLY`
- missing entry / stop / TP source -> no opportunity push
- missing event / wick source -> `WATCH_ONLY`

### 3.5 ExecutionPlan Readiness

ExecutionPlan readiness may consume SourceTrace only after PlanBoundary sources are complete.

Allowed use:

- determine review-only readiness
- mark missing source reasons
- keep not executable when source is missing

Forbidden use:

- becoming executable from DTO-only state
- becoming executable from display-only state
- bypassing PlanBoundary
- bypassing RiskActionGuard

Fallback:

- missing SourceTrace -> not executable
- missing DerivativesRiskContext -> not executable
- missing liquidity / liquidation source -> manual review and fail-closed

### 3.6 RiskActionGuard

RiskActionGuard must consume liquidity, liquidation, leverage, stampede, event, and wick sources only as safety constraints.

Allowed use:

- block new position
- block reverse trade
- block opportunity push
- require manual review
- mark liquidity source missing

Forbidden use:

- high risk -> direct stop loss
- high risk -> direct reverse trade
- wick -> trend reversal
- liquidation cluster -> automatic exit

Fallback:

- missing liquidity source -> `SAFE_FAIL_CLOSED_ONLY`
- missing liquidation source -> `SAFE_FAIL_CLOSED_ONLY`
- missing leverage source -> `SAFE_FAIL_CLOSED_ONLY`
- stampede unknown -> no new position / no reverse / no opportunity push

## 4. Fallback / Missing Data Handling Rules

| Missing Area | Target Module | Required Fallback | Notes |
|---|---|---|---|
| entry source | BoundaryCandidateService / SourceTrace | `INCOMPLETE` / `WATCH_ONLY` | Do not forge entry |
| stop source | BoundaryCandidateService / SourceTrace | `INCOMPLETE` / `WATCH_ONLY` | Do not forge stop |
| TP source | BoundaryCandidateService / SourceTrace | `INCOMPLETE` / `WATCH_ONLY` | Do not forge TP |
| RR source | BoundaryCandidateService / ExecutionPlan readiness | `INCOMPLETE` / `WATCH_ONLY` | RR requires traceable inputs |
| liquidity source | RiskActionGuard / ExecutionPlan readiness | `SAFE_FAIL_CLOSED_ONLY` | Keep all trading actions false |
| multi-timeframe source | BoundaryCandidateService / DecisionEngine | `INCOMPLETE` / `WATCH_ONLY` | Do not assume convergence |
| event window source | BoundaryCandidateService / Push / Watchlist | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` | Do not assume event safety |
| wick confirmation source | BoundaryCandidateService / RiskActionGuard | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` | Wick does not imply reversal |
| OI history | Score / Evidence / Decision | `CLOSED_BASIC_DERIVATIVES_PENDING` | Current OI is weak evidence only |
| Funding history | Score / Evidence / Decision | `CLOSED_BASIC_DERIVATIVES_PENDING` | Last funding is weak evidence only |
| liquidation cluster | Evidence / RiskActionGuard | `SAFE_FAIL_CLOSED_ONLY` | No automatic exit |
| leverage distribution | Score / RiskActionGuard | `SAFE_FAIL_CLOSED_ONLY` | No automatic leverage action |

Global rules:

- Missing production data must be explicit in `missingFields`.
- Missing source must not be silently converted to neutral truth.
- SourceTrace completeness must be checked before BoundaryCandidate `VALID`.
- DerivativesRiskContext completeness must be checked before ExecutionPlan readiness.
- Display-only state must never become executable.
- DTO-only state must never become executable.

## 5. Risk Boundary Constraints

All Source Assembler and integration stages must preserve Risk Action Guard:

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

Non-trading requirements:

- `manualReviewRequired` must remain true
- `notTradeInstruction` must remain true
- Source Assembler must not call order API
- Source Assembler must not create orders
- Source Assembler must not close positions
- Source Assembler must not reverse positions
- Source Assembler must not produce automatic trading commands

## 6. Recommended Next Steps (P3)

1. Create SourceAssembler implementation checklist.
2. Confirm existing DTOs and service signatures by read-only inspection.
3. Decide whether SourceTrace and DerivativesRiskContext should be DTOs, interfaces, or internal service models.
4. Define unit test scope for missing-source fallback behavior.
5. Implement only the minimal SourceAssembler skeleton after checklist closure.
6. Keep external API integration deferred.
7. Keep BoundaryCandidateService integration deferred until SourceAssembler verification is complete.
8. Keep ExecutionPlan readiness deferred until PlanBoundary and RiskActionGuard gates are verified.
9. Keep order API and automatic trading out of scope.

## 7. Current Conclusion

P2 defines how Source Assembler should structure traceable sources and how modules may consume `DerivativesRiskContext` and `SourceTrace`.

This stage does not implement the assembler.

This stage does not connect external derivatives APIs.

This stage does not promote BoundaryCandidate `VALID` to execution readiness.

This stage preserves fail-closed behavior for all missing risk sources.
