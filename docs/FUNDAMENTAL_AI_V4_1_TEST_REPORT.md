# Fundamental AI v4.1 Target Runtime Remediation Test Report

Status: `LOCAL_VALIDATION_COMPLETE_PENDING_EXACT_HEAD_CI`

Exact implementation Head and final CI evidence are recorded after commit and
push. No live provider secret is used by this report.

## Implemented Test Coverage

| Area | Coverage |
|---|---|
| Standard release | Flyway runtime/JAR resources, no special profile, V1-V13 packaged smoke, existing V13 restart, checksum fail closed, SQL-init ownership |
| Provider capability | Exact ADAUSDT, no USD substitution, timeframe isolation, stale capability, HTTP 451, provider fallback, all-unavailable and per-asset isolation |
| CoinGlass | Disabled/key-missing `NOT_CONFIGURED`, auth header, secret redaction, 200/401/403/429/5xx/timeout, rate budget, freshness and provenance |
| AI readiness | Missing vs explicit zero, RPM/cost/budget states, exact-model authorize, fallback not ready, cache, auth/rate/model/provider failure and secret redaction |
| Auth/bootstrap | Password generation/policy, missing/weak rejection, existing-user no overwrite, readiness/liveness split, login/session/logout regression |
| Regression | Product Source, Workflow Contract, authorization, duplicate-skeleton, full H2/Maven, PostgreSQL package smoke, secret and automatic-trading scans |

## Current Evidence

- Focused B01-B04 suites: `PASS`.
- Java 17 standard clean package: `PASS`.
- Standard JAR contents: Flyway Core, Flyway PostgreSQL support, and V1-V13
  migration resources: `PASS`.
- PostgreSQL 16 empty database migration: `13/13 PASS`.
- Packaged JAR restart against an existing V13 database: `PASS`.
- Corrupted V13 checksum startup and readiness fail closed: `PASS`.
- Standard PostgreSQL 16 packaged smoke: `PASS`.
- Full Maven suite: `4582 tests / 4568 passed / 0 failed / 0 errors /
  14 skipped`.
- Exact-head required CI: `PENDING_DRAFT_PR`.

No live provider or AI secret was used. The PostgreSQL smoke used an isolated
container, generated one-time credentials, disabled external provider calls,
and removed temporary artifacts during cleanup.
