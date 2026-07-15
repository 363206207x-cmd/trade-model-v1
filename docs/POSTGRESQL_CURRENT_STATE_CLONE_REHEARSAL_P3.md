# PostgreSQL Current-State Clone Rehearsal P3

## Decision

- Package: `Sanitized Release-Like Current-State Clone Inventory and Backup/Restore Rehearsal P3`
- Base merged-main commit: `c94c99dfa72843e558ac4ce87037bfe71bd5dfaf`
- Branch: `codex/postgresql-current-state-clone-rehearsal-p3`
- Source dataset status: `BLOCKED_MISSING_SANITIZED_RELEASE_LIKE_DUMP`
- P3 database evidence status: `NOT_RUN`
- Effective status: `DRAFT_BRANCH_NOT_MERGED`
- Production readiness: `BLOCKED`

The P3 harness and its offline safety contracts are implemented, but no
sanitized release-like dump or sanitization attestation was present in the
operator environment. The default runner stopped before Docker or database
access. Harness tests are not current-state dataset evidence and are not
reported as a P3 migration, backup, restore, or application-smoke pass.

## Input Gate Result

The guarded default invocation was:

```bash
bash scripts/controlled-current-state-clone-rehearsal-p3.sh
```

It returned:

```text
P3_SANITIZED_DUMP_FILE: MISSING
P3_SANITIZATION_ATTESTATION_FILE: MISSING
P3_DATASET_ID: MISSING
P3_DATASET_CLASS: MISSING
P3_CONFIRM: MISSING
P3_LOCAL_DB_RECREATE_CONFIRM: MISSING
SOURCE_DATASET_STATUS: BLOCKED_MISSING_SANITIZED_RELEASE_LIKE_DUMP
P3_RESULT: BLOCKED_MISSING_SANITIZED_RELEASE_LIKE_DUMP
DATABASE_ACCESS: NOT_ATTEMPTED
DOCKER_ACTION: NOT_ATTEMPTED
PRODUCTION_READINESS: BLOCKED
```

No production database was accessed. No database dump was created or read. No
Docker container was started. No destructive database operation was run.

## Required Operator Input

P3 accepts a PostgreSQL custom-format dump only after all six variables are
present:

```text
P3_SANITIZED_DUMP_FILE
P3_SANITIZATION_ATTESTATION_FILE
P3_DATASET_ID
P3_DATASET_CLASS=SANITIZED_RELEASE_LIKE
P3_CONFIRM=I_CONFIRM_SANITIZED_NON_PRODUCTION_RELEASE_LIKE_DATASET
P3_LOCAL_DB_RECREATE_CONFIRM=I_UNDERSTAND_ONLY_LOCAL_P3_DATABASES_ARE_DROPPED
```

The dump and attestation paths must be absolute and either outside the
repository or under ignored `.runtime/p3-input/`. Their contents are never
copied into Git. Dataset identifiers are hashed before evidence output.

The attestation file uses a key/value contract and must contain:

```text
DATA_SOURCE_CLASS=SANITIZED_RELEASE_LIKE
SANITIZATION_OWNER_OR_PROCESS=<non-secret process reference>
GENERATED_AT_UTC=<timestamp>
SOURCE_POSTGRESQL_VERSION=<version>
SOURCE_FLYWAY_VERSION=<version>
USER_IDENTIFIERS_REMOVED_OR_PSEUDONYMIZED=YES
SECRETS_REMOVED=YES
FREE_TEXT_CLEANED_OR_REPLACED=YES
LOCAL_CONTROLLED_REHEARSAL_ALLOWED=YES
NOT_PRODUCTION_AND_NOT_FOR_PRODUCTION_RESTORE=YES
```

An attestation is a required human/process assertion. The aggregate candidate
scan is an additional misuse guard; zero candidates does not prove that a
dataset is free of every possible secret or personal identifier.

## Runner Safety Contract

`scripts/controlled-current-state-clone-rehearsal-p3.sh` enforces these fixed
targets:

| Item | Fixed value |
|---|---|
| PostgreSQL image | `postgres@sha256:fd1e8d0274f13f5a03a2673a207b28e14823c2f2efc3ca4bb4197c8a9f841bdc` |
| Bind address | `127.0.0.1:55433` |
| Source DB | `trade_model_v1_p3_source` |
| Rehearsal DB | `trade_model_v1_p3_rehearsal` |
| Recovery DB | `trade_model_v1_p3_recovery` |
| Evidence directory | ignored `.runtime/postgresql-p3-rehearsal/` |

The runner rejects remote hosts, alternate ports, alternate database names,
production-like indicators, relative input paths, incomplete attestations,
non-custom dumps, and dumps containing database-creation entries. It uses
bounded commands, a random local password, redacted logs, and an exit trap that
stops the application and removes the disposable container on success or
failure.

It invokes the repository backup and restore contracts. Custom restore now
uses `--no-owner`, `--no-acl`, and `--exit-on-error`. No role or ACL from the
input dump is restored.

## Aggregate-Only Evidence Contract

The runner uses:

- `scripts/current-state-clone-fingerprint.sql`
- `scripts/current-state-clone-restore-verification.sql`
- `scripts/historical-time-basis-inventory.sh`

Permitted evidence includes table row counts, schema/index/constraint counts,
sequence state, Flyway version/checksum/status, integrity anomaly counts,
historical-time distributions, aggregate hashes, and PASS/BLOCKED markers.

The evidence must not contain row identifiers, position quantities or amounts,
prices, source-reference values, reason JSON, free text, credentials, complete
connection strings, or provider keys. Secret, PII, and production-reference
candidate checks output counts only. Any nonzero candidate count blocks the
rehearsal as `BLOCKED_SANITIZATION_ATTESTATION_MISMATCH`.

## Future Controlled Execution Path

Once the required sanitized input exists, one run performs this sequence:

1. Validate the attestation, input location, SHA-256, and custom dump format.
2. Start one digest-pinned local PostgreSQL container.
3. Restore the input into the fixed source database.
4. Verify DB identity, Flyway V6/V7 status, checksums, extensions, roles, FDW,
   aggregate integrity, secret candidates, and historical-time inventory.
5. Invoke `scripts/prod-backup.sh` and record the custom backup SHA-256.
6. Invoke `scripts/prod-restore.sh` into the independent recovery database and
   require exact aggregate fingerprint/inventory matches.
7. Restore the same backup into the rehearsal database.
8. Allow only V6-to-V7 or V7 validate/idempotent migrate. Any other source
   version fails closed.
9. Require unchanged business-table counts after migration.
10. Start the app against rehearsal with every scheduler, AI provider, market
    provider, Push, and external-call path disabled.
11. Check health, Dashboard Home, Run Baseline, safety flags, and zero
    unexpected business writes.
12. Preserve recovery as the pre-migration copy and remove the local container.

The runner never performs a schema-history baseline, repair, historical-time
shift, or production migration.

## Current Evidence Matrix

| Gate | Current result |
|---|---|
| Sanitized release-like dump | `MISSING` |
| Sanitization attestation | `MISSING` |
| Source read-only inventory | `NOT_RUN` |
| Source fingerprint | `NOT_RUN` |
| Controlled backup | `NOT_RUN` |
| Recovery restore | `NOT_RUN` |
| Source/recovery fingerprint | `NOT_RUN` |
| V6-to-V7 or V7 idempotent migration | `NOT_RUN` |
| Post-migration fingerprint | `NOT_RUN` |
| Application readonly smoke | `NOT_RUN` |
| Unexpected business writes | `NOT_MEASURED` |
| Writer cutover | `MISSING_OPERATIONAL_EVIDENCE` |
| Production readiness | `BLOCKED` |

## Offline Contract Tests

`ControlledCurrentStateCloneRehearsalP3ContractTest` verifies missing and
invalid input blocks, exact localhost/database allowlists, explicit recreate
confirmation, checksum/fingerprint requirements, cleanup traps, redaction,
aggregate-only SQL, restore flags, and prohibited migration/production paths.

`ControlledCurrentStateCloneFlywayActionTest` is environment-gated. It can
validate only the three exact localhost P3 databases and migrate only the
rehearsal database. With no controlled DB environment it is skipped; that skip
is expected and is not PostgreSQL evidence.

Authoring validation on this branch recorded `3539` tests, `0` failures, `0`
errors, and `4` skips. The P3 contract class contributed `16` passing tests;
the P3 Flyway action test contributed one of the four environment-gated skips.
The other controlled PostgreSQL skips pre-existed this package. Workflow
contract, YAML parse, shell syntax, and diff-whitespace checks passed. These
results validate code/contracts only and do not change the blocked dataset
evidence status.

## Evidence Artifacts After A Real Run

A successful controlled run would create ignored files under
`.runtime/postgresql-p3-rehearsal/`, including `summary.txt`, input hashes,
source identity, Flyway before/after, source/recovery/rehearsal fingerprints,
backup metadata, restore verification, application smoke, cutover summary, and
checksums. Dumps and attestations remain untracked.

No such runtime evidence bundle exists for this blocked run.

## Remaining Gates

1. Provide an approved sanitized non-production release-like custom dump and
   complete attestation outside Git.
2. Execute P3 without skips and review the redacted evidence bundle.
3. Record real writer deployment/cutover evidence; local code is insufficient.
4. Complete controlled staging/server smoke, real secret-store injection,
   credential rotation, HTTPS/proxy auth smoke, and release-owner approval.

P4 must not start while P3 is blocked on missing input. Production deployment
cannot proceed.
