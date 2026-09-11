# Asset-card native Staging attachments

These are definitions and isolated local tests for the existing
`rine-logic.service` / `/opt/rine-logic/current/app.jar` base chain. They do not
replace the main unit, scheduler, release system or readiness script. This
package grants no server, database-role/ACL, credential/model installation,
service reload/restart or deployment execution. Business PR #1295 remains Draft.

Current final acceptance target: `NATIVE_SYSTEMD_JAR_RUNTIME`, Linux x86_64,
Java 17, standard application JAR, both UBJ models, independent Beta and
actual libgomp/XGBoost loading. Mac ARM, cached container checks and readiness
HTTP 200 are not that result. Dockerfile results are container corroboration only.
`MODEL_MODE=SHADOW`, `PRODUCTION_MODEL_READY=NO`, `BUSINESS_PR_MERGE=NO`,
`STAGING_DEPLOYMENT=NO`, `REAL_DATABASE_PERMISSION_CHANGE=NO`,
`SYSTEMD_CHANGE_EXECUTION=NO`. No production model is manufactured by these tools.

Run the complete `./mvnw test` regression with the required local-test JVM parameters
and **without** Git-location environment overrides. After that exact source has passed
and the candidate commit is clean, build the standard JAR in a separate packaging process:

```sh
card_build_git_dir=$(git rev-parse --absolute-git-dir)
card_build_work_tree=$(pwd -P)
env GIT_OPTIONAL_LOCKS=0 GIT_DIR="$card_build_git_dir" GIT_WORK_TREE="$card_build_work_tree" \
  ./mvnw -Passet-card-native-evidence -DskipTests package
```

Skipping a second test execution in this packaging command is not a substitute for the
completed full regression. Never pass these Git-location overrides to a test process:
tests that create disposable repositories must retain their own repository identity.
The plugin otherwise resolves linked worktrees to the main checkout; the explicit paths
above bind the actual worktree without supplying or fabricating SHA/dirty values.
This profile records only the actual full Git SHA and dirty state. Never copy, invent or hand-write `git.properties`;
the probe refuses missing/dirty identity, and prediction additionally matches the
external candidate SHA. A dirty WIP build is not final native evidence. The
default build and Dockerfile build are not silently switched to this profile.

## Files and trust boundary

Copy `asset-card-runtime-manifest.template` to an independently reviewed,
root-owned non-secret identity document outside Git. The parser accepts exact
known `KEY=VALUE` fields only, not shell syntax, duplicate fields or placeholders.
Use actual reviewed SHA-256 values for the main unit, scheduler drop-in,
readiness script, application JAR and release metadata. The independently observed
native path is `/opt/rine-logic/current/deployment-metadata.txt`; select
`RELEASE_METADATA_FORMAT=KEY_VALUE_V1`. It contains exactly `MERGED_MAIN_SHA`
(40 lowercase hexadecimal characters), `ARTIFACT_SHA256` (64 lowercase hexadecimal
characters, equal to the verified JAR hash), and `DEPLOYED_AT` (a valid UTC instant
in `YYYY-MM-DDTHH:mm:ssZ` format). The file is parsed as data, never sourced;
duplicate, missing, additional or malformed fields fail closed. Its separately
reviewed SHA binds the full text. Metadata and JAR must resolve beside each other
in the same protected release directory, including when the external `current`
pointer is a symlink. Observed server hashes are not built-in defaults: each
release still requires independent exact identity checks. A mismatch stops;
never redirect the manifest to another file or rewrite the external release
contract. Numeric service UID
must differ from the root file-owner UID. Never put passwords, tokens, host keys,
credential hashes, active.env/ai.env contents or environment dumps in this file.

Existing `/opt/rine-logic/current` is owned by the external release system. Its
resolved JAR must remain inside `/opt/rine-logic`, root-owned and non-writable
by the service user, and match the reviewed JAR SHA. No script replaces it.
The model root, credential parent and existing unit-drop-in directory must
already exist, be root-owned and have no group/other write permission.

All shell entrypoints default to CHECK_ONLY or DRY_RUN and share:

```text
bash <entrypoint> --manifest /reviewed/non-secret/runtime.manifest
```

`asset-card-runtime-preflight.sh` checks file/owner/permission/base SHA identity,
credential metadata only, Linux x86_64, Java 17 and libgomp availability. A
present immutable model pointer also receives checksum-only verification.
It never opens or hashes the credential contents, contacts a database or starts
the app. `NATIVE_STATUS=NOT_EXECUTED` remains explicit until an actual predictive
probe succeeds; metadata preflight does not certify model qualification.

## Dedicated database role

`asset-card-role-bootstrap.sql` and `asset-card-role-verify.sql` are reviewed
psql source files for an independently authorized administrator. They are not
invoked by any installer. Bootstrap runs its preconditions before creating
`rine_asset_card_writer`; an existing role is verify-only, never altered.
Passwords are not included or set by bootstrap. Provisioning its credential
through the separately controlled database administration channel is not part
of these scripts.

| Table | Required effective rights | Forbidden |
|---|---|---|
| tm_asset_card_snapshot | SELECT, INSERT, UPDATE | DELETE and all extras |
| tm_asset_card_spot_bar | SELECT, INSERT, DELETE | UPDATE and all extras |
| tm_asset_card_feature_history | SELECT, INSERT, DELETE | UPDATE and all extras |

No sequence, other business table (including column grants), CREATE, TEMP,
TRUNCATE, REFERENCES, TRIGGER, role membership/inheritance/SET ROLE, database,
schema or relation ownership or GRANT OPTION is allowed. CONNECT and public-schema USAGE are
required. PUBLIC grants are effective grants: PostgreSQL has no per-role DENY.
Conflicting old PUBLIC permissions therefore produce a refusal, not an old-ACL
REVOKE. In particular a database with default PUBLIC TEMP needs separate
base-database authorization/hardening; this package cannot change it. Old roles,
ACLs and data are unchanged. The optional, separately approved
`-v card_reconcile_new_table_acl=true` path revokes only inherited
SELECT/INSERT/UPDATE/DELETE from `rine_app` on the three verified V24 tables,
after checking V24 SUCCESS and `rine_migrator` ownership. The default invocation
does not do that. It never changes the pre-existing default ACL, other tables,
role membership or PUBLIC permissions; inherited residual access refuses the
operation. The three card objects must be actual ordinary
`public` tables from V24, not same-named views, foreign tables or other relations.
Callable user-domain `SECURITY DEFINER` routines are also refused because they
can delegate an owner's business access even when direct table ACLs are absent.
This is a read-only catalog refusal, excluding `pg_catalog` and
`information_schema`; it does not revoke or repair historical routine ACLs.

## Protected credentials

Credential CHECK_ONLY inspects the final source file's owner, regular-file and
non-symlink identity, 0400/0600 mode and 1..4096 byte length. No secret content is
printed, hashed or read by this metadata path. Actual preparation/rotation is a
future independently authorized operation, requiring respectively `--prepare`
or `--rotate`, a protected `--candidate` in the same protected credential
directory, and the exact confirmation
`PREPARE_OR_ROTATE_ASSET_CARD_CREDENTIAL_ONLY`.

The candidate must be a UTF-8 single-line password accepted by the dedicated
Java verifier. A new independent connection verifies the expected database,
role and exact permissions using only that file. Password values never enter
argv or environment variables; no plaintext temporary copy or pgpass file is
created. Only successful verification permits an atomic same-directory rename.
Rotation first retains the old protected inode under a timestamped `.previous`
name; failures leave the old live file usable. These are protected rollback
credentials, not logs or temporary plaintext. Their eventual approved retirement
is an operator action. Output is `RESTART_REQUIRED=YES`, not a hot-rotation claim.

The protected root0400 source and the systemd-delivered runtime credential are
different trust channels. On the verified systemd255 host, runtime files are
root:root0440 in a root:root0550 read-only tmpfs and an exact named-user ACL grants
only the service UID access. Java's POSIX mode alone cannot prove this ACL.
The dedicated runtime validation therefore uses the already-present
`/usr/bin/python3` with isolated standard-library metadata inspection; it is not
a new package installation, Python model runtime or credential-content reader.
Absence/failure of this dependency must fail closed before ARMED operation;
PREPARED source-file verification does not claim systemd runtime verification.

The verifier invocation uses the standard JAR's independent main:

```text
AssetCardDataSourceConfiguration verify --jdbc-url <non-secret-url>
  --expected-database <database> --credential-file <protected-path>
  --credential-owner <numeric-uid>
```

`AssetCardDataSourceConfiguration` now supplies that verifier and a private
`assetCardDataSource` / `assetCardJdbcTemplate` pair. Only its lifecycle holder
is registered in the application context, so Boot's default DataSource and
JdbcTemplate auto-configuration remain intact. The Mapper uses the dedicated
connection for the three card tables and the default connection only for existing
OHLCV reads. Missing credentials, authentication/permission failure or an outage
never route a card write to the default pool. Initialization is lazy, with bounded
background retries; creating the application context does not connect the writer.

The pool defaults to two connections (allowed range 1..4), with bounded connection,
validation and statement timeouts. A fresh candidate must authenticate and pass
the same effective-privilege checks before replacing the current generation.
Failed candidates close; the old generation drains its existing leases before
closing. Application shutdown closes both generations. This local lifecycle
support is not a claim that systemd credentials hot-reload: the credential tool
continues to output `RESTART_REQUIRED=YES` and does not restart anything.

The credential CLI starts an independent non-Web Java main and performs read-only
catalog checks over a new JDBC connection; it does not start Spring or the native
model probe. A missing verifier still fails closed before a credential rename.
`AssetCardDataSourceConfigurationTest` exercises real Spring/Hikari and disposable
PostgreSQL. The infrastructure test's `assetCard.credential.integration-jar`
option executes the actual candidate standard JAR entrypoint against that isolated
database. Ordinary compiled-class/fake-root routing tests are not JAR acceptance;
see the implementation evidence for separately recorded results. No test is a
real systemd credential installation or Staging/database acceptance.

## Models and card drop-in

Model installation accepts `--source` and externally reviewed
`--bundle-sha256`. Even DRY_RUN qualification requires the independent
`AssetCardNativeRuntimeProbe --verify-bundle-only` to return exit 0,
`VERIFIED_BUNDLE_STATUS=PASS` and `DATA_KIND=REAL_HISTORICAL`. Checksum-only
PASS and synthetic fixtures never create a current-production-model link.
After later deployment authorization, `--apply --confirm
INSTALL_ASSET_CARD_MODEL_ONLY` can copy only the seven fixed bundle files into
`bundles/<manifest-sha256>`, verify the copied bytes again, set files to 0444 and
directory to 0555, and atomically switch `current`. The old immutable bundle and
`previous` pointer remain available for rollback. Existing bundles are not
overwritten. No native runtime training or resident Python is added.
This installer manages one immutable filesystem pointer only; it does not
generate the model registry's per-symbol bundle mapping or enable a model path
in the service. Real multi-asset mappings still require explicit configuration
and bundle/asset qualification. Pointer installation is not 128-asset coverage.

After later deployment authorization, the drop-in installer additionally
requires `--apply --confirm INSTALL_ASSET_CARD_DROPIN_ONLY`. It writes only
`40-asset-card.conf`, retains an existing card drop-in, and writes a non-secret
rollback inventory. The main unit, readiness script, core scheduler and
environment files remain unchanged. Its LoadCredential exposes the password
at the systemd credential path and the model root is read-only. Default
`CARD_STARTUP_MODE=PREPARED` renders card enabled/external/writer/retention false;
model mode stays SHADOW. No reload, restart, enable, remote command
or deployment is performed. Applying a drop-in is not proof it has been loaded.

## Fixed-window SHADOW and verified retention

Owner-only card UI inspection is separate from model publication eligibility.
The optional non-secret manifest field `OWNER_PREVIEW_USER_ID` defaults to `NONE`;
only one positive, Session-verified user ID may be configured. The installer
renders `TRADE_MODEL_ASSET_CARD_OWNER_PREVIEW_USER_IDS` without changing SHADOW,
the three collection switches or the finite-window checks. A missing field in
an older manifest also means no preview account. The HTTP client cannot select
this account. Preview shows independently verified price/risk fields, not an
unqualified model direction, a probability or an old confidence fallback.
Changing this source configuration is not installation, a restart or a new
network allowance; every new candidate still requires its own exact identity.

The unified approval package is in
`docs/evidence/asset_card_live_signal/IMPLEMENTATION_AND_ACCEPTANCE.md`, first
section. Its 21-GET probe and <=8-hour collection allowance are separate items;
neither is executed by preparing these sources.

`CARD_STARTUP_MODE=ARMED` additionally requires the distinct confirmation
`INSTALL_ASSET_CARD_ARMED_WINDOW_ONLY`. All absolute window, symbol, known
shared-IP allowance, row/database/WAL budgets and storage-proof fields must
validate first. The installer writes only the existing card drop-in, never
generates a start time/window ID, never erases a ledger, and never restarts.
PREPARED cannot accidentally enable collection using the ordinary confirmation.
Missing/unknown quota remains a refusal, not an assumed exchange limit.

State `/var/lib/rine-logic-asset-card/collection` and archives
`/var/lib/rine-logic-asset-card/archive` require 0700 leaf directories owned by the
actual service UID and GID (both explicitly recorded in the manifest). Their
dedicated parent `/var/lib/rine-logic-asset-card` must be root:root 0755 with
unchanged trusted root-owned ancestry and no symlinks. The existing
`/var/lib/rine-logic` contents, ownership and permissions are not modified.
The card attachment adds only these two leaves to `ReadWritePaths`; it does not
reset the base unit's existing business/log writable paths. Before a real start,
verify under the actual service constraints that the two leaves are writable
but their root-owned parent is not. Fake-root tests do not replace this check.
`DATABASE_FILESYSTEM_DEVICE` is an independently
reviewed non-secret `stat` device identity: the installer compares both visible
directories with it, but **does not itself prove the actual PGDATA device**.
The execution preflight must read the real PostgreSQL data location via the
protected administrative channel and verify its device matches; otherwise ARMED
must not be approved. This avoids reporting state-directory free bytes as another
database filesystem's capacity. No new database privileges are added for this.

The Java lease persists its exact plan identity, immutable start/end, cumulative
REST/control/connection counters and first stop reason under an exclusive file
lock; files are 0600, atomically replaced and fsynced with their parent directory.
Missing/corrupt initialized state, ownership/mode failure, backwards time,
database/WAL measurement failure, quota failure or budget exhaustion disables
card transport. STOP before the start time is durable. I/O failure latches a
terminal state instead of reopening a stale OPEN ledger when the filesystem
recovers. Normal shutdown records a clean process handoff; an unclean process
identity refuses automatic restart of that allowance. A permitted clean restart
does not reset time/counters. Deadline checking runs
at ingress and via a 250ms card-only watchdog; DB/WAL/free-space are checked every
15 seconds. The monitored thresholds are not per-INSERT hard limits: the reviewed
package reserves explicit headroom for bounded already-running work and offline
labels. Other business schedulers remain unchanged.

Only the registered Spot symbols are subscribed; only depth5000 REST recovery
is added to the fixed WS allowance. CoinGlass remains read-only cache access,
with unproven source/unit/window quarantined UNKNOWN and training-unqualified.
Window expiry never extends networking to finish a four-hour label; incomplete
evidence remains PENDING, including after restart. No production model is required
for private SHADOW audit persistence, but unqualified data is not training-ready.

The installed retention periods are Bar/Trade=168h, Feature=720h and Label=2160h,
batch128 per asset per 60-second cycle. These are engineering storage settings,
not model sample-size thresholds. During the finite collection window all
historical deletes are protected to prevent deletion offsetting new-row budgets.
Afterward pending-label dependencies still take precedence over age. The service
archives complete original identities/values/timestamps, verifies SHA/count and
read-back before exact deletion, and exports from both database and verified
archives. Archiving/verification/space failure preserves data. Snapshot never
receives DELETE; only the two authorized history tables can be cleaned. The
native manifest sets both collection and archive space floors to 20 GiB.

## Local tests, not deployment

`--test-root` accepts only a protected temporary directory with a protected
`.asset-card-test-root` containing `TEST_FIXTURE_ONLY`. Every target is remapped
inside it, including the fake Java executable; candidate paths cannot escape it.
It is for executable source/atomicity/refusal tests, not native model acceptance.
The JUnit infrastructure contract uses only such roots and disposable PostgreSQL.
Tests may harden their newly created disposable database's PUBLIC ACL to exercise
the success case; that is not permission to alter an existing database.
The coordinated shell matrix and standard-JAR probe use temporary fixture models,
which must be deleted on exit. No real-host credentials or data are used.
