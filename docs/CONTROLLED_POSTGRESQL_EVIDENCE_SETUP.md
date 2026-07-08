# Controlled PostgreSQL Evidence Setup

Package: PDR-LIVE2 Controlled PostgreSQL Evidence Setup
Branch: `codex/pdr-live2-controlled-postgresql-evidence-setup`
Current main commit: `77e726d5` (`77e726d5 Merge pull request #1080 from 363206207x-cmd/codex/pdr-live1-controlled-live-dependency-acceptance`)
Evidence date: 2026-07-08

## Scope

This package prepares the next concrete path to obtain PostgreSQL Flyway migration PASS evidence in a controlled environment.

This is not production deployment. No production database was accessed. No destructive database operation was run. No real secrets were committed or printed. No production-ready claim is made.

## Existing Assets Reviewed

| Asset | Status | Notes |
|---|---|---|
| `src/main/resources/db/migration/V1__baseline_schema_tables.sql` | PRESENT | PostgreSQL baseline table migration. |
| `src/main/resources/db/migration/V2__baseline_schema_indexes.sql` | PRESENT | PostgreSQL baseline index migration. |
| `src/main/resources/db/migration/V3__scheme_rule_config_defaults.sql` | PRESENT | PostgreSQL rule-config default upsert migration. |
| `src/test/java/org/example/trademodel/postgresql/PostgreSqlFlywayMigrationSmokeTest.java` | PRESENT | Testcontainers-backed PostgreSQL Flyway smoke. It skips when Docker/Testcontainers is unavailable. |
| `docs/PRODUCTION_READINESS_RUNBOOK.md` | PRESENT | Records PF9/PF10/PF11/LIVE1 blockers and production readiness remains BLOCKED. |
| `scripts/prod-backup.sh` / `scripts/prod-restore.sh` | PRESENT | Backup/restore helpers require explicit env; restore requires confirmation. They are not migration smoke runners. |
| External controlled DB Flyway runner | MISSING | No committed script currently runs Flyway against an externally supplied controlled PostgreSQL URL. |
| `scripts/controlled-postgresql-evidence-plan.sh` | ADDED_NO_OP_SETUP | Presence/safety helper only. It never connects to DB or runs Flyway. |

## Environment Checks Run

All checks were bounded to five minutes or less and did not print secret values.

| Check | Result |
|---|---|
| `CONTROLLED_POSTGRESQL_JDBC_URL` presence | MISSING |
| `CONTROLLED_POSTGRESQL_USERNAME` presence | MISSING |
| `CONTROLLED_POSTGRESQL_PASSWORD` presence | MISSING |
| `CONTROLLED_DB_JDBC_URL` / username / password presence | MISSING |
| `STAGING_POSTGRESQL_JDBC_URL` / username / password presence | MISSING |
| `TEST_POSTGRESQL_JDBC_URL` / username / password presence | MISSING |
| `command -v docker` | DOCKER_MISSING |
| `docker version` | DOCKER_MISSING |
| `docker info` | DOCKER_MISSING |

## Available Evidence Path

| Path | Current Status | Decision |
|---|---|---|
| Docker/Testcontainers | DOCKER_MISSING | Not available in this environment; do not run `PostgreSqlFlywayMigrationSmokeTest` expecting PASS. |
| Controlled external PostgreSQL | SKIPPED_MISSING_CONTROLLED_DB | No disposable non-production PostgreSQL URL is present; do not run migration. |
| None / setup-only path | CURRENT | Record setup and required next operator steps. Production readiness remains BLOCKED. |

## Controlled DB URL Presence

Controlled DB URL is not present. The check only reported `PRESENT` or `MISSING`; it did not print URL, username, password, host, or database name.

Result: `SKIPPED_MISSING_CONTROLLED_DB`.

## Safe Command Plan

### Path A: Docker/Testcontainers

Use this only after Docker CLI, daemon, and socket availability are confirmed:

```bash
python3 - <<'PY'
import subprocess, sys
cmd = ['./mvnw', '-q', '-Dtest=PostgreSqlFlywayMigrationSmokeTest', 'test']
r = subprocess.run(cmd, timeout=300)
sys.exit(r.returncode)
PY
```

Expected PASS evidence must include successful Flyway V1/V2/V3 migration against PostgreSQL and `flyway_schema_history` success rows.

### Path B: Controlled External PostgreSQL

This repository does not yet contain a committed external controlled-DB Flyway runner. Before attempting PASS evidence through an external controlled DB, the operator must:

1. Provision a disposable non-production PostgreSQL database.
2. Set `CONTROLLED_POSTGRESQL_JDBC_URL`, `CONTROLLED_POSTGRESQL_USERNAME`, and `CONTROLLED_POSTGRESQL_PASSWORD` in the local/server environment, not chat.
3. Set `CONTROLLED_POSTGRESQL_EVIDENCE_CONFIRM=I_CONFIRM_DISPOSABLE_NON_PRODUCTION_POSTGRESQL`.
4. Run the setup helper first:

```bash
bash scripts/controlled-postgresql-evidence-plan.sh
```

5. If the helper reports `READY_FOR_SEPARATE_BOUNDED_FLYWAY_RUNNER`, run a separately approved bounded Flyway runner that redacts all connection values and writes only status summaries.
6. Capture redacted output showing V1/V2/V3 success.
7. Do not use production DB and do not run destructive operations.

## Redaction Policy

- Never print DB URL, username, password, host, or database name.
- Print only presence/status labels such as `PRESENT_REDACTED`, `MISSING`, `DOCKER_MISSING`, `SKIPPED_MISSING_CONTROLLED_DB`, `PASS`, or `FAIL`.
- Do not commit `.env` files or secret-bearing logs.
- Any evidence log must be reviewed for secret exposure before being committed.

## Required Operator Steps To Collect PASS Evidence

1. Choose either Docker/Testcontainers or controlled external PostgreSQL. Do not mix production DB into this workflow.
2. Confirm the environment is disposable or non-production.
3. Confirm all commands have timeout <= 5 minutes.
4. Run the setup helper if using controlled external PostgreSQL.
5. Run a bounded Flyway migration proof only after the controlled DB path is confirmed safe.
6. Capture redacted success output proving V1/V2/V3 migration success and `flyway_schema_history` success rows.
7. Keep production readiness BLOCKED until a later release gate aggregates every PASS evidence item.

## No-Access / No-Destructive Confirmation

PASS for this package.

- No production DB was accessed.
- No controlled DB was accessed because no controlled DB URL was present.
- No destructive DB operation was run.
- No Flyway migration was run.
- No secrets were printed or committed.

## Production Readiness Decision

Production readiness: BLOCKED.

Production deployment cannot proceed.

Reason: PostgreSQL Flyway V1/V2/V3 PASS evidence is still not proven. Docker/Testcontainers is missing locally, and no disposable controlled PostgreSQL URL was supplied.

## Next Action Required

Recommended next package: `PDR-LIVE3 Controlled PostgreSQL Flyway Runner Or Evidence Run`.

The next package should either:

1. Provide Docker/Testcontainers availability and run the bounded existing smoke, or
2. Add and run a guarded external controlled-DB Flyway runner after a disposable non-production PostgreSQL URL is supplied through environment variables.

Production deployment must remain blocked until every production release gate has PASS evidence.
