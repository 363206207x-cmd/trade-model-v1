# PHASE_P12_CONCEPT_VALIDATION_AND_PAPER_BACKTEST_PLAN

## 1. Document Purpose

This document defines the P12 concept validation and preliminary paper-backtest plan for Trade Model V1.

P12 builds on the committed P0-P10 baselines and the committed P11A / P11B safety review documents.

The goal is to validate whether the minimal closed-loop safety chain remains stable in simulated production-like scenarios:

- SourceTrace
- DerivativesRiskContext
- BoundaryCandidateService VALID / fallback behavior
- ExecutionPlan readiness review-only behavior
- RiskActionGuard fallback / fail-closed behavior
- Push / Recheck / Watchlist non-trading behavior

This document is planning only.

It does not modify source code, tests, resources, schema, config, or dashboard.

It does not connect external APIs, Coinglass, live exchange data, or order APIs.

It does not generate actual ExecutionPlan execution or automated trading code.

## 2. P12 Scope

P12 covers dry-run and paper-backtest planning only.

Allowed:

- local fixtures
- historical snapshots already available inside the project or test resources
- synthetic OHLCV / SourceTrace / DerivativesRiskContext examples
- in-memory test scenarios
- review-only output verification
- fallback / fail-closed assertions

Not allowed:

- Coinglass integration
- live OI / Funding / liquidation / leverage data pulls
- live order placement
- live position mutation
- automated open / close / reverse
- production ExecutionPlan generation
- dashboard schema mutation

## 3. Dry-Run Scenario Groups

| Scenario Group | Purpose | Expected Output |
|---|---|---|
| Complete source / safe risk | Verify that complete traceability can produce review-ready state. | BoundaryCandidate may be `VALID`, but remains `manualReviewRequired=true` and `notTradeInstruction=true`. |
| Missing boundary source | Verify fallback when entry / stop / TP / RR is absent. | `INCOMPLETE` or `WATCH_ONLY`; no VALID inference. |
| Missing derivatives risk source | Verify fallback when OI / Funding / liquidation / leverage / long-short source is absent. | Missing fields recorded; downstream remains review-only or fail-closed. |
| High-risk source | Verify RiskActionGuard behavior under liquidity stress, event window, wick-only, or stampede state. | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY`; no execution semantics. |
| Push / Recheck positive naming | Verify legacy positive naming cannot trigger action. | `VALID_EXECUTABLE`, `valid=true`, and `PASS` remain review-only labels. |
| ExecutionPlan readiness | Verify display / advisory outputs remain non-executable. | Review-only display; no executable plan. |

## 4. Simulated Data Inputs

P12 should define local fixture sets for:

- symbol
- timeframe
- latestPrice
- OHLCV window
- RuntimeKlineContext stale status
- dataQualityScore
- entry numeric source
- stop numeric source
- TP numeric source
- RR source
- liquidity source
- multi-timeframe source
- event window blocker
- wick confirmation source
- OI history placeholder
- Funding history placeholder
- liquidation cluster placeholder
- leverage distribution placeholder
- long / short ratio placeholder
- missingFields

All derivative-risk inputs may be synthetic or mocked.

Missing derivative-risk inputs must stay explicit and must not be converted into positive trading signals.

## 5. Verification Steps

### 5.1 Baseline Safety Check

Verify committed baselines:

- P0-P10 documents remain committed.
- P11A naming verification remains committed.
- P11B dry-run verification plan remains committed.
- staged area is empty before generating P12 implementation tasks.
- source code remains clean unless a later task explicitly changes it.

### 5.2 BoundaryCandidateService Check

For each fixture:

1. Provide RuntimeKlineContext-like input.
2. Provide SourceTrace-like input.
3. Provide DerivativesRiskContext-like input.
4. Provide RiskActionGuard state.
5. Evaluate BoundaryCandidateService expected behavior.

Expected outcomes:

- complete source + safe guard may produce BoundaryCandidate `VALID`
- missing source must produce `INCOMPLETE` or `WATCH_ONLY`
- fail-closed guard must downgrade or block positive output
- every result remains manual-review and non-trading

### 5.3 SourceTrace / DerivativesRiskContext Check

Verify:

- source completeness is explicit
- missing source is recorded
- no missing source is silently defaulted into valid source
- derivative-risk gaps remain visible
- `hasRequiredBoundarySources()` does not pass when entry / stop / TP / RR source is absent

### 5.4 ExecutionPlan Readiness Check

Verify:

- display-only state is not executable
- DTO-only state is not executable
- missing source is not executable
- fail-closed risk is not executable
- complete source plus safe guard remains advisory / review-only

ExecutionPlan must not become executable in P12.

### 5.5 RiskActionGuard Check

Verify:

- high risk does not mean direct stop
- high risk does not mean direct reverse
- wick / spike does not mean trend reversal
- stampede state blocks new-open, reverse, and opportunity push semantics
- liquidity worsening does not mean one-shot market close
- missing risk source stays fail-closed or watch-only

### 5.6 Push / Recheck / Watchlist Check

Verify:

- `VALID_EXECUTABLE` remains legacy review-only naming
- `RECHECK_VALID_EXECUTABLE` remains legacy review-only naming
- `valid=true` does not mean executable permission
- `PASS` does not bypass SourceTrace or RiskActionGuard
- `successCount` does not mean trade success
- `executionStatus` means job status only
- Watchlist does not emit open / close / reverse actions

## 6. Expected Output Matrix

| Input Condition | BoundaryCandidate | ExecutionPlan | RiskActionGuard | Push / Recheck / Watchlist |
|---|---|---|---|---|
| all sources complete, risk safe | `VALID` review candidate | advisory / review-only | safe | review-only |
| entry source missing | `INCOMPLETE` / `WATCH_ONLY` | review-only fallback | neutral or fallback | no action |
| stop source missing | `INCOMPLETE` / `WATCH_ONLY` | review-only fallback | neutral or fallback | no action |
| TP source missing | `INCOMPLETE` / `WATCH_ONLY` | review-only fallback | neutral or fallback | no action |
| RR source missing | `INCOMPLETE` / `WATCH_ONLY` | review-only fallback | neutral or fallback | no action |
| liquidity source missing | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY` | fail-closed review-only | fail-closed | no action |
| event blocker active | `WATCH_ONLY` | fail-closed review-only | fail-closed | no action |
| wick-only source | `WATCH_ONLY` | review-only fallback | no trend reversal inference | no action |
| stampede state | `WATCH_ONLY` / fail-closed | no opportunity semantics | fail-closed | no push opportunity |

## 7. Risk Boundaries

P12 must preserve:

- BoundaryCandidate `VALID` is not a trade instruction.
- ExecutionPlan readiness is not automatic execution.
- Display-only output is not execution.
- DTO-only output is not execution.
- Missing source cannot produce executable state.
- High risk does not mean direct stop.
- High risk does not mean direct reverse.
- Wick / spike does not mean trend reversal.
- Stampede state blocks new-open, reverse, and opportunity push semantics.
- Funding / OI / liquidation / leverage / long-short ratio must not directly generate trading action.

## 8. Suggested Future Verification Commands

Commands for a later implementation / verification task:

```bash
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
./mvnw -q -Dtest=BoundaryCandidateServiceImplTest test
./mvnw -q -Dtest=DefaultExecutionPlanDisplayAdapterTest test
./mvnw -q -Dtest=PlanServiceImplTest test
./mvnw -q -Dtest=RuleEngineServiceSourceTraceTest test
./mvnw -q -Dtest=PushRecheckServiceImplTest test
```

This document does not run these commands.

## 9. P12 Acceptance Criteria

P12 planning is complete when:

- dry-run scenarios are defined
- simulated data inputs are defined
- expected outputs are defined
- fallback rules are explicit
- review-only behavior is explicit
- fail-closed behavior is explicit
- Push / Recheck naming cannot imply execution
- no external API is connected
- no actual ExecutionPlan is generated
- no automated trading code is generated

## 10. Current Conclusion

P12 should move from document-only closure into concept validation planning without changing production behavior.

The next implementation task should still be small and controlled:

- use only local fixtures or mocks
- verify fallback and review-only outputs
- keep all trading actions deferred
- keep Coinglass and external derivative APIs out of scope

This P12 document should remain untracked until explicitly reviewed and staged.
