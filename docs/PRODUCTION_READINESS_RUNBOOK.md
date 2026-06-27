# Production Readiness Runbook

This runbook records production-readiness decisions and remaining gates. It is not a deployment approval.

## Status

Production Deployment Readiness is `BLOCKED`.

V1 is locally acceptance-ready. Production deployment remains blocked until database migration, rollback, auth, secrets, observability, deployment packaging, smoke testing, and an explicit human production release gate are complete.

## PDR-2A Database Migration + Rollback Decision

PDR-2A records decisions only. It does not change runtime behavior.

- Production database target: PostgreSQL.
- Migration framework target: Flyway, SQL-first.
- Migration execution model: manual pre-deploy migration.
- Rollback policy: forward-only migrations plus pre-migration backup and restore.
- Initial recovery target: RPO 24h and RTO 4h.
- Current local/test bootstrap: `src/main/resources/schema.sql`.

## PDR-2B Flyway Baseline Skeleton

PDR-2B adds a non-default Flyway project skeleton only. It does not create an executable migration and does not change local/test runtime behavior.

- Flyway dependency scope: isolated behind the explicit Maven profile `flyway-migration`.
- Migration directory: `src/main/resources/db/migration/` exists with a README placeholder only.
- Executable migration files: none; do not add `V*.sql` until PDR-2C.
- Current local/test bootstrap: `src/main/resources/schema.sql` remains unchanged.
- Production database target: PostgreSQL.
- Real PostgreSQL-compatible baseline migration: deferred to PDR-2C.

## Current Schema State

- `schema.sql` remains the local/test bootstrap for now.
- A Flyway skeleton exists only behind the explicit `flyway-migration` Maven profile.
- No executable production migration exists yet.
- No production database is connected by PDR-2B.
- No PostgreSQL driver/config/migration is added by PDR-2B.

## Migration Execution Policy

Production migrations must run as an explicit pre-deploy step.

Application startup must not silently mutate the production schema without a controlled migration process.

PDR-2B defines the safe boundary by keeping Flyway out of the default classpath and adding no executable migration. PDR-2C must create the PostgreSQL-compatible baseline migration before any migration implementation is treated as deployable.

## Rollback Policy

Migrations are forward-only.

Rollback is performed through a verified pre-migration backup and restore procedure.

No destructive migration is allowed without backup and restore validation.

## Backup And Restore Policy

Initial targets:

- RPO: 24h.
- RTO: 4h.

PDR-2A does not implement backup or restore commands. PDR-2D must define PostgreSQL-specific backup and restore commands and validation evidence.

## Remaining Blockers

- Flyway dependency is available only through the non-default `flyway-migration` Maven profile.
- Flyway baseline migration not created.
- PostgreSQL compatibility not verified.
- Backup/restore scripts or commands not implemented.
- Auth/access control missing.
- Observability missing.
- Deployment packaging missing.
- Secrets contract incomplete.

## Next Packages

1. PDR-2C PostgreSQL Compatibility Pass.
2. PDR-2D Backup/Restore Runbook.

## Explicit Non-Scope

PDR-2B does not add executable migration SQL, PostgreSQL connection, schema changes, mapper SQL changes, runtime config changes, deployment files, secrets, auth, Telegram send, Push send, order/execution, or auto-trading semantics.
