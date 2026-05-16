# PHASE_DERIVATIVES_RISK_CONTEXT_PLAN

## 1. Document Purpose

This document defines the P1 data contract for `DerivativesRiskContext` and `SourceTrace` in Trade Model V1.

Purpose:

- provide a clear and traceable structure for derivatives risk inputs
- describe required OI, Funding, liquidation, leverage, liquidity, event, and wick confirmation fields
- define missing-data and fallback rules before any external API integration
- preserve BoundaryCandidate, ExecutionPlan, and RiskActionGuard safety boundaries

This stage does not:

- modify Java code
- modify tests
- modify schema
- modify dashboard
- connect Coinglass
- connect any external derivatives API
- generate execution plans
- generate order or automatic trading logic

This document is a contract plan only.

Baseline status:

- FORMAL BASELINE

## 2. Data Object Overview

### DerivativesRiskContext

`DerivativesRiskContext` represents derivatives-market risk evidence for one symbol and timeframe.

It should aggregate:

- OI history
- Funding history
- liquidation cluster information
- leverage distribution
- long / short ratio
- liquidity stress
- event window blockers
- wick confirmation sources
- missing-field metadata
- aggregate data quality

`DerivativesRiskContext` is not a trading signal.

### SourceTrace

`SourceTrace` represents traceable numeric and logical sources used by PlanBoundary-related modules.

It should cover:

- entry source
- stop source
- TP source
- RR source
- liquidity source
- multi-timeframe source
- event blocker source
- wick confirmation source

`SourceTrace` must never fabricate entry / stop / TP / RR values.

## 3. DerivativesRiskContext Field Definition

| Field | Type | Description | Required | Missing / Fallback Rule | Notes |
|---|---|---|---|---|---|
| `symbol` | `String` | Asset symbol | Yes | `INCOMPLETE` if missing | Example: `BTCUSDT` |
| `timeframe` | `String` | Analysis timeframe | Yes | `INCOMPLETE` if missing | Example: `5m`, `15m`, `1h`, `4h` |
| `contextTime` | `Instant` / `LocalDateTime` | Context build time | Yes | `INCOMPLETE` if missing | Used for freshness and audit |
| `openInterestHistory` | `List<TimeValue<BigDecimal>>` | Historical OI values | Yes | Empty list plus missing field marker | Must include timestamps when available |
| `openInterestDelta` | `BigDecimal` | Latest OI change | No | `null` when unavailable | Must not be inferred without history or prior snapshot |
| `fundingHistory` | `List<TimeValue<BigDecimal>>` | Historical funding rates | Yes | Empty list plus missing field marker | Current last funding alone is not full history |
| `lastFundingRate` | `BigDecimal` | Latest known funding rate | No | `null` when unavailable | Can be used as weak evidence only |
| `liquidationCluster` | `List<PriceCluster>` | Concentrated liquidation price areas | Yes | Empty list plus missing field marker | Requires traceable source; no hardcoded levels |
| `leverageDistribution` | `Map<String, BigDecimal>` | Leverage distribution by bucket | Yes | Missing field marker; risk modules fail closed | Example buckets: `1-5x`, `5-10x`, `10-20x`, `20x+` |
| `longShortRatio` | `BigDecimal` | Aggregated long / short ratio | Yes | `null` plus missing field marker | Should not imply long or short by itself |
| `liquidityStress` | `String` | Liquidity stress label | Yes | `SAFE_FAIL_CLOSED_ONLY` if missing | Suggested values: `LOW`, `MEDIUM`, `HIGH`, `UNKNOWN` |
| `liquidityStressReason` | `String` | Explanation for liquidity stress | No | `null` when unavailable | Must be source-backed |
| `eventWindowBlockers` | `List<String>` | Active event windows blocking VALID | Yes | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` if missing | Missing event source should not be treated as safe |
| `wickConfirmationSources` | `List<String>` | Wick / spike confirmation evidence | Yes | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` if missing | Wick does not imply trend reversal |
| `derivativesCrowdingState` | `String` | Derived crowding label | No | `UNKNOWN` or `NEUTRAL_WITH_MISSING_SOURCE` | Must record missing inputs if degraded |
| `dataQualityScore` | `BigDecimal` | Aggregate source quality score | Yes | `null` if insufficient data | Must not be inflated by placeholder evidence |
| `missingFields` | `List<String>` | Missing required inputs | Yes | Empty only when all required inputs are present | Drives fallback decisions |
| `sourceRefs` | `List<String>` | Evidence or source references | Yes | Empty list when unavailable | Used for audit and review |
| `notTradeInstruction` | `boolean` | Explicit non-trading marker | Yes | Must be `true` | Safety marker |

## 4. SourceTrace Field Definition

| Field | Type | Description | Required | Missing / Fallback Rule | Notes |
|---|---|---|---|---|---|
| `symbol` | `String` | Asset symbol | Yes | `INCOMPLETE` if missing | Must match context symbol |
| `timeframe` | `String` | Source timeframe | Yes | `INCOMPLETE` if missing | Must be explicit |
| `entryPriceSource` | `BigDecimal` | Traceable source value for entry | Yes | `INCOMPLETE` / `WATCH_ONLY` | Must be `null` if source is missing |
| `entrySourceType` | `String` | Entry source type | Yes | `INCOMPLETE` / `WATCH_ONLY` | Example: `support`, `pullback`, `structure_level` |
| `entrySourceReason` | `String` | Reason entry source is selected | Yes | `INCOMPLETE` / `WATCH_ONLY` | Must be human-reviewable |
| `stopPriceSource` | `BigDecimal` | Traceable source value for stop | Yes | `INCOMPLETE` / `WATCH_ONLY` | Must be `null` if source is missing |
| `stopSourceType` | `String` | Stop source type | Yes | `INCOMPLETE` / `WATCH_ONLY` | Example: `swing_low`, `structure_invalidated` |
| `stopSourceReason` | `String` | Reason stop source is selected | Yes | `INCOMPLETE` / `WATCH_ONLY` | Must not be generic placeholder |
| `tpPriceSources` | `List<BigDecimal>` | Traceable source values for TP ladder | Yes | `INCOMPLETE` / `WATCH_ONLY` | Must not invent TP levels |
| `tpSourceType` | `String` | TP source type | Yes | `INCOMPLETE` / `WATCH_ONLY` | Example: `rr_ladder`, `resistance`, `liquidity_level` |
| `tpSourceReason` | `String` | Reason TP source is selected | Yes | `INCOMPLETE` / `WATCH_ONLY` | Must be source-backed |
| `rrSource` | `BigDecimal` | Traceable risk / reward input | Yes | `INCOMPLETE` / `WATCH_ONLY` | Derived only from entry / stop / TP sources |
| `rrRuleRef` | `String` | RR rule reference | Yes | `INCOMPLETE` / `WATCH_ONLY` | Must cite rule or source field |
| `liquiditySource` | `String` | Source for liquidity state | Yes | `SAFE_FAIL_CLOSED_ONLY` | Missing liquidity must keep RiskActionGuard fail-closed |
| `multiTimeframeSource` | `String` | Multi-timeframe confirmation source | Yes | `INCOMPLETE` / `WATCH_ONLY` | Conflict cannot be assumed safe |
| `eventSource` | `String` | Event window blocker source | Yes | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` | Missing event source should not permit VALID |
| `wickSource` | `String` | Wick-only confirmation source | Yes | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` | Wick does not imply reversal |
| `sourceRefs` | `List<String>` | Evidence and decision references | Yes | Empty list with missing marker | Used for audit |
| `missingFields` | `List<String>` | Missing SourceTrace inputs | Yes | Drives fallback | Empty only when complete |
| `manualReviewRequired` | `boolean` | Explicit manual review marker | Yes | Must be `true` | Safety marker |
| `notTradeInstruction` | `boolean` | Explicit non-trading marker | Yes | Must be `true` | Safety marker |

## 5. Fallback / Missing Data Handling

When required derivatives risk or SourceTrace inputs are missing:

| Missing Area | Required Fallback | Reason |
|---|---|---|
| `symbol` / `timeframe` | `INCOMPLETE` | Cannot evaluate without identity and timeframe |
| OI history | `CLOSED_BASIC_DERIVATIVES_PENDING` / weak evidence only | Current OI does not replace history |
| Funding history | `CLOSED_BASIC_DERIVATIVES_PENDING` / weak evidence only | Last funding does not prove persistent crowding |
| liquidation cluster | `SAFE_FAIL_CLOSED_ONLY` for risk action | Missing liquidation source blocks strong risk action conclusions |
| leverage distribution | `SAFE_FAIL_CLOSED_ONLY` for risk action | Missing leverage source blocks leverage-pressure conclusions |
| long / short ratio | weak evidence only | Ratio must not imply long or short by itself |
| liquidity stress | `SAFE_FAIL_CLOSED_ONLY` | RiskActionGuard must remain fail-closed |
| event window blocker | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` | Major event risk cannot be treated as safe when unknown |
| wick confirmation source | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` | Wick-only risk cannot be treated as trend reversal |
| entry source | `INCOMPLETE` / `WATCH_ONLY` | Service must not forge entry |
| stop source | `INCOMPLETE` / `WATCH_ONLY` | Service must not forge stop |
| TP source | `INCOMPLETE` / `WATCH_ONLY` | Service must not forge TP ladder |
| RR source | `INCOMPLETE` / `WATCH_ONLY` | RR cannot be inferred without traceable inputs |

Global fallback rules:

- DTOs and safe defaults may remain `CLOSED` / `CLOSED_BASIC`.
- Missing production data must keep dependent modules as `INCOMPLETE_RISK_LOGIC`, `CLOSED_BASIC_DERIVATIVES_PENDING`, or `SAFE_FAIL_CLOSED_ONLY`.
- BoundaryCandidate `VALID` must not be inferred from missing source data.
- ExecutionPlan must not become executable from display-only or DTO-only state.
- Funding, OI, and liquidation must not be directly interpreted as long, short, stop loss, close, or reverse signals.

## 6. Usage Constraints

### BoundaryCandidateService

BoundaryCandidateService may output `VALID` only when:

- RuntimeKlineContext is complete and fresh
- entry source is traceable
- stop source is traceable
- TP source is traceable
- RR source is traceable
- source refs are available
- no blocking missing fields exist
- `manualReviewRequired = true`
- `notTradeInstruction = true`

If any required source is missing, BoundaryCandidateService must return or preserve:

- `INCOMPLETE`
- `WATCH_ONLY`

It must not forge:

- entry price
- stop price
- TP price
- RR
- source timeframe
- source reason
- source reference

### ExecutionPlan

ExecutionPlan must remain review-only unless:

- PlanBoundary is complete
- SourceTrace is complete
- DerivativesRiskContext is sufficiently complete
- RiskActionGuard allows review progression
- manual review gates are explicitly satisfied

ExecutionPlan must not become executable from:

- DTO-only state
- display-only state
- missing SourceTrace
- missing derivatives risk context

### RiskActionGuard

RiskActionGuard must continue to enforce:

- high risk does not imply direct stop loss
- high risk does not imply direct reverse trade
- wick does not imply trend reversal
- stampede state forbids new positions
- stampede state forbids reverse trades
- stampede state forbids opportunity pushes
- liquidity worsening should not trigger one-shot market exit
- all trading action flags remain false unless future verified sources and review gates explicitly allow otherwise

## 7. Recommended Next Steps (P2)

1. Create a SourceAssembler plan based on this contract.
2. Keep external APIs disconnected until source contract and tests are reviewed.
3. Define minimal DTOs or interfaces for `DerivativesRiskContext` and `SourceTrace`.
4. Add missing-source semantics to Score, Evidence, Decision, Dashboard display, and Review.
5. Design BoundaryCandidateService VALID integration only after source assembler is defined.
6. Design ExecutionPlan readiness only after PlanBoundary and SourceTrace are complete.
7. Design Push / Recheck / Watchlist risk gates only after derivatives context is available.
8. Keep order API and automatic trading out of scope.

## 8. Current Conclusion

P1 defines the data contract needed before any derivatives risk integration.

The system must not promote basic Funding, OI, liquidation, or leverage evidence into trading action.

BoundaryCandidate `VALID` remains a review candidate state, not an execution state.

ExecutionPlan remains non-executable unless future verified PlanBoundary, SourceTrace, derivatives risk context, RiskActionGuard, and manual review gates are complete.
