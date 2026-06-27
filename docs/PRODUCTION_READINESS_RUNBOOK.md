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

## PDR-M1 PostgreSQL Runtime Pack

PDR-M1 adds a PostgreSQL runtime-readiness smoke layer. It does not deploy to production and does not connect to any real database.

- PostgreSQL JDBC driver is available as a runtime dependency.
- Testcontainers PostgreSQL and Flyway PostgreSQL smoke tests are available in test scope.
- Default Spring Boot H2 tests keep Flyway auto-configuration disabled through test-only config so `schema.sql` remains the local/default bootstrap path.
- Flyway V1/V2 migration smoke runs manually in a PostgreSQL Testcontainer when Docker is available; when Docker is unavailable, the smoke test skips gracefully.
- PostgreSQL mapper variants exist for DATEADD / FORMATDATETIME methods in AnalysisRun, PushSnapshot, HotResetEvent, PushRecheckLog, and MonitorAlert mapper paths.
- `UserPositionMapper` generated-key metadata now specifies `keyColumn = "id"`; PostgreSQL identity generated-key behavior is covered by the PostgreSQL smoke when Docker is available.
- Deferred: real production database connection, deployment packaging, auth/access control, secrets manager, observability, real server smoke, and production release-gate approval.

## Current Schema State

- `schema.sql` remains the local/test bootstrap for now.
- A Flyway skeleton exists only behind the explicit `flyway-migration` Maven profile.
- PostgreSQL baseline schema SQL files exist as PDR-2C1 draft migrations.
- PostgreSQL upsert mapper variants exist for asset state and user config.
- PostgreSQL date-function mapper variants exist for the known DATEADD / FORMATDATETIME blockers.
- PostgreSQL JDBC driver is present, but no production database is connected by PDR-M1.
- Testcontainers/Flyway smoke is test-only and does not use real secrets.

## Migration Execution Policy

Production migrations must run as an explicit pre-deploy step.

Application startup must not silently mutate the production schema without a controlled migration process.

PDR-2C1 adds schema SQL drafts only. PDR-2C2A adds mapper upsert compatibility. PDR-M1 adds PostgreSQL driver, test-only Testcontainers/Flyway smoke, date-function mapper variants, and backup/restore command templates. Production migrations are still not deployable until real environment validation and the production release gate are complete.

## Rollback Policy

Migrations are forward-only.

Rollback is performed through a verified pre-migration backup and restore procedure.

No destructive migration is allowed without backup and restore validation.

## Backup And Restore Policy

Initial targets:

- RPO: 24h.
- RTO: 4h.

PDR-2A does not implement backup or restore commands. PDR-2D must define PostgreSQL-specific backup and restore commands and validation evidence.

### PostgreSQL Backup / Restore Draft

These commands are templates. Replace environment variables through the production secrets manager / deployment environment; do not hard-code secrets.

Pre-migration backup requirement:

```bash
PGPASSWORD="$PROD_DATASOURCE_PASSWORD" pg_dump \
  --host="$PROD_DATASOURCE_HOST" \
  --port="${PROD_DATASOURCE_PORT:-5432}" \
  --username="$PROD_DATASOURCE_USERNAME" \
  --dbname="$PROD_DATASOURCE_DATABASE" \
  --format=custom \
  --file="backup/trade_model_v1_$(date +%Y%m%d_%H%M%S).dump"
```

Restore template for a controlled recovery database:

```bash
PGPASSWORD="$RESTORE_DATASOURCE_PASSWORD" pg_restore \
  --host="$RESTORE_DATASOURCE_HOST" \
  --port="${RESTORE_DATASOURCE_PORT:-5432}" \
  --username="$RESTORE_DATASOURCE_USERNAME" \
  --dbname="$RESTORE_DATASOURCE_DATABASE" \
  --clean \
  --if-exists \
  --no-owner \
  "backup/trade_model_v1_YYYYMMDD_HHMMSS.dump"
```

Plain SQL restore alternative when using `pg_dump --format=plain`:

```bash
PGPASSWORD="$RESTORE_DATASOURCE_PASSWORD" psql \
  --host="$RESTORE_DATASOURCE_HOST" \
  --port="${RESTORE_DATASOURCE_PORT:-5432}" \
  --username="$RESTORE_DATASOURCE_USERNAME" \
  --dbname="$RESTORE_DATASOURCE_DATABASE" \
  --file="backup/trade_model_v1_YYYYMMDD_HHMMSS.sql"
```

Restore smoke checklist:

- DB connection succeeds with the restored credentials.
- `flyway_schema_history` exists and reports expected successful migrations.
- `/api/dashboard/home` returns HTTP 200 in the restored environment.
- `/api/review/center` returns HTTP 200 in the restored environment.
- RPO 24h / RTO 4h target evidence is recorded for the restore drill.

## Remaining Blockers

- Flyway remains non-default for runtime startup; test-only Flyway smoke exists.
- PostgreSQL baseline schema SQL has a Testcontainers smoke path, but this local environment may skip it when Docker is unavailable.
- Mapper PostgreSQL compatibility covers known upsert and DATEADD / FORMATDATETIME blockers; live PostgreSQL mapper execution remains to be expanded beyond smoke/static guards.
- Backup/restore command templates exist; real restore drill evidence is still missing.
- Auth/access control missing.
- Observability missing.
- Deployment packaging missing.
- Secrets contract incomplete.

## Next Packages

1. PDR-M2 Production Secrets / Deployment Config Contract.
2. PDR-M3 Auth / Access Control Gate.
3. PDR-M4 Observability + Real Server Deployment Smoke / Restore Drill.

## Explicit Non-Scope

PDR-M1 does not add a real PostgreSQL connection, schema.sql changes, production application config changes, deployment files, secrets, auth, Telegram send, Push send, order/execution, or auto-trading semantics. It adds runtime PostgreSQL driver support, test-only PostgreSQL smoke coverage, mapper PostgreSQL variants, and backup/restore templates while preserving default H2 mapper behavior.
