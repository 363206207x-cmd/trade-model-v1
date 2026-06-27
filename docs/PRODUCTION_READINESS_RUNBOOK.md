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

## PDR-2C1 PostgreSQL Baseline Schema SQL

PDR-2C1 adds PostgreSQL-compatible baseline schema SQL files only. It does not validate the files against a live PostgreSQL database and does not change default local/test behavior.

- Table migration: `src/main/resources/db/migration/V1__baseline_schema_tables.sql`.
- Index migration: `src/main/resources/db/migration/V2__baseline_schema_indexes.sql`.
- Scope: current V1 tables and indexes only; no seed data and no foreign keys beyond current V1 semantics.
- Type strategy: generated identity columns, `TEXT` for JSON-like text fields, `TIMESTAMP WITHOUT TIME ZONE` for current `LocalDateTime` semantics, and PostgreSQL-compatible floating-point types.
- Deferred: mapper PostgreSQL compatibility, PostgreSQL driver, Testcontainers/Flyway smoke validation, real database connection, and backup/restore validation.

## PDR-2C2A Mapper PostgreSQL Upsert Variants

PDR-2C2A adds mapper-level PostgreSQL upsert variants only. It does not change default H2 behavior and does not validate against a live PostgreSQL database.

- MyBatis database-id detection: `DatabaseIdProvider` maps PostgreSQL to `postgresql`, H2 to `h2`, and MySQL to `mysql`.
- Asset state upsert: `AssetStateMapper.mergeUpsertCore` keeps the generic H2 `MERGE INTO ... KEY` fallback and adds a PostgreSQL `ON CONFLICT (symbol) DO UPDATE` variant.
- User config upsert: `UserConfigMapper.saveOrUpdate` keeps the generic MySQL/H2 `ON DUPLICATE KEY UPDATE` fallback and adds a PostgreSQL `ON CONFLICT (user_id) DO UPDATE` variant.
- Deferred: DATEADD / FORMATDATETIME mapper compatibility, PostgreSQL driver, Testcontainers/Flyway smoke validation, real database connection, and backup/restore validation.

## Current Schema State

- `schema.sql` remains the local/test bootstrap for now.
- A Flyway skeleton exists only behind the explicit `flyway-migration` Maven profile.
- PostgreSQL baseline schema SQL files exist as PDR-2C1 draft migrations.
- PostgreSQL upsert mapper variants exist for asset state and user config.
- No production database is connected by PDR-2C2A.
- No PostgreSQL driver/runtime connection is added by PDR-2C2A.

## Migration Execution Policy

Production migrations must run as an explicit pre-deploy step.

Application startup must not silently mutate the production schema without a controlled migration process.

PDR-2C1 adds schema SQL drafts only. PDR-2C2A adds the first mapper compatibility slice for upsert SQL only. PDR-2C2B/PDR-2C3 must complete date-function compatibility and PostgreSQL validation before any migration implementation is treated as deployable.

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
- PostgreSQL baseline schema SQL exists but is not validated against PostgreSQL yet.
- Mapper PostgreSQL compatibility is partial: upsert variants are present, DATEADD / FORMATDATETIME compatibility is still pending.
- PostgreSQL driver/Testcontainers validation not added.
- Backup/restore scripts or commands not implemented.
- Auth/access control missing.
- Observability missing.
- Deployment packaging missing.
- Secrets contract incomplete.

## Next Packages

1. PDR-2C2B Mapper PostgreSQL Date Function Compatibility.
2. PDR-2C3 PostgreSQL Driver + Migration Smoke Validation.
3. PDR-2D Backup/Restore Runbook.

## Explicit Non-Scope

PDR-2C2A does not add PostgreSQL connection, schema.sql changes, Flyway migration changes, runtime application config changes, deployment files, secrets, auth, Telegram send, Push send, order/execution, or auto-trading semantics. It only adds MyBatis database-id detection and PostgreSQL upsert annotation variants while preserving default H2 mapper behavior.
