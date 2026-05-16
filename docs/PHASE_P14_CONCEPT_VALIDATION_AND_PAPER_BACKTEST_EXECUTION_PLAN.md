# PHASE_P14_CONCEPT_VALIDATION_AND_PAPER_BACKTEST_EXECUTION_PLAN

## 1. Document Purpose

This document defines the P14 concept validation and small-scale paper-backtest execution plan for Trade Model V1.

P14 follows the committed P13 dry-run test plan and turns the fixture catalog into an executable verification workflow for a later implementation phase.

The goal is to define how to run local dry-run and paper-backtest scenarios against mock or historical K-line data while preserving:

- review-only outputs
- fallback behavior
- fail-closed behavior
- non-trading semantics

This document is planning only.

It does not modify source code, tests, resources, config, schema, or dashboard.

It does not connect external APIs, Coinglass, live exchange data, or order APIs.

It does not generate actual ExecutionPlan execution or automated trading code.

## 2. P14 Scope

P14 covers the planned execution workflow for local concept validation.

Included:

- dry-run execution steps
- paper-backtest execution steps
- mock K-line input design
- historical K-line fixture design
- SourceTrace verification
- DerivativesRiskContext verification
- BoundaryCandidateService output verification
- ExecutionPlan readiness verification
- RiskActionGuard fallback verification
- Push / Recheck / Watchlist review-only verification
- expected output matrix
- expected audit log matrix

Excluded:

- Coinglass connection
- live OI / Funding / liquidation feed connection
- live order placement
- real position mutation
- production ExecutionPlan execution
- dashboard schema changes
- config changes

## 3. Dry-Run Execution Workflow

| Step | Action | Input | Expected Output |
|---|---|---|---|
| 1 | Load local fixture set | P13 fixture IDs and local mock K-line records | fixture context loaded |
| 2 | Build RuntimeKlineContext-like object | symbol, timeframe, OHLCV, latestPrice, dataQualityScore | fresh / stale / incomplete context classification |
| 3 | Build SourceTrace-like object | entry / stop / TP / RR / liquidity / multi-timeframe / event / wick source | source completeness result |
| 4 | Build DerivativesRiskContext-like object | OI, Funding, liquidation, leverage, long-short, liquidity stress | missing risk fields and quality state |
| 5 | Evaluate BoundaryCandidateService behavior | runtime context + source trace + risk context + guard state | `VALID`, `INCOMPLETE`, or `WATCH_ONLY` review result |
| 6 | Evaluate ExecutionPlan readiness | candidate + source completeness + guard state | review-only / fallback state |
| 7 | Evaluate RiskActionGuard | liquidity, leverage, event, wick, stampede state | safe / fail-closed / watch-only state |
| 8 | Evaluate Push / Recheck / Watchlist semantics | positive naming and fallback state | no order, no executable plan, no auto action |
| 9 | Record audit output | all expected audit tags | deterministic audit log |

Dry-run fixture cases:

| Fixture ID | Local Input | Focus | Required Result | Audit Example |
|---|---|---|---|---|
| P14-DRY-01 | BTCUSDT 15m complete K-line + complete sources | happy-path review candidate | `VALID` review-only | `BOUNDARY_CANDIDATE_VALID_REVIEW_ONLY` |
| P14-DRY-02 | BTCUSDT 15m entry source missing | entry source fallback | `INCOMPLETE` / `WATCH_ONLY` | `SOURCE_TRACE_ENTRY_MISSING` |
| P14-DRY-03 | BTCUSDT 15m stop source missing | stop source fallback | `INCOMPLETE` / `WATCH_ONLY` | `SOURCE_TRACE_STOP_MISSING` |
| P14-DRY-04 | BTCUSDT 15m TP source missing | TP source fallback | `INCOMPLETE` / `WATCH_ONLY` | `SOURCE_TRACE_TP_MISSING` |
| P14-DRY-05 | BTCUSDT 15m RR missing | RR fallback | `INCOMPLETE` / `WATCH_ONLY` | `SOURCE_TRACE_RR_MISSING` |
| P14-DRY-06 | BTCUSDT 15m liquidity missing | liquidity fail-closed | `SAFE_FAIL_CLOSED_ONLY` | `SOURCE_TRACE_LIQUIDITY_MISSING` |
| P14-DRY-07 | BTCUSDT 15m event blocker active | event risk fallback | `WATCH_ONLY` / fail-closed | `RISK_GUARD_EVENT_WINDOW` |
| P14-DRY-08 | BTCUSDT 15m wick unconfirmed | wick confirmation fallback | `WATCH_ONLY` | `RISK_GUARD_WICK_UNCONFIRMED` |
| P14-DRY-09 | BTCUSDT 15m stampede state | opportunity block | no push opportunity | `RISK_GUARD_STAMPEDE_BLOCK` |

## 4. Paper-Backtest Execution Workflow

Paper-backtest execution must remain local and non-trading.

| Step | Action | Input | Expected Output |
|---|---|---|---|
| 1 | Select historical K-line slice | local CSV / JSON / test fixture only | deterministic price window |
| 2 | Replay K-line window | fixed symbol and timeframe | RuntimeKlineContext-like snapshots |
| 3 | Attach mock derivatives risk context | synthetic OI / Funding / liquidation / leverage / long-short values | complete or missing risk state |
| 4 | Attach SourceTrace fixture | traceable entry / stop / TP / RR source or missing markers | source completeness state |
| 5 | Run candidate evaluation | local service-level dry-run only | review candidate or fallback |
| 6 | Run readiness review | no real execution allowed | advisory / review-only state |
| 7 | Run risk guard review | high-risk / safe / missing-source variants | fail-closed or review-only state |
| 8 | Produce paper result | status, fallback, warnings, audit logs | paper-only summary |

Paper-backtest output must not include:

- executable order
- live order request
- trade confirmation
- position mutation
- automated open / close / reverse action

Paper-backtest fixture cases:

| Fixture ID | Historical Slice | Mock Source Setup | Expected Paper Result | Audit Example |
|---|---|---|---|---|
| P14-PBT-01 | BTCUSDT 15m stable trend window | complete entry / stop / TP / RR + safe risk | review-ready candidate only | `EXECUTION_PLAN_REVIEW_ONLY` |
| P14-PBT-02 | BTCUSDT 15m high volatility window | complete boundary source + event active | watch-only / fail-closed | `SOURCE_TRACE_EVENT_BLOCKER_ACTIVE` |
| P14-PBT-03 | BTCUSDT 15m spike window | wick-only signal without confirmation | watch-only | `SOURCE_TRACE_WICK_UNCONFIRMED` |
| P14-PBT-04 | ETHUSDT 1h missing higher timeframe | multi-timeframe source incomplete | watch-only / incomplete | `SOURCE_TRACE_MULTI_TIMEFRAME_MISSING` |
| P14-PBT-05 | BTCUSDT + ETHUSDT portfolio slice | one asset liquidity missing | portfolio review-only / partial fail-closed | `SOURCE_TRACE_LIQUIDITY_MISSING` |
| P14-PBT-06 | BTCUSDT 15m missing derivatives inputs | OI / Funding / liquidation absent | missing-field fallback | `DERIVATIVES_OI_MISSING` |

## 5. Mock And Historical K-Line Inputs

| Input Group | Required Fields | Missing Data Behavior |
|---|---|---|
| K-line base | symbol, timeframe, open, high, low, close, volume, timestamp | missing OHLCV -> `INCOMPLETE` |
| Runtime state | latestPrice, staleStatus, dataQualityScore, kline window size | stale or insufficient window -> `INCOMPLETE` / `WATCH_ONLY` |
| Entry source | sourceType, sourceValue, sourceTimeframe, sourceReason, sourceField, sourceRef | missing -> `INCOMPLETE` / `WATCH_ONLY` |
| Stop source | sourceType, sourceValue, sourceTimeframe, sourceReason, sourceField, sourceRef | missing -> `INCOMPLETE` / `WATCH_ONLY` |
| TP source | level, price, RR, numericSource, sourceTimeframe, sourceRef, allocation | missing -> `INCOMPLETE` / `WATCH_ONLY` |
| RR source | entry / stop / TP source-derived RR | missing -> `INCOMPLETE` / `WATCH_ONLY` |
| Liquidity source | liquidity stress, liquidity source ref | missing -> `SAFE_FAIL_CLOSED_ONLY` |
| Multi-timeframe source | lower / higher timeframe context and source ref | missing -> `WATCH_ONLY` |
| Event window source | active event list and blocker source | missing / active blocker -> `WATCH_ONLY` / fail-closed |
| Wick source | wick confirmation and spike source ref | missing -> `WATCH_ONLY`; no trend reversal inference |
| Derivatives risk | OI, Funding, liquidation, leverage, long-short | missing -> explicit missing field and fallback |

Detailed local input examples should map stale K-line, insufficient window, missing volume, invalid latest price, missing Funding, missing leverage, and missing long-short ratio into explicit `RUNTIME_*` or `DERIVATIVES_*` audit tags with `INCOMPLETE`, `WATCH_ONLY`, or fail-closed fallback.

## 6. BoundaryCandidateService Verification

P14 should verify the following candidate outcomes:

| Scenario | Required Inputs | Expected Candidate Output | Expected Audit |
|---|---|---|---|
| complete safe source | complete runtime, complete SourceTrace, safe guard | `VALID` review candidate | `BOUNDARY_CANDIDATE_VALID_REVIEW_ONLY` |
| missing entry source | complete runtime, entry missing | `INCOMPLETE` / `WATCH_ONLY` | `SOURCE_TRACE_ENTRY_MISSING` |
| missing stop source | complete runtime, stop missing | `INCOMPLETE` / `WATCH_ONLY` | `SOURCE_TRACE_STOP_MISSING` |
| missing TP source | complete runtime, TP missing | `INCOMPLETE` / `WATCH_ONLY` | `SOURCE_TRACE_TP_MISSING` |
| missing RR source | complete runtime, RR missing | `INCOMPLETE` / `WATCH_ONLY` | `SOURCE_TRACE_RR_MISSING` |
| stale runtime | staleStatus stale | `INCOMPLETE` | `RUNTIME_KLINE_CONTEXT_STALE` |
| low data quality | dataQualityScore below threshold | `INCOMPLETE` / `WATCH_ONLY` | `RUNTIME_DATA_QUALITY_LOW` |
| risk guard blocked | complete source plus fail-closed guard | `WATCH_ONLY` / fail-closed | `BOUNDARY_CANDIDATE_RISK_GUARD_BLOCKED` |

Every `VALID` output remains:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- non-executable
- review-only

Additional paper-backtest candidate checks must assert that complete-source candidates keep `manualReviewRequired=true` and `notTradeInstruction=true`, while missing-source, high-risk, or event-window candidates are downgraded and never produce order instructions or executable readiness.

## 7. SourceTrace / DerivativesRiskContext Verification

P14 should verify traceability before candidate promotion.

| Verification Area | Complete Case | Missing Case | Required Fallback |
|---|---|---|---|
| SourceTrace boundary sources | entry / stop / TP / RR complete | any required boundary source missing | `INCOMPLETE` / `WATCH_ONLY` |
| SourceTrace liquidity | liquidity source present | liquidity missing | `SAFE_FAIL_CLOSED_ONLY` |
| SourceTrace multi-timeframe | source present and aligned | source missing or conflicting | `WATCH_ONLY` |
| SourceTrace event blocker | no active blocker | missing or active blocker | `WATCH_ONLY` / fail-closed |
| SourceTrace wick confirmation | wick confirmed or irrelevant | wick-only unconfirmed | `WATCH_ONLY` |
| Derivatives OI | OI fixture present | missing OI | missing field audit + fallback |
| Derivatives Funding | Funding fixture present | missing Funding | missing field audit + fallback |
| Derivatives liquidation | liquidation fixture present | missing liquidation | missing field audit + fallback |
| Derivatives leverage | leverage fixture present | missing leverage | missing field audit + fallback |
| Derivatives long-short | long-short fixture present | missing long-short | missing field audit + fallback |

Audit examples:

| Missing Input | Expected Audit | Expected Consumer Behavior |
|---|---|---|
| entry source | `SOURCE_TRACE_ENTRY_MISSING` | BoundaryCandidate fallback |
| stop source | `SOURCE_TRACE_STOP_MISSING` | BoundaryCandidate fallback |
| TP source | `SOURCE_TRACE_TP_MISSING` | BoundaryCandidate fallback |
| RR source | `SOURCE_TRACE_RR_MISSING` | ExecutionPlan readiness fallback |
| liquidity source | `SOURCE_TRACE_LIQUIDITY_MISSING` | RiskActionGuard fail-closed |
| multi-timeframe source | `SOURCE_TRACE_MULTI_TIMEFRAME_MISSING` | watch-only |
| event blocker source | `SOURCE_TRACE_EVENT_BLOCKER_ACTIVE` | watch-only / fail-closed |
| wick confirmation | `SOURCE_TRACE_WICK_UNCONFIRMED` | watch-only |

## 8. ExecutionPlan Readiness Verification

ExecutionPlan readiness must remain advisory and non-executable.

| Scenario | Candidate | SourceTrace | RiskActionGuard | Expected Readiness |
|---|---|---|---|---|
| complete source + safe guard | `VALID` review candidate | complete | safe | advisory / review-only |
| missing source | any | incomplete | any | `INCOMPLETE` / review-only |
| fail-closed guard | any | complete or incomplete | fail-closed | review-only blocked |
| display-only state | any | any | any | not executable |
| DTO-only state | any | any | any | not executable |

ExecutionPlan readiness must not produce:

- executable instruction
- order payload
- position mutation
- auto-open action
- auto-close action
- auto-reverse action

Readiness audit examples must include `EXECUTION_PLAN_REVIEW_ONLY`, `EXECUTION_PLAN_SOURCE_INCOMPLETE`, `EXECUTION_PLAN_RISK_GUARD_BLOCKED`, and `EXECUTION_PLAN_NOT_EXECUTABLE`.

## 9. RiskActionGuard Verification

RiskActionGuard scenarios:

| Scenario | Mock Input | Expected Guard Behavior | Expected Audit |
|---|---|---|---|
| safe source | all risk sources present and low stress | safe review-only | `RISK_GUARD_SAFE` |
| liquidity stress | liquidity stress high | fail-closed / review-only | `RISK_GUARD_LIQUIDITY_STRESS` |
| leverage missing | leverage source missing | fail-closed / watch-only | `DERIVATIVES_LEVERAGE_MISSING` |
| liquidation missing | liquidation source missing | fail-closed / watch-only | `DERIVATIVES_LIQUIDATION_MISSING` |
| event active | event blocker active | fail-closed / watch-only | `RISK_GUARD_EVENT_WINDOW` |
| wick-only | wick unconfirmed | no trend reversal inference | `RISK_GUARD_WICK_UNCONFIRMED` |
| stampede | stampede state true | block new-open / reverse / opportunity push | `RISK_GUARD_STAMPEDE_BLOCK` |

RiskActionGuard must preserve:

- high risk does not mean direct stop
- high risk does not mean direct reverse
- wick / spike does not mean trend reversal
- liquidity worsening does not mean one-shot market close

RiskActionGuard audit examples must include `RISK_GUARD_SAFE`, `RISK_GUARD_LIQUIDITY_STRESS`, `RISK_GUARD_EVENT_WINDOW`, `RISK_GUARD_WICK_UNCONFIRMED`, and `RISK_GUARD_STAMPEDE_BLOCK`.

## 10. Push / Recheck / Watchlist Verification

Push / Recheck / Watchlist must remain non-trading.

| Signal | Required Interpretation | Expected Audit |
|---|---|---|
| `VALID_EXECUTABLE` | legacy positive recheck naming; review-only | `PUSH_RECHECK_LEGACY_VALID_EXECUTABLE_REVIEW_ONLY` |
| `RECHECK_VALID_EXECUTABLE` | legacy push status; review-only | `PUSH_RECHECK_LEGACY_VALID_EXECUTABLE_REVIEW_ONLY` |
| `valid=true` | candidate still valid for review only | `PUSH_RECHECK_VALID_FLAG_NOT_EXECUTION` |
| `PASS` | review label only | `PUSH_RECHECK_REVIEW_ONLY` |
| `successCount` | recheck success only, not trade success | `PUSH_RECHECK_REVIEW_ONLY` |
| `executionStatus` | recheck job status only, not trade execution | `PUSH_RECHECK_REVIEW_ONLY` |
| Watchlist positive recheck | no open / close / reverse action | `WATCHLIST_NO_TRADE_ACTION` |

Legacy naming dry-run checks:

| Legacy Signal | Source State | Guard State | Required Result |
|---|---|---|---|
| `VALID_EXECUTABLE` | complete | safe | review-only label |
| `VALID_EXECUTABLE` | missing source | any | fallback; no execution permission |
| `valid=true` | complete | safe | candidate still valid for review only |
| `PASS` | complete | fail-closed | blocked by guard |
| `successCount` | any | any | recheck metric only |

## 11. Expected Output Matrix

| Scenario Group | Candidate Output | ExecutionPlan Output | RiskActionGuard Output | Push / Recheck / Watchlist Output | Required Warnings |
|---|---|---|---|---|---|
| complete safe | `VALID` review candidate | advisory review-only | safe | no action | `BOUNDARY_CANDIDATE_VALID_REVIEW_ONLY` |
| missing boundary source | `INCOMPLETE` / `WATCH_ONLY` | review-only fallback | fallback | no action | `SOURCE_TRACE_*_MISSING` |
| missing derivatives source | fallback / watch-only | review-only fallback | fail-closed / watch-only | no action | `DERIVATIVES_*_MISSING` |
| high liquidity stress | `WATCH_ONLY` / fail-closed | blocked review-only | fail-closed | no action | `RISK_GUARD_LIQUIDITY_STRESS` |
| event blocker | `WATCH_ONLY` | blocked review-only | fail-closed | no action | `RISK_GUARD_EVENT_WINDOW` |
| wick-only | `WATCH_ONLY` | review-only fallback | no trend reversal | no action | `RISK_GUARD_WICK_UNCONFIRMED` |
| stampede | fail-closed / watch-only | no opportunity semantics | fail-closed | no opportunity push | `RISK_GUARD_STAMPEDE_BLOCK` |
| legacy Push / Recheck naming | unchanged review label | no executable plan | guard still applies | no action | `PUSH_RECHECK_*` |

## 12. Acceptance Criteria

P14 planning is complete when:

- dry-run execution steps are defined
- paper-backtest execution steps are defined
- mock / historical K-line inputs are defined
- SourceTrace verification is defined
- DerivativesRiskContext verification is defined
- BoundaryCandidateService verification is defined
- ExecutionPlan readiness verification is defined
- RiskActionGuard verification is defined
- Push / Recheck / Watchlist verification is defined
- output matrix is defined
- audit log expectations are defined
- no external API is required
- no actual ExecutionPlan execution is generated
- no order or automated trading logic is generated

## 13. Current Conclusion

P14 defines how to execute concept validation and paper-backtest checks while preserving the safety boundaries established by P0-P13.

The expected safe outcome is:

- complete source may produce review-ready outputs
- missing source produces fallback
- high-risk or unknown-risk state fails closed or stays watch-only
- ExecutionPlan readiness remains advisory
- Push / Recheck / Watchlist remains non-trading
- no external API, order API, actual execution plan, or automated trading is produced

This P14 document should remain untracked until explicitly reviewed and staged.
