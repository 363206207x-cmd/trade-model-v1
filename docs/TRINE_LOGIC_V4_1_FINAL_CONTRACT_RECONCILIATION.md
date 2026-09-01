# TRINE LOGIC v4.1 Final Contract Reconciliation

Status: `OWNER_APPROVED_IMPLEMENTATION_SOURCE`

Package: `FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION`

This document records the Owner's final, bounded reconciliation for the v4.1
decision chain. It is a subordinate amendment to the canonical D0 source. It
does not create a second product architecture and supersedes older statements
only where this document names the conflict explicitly.

## Document Control

| Code | Repository authority | Scope |
|---|---|---|
| D0 | `docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md` | Business objects, state machine, data, decision chain and safety facts |
| D1 | Final interaction specification represented by D0 interaction annexes and registered interaction contracts | Interaction-only clarification of D0 |
| D2 | Registered UI/Figma execution contracts | Layout, navigation, field presentation and Figma execution only |
| R0 | This reconciliation | Only the conflicts and executable rules stated below |

Precedence is `PRODUCT_SOURCE_OF_TRUTH -> D0 -> R0 for explicitly named
conflicts -> D1 for interaction -> D2 for presentation`. D1 and D2 cannot
change D0/R0 object ownership, state authority, score ownership, AI permission,
manual-position boundaries or safety rules.

The 14 business routes remain in product scope. The current Figma delivery is
an implementation-stage subset and is not evidence that routes were deleted.
Home remains the only DataRich terminal, keeps its module order, 6x1/3x2
opportunity layout and 70:30 Position/Final Plan split.

## Versioned Machine Contract

| Contract | Version |
|---|---|
| calculation DAG | `V41-DAG-2026-08-31` |
| normalization | `V41-NORM-WREP-1` |
| direction engine | `V41-DIRECTION-4H1H-1` |
| eight-score engine | `V41-SCORE-1` |
| data quality | `V41-DQ-1` |
| provider matrix | `V41-PROVIDER-MATRIX-1` |
| plan source gate | `V41-PLAN-SOURCE-1` |
| Home ranking | `V41-HOME-RANK-1` |
| Telegram eligibility | `V41-TELEGRAM-3C-1` |

## Acyclic Calculation DAG

The only legal order is:

`Provider/raw -> DQ -> eight scores -> 4h/1h structural score ->
ruleMarketBias -> EvidenceReliability -> OpportunityScore -> RiskScore ->
validatedMarketBias -> candidate -> deterministic plan boundaries -> GPT ->
Gemini -> Grok -> Conflict Resolver -> Rule Validation -> FinalConfidence ->
finalMarketBias -> Final/BLOCKED -> Home ranking -> Message/Telegram`.

`OpportunityScore` uses `EvidenceReliability`; it never consumes
`FinalConfidence`.

## Normalization And Direction

One production normalizer owns all score inputs: Winsorized Rolling Empirical
Percentile, lookback 200 closed samples, minimum 60, winsor bounds 2.5/97.5,
range 0..100, isolated by provider, venue, asset and timeframe. Missing samples
return `null/INSUFFICIENT_SAMPLE`; neutral 50 is forbidden.

4h and 1h own direction. The structural score is
`normalized4h * 0.57 + normalized1h * 0.43`. Thresholds are +70/+35/+15 and
-15/-35/-70; -14..14 is RANGE. WAIT is a data/conflict state, not a numeric
band. 15m owns trigger maturity; 5m owns microstructure risk. Neither may
independently reverse the 4h/1h direction.

Direction has three separate maturity fields:

- `ruleMarketBias`: internal eight-state rule result;
- `validatedMarketBias`: one of six directional states only when DQ >= 70 and
  mandatory current sources, structure and non-confused gates pass;
- `finalMarketBias`: produced only after Candidate, all three AI roles,
  Resolver and Rule Validation complete.

RANGE, WAIT and missing validated direction never start a directional plan.

## Scores And Data Quality

The eight score owners are TrendStructure, CapitalFlow, LeverageRisk,
LiquidityQuality, SentimentTemperature, EventImpact, MacroEnvironment and
EvidenceReliability. LLM output cannot create or replace them.

`OpportunityScore = TrendStructure*0.30 + CapitalFlow*0.20 +
LiquidityQuality*0.15 + SentimentAlignment*0.10 + EventAlignment*0.10 +
MacroAlignment*0.05 + EvidenceReliability*0.10 - LeverageRiskPenalty -
ConflictPenalty - StalePenalty`.

`RiskScore = LeverageRisk*0.35 + LiquidityRisk*0.25 + EventRisk*0.20 +
ConflictAndExecutionRisk*0.20`.

`FinalConfidence = DQ*0.35 + MultiTimeframeConsistency*0.30 +
EvidenceCoverage*0.20 + CrossSourceConsistency*0.15 - ConflictPenalty`,
calculated only after Rule Validation.

`DQ = Completeness*0.30 + Freshness*0.25 + ProviderHealth*0.20 +
CrossSourceConsistency*0.15 + SampleAdequacy*0.10`. DQ 85..100 permits the
full chain; 70..84 forbids CONFIRMATION; below 70 has no validated direction.
Mandatory stale data caps DQ at 69. Mandatory unavailable data fails closed.
Optional unavailable data affects only declared scores and permissions.
CoinGlass disabled is explicit and is not a global hard dependency when the
provider matrix marks its datasets optional.

## Opportunity And Plan Contract

All six validated directions enter `candidate` and start the complete plan
chain. Candidate means a trusted direction is being planned and reviewed; it
cannot expose Final parameters. Legal final state/mode combinations are:

| State | Legal result |
|---|---|
| observing | no directional Final |
| candidate | plan generation in progress, no user-visible Final parameters |
| waiting_trigger | PREPARATION only |
| triggered | CONFIRMATION, REDUCED, PREPARATION, OBSERVATION or BLOCKED |
| high_risk | REDUCED, OBSERVATION or BLOCKED |
| confused | BLOCKED only |
| invalidated | INVALIDATED result, no active plan |
| cooling | no new directional plan |

`waiting_trigger + REDUCED` and `high_risk + CONFIRMATION` are illegal.

Execution values are deterministic and closed-candle based. Entry, stop,
target and RR each require sourceId, provider, timeframe, observedAt,
structureId, calculationReason and analysisId. ATR is only an external buffer
around structural invalidation. AI may explain or downgrade within the same
direction family; it may not invent or replace numeric boundaries.

The existing FinalExecutionPlan object remains the only Final owner.
CONFIRMATION/REDUCED require complete sourced parameters. PREPARATION requires
complete trigger, invalidation and conditional parameters. OBSERVATION/BLOCKED
must keep directional execution parameters null. Only CONFIRMATION/REDUCED may
offer manual position recording; no Final creates a UserPosition.

## Home Projection

Tier 1 requires a current trusted Final with a six-state `finalMarketBias`,
eligible state and CONFIRMATION/REDUCED/PREPARATION. Tier 2 may only fill empty
slots from the current user's real Asset Pool and recent real analysis. A pool
smaller than six displays its actual size. No fixed symbol, fixture, preview or
static price may fill production Home.

Tier 1 ranking is:

`DirectionStrength*0.30 + FinalConfidence*0.25 +
OneHourOpportunityQuality*0.20 + FourHourTrendAlignment*0.10 +
ExecutionFeasibility*0.10 + Freshness*0.05 - RiskPenalty - ConflictPenalty`.

Tie order is lower risk, higher FinalConfidence, higher 1h quality, newer data,
then symbol. Replacement requires a 5-point lead unless the incumbent loses
eligibility. Tier 2 never outranks Tier 1.

Production cards retain one layout and display only asset identity, trusted
price, final/observation direction, integer system confidence/risk, 1h
opportunity and 4h trend. Internal score, mode, state, conflict, ranking reason
and IDs remain available to detail/audit but are not card labels.

## High Risk And Telegram

Home owns current risk facts, Alert owns important state changes, and Message
owns material escalation/off-site delivery facts. Message remains the sole
business-fact owner. Telegram is only a ChannelDelivery.

Telegram has three eligible categories:

1. a new plan only for STRONG_BULLISH/STRONG_BEARISH with a complete trusted
   CONFIRMATION or qualified REDUCED Final;
2. a material opportunity/plan safety change, containing no new direction
   advice;
3. a VERIFIED + FRESH material change to a real active UserPosition.

All remain behind the existing three default-off switches. Push Recheck is not
trade authorization. No category may place, close, add, reduce or reverse a
trade.

## Manual Position And Ownership

For MANUAL_INDEPENDENT without thesis, timeframe and invalidation condition,
entry-logic status is `N/A`; current AI analysis may not infer it. PnL must
report coverage for fees, funding, partial fills and additions, with unsupported
components explicitly UNKNOWN.

AssetPoolItem, Plan, Position, Message and delivery reads remain principal/user
scoped. System defaults are immutable templates materialized as user-owned
relations. Missing or mismatched user identity fails closed.

## AI Quotas And Runtime Proof

AI cache identity is `symbol + timeframe + evidenceHash`, TTL five minutes.
Each analysis permits at most three role calls and one retry per role. DQ failure
causes zero AI calls. Per-run, per-asset, hourly, daily cost/token/call and
concurrency limits are mandatory configuration; exhausted limits fail closed.
Every success, failure, timeout, cache hit, retry and fallback remains traceable.

Runtime acceptance must aggregate one same-run chain from provider observation
through persisted closed OHLCV, analysisId, scores, all direction stages,
candidateId, role traceIds, Resolver, Rule Validation, planId, Home projection
and Message eligibility, with `fixture=false`. HTTP 200 alone is not evidence.

## Safety Boundary

This reconciliation does not authorize Figma/Mobile redesign, CoinGlass calls,
real Telegram sends, production deployment, automatic position mutation,
exchange private APIs or order execution. Automatic trading capability count
remains zero.
