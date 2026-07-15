# Greenfield PostgreSQL First-Boot Rehearsal P3-G

Status: `PASS_LOCAL_CONTROLLED_GREENFIELD_REHEARSAL` on the package branch;
the package is not effective until its Draft PR is reviewed and merged.

Production Deployment Readiness: `BLOCKED`

P4 Allowed: `NO`

## Scope And Provenance

- Base merged main: `72b5bc83f4d670d4adebc03f5fe28e0bb9bba535`.
- Branch: `codex/greenfield-postgresql-first-boot-rehearsal-p3g`.
- Approved provenance decision: `TMV1-GREENFIELD-20260715-001`.
- Data mode: `GREENFIELD_NEW_DATABASE`.
- Initial database state: `EMPTY`.
- Environment: disposable local Docker resources bound only to `127.0.0.1`.
- PostgreSQL image: digest-pinned PostgreSQL 16 (`16.14` in the controlled run).
- No production server, production database, production credential, real
  provider, scheduler, trading path, or external send was used.

This package proves a local Greenfield first-boot and recovery rehearsal. It
does not prove server deployment, secret-store injection, live provider
availability, release-owner deployment approval, or production readiness.

## Guarded Runner

Run only from the exact clean package commit:

```bash
P3G_CONFIRM=I_CONFIRM_LOCAL_GREENFIELD_EMPTY_DATABASE_REHEARSAL \
  bash scripts/controlled-greenfield-first-boot-rehearsal-p3g.sh
```

Without the exact confirmation the runner reports
`BLOCKED_CONFIRMATION_REQUIRED` before Docker or database access. It refuses
non-localhost targets, non-fixed ports, non-fixed database names, a dirty
worktree, the wrong branch, or an unexpected base. Every bounded run uses
unique containers, an internal Docker network, a named volume, random
credentials, and cleanup traps for success, failure, `SIGINT`, and `SIGTERM`.

Docker Desktop records the requested loopback-only binding for a container on
an `--internal` network in `HostConfig.PortBindings`, but does not expose an
effective public port. The runner verifies the configured
`127.0.0.1:18085` boundary without weakening the internal-network policy.
Application health and API requests come from a digest-pinned JDK Smoke client
attached only to the same internal network. The official `prod-smoke.sh` uses
its explicit `FETCH` phase there, then its unchanged Python safety validator
runs on the host in `VALIDATE` phase over transient response files. The
empty-state validator consumes those same temporary files. They are deleted
on exit and are never copied into evidence.

The Flyway action test runs offline in the same digest-pinned JDK image while
sharing the PostgreSQL container network namespace and connecting only to
`127.0.0.1:55435`. It mounts the exact committed Git archive, Maven binary, and
artifact repository read-only, copies them into its disposable layer, and
removes repository-origin tracking only from that disposable copy so offline
Maven does not depend on a host mirror ID. It never mounts Maven settings or a
credential file. Neither action uses Docker host networking or gives the
application external egress.

The application Docker build stage uses a BuildKit Maven cache mount and skips
test compilation; the complete host test suite and bounded Flyway action test
are separate gates. The final runtime stage copies only the packaged
application JAR and does not contain Maven or its repository.

Local evidence is written under ignored path
`.runtime/postgresql-p3g-rehearsal/`. Evidence contains aggregate status,
hashes, schema metadata, and fingerprints only. It excludes passwords, API
keys, complete JDBC URLs, absolute user paths, raw HTTP responses, and business
row contents.

On macOS, Docker Desktop can block while bind-mounting a workspace evidence
directory carrying the `com.apple.provenance` attribute. The PG16 Ops Client
therefore writes the custom-format dump to its runner-owned temporary bind
mount. After `prod-backup.sh` returns successfully, the runner copies that
artifact into the ignored evidence backup directory, applies mode `0600`, and
computes its SHA-256. Restore continues from the same temporary mount; neither
operational script is rewritten or bypassed.

## Observed Evidence

| Gate | Controlled result |
| --- | --- |
| Pre-migration schema | `EMPTY` |
| Pre-migration business rows | `0` |
| Pre-migration Flyway history | `ABSENT` |
| Fresh Flyway V1 through V7 | `PASS`, seven migrations |
| Repeat Flyway migrate | `ZERO_MIGRATIONS` |
| Final Flyway version | `7` |
| Post-migration runtime business rows | `0` |
| Versioned seed allowlist | `tm_rule_config=59` |
| PostgreSQL restart persistence | `PASS`; structure/content fingerprints match |
| Repository `prod-backup.sh` | `PASS_LOCAL_CONTROLLED` |
| Backup format | PostgreSQL custom format |
| Repository `prod-restore.sh` | `PASS_LOCAL_CONTROLLED` |
| Primary/recovery structure | `MATCH` |
| Primary/recovery full content | `MATCH` |
| Flyway history/schema types/historical inventory | `MATCH` |
| Migration/application/backup/recovery roles | separated and narrowly scoped |
| Application database role | `READ_ONLY` |
| Read-only write probe | `DENIED` (`25006` or `42501`) |
| Exact committed application image | `PASS_EXACT_COMMITTED_GIT_ARCHIVE` |
| Primary first boot | `PASS` |
| Primary application restart | `PASS` |
| Recovery application smoke | `PASS` |
| Empty Dashboard/Review Center/Run Baseline | `PASS_FAIL_CLOSED` |
| Primary/recovery application content fingerprints | `MATCH` |
| External network egress | blocked by internal Docker network |
| Compose config expansion | `PASS`; expanded config discarded |
| Container/network/volume cleanup | `PASS` |

The migration allowlist is exact: V3, V5, and V6 insert 19, 16, and 24 rows
respectively into `tm_rule_config`, for 59 total. Every other `tm_*` table
remained empty. Runtime initialization was not counted as migration seed data.

## Application Safety Contract

The application used the `prod` profile with Flyway and SQL initialization
disabled, Hikari read-only enabled, all schedulers disabled, all provider and
AI external calls disabled, and a non-root `app` container user.

The disabled Push Recheck scheduler now adopts configured defaults without
bootstrapping mutable dispatch-config rows. Its existing enabled path still
loads or initializes runtime configuration. This preserves the enabled
behavior while making the disabled production-profile contract startup-safe
for a genuinely read-only application role.

The official `prod-smoke.sh` ran against both Primary and Recovery. The
empty-state validator required:

- no positions, reviews, analyses, decisions, execution plans, monitor alerts,
  push records, or Hot Reset records;
- no fabricated plan fields or asset conclusions;
- AI `DISABLED` or `NOT_CALLED`;
- no provider or Telegram status reported as connected; and
- all manual-review, non-executable, non-trading, no-order, no-send, and
  no-position-mutation safety flags to remain true.

## Docker Context Boundary

The application image was not built from the worktree. The runner required a
clean commit, created a temporary context with `git archive <exact-head>`, ran
`check-docker-context-safety.sh`, labeled the image with that exact revision,
and removed the context after use. `.runtime`, backup directories, dumps,
attestations, and secret environment files are excluded by both the archive
boundary and `.dockerignore`.

## Remaining Gates

1. Independent review of this evidence and merge to main; an open Draft PR is
   not effective evidence.
2. Real deployment-host topology, TLS/reverse-proxy, secret-store injection,
   credential rotation, and server smoke evidence.
3. Approved provider policy/live evidence for dependencies required by the
   release owner.
4. Operational owners, rollback decision authority, incident handling, and
   explicit release-owner approval.
5. A separately authorized P4 gate. P3-G does not authorize P4.

Decision: Production Deployment Readiness remains `BLOCKED`; production
deployment cannot proceed.

Next task: Reviewer Greenfield P3-G Evidence Review and PR Merge Readiness.
