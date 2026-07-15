# Historical Time Basis Strategy

## Decision

- Package: `PostgreSQL Evidence Review and Historical Time Strategy P2`
- Evidence branch: `codex/postgresql-flyway-v7-evidence-p1`
- P2 starting commit: `90041938d955e66ac26d33dc577832ccc63b7ef1`
- Historical inventory mode: aggregate-only, read-only, controlled non-production databases
- Automatic timestamp shifting: **PROHIBITED**
- Production deployment readiness: **BLOCKED**

This strategy separates evidence about current UTC writers from claims about
historical rows. A timestamp distribution can identify a suspicious pattern;
it cannot prove which timezone produced an individual row.

## Read-Only Inventory

The inventory consists of:

- `scripts/historical-time-basis-inventory.sh`: guarded operator entry point.
- `scripts/historical-time-basis-inventory.sql`: PostgreSQL read-only aggregate query.

The wrapper requires an explicit database class of `RESTORE`,
`STAGING_CLONE`, `SANITIZED_REHEARSAL`, or `LOCAL_CONTROLLED`, plus
`HISTORICAL_TIME_INVENTORY_CONFIRM=I_CONFIRM_READ_ONLY_NON_PRODUCTION_DATABASE`.
It refuses production-like target indicators and database names that do not
identify a controlled environment. Missing configuration skips safely. It does
not print connection values or database errors.

The SQL starts `BEGIN TRANSACTION READ ONLY`. It contains no `INSERT`,
`UPDATE`, `DELETE`, `ALTER`, `TRUNCATE`, or `DROP` operation. It inventories:

- `tm_monitor_alert.created_at`
- `tm_monitor_alert.updated_at`
- `tm_monitor_alert.cooldown_until`
- `tm_analysis_run.analysis_time`
- `tm_analysis_run.created_at`
- `tm_analysis_run.started_at`
- `tm_analysis_run.completed_at`
- `tm_hot_reset_event.event_time`
- `tm_hot_reset_event.create_time`
- `tm_hot_reset_event.completed_at`
- `tm_decision_result.create_time`
- `tm_decision_result.valid_from`
- `tm_decision_result.expires_at`

Permitted output is limited to counts, earliest/latest timestamps, day/hour
buckets, null and future counts, time-delta buckets, whole-hour offset-pattern
counts, reference mismatch counts, and an aggregate MD5 fingerprint. It does
not emit credentials, free-text reasons, source references, position amounts,
or row-level business data.

Example controlled invocation:

```bash
HISTORICAL_TIME_INVENTORY_HOST='<controlled-host>' \
HISTORICAL_TIME_INVENTORY_PORT='<controlled-port>' \
HISTORICAL_TIME_INVENTORY_DATABASE='<staging-or-restore-database>' \
HISTORICAL_TIME_INVENTORY_USERNAME='<controlled-user>' \
HISTORICAL_TIME_INVENTORY_PASSWORD='<process-env-only>' \
HISTORICAL_TIME_INVENTORY_DATABASE_CLASS=STAGING_CLONE \
HISTORICAL_TIME_INVENTORY_CONFIRM=I_CONFIRM_READ_ONLY_NON_PRODUCTION_DATABASE \
bash scripts/historical-time-basis-inventory.sh
```

The P2 disposable empty-database run produced summaries for all 13 fields and
aggregate MD5 `638dd7271f23a7dccf810d22507f88a2`. That proves the inventory
contract executes read-only; it is not evidence about a real release dataset.

## Classification Contract

| Classification | Meaning | Minimum evidence |
| --- | --- | --- |
| `VERIFIED_UTC` | The individual value is tied to a trustworthy UTC source or independently verified reference event. | Verifiable source record and matching reference instant. |
| `POST_CUTOVER_UTC` | The row was produced after a writer-specific, approved UTC cutover. | Complete cutover record plus writer/version attribution. |
| `LEGACY_UNVERIFIED` | The row predates a proven cutover or lacks writer attribution. | Default for ambiguous legacy rows. |
| `OFFSET_PATTERN_SUSPECTED` | Aggregate evidence resembles a whole-hour offset such as `+08:00` or `-04:00`. | Statistical pattern only; never sufficient for automatic correction. |
| `REFERENCE_MISMATCH` | The observed value conflicts materially with a trusted same-analysis or same-trace reference. | Identified trusted reference and documented comparison rule. |
| `FUTURE_OUTLIER` | The value is beyond the approved future-tolerance window. | Inventory as-of instant and explicit tolerance. |
| `NO_REFERENCE` | No trustworthy event or writer metadata exists for comparison. | Absence of reference evidence. |

Classification is fail-closed. `VERIFIED_UTC` and `POST_CUTOVER_UTC` are never
inferred merely because values look plausible or cluster around an expected
hour. A row may be escalated from a suspected class only after controlled
reference validation and human review.

## Cutover Contract

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
cutover does not establish another writer's contract. In this repository the
code-level UTC writer changes are known, but no production deployment evidence
or first-row verification has been supplied. All three operational cutover
records therefore remain `MISSING_EVIDENCE`.

## Historical-Row Decision Matrix

| Classification | Default handling | Modify original value | Enter recent windows | Usable for audit |
| --- | --- | ---: | ---: | ---: |
| `VERIFIED_UTC` | Retain and use normally. | No | Yes | Yes |
| `POST_CUTOVER_UTC` | Retain and use normally under the approved cutover. | No | Yes | Yes |
| `LEGACY_UNVERIFIED` | Retain without shifting; exclude from precise instant claims. | No | No | Warning-only |
| `OFFSET_PATTERN_SUSPECTED` | Retain; require human sampling against reference events. | No | No | Warning-only |
| `REFERENCE_MISMATCH` | Isolate in a manual-review list; exclude from automated release decisions. | No | No | Yes, as discrepancy evidence |
| `FUTURE_OUTLIER` | Fail closed and raise a data-quality finding. | No | No | Yes, as anomaly evidence |
| `NO_REFERENCE` | Retain without a timezone claim. | No | No | Unverified-history only |

Recent-window inclusion may be granted later only by a separately reviewed
policy backed by row-level evidence. The inventory itself never changes that
decision.

## No Automatic Shift Migration

This package does not add `V8__shift_historical_timestamps.sql` and must not
execute bulk expressions such as `created_at = created_at - interval '8 hours'`.
Server location, JVM timezone, or an aggregate whole-hour pattern is not a safe
basis for rewriting history.

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
sanitized current-state clone, approve each writer cutover, review suspected
and mismatched samples, complete backup/restore and current-state migration
rehearsal, and obtain release-owner approval. No production database was
accessed and no historical row was modified by P2.
