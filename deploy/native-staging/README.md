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
readiness script, application JAR and release metadata. The only source-supported
metadata candidate is `/opt/rine-logic/release-metadata.json` with `JSON_V1` format.
Its actual path/format has not been verified: real acceptance is blocked until
independent base-chain evidence proves that exact non-secret identity. A mismatch
stops; never point the manifest at an arbitrary file or infer/rewrite that release
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
ACLs and data are unchanged. The three card objects must be actual ordinary
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

The verifier invocation uses the standard JAR's independent main:

```text
AssetCardDataSourceConfiguration verify --jdbc-url <non-secret-url>
  --expected-database <database> --credential-file <protected-path>
  --credential-owner <numeric-uid>
```

Connection integration is currently **BLOCKED**: this stage has not supplied
the dedicated DataSource verifier after its separate source changes were refused
by review. A missing verifier fails closed before a credential rename. Fake-root
CLI routing tests do not prove a fresh database connection or usable credentials.

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
at the systemd credential path, the model root is read-only, writer remains
disabled and model mode stays SHADOW. No reload, restart, enable, remote command
or deployment is performed. Applying a drop-in is not proof it has been loaded.

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
