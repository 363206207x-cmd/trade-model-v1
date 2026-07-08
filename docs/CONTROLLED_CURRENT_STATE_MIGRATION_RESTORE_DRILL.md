# Controlled Current-State Migration + Restore Drill

Package: PDR-LIVE5 Controlled Current-State Migration + Restore Drill Evidence
Branch: `codex/pdr-live5-current-state-migration-restore-drill`
Current main commit: `fe61214d216d5f618d7fc02e4b0f4514384fbd5b`
Evidence date: 2026-07-08

## Scope

This package records the controlled current-state migration, backup, and restore drill status after PDR-LIVE4 proved the empty controlled PostgreSQL Flyway migration gate.

This is not production deployment, not production DB access, not a destructive production operation, and not a production-ready claim.

## Relation To PDR-LIVE4 PASS

PDR-LIVE4 recorded operator-provided controlled PostgreSQL Flyway PASS evidence:

- PostgreSQL version: `16.14`
- Flyway validated 3 migrations
- Applied V1 baseline schema tables
- Applied V2 baseline schema indexes
- Applied V3 scheme rule config defaults
- Final schema version: `v3`
- `CONTROLLED_POSTGRESQL_FLYWAY_RESULT: PASS`

That proves the empty disposable controlled PostgreSQL migration gate only. PDR-LIVE5 checks whether the next gate, controlled current-state backup/restore and migration rehearsal evidence, can run in this environment.

## Environment Presence Result

Controlled source DB env presence:

- `CONTROLLED_CURRENT_STATE_DB_HOST`: MISSING
- `CONTROLLED_CURRENT_STATE_DB_NAME`: MISSING
- `CONTROLLED_CURRENT_STATE_DB_USERNAME`: MISSING
- `CONTROLLED_CURRENT_STATE_DB_PASSWORD`: MISSING

Controlled recovery DB env presence:

- `CONTROLLED_RECOVERY_DB_HOST`: MISSING
- `CONTROLLED_RECOVERY_DB_NAME`: MISSING
- `CONTROLLED_RECOVERY_DB_USERNAME`: MISSING
- `CONTROLLED_RECOVERY_DB_PASSWORD`: MISSING

Local PostgreSQL tool availability check:

- `pg_dump`: MISSING
- `pg_restore`: MISSING
- `psql`: MISSING

No DB host, database, username, password, or URL values were printed or recorded.

## Controlled Drill Helper

Added helper script:

```bash
bash scripts/controlled-current-state-migration-restore-drill.sh
```

The helper defaults to no-op evidence mode. It prints only redacted env presence, skips safely when env is missing, refuses production-like host/database indicators, and requires explicit disposable non-production confirmations before backup/restore can run.

Required confirmations for an actual controlled run:

```bash
CONTROLLED_CURRENT_STATE_DRILL_CONFIRM=I_CONFIRM_DISPOSABLE_NON_PRODUCTION_POSTGRESQL
CONTROLLED_CURRENT_STATE_BACKUP_RUN=I_UNDERSTAND_THIS_READS_CONTROLLED_DB_AND_WRITES_LOCAL_BACKUP
CONTROLLED_CURRENT_STATE_RESTORE_RUN=I_UNDERSTAND_THIS_RESTORES_TO_DISPOSABLE_CONTROLLED_RECOVERY_DB
```

## Commands Run

Environment/tool presence check:

```bash
python3 - <<'PY'
import os, shutil
vars = [
 'CONTROLLED_POSTGRESQL_JDBC_URL',
 'CONTROLLED_POSTGRESQL_USERNAME',
 'CONTROLLED_POSTGRESQL_PASSWORD',
 'CONTROLLED_RECOVERY_POSTGRESQL_JDBC_URL',
 'CONTROLLED_RECOVERY_POSTGRESQL_USERNAME',
 'CONTROLLED_RECOVERY_POSTGRESQL_PASSWORD',
]
for name in vars:
    print(f'{name}: ' + ('PRESENT_REDACTED' if os.environ.get(name) else 'MISSING'))
for tool in ['pg_dump', 'pg_restore', 'psql']:
    print(f'{tool}: ' + ('AVAILABLE' if shutil.which(tool) else 'MISSING'))
PY
```

Controlled drill helper default/no-env run:

```bash
bash scripts/controlled-current-state-migration-restore-drill.sh
```

Output summary:

```text
CONTROLLED_CURRENT_STATE_BACKUP_RESULT: SKIPPED_MISSING_CONTROLLED_DB
CONTROLLED_CURRENT_STATE_RESTORE_RESULT: SKIPPED_MISSING_RECOVERY_DB
CONTROLLED_CURRENT_STATE_MIGRATION_REHEARSAL_RESULT: SKIPPED
CONTROLLED_CURRENT_STATE_DRILL_ACTION: no database access attempted
```

## Results

| Gate | Result | Reason |
|---|---|---|
| Backup | `SKIPPED_MISSING_CONTROLLED_DB` | No controlled current-state source DB env was present. |
| Restore | `SKIPPED_MISSING_RECOVERY_DB` | No controlled recovery DB env was present. |
| Current-state migration rehearsal | `SKIPPED` | Source/recovery DB env was missing, so no rehearsal could run. |

No backup, restore, Flyway, psql, pg_dump, or pg_restore command was executed against a database.

## Safety Confirmation

- No production DB was accessed.
- No destructive operation outside a disposable controlled DB was run.
- No destructive operation was run at all in this package.
- No secrets were printed.
- No `.env` or secret material was committed.
- No Java business logic changed.
- No `schema.sql` changed.
- No Flyway SQL changed.
- No `application.yml` or `application-prod.yml` changed.
- No runtime trading behavior changed.
- No auto-open, auto-close, auto-reverse, order execution, auto-trading, external push send, fake positions, or fake review records were added.

## Production Readiness Decision

Production readiness remains BLOCKED.

Production deployment cannot proceed.

Reason: PDR-LIVE5 prepares a guarded drill path but does not complete backup, restore, or current-state migration rehearsal evidence because controlled source/recovery DB env and local PostgreSQL client tools are missing in this execution environment.

## Remaining Blockers

1. Controlled current-state source PostgreSQL env is missing for this package run.
2. Controlled recovery PostgreSQL env is missing for this package run.
3. Local `pg_dump`, `pg_restore`, and `psql` tools are missing in this execution environment.
4. Backup evidence has not been collected.
5. Restore evidence has not been collected.
6. Current-state migration rehearsal evidence has not been collected.
7. Provider live smoke evidence remains incomplete for the production release gate.
8. Secrets manager, credential rotation, HTTPS/reverse-proxy, access logging, audit logging, and rate limiting evidence remains incomplete.
9. Real server deployment smoke evidence remains incomplete.
10. Release owner approval and complete production release evidence bundle are missing.

## Next Recommendation

Next recommended package: `PDR-LIVE6 Controlled Current-State Drill Execution` after a disposable controlled source DB, a separate disposable recovery DB, and PostgreSQL client tools are available.

The next package must remain non-production, must not access production DB, must not print secrets, and must keep production deployment blocked unless every release gate has PASS evidence.
