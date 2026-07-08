# Controlled PostgreSQL Flyway Evidence Run

Package: PDR-LIVE4 Controlled PostgreSQL Flyway Evidence Run
Branch: `codex/pdr-live4-controlled-postgresql-flyway-evidence-run`
Current main commit: `f647a21b6b5e1dde3262e9be0aa12c40861a1d40`
Evidence date: 2026-07-08

## Scope

This document records operator-provided PASS evidence for a controlled disposable local PostgreSQL Flyway migration run. It is evidence documentation only.

This is not production deployment, not production DB access, not a destructive production operation, and not a production-ready claim.

## Command Run

The operator ran the guarded controlled PostgreSQL Flyway smoke runner with explicit non-production confirmations:

```bash
CONTROLLED_POSTGRESQL_EVIDENCE_CONFIRM=I_CONFIRM_DISPOSABLE_NON_PRODUCTION_POSTGRESQL \
CONTROLLED_POSTGRESQL_FLYWAY_RUN=I_UNDERSTAND_THIS_WRITES_SCHEMA_TO_CONTROLLED_DB \
bash scripts/controlled-postgresql-flyway-smoke.sh
```

## Redacted Environment Status

| Item | Status |
|---|---|
| Environment type | Disposable local Docker PostgreSQL |
| PostgreSQL endpoint | `localhost:55432` |
| Database | `trade_model_smoke` |
| Confirmation for disposable non-production DB | PRESENT |
| Confirmation for schema-writing Flyway run | PRESENT |
| Secret values printed | NO |
| Production DB used | NO |
| Destructive operation outside disposable controlled DB | NO |

Credential values, JDBC URL details beyond the local host/port/database evidence above, usernames, and passwords are not recorded in this repository.

## PostgreSQL Runtime

- PostgreSQL version: `16.14`
- Database role: disposable local controlled smoke database
- Production DB accessed: NO

## Flyway Migration Summary

- Flyway validated 3 migrations.
- Applied migration V1: baseline schema tables.
- Applied migration V2: baseline schema indexes.
- Applied migration V3: scheme rule config defaults.
- Final schema version: `v3`.
- Runner result: `CONTROLLED_POSTGRESQL_FLYWAY_RESULT: PASS`.

## Result

Controlled PostgreSQL Flyway evidence result: PASS.

This closes the previously missing empty PostgreSQL Flyway V1/V2/V3 migration evidence for a disposable controlled local PostgreSQL database. It does not prove current-state migration, rollback, restore, provider live smoke, server deployment, secret rotation, or production release readiness.

## Safety Confirmation

- No production DB was accessed.
- No destructive operation outside the disposable controlled DB was run.
- No secrets were printed.
- No `.env` or secret material was committed.
- No Java business logic changed.
- No `schema.sql` changed.
- No Flyway SQL changed.
- No runtime trading behavior changed.
- No auto-open, auto-close, auto-reverse, order execution, auto-trading, external push send, fake positions, or fake review records were added.

## Production Readiness Decision

Production readiness remains BLOCKED.

Production deployment cannot proceed.

Reason: this package records one controlled PostgreSQL Flyway migration gate only. The release gate still requires additional production-like evidence and explicit owner approval before any production deployment can be considered.

## Remaining Blockers

1. Current-state migration and rollback drill have not been executed against a controlled current-state database.
2. Backup and restore evidence with post-restore smoke is incomplete.
3. Provider live smoke evidence remains incomplete for the production release gate.
4. Secrets manager, credential rotation, HTTPS/reverse-proxy, access logging, audit logging, and rate limiting evidence remains incomplete.
5. Real server deployment smoke evidence remains incomplete.
6. Release owner approval and complete production release evidence bundle are missing.

## Next Recommendation

Next recommended package: `PDR-LIVE5 Current-State Migration And Rollback Evidence`.

That package should use a safe controlled non-production or production-like database, must not access production DB without explicit separate approval, and must keep production deployment blocked unless every release gate has PASS evidence.
