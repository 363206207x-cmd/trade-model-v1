# V1 Real Historical Replay Validation Plan

## Baseline

- Base main commit: `299fe0763448e7cb961f10a6e626a1ae28fafdee`
- P2 deterministic business stress: PASS
- P3 synthetic replay-style package: PARTIAL
- Production deployment readiness: BLOCKED

## Discovery Result

The allowed repository directories do not exist and `V1_REAL_HISTORICAL_FIXTURE_DIR` was not supplied. No internet download or unrelated filesystem search was attempted.

- Fixture classification: `MISSING_REAL_HISTORICAL_FIXTURE`
- Real replay execution: blocked before business services
- Required package result: `BLOCKED_MISSING_REAL_FIXTURE`

## Prepared Components

Test-support code prepares:

1. UTF-8 CSV loading for the required candle columns.
2. OHLCV, timestamp, duplicate, timeframe, and continuity validation.
3. SHA-256 calculation.
4. Gap reporting without silent filling.
5. A monotonic replay clock exposing only candles at or before the current replay time.
6. Structural separation of assertion labels/event tags from replay candles.
7. A fail-closed gate test proving Evidence, Score, Decision, Plan, and Position Monitor services are not invoked when provenance-backed input is missing.

These components are preparation only. They do not constitute a direct real-business-pipeline replay run.

## Direct Pipeline Assessment

The actual chain is owned by `AnalysisAssemblerServiceImpl`, but its current runtime entry obtains market environment data through `RealMarketEnvironmentService`, asks `DecisionEngineService` to fetch klines through `RealMarketDataFetcherService`, and persists the assembled run. There is no injected historical clock/candle source for the full assembler path.

`PlanServiceImpl` can consume source-trace/boundary inputs, and `PositionMonitorServiceImpl` can monitor a manual UserPosition, but neither may be exercised as real historical evidence until a valid fixture exists and a time-bounded input seam is provided. Constructing final DecisionResult, ExecutionPlan, or PositionMonitorResult in the test would violate the anti-cheating rule.

## Next Execution Preconditions

Before a real replay can run:

1. Supply a provenance-backed fixture in an allowed local path.
2. Verify redistribution status before committing raw data.
3. Populate and review the manifest, including SHA-256 and known gaps.
4. Require at least two symbols, 5m/15m/1h coverage, six labeled windows, and sufficient warm-up bars for PASS eligibility.
5. Add the smallest time-bounded replay input seam into the existing context model without enabling provider or production access.
6. Keep expected labels in a separate post-run evaluation file.
7. Re-run integrity, no-lookahead, direct-pipeline, full Maven, and safety gates.

## Safety Boundary

- No production server or DB access
- No live Binance or AI provider call
- No real exchange order or external Push/Telegram
- No auto-open, auto-close, auto-reverse, or auto-trading
- No secret read/print/commit
- No real-history, provider-validation, profitability, or production-readiness claim without evidence
