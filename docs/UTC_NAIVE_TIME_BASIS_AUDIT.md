# UTC-Naive Time Basis Audit

## 1. Scope and decision

This audit records the Reviewer Round 8 timestamp fixes, the Reviewer Round 10 closure for Dashboard UTC-day metrics and Run Baseline read windows, and the Reviewer Round 11 closure for new Monitor Alert writes and Hot Reset fallback times. Round 11 removes database/JVM-local clocks from those new-write paths without relabeling historical rows as UTC-proven.

The compatibility contract is:

- A UTC-naive value is a UTC instant represented as `LocalDateTime` without an offset.
- It is not database-local time and must not be compared with `CURRENT_DATE`, `CURRENT_TIMESTAMP`, `LOCALTIMESTAMP`, or `NOW()`.
- Query callers calculate explicit UTC bounds from an injected `Clock` and bind those values as parameters.
- Running both the JVM and database in UTC is recommended configuration, but it is not a substitute for explicit query bounds.

This package does not migrate columns to `TIMESTAMP WITH TIME ZONE`, change business-date fields, or set a global JVM timezone.

## 2. Reviewed contracts

| Table and field | Writer | Storage basis | Query basis after audit | Mixed basis | Result |
|---|---|---|---|---|---|
| `tm_decision_result.create_time` | `AnalysisAssemblerServiceImpl` | UTC-naive from the assembler clock | Explicit UTC day `[startInclusive, endExclusive)` | No | Fixed in Round 8 |
| `tm_push_recheck_log.create_time` and `recheck_time` | `PushRecheckServiceImpl` | UTC-naive through `UtcLocalTimePolicy` | Explicit UTC window `[windowStartInclusive, asOfInclusive]` | No | Fixed in Round 8 |
| `tm_push_snapshot.expires_at` | `PushSnapshotService` | UTC-naive through `UtcLocalTimePolicy` | Explicit UTC as-of supplied by callers | No | Existing contract retained |
| `tm_analysis_run.analysis_time` | `AnalysisTimePolicy` and analysis-run orchestration | UTC-naive for generated/offset-aware inputs; legacy no-offset input is also accepted | Explicit Baseline UTC window `[windowStartInclusive, asOfInclusive]` | Historical input basis is not uniformly proven | Baseline read fixed in Round 10; broader writer audit remains |
| `tm_hot_reset_event.event_time` | `HotResetServiceImpl` | New null-`occurredAt` fallback, event/create/completion timestamps, asset-state updates, and rebuild outcomes use an injected UTC clock. Non-null command time has an explicit UTC-naive caller contract. | Explicit Baseline UTC window `[windowStartInclusive, asOfInclusive]` | Upgrade-era historical command inputs remain unverified | New fallback writes fixed in Round 11; historical `event_time` evidence remains a production gate |
| `tm_monitor_alert.created_at` / `updated_at` / `cooldown_until` | `MonitorAlertWriteServiceImpl` and `MonitorAlertMapper.insert` | New rows explicitly bind typed UTC-naive values derived from one injected-clock read; database defaults are not used by this writer | Throttle, semantic suppression, and Baseline all use explicit inclusive UTC bounds | Upgrade-era rows created from database-session defaults are unverified | New writes and reads fixed in Round 11; historical-row policy remains required |
| `tm_missed_opportunity.biz_date` | Missed-opportunity business records | Business date, not an instant | Dashboard passes the same UTC calendar date used by decision-today metrics | Not applicable | Dashboard UTC-day contract fixed in Round 10; column type unchanged |

The limited audit searched mapper SQL for `CURRENT_DATE`, `CURRENT_TIMESTAMP`, `LOCALTIMESTAMP`, `NOW()`, `DATEADD`, `INTERVAL`, and date casts, and searched writers for `UtcLocalTimePolicy.now`, `utcLocalNow`, and UTC `LocalDateTime.ofInstant` conversions.

Round 11 additionally changes only new Monitor Alert writes, their two write-side window queries, and Hot Reset service fallback timestamps. It does not rewrite historical rows, change timestamp column types, or execute a migration. Ambiguous historical writer contracts are recorded rather than guessed and remain part of production-readiness blocking evidence.

## 3. Decision count contract

`DecisionServiceImpl` uses its injected UTC `Clock` to calculate:

```text
utcDate = clock.instant() at UTC
startUtc = utcDate 00:00:00
endUtc = startUtc + 1 day
```

`DecisionResultMapper.countDecisionsInRange` then applies the index-friendly predicate:

```sql
create_time >= :startInclusive
AND create_time < :endExclusive
```

The same `utcDate` is also passed to `MissedOpportunityMapper.countByBizDate`. The contract is therefore `Dashboard today = the current UTC calendar day` for both decisions and missed opportunities. Neither metric uses the JVM default timezone or database current date, and `tm_missed_opportunity.biz_date` remains a date column.

## 4. Unified Run Baseline window contract

`RunBaselineServiceImpl` obtains one UTC-naive as-of value from its injected clock, derives one start boundary, and passes that exact pair to every Baseline summary:

```text
asOfUtc = UtcLocalTimePolicy.now(clock)
windowStartUtc = asOfUtc - windowMinutes
```

The covered timestamp columns are:

- `tm_monitor_alert.created_at`
- `tm_analysis_run.analysis_time`
- `tm_push_recheck_log.create_time`
- `tm_hot_reset_event.event_time`

Every Baseline query, including Hot Reset trigger-type grouping, applies:

```sql
create_time >= :windowStartInclusive
AND create_time <= :asOfInclusive
```

The upper bound excludes future records. These Baseline methods each have one portable SQL definition and no `CURRENT_TIMESTAMP`, `CURRENT_DATE`, H2 `DATEADD`, or PostgreSQL interval calculation. Push Recheck retains its existing explicit range contract. Monitor Alert throttle and semantic-suppression reads now use the same explicit UTC-naive range contract.

## 5. Reviewer Round 11 write contract

For each Monitor Alert candidate, `MonitorAlertWriteServiceImpl` reads its injected clock once:

```text
nowUtc = UtcLocalTimePolicy.now(clock)
throttleStartUtc = nowUtc - 15 minutes
semanticStartUtc = nowUtc - 45 minutes
cooldownUntilUtc = nowUtc + 15 minutes (OPEN only)
```

The same `nowUtc` is bound as `created_at` and `updated_at`; `cooldown_until` is a typed `LocalDateTime` parameter or explicit null. `MonitorAlertMapper.insert` no longer relies on `DEFAULT CURRENT_TIMESTAMP`, and both write-side window queries use `created_at >= :windowStartInclusive AND created_at <= :asOfInclusive`.

`HotResetServiceImpl` also uses an injected UTC clock. One evaluation time is reused for null-`occurredAt` fallback, decision/plan invalidation, asset-state updates, event creation/completion, and the initial result. The legacy entry point leaves `occurredAt` empty so this same fallback contract applies instead of generating a JVM-local wall-clock value. A rebuild outcome takes one new UTC completion time when that later operation runs. Explicit command `occurredAt` values are required to be UTC-naive.

The controlled PostgreSQL test is environment-gated and executes `SET TIME
ZONE 'UTC'`, `SET TIME ZONE 'Asia/Shanghai'`, and `SET TIME ZONE
'America/New_York'` before the real Writer -> Mapper -> Run Baseline chain.
The original Round 11 run was skipped because its Docker daemon/socket was
unavailable. Subsequent P1/P2 evidence on 2026-07-15 ran all three PostgreSQL
sessions as **PASS_NOT_SKIPPED** on disposable PostgreSQL 16.14. The historical
skip remains an accurate record; the current controlled-local gate is recorded
in `docs/POSTGRESQL_FLYWAY_V7_CONTROLLED_EVIDENCE.md`.

## 6. Boundary evidence

Fixed instant: `2026-07-14T23:30:00Z`.

| JVM default timezone | UTC day start | UTC day end |
|---|---|---|
| `UTC` | `2026-07-14T00:00:00` | `2026-07-15T00:00:00` |
| `Asia/Shanghai` | `2026-07-14T00:00:00` | `2026-07-15T00:00:00` |
| `America/New_York` | `2026-07-14T00:00:00` | `2026-07-15T00:00:00` |

Decision day boundaries:

| Stored UTC-naive time | Included |
|---|---|
| `2026-07-13T23:59:59` | No |
| `2026-07-14T00:00:00` | Yes |
| `2026-07-14T23:59:59` | Yes |
| `2026-07-15T00:00:00` | No |

Unified Baseline window for `asOfUtc=2026-07-14T12:00:00`, 30 minutes:

| Stored UTC-naive time | Included |
|---|---|
| `2026-07-14T11:29:59` | No |
| `2026-07-14T11:30:00` | Yes |
| `2026-07-14T11:59:59` | Yes |
| `2026-07-14T12:00:00` | Yes |
| `2026-07-14T12:00:01` | No |

The five-point boundary fixture is executed for Monitor Alert, Analysis Run, low-data-quality Analysis Run, Push Recheck, Hot Reset count, and Hot Reset trigger-type distribution. Fixed-clock service tests also prove that JVM defaults `UTC`, `Asia/Shanghai`, and `America/New_York` pass the same two boundaries to every mapper.

## 7. Historical rows, safety, and remaining gates

- New Monitor Alert rows written through the reviewed service are UTC-naive and verifiable.
- Monitor Alert rows written before this change may contain database-session local time. This PR does not fabricate a conversion. Before production enablement, the operator must explicitly choose a bounded-window cleanup, migration, ignore-before cutoff, or observation policy.
- Historical Hot Reset `event_time` values supplied by older callers remain unverified even though new null-time fallback and service-generated timestamps use the UTC clock.

- No live AI or market provider was called.
- No secret was read.
- No production database or migration was accessed.
- No position, order, scheduler, external Push, Telegram, webhook, or email behavior changed.
- The broader historical time basis of `analysis_time`, Monitor Alert timestamps, and Hot Reset `event_time` is not declared fully normalized by this fix.
- PostgreSQL/Flyway V7 controlled-local execution is complete; real release
  dataset inventory, writer cutovers, backup/restore, and current-state
  rehearsal remain incomplete. See `docs/HISTORICAL_TIME_BASIS_STRATEGY.md`.
- Production readiness remains `BLOCKED`; production deployment cannot proceed.
