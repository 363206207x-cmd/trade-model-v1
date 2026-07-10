# Flyway PostgreSQL Migrations

This directory contains the current PostgreSQL-target Flyway migration drafts/evidence path.

Current migration files:

- `V1__baseline_schema_tables.sql`: PostgreSQL baseline table creation.
- `V2__baseline_schema_indexes.sql`: PostgreSQL baseline index creation.
- `V3__scheme_rule_config_defaults.sql`: PostgreSQL rule-config default upserts.
- `V4__ohlcv_ingestion_provenance.sql`: additive OHLCV fetch/source/freshness/provenance/run audit columns and index.
- `V5__provider_scan_profile_orchestration.sql`: additive user scan-profile settings and versioned provider-scan threshold defaults.

`src/main/resources/schema.sql` remains the H2 local/test bootstrap path. It is intentionally separate from the PostgreSQL Flyway migration path.

Production target database: PostgreSQL.
Migration framework target: Flyway, SQL-first.
Migration execution model: explicit manual pre-deploy migration.
Rollback policy: forward-only migrations plus verified pre-migration backup and restore.

PDR-PF3 status: empty PostgreSQL migration evidence is `BLOCKED_TIMEOUT` after an approximately 1h27m manually interrupted Docker/Testcontainers/PostgreSQL evidence run. Production Deployment Readiness remains `BLOCKED` until Docker-backed or server-backed migration evidence, current-state migration evidence, backup evidence, and restore evidence are complete.


PDR-PF4 status: current-state migration and rollback drill requirements are documented in `docs/CURRENT_STATE_MIGRATION_ROLLBACK_DRILL.md`. The drill documentation does not execute migrations, backups, restores, destructive DB operations, or production DB access.


PDR-LIVE3 status: controlled external PostgreSQL Flyway smoke runner is available through `scripts/controlled-postgresql-flyway-smoke.sh` and `ControlledPostgreSqlFlywaySmokeTest`. It skips when controlled DB env is missing, requires explicit non-production and run confirmations, redacts connection values, and does not claim PASS until a disposable controlled PostgreSQL database is supplied.

CALL-1B extends `PostgreSqlFlywayMigrationSmokeTest` to verify clean V1-V5 migration, V5 columns/defaults, profile save/load, audit insertion, timestamp handling, rollback atomicity, and mapper-compatible reads. A run skipped because Docker/Testcontainers is unavailable is recorded as `SKIPPED_DOCKER_UNAVAILABLE`, not PASS. Production readiness remains `BLOCKED` independently of this bounded test.
