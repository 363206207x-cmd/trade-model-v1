# P2 Position Monitoring Backend Capability Audit

## Audit Status

- Package: `P2_POSITION_MONITORING_BACKEND_IMPLEMENTATION`
- Candidate branch: `codex/p2-position-monitoring-backend-contract`
- Authorization baseline: `0e133093` (`#1168` merged)
- Result: `AUDIT_REMEDIATION_COMPLETE`
- Mainline completion: `NO` (only merged `main` can complete P2)

## Source Chain

`tm_user_position`
-> `UserPositionService.listOpenPositionsForUser`
-> `PositionMonitorServiceImpl`
-> fresh market-price snapshot + exact execution-plan source + verified current evidence context
-> `SinglePositionRiskCalculator` / `PositionReversalEvaluator`
-> `tm_position_monitor_log`
-> `DashboardHomeServiceImpl`
-> `DashboardHomeVO.PositionVO`

Only a monitor row with `source_status=VERIFIED`, a non-empty mark-price source,
complete independent semantic fields, and `observed_at <= asOf < fresh_until`
may expose monitor results on Home. Pending, invalid, expired, malformed, or
semantically inconsistent rows fail closed.

## Frozen Field Mapping

| Frozen field | Backend source | Missing / untrusted behavior |
| --- | --- | --- |
| `symbol` | `tm_user_position.asset_symbol` | Position omitted when core identity is invalid |
| `direction` | `tm_user_position.side` (`LONG` / `SHORT`) | Position module is invalid; no inferred direction |
| `entryPrice` | `tm_user_position.entry_price` | No PnL; module remains incomplete |
| `markPrice` | latest trusted monitor `current_price` | `null` |
| `markPriceSource` | monitor `mark_price_source` | `null` |
| `markPriceObservedAt` | monitor `observed_at` | `null` |
| `markPriceFresh` | verified source plus strict freshness window | `false` |
| `pnlAmount` | direction-aware `(mark-entry)*quantity` | `null` when mark price or quantity is unavailable |
| `pnlPercent` | direction-aware price return | `null` when mark price is unavailable |
| `riskLevel` | independent per-position risk calculation persisted in monitor log | `null` |
| `riskTrend` | comparison with the prior historically trusted risk level | `null` |
| `monitorConclusion` | `monitor_conclusion` | `null` |
| `entryLogicStatus` | `entry_logic_status` | `null` |
| `reversalStatus` | current rule-layer direction compared with position direction | `null` when the rule source is unavailable |
| `riskReason` | `risk_change_reason` | `null` |
| `suggestedAction` | explicit manual-advisory enum paired with monitor conclusion | `null` |
| `lastMonitorTime` | trusted monitor `created_at` | `null` |
| `dataState` | trusted monitor state projection | `WAITING_MONITOR_DATA` |

## Independent State Contracts

- Entry logic: `STILL_VALID`, `WEAKENED`, `INVALIDATED`
- Monitor conclusion: `LOGIC_VALID`, `LOGIC_WEAKENED`, `PLAN_INVALIDATED`,
  `NEAR_STOP_LOSS`, `NEAR_TAKE_PROFIT`, `HIGH_RISK_OBSERVATION`,
  `WAIT_USER_CONFIRM_CLOSE`
- Reversal: `NO_REVERSAL`, `WEAK_REVERSAL`, `STRONG_REVERSAL`
- Risk reason: `NO_CLEAR_RISK_FACTOR`, `OPPOSING_EVIDENCE_INCREASED`,
  `STRUCTURE_CHANGED`, `EVENT_IMPACT`, `DATA_QUALITY_DEGRADED`
- Position risk: `LOW`, `MEDIUM`, `HIGH`, `EXTREME`
- Position risk trend: `STABLE`, `INCREASED`, `SHARPLY_INCREASED`
- Manual advisory: `CONTINUE_HOLD`, `NO_ADD_POSITION`, `REDUCE_POSITION`,
  `TIGHTEN_STOP`, `MOVE_STOP`, `PARTIAL_TAKE_PROFIT`, `WAIT_CONFIRMATION`,
  `RECORD_CLOSE_REVIEW`
- Monitor source: `VERIFIED`, `PENDING_VERIFICATION`, `INVALID`
- Home data state: `NO_POSITION`, `OPEN_MONITORING`, `WAITING_MONITOR_DATA`,
  `RISK_ESCALATED`, `PLAN_INVALIDATED`, `CLOSED`

`logic_status` remains a nullable legacy compatibility column. New writes do not
populate it, and no new Home, message, review, or scan decision reads it as a
fallback.

## Schema And Persistence

Migration `V10__position_monitoring_backend_contract.sql` adds independent
semantic columns, mark-price provenance, source status, observation time, and a
strict freshness boundary. Existing rows are migrated to
`PENDING_VERIFICATION`; their new semantic fields are left `NULL` rather than
invented from legacy values.

Database checks enforce exact enums and reject:

- verified rows with incomplete semantic payloads;
- verified rows without a positive freshness window;
- pending or invalid rows carrying risk, conclusion, reversal, or action values.

The PostgreSQL migration regression starts from Flyway V8, inserts legacy
position and monitor rows, applies V9 and V10, and verifies both the legacy
backfill and a post-V10 insert. Legacy semantic fields remain `NULL`, while
`source_status` is backfilled/defaulted to `PENDING_VERIFICATION`.

## Verified Trust Gate

`VERIFIED` is no longer inferred from a direction result alone. A monitor run
must have all of the following at record time:

- a successful current analysis run whose analysis identity and symbol match;
- a current decision with valid, passing data quality;
- at least one evidence item and all eight score dimensions;
- required direction and multi-timeframe context;
- fresh analysis and decision timestamps relative to the fresh market-price
  observation;
- an exact execution-plan source and an available reversal assessment.

Missing or stale evidence becomes `PENDING_VERIFICATION`; malformed identity,
quality, or timestamp data becomes `INVALID`. A `VERIFIED` command is also
rejected if its freshness window has already elapsed when persisted.

## Risk And State Behavior

- Risk is calculated per `UserPosition`, never copied from an owner aggregate.
- `riskLevel` answers absolute severity; `riskTrend` answers change from the
  prior historically trusted result. `HIGH` or `EXTREME` alone does not produce
  `RISK_ESCALATED`; only `INCREASED` or `SHARPLY_INCREASED` does.
- Direction, entry price, mark price, quantity, leverage, stop loss, take profit,
  and available market-risk context are evaluated independently per position.
- Missing or directionally invalid boundaries increase risk instead of producing
  a low-risk default.
- Closed positions are rejected by the monitor and excluded from Home active
  positions; closing remains manual and leads to history/review.
- No action enum represents automatic open, close, reverse, or order execution.

Provider scan transitions now ignore every monitor row that is not both
`VERIFIED` and fresh at scan time. Message history uses the original row as a
historical trust snapshot, so a previously valid message does not become an
error merely because its realtime freshness window later expires. A separate
current recheck remains subject to realtime freshness.

## API Contract

`GET /api/dashboard/home` exposes the frozen independent position fields. A
trusted monitor result may include mark-price provenance, PnL, risk, conclusion,
entry-logic status, reversal status, risk reason, suggested action, monitor time,
and data state. If trust fails, those result fields are `null` and the state is
`WAITING_MONITOR_DATA`; there is no cross-field fallback.

## Validation Evidence

| Check | Result |
| --- | --- |
| Focused trust/risk/provider/message/migration tests | `PASS` |
| Full Maven suite | `4333 passed, 0 failed, 0 errors, 14 skipped` |
| JDK 17 CI-equivalent `-Pci verify` | `PASS` |
| H2 schema and mapper constraint integration | `PASS` |
| PR CI quality gate | `735 passed, 0 failed, 0 errors, 0 skipped` (run `31359190007`) |
| PostgreSQL 16 Testcontainers migration smoke | `PASS`: `1 passed, 0 skipped` in PR CI |
| Product Source Gate | `PASS` |
| Workflow contract | `PASS` (run `31359189984`) |
| `git diff --check` | `PASS` |

The local PostgreSQL smoke was skipped because Docker is unavailable. The
Docker-enabled GitHub runner executed it against PostgreSQL 16 with no skip,
proving the V8 legacy setup, V9/V10 Flyway upgrade, historical backfill, new-row
defaults, and resulting constraints on the PR merge commit.

## Completion Gates

- `EVERY_FROZEN_FIELD_HAS_SOURCE = PASS`
- `EVERY_STATE_HAS_ENUM = PASS`
- `EVERY_DISPLAY_FIELD_HAS_TRUST_RULE = PASS`
- `VERIFIED_REQUIRES_FRESH_EVIDENCE_AND_REQUIRED_CONTEXT = PASS`
- `RISK_LEVEL_AND_RISK_TREND_SEPARATED = PASS`
- `PROVIDER_TRUSTED_RESULT_ONLY = PASS`
- `HISTORICAL_SNAPSHOT_REALTIME_EXPIRY_SAFE = PASS`
- `MISSING_CAPABILITIES_IDENTIFIED = PASS`
- `NO_SEMANTIC_FALLBACK = PASS`
- `NO_FAKE_DATA = PASS`
- `NO_AGGREGATE_RISK_AS_POSITION_RISK = PASS`
- `NO_AUTO_TRADING = PASS`
- `NO_AUTO_CLOSE = PASS`
- `NO_AUTO_REVERSE = PASS`
- `FIGMA_CHANGED = NO`
- `MOBILE_CHANGED = NO`

## Review Boundary

The remediated candidate has passed local validation and both required PR
workflows. Merge recommendation: `APPROVE`, subject to normal Product Owner/code
review. P2 remains incomplete until the reviewed commit is merged to `main` and
merged-main validation passes.
