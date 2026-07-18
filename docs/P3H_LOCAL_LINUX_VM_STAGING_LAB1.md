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
Bootstrap starts only the minimal Linux VM under a 20-minute bound. Lima's
internal limit is 21 minutes, so it cannot preempt that external contract.
Package and Docker installation then runs once in a separate, sanitized
`GUEST_PACKAGE_AND_DOCKER_PROVISION` stage with a 30-minute bound. The complete
bootstrap process tree has a 60-minute hard limit and uses 15-second polling.
Failure invokes ownership-checked LAB1 cleanup rather than leaving a partial
VM.
Remote-stage failures preserve only an enumerated failure reason and a SHA-256
of the repository-external sanitized failure summary; raw stage output remains
temporary and is removed during cleanup.
Repository and host runtime directories are not mounted into the VM. The exact
committed source is transferred as a `git archive` over pinned SSH, and its
SHA-256 is recomputed before extraction and image construction.
The `current` release link must resolve exactly to `releases/<exact-head>`;
arbitrary or unversioned release paths fail closed.
R1 has one 180-minute hard bound. Application image construction is limited to
45 minutes, each pinned runtime-image pull to 20 minutes, all runtime-image
pulls to 40 minutes, deployment and backup/restore to 30 minutes each, and the
combined rotation, VM reboot, and post-reboot verification phase to 30 minutes.
No operation is automatically retried. Docker
operations run in a dedicated child process group; timeout handling sends TERM,
waits at most 15 seconds, then sends KILL to that exact group. The runner polls
every 15 seconds and emits a sanitized heartbeat every 60 seconds.

Image construction and pulls fingerprint the target image, Docker storage,
BuildKit storage, and containerd storage. Every progress probe has its own
five-second TERM/KILL bound. Failed or timed-out probes never reset the progress
clock. Fifteen minutes without a successful fingerprint change returns
`BLOCKED_NO_PROGRESS_TIMEOUT` and triggers scoped cleanup. The
temporary build output is deleted and is never emitted or preserved as
evidence. Before image construction, a bounded preflight requires at least
4096 MiB available memory, 15 GiB available disk, an active Docker daemon, DNS,
the required registry, and Maven Central. The same lifecycle script then runs
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
private temporary `PGPASSFILE`. All five libpq fields escape colon and
backslash, and password files reject empty, embedded CR/LF, symlink, directory,
ownership, and permission violations. R1 does not place passwords in Docker
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

## Hung Run Abort Evidence

The pre-hardening run produced no final result and no failure summary. The task
was explicitly classified as exceeding its acceptable global run contract and
was aborted. Exact PID inspection confirmed that PID `82414` was the R1 runner;
TERM stopped it within 15 seconds, so KILL was not used. Telemetry captured
before cleanup showed the application unit inactive, no Java process, and one
Docker client executing the exact revision-labeled application image build.
The unique blocked stage is therefore `APPLICATION_IMAGE_BUILD`.

The operator report classified elapsed time as greater than ten hours. The PID
sample available in this resumed execution context showed `00:37:44`; both are
retained rather than silently reconciling unlike observations. Neither is a
PASS. VM-level Docker, memory, and disk snapshots requested after termination
are `NOT_AVAILABLE_AFTER_FAIL_SAFE_CLEANUP` because the existing EXIT trap had
already destroyed the VM. No values are fabricated.

`R1_RESULT: BLOCKED_GLOBAL_TIMEOUT_EXCEEDED`

`R1_PASS: NO`

`CURRENT_BLOCKED_STAGE: APPLICATION_IMAGE_BUILD`

`LAB_VM: ABSENT`

`LAB_CONTAINER_COUNT: 0`

`LAB_NETWORK_COUNT: 0`

`LAB_VOLUME_COUNT: 0`

`LAB_SECRET_FILES: ABSENT`

`UNRELATED_RESOURCES_TOUCHED: NO`

## Historical Bounded Bootstrap Evidence

The timeout and no-progress contract is implemented and its offline contract
tests pass. Commit `9223f8d6a00935391cd1415944ae5b962f23c1b1` was then used
for the single permitted clean attempt. The attempt created a new Lima VM, but
Lima did not complete startup before its own 15-minute bound and returned
`BLOCKED_LIMA_START_TIMEOUT_OR_FAILURE`. The R1 runner never started, and no
deployment, Flyway, backup/restore, rotation, or reboot result is represented
as PASS. A second attempt was not run.

`P3H_LAB_RESULT: BLOCKED_LOCAL_LAB_BOOTSTRAP`

`R1_RESULT: BLOCKED`

`R1_PROCESS_STARTED: NO`

`BLOCKED_STAGE: VM_BOOTSTRAP`

`BLOCKED_REASON: BLOCKED_LIMA_START_TIMEOUT_OR_FAILURE`

`STAGE_ELAPSED_MINUTES: 16`

`GLOBAL_ELAPSED_MINUTES: 16`

`GLOBAL_TIMEOUT_TRIGGERED: NO`

`NO_PROGRESS_TIMEOUT_TRIGGERED: NO`

`LAB_VM_CLEANUP: PASS`

`LAB_DOCKER_CLEANUP: PASS`

`LAB_SECRET_CLEANUP: PASS`

`RAW_LOGS_EXPOSED: NO`

`RETRY_COUNT: 0`

`REAL_EXTERNAL_STAGING_STATUS: NOT_RUN`

`P3H_RESULT: NOT_COMPLETE`

`P4_ALLOWED: NO`

`PRODUCTION_READINESS: BLOCKED`

The elapsed values are rounded up from the last bounded heartbeat and terminal
event. Cleanup verification found zero R1 or bootstrap processes, no LAB VM,
no LAB root, and no dedicated LAB NTP service. Because the VM was destroyed,
its LAB-scoped containers, network, volumes, identity, certificates, generated
credentials, and attestations are absent. No unrelated resource was selected
for cleanup.

## Reviewer Round 1 Authorized Attempt

Commit `4ee04d8cc58026b5bd2b7a8fde058ed00e1c5557` contains the Round 1
bootstrap, timeout, cleanup, progress-probe, and `.pgpass` integrity fixes. Its
offline process-tree tests prove that a direct child and descendants are
terminated at the global deadline, TERM escalates to KILL after a finite grace
period, unrelated process groups are preserved, hanging cleanup is bounded,
and hanging progress probes cannot suppress the no-progress or global limit.
The exact Head passed local tests, workflow contract, Lima validation, GitHub
CI, and the canonical-path delivery check. The isolated-worktree delivery check
returned `WRONG_PROJECT_PATH` as expected; it is not represented as a second
delivery PASS or failure.

The one newly authorized clean run started from zero LAB processes, no LAB VM,
no LAB root, no LAB NTP job, and no LAB containers, network, or volumes. Minimal
VM startup passed. The independent package and Docker stage passed in 844
seconds, and bootstrap reached `17_OF_17_READY`. R1 then started, but the
sanitized remote-preflight evidence was classified
`P3H_EVIDENCE_REDACTION: BLOCKED_UNSAFE_OR_INCOMPLETE`. The runner failed closed
as `BLOCKED_REMOTE_PREFLIGHT_EVIDENCE` before application image construction.
Raw preflight output was neither emitted nor retained, so no narrower reason is
invented. No second attempt ran.

`REVIEWER_AUTHORIZED_ATTEMPT_USED: 1_OF_1`

`SECOND_ATTEMPT: NO`

`MINIMAL_VM_START: PASS`

`GUEST_PACKAGE_AND_DOCKER_PROVISION: PASS`

`GUEST_PROVISION_ELAPSED_SECONDS: 844`

`P3H_LAB_INPUT_STATUS: 17_OF_17_READY`

`R1_PROCESS_STARTED: YES`

`R1_RESULT: BLOCKED`

`BLOCKED_STAGE: REMOTE_PREFLIGHT`

`BLOCKED_REASON: BLOCKED_REMOTE_PREFLIGHT_EVIDENCE`

`STAGE_ELAPSED_MINUTES: 1`

`GLOBAL_ELAPSED_MINUTES: 1`

`GLOBAL_TIMEOUT_TRIGGERED: NO`

`NO_PROGRESS_TIMEOUT_TRIGGERED: NO`

`APPLICATION_IMAGE_BUILD: NOT_RUN`

`REMOTE_EXECUTION_STATUS: BLOCKED_REMOTE_PREFLIGHT_EVIDENCE`

`VM_REBOOT: NOT_RUN`

`LAB_VM_CLEANUP: PASS`

`LAB_DOCKER_CLEANUP: PASS`

`LAB_SECRET_CLEANUP: PASS`

`RESOURCE_CLEANUP: PASS`

`RAW_LOGS_EXPOSED: NO`

`REAL_EXTERNAL_STAGING_STATUS: NOT_RUN`

`P3H_RESULT: NOT_COMPLETE`

`P4_ALLOWED: NO`

`PRODUCTION_READINESS: BLOCKED`

Post-cleanup verification again found zero R1/bootstrap processes, no LAB VM,
no LAB root, and no dedicated LAB NTP service. The PASS labels above apply only
to bootstrap and its local input preparation. They do not promote remote
preflight, deployment, Flyway, backup/restore, rotation, reboot, P3-H, P4, or
production readiness.

## Safety Boundary

The application keeps providers, AI, schedulers, Push, Telegram, webhook,
email, trading, order execution, automatic position actions, and production
database access disabled. LAB1 evidence is partial local VM evidence only. It
does not complete P3-H external staging, permit P4, or establish production
readiness.
