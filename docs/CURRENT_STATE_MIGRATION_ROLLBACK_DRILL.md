# Current-State Migration + Rollback Drill

Package: PDR-PF4 Current-State Migration + Rollback Drill
Date: 2026-07-07
Branch: `codex/pdr-pf4-migration-rollback-drill`
Current main commit reviewed: `954210a65b0705788930a968a8092fba7598fb9b`
Production readiness: BLOCKED
Production deployment: cannot proceed

Current evidence addendum (2026-07-15): controlled localhost PostgreSQL P1/P2
proved fresh V1-V7, V6-V7, three session timezones, Dashboard V7 validity,
mapper compatibility, and fail-closed application smoke. This does not replace
the current-state dataset backup/restore drill defined by this document.

P3.1 addendum (2026-07-15): a deterministic generated release-like V6 dataset
completed source inventory, container-native PostgreSQL 16 backup/restore,
source/recovery structure and full-content fingerprint comparison, V6-to-V7
stable-content comparison, aggregate historical-time inventory, and
fail-closed read-only application smoke. Same-row-count status/time/plan
mutations were detected and rollback restored the original content
fingerprint. Its result is
`PASS_GENERATED_RELEASE_LIKE_REHEARSAL`, but its dataset status is explicitly
`GENERATED_RELEASE_LIKE_NOT_SANITIZED_CLONE`.

Greenfield provenance addendum (2026-07-15): approved decision
`TMV1-GREENFIELD-20260715-001` selects `GREENFIELD_NEW_DATABASE`, requires no
historical business-data preservation, records no existing formal business
database, and sets the go-live initial state to `EMPTY`. The P3.2
sanitized-clone route and final gate are therefore
`NOT_APPLICABLE_BY_APPROVED_GREENFIELD_DECISION`, not PASS. This historical
migration/rollback process and its tooling remain available for recovery,
incident analysis, or a future separately approved existing-data mode.

## Scope

This package defines the safe process and evidence requirements for a current-state PostgreSQL migration and rollback rehearsal. It does not execute a real migration, access a production database, run destructive database operations, change business runtime behavior, or claim production readiness.

## Relation To PDR-PF3 Timeout Evidence

PDR-PF3 recorded empty PostgreSQL migration evidence as `BLOCKED_TIMEOUT`
after an approximately 1h27m interrupted Docker/Testcontainers/PostgreSQL
evidence run. That is retained as historical evidence.

Subsequent P1/P2 controlled evidence completed fresh V1-V7 and V6-V7 on
disposable PostgreSQL 16.14 with no skipped PostgreSQL test. The former
existing-data P3.2 route is no longer an open gate for the approved Greenfield
mode. Greenfield P3-G must instead prove empty-database first boot,
backup/restore, read-only deployment rehearsal, rollback decision points, and
a redacted evidence bundle before any later phase or deployment decision.

## Existing Assets Reviewed

| Asset | Finding |
|---|---|
| `src/main/resources/db/migration/V1__baseline_schema_tables.sql` through `V7__decision_plan_offset_times.sql` | Current PostgreSQL Flyway chain is V1-V7; fresh and V6-V7 disposable runs passed. |
| `src/main/resources/db/migration/README.md` | Records historical PF3 timeout and current P1/P2 controlled evidence without claiming production readiness. |
| `scripts/controlled-postgresql-flyway-v7-evidence.sh` | Repeats digest-pinned disposable migration, timezone, Dashboard, mapper, inventory, and app smoke evidence. |
| `docs/HISTORICAL_TIME_BASIS_STRATEGY.md` | Defines aggregate-only inventory, writer-specific cutovers, and fail-closed handling of unverified history. |
| `scripts/prod-backup.sh` | Requires explicit PostgreSQL env vars and uses `pg_dump`; no DB URL or secrets are hardcoded. |
| `scripts/prod-restore.sh` | Requires explicit restore env vars and confirmation; custom restore uses `--no-owner`, `--no-acl`, and `--exit-on-error`. |
| `scripts/generate-p3-release-like-fixture.sh` | Produces a fixed-seed V6 generated fixture, aggregate checks, custom dump, and generated-only attestation under ignored runtime storage. |
| `scripts/controlled-current-state-clone-rehearsal-p3.sh` | Fixed localhost/digest/database allowlists, strict attestation/path validation, generated/sanitized class isolation, bounded container-native backup/restore, structure/content evidence, dedicated read-only app role, and cleanup trap. Generated P3.1 passed; sanitized P3.2 is retained but not applicable to the approved Greenfield route. |
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
10. P1/P2 controlled-local evidence is attached, while the distinct
    current-state dataset, backup/restore, and historical cutover evidence is
    either completed or explicitly carried as a blocker.

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
5. For the current P3-U1/P3-H contract, verify `flyway_schema_history`
   contains successful V1-V8 rows with the reviewed checksums.
6. Run readonly app smoke against the migrated rehearsal database:

```bash
export APP_URL="http://staging-app-host:8081"
export TRADE_MODEL_SMOKE_USERNAME="operator"
export TRADE_MODEL_SMOKE_PASSWORD="redacted"

bash scripts/prod-smoke.sh
```

7. Run release-gate evidence collection in incomplete-safe mode:

```bash
export APP_URL="http://staging-app-host:8081"
export TRADE_MODEL_SMOKE_USERNAME="operator"
export TRADE_MODEL_SMOKE_PASSWORD="redacted"
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
7. Flyway migration command output and `flyway_schema_history` rows for V1-V7.
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

The original PF4 package was planning/evidence-definition only. Later P1/P2
evidence executed Flyway only against disposable localhost databases and
removed its auxiliary databases and container.

- No production DB was accessed.
- No destructive operation was run outside disposable controlled databases.
- Fresh V1-V7 and V6-V7 controlled-local migration evidence is complete.
- Generated P3.1 executed PostgreSQL 16 backup, clean restore, V6-to-V7
  migration, and application smoke against deterministic synthetic data.
- Its source/recovery structure and full-content fingerprints matched. The
  V6/V7 migration-stable content matched after excluding only the two new,
  separately verified null validity columns.
- Application smoke used a random dedicated read-only role; its write probe
  was denied, Flyway was disabled, and the full app before/after content
  fingerprint matched. Unexpected business writes were `0`.
- Attestation uniqueness, class, generation time, PostgreSQL dump-header
  version, and restored Flyway version checks passed without copying raw
  attestation content.
- The run used PostgreSQL 16 container-native backup/restore. The official
  `prod-backup.sh` and `prod-restore.sh` operational path was not executed, so
  that distinct gate remains blocked.
- This generated run is not a sanitized current-state release dataset. It
  proves the harness and generated rehearsal, not existing-data migration.
- Decision `TMV1-GREENFIELD-20260715-001` makes the P3.2 sanitized-clone route
  `NOT_APPLICABLE_BY_APPROVED_GREENFIELD_DECISION`, not PASS. No fake
  sanitized clone will be produced.
- Historical time cutovers remain unverified and no timestamp was shifted.
- No Java business logic changed.
- No schema.sql or Flyway SQL migration changed.

## Production Readiness Decision

Production readiness remains BLOCKED.

Production deployment cannot proceed.

PDR-PF4 defines reusable drill and evidence requirements. Generated P3.1
proves that the local rehearsal machinery works, but it does not prove
existing-data migration. Under the approved Greenfield mode, readiness remains
blocked pending a separate Greenfield P3-G empty-database first-boot,
backup/restore, and read-only deployment rehearsal plus all human release
gates.

## P3-G Greenfield First-Boot And Recovery Evidence

The P3-G branch executed the approved empty-database route in a disposable
localhost PostgreSQL 16 environment. It started from an empty schema, applied
Flyway V1-V7, applied zero migrations on repeat, restarted PostgreSQL, ran the
repository's official backup and restore scripts, and compared Primary and
Recovery with structure plus full-content fingerprints. The same exact-commit
application image then completed Primary first boot, Primary restart, and
Recovery read-only smoke without changing database content.

This result is `PASS_LOCAL_CONTROLLED_GREENFIELD_REHEARSAL`, not a production
restore or server rehearsal. It does not replace real secret-store, host,
proxy, monitoring, owner, and rollback-decision evidence. It remains pending
Draft PR review/merge and cannot authorize P4.

See `docs/GREENFIELD_POSTGRESQL_FIRST_BOOT_REHEARSAL_P3G.md`.

## Next Remediation Recommendation

Recommended next action: **Reviewer Greenfield P3-G Evidence Review and PR
Merge Readiness**. The sanitized-clone route remains stopped by the approved
Greenfield decision. P4 is not allowed.

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
