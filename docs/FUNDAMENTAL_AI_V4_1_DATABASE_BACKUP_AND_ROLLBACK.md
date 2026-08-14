# Fundamental AI v4.1 Database Backup And Rollback

## Backup Gate

1. Stop writes that are not required for availability, or take a transactionally
   consistent PostgreSQL snapshot.
2. Run `bash scripts/prod-backup.sh` with the production database variables
   supplied by the secret store.
3. Confirm the custom-format dump exists, is mode `0600`, has a recorded size
   and checksum, and can be read by `pg_restore --list`.
4. Store the dump outside the application host's disposable release directory.
5. Record the backup identifier in the release checklist before migration.

## Migration Contract

- Flyway owns V1 through V13. The application artifact and migration set are a
  single release unit.
- Migration failure keeps readiness unavailable and stops the release.
- Do not edit an already-applied migration and do not invent a down migration.
- The controlled PostgreSQL 16 smoke must validate all thirteen migrations
  before merge and again from merged `main`.

## Six-Step Rollback

1. Stop the new application artifact so it cannot write further data.
2. Restore the previously approved artifact.
3. If schema/data compatibility requires it, restore the pre-release database
   backup with `scripts/prod-restore.sh` and its explicit confirmation.
4. Start the previous artifact with the previous reviewed environment set.
5. Verify login, liveness, readiness, authenticated Home and Session logout.
6. Confirm provider health and scheduler state separately before declaring the
   rollback complete.

Restoration is an operator decision because it can overwrite data. A rollback
without database restore is allowed only when the previous artifact is known to
be compatible with the migrated schema and the Rollback Decision Owner records
that basis.

## Evidence

Record: release commit, previous artifact checksum, backup file identifier and
checksum, Flyway version before/after, restore command result, smoke result,
decision owner, start/end time, and any lost-write window. Never record database
passwords or connection strings containing credentials.
