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
application health, then proxy health. `STEADY_STATE_START` accepts only an
already valid V7 database, runs Flyway checksum validation, rejects failed or
unexpected migrations, verifies and refreshes read-only grants, rematerializes
the selected active Secret versions, then starts app/proxy in order. It never
runs baseline, repair, or clean.

All long-running services use `restart: "no"`; systemd is the sole lifecycle
owner. Failed starts remove partial app/proxy/holder and materialized Secret
state while preserving source Secret files and the PostgreSQL data volume.

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

Image references are immutable digests. The application image is built from an
exact `git archive` using `Dockerfile.p3h` and must carry the exact source SHA in
`org.opencontainers.image.revision`.

`scripts/p3h-filter-known-hosts.sh` fingerprints each `ssh-keyscan` candidate
line, requires exactly one approved match, and writes only that line to the
actual `UserKnownHostsFile`. The unfiltered candidate file is never trusted.

P4 remains disallowed and production readiness remains blocked.

The disposable local runner can prove only
`PASS_LOCAL_DISPOSABLE_P3H_TEMPLATE_SMOKE`. It is not authorized staging
evidence. Real staging remains `BLOCKED_MISSING_AUTHORIZED_INPUT`.
