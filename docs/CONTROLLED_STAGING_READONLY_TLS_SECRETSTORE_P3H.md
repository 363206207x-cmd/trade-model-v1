# Controlled Staging Read-Only TLS And Secret-Store Evidence P3-H

Status: `NOT_COMPLETE`

Offline Harness: `PASS`

Local Compose Template Smoke: `PASS_LOCAL_DISPOSABLE_P3H_TEMPLATE_SMOKE`

Real Staging: `BLOCKED_MISSING_AUTHORIZED_INPUT`

Production Deployment Readiness: `BLOCKED`

P4 Allowed: `NO`

## Scope And Provenance

- Base merged main: `8f0640331e58e8b8b657c7db08e6d79b03d37a4f`.
- Branch: `codex/staging-readonly-tls-secrets-p3h`.
- Data provenance: `GREENFIELD_NEW_DATABASE`.
- Greenfield decision: `TMV1-GREENFIELD-20260715-001`.
- P3-G status: `EFFECTIVE_MERGED_MAIN`.
- Environment requested by the package: explicitly authorized, non-production Linux staging only.

P3-H is not production deployment. The package adds a fail-closed deployment
harness, immutable deployment templates, offline contract tests, and an
actual disposable localhost Compose smoke. It does not claim that an
authorized staging server, real TLS endpoint, real Secret Store, rotation
drill, server backup/restore drill, or reboot was exercised.

## Controlled Input Result

Only presence was checked for the P3-H environment contract. Values were not
read, printed, searched for, or inferred. At the start of this package every
required controlled server and Secret Store input was absent. The guarded
default invocation therefore stopped before network or secret access:

```text
P3H_INPUT_STATUS: MISSING_REQUIRED_INPUTS
P3H_MISSING_INPUT_COUNT: 17
P3H_RESULT: BLOCKED_MISSING_CONTROLLED_STAGING_INPUT
FAILED_OR_CURRENT_STAGE: input-presence
SERVER_ACCESS: NOT_ATTEMPTED
SECRET_ACCESS: NOT_ATTEMPTED
P4_ALLOWED: NO
PRODUCTION_READINESS: BLOCKED
```

`P3H_CA_BUNDLE_FILE` is conditionally required only for `INTERNAL_CA`, so it
is not included in the 17-input default count. No SSH connection, host-key
probe, remote command, server inventory, secret mount, database operation, or
HTTPS request was attempted.

## Offline And Local Template Evidence

| Contract | Repository/local result | Real-server evidence |
| --- | --- | --- |
| Missing input stops before access | `GUARD_PASS` | `NOT_ATTEMPTED` |
| Greenfield startup order | `PASS_LOCAL_DISPOSABLE`: PostgreSQL healthy -> empty preflight -> role bootstrap -> Flyway V1-V7 -> grants -> Secret materialization -> app healthy -> proxy healthy | `NOT_ATTEMPTED` |
| Explicit lifecycle mode | `GUARD_PASS`: initialize, Greenfield recovery, and steady-state modes are explicit; recovery requires a separate confirmation and no mode is inferred from database contents | `NOT_ATTEMPTED` |
| Partial initialization recovery | `PASS_LOCAL_DISPOSABLE`: V1-V3 continuous prefix recovered to V7; non-contiguous history, checksum mismatch, failed migration, unknown object, missing confirmation, and business-data drift fail closed | `NOT_ATTEMPTED` |
| Persistent-volume restart | `PASS_LOCAL_DISPOSABLE`: first boot -> retained database volume -> steady-state restart -> reboot-like stop/start, with zero repeated migrations and matching content fingerprints | `NOT_ATTEMPTED` |
| Active Secret version | `PASS_LOCAL_DISPOSABLE`: after reboot-like restart, V2 database/admin authentication succeeded and both V1 credentials were denied | `NOT_ATTEMPTED` |
| Failed-start cleanup | `PASS_LOCAL_DISPOSABLE`: measured cleanup stopped/removed PostgreSQL and every project container, removed materialized tmpfs, and preserved Primary/Backup volumes plus source Secrets | `NOT_ATTEMPTED` |
| Strict Greenfield inventory | `PASS_LOCAL_DISPOSABLE`: clean database accepted; function, non-public table, foreign server/FDW, extension, and sequence fixtures rejected | `NOT_ATTEMPTED` |
| Role provisioning | `PASS_LOCAL_DISPOSABLE`: migration owner, read-only app, backup reader, recovery owner, Primary and Recovery | `NOT_ATTEMPTED` |
| Staging and Secret Store attestations | `GUARD_PASS`: exact keys, one occurrence, nonempty/non-placeholder values, canonical external files | `NOT_VALIDATED_INPUT_MISSING` |
| SSH key and pinned host-key policy | `GUARD_PASS`: every scanned line is fingerprinted and only one exact approved line reaches `UserKnownHostsFile`; zero/duplicate matches block | `NOT_ATTEMPTED` |
| Production-like target rejection | `GUARD_PASS` | `NOT_ATTEMPTED` |
| Secret backend | `SYSTEMD_CREDENTIALS` adapter implemented; other advertised backends fail `BLOCKED_BACKEND_NOT_IMPLEMENTED` | `NOT_ATTEMPTED` |
| `/run/` runtime mount policy | `GUARD_PASS`; requires `findmnt` proof of tmpfs/ramfs, read-only options and nonpersistent source | `NOT_ATTEMPTED` |
| Spring Config Tree injection | `PASS_LOCAL_DISPOSABLE`: UID 10001 reads 0400 files from tmpfs; UID 10002 is denied | `NOT_ATTEMPTED` |
| Missing Config Tree secret | `FAIL_CLOSED_OFFLINE` | `NOT_ATTEMPTED` |
| Immutable image sources and exact Git archive | `PASS_LOCAL_DISPOSABLE`: expected branch, clean worktree, exact Head archive, context safety, archive-only build, and exact revision label | `NOT_BUILT_ON_SERVER` |
| Internal backend network and proxy-only ports | `PASS_LOCAL_DISPOSABLE` | `NOT_ATTEMPTED` |
| Scheduler, AI, provider and external calls | template locked off | `NOT_ATTESTED_ON_SERVER` |
| Host-header contract | `PASS_LOCAL_DISPOSABLE`: unknown HTTP rejected, unknown HTTPS SNI rejected, approved redirect target fixed, approved Host forwarded | `NOT_ATTEMPTED` |
| TLS target and TLS 1.3 | `PASS_LOCAL_DISPOSABLE`; remote harness binds URL/host/port and fails TLS 1.3 when the client supports it but the server does not | `NOT_ATTEMPTED` |
| Read-only application write probe | `DENIED_LOCAL_DISPOSABLE` | `NOT_ATTEMPTED` |
| Read-only membership/default ACL/Sequence contract | `PASS_LOCAL_DISPOSABLE`: app/backup memberships reject, default ACLs are exact SELECT-only, Sequence USAGE/UPDATE and database CREATE/TEMP are denied | `NOT_ATTEMPTED` |
| Secret values in inspect/process arguments | `ABSENT_LOCAL_DISPOSABLE` | `NOT_ATTEMPTED` |
| Access-log redaction and rate limiting | template/contract prepared | `NOT_ATTEMPTED` |
| Secret rotations | evidence validator prepared | `NOT_ATTEMPTED` |
| Backup and recovery | documented against official scripts | `NOT_ATTEMPTED` |
| Service/server reboot | reboot evidence contract prepared | `NOT_ATTEMPTED` |
| Secret leak scan | fixture-level contract tested | `NOT_ATTEMPTED_ON_SERVER` |

Offline `GUARD_PASS` means only that the repository contract fails closed.
`PASS_LOCAL_DISPOSABLE` means the exact template ran against generated local
credentials and disposable local containers. Neither may be translated to a
real-server PASS.

## Deployment Assets

`deploy/p3h/docker-compose.p3h.yml` defines digest-pinned PostgreSQL 16,
Flyway, application, and Nginx services. `p3h-compose-start.sh` is the only
lifecycle entrypoint and requires one explicit mode. `INITIALIZE_GREENFIELD`
enforces strict empty-object inventory, role/database bootstrap, Flyway V1-V7,
and grants. `RECOVER_GREENFIELD_INITIALIZATION` requires a distinct exact
confirmation, validates a continuous checksum-valid V1-VN prefix or a V7
pre-grant state, exact P3-H identity/objects, and zero business rows, then
continues to V7. Recovery and steady state both run `CORE_STATE_VERIFY`,
refresh grants, then run `FULL_READONLY_STATE_VERIFY`. No mode runs Flyway
baseline, repair, or clean. Recovery's pre-migrate Flyway validation ignores
only pending migrations (`*:pending`), while applied checksum, failed,
missing, and future states remain fail-closed; post-migrate validation is
strict. PostgreSQL and the
application expose no host ports. The backend network is internal; only the
reverse proxy publishes 80/443. The application runs read-only with Flyway and
SQL init disabled, and all schedulers, AI, providers, and external-call
switches off.

All long-running Compose services use `restart: "no"`; systemd is the sole
lifecycle owner, so Docker daemon auto-restart cannot bypass migration,
Secret, app-health, or proxy-health ordering. A failed start stops and removes
PostgreSQL and every project container plus the materialized tmpfs volume.
Cleanup PASS requires measured zero project containers, absent materialized
volume, and present Primary/Backup volumes. It does not remove source Secret
files or persistent database/backup volumes. Deleting persistent volumes
remains a separate explicit disposable-environment action.

The bootstrap creates `p3h_migration_owner`, `p3h_app_readonly`,
`p3h_backup_reader`, and `p3h_recovery_owner`, plus the Primary and independent
Recovery databases. No password is embedded in SQL, Compose, an environment
declaration, an image, or a process argument; database tools receive their
credentials from mounted Secret files at runtime.

Active application database/admin Secret versions are selected only by the
non-sensitive `P3H_ACTIVE_APP_DATABASE_SECRET_VERSION` and
`P3H_ACTIVE_APP_ADMIN_SECRET_VERSION` values (`V1` or `V2`). Rotation updates
the database role through a separately confirmed one-shot action. Restart
does not select V1 implicitly and does not reactivate an old credential. The
reboot-like local path proves V2 admin HTTP 200 and V2 database connection,
then proves V1 admin HTTP 401/403 and V1 database connection denial.

The app and backup roles are required to have zero `pg_auth_members` rows, so
neither can `SET ROLE` to migration, recovery, bootstrap, or another user
role. Read-only grant refresh first clears existing/default table and Sequence
ACLs and then grants only SELECT. Full verification rejects non-SELECT default
ACL entries, Sequence USAGE/UPDATE, table writes, schema CREATE, database
CREATE/TEMP, missing SELECT, or any role membership.

Only `SYSTEMD_CREDENTIALS` is implemented for a future server run. The unit
uses `LoadCredentialEncrypted=` and a bounded adapter; SOPS, Vault, and cloud
agent modes fail closed as not implemented. The adapter requires a systemd
credential path under `/run/credentials/`; remote preflight separately proves
the effective mount with `findmnt`.

An isolated root materializer copies only application secrets into a Docker
managed tmpfs volume, assigns fixed UID/GID 10001 and mode 0400, then exits.
The non-root application mounts that volume read-only and uses
`configtree:/run/secrets/config/` for:

- `spring.datasource.password`;
- `trade-model.auth.admin-password`;
- `binance.api.key`; and
- `binance.api.secret`.

No `.env`, secret value, private key, dump, backup, or attestation is included
in the image build context. The nonfunctional Binance placeholders are needed
only to satisfy the existing production position-provider guard; external
calls remain disabled and the backend network has no external egress.

## TLS And Proxy Contract

The template requires TLS 1.2/1.3, verified hostnames, a 308 HTTP-to-HTTPS
redirect, short staging HSTS (`max-age=86400`, no preload), bounded request
sizes/timeouts, forwarded headers, hidden server version, and 429 rate-limit
responses. It does not allow `curl -k` or `--insecure`.

Only health endpoints may be unauthenticated. Dashboard, Review Center, Run
Baseline, and `/api/**` require application authentication. A real evidence
run must prove good credentials succeed, missing/bad credentials fail, health
details remain hidden, and Authorization/Cookie values never enter logs.

The disposable local template proved approved-host redirect and HTTPS health,
unknown HTTP/HTTPS host rejection, and a TLS 1.3 handshake with a generated
one-day localhost certificate. This is not certificate issuance or real
staging endpoint evidence. Authenticated dashboard/review smoke, real rate
limit evidence, certificate rotation/renewal, and server proxy-log evidence
remain `NOT_ATTEMPTED`.

## Backup And Restore Plan

The only approved operational paths remain `scripts/prod-backup.sh` and
`scripts/prod-restore.sh`. A future authorized P3-H run must use a read-only
backup role, create a PostgreSQL custom-format artifact with mode 0600 and a
SHA-256, and keep the dump in an ignored controlled directory.

Restore must target the independent recovery database
`trade_model_v1_p3h_recovery` and must never overwrite the staging primary.
The run must compare schema, all content, Flyway history, schema types,
indexes, constraints, sequences, and business-row counts, then record actual:

- `BACKUP_DURATION_SECONDS`;
- `RESTORE_DURATION_SECONDS`;
- `OBSERVED_RPO`; and
- `OBSERVED_RTO`.

Current backup, restore, and restore-content statuses are `NOT_ATTEMPTED`.

## Required Real-Server Evidence

A future execution may report P3-H PASS only after all inputs are explicitly
provided outside chat/GitHub and all of the following are collected as
redacted evidence from one authorized non-production server:

1. attestation, host identity, OS/Docker/UTC/NTP/firewall baseline;
2. exact source archive and immutable image metadata;
3. empty Greenfield PostgreSQL followed by Flyway V1-V7 and zero-repeat run;
4. isolated migration, read-only app, backup, and recovery roles;
5. denied application write probe;
6. real runtime Secret Store mount and fail-closed Config Tree startup;
7. verified TLS, redirect, authenticated/unauthenticated HTTPS smoke, 429, and redacted logs;
8. official backup/restore to the independent recovery database with content match;
9. admin/database credential rotations and TLS renewal/rotation;
10. service restarts and an explicitly authorized physical/VM server reboot;
11. server-side secret leak candidate count of zero; and
12. cleanup or approved owner-backed continued-running status.

No real-server item above is PASS in this package.

## Round 2 Local Lifecycle Evidence

The exact committed image was built from `git archive <exact-head>` after a
clean-worktree and expected-branch check. An untracked or modified worktree is
blocked before Docker. The image revision label matched the same full Head.

The same disposable PostgreSQL volume then completed:

1. strict Greenfield negative fixtures and clean-inventory acceptance;
2. confirmed first boot through Flyway V1-V7 and app/proxy health;
3. explicit V2 database/admin activation;
4. stack stop with PostgreSQL volume retained;
5. steady-state restart with Flyway `validate`, zero new migrations, V2 active,
   V1 denied, and matching content fingerprint;
6. a reboot-like all-container stop and ordered steady-state restart with the
   same checks;
7. three injected failures with partial-stack/Secret cleanup and primary-volume
   preservation; and
8. final steady-state recovery, non-root Secret readability, Host/TLS, and
   denied-write checks.

This sequence did not restart the host Docker daemon and is explicitly a
reboot-like local simulation, not physical/VM reboot evidence.

## Round 3 Local Integrity Evidence

The disposable runner now also exercises an explicitly confirmed Greenfield
initialization recovery. A real V1-V3 prefix is validated and continued to V7;
a V7 state with missing read-only grants is repaired only after core state
verification. Recovery rejects a missing confirmation, non-contiguous Flyway
history, checksum mismatch, failed migration, unknown business object, unknown
rule default, or any business row. It never calls Flyway baseline, repair, or
clean. The pre-migrate Flyway validation ignores only `*:pending`; the
post-migrate validation remains strict.

Failure injection is measured after cleanup: every project container,
including PostgreSQL, must be absent; the materialized Secret volume must be
absent; and Primary/Backup volumes must remain. Cleanup command or inspection
failure cannot emit PASS. Read-only drift fixtures prove app/backup membership
rejection, denied `SET ROLE`, exact SELECT-only default ACLs, Sequence SELECT
without USAGE/UPDATE, and denied database CREATE/TEMP. The reboot-like path
rechecks all four credential outcomes: V2 admin/database succeed and V1
admin/database are denied.

## Local Validation

- P3-H contract suite: 106 tests, 0 failures, 0 errors; its single full Docker
  lifecycle test is skipped by default and is never represented as PASS when
  skipped.
- Environment-gated P3-H Docker JUnit: `PASS` when explicitly enabled.
- Disposable Compose template smoke:
  `PASS_LOCAL_DISPOSABLE_P3H_TEMPLATE_SMOKE`.
- Disposable resource cleanup: `PASS`.
- Full Maven suite: 3,774 tests, 0 failures, 0 errors, 14 environment-gated
  skips. No skipped test is represented as PASS.
- Delivery, workflow-contract, state, YAML, and diff checks are recorded in
  the PR validation summary for the exact package head.
- The default real-staging runner still returns
  `BLOCKED_MISSING_CONTROLLED_STAGING_INPUT` before access.

The canonical-path delivery check is performed against the exact committed
package head during handoff; a linked worktree is intentionally rejected by
that script's path guard.

## Safety Boundary

- No production server or production database was accessed.
- No real secret or local secret file was read.
- No provider or AI call was made.
- No scheduler was started.
- No order, auto-open, auto-close, auto-reverse, auto-trading, position
  mutation, Push, Telegram, webhook, or email action was introduced or run.
- No production migration or destructive database operation was run.
- P4 remains blocked.

## Decision And Next Task

`OFFLINE_HARNESS: PASS`

`LOCAL_COMPOSE_TEMPLATE_SMOKE: PASS_LOCAL_DISPOSABLE_P3H_TEMPLATE_SMOKE`

`REAL_STAGING_STATUS: BLOCKED_MISSING_AUTHORIZED_INPUT`

`P3H_RESULT: NOT_COMPLETE`

Production readiness remains `BLOCKED`; production deployment cannot
proceed. The next task is **Reviewer P3-H Offline Harness Round 4 Re-review**.
Any later real execution requires a new explicit authorized input set and must
preserve the no-secret-output contract.
