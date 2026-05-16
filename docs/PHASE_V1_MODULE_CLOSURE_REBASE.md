# PHASE_V1_MODULE_CLOSURE_REBASE

## 1. Review Object

This document records the Trade Model V1 module closure rebase baseline.

Review baseline HEAD:

- `860c754 feat(dto): add BoundaryCandidateDTO + related DTOs and unit test with BoundaryStatusEnum`

This is a documentation-only baseline.

This stage does not:

- modify Java code
- modify tests
- modify resources
- modify schema
- modify dashboard
- connect Coinglass
- connect any external derivatives API
- generate execution plans
- generate order or automatic trading logic

Core review question:

When Coinglass, OI history, Funding history, liquidation, and leverage risk data are missing, which closed modules remain valid, and which modules must be re-labeled as basic closure only.

## 2. Status Labels

| Label | Meaning |
|---|---|
| `CLOSED` | The module is closed for its stated scope and does not depend on missing derivatives risk data. |
| `CLOSED_BASIC` | The base structure or DTO-level capability is closed, but production chain completeness is not implied. |
| `CLOSED_BASIC_DERIVATIVES_PENDING` | The basic module is closed, but derivatives risk enrichment is still pending. |
| `SAFE_FAIL_CLOSED_ONLY` | The module safely blocks or displays fallback state, but is not connected to real production sources. |
| `INCOMPLETE_RISK_LOGIC` | The module involves risk, VALID output, execution readiness, or trading semantics, but required evidence or production chain is incomplete. |

## 3. Module Closure Rebase Table

| Priority | Module | Current Closure State | Rebased Label | Reason |
|---|---|---|---|---|
| P0 | BoundaryCandidateService VALID integration | No confirmed production integration in current HEAD | `INCOMPLETE_RISK_LOGIC` | DTO factory exists, but Service does not yet assemble VALID from RuntimeKlineContext and traceable sources. |
| P0 | RuntimeKlineContext production chain | No complete production chain found | `INCOMPLETE_RISK_LOGIC` | VALID requires fresh OHLCV, kline window, latest price, quality, and source trace. |
| P0 | PlanBoundary SourceTrace | Fail-closed display adapter exists | `SAFE_FAIL_CLOSED_ONLY` | Source trace is safely marked missing / incomplete, but no real source assembler is connected. |
| P0 | ExecutionPlan / PlanService | Advisory / placeholder plan | `INCOMPLETE_RISK_LOGIC` | Current plan does not contain real executable entry / stop / TP production values. |
| P0 | RiskActionGuard display | Fail-closed display exists | `SAFE_FAIL_CLOSED_ONLY` | It blocks actions by default, but has no real liquidity, stampede, wick, liquidation, or leverage risk source. |
| P0 | MarketEnvironment derivatives chain | Binance minimal chain exists | `CLOSED_BASIC_DERIVATIVES_PENDING` | Current chain has 24h ticker, last funding, and current OI only. |
| P0 | ScoreService / eight scores | Lightweight scoring exists | `CLOSED_BASIC_DERIVATIVES_PENDING` | Funding, leverage, liquidity, sentiment, macro, and event scores remain heuristic. |
| P0 | EvidenceService | Basic evidence exists | `CLOSED_BASIC_DERIVATIVES_PENDING` | Funding / OI / leverage evidence is explanatory and heuristic, not complete derivatives risk evidence. |
| P0 | Push / Recheck / Watchlist chain | Basic status chain exists | `CLOSED_BASIC_DERIVATIVES_PENDING` | Recheck does not re-evaluate liquidation clusters, OI history, funding history, leverage pressure, or liquidity stress. |
| P1 | BoundaryCandidateDTO | DTO and static valid factory exist | `CLOSED_BASIC` | Can express a VALID candidate, but does not prove Service or risk source completeness. |
| P1 | BoundaryEntryDTO / BoundaryStopDTO / BoundaryTakeProfitLevelDTO | Basic fields exist | `CLOSED_BASIC` | Entry / stop / TP can carry numeric source fields, but production source chain is not complete. |
| P1 | BoundarySourceFieldsDTO | Source summary fields exist | `CLOSED_BASIC` | Field carrier exists; source assembler is still missing. |
| P1 | Dashboard PlanBoundary display | Safe display exists | `SAFE_FAIL_CLOSED_ONLY` | Display is safe and fail-closed, but not proof of real PlanBoundary production readiness. |
| P1 | ExecutionPlan display | Review-only mapping exists | `SAFE_FAIL_CLOSED_ONLY` | Even VALID boundary maps to manual review only, not executable. |
| P1 | DecisionEngine | Basic decision chain exists | `CLOSED_BASIC_DERIVATIVES_PENDING` | Derivatives risk is not a complete first-class decision context. |
| P1 | Position monitor | Position and liquidation price fields exist | `CLOSED_BASIC_DERIVATIVES_PENDING` | Account liquidation price is not market-wide liquidation risk. |
| P1 | Review / replay aggregate | Read model aggregation exists | `CLOSED_BASIC_DERIVATIVES_PENDING` | Review can aggregate available evidence, but cannot recover missing derivatives risk evidence. |
| P1 | schema / config | Basic fields exist | `CLOSED_BASIC_DERIVATIVES_PENDING` | No Coinglass, OI history, Funding history, liquidation cluster, or leverage distribution contract exists. |
| P2 | Dashboard static UI / safe defaults | Safe display defaults exist | `CLOSED` | Static safe display and non-trading labels do not depend on derivatives risk sources. |
| P2 | DTO getter / setter / unit tests | Structural tests exist | `CLOSED` | Pure DTO structure and safety defaults do not require external risk data. |

## 4. Directly Affected Modules

### 4.1 MarketEnvironment

Current valid capability:

- Binance 24h ticker
- last funding rate
- current open interest
- minimal open interest delta
- minimal derivatives crowding state

Missing risk evidence:

- Coinglass data
- OI history
- Funding history
- liquidation heatmap
- liquidation clusters
- leverage distribution
- long / short ratio
- liquidity stress source

Rebase result:

- `CLOSED_BASIC_DERIVATIVES_PENDING`

### 4.2 ScoreService / Eight Scores

Directly affected scores:

- Funding momentum score
- leverage risk score
- liquidity quality score
- sentiment temperature score
- macro environment score
- event impact score

Current limitations:

- Funding uses current last funding only
- OI uses current value or minimal delta only
- leverage risk is heuristic
- liquidity quality uses 24h range and volatility regime
- no liquidation cluster evidence
- no leverage distribution evidence
- no order book depth or slippage model

Rebase result:

- `CLOSED_BASIC_DERIVATIVES_PENDING`

### 4.3 EvidenceService

Current valid capability:

- basic price structure evidence
- funding evidence
- OI evidence
- leverage evidence
- macro evidence
- event evidence

Required boundary:

Funding, OI, and liquidation evidence must not be directly interpreted as:

- opening long
- opening short
- stop loss
- close position
- reverse trade

Rebase result:

- `CLOSED_BASIC_DERIVATIVES_PENDING`

### 4.4 BoundaryCandidateService / RuntimeKlineContext

Current valid capability:

- BoundaryCandidateDTO has a static `valid(...)` factory
- Entry / Stop / TP DTOs can carry numeric source fields

Missing production dependencies:

- RuntimeKlineContext production chain
- entry source assembler
- stop source assembler
- TP source assembler
- RR source / rule assembler
- liquidity source
- multi-timeframe source
- event window blocker
- wick confirmation source

Rebase result:

- `INCOMPLETE_RISK_LOGIC`

### 4.5 ExecutionPlan / Push / Recheck / Watchlist

Current valid capability:

- advisory ExecutionPlan
- push snapshot
- recheck status chain
- watchlist / observation display semantics

Missing risk dependencies:

- liquidation risk confirmation
- funding crowding history
- OI trap detection
- leverage pressure
- liquidity stress
- stampede source

Rebase result:

- ExecutionPlan: `INCOMPLETE_RISK_LOGIC`
- Push / Recheck / Watchlist: `CLOSED_BASIC_DERIVATIVES_PENDING`

## 5. Indirectly Affected Modules

### 5.1 DecisionEngine

DecisionEngine remains usable for the basic V1 chain.

However, derivatives risk is not yet a complete first-class decision context.

It must not claim complete coverage of:

- liquidation risk
- leverage crowding
- extreme funding divergence
- OI trap accumulation
- liquidity stampede

Rebase result:

- `CLOSED_BASIC_DERIVATIVES_PENDING`

### 5.2 Dashboard

Dashboard display is safe, but many states remain fail-closed or backend pending.

Affected display slots:

- PlanBoundary display
- SourceTrace display
- ExecutionPlan display
- RiskActionGuard display
- MarketEnvironmentMini
- Watchlist / Push observation slots

Rebase result:

- `SAFE_FAIL_CLOSED_ONLY` for PlanBoundary / ExecutionPlan / RiskActionGuard display chain

### 5.3 Review / Replay

Review can aggregate stored run, decision, plan, evidence, score, push, alert, and replay data.

But if derivatives risk evidence was missing at run time, Review cannot reconstruct it later.

Rebase result:

- `CLOSED_BASIC_DERIVATIVES_PENDING`

## 6. Basically Unaffected Modules

The following modules remain valid for their current limited scope:

- DTO getter / setter methods
- BoundaryCandidateDTO structural validation
- BoundaryCandidateDTO unit test coverage
- VO safe defaults
- mapper base read / write behavior
- system health
- static dashboard labels
- non-trading display defaults

These modules may remain `CLOSED` or `CLOSED_BASIC`, depending on whether they are pure structure or basic DTO capability.

## 7. BoundaryCandidate VALID vs ExecutionPlan

BoundaryCandidate `VALID` means only that a boundary candidate DTO satisfies minimum structural requirements.

It can mean:

- symbol exists
- timeframe exists
- entry exists
- stop exists
- take profit levels exist
- sourceFields exists
- dataQualityScore exists
- boundaryStatus = VALID
- manualReviewRequired = true
- notTradeInstruction = true

It does not mean:

- open long
- open short
- place order
- stop loss instruction
- close position
- reverse trade
- order API connected
- automatic trading connected
- ExecutionPlan complete
- PlanBoundary production chain complete
- RuleEngine complete

ExecutionPlan is a separate layer.

ExecutionPlan requires, at minimum:

- complete PlanBoundary
- complete entry / stop / TP source trace
- complete risk context
- liquidity state
- stampede state
- wick-only risk state
- account risk state
- manual review

In current V1, BoundaryCandidate `VALID` can only support review-only or downstream readiness evaluation. It must not be treated as an executable plan.

## 8. Risk Data Missing Limitations

Missing Coinglass / OI history / Funding history / liquidation / leverage risk data prevents reliable judgment of:

- liquidation clusters
- liquidation heatmap zones
- cross-exchange OI accumulation
- OI trend vs trap accumulation
- extreme funding persistence
- long / short ratio crowding
- leverage distribution
- liquidity stress
- market stampede
- wick-only events
- real trend reversal
- safe position opening
- safe reverse trading
- safe market exit

Therefore:

- Funding does not imply long or short
- OI does not imply long or short
- Liquidation does not imply stop loss or reverse trade
- high risk does not imply direct stop loss
- high risk does not imply direct reverse trade
- wick does not imply trend reversal
- stampede state forbids new positions, reverse trades, and opportunity pushes

## 9. Fallback Rules

When derivatives risk sources are missing:

| Module | Required fallback |
|---|---|
| BoundaryCandidateService | `INCOMPLETE` or `WATCH_ONLY` |
| PlanBoundary SourceTrace | `SAFE_FAIL_CLOSED` / source trace missing |
| ExecutionPlan display | `BOUNDARY_PENDING`, `INCOMPLETE`, or review-only |
| RiskActionGuard | manual review required, all trading action flags false |
| Push / Watchlist | no opportunity push when risk source is required but missing |
| Score / Evidence | label as heuristic / derivatives pending |
| Review | record evidence missing, do not infer missing risk state |

## 10. Risk Action Guard Principles

All future stages must preserve:

- risk high does not mean direct stop loss
- risk high does not mean direct reverse trade
- wick does not mean trend reversal
- stampede state forbids new positions
- stampede state forbids reverse trades
- stampede state forbids opportunity pushes
- liquidity worsening should not trigger one-shot market exit
- VALID candidate is not a trading action
- review-only ExecutionPlan is not automatic execution

## 11. P0 Freeze Boundary

This document freezes the current V1 module status baseline.

Before the derivatives risk source contract is defined, future work must not modify core production behavior in:

- BoundaryCandidateService
- RuntimeKlineContext production chain
- ExecutionPlan
- RiskActionGuard
- RuleEngine
- PlanReadiness
- dashboard business semantics
- order API
- automatic trading workflow

The purpose is to prevent accidental promotion from basic closure to production execution semantics.

## 12. Recommended Next Steps

### P0

1. Commit this module closure rebase baseline.
2. Update PlanBoundary / SourceTrace status documents:
   - BoundaryCandidateDTO exists
   - RuntimeKlineContext remains incomplete
   - BoundaryCandidateService VALID integration remains incomplete
   - source assembler remains incomplete
3. Mark missing-source modules as fallback:
   - `INCOMPLETE`
   - `WATCH_ONLY`
   - `SAFE_FAIL_CLOSED`
4. Keep ExecutionPlan, BoundaryCandidateService, and RiskActionGuard core behavior frozen.

### P1

1. Define DerivativesRiskContext / SourceTrace data contract.
2. Do not connect external APIs yet.
3. Define fields for:
   - OI history
   - Funding history
   - liquidation cluster
   - leverage distribution
   - long / short ratio
   - liquidity stress
   - event window blocker
   - wick confirmation source
4. Add explicit derivatives-risk-missing semantics for Score / Evidence / Decision / Dashboard.

### P2

1. After source contract closure, design source assembler.
2. Then design BoundaryCandidateService VALID integration.
3. Then design PlanReadiness.
4. Then design ExecutionPlan readiness.
5. Then design Push / Watchlist risk gate enhancement.
6. Continue to defer order API and automatic trading.

## 13. Review Conclusion

Current V1 valid closures:

- DTO base structure
- BoundaryCandidateDTO valid factory
- safe display defaults
- fail-closed PlanBoundary / ExecutionPlan / RiskActionGuard display
- basic MarketEnvironment
- basic Score / Evidence / Decision
- basic Push / Recheck / Review chain

Current V1 must not be labeled as:

- complete derivatives risk system
- complete PlanBoundary production chain
- complete ExecutionPlan production chain
- complete RuleEngine risk chain
- automatic trading system

Final rebase conclusion:

- DTO and safe display modules remain valid for their limited scope.
- Market / Score / Evidence / Decision / Push / Review are basic closures with derivatives risk pending.
- BoundaryCandidateService / RuntimeKlineContext / ExecutionPlan / PlanReadiness / RuleEngine remain incomplete for risk production semantics.
- Order API and automatic trading remain out of scope.
