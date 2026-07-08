# Controlled PostgreSQL 16 Clean Restore Evidence

Package: PDR-LIVE7 PostgreSQL 16-aligned Clean Restore Evidence
Branch: `codex/pdr-live7-postgresql16-clean-restore-evidence`
Current main commit: `bc0cb91a708146c4e5d29238893821f335f4a042`
Evidence date: 2026-07-08

## Scope

This document records operator-provided controlled backup/restore clean evidence from disposable local PostgreSQL containers using PostgreSQL 16 container-native tools.

This is not production deployment, not production DB access, not a destructive production operation, and not a production-ready claim.

## Relation To LIVE6 Warning

PDR-LIVE6 recorded controlled backup/restore evidence with:

- Backup: `PASS`
- Restore: `PASS_WITH_WARNING`
- Warning: `transaction_timeout`
- Restored `tm_*` table count: `27`
- Flyway success count: `3`

The warning was caused by PostgreSQL 18 client tools restoring into a PostgreSQL 16 server.

PDR-LIVE7 reran the backup/restore path with PostgreSQL 16 container-native tools so the client/server major versions are aligned.

## Controlled Environment

| Role | Container | Database | Scope |
|---|---|---|---|
| Source DB | `trade-model-pg-smoke` | `trade_model_smoke` | Disposable local PostgreSQL |
| Restore DB | `trade-model-pg-restore` | `trade_model_restore` | Disposable local PostgreSQL |
| Backup tool | `postgres:16-alpine` container-native `pg_dump` | n/a | Version-aligned PostgreSQL 16 client tool |
| Restore tool | `postgres:16-alpine` container-native `pg_restore` | n/a | Version-aligned PostgreSQL 16 client tool |

No production DB was accessed. No secrets are recorded in this document.

## Results

| Gate | Result | Evidence |
|---|---|---|
| Backup | `PASS` | PostgreSQL 16 container-native `pg_dump` completed from disposable source DB. |
| Restore | `PASS_CLEAN` | PostgreSQL 16 container-native `pg_restore` completed without the prior `transaction_timeout` warning/error. |
| Restore validation | `PASS` | Restored DB has 27 `tm_*` tables and 3 successful Flyway migrations. |

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

Restore validation result: `PASS`.

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

Reason: this package proves clean PostgreSQL 16-aligned backup/restore validation in disposable local containers only. Production deployment still requires production-like current-state migration/rollback evidence, provider live smoke, secrets/access hardening evidence, real server smoke, and release owner approval.

## Remaining Blockers

1. Evidence was collected against disposable local containers only, not a production-like current-state environment.
2. Current-state migration and rollback drill evidence remains incomplete for a production-like database.
3. Provider live smoke evidence remains incomplete for the production release gate.
4. Secrets manager, credential rotation, HTTPS/reverse-proxy, access logging, audit logging, and rate limiting evidence remains incomplete.
5. Real server deployment smoke evidence remains incomplete.
6. Release owner approval and complete production release evidence bundle are missing.

## Next Recommendation

Next recommended package: `PDR-LIVE8 Production-like Current-State Migration Rollback Evidence`.

That package should run a controlled current-state migration and rollback drill in a production-like non-production environment, with redacted evidence and no production deployment claim. Production deployment must remain blocked unless every release gate has PASS evidence and release-owner approval.
