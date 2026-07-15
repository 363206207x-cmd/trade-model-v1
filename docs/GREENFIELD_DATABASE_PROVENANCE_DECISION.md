# Greenfield Database Provenance Decision

## Approved Decision

- Decision reference: `TMV1-GREENFIELD-20260715-001`
- Decision status: `APPROVED`
- Data provenance mode: `GREENFIELD_NEW_DATABASE`
- Historical business data must be preserved: `NO`
- Existing business database exists: `NO`
- Go-live database initial state: `EMPTY`
- Decision package: PR `#1127`, merged/effective on main

The approved release-owner/data-owner decision is internally consistent: the
formal database starts empty, and no existing formal business database or
historical business data must be migrated. This record contains no owner name,
email address, database address, account, credential, or secret.

## Data Provenance Boundary

Local H2 data, test fixtures, and repository-generated PostgreSQL datasets are
test and rehearsal inputs only. They are not production history and must not be
promoted, copied, or described as historical business data for go-live.

No fabricated sanitized clone will be produced. The existing-data P3.2 route
is stopped by this approved Greenfield decision:

- `P3.2_SANITIZED_CLONE_ROUTE: NOT_APPLICABLE_BY_APPROVED_GREENFIELD_DECISION`
- `P3_FINAL_SANITIZED_CLONE_GATE: NOT_APPLICABLE_BY_APPROVED_GREENFIELD_DECISION`
- `P3_SANITIZED_CLONE: NOT_APPLICABLE_BY_GREENFIELD_DECISION`

These classifications mean **not applicable**, not PASS. They do not prove an
existing-data migration or sanitized-clone rehearsal.

## Evidence Retained

P3.1 remains valid as bounded harness and generated-data rehearsal evidence:

- generated release-like dataset rehearsal: `PASS`;
- fresh PostgreSQL/Flyway V1-to-V7 and V6-to-V7 paths verified;
- PostgreSQL 16 backup and restore toolchain exercised on generated data;
- structure and full-content fingerprints verified;
- same-row-count content changes detected;
- dedicated read-only application role verified and write probe denied;
- application startup preserved the content fingerprint; and
- strict attestation parsing verified.

The correct aggregate classification is:

- `P3_HARNESS_AND_GENERATED_REHEARSAL: PASS`
- `EXISTING_DATA_MIGRATION: NOT_APPLICABLE`

The generated evidence must never be relabeled as
`EXISTING_DATA_MIGRATION: PASS`.

## Tool Retention

The generated-data tooling, backup/restore runner, structure/content
fingerprints, read-only role, attestation validator, historical-time inventory,
and sanitized-clone support code remain in the repository. They remain useful
for recovery rehearsals, incident analysis, and a future separately approved
data-migration mode. The current go-live path does not depend on historical
data migration.

## P3-G Controlled Rehearsal Result

Merged main `72b5bc83f4d670d4adebc03f5fe28e0bb9bba535` made this provenance
decision effective and allowed the separately scoped P3-G package to start.
The P3-G branch records
`PASS_LOCAL_CONTROLLED_GREENFIELD_REHEARSAL` for:

- an empty disposable localhost PostgreSQL 16 database;
- Fresh Flyway V1-V7 and zero-repeat migration;
- an exact versioned seed allowlist with no runtime business rows;
- repository `prod-backup.sh` and `prod-restore.sh` execution;
- matching Primary/Recovery structure and full-content fingerprints; and
- Primary, restart, and Recovery application smoke through separated read-only
  application roles.

This is branch-local evidence until its Draft PR is merged. It does not alter
the approved data mode, prove server deployment, authorize P4, or change
Production Deployment Readiness from `BLOCKED`. See
`docs/GREENFIELD_POSTGRESQL_FIRST_BOOT_REHEARSAL_P3G.md`.

## Next Gate

P3-G has run locally and now requires independent evidence review plus a Draft
PR merge-readiness decision. An unmerged P3-G PR is not effective evidence.
This package does not authorize P4 and is not production deployment.

- `GREENFIELD_P3_G: PASS_LOCAL_CONTROLLED_PENDING_PR_EFFECTIVE`
- `P4: BLOCKED`
- `PRODUCTION_READINESS: BLOCKED`

Production deployment cannot proceed.
