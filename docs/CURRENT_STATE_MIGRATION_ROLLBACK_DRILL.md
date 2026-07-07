# Current-State Migration + Rollback Drill

Package: PDR-PF4 Current-State Migration + Rollback Drill
Date: 2026-07-07
Branch: `codex/pdr-pf4-migration-rollback-drill`
Current main commit reviewed: `954210a65b0705788930a968a8092fba7598fb9b`
Production readiness: BLOCKED
Production deployment: cannot proceed

## Scope

This package defines the safe process and evidence requirements for a current-state PostgreSQL migration and rollback rehearsal. It does not execute a real migration, access a production database, run destructive database operations, change business runtime behavior, or claim production readiness.

## Relation To PDR-PF3 Timeout Evidence

PDR-PF3 recorded empty PostgreSQL migration evidence as `BLOCKED_TIMEOUT` after an approximately 1h27m interrupted Docker/Testcontainers/PostgreSQL evidence run. It reviewed the Flyway SQL files but did not prove that V1/V2/V3 migrations complete against PostgreSQL.

PDR-PF4 therefore does not rerun the long PostgreSQL smoke. It defines the next safe evidence path: rehearse migration from a current-main-like database state, prove backup and restore, define rollback decision points, and collect a redacted evidence bundle before any production deployment decision.

## Existing Assets Reviewed

| Asset | Finding |
|---|---|
| `src/main/resources/db/migration/V1__baseline_schema_tables.sql` | PostgreSQL baseline table migration exists. |
| `src/main/resources/db/migration/V2__baseline_schema_indexes.sql` | PostgreSQL baseline index migration exists. |
| `src/main/resources/db/migration/V3__scheme_rule_config_defaults.sql` | PostgreSQL rule-config defaults migration exists. |
| `src/main/resources/db/migration/README.md` | Records PostgreSQL/Flyway path and PDR-PF3 `BLOCKED_TIMEOUT`. |
| `scripts/prod-backup.sh` | Requires explicit PostgreSQL env vars and uses `pg_dump`; no DB URL or secrets are hardcoded. |
| `scripts/prod-restore.sh` | Requires explicit restore env vars and refuses to run unless `RESTORE_CONFIRM=I_UNDERSTAND_RESTORE_CAN_OVERWRITE_DATA`. |
| `scripts/prod-release-gate.sh` | Can require backup evidence, does not run restore automatically, and remains incomplete until restore and human evidence are recorded. |
| `docs/PRODUCTION_READINESS_RUNBOOK.md` | Contains production migration policy, server skeleton, smoke commands, and restore troubleshooting; PDR-PF4 adds the explicit current-state/rollback drill shape. |

## Required Preconditions Before Any Real DB Migration

All preconditions must be true before a real migration rehearsal or production-like drill begins:

1. The target database is a staging, cloned, or otherwise disposable PostgreSQL database, not the production primary.
2. Production secrets are not pasted into docs, PRs, logs, or chat.
3. Database host, username, password, database, and port are supplied through environment variables only.
4. A pre-migration backup is created and its checksum, size, and timestamp are recorded.
5. Restore is tested against a separate recovery database before any production decision.
6. The app remains stopped or isolated from the rehearsal database during migration and restore validation unless the drill step explicitly starts it for readonly smoke.
7. No auto-open, auto-close, auto-reverse, order execution, auto-trading, external push send, fake positions, or fake review records are enabled.
8. A human rollback owner is assigned before the drill starts.
9. A stop/go decision point is declared before Flyway migration runs.
10. PDR-PF3 empty migration evidence is either resolved in a Docker-capable/server-backed environment or explicitly carried as a known blocker.

## Backup Plan

Backup must run before migration rehearsal and before any production deployment decision.

Safe command template for a staging or cloned PostgreSQL database:

```bash
export PROD_DATASOURCE_HOST="staging-db-host"
export PROD_DATASOURCE_PORT="5432"
export PROD_DATASOURCE_USERNAME="staging_operator"
export PROD_DATASOURCE_PASSWORD="redacted"
export PROD_DATASOURCE_DATABASE="trade_model_v1_staging"
export BACKUP_DIR="./backups/pdr-pf4"

bash scripts/prod-backup.sh
```

Required backup evidence:

- command start/end timestamp
- database identifier with secrets redacted
- backup file path
- backup file size
- checksum command and result, for example `shasum -a 256 <backup-file>`
- `pg_dump` version
- confirmation that backup was taken before migration

## Restore Drill Plan

Restore must be rehearsed against a separate recovery database, not the production primary.

Safe command template for a recovery database:

```bash
export RESTORE_DATASOURCE_HOST="recovery-db-host"
export RESTORE_DATASOURCE_PORT="5432"
export RESTORE_DATASOURCE_USERNAME="recovery_operator"
export RESTORE_DATASOURCE_PASSWORD="redacted"
export RESTORE_DATASOURCE_DATABASE="trade_model_v1_recovery"
export RESTORE_BACKUP_FILE="./backups/pdr-pf4/trade_model_v1_YYYYMMDD_HHMMSS.dump"
export RESTORE_CONFIRM="I_UNDERSTAND_RESTORE_CAN_OVERWRITE_DATA"

bash scripts/prod-restore.sh
```

Required restore evidence:

- restore target is a recovery/staging database, not production primary
- restore command start/end timestamp
- restore command result
- row/table-count sanity checks after restore
- app readonly smoke against the restored database, if an app instance is started for the drill
- rollback owner sign-off that restore meets the recovery objective

## Current-State Migration Rehearsal Plan

The current-state rehearsal must use a current-main-like database state. It may be created from a sanitized backup, staging snapshot, or controlled fixture database. It must not use unredacted production dumps in repository artifacts.

Recommended rehearsal sequence:

1. Capture baseline metadata from the rehearsal database:

```bash
psql "$REHEARSAL_DATABASE_URL" -c "select current_database(), current_user, now();"
psql "$REHEARSAL_DATABASE_URL" -c "select count(*) from information_schema.tables where table_schema = 'public';"
psql "$REHEARSAL_DATABASE_URL" -c "select count(*) from flyway_schema_history;" || true
```

2. Create and verify a pre-migration backup with `scripts/prod-backup.sh`.
3. Restore the backup into a separate recovery database with `scripts/prod-restore.sh` and verify the restored state.
4. Run Flyway migration only in the rehearsal environment, using the same migration path as production would use.
5. Verify `flyway_schema_history` contains successful V1/V2/V3 rows.
6. Run readonly app smoke against the migrated rehearsal database:

```bash
export APP_URL="http://staging-app-host:8081"
export SMOKE_AUTH_USERNAME="operator"
export SMOKE_AUTH_PASSWORD="redacted"

bash scripts/prod-smoke.sh
```

7. Run release-gate evidence collection in incomplete-safe mode:

```bash
export APP_URL="http://staging-app-host:8081"
export APP_ADMIN_USERNAME="operator"
export APP_ADMIN_PASSWORD="redacted"
export RELEASE_GATE_REQUIRE_DOCKER="true"
export RELEASE_GATE_REQUIRE_BACKUP="true"
export RELEASE_GATE_REQUIRE_PROVIDER_SMOKE="false"

bash scripts/prod-release-gate.sh
```

The release gate may still report `INCOMPLETE` until restore drill, provider evidence, HTTPS/reverse-proxy evidence, and human review are complete.

## Rollback Decision Tree

```text
Before migration:
  If backup fails -> STOP. Do not migrate.
  If restore drill fails -> STOP. Do not migrate.
  If baseline sanity checks fail -> STOP. Investigate source state.

During migration:
  If Flyway migration fails before app traffic -> keep app stopped, restore from backup to recovery DB, inspect failure, do not proceed.
  If migration succeeds but smoke fails -> keep deployment blocked, compare migrated DB to restored backup, decide restore or fix-forward in staging only.

After migration rehearsal:
  If readonly smoke passes and restore evidence passes -> record evidence; production readiness still BLOCKED until remaining gates pass.
  If any safety field fails -> rollback rehearsal state and keep production deployment blocked.
  If any prohibited trading/push surface appears -> STOP and treat as release blocker.
```

Rollback is restore-based. There is no automatic application rollback, no automatic schema downgrade, and no production restore execution in this package.

## Required Evidence Bundle

A complete PDR-PF4 evidence bundle must include:

1. Git commit and migration file list.
2. Redacted database environment description.
3. Pre-migration baseline table and Flyway history summary.
4. Backup command output, backup file metadata, and checksum.
5. Restore drill command output against a recovery database.
6. Post-restore sanity checks.
7. Flyway migration command output and `flyway_schema_history` rows for V1/V2/V3.
8. Post-migration table/index sanity checks.
9. Readonly production-smoke output.
10. Release-gate output, even if `INCOMPLETE`.
11. Rollback decision record and owner sign-off.
12. Confirmation that no production DB was accessed during local/planning evidence.
13. Confirmation that no destructive DB operation was run during local/planning evidence.
14. Confirmation that no trading/order/push/fake-record capability was added.

## Safe Environment Commands

These commands are templates for a controlled staging or cloned database environment. They are not run by this package.

```bash
# 1. Confirm target identity. Redact output before sharing.
psql "$REHEARSAL_DATABASE_URL" -c "select current_database(), current_user, now();"

# 2. Backup staging/current-state clone.
bash scripts/prod-backup.sh

# 3. Restore backup to a separate recovery database.
RESTORE_CONFIRM=I_UNDERSTAND_RESTORE_CAN_OVERWRITE_DATA bash scripts/prod-restore.sh

# 4. Run migration in staging only, using the project Flyway migration path.
docker compose --profile migrate run --rm migrate

# 5. Verify Flyway history and critical tables.
psql "$REHEARSAL_DATABASE_URL" -c "select version, description, success from flyway_schema_history order by installed_rank;"
psql "$REHEARSAL_DATABASE_URL" -c "select count(*) from information_schema.tables where table_schema = 'public' and table_name like 'tm_%';"

# 6. Run readonly smoke.
bash scripts/prod-smoke.sh

# 7. Record release-gate status.
bash scripts/prod-release-gate.sh
```

## Local Package Evidence

This PF4 package is planning/evidence-definition only.

- No production DB was accessed.
- No destructive DB operation was run.
- No long Docker/Testcontainers/PostgreSQL smoke was rerun.
- No Flyway migration command was executed.
- No backup or restore command was executed.
- No Java business logic changed.
- No schema.sql or Flyway SQL migration changed.

## Production Readiness Decision

Production readiness remains BLOCKED.

Production deployment cannot proceed.

PDR-PF4 defines the drill and evidence requirements. It does not prove production migration readiness until the evidence bundle is completed in a safe staging/server-backed environment and reviewed by the human release gate owner.

## Next Remediation Recommendation

Recommended next package: `PDR-PF5 Staging Migration Evidence Collection` or `PDR-PF5 Secrets and Access Hardening`, depending on whether a Docker-capable/server-backed PostgreSQL staging environment is available.

If no safe PostgreSQL staging environment is available, first resolve Docker/Testcontainers/server-backed PostgreSQL access before attempting migration evidence again.

## Prohibited Items Preserved

- no auto-open
- no auto-close
- no auto-reverse
- no order execution
- no auto-trading
- no external push send
- no fake positions
- no fake review records
- no production-ready claim
