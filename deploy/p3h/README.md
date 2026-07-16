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

The deterministic startup chain is PostgreSQL health, Greenfield emptiness
preflight, four-role bootstrap, Flyway V1-V7, read-only/default grants, tmpfs
Secret materialization, application health, then proxy health. A failed
one-shot stage prevents the application and proxy from starting.

The committed Compose file starts with the V1 admin/database secret versions.
Rotation is a controlled server drill and must produce redacted evidence before
any V2 activation is accepted. Only the `SYSTEMD_CREDENTIALS` backend is
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

P4 remains disallowed and production readiness remains blocked.

The disposable local runner can prove only
`PASS_LOCAL_DISPOSABLE_P3H_TEMPLATE_SMOKE`. It is not authorized staging
evidence. Real staging remains `BLOCKED_MISSING_AUTHORIZED_INPUT`.
