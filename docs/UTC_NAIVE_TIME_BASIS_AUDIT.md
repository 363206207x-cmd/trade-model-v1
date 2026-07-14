# UTC-Naive Time Basis Audit

## 1. Scope and decision

This audit closes the two Reviewer Round 8 findings where an application-written UTC wall-clock value was stored in a timestamp-without-time-zone column and later compared with the database session clock.

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
| `tm_analysis_run.analysis_time` | `AnalysisTimePolicy` and analysis-run orchestration | UTC-naive for generated/offset-aware inputs; legacy no-offset input is also accepted | Run Baseline still contains database-clock window SQL | Potentially yes | Follow-up contract required; not silently relabeled as fixed |
| `tm_hot_reset_event.event_time` | Hot Reset command/event persistence | Caller-provided no-offset time; assembler-triggered events use UTC-naive | Run Baseline still contains database-clock window SQL | Input basis is not uniform | Follow-up contract required before changing query semantics |
| `tm_monitor_alert.created_at` | Database default for monitor-alert inserts | Database-session time | Database-session time windows | No proven UTC-naive mix in reviewed writer | No change |
| `biz_date` fields | Missed-opportunity business records | Business date, not an instant | Explicit `biz_date` equality | Not applicable | Kept separate from UTC instant policy |

The limited audit searched mapper SQL for `CURRENT_DATE`, `CURRENT_TIMESTAMP`, `LOCALTIMESTAMP`, `NOW()`, `DATEADD`, `INTERVAL`, and date casts, and searched writers for `UtcLocalTimePolicy.now`, `utcLocalNow`, and UTC `LocalDateTime.ofInstant` conversions.

Only the two confirmed Reviewer findings are changed in this package. Ambiguous historical contracts are recorded rather than guessed. They remain part of production-readiness blocking evidence.

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

The contract is `today decisions = decisions in the current UTC calendar day`. It does not use the JVM default timezone or database current date. The existing Dashboard label is unchanged pending product confirmation; the internal KPI basis is explicitly UTC.

## 4. Push Recheck window contract

`RunBaselineServiceImpl` obtains one UTC-naive as-of value from its injected clock and uses it for every status count:

```text
asOfUtc = UtcLocalTimePolicy.now(clock)
windowStartUtc = asOfUtc - windowMinutes
```

`PushRecheckLogMapper.countByStatusInWindow` applies:

```sql
create_time >= :windowStartInclusive
AND create_time <= :asOfInclusive
```

The upper bound excludes future records. The mapper has one portable SQL definition and no H2/PostgreSQL database-clock variants.

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

Recheck window for `asOfUtc=2026-07-14T12:00:00`, 30 minutes:

| Stored UTC-naive time | Included |
|---|---|
| `2026-07-14T11:29:59` | No |
| `2026-07-14T11:30:00` | Yes |
| `2026-07-14T11:59:59` | Yes |
| `2026-07-14T12:00:00` | Yes |
| `2026-07-14T12:00:01` | No |

## 6. Safety and remaining gates

- No live AI or market provider was called.
- No secret was read.
- No production database or migration was accessed.
- No position, order, scheduler, external Push, Telegram, webhook, or email behavior changed.
- PostgreSQL/Flyway V7 controlled execution is still not complete.
- Production readiness remains `BLOCKED`; production deployment cannot proceed.
