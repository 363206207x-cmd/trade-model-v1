# Historical Time Basis Strategy

## Decision

- Package: `Historical Time Inventory Field Semantics P2.1`
- Evidence branch: `codex/postgresql-flyway-v7-evidence-p1`
- P2.1 starting commit: `628c5554f57b23a32ec99fa7fa18c392037bbd3b`
- Historical inventory mode: aggregate-only, read-only, controlled non-production databases
- Automatic timestamp shifting: **PROHIBITED**
- Production deployment readiness: **BLOCKED**

This strategy separates field semantics, candidate signals, trusted-reference
classification, and writer cutover proof. A timestamp distribution can identify
a candidate pattern; it cannot prove which timezone produced an individual row.

## Guarded Read-Only Inventory

The inventory consists of:

- `scripts/historical-time-basis-inventory.sh`: guarded operator entry point.
- `scripts/historical-time-basis-inventory.sql`: PostgreSQL read-only aggregate query.

The wrapper requires an explicit database class of `RESTORE`,
`STAGING_CLONE`, `SANITIZED_REHEARSAL`, or `LOCAL_CONTROLLED`, plus
`HISTORICAL_TIME_INVENTORY_CONFIRM=I_CONFIRM_READ_ONLY_NON_PRODUCTION_DATABASE`.
It refuses production-like target indicators and database names that do not
identify a controlled environment. Missing configuration skips safely. It does
not print connection values or database errors.

The wrapper applies these PostgreSQL session limits with no automatic retry:

```text
default_transaction_read_only=on
statement_timeout=120000
lock_timeout=5000
idle_in_transaction_session_timeout=60000
```

A recognized statement, lock, or idle-transaction timeout returns
`BLOCKED_INVENTORY_TIMEOUT`; all other database failures return only
`FAIL_REDACTED`. The SQL retains `BEGIN TRANSACTION READ ONLY` and contains no
`INSERT`, `UPDATE`, `DELETE`, `ALTER`, `TRUNCATE`, or `DROP` operation.

## Field-Level Semantics

The SQL contains an explicit catalog for every inventoried field. It reports
`NOT_APPLICABLE` when a future or reference check is not semantically valid;
zero is reserved for an applicable check that actually ran and found no rows.

| Field | Semantic type | Future check | Relation/reference contract | Offset candidate |
| --- | --- | --- | --- | ---: |
| `tm_monitor_alert.created_at` | `EVENT_INSTANT` | as-of + 5m | none | no |
| `tm_monitor_alert.updated_at` | `AUDIT_UPDATE_TIME` | as-of + 5m | `updated_at >= created_at`; no maximum delta assumed | no |
| `tm_monitor_alert.cooldown_until` | `SCHEDULED_DEADLINE` | `NOT_APPLICABLE` | `cooldown_until >= created_at`; duration distribution; no guessed maximum | no |
| `tm_analysis_run.analysis_time` | `CANONICAL_ANALYSIS_TIME` | `NOT_APPLICABLE` | distribution relative to `created_at`; no timezone classification | no |
| `tm_analysis_run.created_at` | `EVENT_INSTANT` | as-of + 5m | candidate comparison with near-simultaneous `started_at` | yes, candidate only |
| `tm_analysis_run.updated_at` | `AUDIT_UPDATE_TIME` | as-of + 5m | `updated_at >= created_at`; no maximum delta assumed | no |
| `tm_analysis_run.started_at` | `EVENT_INSTANT` | `NOT_APPLICABLE` | `started_at <= completed_at` when completed | no |
| `tm_analysis_run.completed_at` | `LIFECYCLE_COMPLETION_TIME` | `NOT_APPLICABLE` | `completed_at >= started_at`; null allowed | no |
| `tm_hot_reset_event.event_time` | `EVENT_INSTANT` | as-of + 5m | may precede `create_time`; distribution only | no |
| `tm_hot_reset_event.create_time` | `EVENT_INSTANT` | as-of + 5m | none | no |
| `tm_hot_reset_event.completed_at` | `LIFECYCLE_COMPLETION_TIME` | `NOT_APPLICABLE` | `completed_at >= create_time`; processing-delay distribution | no |
| `tm_decision_result.create_time` | `EVENT_INSTANT` | as-of + 5m | none | no |
| `tm_decision_result.valid_from` | `VALIDITY_START` | `NOT_APPLICABLE` | paired interval, `valid_from <= expires_at`; future allowed | no |
| `tm_decision_result.expires_at` | `VALIDITY_END` | `NOT_APPLICABLE` | paired interval, `expires_at >= valid_from`; future allowed | no |

The catalog records `field_name`, `semantic_type`, `future_check_mode`,
`relation_check_mode`, `reference_field`, `expected_ordering`,
`tolerance_contract`, and `offset_pattern_applicable` in every `FIELD_POLICY`
line. `analysis_time` is a canonical business time, not an alias for
`created_at`. `event_time` may legitimately precede processing. Scheduled
deadlines, validity endpoints, and lifecycle completion timestamps are not
eligible for generic whole-hour offset detection.

The only currently catalog-approved offset candidate relation is
`tm_analysis_run.created_at` relative to the writer's near-simultaneous
`started_at`. Even there, the SQL emits only `OFFSET_PATTERN_CANDIDATE` counts.
Historical rows still require a trustworthy reference and writer attribution
before any row-level conclusion.

## Aggregate Output Contract

Permitted SQL output is limited to:

- field policy and aggregate field summaries;
- earliest/latest and day/hour aggregate buckets;
- future-event candidate counts for applicable event/audit fields;
- ordering anomaly counts;
- duration summaries and buckets with no invented extreme-duration threshold;
- `NOT_ACTIVE`, `ACTIVE`, and `EXPIRED` validity-state counts;
- both-null and partial-null validity counts;
- catalog-approved `OFFSET_PATTERN_CANDIDATE` counts;
- an aggregate MD5 fingerprint.

The SQL does not emit credentials, identifiers, free-text reasons, source
references, position amounts, or row-level business data. It also does not emit
or infer `VERIFIED_UTC`, `POST_CUTOVER_UTC`, or `REFERENCE_MISMATCH`.

## Empty and Nonempty Evidence Boundaries

The P2.1 disposable empty-database run produced policy and summary lines for
all 14 fields and aggregate MD5 `7d76b5be314314117f5dbf118de3ad0c`.
That empty run proves only:

1. the SQL executes;
2. the transaction is read-only;
3. the aggregate output shape is complete;
4. no row-level business data is exposed.

It does **not** prove that classification rules are correct, that false
positives are impossible, or that a release dataset is ready for automated
classification.

`ControlledPostgreSqlHistoricalTimeInventorySemanticsTest` separately inserts
nonempty fixtures into disposable PostgreSQL 16 and executes the exact query
between the `INVENTORY_QUERY_BEGIN` and `INVENTORY_QUERY_END` markers. Under
`UTC`, `Asia/Shanghai`, and `America/New_York` sessions it proved identical
aggregates for:

- normal `created_at + 15m` cooldown: zero false positives, 900-second duration;
- future `valid_from` and a 24-hour validity interval: zero future/mismatch false positives;
- normal validity states: one each of `NOT_ACTIVE`, `ACTIVE`, and `EXPIRED`;
- `created_at = asOf + 6m`: one `EVENT_FUTURE_OUTLIER` candidate;
- one each of `AUDIT_ORDER_INVALID`, `SCHEDULE_ORDER_INVALID`,
  `VALIDITY_ORDER_INVALID`, and `VALIDITY_PARTIAL_NULL`;
- one catalog-approved `+8h` `OFFSET_PATTERN_CANDIDATE`.

These controlled fixtures prove the implemented aggregate semantics for the
tested cases. They do not characterize a real release dataset.

## Three-Layer Classification Contract

| Evidence layer | Permitted result | What it cannot prove |
| --- | --- | --- |
| Inventory candidate signal | Candidate counts, ordering anomalies, duration distributions, future-event candidates, validity states | Row identity, original writer timezone, verified mismatch, or cutover status |
| Trusted-reference validation | Row-level `REFERENCE_MISMATCH` or verified reference agreement under a separately documented comparison rule | Writer cutover or all historical rows |
| Cutover-proven classification | `VERIFIED_UTC` / `POST_CUTOVER_UTC` after writer/version/deployment/first-row/operator evidence | Unattributed pre-cutover rows |

Candidate signals remain fail-closed. A future-event or offset candidate is not
an instruction to rewrite, quarantine, or exclude a row automatically.
`REFERENCE_MISMATCH` requires a trusted same-event reference and a documented
comparison rule outside the aggregate SQL. `VERIFIED_UTC` and
`POST_CUTOVER_UTC` require writer attribution or approved cutover evidence.

## Writer Cutover Contract

Cutover is writer-specific. The register must track these independently:

- `monitorAlertUtcWriterCutover`
- `hotResetUtcWriterCutover`
- `analysisTimeContractCutover`

Each approved cutover record must include:

1. deployed version or merge commit;
2. actual deployment time as an instant;
3. application startup-log evidence;
4. relevant migration or service restart time;
5. the first new row whose UTC writer and reference instant are verifiable;
6. operator identity and explicit confirmation.

The first timestamp that merely appears UTC is not a cutover. One writer's
cutover does not establish another writer's contract. The repository contains
code-level UTC writer changes, but no production deployment or first-row
verification. All operational cutover records remain `MISSING_EVIDENCE`.

## Historical-Row Decision Matrix

| Evidence state | Default handling | Modify original value | Enter precise recent windows | Audit use |
| --- | --- | ---: | ---: | ---: |
| Trusted-reference verified | Retain under the approved reference rule. | No | Policy-dependent | Yes |
| Cutover-proven UTC | Retain under the approved writer cutover. | No | Yes | Yes |
| Legacy/unattributed | Retain without shifting; no exact timezone claim. | No | No | Warning-only |
| Offset/future candidate | Retain for bounded human review. | No | No | Candidate evidence only |
| Ordering anomaly | Retain for bounded human review. | No | No | Anomaly evidence only |
| No trustworthy reference | Retain without a timezone claim. | No | No | Unverified-history only |

Recent-window inclusion or remediation may be approved only by a separately
reviewed policy backed by row-level evidence. The inventory never changes data
or grants that approval.

## No Automatic Shift Migration

This package does not add `V8__shift_historical_timestamps.sql` and must not
execute bulk expressions such as
`created_at = created_at - interval '8 hours'`. Server location, JVM timezone,
or an aggregate whole-hour pattern is not a safe basis for rewriting history.

Any future historical correction requires a separate PR and all of:

- completed backup;
- successful restore drill;
- dry-run report;
- exact affected-row counts;
- reversible row mapping;
- human approval;
- bounded batches;
- durable audit log.

Until those gates pass, original values remain unchanged and production
readiness remains **BLOCKED**.

## Remaining Evidence

Before a release decision, run the inventory against a sanctioned restore or
sanitized current-state clone, approve each writer cutover, review candidate
samples against trustworthy references, complete backup/restore and
current-state migration rehearsal, and obtain release-owner approval. No
production database was accessed and no historical row was modified by P2.1.
