# V1 Historical Replay Validation Evidence

## Evidence Identity

- Branch: `codex/p3-historical-market-replay-validation`
- Base main commit: `803ed20ead6353315cbbaccca18c92f659fd82a0`
- Replay data source type: `LOCAL_REPLAY_FIXTURE_NOT_PROVIDER`
- Fixture classification: `SYNTHETIC_REPLAY_FIXTURE`
- Live/provider data: not used
- Profitability evidence: not claimed

## Final Status

- `HISTORICAL_REPLAY_RESULT: PARTIAL`
- `OPPORTUNITY_REPLAY_STATUS: PASS`
- `EXECUTION_PLAN_REPLAY_STATUS: PASS`
- `POSITION_MONITOR_REPLAY_STATUS: PASS`
- `PRODUCTION_READINESS: BLOCKED`

`PARTIAL` is mandatory at package level because the replay uses clearly labeled local synthetic paths rather than real historical fixtures. The three focused statuses are PASS only for the tested local replay contract.

## Scenario Results

| Scenario | Opportunity / plan | Recheck / monitor result | Status |
| --- | --- | --- | --- |
| Uptrend breakout | BULLISH candidate; complete review-only plan | Not applicable | PASS |
| Downtrend breakdown | BEARISH candidate; complete review-only plan | No long false positive | PASS |
| Fake breakout reversal | Initial BULLISH candidate | `INVALIDATED` after structured reversal price | PASS |
| Choppy range | Observing; incomplete plan | No executable plan | PASS |
| Wick stop sweep | Observing; incomplete plan | Near-stop paper path yields `LOGIC_WEAKENED` | PASS |
| Fast crash rebound | Confused; incomplete plan | High-risk paper path yields `HIGH_RISK` | PASS |
| Slow trend pullback | Waiting trigger; complete review-only plan | Candidate survives controlled pullback path | PASS |
| High-risk event window | High risk; incomplete plan | External context fail-closed | PASS |
| Price drift after signal | Initial candidate | `DRIFTED_FROM_ENTRY_ZONE` | PASS |
| Paper open to TP | Manual paper OPEN only | Near TP, `MANUAL_REVIEW`, positive local PnL display | PASS |
| Paper open to stop | Manual paper OPEN only | `PLAN_INVALIDATED`, `RECHECK_PLAN` | PASS |
| Closed-position exclusion | No active paper position | Batch total 0; direct monitor rejects CLOSED | PASS |

## Opportunity Detection Evidence

- Decision replay scenarios: 9
- Valid initial opportunities: 5
- No-trade/high-risk/confused scenarios: 4
- False positives: 0
- False negatives: 0
- Both BULLISH and BEARISH rule-layer directions are exercised.
- `RULE_ONLY_FALLBACK` is asserted for every replay decision; no AI provider is called.

## Execution Plan Evidence

- Complete review-only plans: 5 initial opportunity states
- Incomplete/fail-closed plans: 4 no-trade, confused, wick-risk, or high-risk states
- Complete plans contain direction, entry, stop, staged target, invalidation, risk snapshot, analysis identifier, and source-completeness summary.
- Every plan remains `notTradeInstruction`, `notExecutable`, `notAutoTrading`, `notOrderExecution`, and `notUserPositionCreation`.
- Fake breakout reversal and price drift are freshness outcomes, not trading authorization.

## Position Monitor Usefulness

The monitor helped in the following local paper paths:

- Valid logic: `LOGIC_VALID` / `SUPPORTED`
- Weakening near stop: `LOGIC_WEAKENED` / `MANUAL_REVIEW`
- Strong reversal or stop breach: `PLAN_INVALIDATED` / `RECHECK_PLAN`
- TP zone: `LOGIC_VALID` plus `MANUAL_REVIEW`
- High-risk context: `HIGH_RISK` / `RISK_REVIEW`
- Closed position: excluded from batch monitoring and rejected by direct monitor call

The monitor did not mutate positions and did not invoke `manualClose`.

## Remaining Gaps

1. No real historical CSV/JSON fixture exists in the repository.
2. The production decision engine consumes a small current-window kline shape rather than a first-class candle-by-candle replay session.
3. The test-only execution-plan adapter proves boundary completeness semantics but is not a production plan generator.
4. Slippage, fees, latency, partial fills, and exchange microstructure are not modeled.
5. No claim is made about live-provider behavior, profitability, production readiness, or release approval.

## Safety Evidence

- No production server or DB accessed
- No live Binance or other provider call
- No OpenAI/Gemini/xAI/Grok call
- No real order, order execution, auto-open, auto-close, auto-reverse, or auto-trading
- No external Push or Telegram send
- No fake persisted position or review record outside explicit local paper/replay test state
- Production deployment remains blocked

## Validation Record

- `./mvnw -q -Dtest=V1HistoricalReplayValidationTest test`: PASS
- `./mvnw test -q`: PASS
- `bash -n scripts/v1-historical-replay-local.sh`: PASS
- `V1_HISTORICAL_REPLAY_CONFIRM=NO bash scripts/v1-historical-replay-local.sh`: PASS / `DRY_RUN`
- `bash scripts/check-workflow-contract.sh`: PASS
- `bash scripts/v1-delivery-check.sh`: PASS
- `bash scripts/v1-state.sh`: PASS with expected branch-local `P0_0_DONE_PENDING_MERGED_MAIN`
