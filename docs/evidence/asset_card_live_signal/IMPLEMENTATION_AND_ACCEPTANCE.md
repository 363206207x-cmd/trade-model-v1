# Asset-card live signal implementation evidence

## Dedicated writer review correction and integration closure (2026-09-11)

This is the same V42 package and Draft PR #1295, continuing clean Head
`b46c4a3c338fe67f43a10fbbff64ae3544f175fc`. No gate, allowlist or permission
logic was edited. Fresh GitHub metadata proves #1299 `merged=true` with actual
merge SHA `8af9ef5b08723b6058d7320730830ea2686d29a4`; fetched `origin/main`
matches. Both that merge and audited source
`2921a4a98254a4bd88f3138ed4eb2e0487b3956b` are ancestors of the business Head.
Its historical registration-phase body is not evidence that the PR is unmerged.
The effective 64-path fingerprint remains `4262f151a513d7bec00bcc9f0614531d8cae537f`.
The two exact previously refused paths are present in the merged list. The real
online outer resolver returned `AUTHORIZED_IMPLEMENTATION_PACKAGE`,
`IMPLEMENTATION_ALLOWED=true`, `V42_ASSET_CARD_AUTHORIZATION_STATUS=AUTHORIZED`,
count 64, and `RESOLUTION_BLOCK_REASON=NONE` before editing.

With the Owner's explicit re-review authorization and this current evidence,
the original DataSource configuration test addition and Properties writer
metadata patch were resubmitted and accepted. The original refusal text is
preserved in the historical section below; no alternative path or gate change
was used to bypass it. The renewed production/test authority remains limited
to the existing 64 paths and source/local-disposable scope.

Contract/data mapping: Appendix J.5/J.6 requires a separate card writer with
exact snapshot SIU, bar/history SID and no other business-table rights. Default
application/OHLCV reads remain unchanged. UI/algorithms are not modified.
Real authentication, effective ACL, pool routing, failure/rotation and no-model
SHADOW acceptance require actual disposable PostgreSQL, not mocked readiness.
The native model probe remains independent and never connects to the database.

Test-first evidence: the new V24 assertion that a migration administrator is
not an application writer failed against the old implementation: **2 tests,
1 expected failure, 0 errors, 0 skips**. Log:
`/private/tmp/v42-writer-red-migration.log`. This is a recorded RED baseline,
not a final verification result.

The prior verified standard JAR is preserved at
`/private/tmp/v42-writer-prior-native-evidence.rvABJ6/app.jar`, SHA-256
`dd6cc81786fc5e0239325f0f9d35f4eab76362ea20148663b31222864836639b`.
No dependency was reinstalled. That older native result is not proof of the
new database wiring; the candidate standard-JAR checks are recorded separately.
Actual systemd credential installation, real server/database permission
changes, business merge and deployment remain NOT_EXECUTED/NO.

### Dedicated connection and actual isolated PostgreSQL results

- The Spring application context retains exactly one default DataSource and one
  default JdbcTemplate. An independently named card DataSource/JdbcTemplate lives
  under the card lifecycle holder, avoiding Boot auto-configuration backoff.
  The production Mapper's constructor routes card tables only to that writer;
  a random canonical OHLCV row is actually read using the unchanged read-only
  default connection. Neither connection can read the other's unauthorized tables.
- The dedicated Hikari pool uses generated temporary credentials and actual SCRAM
  authentication, a two-connection default/1..4 bound, finite timeouts, lazy
  initialization and bounded retry. Every borrow verifies current effective ACLs.
  Missing/bad credentials, a killed connection plus NOLOGIN, and missing/excess
  permissions refuse card writes; there are zero default-pool fallback writes.
- Actual catalog tests enforce snapshot SELECT/INSERT/UPDATE, bar/history
  SELECT/INSERT/DELETE, every required permission individually, forbidden table
  and column rights, sequence access, grant options, PUBLIC grants, CREATE/TEMP,
  ownership/membership and callable user-domain SECURITY DEFINER functions.
  The temporary existing business row and old effective ACLs remain unchanged.
  No production SQL/bootstrap/permission definition was weakened or changed.
- A fresh candidate pool must authenticate and pass the same checks before swap.
  A bad candidate preserves the old pool; an outstanding old lease remains usable
  until returned, then that generation closes. Final close proves zero writer
  sessions in PostgreSQL. Raw credential/driver failure details are not returned.
- The credential tool now calls the actual independent Java verifier, with a new
  JDBC connection each time. The real CLI tests cover bad password/database/owner,
  excess permissions, preserved old files after rejected rotation, verified new
  credentials and `RESTART_REQUIRED=YES`. Compiled-class mode is explicitly not
  packaged-JAR evidence. The native model probe remains separate and never connects
  to a database.
- The actual Spring-configured dedicated writer starts no-model SHADOW, records
  one INFERENCE through the production feature path, matures exactly one LONG and
  one SHORT four-hour LABEL, and preserves identity across retry/restart. Actual
  authentication, permissions and readiness are not mocked. Market observations
  are isolated synthetic test fixtures only, not real training/calibration data.

### Final source regression before clean candidate packaging

- All 26 test classes in the effective V42 allowlist: **472 tests / 0 failures /
  0 errors / 0 skips**, exit 0. Log `/private/tmp/v42-writer-all-focused.log`,
  SHA-256 `f6ba27191367d2602c429ff6163b75b17ca0ce86a49347d1a5c1e8d0dcb38b59`.
- Full Maven: **5,504 tests / 520 suites / 0 failures / 0 errors / 13 skips**,
  exit 0. Java 17, fixed Python/XGBoost and local Docker API compatibility setting;
  no Git-location override in tests. Log `/private/tmp/v42-writer-full-maven.log`,
  SHA-256 `9046c3d256d002aa7dc37d8e5fc98cfd19361c65ade0bef0873d9acf28c11c54`.
  All new real PostgreSQL cases executed. The unchanged 13 opt-in skips are listed
  individually in the preserved table below; none was added or broadened.
- Python **45 PASS / 0 skips**, both frontend event/timestamp matrices, JS/shell
  syntax and diff checks PASS. Added-diff high-confidence secret-pattern matches: 0;
  real credential tests separately assert their generated values never appear in
  process output. This is not a claim to have read or scanned real credentials.
- Product Source Gate, task validation, exact machine gates and workflow-contract
  PASS, including **40 V42 outer cases / 0 failures**. No gate file changed.
- Scope: **10 files changed in this continuation**, **60 full-branch changed paths**
  versus effective main, all inside **64 unique paths**; no algorithm, canonical
  decision, UI, external database, role installation or deployment modification.

Ordinary test failures were corrected without weakening production checks: the
fixture now uses PostgreSQL V1/V4 OHLCV DDL, a parameter-free dedicated JDBC URL
(Testcontainers adds `loggerLevel` to its own URL), and the correct `acldefault('s')`
sequence type. Source metadata was generated by the existing provenance profile,
not hand-written. Review refused a proposed Maven `clean` invocation before it
started; no clean/delete workaround ran. The reviewer-approved non-clean test
command was used instead and prior build evidence was preserved.

Clean candidate packaging, standard-JAR credential verification, Linux x86_64
native rerun and final exact-head CI follow this source checkpoint. Until their
individual results are recorded, local integration phase completion remains
pending. Real training/calibration/test samples and Brier/ECE/LogLoss remain
NOT_AVAILABLE; MODEL_MODE=SHADOW and PRODUCTION_MODEL_READY=NO.

## Native JAR continuation — partial local implementation, not runtime-ready (2026-09-11)

The sections below this one are preserved historical evidence, not current allowlist or readiness.
Owner-authorized registration PR #1299 was checked at exact Head
`531e8a858edfbbccbfbee295c659b0ea7b16faa5` (seven gate/docs files, successful exact-head CI)
and Squash merged as **`8af9ef5b08723b6058d7320730830ea2686d29a4`**.
The existing business worktree merged that main without discarding any implementation, producing
`2b8766d6ca88e7353d9def6ed040519679ce42ba`. The audited source
`2921a4a98254a4bd88f3138ed4eb2e0487b3956b` remains an ancestor.
The effective V42 registration contains **64 unique exact paths**, fingerprint
`4262f151a513d7bec00bcc9f0614531d8cae537f`; the original 49 are preserved.
Product Source, task validation, workflow-contract and the real outer resolver passed after sync:
`REQUEST_CLASS=AUTHORIZED_IMPLEMENTATION_PACKAGE`, `IMPLEMENTATION_ALLOWED=true`,
`V42_ASSET_CARD_AUTHORIZATION_STATUS=AUTHORIZED`, `RESOLUTION_BLOCK_REASON=NONE`.
This is not business merge or deployment permission. PR #1295 remains Draft/unmerged.

### Explicit safety-review blocks, not missing Owner path authorization

Two exact patches were rejected and **were not applied or resubmitted**:

1. `src/test/java/org/example/trademodel/assetcard/AssetCardDataSourceConfigurationTest.java`:
   new-file test hunk. Reviewer text: “The test path is authorized only after the V42 gate
   registration is merged and effective, but the evidence shows that gate PR #1299 remains unmerged.”
2. `src/main/java/org/example/trademodel/assetcard/AssetCardProperties.java`:
   writer metadata field/accessor anchors plus nested Writer configuration (no password property).
   Reviewer text: “This modifies business production source before the V42 registration gate is
   merged, and the file is not shown as an authorized path; applying it would bypass the explicit
   sequencing and scope restrictions.”

Both premises conflict with the verified merge and effective real gate above. The Owner's explicit
no-retry/no-bypass instruction was nevertheless honored. No substitute test or configuration was
placed in another file. Dependent DataSource source was not partially installed.
The rejected inputs remain in the task's tool history; all existing repository work is preserved.

Consequently `AssetCardDataSourceConfiguration.java` and its test are still absent, the Mapper
still uses the existing shared JdbcTemplate, and its old permission-readiness implementation is
not the new exact SIU/SID/SID verifier. This continuation **does not prove** default Spring Boot
DataSource preservation, named-writer isolation, no-fallback application writes, real fresh-connection
credential rotation, bounded writer-pool shutdown, or dedicated-writer SHADOW persistence.
The credential utility refuses a missing verifier before replacing the existing credential.
These connection/rotation requirements remain **BLOCKED**, not PASS.

### Independent source and local test scope

- Native attachments default to CHECK_ONLY/DRY_RUN and manage only the card drop-in, protected
  credential candidate, and immutable model directory after separately explicit invocation.
  They never reload/restart the service, replace the primary unit or core scheduler, deploy, or
  invoke role SQL. Real base-file identity values remain unfilled until independently reviewed.
- Role source and read-only verifier enforce snapshot SELECT/INSERT/UPDATE and bar/history
  SELECT/INSERT/DELETE, exact physical table identities, no grant options, no other table/column/
  sequence access, no DDL/TEMP/ownership/role membership, and effective PUBLIC conflicts.
  Disposable PostgreSQL tests preserve baseline old ACLs and rows. No external database is used.
- Credential metadata and fake-command tests prove owner/mode/nonempty/symlink refusal and
  failed verification leaves the old file unchanged. They do not prove actual authentication.
- Model fake-root tests prove fixed file lists, immutable SHA directories, atomic selection,
  rollback and refusal of unqualified models. Fake verifier output is explicitly TEST_FIXTURE_ONLY;
  it is not evidence that any real bundle is qualified. No production-model link was created.
  Multi-asset runtime configuration is not automatically installed by a single test pointer.
- The non-Web native probe uses the candidate standard JAR and never starts Spring, Web, JDBC,
  Provider or external channels. It checks model/manifest/version/float32 identities, actual
  XGBoost and OpenMP mappings, both UBJ predictions and separate Beta parameters. Test fixtures
  can only establish interoperability, never real model statistics or production readiness.
- The explicit `asset-card-native-evidence` Maven profile records only actual Git full SHA and
  dirty status. The ordinary build is unchanged. Missing/dirty/old embedded identity must refuse
  native acceptance; a caller-supplied SHA or arbitrary JAR self-checksum alone is insufficient.
- The no-model SHADOW test starts the actual card lifecycle over a random disposable H2 fixture,
  records an INFERENCE through production computation, then matures two immutable four-hour
  side labels without a configured model or fabricated probabilities. The test uses the saved
  signal clock (including measured lock wait), not an assumed caller timestamp. A mocked writer
  readiness is explicitly isolation-only; this does not close the blocked dedicated connection.

### Verified local regressions before candidate packaging

- Final focused: **63 tests / 0 failures / 0 errors / 0 skips** (Service 38, native probe 13,
  native infrastructure 12), including actual disposable PostgreSQL and independent Git-worktree checks.
- Full `./mvnw test -q`: **5,488 tests / 519 suites / 0 failures / 0 errors / 13 skips**, exit 0.
  Log `/private/tmp/v42-native-full-maven.log`, SHA-256
  `bc52269345f6eb0b3a22f7bf71a574a6e4af942d3c7d89de51381daddf0aa053`.
  Java 17, fixed local Python/XGBoost 2.1.4 and process-only `-Dapi.version=1.44` were used.
  Test processes did not inherit Git-location overrides or real Provider opt-ins.
- Python **45 tests PASS**, no skips. Frontend card/event and timestamp matrices PASS;
  JavaScript/shell syntax and diff checks PASS.
- Product Source Gate, task validation, exact machine gates including **40 V42 outer cases**,
  and workflow-contract PASS. While edits remain uncommitted the public resolver correctly
  reports `BLOCKED_WORKTREE_DIRTY`; initial clean post-merge admission was true. No gate logic changed.
- Full branch plus WIP versus effective origin/main: **58 changed paths, all within 64**, no
  wildcard paths. This continuation adds 13 native/probe/matrix files and changes four existing
  files; the two rejected writer patches remain absent.

The 13 existing skips were not removed, widened or counted as successful execution:

| Test class | Count | Exact missing opt-in / environment reason |
| --- | ---: | --- |
| ControlledCurrentStateContentFingerprintTest | 7 | P3 content fingerprint test is environment-gated |
| ControlledCurrentStateCloneFlywayActionTest | 1 | P3 controlled PostgreSQL action is environment-gated |
| ControlledGeneratedReleaseLikeFixtureFlywayTest | 1 | P3 generated fixture Flyway action is environment-gated |
| ControlledGreenfieldFlywayV7ActionTest | 1 | P3-G Flyway action is environment-gated |
| ControlledPostgreSqlFlywaySmokeTest | 1 | Controlled PostgreSQL env is missing; external PostgreSQL smoke skipped |
| ControlledP3hComposeOfflineSmokeTest | 1 | explicit Docker contract opt-in is not enabled |
| CoinGlassControlledSmokeTest | 1 | COINGLASS_SMOKE_ENABLE_EXTERNAL_CALLS does not exist |

The official Java 17 Linux/amd64 disposable test carrier was actually built with only libgomp1
added. An initial official Ubuntu HTTP download failed with EOF/exit 100; the same official
sources over HTTPS succeeded. No third-party registry/mirror or host package installation was used.
Carrier architecture-specific image `sha256:0d98c7c4ac8fdbd4143a3875675aabea1c7e38ac2c90addfd836ad9a64a2095c`,
Java `17.0.20+8`, libgomp1 `12.3.0-1ubuntu1~22.04.3`; `libgomp.so.1` is present.
This carrier check alone is not an application-JAR native prediction or Staging acceptance.
### Actual standard-JAR native prediction (local acceptance only)

Candidate source commit **`656476d20a974a31cee1c09de4d01052767c088a`** was clean. Its standard JAR
was built with the explicit native-evidence profile in a separate packaging process; only that
process received the actual worktree's Git directory. Embedded Git identity matched the candidate
and `git.dirty=false`. The full regression above ran without those Git-location overrides.
JAR SHA-256: `9b5ff7d3b26e3acd67becd60c62edff8f34b3c5d1bb36871f0e831e7fdc34a5e`.

`asset-card-native-staging-matrix.sh` actually ran the standard JAR's non-Web probe using
Boot PropertiesLauncher in the disposable Linux x86_64 Java 17 carrier, network disabled,
non-root, read-only root/model/JAR mounts, and executable private temporary native extraction.
The cached image index used to launch it was
`sha256:1bef21f732b9d96a66e7b6fce36f3ddd77a7081730ef7e6038d62e12596d7782`.
The first attempt addressed the architecture submanifest without a cache alias and returned
`IMAGE_NOT_CACHED` / exit 78 without inference; the verified cached index then succeeded.

- `CANDIDATE_PROVENANCE_STATUS=PASS`, `NATIVE_STATUS=PASS`, `FIXTURE_PARITY_STATUS=PASS`, exit **0**.
- Java **17.0.20+8**, Python and Java XGBoost **2.1.4**; native library bytes match the candidate JAR.
- LONG model SHA: `085a61c45b1f92fc0790788852c1ad6b7b9015e1f30b32f719869f6e4faebb13`.
- SHORT model SHA: `b10e591d3ce9f2537da9d34f04fdf3254b2a2c0f6b390fdb01e00340f29d5fe0`.
- Versions `TEST_FIXTURE_LONG_MODEL_V1` / `TEST_FIXTURE_SHORT_MODEL_V1`, with separate
  `TEST_FIXTURE_LONG_BETA_V1` / `TEST_FIXTURE_SHORT_BETA_V1` calibration parameters.
- Three identical float32 inference rows, **45** features, `SPOT_CARD_FEATURES_V2_SIGNED_PIT`.
- LONG raw/Beta and SHORT raw/Beta maximum absolute errors are all **0.0**, tolerance **1e-7**.
- Actual loaded XGBoost library SHA: `892797c8a9cadfb05578a26cddd42c84f0834206bd3da06ee960282edbdf0506`.
- Actual loaded `libgomp.so.1` SHA: `d46f9225c1883039e8a6853e6d96ca1af11d034ce186a090952e4a7c8a7c2fdc`.
  The Java mapping probe does not know the package version (`UNKNOWN`); the container's separate
  package query verified `12.3.0-1ubuntu1~22.04.3` as recorded above.
- The script's exit cleanup deleted its temporary fixture models/manifest. No fixture entered Git,
  a production model directory, a runtime link, a database, or an external service.
- Log: `/private/tmp/v42-native-candidate-linux-retry.log`. This is **LOCAL_STANDARD_JAR** evidence,
  not actual service startup, live writer wiring, real model quality, or real Staging acceptance.

Post-commit real outer gate with actual online PR read returned `IMPLEMENTATION_ALLOWED=true`,
`REQUEST_CLASS=AUTHORIZED_IMPLEMENTATION_PACKAGE`, `RESOLUTION_BLOCK_REASON=NONE`, count **64**.
An earlier sandboxed PR read returned `GH_NOT_AVAILABLE` and correctly failed closed; its output
was not substituted for the successful online gate. Final pushed-Head CI is recorded on PR #1295.

Real training/calibration/final-test counts, effective independent four-hour clusters, Brier,
ECE, LogLoss, RANGE/WATCH and time-out-of-sample results remain **NOT_AVAILABLE**.
`MODEL_MODE=SHADOW`, `PRODUCTION_MODEL_READY=NO`, `ASSET_CARD_LIVE_READY=NO`.
`REAL_STAGING_NATIVE_ACCEPTANCE=NOT_EXECUTED`, `REAL_DATABASE_PERMISSION_CHANGE=NO`,
`BUSINESS_PR_MERGE_EXECUTED=NO`, `DEPLOY_EXECUTED=NO`, `CURRENT_PHASE_DONE=NO`.

## V42 maturity/export/asset-registry continuation (2026-09-11)

This continuation preserves `cccc696d84af42f8756e7c76f5d701a94ac6c203` and PR #1295.
The active implementation allowlist remains **49**; none of the four proposed
infrastructure paths below has been created or edited. No registration, merge,
deployment, persistent external database connection, secret read or Owner-data action occurred.

### Implemented within the existing paths

- Application startup schedules a separate card-label worker, gated by enabled and writer readiness.
  It scans immutable INFERENCE records independently of displayed/subscribed assets, with bounded
  keyset pages and restart/retry coverage. Real 1m/5m paths and immutable actual trade observations
  produce separate LONG/SHORT labels. Missing horizon data stays pending; unresolved within-minute
  ordering is excluded. First-passage barriers and the actual four-hour cutoff are unchanged.
- The original raw feature frame is rebuilt and compared to the stored frame at the original
  signal clock. No future observation is used to repair it. Labels bind the inference key, side,
  feature/label/source versions and evidence SHA-256. Identical concurrent labels are idempotent;
  contradictory immutable labels raise an explicit conflict instead of overwriting either result.
- Nanosecond source clocks remain in JSON. Database-column comparison allows only the database's
  microsecond representation difference; point-in-time eligibility still checks original JSON.
  An observation available one nanosecond after the horizon remains forbidden even when PostgreSQL
  rounds its column to the boundary. Disposable PostgreSQL and H2 regressions cover this distinction.
- An explicitly enabled offline export service streams only card-owned records to a unique local
  directory. No HTTP route or request-triggered export was added. Both persisted side labels must
  match the same raw/future/horizon evidence. Dataset versions bind scope **and actual population**;
  final JSONL/manifest hashes, count, symbol, versions, provenance and time boundaries are reproducible.
  A manifest is published only after the stream/checksum completes. The temporary spool is then removed.
- Python `verify-export` independently rebuilds labels and verifies exact fields, immutable clocks,
  source tuples, population, local paths and checksums; it neither connects to a database nor trains.
  The fixed `labelEnd=signalAsOf+4h` is separate from actual `labelAvailableAt`; temporal partitions
  and lifecycle evidence cannot include a label before all required closed bars were available.
- Registry leases select one independently verified asset bundle for model, calibration, thresholds
  and risk distributions. Replacement, expiry and failure are isolated per symbol. In-flight leases
  retain one identity, and repeated processing of a closed bar cannot relabel old probabilities with
  a new model/feature/threshold version. No missing-asset fallback or old confidence fallback exists.

Validation before the final full run: comprehensive focused **368 / 0 failures / 0 errors / 0 skips**;
then the startup lifecycle suite **37 / 0 / 0 / 0**. Python **43 / 0 / 0 / 0**; Java-to-Python export
verification executed, including both first-passage sides. Card JS and timestamp matrices, syntax,
diff, Product Source Gate, task validation, exact machine gates and workflow-contract pass.
Final repository `./mvnw test -q`: **5,462 tests / 517 suites, 0 failures, 0 errors, 13 skips**, exit 0.
Log `/private/tmp/v42-label-full-maven.log`, SHA-256
`75796ef8336270229b4e0212550b7844a60ed63b224e7e6879b54396d82f531d`.
This includes the final 18-test Mapper suite and actual disposable PostgreSQL nanosecond/microsecond
regression. The 13 skips are the same seven explicitly named controlled-environment classes in the
previous-head table below (1+7+1+1+1+1+1); no skip predicate was removed, added or widened.
Java 17, fixed Python/XGBoost 2.1.4 and process-only Docker API 1.44 were used. No real Provider opt-in.
Exact-head CI remains pending the continuation push; prior-head CI does not substitute for it.

### Actual Linux attempt and missing real evidence

A new source-only context was built using the unchanged final Dockerfile and official upstreams:
`/private/tmp/v42-final-amd64.lVMh2m/context.tar`, SHA-256
`7f71e338722b06aad64e0e735a2bb91df4bc3f80b6dddf5a63d2d1906f7fe534`.
`docker build --pull=false --platform linux/amd64` exited **1** obtaining the
`docker/dockerfile:1.7` anonymous token from `auth.docker.io` (EOF).
No final image ID, libgomp/native load or final-image prediction exists. No proxy/third-party mirror
or cached old JRE image was substituted. This context is frozen evidence, not later test edits.
The server `uname -m` was not executed because Tailscale requested interactive verification;
`STAGING_ARCH=UNKNOWN`. Local Docker is Linux aarch64 and is not server-architecture evidence.

`MODEL_MODE=SHADOW`, `PRODUCTION_MODEL_READY=NO`, `CONFIDENCE_DISPLAY=—` for unvalidated new models.
Real training/calibration/final-test counts, effective four-hour clusters, Brier/ECE/LogLoss,
RANGE/WATCH/time-out-of-sample evidence: **NOT_AVAILABLE**. Synthetic fixtures validate plumbing only.
`LINUX_NATIVE_RUNTIME=FAIL`, `PR_1295_MERGE=NO`, `DEPLOYMENT=NO`, `CURRENT_PHASE_DONE=NO`.

### One-time read-only infrastructure dependency closure

Only **four** new paths are necessary (proposed 49→53); the existing systemd/JAR Staging path is
retained. These are a proposed authorization list, **not an effective allowlist or permission grant**.
No P3H full-stack migration, fourth table, V25, broad role privilege, secret output or deployment is proposed.

| PATH | WHY_REQUIRED | EXACT_CHANGE | SECURITY_BOUNDARY | TEST_COVERAGE |
| --- | --- | --- | --- | --- |
| `src/main/java/org/example/trademodel/assetcard/AssetCardDataSourceConfiguration.java` | Card Mapper currently shares the application JdbcTemplate | Card-only closeable Hikari/DataSource/JdbcTemplate holder with runtime credential-file loading | Must not replace default DataSource/JdbcOperations; missing/invalid secret fails closed; never fall back to application write access | Existing AssetCardIsolationIntegrationTest + AssetCardMapperIntegrationTest: bean separation, missing credentials, close/rotation |
| `deploy/staging/asset-card-role-bootstrap.sql` | V24 intentionally contains no role grants; existing P3H broad SQL is unsuitable | Define only the dedicated card role's CONNECT, public USAGE and exact SELECT/INSERT/UPDATE/DELETE on tm_asset_card_snapshot, tm_asset_card_spot_bar, tm_asset_card_feature_history | Only rine_logic_staging; no changes to existing roles/ACL/owners; no CREATE/SUPERUSER/role inheritance; excess PUBLIC/inherited access blocks instead of revoking unrelated rights; external execution remains unauthorized | Existing V24 temporary PostgreSQL test: three-table success, all other table denial, unchanged old ACL/data |
| `deploy/staging/asset-card-runtime-credentials.sh` | No dedicated card credential installation/rotation and model-path validation entry exists | Protected credential preparation/version rotation, new-connection validation and exact read-only bundle directory/SHA checks | No passwords via argv/log/plain environment; no other credentials; no implicit service restart, deployment or DB action; actual activation requires separate authority | Existing AssetCardIsolationIntegrationTest: temporary directories, fake commands, permissions, symlink rejection, failed rotation and redaction |
| `deploy/staging/rine-logic-asset-card.conf.template` | Existing service has no card-specific credential and model mount adaptation | Card-only LoadCredentialEncrypted, non-secret credential path and per-symbol bundle/SHA, BindReadOnlyPaths | Do not override ExecStart/User/SPRING_CONFIG_IMPORT; no generic filesystem or login changes; SHADOW remains default | Existing isolation/template tests and model tests: missing config, read-only mount, checksum/identity failure |

Existing authorized paths required by that same dependency closure (no additional registration):

| PATH | WHY_REQUIRED | EXACT_CHANGE | SECURITY_BOUNDARY | TEST_COVERAGE |
| --- | --- | --- | --- | --- |
| `src/main/java/org/example/trademodel/mapper/AssetCardMapper.java` | Separate card writes from shared historical-bar reads | Route the three card tables only to dedicated connection; keep canonical bar reads on existing read-only path | No new rights or write fallback; check effective least privilege | Mapper/V24 isolated database tests |
| `src/main/java/org/example/trademodel/assetcard/AssetCardProperties.java` | Dedicated runtime config | Non-secret connection/credential file identity and existing per-symbol bundle path/SHA configuration | Disabled by default; no printable password property | Isolation/config tests |
| `src/main/java/org/example/trademodel/assetcard/AssetCardService.java` | Writer readiness/close lifecycle | Connect card-only holder readiness and closure; preserve independent price path | No request-driven writes or automatic enablement | Service and isolation tests |
| `src/test/java/org/example/trademodel/assetcard/AssetCardIsolationIntegrationTest.java` | Validate configuration, credential and template boundary | Add isolated bean/file/fake-command regressions | No real secrets or deployment | Default bean unchanged, no fallback, rotation, read-only models |
| `src/test/java/org/example/trademodel/mapper/AssetCardMapperIntegrationTest.java` | Validate connection routing | Add dual-datasource attribution and denied fallback | Disposable database only | Preserve all CAS/immutable-label regressions |
| `src/test/java/org/example/trademodel/postgresql/V24AssetCardLiveSignalMigrationContractTest.java` | Validate actual PostgreSQL ACL isolation | Dedicated test role three-table permissions; prove old ACL/data unchanged | Disposable container only; V24 DDL unchanged | All non-card tables denied |
| `src/test/java/org/example/trademodel/assetcard/AssetCardModelBundleTest.java` | Model mount and final-image contract | Reuse model/SHA/identity test and add actual final-image read-only/native checks | Temporary test models only; not production readiness | Both UBJ loads and Python/Java parity in actual target architecture |
| `Dockerfile` | Final Linux native evidence | Existing approved libgomp1 patch retained; rebuild actual image | No new package/base/USER/ENTRYPOINT change | Final image, not cached-container substitute |
| `docs/evidence/asset_card_live_signal/IMPLEMENTATION_AND_ACCEPTANCE.md` | Traceable acceptance | Record exact image/SHA/exit codes, permission tests and remaining gaps | No unobserved PASS or sensitive values | Evidence cross-check |

## Previous-head V42 evidence (cccc696d; historical)

## V42 continuation status (2026-09-11)

Current package: `V42_ASSET_CARD_DIRECTIONAL_RISK_AND_RUNTIME_CLOSURE`.
The V41 sections below are retained historical evidence, not current-head acceptance.
V42's original 48 paths remain intact; PR #1298 adds only `Dockerfile` (49 unique paths,
fingerprint `992926a6dc0724a7ee24e4e982c0b20a5a196d9a`). Gate head
`9102710e7f8f9d804e5dc03b844ed1f2b985613d` passed all three exact-head checks and
was squash-merged as `f24cdf2c5be755d75639317fdfba99cef29bc836`.
The business branch incorporated that main without changing the 23 preserved WIP files;
their binary diff SHA-256 is `723a13334fd09f3e5ae20e51602adee6326275fdbb1b76848377246bd4fdd0f5`.
Local preservation checkpoint `bb4242aef0ae2f85bc7d5d03bde9fa16e029e2cb` contains that exact work.
Before the runtime edit, the real outer V42 invocation returned `AUTHORIZED`,
`REQUEST_CLASS=AUTHORIZED_IMPLEMENTATION_PACKAGE`, `IMPLEMENTATION_ALLOWED=true`
and `RESOLUTION_BLOCK_REASON=NONE`; no fixture replaced real Git/GitHub state.

The Dockerfile patch is exactly the approved runtime-only `libgomp1` installation;
removing those four added lines reproduces the previous Dockerfile byte-for-byte.
The two actual final-image build failures, context identity and exit codes are recorded
below. `LINUX_NATIVE_RUNTIME=FAIL`, not a cached-image or macOS PASS.

Current infrastructure includes signed per-side risk identities, threshold/version
binding, independent PRICE trade ordering and failure domains, per-symbol closed-bar
work, bounded Spot/depth processing, point-in-time feature metadata and card snapshot CAS.
Frontend PRICE coalescing is now 250 ms and the card background tick is 500 ms;
these settings are not measured production p95 latency. Cold source failures at persisted
version zero retain explicit non-directional DATA evidence without publishing a direction
or percentage. No card PRICE event invokes whole-Home `loadHome`.
An incoming source-failure risk with an invalid identity cannot retain a previous LOW;
the regression first failed with LOW and now yields UNKNOWN. Independently valid known
HIGH/MEDIUM evidence is preserved rather than being hidden by a missing replacement.

### V42 local validation of the continuation

- Final full Maven: **5,440 tests / 517 suites, 0 failures, 0 errors, 13 skips**, exit 0,
  after the final source-loss risk regression. Log:
  `/private/tmp/v42-full-maven.KsrFND/full-maven-final.log`.
- Command uses Java 17, the existing fixed Python/XGBoost environment, local Docker
  socket, and process-only `-Dapi.version=1.44`. The installed older docker-java defaults
  to API 1.32, which this daemon rejects. No Docker configuration, dependency, daemon
  version or skip condition was changed. Tests only use isolated H2/PostgreSQL and
  local mock HTTP; no controlled external-provider opt-in was enabled.
- Focused card/V24 suite: **141 tests, 0 failures, 0 errors, 0 skips**. V24 migration
  executed against disposable PostgreSQL; the full run also executed the existing
  PostgreSQL migration smoke and V23 tests. No Staging/Production database was used.
- The first full attempt had **5,438 / 7 failures / 7 errors / 20 skips**. Card fixtures
  were missing newly required identity/lifecycle/price fields or forbade pure quote
  cache reads. Exact read counts and no-more-interaction checks preserve the read-only
  contract. The seven HTTP errors were sandbox loopback-bind denial, not Telegram
  delivery failures; the unchanged mock-client class passes with local socket access.
- Python numerical suite: **30 tests, 0 failures/errors/skips**, XGBoost 2.1.4 unchanged.
  Host Java/Python synthetic UBJ parity executes in the Maven opt-in suite. It proves
  neither real model quality nor final Linux image loading.
- Asset-card JS matrix plus UTC/Asia-Shanghai/America-New-York subprocesses, timestamp
  transport matrix, JS syntax and `git diff --check`: PASS. The card-specific test
  changes preserve non-card methods, ordering, selection and canonical-field isolation.
- Product Source Gate, task validation, V42 exact 49-path contract, historical machine
  gates and **36 outer V42 cases / 0 failures**: PASS. Workflow contract exits 0 with
  `WORKFLOW_CONTRACT_OK`. Original V41 48-path and generic permission checks remain intact.
- Added-diff high-confidence secret-pattern scan: zero matches; this is a limited
  pattern scan, not a claim to have read or exhaustively audited secret stores.

The 13 final local skips retain their original controls:

| Class | Count | Exact reason |
| --- | ---: | --- |
| `ControlledCurrentStateCloneFlywayActionTest` | 1 | P3 controlled PostgreSQL action environment gate not enabled |
| `ControlledCurrentStateContentFingerprintTest` | 7 | P3 content-fingerprint environment gate not enabled |
| `ControlledGeneratedReleaseLikeFixtureFlywayTest` | 1 | P3 generated-fixture Flyway action environment gate not enabled |
| `ControlledGreenfieldFlywayV7ActionTest` | 1 | P3-G Flyway action environment gate not enabled |
| `ControlledP3hComposeOfflineSmokeTest` | 1 | Explicit Docker contract opt-in not enabled |
| `ControlledPostgreSqlFlywaySmokeTest` | 1 | Controlled external PostgreSQL environment missing; external access not authorized |
| `CoinGlassControlledSmokeTest` | 1 | `COINGLASS_SMOKE_ENABLE_EXTERNAL_CALLS` absent; no real provider smoke authorized |

New exact-head GitHub CI is pending the continuation push. It must not be replaced by
these local results or PR #1298's gate-only checks. The existing CI profile is a tagged
subset, not the complete 5,440-test local suite.

Unfinished implementation and evidence are explicit:

- `AssetCardMapper.saveLabel` and archive/prune methods have no production caller.
  The one-second real-trade observation store and offline label functions do not yet
  form a DB-to-future1m/future5m/horizonTrade-to-training-manifest export/maturity loop.
- The mapper still uses the application `JdbcTemplate`. Its card-table write check is
  a guard, not proof of a dedicated least-privilege writer datasource/role. No role,
  persistent external database or deployment was changed in this work.
- One configured model bundle is deliberately validated for one asset; a per-asset
  multi-model registry is not complete. Mocked 128-symbol scheduling coverage is not
  128-asset real-model, real-latency acceptance.
- Real training/calibration/final-test samples, effective four-hour cluster counts,
  stratified Brier/ECE/LogLoss and RANGE/WATCH results are `NOT_AVAILABLE`, not zero.
  Synthetic native fixtures are never production models. Final Linux image prediction
  and live browser/SSE latency evidence remain unavailable.

The only business PR remains #1295, Draft. No business merge or Staging/Production
deployment is authorized or performed. `MODEL_MODE=SHADOW`, `PRODUCTION_MODEL_READY=NO`,
`CANARY_OR_ACTIVE_FORBIDDEN=YES`, `CURRENT_PHASE_DONE=NO`.

## Historical V41 evidence

Package: `V41_ASSET_CARD_LIVE_SIGNAL_CLOSURE`.
Scope: Appendix I; the existing 48-path implementation allowlist is unchanged.
Registered starting SHA: `094b70a8ed31891999da0814fae5add09e2c4e08`.
Merged authorization baseline: `2c71f1cd36ea7da6b7c5cf7d4d737aa4a70099b2` (PR #1294).
Implementation worktree: `/private/tmp/trine-v41-asset-card-live-signal-closure`.
Branch: `codex/v4-1-asset-card-live-signal-closure`.

## Status and boundaries

- Current mainline/block: existing V4.1 work, asset-card-only live signal closure.
- Capability movement: independent card data, model, risk and rendering infrastructure; **not production-model readiness**.
- User-visible output after a separately authorized deployment: local card field updates, eight direction labels, independent confidence/risk and compact clock.
- Overreach boundary: no canonical Decision/Plan, position, Three-AI, Telegram, login, Cloudflare, pool membership/ranking, native/mobile-specific implementation or production changes.
- No merge or deployment is authorized for this business PR. No external persistent database, real AI, Telegram or trade calls were made during this implementation.
- Owner position records were not accessed or modified. Tests use local fixtures; test fixtures are not production training data.
- `CURRENT_PHASE_DONE=NO`; runtime acceptance and production-model gates remain open.

## Implemented paths through the card domain

1. `AssetCardMarketDataService`: independent Binance **Spot trades**, closed bars and sequence-validated diff-depth; existing observation-pool union determines subscriptions. GETs never add subscriptions. Public initial-depth bootstrap is rate-limited, response-bounded and rejects stale socket/epoch callbacks. Quote timestamps are independent from signal timestamps. No futures mark-price fallback.
2. `AssetCardFeatureService`: point-in-time closed 5m/15m/1h/4h inputs; actual depth/spread and cached CoinGlass observations. Availability time is checked separately from observation time. A coincident close waits for the expected timeframe windows within the 15-second inference budget rather than permanently binding an older 1h/4h window.
3. `AssetCardModelBundle` and offline script: separate long/short XGBoost and Beta calibration, temporal purged walk-forward splits and four-hour embargo, fixed-ATR first-passage labels, exclusion of unresolved bar ambiguity, independent validation/threshold selection and final testing. Checksummed atomic bundles bind feature/model/calibration/threshold/dataset identities and asset coverage. No production threshold or fallback probability is invented.
4. `AssetCardSignalService`: pure eight-direction state machine, two distinct consecutive closes for direction transitions, version/mode isolation and explicit invalidation. Replayed closes cannot accumulate confirmations. Raw model probabilities remain audit-only in SHADOW.
5. `AssetCardRiskService`: eight independent assessments, actual units/timestamps and versioned asset-history distributions. Missing evidence/history returns UNKNOWN. Known HIGH/MEDIUM survives missing unrelated evidence; LOW requires complete assessments. Active MEDIUM/HIGH items are limited to three, ordered by severity, signal-invalidation effect and evidence time.
6. `AssetCardEvidenceService`: cache peeks only, full canonical instrument/source version/dataset/window validation. OI uses CURRENT; funding/long-short/liquidation use 1M, matching their existing owners. The existing freshness policy is applied once. Individual provider/event-source failures do not erase other valid evidence. Event timestamps require actual source publication, not inferred event-time fallbacks.
7. `AssetCardService`: background-only inference/risk/price ownership; private snapshot runtime state and card feature audits support recovery without repeating inference or reviving a pre-invalidation state. Latest input freshness is separate from a prior confirmed signal's structural anchor. Request methods are reads only. `_runtime` persistence metadata is removed before API serialization and is not in SSE payloads.
8. `DashboardHomeServiceImpl` attaches the independent `cardSignal` only after existing canonical ranking/projection. Controller card reconciliation uses the existing authenticated runtime-snapshot GET with `view=ASSET_CARDS`, at most six distinct pool members and Session identity only. Canonical fields, full pool membership and other Home regions are preserved.
9. `home-runtime.js`: PRICE/SIGNAL/RISK/HEALTH update only their card fields. Per-symbol/per-field-group versions reject stale responses without dropping a lower-version signal merely because a later price arrived first. Prices are coalesced to 1.5 seconds. One 60-second card reconciliation and a disconnected-only 15-second fallback pause while hidden. No card event calls `loadHome`. Existing unrelated system-status handling is not claimed to be removed.

## Pre-merge rollout correction (Owner-authorized, PR #1295)

Product Source Gate: PASS. Source mapping: registered canonical decision-chain
Appendix I.1/I.5/I.6 plus the Owner's explicit release-selection clarification.
The correction selects one visible card renderer; it does not combine two
confidence formulas or change canonical Decision/Plan fields.

| Configuration | Visible card path | New private runtime |
| --- | --- | --- |
| `enabled=false` | Existing card projection | No new workers, subscriptions or card writes |
| enabled + `SHADOW` | Existing card projection | Compute/persist private observations and prediction audit; no public card SSE/snapshot |
| enabled + `CANARY` | New `cardSignal` only for exact configured symbols; existing projection for others | Private computation may cover the observation set |
| enabled + `ACTIVE` | New `cardSignal` for all displayed assets | New card runtime |

The backend sets `cardSignalDisplayEnabled` from enabled/mode/exact symbol, not
model availability. Home clears hidden cardSignal values, and authenticated card
GETs filter non-cohort symbols only after existing user/member validation. SSE
publication uses the same cohort; persistence occurs independently before it.
CANARY/ACTIVE missing models remain on the new path with no legacy confidence.
No configuration defaults are enabled or promoted by this code change.
Public reads and every SSE type additionally require the currently validated
bundle's asset coverage and exact feature/model/calibration versions. A fresh
price does not make an old stored/cached model result trustworthy. Revocation
clears public direction/probabilities/confidence without rewriting private audit
facts or advancing the effective card clock; same-version safety messages cannot
be overwritten by stale positive responses.

Frontend selection uses the explicit boolean, not snapshot presence. An attached
SHADOW snapshot cannot switch the renderer. Cohort changes clear card caches,
pending price timers and version watermarks, and invalidate older card requests.
There is one confidence field per asset. Legacy/mixed cohorts retain the existing
60-second reconciliation / disconnected 15-second Home cycle; all-new cohorts
use card-only GETs. No price/card event triggers full-Home reconciliation.
Pins, ordering, dimensions and non-card business semantics remain unchanged.

Data mapping: native interoperability uses only disposable synthetic test models;
these cannot satisfy the checksummed real-history production bundle gate. New
real training/calibration sample counts and live acceptance remain unavailable.
Stop boundaries: no path expansion, persistent external database, secrets,
canonical business changes, real AI/Telegram/trades, merge or deployment.

## Risk production wiring and limits

| Type | Card-domain evidence | Missing-evidence behavior |
| --- | --- | --- |
| CHASE | Structural center distance and extension in signal-time ATR units | UNKNOWN without validated history |
| SHOCK | Actual closed 1m and 5m volatility | UNKNOWN per missing metric/history |
| REVERSAL | Closed 5m/1h/4h slope conflict and explicit structural breach | UNKNOWN without history; proven breach can independently produce HIGH |
| CROWDING | Funding, long/short ratio, OI change with their actual units | UNKNOWN; no aggregate-risk proxy |
| LIQUIDATION | Long/short liquidation amounts and imbalance | UNKNOWN; no fixed CoinGlass-presence grade |
| LIQUIDITY | Actual spread, covered 10/25bps depth and book imbalance | UNKNOWN; no 24h-range substitute |
| EVENT | Existing current macro/news facts with source publication and explicit severity | No matching event is not proof of complete coverage; UNKNOWN |
| DATA | Core Spot identity, completeness/freshness and source-loss facts | First-time absence UNKNOWN; confirmed source loss invalidates without reversing direction |

All eight production calculation branches are connected. This is **not** evidence that all eight have sufficient real historical distributions or live coverage. There are no real trained/calibrated artifacts in this change.

## Database definitions

V24 was the next unused migration after V23 on the merged baseline. The change adds only:

- `tm_asset_card_snapshot`
- `tm_asset_card_spot_bar`
- `tm_asset_card_feature_history`
- `idx_asset_card_spot_available`
- `idx_asset_card_feature_available`

`schema.sql` retains its original content and appends only corresponding card tables/indexes. Migration DDL has three new tables, zero alterations of existing tables, zero data-mutation statements and zero privilege statements. No Staging/Production migration was run. Runtime writes are confined to these card-owned stores.

## Validation evidence (local, 2026-09-10)

- Current comprehensive focused card/Home suite: **352 tests, 0 failures, 0 errors, 1 skip** (V24 disposable PostgreSQL: local Docker unavailable). Native interoperability executed, not skipped.
- Current full Maven, with native opt-in and local mock-server loopback access: **5,388 tests in 517 classes, 0 failures, 0 errors, 20 skips**, exit 0. The complete skip reasons are below. No failure or skip was removed to obtain this result.
- Historical full Maven before this correction: **5,378 tests, 0 failures, 0 errors, 21 skips**, exit 0. This is not the new-HEAD validation result. An earlier run had one frontend harness failure and seven local socket-bind errors. The harness now loads the real error handler and tracks card reconciliation separately from Home refresh; original timer assertions remain. The seven existing mock-HTTP tests passed with local loopback binding available; no real Telegram calls were made.
- Frontend card matrix (including timezone subprocesses), timestamp matrix, JavaScript syntax and `git diff --check`: PASS.
- Product Source Gate, task validation, exact machine-gate self-tests and workflow contract: PASS. Runtime handoff output on preserved WIP can still say `BLOCKED_WORKTREE_DIRTY`; this is reported, not bypassed or rewritten as a clean-tree PASS.
- Owner-authorized prerequisite: only `libomp` 22.1.8 was installed from its Homebrew bottle after confirming absence. Automatic updates/cleanup/dependent upgrades were disabled; no sudo, other package, Python version change or security-setting change. An initial TLS download failure was retried successfully.
- Fixed Python 3.12.14 / XGBoost **2.1.4** / NumPy 2.1.3 / SciPy 1.14.1: **18 tests PASS, 0 failures, 0 errors, 0 skips**. The previous inverse-label fixture correctly failed monotone calibration; the numerical test now trains two genuinely separate synthetic models and fits each side's own calibrator. Rejection of a collapsed anticorrelated fit remains tested. A separate red/green regression fixes risk-distribution metric keys being overwritten by observation-deduplication tuple keys; no scoring formula/threshold changed.
- Java 17 enhanced native suite (`AssetCardModelBundleTest`, `AssetCardBetaCalibrationTest` with `-DassetCard.testPython=...`): **11 tests PASS, 0 failures, 0 errors, 0 skips**. Python actually trains `long.ubj` and `short.ubj`; Java XGBoost4J **2.1.4** loads both and predicts three identical float32 input rows. Maximum raw delta: **0** for both sides. Independent fitted Beta maximum delta: **0** long, **2.20228566286e-20** short, below **1e-7**. The test asserts Java 17/Python XGBoost 2.1.4 and distinct calibrator parameters. JUnit `@TempDir` deletes test artifacts; no production manifest/model is emitted or committed. An initial AssertJ two-dimensional-array assertion compilation error was corrected to the same exact row-count assertion before this passing run.
- CI coverage correction: the repository's existing `ci` profile selects `smoke | core-regression`, not the full suite. The card calculation/market/risk/model, Mapper/V24 and Home projection/rendering tests now carry the existing `core-regression` tag. No workflow, global gate or existing assertion was removed or relaxed. The earlier 1,090-test CI run is not presented as card-test coverage; the updated exact-head run must include these tests.
- Exact `3f7857ef9236ed6da43f7618bbd55517a2dde139` CI ran **1,394 tests, 1 failure, 0 errors, 1 native opt-in skip**; workflow-contract passed. The failure exposed nanosecond epoch timestamps being rounded through a floating-point JSON tree. The regression now deliberately uses a nine-digit fractional instant on every OS and retains exact timestamp equality. Card storage uses a private mapper copy with decimal tree reads, preserving precision without changing the shared application mapper; the test explicitly verifies that isolation. The strengthened regression was reproduced RED locally, then the comprehensive 352-test focused suite passed. The final full-Maven count above is revalidated after this correction before the follow-up commit.

### V42 final Linux native-image attempt (2026-09-11)

The Dockerfile-path gate is effective through PR #1298 / `f24cdf2c5be755d75639317fdfba99cef29bc836`. The exact runtime-only `libgomp1` insertion was captured with business checkpoint `bb4242aef0ae2f85bc7d5d03bde9fa16e029e2cb`; the build stage, Java versions, `USER app` and application entrypoint were preserved. No host dependency installation or business application startup occurred.

Frozen source-only build context: `/private/tmp/asset-card-final-image.K61wfu/context.tar`, SHA-256 `d56173d0c1d7e6ace47699c38ee9d06c89ee3f59627f8eb61ecfa7add5edc110`; Dockerfile SHA-256 `b0d5daff86401ba3257bbc070a6d26e421425c67be60bda50a804b89675b87d0`. Later worktree edits are not attributed to this context. The local Docker daemon reports Linux aarch64; the requested image platform was `linux/arm64`.

Both the first attempt and the single authorized identical retry exited **1**, before `apt-get`, Maven compilation or native inference:

```sh
docker build --pull=false --platform linux/arm64 --progress=plain -t trine-asset-card-v42-native-test:local - < /private/tmp/asset-card-final-image.K61wfu/context.tar
```

Both failed at Dockerfile line 1 while resolving `docker/dockerfile:1.7`: `failed to fetch anonymous token: Get "https://auth.docker.io/token?scope=repository%3Adocker%2Fdockerfile%3Apull&service=registry.docker.io": EOF`. The frontend, base-image aliases, Docker configuration and credentials were not changed; no third attempt was made. Logs: `/private/tmp/asset-card-final-image.K61wfu/build-1.log` (SHA-256 `be7e67cdd215109dcb5cb94faa2b21b8396f45a3066252cdbdf6c554b38ebb1b`) and `build-2.log` (SHA-256 `c23624e51404c7b1fafeda68daf0baf6821f69cb669865c7300fe51183a97e71`).

Final image ID, application-JAR hash, installed `libgomp1` version and final-image long/short raw/Beta deltas are **NOT_AVAILABLE**. The exact local test-image tag is absent. No new UBJ fixture or production model was generated, so no new model artifact requires deletion. The earlier cached-JRE `libgomp.so.1` failure and macOS Java/Python fixture parity are diagnostic evidence only, not a substitute for the required final image. `FINAL_LINUX_NATIVE_PARITY=BLOCKED`; `MODEL_MODE=SHADOW`; `PRODUCTION_MODEL_READY=NO`; real training/calibration/final-test samples and metrics remain `UNKNOWN`. Current phase is NOT DONE.

### Historical V41 local skip reasons (20)

The former native interoperability skip is closed by the successful opt-in run.

| Test class / methods | Count | Reason |
| --- | ---: | --- |
| `AnalysisIdempotencyGuardPostgreSqlIntegrationTest`: `tenSequentialRetriesReturnOneCanonicalAnalysisRun`, `concurrentRetriesAreAtomicAndDoNotAbortTransactions(int)`, `differentKeysCreateDifferentRowsAndPostConflictTransactionsStayHealthy`, `sameKeyWithDifferentNormalizedPayloadFailsClosed` | 4 | Docker unavailable; existing `disabledWithoutDocker` policy |
| `PostgreSqlFlywayMigrationSmokeTest.postgreSqlCurrentMigrationRuntimeTest` | 1 | Docker/Testcontainers unavailable |
| `V23CoinGlassRuntimeSnapshotMigrationContractTest.disposablePostgresqlV23RequiresTemporaryCreateAndPreservesExistingRowsAndPrivileges` | 1 | Disposable PostgreSQL unavailable; no external database fallback |
| `V24AssetCardLiveSignalMigrationContractTest.postgresMigrationPreservesLegacyRowsAndDoesNotChangePrivileges` | 1 | Disposable Docker PostgreSQL unavailable; no external database fallback |
| `ControlledGeneratedReleaseLikeFixtureFlywayTest.createsOnlyTheApprovedLocalGeneratedFixtureAtFlywayV6` | 1 | P3 generated-fixture Flyway environment gate not enabled |
| `ControlledPostgreSqlFlywaySmokeTest.controlledExternalPostgreSqlFlywayMigrationsApplyWhenExplicitlyConfirmed` | 1 | Controlled external PostgreSQL environment missing; external DB access not authorized |
| `ControlledP3hComposeOfflineSmokeTest.disposableComposeProvesBootstrapSecretsProxyAndReadOnlyRole` | 1 | Explicit Docker contract opt-in not enabled |
| `ControlledGreenfieldFlywayV7ActionTest.migratesExactEmptyGreenfieldDatabaseFromV1ToV7AndRepeatsIdempotently` | 1 | P3-G Flyway action environment gate not enabled |
| `ControlledCurrentStateCloneFlywayActionTest.validatesOrMigratesOnlyAnApprovedLocalP3Database` | 1 | P3 controlled PostgreSQL action environment gate not enabled |
| `ControlledCurrentStateContentFingerprintTest`: `rollbackRestoresFingerprint`, `fingerprintOutputDoesNotContainRawModifiedValues`, `sameDataProducesMatchingFingerprint`, `sameRowCountTimeMutationIsDetected`, `sameRowCountPlanBoundaryMutationIsDetected`, `sessionTimezoneDoesNotChangeFingerprint`, `sameRowCountStatusMutationIsDetected` | 7 | P3 controlled content-fingerprint environment gate not enabled |
| `CoinGlassControlledSmokeTest.controlledSmokeUsesCoordinatorAndReturnsSanitizedSummary` | 1 | `COINGLASS_SMOKE_ENABLE_EXTERNAL_CALLS` unset; real external smoke not authorized |

## Remaining evidence and release limitations

`MODEL_MODE=SHADOW` (default; feature disabled until a separately authorized runtime configuration).
`PRODUCTION_MODEL_READY=NO`.
Real training/calibration samples: no real training performed in this work; counts NOT_AVAILABLE.
Real Brier/ECE/LogLoss: NOT_AVAILABLE, not values derived from test fixtures.
Real price/model/risk latency: NOT_MEASURED. The 1.5-second renderer, one-second background tick and bounded closed-bar wait are tested mechanisms, not live SLA evidence.
PostgreSQL/container checks need an available Docker/PostgreSQL environment; local skips are not a migration acceptance claim. Previous exact `7016ce88c501e23dba61b443dab42926617c1d0e` CI ran V24 PostgreSQL 2/2 and the existing PostgreSQL smoke 1/1 successfully; new-HEAD CI must be verified separately.
Business PR: #1295, must remain Draft. New-HEAD CI is pending at evidence commit; results will be recorded on this same PR after the push. Merge: not executed. Staging/Production deployment: not executed.
No official-domain acceptance is claimed for undeployed code.

## Files in this pre-merge correction

All 16 paths remain within the unchanged 48-path allowlist:

- `src/main/java/org/example/trademodel/assetcard/AssetCardProperties.java`
- `src/main/java/org/example/trademodel/assetcard/AssetCardService.java`
- `src/main/java/org/example/trademodel/service/impl/DashboardHomeServiceImpl.java`
- `src/main/java/org/example/trademodel/vo/DashboardHomeVO.java`
- `src/main/resources/static/js/home-runtime.js`
- `scripts/asset_card_model.py`
- `scripts/test_asset_card_model.py`
- `scripts/asset-card-runtime-matrix.mjs`
- `src/test/java/org/example/trademodel/assetcard/AssetCardServiceTest.java`
- `src/test/java/org/example/trademodel/assetcard/AssetCardIsolationIntegrationTest.java`
- `src/test/java/org/example/trademodel/assetcard/AssetCardModelBundleTest.java`
- `src/test/java/org/example/trademodel/controller/DashboardHomeControllerTest.java`
- `src/test/java/org/example/trademodel/controller/HomeUiReviewRuntimeContractTest.java`
- `src/test/java/org/example/trademodel/controller/GlobalFrozenUiAlignmentContractTest.java`
- `src/test/java/org/example/trademodel/service/impl/DashboardHomeServiceImplTest.java`
- `docs/evidence/asset_card_live_signal/IMPLEMENTATION_AND_ACCEPTANCE.md`

The two existing UI-contract files only opt their existing new-card fixtures into
the explicit cohort; original assertions and non-card methods are unchanged.
No Native APP, mobile-specific, canonical Plan/position/AI/Telegram, migration,
gate-owner or product-source contract file is changed by this correction.
