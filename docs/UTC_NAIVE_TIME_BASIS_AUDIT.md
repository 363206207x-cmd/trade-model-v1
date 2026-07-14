# UTC-Naive Time Basis Audit

## 1. Scope and decision

This audit records the Reviewer Round 8 timestamp fixes and the Reviewer Round 10 closure for Dashboard UTC-day metrics and Run Baseline read windows. Round 10 removes database-clock calculations from the Baseline alert, analysis-run, Push Recheck, and Hot Reset queries without relabeling every historical writer as UTC-proven.

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
| `tm_hot_reset_event.event_time` | Hot Reset command/event persistence | Caller-provided no-offset time; assembler-triggered events use UTC-naive | Explicit Baseline UTC window `[windowStartInclusive, asOfInclusive]` | Historical input basis is not uniformly proven | Baseline read fixed in Round 10; broader writer audit remains |
| `tm_monitor_alert.created_at` | Database default for monitor-alert inserts | Database-session time | Explicit Baseline UTC window `[windowStartInclusive, asOfInclusive]` | No proven UTC-naive mix in reviewed writer | Baseline read fixed; write-side throttle queries unchanged |
| `tm_missed_opportunity.biz_date` | Missed-opportunity business records | Business date, not an instant | Dashboard passes the same UTC calendar date used by decision-today metrics | Not applicable | Dashboard UTC-day contract fixed in Round 10; column type unchanged |

The limited audit searched mapper SQL for `CURRENT_DATE`, `CURRENT_TIMESTAMP`, `LOCALTIMESTAMP`, `NOW()`, `DATEADD`, `INTERVAL`, and date casts, and searched writers for `UtcLocalTimePolicy.now`, `utcLocalNow`, and UTC `LocalDateTime.ofInstant` conversions.

Round 10 changes only Dashboard today-count selection and Run Baseline read queries. `MonitorAlertMapper.countOpenInThrottleWindow` and `countAnyInSemanticWindow` remain write-side throttle contracts and were intentionally not changed. Ambiguous historical writer contracts are recorded rather than guessed and remain part of production-readiness blocking evidence.

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

The upper bound excludes future records. These Baseline methods each have one portable SQL definition and no `CURRENT_TIMESTAMP`, `CURRENT_DATE`, H2 `DATEADD`, or PostgreSQL interval calculation. Push Recheck retains its existing explicit range contract. Monitor Alert write-side throttle methods retain their existing clock contract because they are outside this bounded read-side package.

## 5. Boundary evidence

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

## 6. Safety and remaining gates

- No live AI or market provider was called.
- No secret was read.
- No production database or migration was accessed.
- No position, order, scheduler, external Push, Telegram, webhook, or email behavior changed.
- The broader historical time basis of `analysis_time` and `event_time` is not declared fully normalized by this read-side fix.
- PostgreSQL/Flyway V7 controlled execution is still not complete.
- Production readiness remains `BLOCKED`; production deployment cannot proceed.
