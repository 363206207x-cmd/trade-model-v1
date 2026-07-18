# Controlled P3-H Deployment Assets

These files are templates for one explicitly authorized, non-production Linux
staging host. They are not a production deployment recipe and are not usable
without the P3-H attestations, fixed SSH host key, runtime-only secret mount,
and explicit confirmation required by
`scripts/controlled-staging-readonly-deployment-p3h.sh`.

The topology has two networks. Only the reverse proxy joins `p3h_edge` and
publishes ports 80/443. PostgreSQL and the application have no host ports and
join only the internal `p3h_backend` network. The application uses a read-only
database role, imports active secrets through Spring Config Tree, and keeps all
schedulers, AI, providers, and external calls disabled.

The lifecycle entrypoint requires an explicit mode. `INITIALIZE_GREENFIELD`
performs strict empty-object inventory, four-role/two-database bootstrap,
Flyway V1-V7, read-only/default grants, tmpfs Secret materialization,
application health, then proxy health. `RECOVER_GREENFIELD_INITIALIZATION`
requires `I_CONFIRM_RECOVER_CONTROLLED_GREENFIELD_INITIALIZATION` and accepts
only a checksum-valid continuous Flyway V1-VN prefix (or V7 before grants),
the exact versioned rule-default rows and normalized PostgreSQL schema
fingerprint, the exact P3-H role/database identity, known migration objects,
and zero business rows. It continues migrate to V7, runs core verification, refreshes
grants, and then runs full read-only verification. `STEADY_STATE_START` accepts
only an already valid V7 database and uses the same core -> grants -> full
sequence. Recovery pre-validates the applied prefix while ignoring only
pending migrations (`*:pending`); post-migrate validation remains strict. No
mode runs baseline, repair, or clean.

All long-running services use `restart: "no"`; systemd is the sole lifecycle
owner. Failed starts stop and remove every project container, including
PostgreSQL, and remove materialized Secret state. PASS is emitted only after
Docker reports zero project containers, no materialized Secret volume, and
the Primary/Backup volumes still present. Source Secret files and persistent
database/backup volumes are preserved.

The app and backup roles may not be members of any other role. Default table
and Sequence ACLs are cleared before exact SELECT-only grants are applied;
PUBLIC table/Sequence privileges and app/backup/PUBLIC column writes are
cleared as well. Effective table permissions, Sequence USAGE/UPDATE, column
writes, and database CREATE/TEMP privileges are denied and verified. A
reboot-like local restart rechecks V2 database/admin success
and V1 database/admin denial before active-version preservation is reported.

The controlled runner validates strict server and Secret Store Attestations
before accepting the staging DNS name, SSH DNS/IPv4 host, and non-reserved
POSIX deployment user. All grammar checks complete before source upload,
`ssh-keyscan`, or SSH access; raw values are never emitted as evidence.

The active database/admin versions are explicit non-sensitive `V1|V2`
settings. A separately confirmed database activation action changes the role
credential; later restarts rematerialize the selected versions and never
silently revert to V1. Rotation is a controlled server drill and must produce
redacted evidence before real staging V2 activation is accepted. Only the
`SYSTEMD_CREDENTIALS` backend is
implemented. Its unit uses `LoadCredentialEncrypted=` and an adapter bound to
`CREDENTIALS_DIRECTORY`; other named backend modes remain blocked. Source
Secret files stay under the approved `/run` mount. An isolated root
materializer copies only application Config Tree values into a Docker tmpfs,
sets UID/GID 10001 and mode 0400, and exits. The non-root app mounts that tmpfs
read-only. Neither source nor materialized Secret data may enter a release,
Docker image, persistent volume, evidence bundle, Git, logs, inspect output, or
process arguments.

Image references are immutable digests. The LAB1 path first builds one
Spring Boot JAR from an exact `git archive` on the controlled host, then uploads
a three-file artifact containing the JAR, `Dockerfile.runtime.p3h`, and a
non-sensitive manifest. The target verifies the archive and JAR SHA-256 values,
prefetches all pinned runtime images, and performs only a runtime-only image
build. The image must carry both the exact source SHA in
`org.opencontainers.image.revision` and the JAR SHA in
`org.example.trademodel.app-jar-sha256`, and must run as UID/GID 10001. Target
VM Maven downloads and source compilation are not an accepted LAB1 success path.

`scripts/p3h-filter-known-hosts.sh` fingerprints each `ssh-keyscan` candidate
line, requires exactly one approved match, and writes only that line to the
actual `UserKnownHostsFile`. The unfiltered candidate file is never trusted.

P4 remains disallowed and production readiness remains blocked.

The disposable local runner can prove only
`PASS_LOCAL_DISPOSABLE_P3H_TEMPLATE_SMOKE`. It is not authorized staging
evidence. Real staging remains `BLOCKED_MISSING_AUTHORIZED_INPUT`.

`scripts/controlled-staging-readonly-deployment-p3h-r1.sh` is a separate,
review-pending remote execution path. Its `LOCAL_LIMA_LAB` target is bound to
the exact disposable LAB1 VM and cannot claim external staging evidence. The
`AUTHORIZED_EXTERNAL_STAGING` target delegates to the original guard and does
not inherit any generated LAB1 input. See
`docs/P3H_LOCAL_LINUX_VM_STAGING_LAB1.md`.
