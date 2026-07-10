# V1 Historical Replay Validation Plan

## Purpose

P3 validates V1 business behavior against local replay-style market paths that are more sequential and path-dependent than the P2 point scenarios. It remains a local test package, not production deployment, provider validation, real trading, or profitability evidence.

## Baseline

- Base main commit: `803ed20ead6353315cbbaccca18c92f659fd82a0`
- P2 business stress result: PASS on merged main by PR #1104
- Production deployment readiness: BLOCKED

## Replay Data Contract

- Replay data source type: `LOCAL_REPLAY_FIXTURE_NOT_PROVIDER`
- Fixture implementation: in-memory OHLCV candle paths in `V1HistoricalReplayValidationTest`
- Every candle includes timestamp, open, high, low, close, and volume.
- The fixture paths are deliberately shaped local samples. They are not downloaded history, live Binance data, AI evidence, or profitability evidence.
- The local adapter supplies candles to a mocked `RealMarketDataFetcherService`; no provider implementation or network client is called.

## Validation Chain

1. Local OHLCV paths feed the existing `DecisionEngineService` through the local adapter.
2. The AI orchestrator is absent, so the existing `RULE_ONLY_FALLBACK` path is asserted and no AI provider is called.
3. A test-only replay adapter creates a review-only `ExecutionPlanDO` only when the decision permits an opportunity and local structural boundaries are complete.
4. `PushRecheckServiceImpl` validates fake-breakout invalidation and post-signal price drift.
5. `PositionMonitorServiceImpl` validates manual paper positions through valid, weakened, reversal, TP-zone, stop-zone, and high-risk paths.
6. Closed positions are rejected from active monitoring.

## Scenario Matrix

| Scenario | Expected business behavior |
| --- | --- |
| `HISTORICAL_STYLE_UPTREND_BREAKOUT` | Long candidate and complete review-only plan |
| `HISTORICAL_STYLE_DOWNTREND_BREAKDOWN` | Short candidate and complete review-only plan |
| `FAKE_BREAKOUT_REVERSAL` | Initial candidate, then structured Push Recheck invalidation |
| `CHOPPY_RANGE_NO_TRADE` | Observing/no complete plan |
| `WICK_STOP_SWEEP` | No complete plan; monitor path warns near stop when manually opened |
| `FAST_CRASH_REBOUND` | Confused/high-risk review block; no complete plan |
| `SLOW_TREND_PULLBACK` | Waiting-trigger candidate with complete review-only plan |
| `HIGH_RISK_EVENT_WINDOW` | High-risk block; no complete plan |
| `PRICE_DRIFT_AFTER_SIGNAL` | Original candidate becomes drifted in Push Recheck |
| `PAPER_OPEN_TO_TAKE_PROFIT` | Manual paper position reaches TP review zone; no auto-close |
| `PAPER_OPEN_TO_STOP_OR_INVALIDATION` | Manual paper position is invalidated; no auto-close |

## Metrics

- Scenario count
- Valid opportunity count
- False-positive count
- False-negative count
- Complete/incomplete plan count
- Recheck invalidated/drifted count
- Monitor coverage for `LOGIC_VALID`, `LOGIC_WEAKENED`, `PLAN_INVALIDATED`, and `HIGH_RISK`
- Closed positions in active monitoring

## Result Policy

The deterministic local assertions can pass, but the package-level `HISTORICAL_REPLAY_RESULT` remains `PARTIAL` because the repository contains no real local historical fixture and no direct production candle replay engine. This package must not be upgraded to real-history or profitability evidence.

## Commands

```bash
./mvnw -q -Dtest=V1HistoricalReplayValidationTest test
V1_HISTORICAL_REPLAY_CONFIRM=NO bash scripts/v1-historical-replay-local.sh
V1_HISTORICAL_REPLAY_CONFIRM=YES bash scripts/v1-historical-replay-local.sh
```

## Safety Boundary

- No production server or production DB access
- No Binance, OpenAI, Gemini, xAI, Grok, news, macro, or other provider call
- No order execution, auto-open, auto-close, auto-reverse, or auto-trading
- No external Push or Telegram send
- No fake persisted UserPosition or Review records outside the explicit local paper/replay test
- No production-ready or profitability claim
