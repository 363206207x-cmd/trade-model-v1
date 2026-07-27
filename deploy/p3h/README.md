# Controlled P3-H Deployment Assets

These files are templates for one explicitly authorized, non-production Linux
staging host. They are not a production deployment recipe and are not usable
without the P3-H attestations, fixed SSH host key, runtime-only secret mount,
and explicit confirmation required by
`scripts/controlled-staging-readonly-deployment-p3h.sh`.

The topology has two networks. Only the reverse proxy joins `p3h_edge` and
publishes ports 80/443. PostgreSQL and the application have no host ports and
join only the internal `p3h_backend` network. The application role is read-only
for business data and has only the exact `tm_user` column/sequence privileges
needed to bootstrap the personal user and update `last_login_at`. It imports
active secrets through Spring Config Tree and keeps all schedulers, AI,
providers, and external calls disabled.

The lifecycle entrypoint requires an explicit mode. `INITIALIZE_GREENFIELD`
performs strict empty-object inventory, four-role/two-database bootstrap,
Flyway V1-V9, business-read-only/auth-session grants, tmpfs Secret materialization,
application health, then proxy health. `RECOVER_GREENFIELD_INITIALIZATION`
requires `I_CONFIRM_RECOVER_CONTROLLED_GREENFIELD_INITIALIZATION` and accepts
only a checksum-valid continuous Flyway V1-VN prefix (or V9 before grants),
the exact versioned rule-default rows and normalized PostgreSQL schema
fingerprint, the exact P3-H role/database identity, known migration objects,
and zero business rows. It continues migrate to V9, runs core verification, refreshes
grants, and then runs full read-only verification. `STEADY_STATE_START` accepts
only an already valid V9 database, including `tm_user`, and uses the same core -> grants -> full
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
and Sequence ACLs are cleared before exact SELECT grants are applied;
PUBLIC table/Sequence privileges and app/backup/PUBLIC column writes are
cleared as well. The only application write exceptions are column-scoped
`tm_user` INSERT for `username`, `password_hash`, `created_at`, and
`last_login_at`; UPDATE of `last_login_at`; and USAGE of `tm_user_id_seq`.
The verifier rejects every other table, column, Sequence UPDATE, schema, or
database write privilege. A reboot-like local restart rechecks the selected
database credential. The current local contract keeps the admin credential at
V1 and records admin rotation as not run; it does not claim that bootstrap
overwrites an existing personal-user password.

The controlled runner validates strict server and Secret Store Attestations
before accepting the staging DNS name, SSH DNS/IPv4 host, and non-reserved
POSIX deployment user. All grammar checks complete before source upload,
`ssh-keyscan`, or SSH access; raw values are never emitted as evidence.

The active database/admin versions are explicit non-sensitive `V1|V2`
settings. A separately confirmed database activation action changes the role
credential; later restarts rematerialize the selected database version and
never silently revert to V1. Personal-user/admin rotation needs a distinct,
approved `tm_user` password-rotation path and is not inferred from secret
materialization. Rotation is a controlled server drill and must produce
redacted evidence before real staging activation is accepted. Only the
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

The current authenticated smoke contract is form login plus server-side
Session and CSRF: anonymous API access is rejected, `/login` supplies the CSRF
token, the smoke stores a Cookie jar, performs authenticated Dashboard/Review
reads, posts CSRF-protected logout, and proves the pre-logout Session no longer
works. `TRADE_MODEL_SMOKE_USERNAME` and `TRADE_MODEL_SMOKE_PASSWORD` are
runtime-only inputs. Basic Auth is not a current P3-H smoke path.

The disposable local runner can prove only
`PASS_LOCAL_DISPOSABLE_P3H_TEMPLATE_SMOKE`. It is not authorized staging
evidence. Real staging remains `BLOCKED_MISSING_AUTHORIZED_INPUT`.
