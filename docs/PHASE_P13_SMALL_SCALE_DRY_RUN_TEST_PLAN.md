# PHASE_P13_SMALL_SCALE_DRY_RUN_TEST_PLAN

## 1. Document Purpose

This document defines the P13 small-scale dry-run test plan for Trade Model V1.

P13 converts the P12 concept validation and paper-backtest plan into a concrete fixture and mock-data test matrix.

The goal is to verify review-only, fallback, and fail-closed behavior across:

- SourceTrace
- DerivativesRiskContext
- BoundaryCandidateService
- ExecutionPlan readiness
- RiskActionGuard
- Push / Recheck / Watchlist

This document is planning only.

It does not modify source code, tests, resources, config, schema, or dashboard.

It does not connect external APIs, Coinglass, live exchange data, or order APIs.

It does not generate actual ExecutionPlan execution or automated trading code.

## 2. P13 Scope

P13 defines small-scale dry-run test cases using local fixtures and mocks.

Fixture groups:

- single-asset fixture
- portfolio fixture
- multi-timeframe fixture

Mock data categories:

- price
- volume
- leverage
- Funding
- OI
- liquidation cluster
- long / short ratio
- liquidity stress
- event window
- wick confirmation

Verification targets:

- BoundaryCandidate `VALID` / fallback behavior
- SourceTrace traceability
- DerivativesRiskContext missing-field behavior
- ExecutionPlan readiness review-only / fallback behavior
- RiskActionGuard fail-closed behavior
- Push / Recheck / Watchlist non-trading constraints

## 3. Fixture & Mock Data Plan

| Fixture ID | Fixture Type | Mock Data | Verification Target | Expected Safety Result |
|---|---|---|---|---|
| P13-SA-01 | Single asset | BTCUSDT, 15m, complete price / volume / entry / stop / TP / RR source, safe liquidity, no event window, wick confirmed | BoundaryCandidateService complete-source path | `VALID` review candidate only; `manualReviewRequired=true`; `notTradeInstruction=true` |
| P13-SA-02 | Single asset | BTCUSDT, 15m, missing entry numeric source, complete price / volume | SourceTrace missing boundary source | `INCOMPLETE` / `WATCH_ONLY`; no VALID inference |
| P13-SA-03 | Single asset | BTCUSDT, 15m, missing stop numeric source, complete entry and TP | SourceTrace missing stop source | `INCOMPLETE` / `WATCH_ONLY`; no execution semantics |
| P13-SA-04 | Single asset | BTCUSDT, 15m, missing TP numeric source, complete entry and stop | SourceTrace missing TP source | `INCOMPLETE` / `WATCH_ONLY`; TP ladder not inferred |
| P13-SA-05 | Single asset | BTCUSDT, 15m, missing RR source | RR traceability gate | `INCOMPLETE` / `WATCH_ONLY`; no Plan readiness |
| P13-SA-06 | Single asset | BTCUSDT, 15m, complete boundary source but missing liquidity source | RiskActionGuard liquidity fallback | `SAFE_FAIL_CLOSED_ONLY` / review-only |
| P13-SA-07 | Single asset | BTCUSDT, 15m, event window active | Event blocker fallback | `WATCH_ONLY` / fail-closed review-only |
| P13-SA-08 | Single asset | BTCUSDT, 15m, wick-only signal without confirmation | Wick confirmation fallback | `WATCH_ONLY`; no trend reversal inference |
| P13-SA-09 | Single asset | BTCUSDT, 15m, stampede state | RiskActionGuard stampede gate | no new-open / reverse / opportunity push |
| P13-PA-01 | Portfolio | BTCUSDT + ETHUSDT, mixed source completeness, one asset missing liquidity source | Portfolio-level risk fallback | affected asset fail-closed; portfolio remains review-only |
| P13-PA-02 | Portfolio | BTCUSDT + ETHUSDT, leverage high on one asset, complete boundary sources | Leverage / exposure risk fallback | high-risk asset review-only / fail-closed |
| P13-MT-01 | Multi-timeframe | BTCUSDT, 5m / 15m / 1h aligned, complete sources | Multi-timeframe source traceability | review-ready only; no automatic execution |
| P13-MT-02 | Multi-timeframe | BTCUSDT, 5m bullish, 1h conflicting, complete boundary source | Multi-timeframe conflict | `WATCH_ONLY`; no VALID promotion |
| P13-MT-03 | Multi-timeframe | BTCUSDT, 15m complete, 4h source missing | Missing higher-timeframe source | `WATCH_ONLY` / `INCOMPLETE`; no execution readiness |
| P13-PR-01 | Push / Recheck | legacy `VALID_EXECUTABLE`, `valid=true`, `PASS` after positive recheck | Push / Recheck naming safety | review-only label; no order, no executable plan |
| P13-PR-02 | Push / Recheck | `successCount` increment and `executionStatus=COMPLETED` for recheck job | Replay / ops semantics | job success only; not trade success |
| P13-WL-01 | Watchlist | positive recheck with missing SourceTrace | Watchlist non-trading gate | no open / close / reverse action |

## 4. Verification Steps

### 4.1 Baseline Verification

Before running any future dry-run test implementation:

1. Confirm P0-P12 documents are committed.
2. Confirm staged area is empty.
3. Confirm source code is clean unless a future implementation task explicitly changes it.
4. Confirm no external API keys or live API clients are required.
5. Confirm every test uses local fixtures or mocks.

### 4.2 SourceTrace Verification

For each fixture:

1. Build SourceTrace-like input.
2. Mark entry / stop / TP / RR source completeness.
3. Mark liquidity / multi-timeframe / event / wick source completeness.
4. Verify missing required source fails completeness.
5. Verify missing source remains visible in audit output.

Expected audit log entries:

- `SOURCE_TRACE_COMPLETE`
- `SOURCE_TRACE_ENTRY_MISSING`
- `SOURCE_TRACE_STOP_MISSING`
- `SOURCE_TRACE_TP_MISSING`
- `SOURCE_TRACE_RR_MISSING`
- `SOURCE_TRACE_LIQUIDITY_MISSING`
- `SOURCE_TRACE_MULTI_TIMEFRAME_MISSING`
- `SOURCE_TRACE_EVENT_BLOCKER_ACTIVE`
- `SOURCE_TRACE_WICK_UNCONFIRMED`

### 4.3 DerivativesRiskContext Verification

For each fixture:

1. Provide mock OI history or explicit missing marker.
2. Provide mock Funding history or explicit missing marker.
3. Provide mock liquidation cluster or explicit missing marker.
4. Provide mock leverage distribution or explicit missing marker.
5. Provide mock long / short ratio or explicit missing marker.
6. Provide liquidity stress state.
7. Verify missing derivatives fields trigger fallback in downstream modules.

Expected audit log entries:

- `DERIVATIVES_RISK_CONTEXT_COMPLETE`
- `DERIVATIVES_OI_MISSING`
- `DERIVATIVES_FUNDING_MISSING`
- `DERIVATIVES_LIQUIDATION_MISSING`
- `DERIVATIVES_LEVERAGE_MISSING`
- `DERIVATIVES_LONG_SHORT_MISSING`
- `DERIVATIVES_LIQUIDITY_STRESS_HIGH`

### 4.4 BoundaryCandidateService Verification

For each fixture:

1. Provide RuntimeKlineContext-like input.
2. Provide SourceTrace-like input.
3. Provide DerivativesRiskContext-like input.
4. Provide RiskActionGuard state.
5. Verify candidate output.

Expected behavior:

- complete source + safe guard may return BoundaryCandidate `VALID`
- `VALID` must remain review candidate only
- missing entry / stop / TP / RR source must return fallback
- high-risk or fail-closed guard must downgrade positive output
- `manualReviewRequired=true`
- `notTradeInstruction=true`

Expected audit log entries:

- `BOUNDARY_CANDIDATE_VALID_REVIEW_ONLY`
- `BOUNDARY_CANDIDATE_INCOMPLETE_SOURCE`
- `BOUNDARY_CANDIDATE_WATCH_ONLY_SOURCE`
- `BOUNDARY_CANDIDATE_RISK_GUARD_BLOCKED`

### 4.5 ExecutionPlan Readiness Verification

For each fixture:

1. Provide BoundaryCandidate output.
2. Provide SourceTrace completeness state.
3. Provide RiskActionGuard state.
4. Verify ExecutionPlan display / readiness output.

Expected behavior:

- display-only state is not executable
- DTO-only state is not executable
- missing source is not executable
- high-risk state is not executable
- complete source + safe guard remains advisory / review-only

Expected audit log entries:

- `EXECUTION_PLAN_REVIEW_ONLY`
- `EXECUTION_PLAN_SOURCE_INCOMPLETE`
- `EXECUTION_PLAN_RISK_GUARD_BLOCKED`
- `EXECUTION_PLAN_NOT_EXECUTABLE`

### 4.6 RiskActionGuard Verification

For each high-risk fixture:

1. Provide liquidity stress.
2. Provide leverage / liquidation / long-short / event / wick state.
3. Verify guard output.

Expected behavior:

- high risk does not mean direct stop
- high risk does not mean direct reverse
- wick / spike does not mean trend reversal
- stampede state blocks new-open, reverse, and opportunity push semantics
- missing risk source stays fail-closed or watch-only

Expected audit log entries:

- `RISK_GUARD_SAFE`
- `RISK_GUARD_FAIL_CLOSED`
- `RISK_GUARD_LIQUIDITY_STRESS`
- `RISK_GUARD_EVENT_WINDOW`
- `RISK_GUARD_WICK_UNCONFIRMED`
- `RISK_GUARD_STAMPEDE_BLOCK`

### 4.7 Push / Recheck / Watchlist Verification

For Push / Recheck / Watchlist fixtures:

1. Provide legacy positive naming values.
2. Provide SourceTrace completeness state.
3. Provide RiskActionGuard state.
4. Verify no trading action is emitted.

Expected behavior:

- `VALID_EXECUTABLE` remains review-only naming
- `RECHECK_VALID_EXECUTABLE` remains review-only naming
- `valid=true` does not mean executable permission
- `PASS` does not bypass SourceTrace or RiskActionGuard
- `successCount` does not mean trade success
- `executionStatus` means job status only
- Watchlist does not emit open / close / reverse actions

Expected audit log entries:

- `PUSH_RECHECK_REVIEW_ONLY`
- `PUSH_RECHECK_LEGACY_VALID_EXECUTABLE_REVIEW_ONLY`
- `PUSH_RECHECK_VALID_FLAG_NOT_EXECUTION`
- `WATCHLIST_NO_TRADE_ACTION`

## 5. Expected Output Matrix

| Fixture ID | BoundaryCandidate | SourceTrace | DerivativesRiskContext | ExecutionPlan | RiskActionGuard | Push / Recheck / Watchlist | Expected Warnings / Audit Logs |
|---|---|---|---|---|---|---|---|
| P13-SA-01 | `VALID` review candidate | complete | complete / non-blocking | advisory review-only | safe | no action | `BOUNDARY_CANDIDATE_VALID_REVIEW_ONLY`, `EXECUTION_PLAN_REVIEW_ONLY` |
| P13-SA-02 | `INCOMPLETE` / `WATCH_ONLY` | entry missing | any | source incomplete | fallback | no action | `SOURCE_TRACE_ENTRY_MISSING`, `BOUNDARY_CANDIDATE_INCOMPLETE_SOURCE` |
| P13-SA-03 | `INCOMPLETE` / `WATCH_ONLY` | stop missing | any | source incomplete | fallback | no action | `SOURCE_TRACE_STOP_MISSING`, `BOUNDARY_CANDIDATE_INCOMPLETE_SOURCE` |
| P13-SA-04 | `INCOMPLETE` / `WATCH_ONLY` | TP missing | any | source incomplete | fallback | no action | `SOURCE_TRACE_TP_MISSING`, `BOUNDARY_CANDIDATE_INCOMPLETE_SOURCE` |
| P13-SA-05 | `INCOMPLETE` / `WATCH_ONLY` | RR missing | any | source incomplete | fallback | no action | `SOURCE_TRACE_RR_MISSING`, `EXECUTION_PLAN_SOURCE_INCOMPLETE` |
| P13-SA-06 | `WATCH_ONLY` / fail-closed | liquidity missing | liquidity missing | review-only fail-closed | fail-closed | no action | `SOURCE_TRACE_LIQUIDITY_MISSING`, `RISK_GUARD_FAIL_CLOSED` |
| P13-SA-07 | `WATCH_ONLY` | event active | event active | review-only fail-closed | fail-closed | no action | `SOURCE_TRACE_EVENT_BLOCKER_ACTIVE`, `RISK_GUARD_EVENT_WINDOW` |
| P13-SA-08 | `WATCH_ONLY` | wick unconfirmed | wick risk unknown | review-only fallback | fallback | no action | `SOURCE_TRACE_WICK_UNCONFIRMED`, `RISK_GUARD_WICK_UNCONFIRMED` |
| P13-SA-09 | `WATCH_ONLY` / fail-closed | complete | stress high | no opportunity semantics | fail-closed | no push opportunity | `RISK_GUARD_STAMPEDE_BLOCK` |
| P13-PA-01 | mixed per asset | mixed completeness | mixed risk | portfolio review-only | partial fail-closed | no action | `SOURCE_TRACE_LIQUIDITY_MISSING` |
| P13-PA-02 | review-only | complete | leverage high | review-only fail-closed | fail-closed | no action | `DERIVATIVES_LEVERAGE_MISSING` or `RISK_GUARD_FAIL_CLOSED` |
| P13-MT-01 | `VALID` review candidate | complete | non-blocking | advisory review-only | safe | no action | `SOURCE_TRACE_COMPLETE`, `EXECUTION_PLAN_REVIEW_ONLY` |
| P13-MT-02 | `WATCH_ONLY` | complete but timeframe conflict | non-blocking | review-only fallback | fallback | no action | `SOURCE_TRACE_MULTI_TIMEFRAME_MISSING` or conflict audit |
| P13-MT-03 | `WATCH_ONLY` / `INCOMPLETE` | higher-timeframe missing | unknown | source incomplete | fallback | no action | `SOURCE_TRACE_MULTI_TIMEFRAME_MISSING` |
| P13-PR-01 | unchanged | source state must still apply | risk state must still apply | no executable plan | guard still applies | review-only label | `PUSH_RECHECK_LEGACY_VALID_EXECUTABLE_REVIEW_ONLY` |
| P13-PR-02 | unchanged | not applicable | not applicable | no executable plan | not applicable | job status only | `PUSH_RECHECK_VALID_FLAG_NOT_EXECUTION` |
| P13-WL-01 | no VALID inference | missing source | unknown | no executable plan | fallback | no action | `WATCHLIST_NO_TRADE_ACTION` |

## 6. Risk Boundaries

P13 must preserve:

- BoundaryCandidate `VALID` is not a trade instruction.
- ExecutionPlan readiness is not automatic execution.
- Display-only output is not execution.
- DTO-only output is not execution.
- Missing source cannot produce executable state.
- High risk does not mean direct stop.
- High risk does not mean direct reverse.
- Wick / spike does not mean trend reversal.
- Stampede state blocks new-open, reverse, and opportunity push semantics.
- Funding, OI, liquidation, leverage, and long-short ratio must not directly generate trading action.
- Push / Recheck naming cannot imply execution.
- Watchlist cannot emit open / close / reverse actions from review-only status.

## 7. Acceptance Criteria

P13 planning is complete when:

- fixture groups are defined
- mock data categories are defined
- SourceTrace verification targets are defined
- DerivativesRiskContext verification targets are defined
- BoundaryCandidateService outputs are defined
- ExecutionPlan review-only expectations are defined
- RiskActionGuard fail-closed expectations are defined
- Push / Recheck / Watchlist non-trading expectations are defined
- expected warnings / audit logs are listed
- every scenario avoids order placement
- every scenario avoids actual ExecutionPlan execution
- every scenario avoids automated trading logic
- all fallback gates are explicit

## 8. Current Conclusion

P13 provides the small-scale dry-run test case catalog needed before any implementation-oriented dry-run task.

The next phase should choose whether to:

- implement local fixture-only tests for the P13 matrix
- keep tests document-only and add a P14 verification checklist
- split Push / Recheck naming correction into a separate semantic migration phase

No external API, Coinglass integration, executable plan generation, or automated trading behavior is included in P13.
