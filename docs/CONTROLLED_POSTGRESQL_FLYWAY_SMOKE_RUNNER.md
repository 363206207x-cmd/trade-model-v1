# Controlled PostgreSQL Flyway Smoke Runner

Package: PDR-LIVE3 Controlled PostgreSQL Flyway Smoke Runner
Branch: `codex/pdr-live3-controlled-postgresql-flyway-smoke-runner`
Current main commit: `f617a2f8` (`f617a2f8 Merge pull request #1081 from 363206207x-cmd/codex/pdr-live2-controlled-postgresql-evidence-setup`)
Evidence date: 2026-07-08

## Scope

This package adds a guarded runner for PostgreSQL Flyway migration PASS evidence in a controlled non-production environment.

This is not production deployment. No production database was accessed. No destructive database operation was run. No secrets were committed or printed. No production-ready claim is made.

## What Was Added

| Asset | Purpose | Safety behavior |
|---|---|---|
| `scripts/controlled-postgresql-flyway-smoke.sh` | Bounded runner for controlled external PostgreSQL Flyway smoke | Skips when controlled DB env is missing; requires explicit non-production confirmation and explicit run confirmation; refuses production-like JDBC URL indicators; never prints connection values. |
| `src/test/java/org/example/trademodel/postgresql/ControlledPostgreSqlFlywaySmokeTest.java` | Test-only Flyway migration smoke against an externally supplied controlled PostgreSQL URL | Skips when env is missing; validates confirmations and production-like URL indicators before connecting; checks V1/V2/V3 migration output and `flyway_schema_history`. |

The existing Testcontainers smoke remains unchanged: `src/test/java/org/example/trademodel/postgresql/PostgreSqlFlywayMigrationSmokeTest.java`.

## Environment Result In This Run

| Check | Result |
|---|---|
| `CONTROLLED_POSTGRESQL_JDBC_URL` | MISSING |
| `CONTROLLED_POSTGRESQL_USERNAME` | MISSING |
| `CONTROLLED_POSTGRESQL_PASSWORD` | MISSING |
| Runner default execution | `SKIPPED_MISSING_CONTROLLED_DB` |
| Targeted Maven test without env | PASS command result with test skipped by assumption |
| Production DB access | NONE |
| Destructive DB operation | NONE |
| Secret printing | NONE |

## Runner Contract

The runner may only execute Flyway after all of these are true:

1. `CONTROLLED_POSTGRESQL_JDBC_URL` is present.
2. `CONTROLLED_POSTGRESQL_USERNAME` is present.
3. `CONTROLLED_POSTGRESQL_PASSWORD` is present.
4. `CONTROLLED_POSTGRESQL_EVIDENCE_CONFIRM=I_CONFIRM_DISPOSABLE_NON_PRODUCTION_POSTGRESQL`.
5. `CONTROLLED_POSTGRESQL_FLYWAY_RUN=I_UNDERSTAND_THIS_WRITES_SCHEMA_TO_CONTROLLED_DB`.
6. The JDBC URL does not contain production-like indicators: `prod`, `production`, `live`, `primary`, or `main`.
7. The command completes within 300 seconds.

If any controlled DB env value is missing, the result is `SKIPPED_MISSING_CONTROLLED_DB` and no database access is attempted.

## Command To Run In A Controlled Environment

The operator must set environment variables locally or on a controlled server. Do not paste secrets into chat and do not commit `.env` files.

```bash
CONTROLLED_POSTGRESQL_EVIDENCE_CONFIRM=I_CONFIRM_DISPOSABLE_NON_PRODUCTION_POSTGRESQL CONTROLLED_POSTGRESQL_FLYWAY_RUN=I_UNDERSTAND_THIS_WRITES_SCHEMA_TO_CONTROLLED_DB bash scripts/controlled-postgresql-flyway-smoke.sh
```

The script reads the controlled DB URL, username, and password from environment variables and redacts their values from output.

## PASS Evidence Requirements

A future PASS must show, without secrets:

1. Runner result `CONTROLLED_POSTGRESQL_FLYWAY_RESULT: PASS`.
2. Successful Flyway migration against PostgreSQL using `classpath:db/migration`.
3. `flyway_schema_history` contains at least three successful migration rows.
4. 27 `tm_%` tables exist in `public` schema.
5. Representative required tables exist: `tm_analysis_run`, `tm_decision_result`, `tm_execution_plan`, `tm_user_position`, `tm_position_monitor_log`, `tm_review_result`, `tm_opportunity_log`, `tm_push_snapshot`, `tm_push_recheck_log`, `tm_ai_call_log`, and `tm_asset_state`.
6. Representative required indexes exist: `idx_tm_user_position_status_opened_at`, `idx_tm_push_snapshot_analysis_id`, `idx_tm_ai_call_log_trace_id`, and `uk_tm_review_result_analysis_id`.

## Redaction Policy

- Never print DB URL, username, password, host, or database name.
- Print only status labels such as `PRESENT_REDACTED`, `MISSING`, `SKIPPED_MISSING_CONTROLLED_DB`, `BLOCKED_CONFIRMATION_REQUIRED`, `BLOCKED_PRODUCTION_INDICATOR`, `PASS`, or `FAIL`.
- Do not commit `.env` files or secret-bearing logs.
- Review any future smoke output for accidental secret exposure before committing evidence.

## Current Evidence Decision

Controlled PostgreSQL Flyway smoke result: `SKIPPED_MISSING_CONTROLLED_DB`.

Production readiness: BLOCKED.

Production deployment cannot proceed.

Reason: the runner exists and validates the safe path, but no disposable non-production PostgreSQL URL was present in this environment, so Flyway V1/V2/V3 PASS evidence remains unproven.

## Next Action Required

Recommended next package: `PDR-LIVE4 Controlled PostgreSQL Flyway Evidence Run`.

PDR-LIVE4 should be run only after a disposable non-production PostgreSQL URL is supplied through environment variables and the operator explicitly approves the bounded runner command. Production deployment must remain blocked until all production release gates have PASS evidence.
