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

## Current Schema State

- `schema.sql` remains the local/test bootstrap for now.
- No production migration framework exists yet.
- No production database is connected by PDR-2A.
- No PostgreSQL driver/config/migration is added by PDR-2A.

## Migration Execution Policy

Production migrations must run as an explicit pre-deploy step.

Application startup must not silently mutate the production schema without a controlled migration process.

PDR-2B must define the exact Flyway command and safe execution boundary before any migration implementation is treated as deployable.

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

- Flyway dependency not added.
- Flyway baseline migration not created.
- PostgreSQL compatibility not verified.
- Backup/restore scripts or commands not implemented.
- Auth/access control missing.
- Observability missing.
- Deployment packaging missing.
- Secrets contract incomplete.

## Next Packages

1. PDR-2B Flyway Baseline Skeleton.
2. PDR-2C PostgreSQL Compatibility Pass.
3. PDR-2D Backup/Restore Runbook.

## Explicit Non-Scope

PDR-2A does not add Flyway, migration SQL, PostgreSQL connection, schema changes, mapper SQL changes, runtime config changes, deployment files, secrets, auth, Telegram send, Push send, order/execution, or auto-trading semantics.
