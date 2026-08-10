# Flyway PostgreSQL Migrations

This directory contains the current PostgreSQL-target Flyway migration drafts/evidence path.

Current migration files:

- `V1__baseline_schema_tables.sql`: PostgreSQL baseline table creation.
- `V2__baseline_schema_indexes.sql`: PostgreSQL baseline index creation.
- `V3__scheme_rule_config_defaults.sql`: PostgreSQL rule-config default upserts.
- `V4__ohlcv_ingestion_provenance.sql`: additive OHLCV fetch/source/freshness/provenance/run audit columns and index.
- `V5__provider_scan_profile_orchestration.sql`: additive user scan-profile settings and versioned provider-scan threshold defaults.
- `V6__derivatives_business_rule_defaults.sql`: versioned derivatives evidence, risk, opportunity, and hot-reset rule defaults.
- `V7__decision_plan_offset_times.sql`: additive offset-aware `valid_from` / `expires_at` authority for execution-plan validity; historical rows remain null and fail closed until re-analysis.
- `V8__personal_user_session_authentication.sql`: personal `tm_user` identity storage for BCrypt-backed form login and server-side Session authentication; runtime bootstrap requires explicit credentials.
- `V9__user_position_ownership_foundation.sql`: nullable canonical `tm_user_position.user_id` ownership plus owner-scoped review feedback keys, indexes, and restrictive foreign keys; legacy position rows remain unclaimed and legacy review rows remain shared, with no automatic ownership backfill.
- `V10__position_monitoring_backend_contract.sql`: independent Position Monitoring semantics, source trust, and freshness contract.

`src/main/resources/schema.sql` remains the H2 local/test bootstrap path. It is intentionally separate from the PostgreSQL Flyway migration path.

Production target database: PostgreSQL.
Migration framework target: Flyway, SQL-first.
Migration execution model: explicit manual pre-deploy migration.
Rollback policy: forward-only migrations plus verified pre-migration backup and restore.

PDR-PF3 historical status: the first empty-PostgreSQL evidence attempt was
`BLOCKED_TIMEOUT` after an approximately 1h27m manually interrupted
Docker/Testcontainers run. That remains part of the audit history; it is no
longer the current controlled-local result.


PDR-PF4 status: current-state migration and rollback drill requirements are documented in `docs/CURRENT_STATE_MIGRATION_ROLLBACK_DRILL.md`. The drill documentation does not execute migrations, backups, restores, destructive DB operations, or production DB access.


PDR-LIVE3 status: controlled external PostgreSQL Flyway smoke runner is available through `scripts/controlled-postgresql-flyway-smoke.sh` and `ControlledPostgreSqlFlywaySmokeTest`. It skips when controlled DB env is missing, requires explicit non-production and run confirmations, redacts connection values, and does not claim PASS until a disposable controlled PostgreSQL database is supplied.

CALL-1B originally extended `PostgreSqlFlywayMigrationSmokeTest` through V5;
the current contract verifies the full V1-V10 chain, including Position Monitoring
semantic/trust migration, profile
save/load, audit insertion, timestamp handling, rollback atomicity, and
mapper-compatible reads. A run skipped because Docker/Testcontainers is
unavailable is still recorded as `SKIPPED_DOCKER_UNAVAILABLE`, not PASS.

P1/P2 current controlled-local status: the digest-pinned
`scripts/controlled-postgresql-flyway-v7-evidence.sh` run against disposable
localhost PostgreSQL `16.14` completed with `PASS` on 2026-07-15. Fresh V1-V7,
V6-V7, mapper compatibility, three PostgreSQL session timezones, the
PostgreSQL-backed Dashboard validity chain, read-only historical inventory,
and application smoke all completed as `PASS_NOT_SKIPPED`/`PASS`. The runner
removed its auxiliary databases and container. See
`docs/POSTGRESQL_FLYWAY_V7_CONTROLLED_EVIDENCE.md` and
`docs/HISTORICAL_TIME_BASIS_STRATEGY.md`.

That V7 evidence remains historical and target-pinned. P3-U1 validates V8 with
the local H2 bootstrap and static migration contracts; controlled PostgreSQL
V1-to-V8 execution has not run and is not reported as PASS.

Production readiness remains `BLOCKED`. The local disposable evidence does
not prove migration of the real release dataset, production-like
current-state backup/restore, historical-time cutovers, controlled server
operation, or release-owner approval.
