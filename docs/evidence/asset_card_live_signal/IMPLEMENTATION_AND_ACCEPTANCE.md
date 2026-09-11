# Asset-card live signal implementation evidence

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
