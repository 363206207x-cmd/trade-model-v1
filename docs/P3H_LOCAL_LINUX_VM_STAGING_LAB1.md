# P3-H LAB1 Local Disposable Linux Staging

## Decision

`P3-H-LAB1` is an explicitly authorized, local-only, non-production deployment
lab. It uses a disposable Lima Linux VM to exercise the remote P3-H deployment
path without connecting to an external server, production database, production
traffic, or real provider credentials.

The authorization reference is `P3H-LAB1-USER-AUTH-20260717`. It applies only
to the VM named `trade-model-p3h-staging-lab` on the operator's current Mac.
It is not an organizational staging approval and cannot be reused for an
external target.

## Target Separation

The independent R1 runner accepts exactly one explicit target class:

- `LOCAL_LIMA_LAB`: requires the exact VM name, dedicated LAB1 identity,
  LAB1 attestations, internal CA, nonfunctional provider values, local loopback
  SSH mapping, and destructive cleanup policy.
- `AUTHORIZED_EXTERNAL_STAGING`: delegates to the previously merged P3-H guard
  and keeps its complete authorized-input contract. It never generates an
  address, identity, owner reference, attestation, or secret.

There is no automatic fallback between target classes. A local LAB1 attestation
is rejected for an external target. Local evidence always reports
`REAL_EXTERNAL_STAGING_STATUS: NOT_RUN`.

## VM And Access Contract

The VM contract uses Lima's official Debian 12 template with 4 CPUs, 8 GiB
memory, 40 GiB disk, and systemd. Rootful Docker Engine and Docker Compose v2
come from Docker's official Debian repository. The VM also requires OpenSSH,
UTC, and synchronized time.
Bootstrap has an 85-minute outer bound and a 75-minute Lima provisioning bound;
failure invokes ownership-checked LAB1 cleanup rather than leaving a partial VM.
Remote-stage failures preserve only an enumerated failure reason and a SHA-256
of the repository-external sanitized failure summary; raw stage output remains
temporary and is removed during cleanup.
Repository and host runtime directories are not mounted into the VM. The exact
committed source is transferred as a `git archive` over pinned SSH, and its
SHA-256 is recomputed before extraction and image construction.
The `current` release link must resolve exactly to `releases/<exact-head>`;
arbitrary or unversioned release paths fail closed.
The initial remote deployment has a fixed 210-minute outer bound. Image
construction permits at most two 60-minute attempts inside the same disposable
VM so a timeout, transient network failure, or registry rate limit can reuse
only the non-secret BuildKit and Maven download cache. Maven, storage, and
unknown failures stop immediately; a second transient failure returns its
enumerated `BLOCKED_IMAGE_BUILD_*` category, and an outer-stage timeout returns
`BLOCKED_REMOTE_STAGE_TIMEOUT`. The temporary build output is deleted and is
never emitted or preserved as evidence. There is no unbounded retry. This is
necessary because a fresh VM must fetch pinned container layers and build the
exact source archive. Before image construction, the same lifecycle script runs
a config-only Compose check directly and through a transient unit with the same
systemd user/group and hardening properties. Neither check can start containers,
and each emits only an enumerated category. A failed systemd start likewise exposes only the
allowlisted Compose failure step or status as a `BLOCKED_*` reason; temporary
config output, unit journals, and container logs are never copied into evidence.
Flyway uses the immutable official `flyway/flyway:12.11.0-alpine` multi-architecture
OCI index so the same pin resolves native `linux/arm64` and `linux/amd64`
manifests. The earlier single-architecture Flyway 10 digest could not execute in
the arm64 lab and was rejected rather than emulated. Flyway failures are reduced
to an allowlisted category such as architecture, authentication, connection,
role, permission, migration file, checksum, SQL compatibility, or unsupported
database; the SQL state, statement, and journal content stay local to the
disposable VM and are deleted.
Database fingerprints reuse the P3/P3-G structure and seeded content
fingerprint SQL through the read-only backup role rather than maintaining a
second dynamic-row hashing implementation. Failures expose only
`STRUCTURE_QUERY` or `CONTENT_QUERY`; query output and database content never
enter failure evidence.
The hardened unit keeps `ProtectHome=yes`; Docker CLI state is instead isolated
in its 0700 systemd `RuntimeDirectory` and is removed with the unit lifecycle.
TLS V2 activation rewires only encrypted credential paths,
rematerializes into the runtime tmpfs, reseals it read-only, and is required to
survive the VM reboot.

The deployment account is the non-root `p3h-deploy` user. The bootstrap creates
one LAB1 Ed25519 identity and never searches or reads the operator's existing
SSH directory. The SSH host key fingerprint is obtained independently through
the Lima console and must exactly match the only network-scanned key accepted
by `p3h-filter-known-hosts.sh`.

## Secret Contract

Random database/admin V1 and V2 credentials, backup/recovery credentials, and
nonfunctional Binance placeholders are generated for this lab only. A short
lived internal CA signs two server certificates for the exact DNS SAN
`trade-staging.lab.test`.

Plaintext generation material is transient and removed after provisioning.
Persistent guest material consists only of machine-bound encrypted systemd
credential files. The credential holder and application service use
`LoadCredentialEncrypted=`; runtime files are mounted by systemd under
`/run/credentials`. Compose receives Secret file paths, never Secret values.
Official backup and restore scripts support password-file input and create a
private temporary `PGPASSFILE`; R1 does not place passwords in Docker
environment values, command arguments, image layers, logs, or evidence.

TLS verification uses the generated CA and the approved hostname. `curl -k`,
`--insecure`, global DNS changes, and `/etc/hosts` changes on macOS are
prohibited.

## Execution Stages

The controlled run performs these fail-closed stages:

1. Validate all 17 generated non-secret input bindings and strict LAB1 target
   ownership.
2. Match the console and network SSH host-key fingerprints.
3. Run the merged remote host and runtime-secret preflight.
4. Transfer and verify the exact source archive and build a revision-labeled,
   non-root application image.
5. Start Greenfield PostgreSQL through systemd Credentials, create four scoped
   roles and two databases, migrate Flyway V1 through V7, and prove the
   application role is read-only.
6. Repeat the steady-state start and prove zero new migrations.
7. Validate TLS 1.2, TLS 1.3 when supported by the client, redirect, rejected
   unknown host, denied unauthenticated API, authenticated dashboard, empty
   fail-closed dashboard, and HTTP 429 rate limiting.
8. Run `scripts/prod-backup.sh`, restore with `scripts/prod-restore.sh` into the
   independent recovery database, and compare structure and full content
   fingerprints.
9. Rotate application admin and database credentials from V1 to V2, prove V2
   succeeds and V1 is denied, rotate the TLS certificate, and restart through
   systemd.
10. Reboot the actual Linux VM with `systemctl reboot`, require a changed kernel
    boot ID, re-pin the host key, and rerun V2/V1, TLS, Flyway, read-only,
    content, dashboard, and leak checks.
11. Stop the stack, delete only LAB1 containers/volumes/network/image, remove
    the encrypted credentials and deployment release, destroy the exact Lima
    VM, and remove the dedicated identity, CA material, inputs, and original
    attestations.

Any stage failure prevents later PASS labels and invokes the same scoped
cleanup. The redactor preserves only allowlisted status fields and hashes.

## Evidence Status

At the implementation checkpoint, contract tests are present and the mandatory
non-skipped Lima run is pending. This section must be replaced with measured
results before the draft PR can request evidence review.

`P3H_LAB_RESULT: PENDING_NON_SKIPPED_RUN`

`REAL_EXTERNAL_STAGING_STATUS: NOT_RUN`

`P3H_RESULT: NOT_COMPLETE`

`P4_ALLOWED: NO`

`PRODUCTION_READINESS: BLOCKED`

## Safety Boundary

The application keeps providers, AI, schedulers, Push, Telegram, webhook,
email, trading, order execution, automatic position actions, and production
database access disabled. LAB1 evidence is partial local VM evidence only. It
does not complete P3-H external staging, permit P4, or establish production
readiness.
