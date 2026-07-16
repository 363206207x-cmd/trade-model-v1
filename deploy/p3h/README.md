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

The committed Compose file starts with the V1 admin/database secret versions.
Rotation is a controlled server drill and must produce redacted evidence before
any V2 activation is accepted. Secret files are supplied by the approved
backend under `/run`; this directory must never be copied into a release,
Docker build context, evidence bundle, Git, or logs.

Image references are immutable digests. The application image is built from an
exact `git archive` using `Dockerfile.p3h` and must carry the exact source SHA in
`org.opencontainers.image.revision`.

P4 remains disallowed and production readiness remains blocked.
