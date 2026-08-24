# B.1.2.3.1 State Semantic Ownership Evidence

Start Head: `5066a61c52dca77c919dd2555bfc9c29e0ed97df`

Branch: `codex/frontend-interaction-runtime-closure`

PR: `#1195` (Draft, open, unmerged)

## Scope

This residual closure changes only two semantic bindings:

1. global Home data time ownership;
2. risk-level and opportunity-state conclusion separation.

It does not change Home layout, Position/close behavior, TRINE LOGIC copy,
Three-AI, Recheck, authentication, Telegram, schema, enums, state machines,
risk algorithms, or automatic-trading boundaries.

## Data timestamp chain

Old, invalid chain:

`LocalRealReadinessService.updatedAt -> DashboardHomeServiceImpl -> status/header`

Current formal chain:

`tm_persisted_ohlcv_bar.close_time_ms`
`-> PersistedOhlcvBarMapper.selectLatestClosedBar()`
`-> LocalRealDataStatusService.latestClosedBarAt()`
`-> DashboardHomeServiceImpl.globalDataUpdatedAt`
`-> System Status Data + PageHeader.updatedAt`

Null is preserved. There is no readiness, application-start, current-time, or
Provider CONNECTED fallback. The UI-review fixture keeps `updatedAt=null`, so
it displays `—` rather than inventing a market timestamp.

## Timestamp matrix

| Scenario | Status Data | PageHeader | Result |
|---|---|---|---|
| readiness timestamp exists, no persisted closed bar | `—` | null / no fake update | PASS |
| readiness state changes, latest closed bar unchanged | unchanged formal time | same unchanged time | PASS |
| latest persisted closed bar changes | new closed-bar time | same new closed-bar time | PASS |
| BTC to ETH selected-asset change | unchanged global time | unchanged global time | PASS |

## Risk and opportunity-state matrix

| Opportunity State | Risk Level | State label | Risk label | Conclusion | Result |
|---|---|---|---|---|---|
| `WAITING_TRIGGER` | `HIGH` | 等待触发 | 高 | 当前风险较高 | PASS |
| `WAITING_TRIGGER` | `EXTREME` | 等待触发 | 极高 | 当前风险极高 | PASS |
| `HIGH_RISK` | `EXTREME` | 高风险观察 | 极高 | 高风险观察 | PASS |
| missing | `HIGH` | null | 高 | 当前风险较高 | PASS |

The missing-state case proves that `riskLevel` is not an Opportunity State
fallback.

## Local validation

- Java 17 compile: PASS.
- Focused tests: `LocalRealDataStatusServiceTest` 5 and
  `DashboardHomeServiceImplTest` 103, total `108/108` PASS.
- Ownership/Node regression tests after source correction: PASS.
- Full Maven LOCAL RUN: `4782` tests, `0` failures, `0` errors, `14` skipped.
- Skips: existing Docker/Testcontainers-unavailable policy.
- `git diff --check`: PASS.

Exact-head GitHub CI is intentionally not conflated with this local run. The
final canonical PR comment records the pushed Head and exactly one required
`quality-gate` plus one required `workflow-contract`; no aggregate CI count is
claimed.

## Status

- `DATA_TIMESTAMP_OWNERSHIP = PASS`
- `RISK_LEVEL_OPPORTUNITY_STATE_SEPARATION = PASS`
- `B1_2_3_IMPLEMENTATION_DONE = YES`
- `CURRENT_PHASE_DONE = NO`
- `MERGE = NO`
