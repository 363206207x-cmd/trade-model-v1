# Controlled Backup Restore Evidence Run

Package: PDR-LIVE6 Controlled Backup Restore Evidence Run
Branch: `codex/pdr-live6-controlled-backup-restore-evidence-run`
Current main commit: `dd3ab7bbab22d27d2d4854b1e855bd5be303deb2`
Evidence date: 2026-07-08

## Scope

This document records operator-provided controlled backup and restore evidence from disposable local PostgreSQL containers.

This is not production deployment, not production DB access, not a destructive production operation, and not a production-ready claim.

## Relation To Previous Evidence

PDR-LIVE4 proved the empty controlled PostgreSQL Flyway migration gate:

- PostgreSQL version: `16.14`
- Flyway validated 3 migrations
- Applied V1 baseline schema tables
- Applied V2 baseline schema indexes
- Applied V3 scheme rule config defaults
- Final schema version: `v3`
- `CONTROLLED_POSTGRESQL_FLYWAY_RESULT: PASS`

PDR-LIVE5 prepared the controlled current-state migration + restore drill path but recorded skipped local evidence because controlled source/recovery DB env and PostgreSQL client tools were unavailable in that execution environment.

PDR-LIVE6 records the operator's later local disposable PostgreSQL backup/restore evidence after local `pg_dump` / `pg_restore` validation.

## Controlled Databases

| Role | Controlled endpoint | Database | Scope |
|---|---|---|---|
| Source DB | `localhost:55432` | `trade_model_smoke` | Disposable local PostgreSQL container |
| Restore DB | `localhost:55433` | `trade_model_restore` | Disposable local PostgreSQL container |

No production DB was accessed. No secrets are recorded in this document.

## Backup Command Summary

Operator command, with no password or secret material recorded:

```bash
pg_dump -h localhost -p 55432 -U trade_model_smoke -d trade_model_smoke -Fc -f /tmp/trade-model-db-evidence/trade_model_smoke.dump
```

Backup result: PASS.

## Restore Command Summary

The operator restored the custom-format backup into the disposable restore DB.

Restore result: PASS_WITH_WARNING.

Recorded warning/error:

```text
error: unrecognized configuration parameter "transaction_timeout"
warning: errors ignored on restore: 1
```

Interpretation: restore completed with a PostgreSQL configuration compatibility warning. The warning must be remediated or explicitly accepted by a controlled restore policy before this evidence can contribute to production deployment readiness.

## Validation Query Summaries

Restored `tm_*` table count query:

```sql
select count(*)
from information_schema.tables
where table_schema='public'
  and table_name like 'tm_%';
```

Result: `27`.

Restored Flyway success count query:

```sql
select count(*)
from flyway_schema_history
where success = true;
```

Result: `3`.

Restore validation result: PASS.

## Evidence Summary

| Gate | Result | Evidence |
|---|---|---|
| Backup | `PASS` | `pg_dump` custom-format backup completed from disposable source DB. |
| Restore | `PASS_WITH_WARNING` | `pg_restore` completed into disposable restore DB with `transaction_timeout` compatibility warning. |
| Restore validation | `PASS` | Restored DB has 27 `tm_*` tables and 3 successful Flyway migrations. |

## Safety Confirmation

- No production DB was accessed.
- No destructive operation outside disposable controlled DB was run.
- No secrets were printed or committed.
- No `.env` or credential material was committed.
- No Java business logic changed.
- No `schema.sql` changed.
- No Flyway SQL changed.
- No `application.yml` or `application-prod.yml` changed.
- No runtime trading behavior changed.
- No auto-open, auto-close, auto-reverse, order execution, auto-trading, external push send, fake positions, or fake review records were added.

## Production Readiness Decision

Production readiness remains BLOCKED.

Production deployment cannot proceed.

Reason: this package proves controlled local disposable backup and restore validation, but restore completed with a `transaction_timeout` warning and the evidence is still local/disposable only. Production deployment still requires warning remediation or accepted policy, controlled current-state migration/rollback evidence, provider live smoke, secrets/access hardening evidence, real server smoke, and release owner approval.

## Remaining Blockers

1. `transaction_timeout` restore warning needs remediation or explicit accepted policy.
2. Evidence was collected against disposable local containers only, not a production-like current-state environment.
3. Current-state migration and rollback drill evidence remains incomplete.
4. Provider live smoke evidence remains incomplete for the production release gate.
5. Secrets manager, credential rotation, HTTPS/reverse-proxy, access logging, audit logging, and rate limiting evidence remains incomplete.
6. Real server deployment smoke evidence remains incomplete.
7. Release owner approval and complete production release evidence bundle are missing.

## Next Recommendation

Next recommended package: `PDR-LIVE7 Restore Warning Policy + Current-State Drill Closure`.

That package should either remediate the `transaction_timeout` warning or record an explicit acceptance policy for the warning in disposable controlled restore evidence, then continue toward controlled current-state migration and rollback evidence. Production deployment must remain blocked unless every release gate has PASS evidence and release-owner approval.
