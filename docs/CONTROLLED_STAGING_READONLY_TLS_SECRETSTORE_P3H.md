# Controlled Staging Read-Only TLS And Secret-Store Evidence P3-H

Status: `BLOCKED_MISSING_CONTROLLED_STAGING_INPUT`

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
harness, immutable deployment templates, and offline contract tests. It does
not claim that a staging server, TLS endpoint, Secret Store, rotation drill,
backup/restore drill, or reboot was exercised.

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

## Offline Harness Evidence

| Contract | Offline result | Server evidence |
| --- | --- | --- |
| Missing input stops before access | `GUARD_PASS` | `NOT_ATTEMPTED` |
| Staging and Secret Store attestations | strict parser and allowlist prepared | `NOT_VALIDATED_INPUT_MISSING` |
| SSH key and pinned host-key policy | `GUARD_PASS` | `NOT_ATTEMPTED` |
| Production-like target rejection | `GUARD_PASS` | `NOT_ATTEMPTED` |
| Secret backend and `/run/` mount policy | `GUARD_PASS` | `NOT_ATTEMPTED` |
| Spring Config Tree injection | `GUARD_PASS_OFFLINE` | `NOT_ATTEMPTED` |
| Missing Config Tree secret | `FAIL_CLOSED_OFFLINE` | `NOT_ATTEMPTED` |
| Immutable image sources and exact Git archive | contract prepared | `NOT_BUILT_ON_SERVER` |
| Internal backend network and proxy-only ports | `GUARD_PASS_OFFLINE` | `NOT_ATTEMPTED` |
| Scheduler, AI, provider and external calls | template locked off | `NOT_ATTESTED_ON_SERVER` |
| Verified TLS and HTTPS smoke | harness prepared | `NOT_ATTEMPTED` |
| Access-log redaction and rate limiting | template/contract prepared | `NOT_ATTEMPTED` |
| Secret rotations | evidence validator prepared | `NOT_ATTEMPTED` |
| Backup and recovery | documented against official scripts | `NOT_ATTEMPTED` |
| Service/server reboot | reboot evidence contract prepared | `NOT_ATTEMPTED` |
| Secret leak scan | fixture-level contract tested | `NOT_ATTEMPTED_ON_SERVER` |

Offline `GUARD_PASS` means only that the repository contract fails closed. It
must not be translated to a real-server PASS.

## Deployment Assets

`deploy/p3h/docker-compose.p3h.yml` defines digest-pinned PostgreSQL 16,
Flyway, application, and Nginx services. PostgreSQL and the application expose
no host ports. The backend network is internal; only the reverse proxy
publishes 80/443. The application runs read-only with Flyway and SQL init
disabled, and all schedulers, AI, providers, and external-call switches off.

Secrets are file-mounted from the approved runtime directory. The application
uses `configtree:/run/secrets/config/` for:

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

Current TLS, redirect, authenticated HTTPS smoke, negative-auth smoke, rate
limit, certificate rotation/renewal, and proxy log evidence are all
`NOT_ATTEMPTED` because no controlled server input was supplied.

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

No item above is PASS in this package.

## Local Validation

- P3-H contract tests: 30 tests, 0 failures, 0 errors, 0 skipped.
- Full Maven suite: 3,698 tests, 0 failures, 0 errors, 13 environment-gated
  skips.
- The existing Docker/Testcontainers checks reported no available local
  Docker daemon and remained skipped. They are not represented as PostgreSQL
  or server PASS evidence.
- Shell syntax, workflow contract, YAML parsing, and `git diff --check` passed.
- The default P3-H runner again returned
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

`P3H_RESULT: BLOCKED_MISSING_CONTROLLED_STAGING_INPUT`

Production readiness remains `BLOCKED`; production deployment cannot
proceed. The next task is **Reviewer Controlled Staging P3-H Evidence Review
and PR Merge Readiness**. Any later real execution requires a new explicit
authorized input set and must preserve the no-secret-output contract.
