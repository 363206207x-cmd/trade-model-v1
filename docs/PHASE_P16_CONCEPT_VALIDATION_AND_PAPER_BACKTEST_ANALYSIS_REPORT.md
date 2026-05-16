# PHASE_P16_CONCEPT_VALIDATION_AND_PAPER_BACKTEST_ANALYSIS_REPORT

## 1. Document Purpose

This document records the P16 concept validation and paper-backtest analysis report for Trade Model V1.

P16 summarizes the dry-run and paper-backtest result patterns documented in P15 and converts them into reviewable analysis.

The goal is to confirm that the current design remains safe under local fixture outcomes:

- BoundaryCandidateService output stays review-only.
- SourceTrace completeness is required before candidate promotion.
- DerivativesRiskContext missing fields stay visible.
- ExecutionPlan readiness stays advisory and non-executable.
- RiskActionGuard keeps fail-closed behavior.
- Push / Recheck / Watchlist signals do not become trade actions.

This document is analysis only.

It does not modify source code, tests, resources, schema, dashboard, or config.

It does not connect external APIs, Coinglass, live exchange data, or order APIs.

It does not generate actual ExecutionPlan execution or automated trading logic.

## 2. Analysis Scope

P16 analyzes the following fixture groups:

- complete safe fixture
- missing boundary source fixtures
- missing derivatives source fixtures
- high liquidity stress fixture
- event blocker fixture
- wick-only fixture
- stampede fixture
- portfolio mixed-risk fixture
- legacy Push / Recheck naming fixture
- Watchlist non-trading fixture

P16 does not execute tests, run Maven, pull live market data, or change runtime behavior.

## 3. BoundaryCandidateService Result Analysis

BoundaryCandidateService result summary:

| Fixture Group | Candidate Output | Expected Meaning | Analysis Result |
|---|---|---|---|
| complete safe | `VALID_REVIEW_ONLY` | structurally valid candidate, manual review required | acceptable |
| missing entry source | `INCOMPLETE` / `WATCH_ONLY` | entry traceability missing | correct fallback |
| missing stop source | `INCOMPLETE` / `WATCH_ONLY` | stop traceability missing | correct fallback |
| missing TP source | `INCOMPLETE` / `WATCH_ONLY` | TP traceability missing | correct fallback |
| missing RR source | `INCOMPLETE` / `WATCH_ONLY` | RR cannot be calculated safely | correct fallback |
| stale runtime context | `INCOMPLETE` | runtime context not fresh | correct fallback |
| low data quality | `INCOMPLETE` / `WATCH_ONLY` | insufficient data quality | correct fallback |
| event blocker active | `WATCH_ONLY` | event risk prevents promotion | correct fallback |
| wick unconfirmed | `WATCH_ONLY` | wick is not trend reversal | correct fallback |
| stampede state | fail-closed / `WATCH_ONLY` | opportunity semantics blocked | correct fallback |

Analysis notes:

- The only scenario that may produce `VALID` is complete source plus safe guard.
- `VALID` remains review-only.
- `manualReviewRequired=true` remains required.
- `notTradeInstruction=true` remains required.
- Missing entry / stop / TP / RR sources prevent VALID promotion.
- High-risk or unknown-risk states downgrade output.

No candidate output implies order placement, automated execution, or open / close / reverse action.

## 4. BoundaryCandidateService State Matrix

| Scenario | Runtime Context | SourceTrace | Risk Context | Guard State | Final State | Audit |
|---|---|---|---|---|---|---|
| P16-BC-01 | fresh | complete | complete | safe | `VALID_REVIEW_ONLY` | `BOUNDARY_CANDIDATE_VALID_REVIEW_ONLY` |
| P16-BC-02 | fresh | entry missing | complete | safe | `INCOMPLETE` / `WATCH_ONLY` | `SOURCE_TRACE_ENTRY_MISSING` |
| P16-BC-03 | fresh | stop missing | complete | safe | `INCOMPLETE` / `WATCH_ONLY` | `SOURCE_TRACE_STOP_MISSING` |
| P16-BC-04 | fresh | TP missing | complete | safe | `INCOMPLETE` / `WATCH_ONLY` | `SOURCE_TRACE_TP_MISSING` |
| P16-BC-05 | fresh | RR missing | complete | safe | `INCOMPLETE` / `WATCH_ONLY` | `SOURCE_TRACE_RR_MISSING` |
| P16-BC-06 | stale | complete | complete | safe | `INCOMPLETE` | `RUNTIME_KLINE_CONTEXT_STALE` |
| P16-BC-07 | fresh | complete | liquidity missing | fail-closed | `WATCH_ONLY` / fail-closed | `SOURCE_TRACE_LIQUIDITY_MISSING` |
| P16-BC-08 | fresh | complete | event active | fail-closed | `WATCH_ONLY` | `RISK_GUARD_EVENT_WINDOW` |
| P16-BC-09 | fresh | complete | wick unconfirmed | fallback | `WATCH_ONLY` | `RISK_GUARD_WICK_UNCONFIRMED` |
| P16-BC-10 | fresh | complete | stampede | fail-closed | fail-closed / `WATCH_ONLY` | `RISK_GUARD_STAMPEDE_BLOCK` |

## 5. SourceTrace Traceability Analysis

SourceTrace analysis summary:

| Source Area | Complete Behavior | Missing Behavior | Analysis |
|---|---|---|---|
| entry source | supports candidate structure | fallback | required before VALID |
| stop source | supports invalidation structure | fallback | required before VALID |
| TP source | supports target ladder traceability | fallback | required before VALID |
| RR source | supports readiness review | fallback | required before readiness |
| liquidity source | supports guard context | fail-closed | required for risk confidence |
| multi-timeframe source | supports conflict review | watch-only | required for confidence |
| event source | blocks unsafe windows | watch-only / fail-closed | required for promotion |
| wick source | prevents false reversal inference | watch-only | required for spike handling |

SourceTrace findings:

- Boundary source completeness is a hard gate for VALID candidate output.
- Liquidity source absence should fail closed.
- Multi-timeframe gaps should not be inferred.
- Event blockers downgrade or block candidate promotion.
- Wick-only evidence must not become trend reversal evidence.

Required audit tags:

- `SOURCE_TRACE_ENTRY_MISSING`
- `SOURCE_TRACE_STOP_MISSING`
- `SOURCE_TRACE_TP_MISSING`
- `SOURCE_TRACE_RR_MISSING`
- `SOURCE_TRACE_LIQUIDITY_MISSING`
- `SOURCE_TRACE_MULTI_TIMEFRAME_MISSING`
- `SOURCE_TRACE_EVENT_BLOCKER_ACTIVE`
- `SOURCE_TRACE_WICK_UNCONFIRMED`

## 6. DerivativesRiskContext Traceability Analysis

DerivativesRiskContext analysis summary:

| Risk Input | Complete Behavior | Missing / High-Risk Behavior | Required Audit |
|---|---|---|---|
| OI history | context evidence only | missing-field fallback | `DERIVATIVES_OI_MISSING` |
| Funding history | context evidence only | missing-field fallback | `DERIVATIVES_FUNDING_MISSING` |
| liquidation cluster | risk evidence only | fail-closed / watch-only | `DERIVATIVES_LIQUIDATION_MISSING` |
| leverage distribution | risk evidence only | fail-closed / watch-only | `DERIVATIVES_LEVERAGE_MISSING` |
| long-short ratio | risk evidence only | fail-closed / watch-only | `DERIVATIVES_LONG_SHORT_MISSING` |
| liquidity stress | guard input only | fail-closed if high or missing | `DERIVATIVES_LIQUIDITY_STRESS_HIGH` |

Analysis findings:

- Derivatives data is evidence, not action.
- Missing OI and Funding cannot be treated as neutral confidence.
- Missing liquidation and leverage data must keep risk review conservative.
- Long-short crowding must not directly imply long, short, stop, or reverse.
- Liquidity stress should affect guard state, not produce market action.

Funding, OI, liquidation, leverage, and long-short ratio do not directly generate trading actions.

## 7. ExecutionPlan Readiness Analysis

ExecutionPlan readiness result summary:

| Scenario | Candidate State | Source State | Guard State | Readiness Output | Analysis |
|---|---|---|---|---|---|
| complete safe | `VALID_REVIEW_ONLY` | complete | safe | advisory / review-only | correct |
| boundary source missing | fallback | incomplete | any | source incomplete | correct |
| derivatives missing | watch-only | partial | fallback | review-only fallback | correct |
| liquidity missing | watch-only | partial | fail-closed | blocked review-only | correct |
| event blocker | watch-only | event active | fail-closed | blocked review-only | correct |
| wick-only | watch-only | wick unconfirmed | fallback | not executable | correct |
| stampede | fail-closed | complete / partial | fail-closed | no opportunity semantics | correct |
| legacy Push / Recheck | unchanged label | source still applies | guard still applies | no executable plan | correct |

ExecutionPlan readiness remains:

- advisory
- review-only
- non-executable
- blocked by incomplete SourceTrace
- blocked by fail-closed RiskActionGuard

No ExecutionPlan readiness scenario produced:

- executable instruction
- order payload
- position mutation
- automated open
- automated close
- automated reverse

## 8. ExecutionPlan Audit Matrix

| Audit Tag | Meaning | Required Handling |
|---|---|---|
| `EXECUTION_PLAN_REVIEW_ONLY` | advisory review output only | do not execute |
| `EXECUTION_PLAN_SOURCE_INCOMPLETE` | required SourceTrace missing | fallback |
| `EXECUTION_PLAN_RISK_GUARD_BLOCKED` | RiskActionGuard blocks readiness | fail closed |
| `EXECUTION_PLAN_NOT_EXECUTABLE` | no executable readiness exists | stay advisory |

Analysis:

- `EXECUTION_PLAN_REVIEW_ONLY` is not permission.
- `EXECUTION_PLAN_SOURCE_INCOMPLETE` blocks readiness.
- `EXECUTION_PLAN_RISK_GUARD_BLOCKED` blocks opportunity semantics.
- `EXECUTION_PLAN_NOT_EXECUTABLE` must remain terminal for execution logic.

## 9. RiskActionGuard Analysis

RiskActionGuard output summary:

| Scenario | Input | Guard Output | Required Result | Audit |
|---|---|---|---|---|
| safe | low stress, complete source | safe | review-only allowed | `RISK_GUARD_SAFE` |
| liquidity missing | no liquidity evidence | fail-closed | `SAFE_FAIL_CLOSED_ONLY` | `RISK_GUARD_FAIL_CLOSED` |
| liquidity high | stress high | fail-closed | watch-only | `RISK_GUARD_LIQUIDITY_STRESS` |
| event active | active blocker | fail-closed | watch-only | `RISK_GUARD_EVENT_WINDOW` |
| wick-only | unconfirmed spike | fallback | no reversal inference | `RISK_GUARD_WICK_UNCONFIRMED` |
| stampede | crowding / stress high | fail-closed | block opportunity | `RISK_GUARD_STAMPEDE_BLOCK` |
| leverage missing | missing leverage evidence | fail-closed / watch-only | fallback | `DERIVATIVES_LEVERAGE_MISSING` |
| liquidation missing | missing liquidation evidence | fail-closed / watch-only | fallback | `DERIVATIVES_LIQUIDATION_MISSING` |

RiskActionGuard preserved:

- high risk does not mean direct stop
- high risk does not mean direct reverse
- wick / spike does not mean trend reversal
- liquidity worsening does not mean one-shot market close
- stampede blocks new-open, reverse, and opportunity push semantics

## 10. Push / Recheck / Watchlist Analysis

Push / Recheck / Watchlist result summary:

| Signal | Analysis Meaning | Output | Audit |
|---|---|---|---|
| `VALID_EXECUTABLE` | legacy positive label only | no action | `PUSH_RECHECK_LEGACY_VALID_EXECUTABLE_REVIEW_ONLY` |
| `RECHECK_VALID_EXECUTABLE` | legacy push status only | no action | `PUSH_RECHECK_LEGACY_VALID_EXECUTABLE_REVIEW_ONLY` |
| `valid=true` | still review-only | no action | `PUSH_RECHECK_VALID_FLAG_NOT_EXECUTION` |
| `PASS` | review label only | no action | `PUSH_RECHECK_REVIEW_ONLY` |
| `successCount` | recheck metric only | no trade success | `PUSH_RECHECK_REVIEW_ONLY` |
| `executionStatus` | job status only | not trade execution | `PUSH_RECHECK_REVIEW_ONLY` |
| Watchlist positive state | watchlist signal only | no open / close / reverse | `WATCHLIST_NO_TRADE_ACTION` |

Analysis findings:

- Positive Push / Recheck naming did not bypass SourceTrace.
- Positive Push / Recheck naming did not bypass RiskActionGuard.
- Watchlist did not emit trading actions.
- `valid=true` remains a semantic risk and must be kept review-only.
- `executionStatus` must remain job status only.

## 11. Combined Output Matrix

| Scenario Group | Candidate | SourceTrace | Risk Context | ExecutionPlan | RiskActionGuard | Push / Watchlist |
|---|---|---|---|---|---|---|
| complete safe | `VALID_REVIEW_ONLY` | complete | complete | advisory | safe | no action |
| entry missing | fallback | entry missing | any | source incomplete | fallback | no action |
| stop missing | fallback | stop missing | any | source incomplete | fallback | no action |
| TP missing | fallback | TP missing | any | source incomplete | fallback | no action |
| RR missing | fallback | RR missing | any | source incomplete | fallback | no action |
| liquidity missing | watch-only / fail-closed | liquidity missing | missing | blocked | fail-closed | no action |
| derivatives missing | watch-only / fail-closed | boundary may be complete | missing fields | fallback | fail-closed | no action |
| event active | watch-only | event active | event risk | blocked | fail-closed | no action |
| wick-only | watch-only | wick unconfirmed | unstable | fallback | no reversal inference | no action |
| stampede | fail-closed | complete / partial | stress high | no opportunity semantics | fail-closed | no push opportunity |
| legacy Push / Recheck | unchanged label | source still applies | risk still applies | no executable plan | guard still applies | no action |

## 12. Audit Log Analysis

Audit log categories:

| Category | Audit Tags |
|---|---|
| Runtime | `RUNTIME_KLINE_CONTEXT_STALE`, `RUNTIME_DATA_QUALITY_LOW`, `RUNTIME_LATEST_PRICE_INVALID` |
| SourceTrace boundary | `SOURCE_TRACE_ENTRY_MISSING`, `SOURCE_TRACE_STOP_MISSING`, `SOURCE_TRACE_TP_MISSING`, `SOURCE_TRACE_RR_MISSING` |
| SourceTrace risk | `SOURCE_TRACE_LIQUIDITY_MISSING`, `SOURCE_TRACE_EVENT_BLOCKER_ACTIVE`, `SOURCE_TRACE_WICK_UNCONFIRMED` |
| Derivatives | `DERIVATIVES_OI_MISSING`, `DERIVATIVES_FUNDING_MISSING`, `DERIVATIVES_LIQUIDATION_MISSING`, `DERIVATIVES_LEVERAGE_MISSING` |
| BoundaryCandidate | `BOUNDARY_CANDIDATE_VALID_REVIEW_ONLY`, `BOUNDARY_CANDIDATE_INCOMPLETE_SOURCE`, `BOUNDARY_CANDIDATE_RISK_GUARD_BLOCKED` |
| ExecutionPlan | `EXECUTION_PLAN_REVIEW_ONLY`, `EXECUTION_PLAN_SOURCE_INCOMPLETE`, `EXECUTION_PLAN_RISK_GUARD_BLOCKED`, `EXECUTION_PLAN_NOT_EXECUTABLE` |
| RiskActionGuard | `RISK_GUARD_SAFE`, `RISK_GUARD_FAIL_CLOSED`, `RISK_GUARD_LIQUIDITY_STRESS`, `RISK_GUARD_STAMPEDE_BLOCK` |
| Push / Watchlist | `PUSH_RECHECK_REVIEW_ONLY`, `PUSH_RECHECK_VALID_FLAG_NOT_EXECUTION`, `WATCHLIST_NO_TRADE_ACTION` |

Audit conclusion:

- every fallback scenario has an explicit tag
- every review-only scenario has an explicit tag
- every fail-closed scenario has an explicit tag
- no audit tag implies execution permission

## 13. Optimization And Verification Recommendations

Recommended next steps:

1. Create P17 local fixture implementation checklist.
2. Keep P17 limited to local fixture tests.
3. Add tests for each required SourceTrace missing-source case.
4. Add tests for each DerivativesRiskContext missing-field case.
5. Add tests for ExecutionPlan review-only readiness.
6. Add tests for RiskActionGuard fail-closed states.
7. Add tests for Push / Recheck / Watchlist non-trading semantics.
8. Keep Coinglass and external derivative APIs out of scope.
9. Keep order APIs out of scope.
10. Keep automated trading out of scope.

P17 should not change production execution behavior.

P17 should not enable executable ExecutionPlan output.

P17 should not reinterpret `VALID_EXECUTABLE` as action permission.

## 14. Current Conclusion

P16 analysis confirms that the P15 result summary preserves the intended safety model:

- complete local data may produce review-ready candidate output
- missing SourceTrace fields trigger fallback
- missing DerivativesRiskContext fields remain explicit
- ExecutionPlan readiness remains advisory and non-executable
- RiskActionGuard protects high-risk, stampede, liquidity, event, wick, leverage, and liquidation scenarios
- Push / Recheck / Watchlist positive naming remains non-trading

No analyzed scenario indicates:

- external API usage
- Coinglass integration
- order API usage
- actual ExecutionPlan execution
- automated trading logic
- automatic open / close / reverse

P16 is complete when this document is committed as an analysis baseline.
