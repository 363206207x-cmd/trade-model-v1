# V1 Business Stress Test Plan

Package: P2 V1 End-to-End Business Stress Test
Branch: `codex/p2-v1-business-stress-test`
Current main commit: `761d3603b413abab054631502597c0fd86d40e64`
Status date: 2026-07-10

`REAL_TRADING_EXECUTED: NO`

`AUTO_OPEN_EXECUTED: NO`

`AUTO_CLOSE_EXECUTED: NO`

`AUTO_REVERSE_EXECUTED: NO`

`ORDER_EXECUTION_EXECUTED: NO`

`PROVIDER_CALLS_EXECUTED: NO`

`PRODUCTION_READINESS: BLOCKED`

## Purpose

This package runs a local deterministic business stress test for the V1 decision-support loop:

Synthetic market context -> evidence semantics -> decision -> manual execution-plan completeness -> manual paper position -> position monitor -> suggested manual action -> close/review-only boundary.

It verifies business semantics, not live profitability, provider connectivity, production deployment, or automatic trading.

## Scope

The test uses deterministic `SYNTHETIC_SCENARIO_DATA`, local unit/service fixtures, Mockito doubles, and H2/test context where applicable. It may execute local JUnit tests. It must not call real Binance, OpenAI, Gemini, xAI/Grok, external news/macro providers, production servers, production databases, Push send, Telegram send, or order/execution surfaces.

## Scenario List

| Scenario | Expected behavior |
|---|---|
| `BULLISH_BREAKOUT_VALID` | BULLISH candidate with complete manual review plan. |
| `BULLISH_PULLBACK_VALID` | BULLISH waiting/candidate state with complete manual review plan. |
| `BEARISH_BREAKDOWN_VALID` | BEARISH candidate with complete manual review plan. |
| `NOISY_RANGE_NO_TRADE` | No complete manual plan; no false positive opportunity. |
| `HIGH_RISK_BLOCKED` | High-risk block; no complete manual plan. |
| `CONFLICTED_AI_OR_RULES` | Confused/downgraded state; no directional executable plan. |
| `PRICE_DRIFT_AFTER_SIGNAL` | Push Recheck freshness validation returns drifted/invalidated review-only status. |
| `ENTRY_LOGIC_STILL_VALID` | Manual paper OPEN position remains `LOGIC_VALID`. |
| `ENTRY_LOGIC_WEAKENS_AFTER_OPEN` | Monitor detects weakened entry logic and asks manual review. |
| `STRONG_REVERSAL_AFTER_OPEN` | Monitor detects invalidation/reversal and asks manual review. |
| `HIT_TAKE_PROFIT_ZONE` | Monitor detects take-profit zone and suggests manual review. |
| `HIT_STOP_ZONE` | Monitor detects stop zone/invalidation and suggests manual review. |
| `CLOSED_POSITION_REVIEW_ONLY` | Closed paper position is excluded from active monitoring. |

## Metrics

Opportunity discovery metrics:

- scenarios tested
- valid opportunities detected
- missed valid opportunities
- false positives
- conflict downgrades
- high-risk blocks
- confused blocks

Execution-plan metrics:

- plans generated
- complete manual plans
- plans with direction, entry, stop, TP, invalidation, risk/source traceability
- incomplete plans rejected
- price-drift invalidations

Position-monitor metrics:

- paper positions opened manually by test harness
- monitor cycles run
- logic-valid detections
- logic-weakened detections
- reversal / invalidation detections
- stop-zone detections
- take-profit-zone detections
- suggested manual actions generated
- CLOSED positions excluded from active monitoring

Safety metrics:

- real provider calls: must be 0
- order executions: must be 0
- auto-open / auto-close / auto-reverse: must be 0
- external Push sends: must be 0
- fake positions outside explicit local paper/test scenario: must be 0
- fake review records outside explicit local paper/test scenario: must be 0

## Pass / Fail Criteria

PASS requires at least 10 deterministic scenarios, no false positives in no-trade/high-risk/confused scenarios, complete manual plans for valid opportunities, manual paper positions entering monitor, closed positions excluded, monitor detecting valid/weakened/invalidation/stop-or-TP states, suggested manual action text where risk appears, Maven tests passing, and production readiness remaining BLOCKED.

PARTIAL is allowed only when missing implementation is explicitly recorded. The package must not fake PASS.

FAIL occurs if any test introduces auto-open, auto-close, auto-reverse, order execution, real provider calls, CLOSED active monitoring, execution suggestion becoming a real position, executable-looking incomplete plans, or failing tests.

## Evidence Format

Generated local evidence, if the optional harness is used, goes under `build/v1-business-stress/` and is ignored by Git. Redacted summary evidence is recorded in `docs/V1_BUSINESS_STRESS_TEST_EVIDENCE.md`.

## Commands

Targeted deterministic test:

```bash
./mvnw -q -Dtest=V1BusinessStressTest test
```

Dry-run harness:

```bash
bash scripts/v1-business-stress-local.sh --dry-run
```

Confirmed local harness, only when explicitly approved:

```bash
V1_BUSINESS_STRESS_CONFIRM=YES bash scripts/v1-business-stress-local.sh
```

## Safety Boundary

This is a local paper-only business stress package. It is not production deployment, real trading, provider live smoke, release approval, or profitability evidence.
