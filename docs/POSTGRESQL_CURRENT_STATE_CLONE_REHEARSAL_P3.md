# PostgreSQL Current-State Clone Rehearsal P3

## Decision

- Package branch: `codex/postgresql-current-state-clone-rehearsal-p3`
- Base merged-main commit: `c94c99dfa72843e558ac4ce87037bfe71bd5dfaf`
- Generated P3.1 rehearsal: `PASS_GENERATED_RELEASE_LIKE_REHEARSAL`
- Source dataset status: `GENERATED_RELEASE_LIKE_NOT_SANITIZED_CLONE`
- Final sanitized current-state clone gate: `BLOCKED_NOT_RUN`
- P4 allowed: `NO`
- Production readiness: `BLOCKED`

P3 now has an executed end-to-end generated-data rehearsal. That rehearsal
proves the bounded harness, PostgreSQL 16 backup/restore, V6-to-V7 migration,
aggregate inventory, and local application smoke against deterministic
repository-generated data. It does **not** prove behavior against a sanctioned
sanitized current-state clone and must not be called a completed final P3 gate.

Detailed generated evidence is in
`docs/P3_GENERATED_RELEASE_LIKE_FIXTURE_EVIDENCE.md`.

## Two Dataset Classes

The runner accepts two deliberately separate contracts:

| Dataset class | Confirmation | Successful result | Final sanitized-clone gate |
|---|---|---|---|
| `GENERATED_RELEASE_LIKE` | `I_CONFIRM_GENERATED_NON_PRODUCTION_RELEASE_LIKE_DATASET` | `PASS_GENERATED_RELEASE_LIKE_REHEARSAL` | `BLOCKED_NOT_RUN` |
| `SANITIZED_RELEASE_LIKE` | `I_CONFIRM_SANITIZED_NON_PRODUCTION_RELEASE_LIKE_DATASET` | `PASS_SANITIZED_RELEASE_LIKE_REHEARSAL` | pending evidence review |

A generated attestation cannot be passed as a sanitized attestation. Neither
class can set `P4_ALLOWED: YES`, and neither run claims production readiness.

## Generated Fixture Path

The deterministic generator is:

```bash
bash scripts/generate-p3-release-like-fixture.sh
```

It uses fixed seed `20260715`, the digest-pinned PostgreSQL 16.14 image,
localhost port `55434`, and disposable database
`trade_model_v1_p3_generated_source`. Flyway is stopped at V6 before fixture
data is inserted. The ignored outputs are:

```text
.runtime/p3-input/generated-release-like-v6.dump
.runtime/p3-input/generated-release-like.attestation
.runtime/p3-input/generated-release-like.summary
```

The attestation states `DATA_SOURCE_CLASS=GENERATED_RELEASE_LIKE`, all real
data inclusion fields are `NO`, and
`SUITABLE_FOR_FINAL_SANITIZED_CLONE_GATE=NO`.

## Sanitized Input Gate

Final P3.2 requires a separately sanctioned custom-format dump and attestation:

```text
P3_SANITIZED_DUMP_FILE
P3_SANITIZATION_ATTESTATION_FILE
P3_DATASET_ID
P3_DATASET_CLASS=SANITIZED_RELEASE_LIKE
P3_CONFIRM=I_CONFIRM_SANITIZED_NON_PRODUCTION_RELEASE_LIKE_DATASET
P3_LOCAL_DB_RECREATE_CONFIRM=I_UNDERSTAND_ONLY_LOCAL_P3_DATABASES_ARE_DROPPED
```

Paths must be absolute and outside tracked repository content, or under the
ignored `.runtime/p3-input/` directory. The attestation must confirm removal
or pseudonymization of user identifiers, removal of secrets, replacement of
free text, non-production provenance, and explicit local rehearsal approval.
Aggregate candidate scans supplement but do not replace that attestation.

## Runner Safety Contract

`scripts/controlled-current-state-clone-rehearsal-p3.sh` fixes these targets:

| Item | Fixed value |
|---|---|
| PostgreSQL image | `postgres@sha256:fd1e8d0274f13f5a03a2673a207b28e14823c2f2efc3ca4bb4197c8a9f841bdc` |
| Bind address | `127.0.0.1:55433` |
| Source DB | `trade_model_v1_p3_source` |
| Rehearsal DB | `trade_model_v1_p3_rehearsal` |
| Recovery DB | `trade_model_v1_p3_recovery` |
| Evidence directory | ignored `.runtime/postgresql-p3-rehearsal/` |

It rejects remote hosts, alternate ports/database names, production-like
indicators, relative input paths, mismatched class/confirmation/attestation,
non-custom dumps, database-creation entries, duplicate/conflicting/unknown
attestation keys, invalid/future attestation timestamps, source-version
mismatches, and symlinked input files or parent directories. Every external
command is bounded. The exit trap stops the local app and removes the
container on success, failure, or interruption.

SQL files are passed explicitly to background psql processes; an empty stdin
cannot count as successful evidence. Backup and restore use `pg_dump` and
`pg_restore` from the pinned PostgreSQL 16 container with `--no-owner`,
`--no-acl`, and `--exit-on-error`. This avoids host PostgreSQL client/server
version drift such as the PostgreSQL 18 `transaction_timeout` setup error
against PostgreSQL 16.

## Executed Generated Evidence

The generated run completed on 2026-07-15 with:

| Gate | Result |
|---|---|
| Source Flyway | V6 |
| Source row counts | analysis=138, decision=120, plan=121, position=7, monitor=8, OHLCV=1200 |
| Source structure fingerprint | `dc2f3d64b0a3b5cfc23e980528a77741e538ae8582150438a58536e23f277cda` |
| Source content fingerprint | `3b77c183a8cd99693f0a582eae8dd608a2bef89d47877ecfa89b0467e9f4b179` |
| Same-row-count mutation detection | `PASS` for status, timestamp, and plan-boundary mutations with rollback restoration |
| Timezone-independent content fingerprint | `PASS` for UTC, Asia/Shanghai, and America/New_York |
| Controlled backup | `PASS` |
| Backup SHA-256 | `9074dc0523e1f3c5026b406d86383cfa71053d226899db087b115843ebb02ff3` |
| Recovery restore | `PASS` |
| Source/recovery structure / full content | `MATCH / MATCH` |
| Migration path | `V6_TO_V7` |
| Migration | `PASS` |
| Historical validity rewrites | `0` |
| Pre/post migration stable content | `MATCH`; V7 validity columns alone are excluded and separately proven all null |
| Historical-time inventory | `PASS_READ_ONLY_AGGREGATE` |
| Application smoke | `PASS` |
| Application database role | random dedicated `READ_ONLY`; connect limited to rehearsal DB |
| Write probe / Flyway during app smoke | `DENIED / DISABLED` |
| App pre/post full content | `MATCH` |
| Same-symbol A/B plan isolation | `PASS` |
| Incomplete-plan fail-closed case | `PASS` |
| Expired historical-plan fail-closed case | `PASS` |
| Revalidation fail-closed case | `PASS` |
| Post-migration structure fingerprint | `aac46a72990a9c5dccc90b8afaffbd9b2a4080788320b00b939533397a347323` |
| Post-migration content fingerprint | `e037df07fb60366a724d5f04072fc8508e4fb6dd23dc1f016e55006d3bce94cf` |
| Unexpected business writes | `0` |
| Container cleanup | `PASS` |

The V6 source validation targets V6 exactly, while the rehearsal migration
targets V7. Pending V7 is therefore not confused with a V1-V6 checksum
failure, and migration still must apply exactly one V7 row.

## Aggregate-Only Evidence Contract

The runner uses:

- `scripts/current-state-clone-fingerprint.sql`
- `scripts/current-state-clone-content-fingerprint.sql`
- `scripts/current-state-clone-restore-verification.sql`
- `scripts/historical-time-basis-inventory.sh`
- `scripts/p3-attestation-validate.sh`

The structure fingerprint contains schema/table/index/constraint counts,
sequence state, and Flyway status/checksum. The independent content
fingerprint contains only table name, row count, and two seeded
`hashtextextended` sum/XOR pairs for each `tm_*` table. Evidence also includes
integrity anomaly counts, time distributions, aggregate hashes, and
PASS/BLOCKED markers. It excludes business rows, field values, identifiers,
quantities, prices, reason text/JSON, credentials, URLs, and provider keys.

The historical inventory is compatible with V6 and V7. It reports whether the
two V7 validity columns exist and never invents values for V6 or legacy rows.

## Application Smoke Contract

The app starts only against the disposable rehearsal DB through a newly
generated read-only role. That role is non-superuser, cannot create databases
or roles, cannot create in the schema, and can connect only to the rehearsal
database. It receives schema usage and SELECT-only table/sequence access. A
no-op UPDATE must be denied before startup. Flyway is disabled, SQL init is
`never`, and Hikari is read-only. All schedulers, AI, market providers,
external calls, Push, and provider escalation are disabled.
The generated run checks:

- health and Run Baseline;
- Dashboard Home safety fields;
- no-open-position and unique-position cases;
- same-symbol multiple positions require explicit position selection;
- exact BTC A and B source plan/analysis identities do not cross;
- incomplete history remains fail-closed;
- expired history remains in position-monitoring review and never becomes a
  current executable suggestion;
- revalidation-required history remains review-only;
- no forbidden trading language; and
- unchanged structure and full content fingerprints after app startup and
  requests.

## Tests

- `ControlledGeneratedReleaseLikeFixtureContractTest` locks seed, digest,
  local target, row coverage, safety flags, aggregate checks, deterministic
  fingerprinting, dump policy, cleanup, and ignored artifacts.
- `ControlledGeneratedReleaseLikeFixtureFlywayTest` can create only the exact
  local generated V6 database after explicit confirmation.
- `ControlledCurrentStateContentFingerprintTest` proves same-data equality,
  same-row-count mutation detection, rollback restoration, three-session
  timezone stability, and value-redacted output against real PostgreSQL.
- `P3AttestationValidateScriptTest` proves duplicate/conflicting keys,
  malformed/future timestamps, class impersonation, and PostgreSQL/Flyway
  mismatches fail closed without raw attestation output.
- `ControlledCurrentStateCloneRehearsalP3ContractTest` proves generated and
  sanitized classes cannot impersonate one another and never unlock P4.
- `ControlledCurrentStateCloneFlywayActionTest` validates only the observed
  V6/V7 source version and migrates only the exact rehearsal DB to V7.

Environment-gated tests that do not receive an approved database remain
skipped and are not reported as PostgreSQL evidence.

## Remaining Gates

1. Acquire an approved, sanitized, non-production release-like current-state
   custom dump and separate attestation.
2. Run P3.2 under `SANITIZED_RELEASE_LIKE` and review its redacted bundle.
3. Complete operational writer-cutover evidence; generated/local rows cannot
   establish production deployment history.
4. Complete controlled server, secret-store/rotation, HTTPS/proxy auth, and
   release-owner gates.
5. Execute and evidence the official operational backup/restore scripts in an
   approved PostgreSQL 16 environment. This P3.1 run used the equivalent
   container-native commands, so `PROD_BACKUP_SCRIPT` and
   `PROD_RESTORE_SCRIPT` remain `NOT_EXECUTED` and the operational-script gate
   remains `BLOCKED`.

Next package: **Sanctioned Sanitized Release-Like Clone Acquisition and P3
Final Evidence P3.2**. Production deployment cannot proceed.
