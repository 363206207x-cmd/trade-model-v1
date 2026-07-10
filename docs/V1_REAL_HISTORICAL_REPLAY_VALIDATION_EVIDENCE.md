# V1 Real Historical Replay Validation Evidence

## Evidence Identity

- Branch: `codex/p4-real-historical-fixture-direct-replay`
- Base main commit: `299fe0763448e7cb961f10a6e626a1ae28fafdee`
- Discovery scope: repository allowlist directories plus presence-only check of `V1_REAL_HISTORICAL_FIXTURE_DIR`
- Internet downloads: none
- Provider calls: none

## Final Status Fields

- `REAL_HISTORICAL_FIXTURE_STATUS: MISSING`
- `FIXTURE_PROVENANCE_STATUS: MISSING_EVIDENCE`
- `FIXTURE_INTEGRITY_STATUS: NOT_RUN_NO_FIXTURE`
- `DIRECT_REPLAY_ADAPTER_STATUS: PREPARED_NOT_RUN`
- `NO_LOOKAHEAD_GUARD_STATUS: PASS_CONTRACT_TEST_ONLY`
- `OPPORTUNITY_REAL_REPLAY_STATUS: NOT_RUN_BLOCKED_MISSING_FIXTURE`
- `EXECUTION_PLAN_REAL_REPLAY_STATUS: NOT_RUN_BLOCKED_MISSING_FIXTURE`
- `POSITION_MONITOR_REAL_REPLAY_STATUS: NOT_RUN_BLOCKED_MISSING_FIXTURE`
- `REAL_HISTORICAL_REPLAY_RESULT: BLOCKED_MISSING_REAL_FIXTURE`
- `PRODUCTION_READINESS: BLOCKED`

## Fixture Discovery Evidence

| Allowed source | Result |
| --- | --- |
| `src/test/resources/replay/real/` | Missing |
| `data/replay/real/` | Missing |
| `V1_REAL_HISTORICAL_FIXTURE_DIR` | Not supplied |

No unrelated local path was read. No candle data was downloaded, generated, relabeled, or committed.

Manifest state:

- Classification: `MISSING_REAL_HISTORICAL_FIXTURE`
- Row count: 0
- Date range: unavailable
- Symbols/timeframes: none
- SHA-256: unavailable
- Redistribution status: not applicable because there is no fixture

## Prepared Guard Evidence

- The loader requires all eight mandatory candle columns.
- Contract tests reject invalid OHLC and duplicate series timestamps.
- Continuity gaps are reported and not filled.
- SHA-256 calculation is prepared for a future local fixture.
- Replay frames expose only candles at or before replay time.
- Replay time cannot move backward.
- Historical candle and adapter types have no label/event-tag field.
- A separate assertion-label map remains outside the adapter.
- Missing-fixture gate tests verify Evidence, Score, Decision, Plan, and Position Monitor services receive no calls.

Contract-only temporary rows used to test parsing/guards are not a committed fixture and do not count as historical evidence.

## Direct Pipeline Result

The real direct pipeline was not run. The current architecture has no full-assembler seam that accepts a replay clock and time-bounded local candle source while bypassing live market environment reads and persistence. Because no real fixture exists, adding such a production seam now would not create valid business evidence and would expand scope unnecessarily.

No DecisionResult, ExecutionPlan, UserPosition, PositionMonitorResult, or Review record was fabricated for this evidence.

## Metrics

All real replay business metrics are `NOT_RUN`:

- Labeled windows: 0
- Detected/missed/false-positive windows: not evaluated
- Detection delay: not evaluated
- Complete plans: not evaluated
- Price-drift invalidations: not evaluated
- Manual paper entries and monitor cycles: 0
- Future-data leakage in guard tests: 0
- Provider calls: 0
- Production access: 0
- Exchange orders: 0
- Auto-open/close/reverse/trading: 0
- External Push/Telegram sends: 0

## Remaining Blockers

1. Provenance-backed real local fixture is missing.
2. Manifest provenance, license/redistribution, hash, range, symbols, timeframes, and gaps cannot be completed.
3. Required two-symbol, three-timeframe, six-window coverage is unavailable.
4. Full `AnalysisAssemblerServiceImpl` has no injected time-bounded replay source/clock seam.
5. Direct Evidence -> Score -> Decision -> Plan -> manual paper monitor replay has not run.

## Validation Record

- `./mvnw -q -Dtest=RealHistoricalFixtureValidationTest test`: PASS
- `./mvnw -q -Dtest=NoLookaheadReplayGuardTest test`: PASS
- `./mvnw -q -Dtest=V1RealHistoricalReplayValidationTest test`: PASS; gate result remains `BLOCKED_MISSING_REAL_FIXTURE`
- `./mvnw test -q`: PASS
- Script syntax and default dry-run: PASS
- Manifest/status YAML parse: PASS
- `git diff --check`: PASS
- `bash scripts/check-workflow-contract.sh`: PASS
- `bash scripts/v1-delivery-check.sh`: PASS
- `bash scripts/v1-state.sh`: PASS with expected branch-local `P0_0_DONE_PENDING_MERGED_MAIN`

## Decision

Do not run or claim real historical replay until the missing fixture and provenance evidence are supplied. Production deployment remains blocked.
