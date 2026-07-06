# Trade Model V1 Current State

Contract: docs/PROJECT_DELIVERY_CONTRACT.md
Contract Version: v1.0
Current Phase: P0-0 Contract Lock + Baseline + Dead Code Candidate Report
Current Phase Status: DONE
Completion Effective State: derived by v1 state runtime
Existing Module Maturity: PARTIAL
Current Work Package: PDR-PF1 Status Source Cleanup
Next Business Phase: Post-freeze user acceptance / production readiness remediation
Next Business Phase Allowed: YES for scoped remediation/business packages; NO for production deployment
Production Deployment Readiness: BLOCKED
Historical Latest Production Readiness Package: PDR-M7 Real Provider Live Smoke Harness recorded on branch codex/pdr-m7-real-provider-live-smoke-harness

---

## Post-1066 Status Closure

PR #1066 is merged into `main` by merge commit `694c68d8418a207ac54c825f6c8e7e63f0853859`.

Post-merge local validation is recorded as passed:

1. `./mvnw test -q` PASS.
2. `bash scripts/v1-delivery-check.sh` PASS.
3. `V1_STATE_RESULT` PASS.
4. `WORKTREE_CLEAN` Yes.
5. `MAIN_SYNC` OK.
6. `OPEN_PR_STATUS` NONE.
7. `BLOCKERS` none.

Current package status after #1066:

- Review + AI conflict package: current round DONE / usable, progress 86%.
- Position monitor package: current round DONE / usable, progress 83%.
- Overall project real progress: 74%.
- Project real status: V1 local acceptance-ready, not production-ready.
- Production readiness remains BLOCKED.
- Next business phase allowed: YES.

The following prohibited items remain outside V1 scope:

- no auto-open
- no auto-close
- no auto-reverse
- no order execution
- no auto-trading
- no external push send
- no fake positions
- no fake review records

## Effective State Rule

Compatibility note: `scripts/v1-state.sh` still prints `CURRENT_PHASE: P0-0` as the contract-baseline phase. The active delivery handoff is tracked by `Current Work Package`, `Next Business Phase`, and the Delivery Progress Matrix.

P0-0 is effective only because its DONE commit is merged to `main`, local `main` is synced, and the worktree was clean when the runtime gate evaluated it.

P0-1 UserPosition is effective because its implementation is merged to clean / synced `main`.

P0-2 ExecutionPlan Source Gate is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P0-3.

P0-3 AccountRisk integrates UserPosition is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P0-4.

P0-4 PositionMonitorLog is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P0-5.

P0-5 PositionMonitorService is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P0-6.

P0-6 Review integrates UserPosition is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P1-1.

P1-1 PushRecheck semantic hardening is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P1-2.

P1-2 ConfusedState + AiConflict hardening is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P1-3.

P1-3 HotReset real action is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P1-4.

P1-4 OpportunityLog is effective because its implementation is merged to clean / synced `main` by PR #1017 and the runtime gate allowed P2-1.

P2-1 Macro / News / External Context is effective because its implementation is merged to clean / synced `main` by PR #1018 commit `d7fef874b39aabbd07f6b05fd97f4725e89e79b5` and the runtime gate allowed P2-2.

P2-2 AI Orchestrator + AiCallLog is effective because its implementation is merged to clean / synced `main` by PR #1019 commit `92fd7cbf17db31c8ea2bfd4673badde1c69d20cd` and the runtime gate allowed P2-3.

P2-3 Scheduler / Idempotency / Trace is effective because its implementation is merged to clean / synced `main` by PR #1020 commit `5c2b2b47eb7fa4cfc9c428ef022375f4ca890b23` and runtime state allowed P3-1 to proceed.

P3-1 Dashboard Final is effective because its final homepage UI layout is merged to clean / synced `main` by PR #1023 commit `f543832cf5907fe00920ca3f05666566daa16b7a`, full Maven validation passed, and the merged PR changed only `src/main/resources/templates/dashboard.html`.

P3-2 Full E2E Acceptance is effective because its acceptance evidence is merged to clean / synced `main` by PR #1025 Dashboard Manual UserPosition Binding, PR #1027 Full UserPosition lifecycle E2E acceptance, and PR #1028 Dashboard E2E state proof / commit `1b08abd`. Full Maven validation, delivery check, and `v1-state` pass on clean / synced main.

P3-3 Final Delivery & System Freeze is effective because final docs/status closure now records the local acceptance-ready freeze state on clean / synced `main`: full Maven validation passed, delivery check passed, `v1-state` passed with blockers none, `/api/dashboard/home` returned HTTP 200 success with the expected Dashboard Home shape, and `/api/review/center` returned HTTP 200 success with the expected Review Center shape.

---

## Current Allowed Work

Only the following work is allowed during and after the post-1068 / PDR-PF1 status-source cleanup:

1. Status-source cleanup.
2. The next explicitly scoped remediation package.
3. Production-readiness remediation only when explicitly scoped.

PDR-M7 is the historical latest production-readiness package, not the only currently allowed work. Production deployment remains BLOCKED until a separate production release gate clears it.

---

## Current Forbidden Work

The following work remains blocked during and after the post-1068 / PDR-PF1 status-source cleanup:

1. no auto-open
2. no auto-close
3. no auto-reverse
4. no order execution
5. no auto-trading
6. no external push send
7. no fake positions
8. no fake review records
9. no production-ready claim
10. Treating local acceptance readiness as production deployment approval.

---

## Current Known Critical Gaps

1. Production deployment remains blocked by non-production runtime/config evidence.
2. P3-3 Final Delivery & System Freeze completion does not prove production readiness, Push send, Telegram send, external channel, order execution, or auto-trading capability.
3. Production-readiness remediation remains a separate future scope and requires a separate human release gate.

## P3-2 Full E2E Acceptance Closure

Merged main evidence:

1. `DecisionServiceImplTest.getLatestDecisionResultsDoesNotInferOpenPositionFromTriggeredDecisionWithoutManualUserPosition` proves ExecutionPlan / triggered DecisionResult state without UserPosition does not render as an opened position.
2. `DecisionServiceImplTest.getLatestDecisionResultBySymbolUsesManualOpenUserPositionAsDashboardPositionSource` proves manual OPEN UserPosition is the dashboard real-position source.
3. `DecisionServiceImplTest.getLatestDecisionResultBySymbolExcludesClosedAndNonManualUserPositionRows` and `countOpenPositionsCountsOnlyManualOpenUserPositions` prove CLOSED and non-MANUAL UserPosition rows are excluded from open dashboard display.
4. `UserPositionFullLifecycleE2EAcceptanceTest.manualUserPositionFlowsThroughMonitorCloseReviewAndRuleFeedbackWithoutExecutableSurfaces` proves the UserPosition -> PositionMonitor -> manual close -> Review -> rule feedback chain.
5. `DashboardControllerTest.summary_json_exposesManualUserPositionFieldsAndKeepsExecutionPlanOnlyRowsNonPosition`, `dashboardTemplateHomePositionReadsOnlyManualUserPositionReadModelFields`, `StaticNoTradeInstructionGuardTest.dashboardPositionExecutionRowKeepsManualPositionDisplayPassive`, and `CandidatePushReviewOnlyMvpClosureTest.dashboardDisplaysInternalPushPreviewAsDisabledReviewOnlySurface` prove dashboard E2E key states and safety anchors.
6. `PositionMonitorServiceImplTest` and `PositionMonitorLogServiceImplTest` prove HOLD / LOGIC_WEAKENED / PLAN_INVALIDATED monitor outcomes and one-log monitor persistence.
7. `UserPositionRiskAdapterTest` and `PositionMonitorServiceImplTest.riskBlockedAndRiskIncreasedAreFailClosed` prove AccountRisk high-risk blocking.
8. `PushRecheckServiceImplTest` proves PushRecheck risk/confused/drifted/expired behavior remains review-only and does not create UserPosition.
9. `ConfusedStateServiceImplTest` and `HotResetServiceImplTest` prove ConfusedState and HotReset safety states.
10. `UserPositionReviewAdapterTest` and `ReviewControllerUserPositionReviewTest` prove Review execution deviation and rule feedback.
11. `OpportunityLogServiceImplTest`, `MacroEventServiceImplTest`, `NewsEventServiceImplTest`, `ExternalContextEvidenceBuilderTest`, `AiDecisionOrchestratorServiceImplTest`, and `AiCallLogServiceImplTest` prove the remaining contract E2E evidence around OpportunityLog, Macro / News, AI fallback, and AiCallLog.
12. `./mvnw test -q` passed on clean / synced `main`.
13. `bash scripts/v1-delivery-check.sh` passed on clean / synced `main`.
14. `bash scripts/v1-state.sh` passed with `WORKTREE_CLEAN: Yes`, `OPEN_PR_STATUS: NONE`, `MAIN_SYNC: OK`, `CLEAN_SYNCED_MAIN: YES`, and `BLOCKERS: none`.

P3-2 Full E2E Acceptance is DONE/effective as E2E acceptance evidence. Production Deployment Readiness remains BLOCKED and no production-ready claim is made.

## P3-3 Final Delivery & System Freeze Closure

Final local acceptance-ready freeze evidence:

1. `GET /api/dashboard/home` returned HTTP 200 success with `header`, `systemState`, `assets`, `positions`, `executionSuggestion`, `aiDecision`, `pushInbox`, `diagnostics`, and `safety`.
2. Dashboard Home Aggregation API is merged/effective.
3. Dashboard Data Fill P1-P5 are merged/effective: decision/systemState/assets, manual positions/executionSuggestion, AI role evidence, pushInbox readonly data, and Telegram readonly status contract.
4. Push Inbox remains readonly.
5. Telegram status remains `WAITING_SYNC` until a verified status source exists; no Telegram send is implemented.
6. `GET /api/review/center` returned HTTP 200 success with `summary`, `positionReviews`, `opportunityReviews`, `pushReviews`, and `ruleFeedback`.
7. `/review/dashboard` exists as the Review Center route; four tabs exist: 持仓复盘 / 机会复盘 / 推送复盘 / 规则反馈.
8. Review Center data is readonly and does not fabricate records.
9. Mainline validation passed on `main`: clean worktree before closure, no open PRs, MAIN_SYNC OK, full Maven PASS, delivery check PASS, and `v1-state` blockers none.
10. P0-0 through P3-2 remain DONE/effective.
11. Production Deployment Readiness remains BLOCKED.
12. No production deployment approval is granted.
13. No order / execution / auto-trading capability exists.

P3-3 Final Delivery & System Freeze is DONE/effective as a local acceptance-ready / read-only decision support / review workflow freeze. It is not production deployment ready.

---

## Current Deployment Readiness

Production deployment remains BLOCKED.

Blocking evidence:

- `src/main/resources/application.yml` uses `jdbc:h2:mem:trade_model_v1`.
- `src/main/resources/application.yml` has empty datasource password.
- `src/main/resources/application.yml` and `src/main/resources/application.properties` enable H2 console.
- `src/main/resources/application.properties` defaults `position.provider.type` to `SIMULATED`.
- PDR-1 added `src/main/resources/application-prod.yml` and `ProductionProfileSafetyGuard`, but this is only a production config/profile safety gate and does not prove production deployment readiness.
- PostgreSQL JDBC driver, test-only Testcontainers/Flyway smoke, mapper DATEADD / FORMATDATETIME variants, and backup/restore templates exist after PDR-M1, but no real production database is connected.
- Dockerfile, Docker Compose skeleton, `.env.example`, readonly smoke script, and backup/restore template scripts exist after PDR-M2, but no real server is deployed.
- Single-operator Basic Auth exists after PDR-M3, but no HTTPS/reverse-proxy hardening, credential rotation, audit logging, rate limiting, secrets manager integration, real server auth smoke, or production release gate exists yet.
- Minimal public health/readiness endpoints and authenticated smoke checks exist after PDR-M4, readonly provider readiness checks exist after PDR-M5, the PDR-M6A acceptance evidence framework exists, and the PDR-M7 opt-in provider live smoke harness exists, but no completed real-server evidence, metrics dashboards, log aggregation, alerting, live provider connection proof, or production release approval exists yet.
- No production database is connected in this package.
- No full observability stack, real server deployment smoke/rollback evidence, real restore drill evidence, secrets manager integration, verified external-provider integration, or production release gate exists yet.

### PDR-2A Database Migration + Rollback Decision Pack

PDR-2A records the production database and migration decisions only. It does not add runtime implementation.

- Production database target: PostgreSQL.
- Migration framework target: Flyway, SQL-first.
- Rollback policy: forward-only migrations plus pre-migration backup and restore.
- Migration execution model: explicit manual pre-deploy migration; application startup must not silently mutate the production schema without a controlled migration process.
- Initial recovery target: RPO 24h and RTO 4h.
- Current schema state: `src/main/resources/schema.sql` remains local/test bootstrap for now.
- No PostgreSQL driver, Flyway dependency, migration SQL, mapper SQL change, production DB connection, backup script, deployment script, secret, auth, Telegram send, Push send, order/execution, or auto-trading semantics are added by PDR-2A.
- Production readiness remains BLOCKED.

Database / deployment remaining blockers after PDR-M7:

- Flyway remains non-default for runtime startup; PDR-M1 adds only test/manual smoke coverage.
- PostgreSQL baseline schema SQL has a Testcontainers smoke path, but local evidence depends on Docker availability.
- Mapper PostgreSQL variants cover known upsert and DATEADD / FORMATDATETIME blockers; broader live mapper execution remains deferred.
- Docker Compose deployment skeleton, `.env.example`, smoke/backup/restore scripts, the PDR-M6A evidence template, the conservative release gate runner, and the opt-in provider live smoke harness exist, but real server deployment smoke, real provider smoke evidence, and real restore drill evidence are still missing.
- Auth/access control baseline exists as single-operator Basic Auth, but real server auth smoke, HTTPS/reverse-proxy hardening, credential rotation, audit logging, rate limiting, and secrets manager integration remain missing.
- Observability is minimal: health/readiness exists, but metrics dashboards, log aggregation, alerting, real server smoke evidence, and restore drill evidence remain missing.
- Deployment packaging is skeletal only and not release-gated.
- Secrets contract exists as placeholders only, including admin credentials; secrets manager integration is missing.

Next production-readiness packages:

1. User-run real server acceptance evidence collection using `docs/PRODUCTION_ACCEPTANCE_EVIDENCE_TEMPLATE.md`, `scripts/prod-release-gate.sh`, and `scripts/prod-provider-smoke.sh`.
2. PDR-M8 Secrets Manager / HTTPS / Reverse Proxy / Credential Rotation / Audit Hardening.
3. PDR-M9 Production release-gate status closure only after completed redacted evidence and explicit approval.

### PDR-2B Flyway Baseline Skeleton

PDR-2B adds the Flyway project skeleton without changing default local/test runtime behavior.

- Flyway dependency scope: explicit non-default Maven profile `flyway-migration` only.
- Migration directory: `src/main/resources/db/migration/` with README placeholder only.
- Executable migration files: none; no `V*.sql` baseline exists yet.
- Current schema state: `src/main/resources/schema.sql` remains local/test bootstrap and is not copied.
- Production target database remains PostgreSQL.
- Real PostgreSQL-compatible baseline migration is deferred to PDR-2C.
- No PostgreSQL driver, production DB connection, schema change, mapper SQL change, application config change, backup script, deployment script, secret, auth, Telegram send, Push send, order/execution, or auto-trading semantics are added by PDR-2B.
- Production readiness remains BLOCKED.

### PDR-2C1 PostgreSQL Baseline Schema SQL

PDR-2C1 adds PostgreSQL-compatible Flyway baseline schema SQL drafts without changing local/test runtime behavior.

- Table migration: `src/main/resources/db/migration/V1__baseline_schema_tables.sql`.
- Index migration: `src/main/resources/db/migration/V2__baseline_schema_indexes.sql`.
- Scope: current V1 table and index semantics only.
- JSON-like fields remain `TEXT`; timestamp fields use `TIMESTAMP WITHOUT TIME ZONE`; generated numeric ids use PostgreSQL identity columns.
- V1 remains foreign-key-free for this baseline, matching current schema semantics.
- No seed data, schema.sql change, mapper SQL change, Java/config/test change, PostgreSQL driver, Testcontainers, production DB connection, backup script, deployment script, secret, auth, Telegram send, Push send, order/execution, or auto-trading semantics are added by PDR-2C1.
- Mapper compatibility and real PostgreSQL validation remain deferred to PDR-2C2/PDR-2C3.
- Production readiness remains BLOCKED.

### PDR-2C2A Mapper PostgreSQL Upsert Variants

PDR-2C2A adds MyBatis mapper-level PostgreSQL upsert variants without changing default local/test H2 behavior.

- `MyBatisDatabaseIdProviderConfig` maps PostgreSQL to `postgresql`, H2 to `h2`, and MySQL to `mysql` for MyBatis database-specific annotation selection.
- `AssetStateMapper.mergeUpsertCore` keeps the generic H2 `MERGE INTO ... KEY` fallback and adds a PostgreSQL `ON CONFLICT (symbol) DO UPDATE` variant that does not overwrite `hot_reset_*` fields.
- `UserConfigMapper.saveOrUpdate` keeps the generic MySQL/H2 `ON DUPLICATE KEY UPDATE` fallback and adds a PostgreSQL `ON CONFLICT (user_id) DO UPDATE` variant.
- Focused tests prove default H2 upsert behavior still works and annotation guards prove PostgreSQL variants do not contain H2/MySQL upsert syntax.
- DATEADD / FORMATDATETIME mapper compatibility, PostgreSQL driver/Testcontainers validation, production DB connection, backup script, deployment script, secret, auth, Telegram send, Push send, order/execution, and auto-trading semantics remain deferred / blocked.
- Production readiness remains BLOCKED.

### PDR-M1 PostgreSQL Runtime Pack

PDR-M1 adds PostgreSQL runtime smoke readiness while preserving default H2/local behavior.

- `pom.xml` includes PostgreSQL JDBC runtime dependency and test-scoped Testcontainers / Flyway PostgreSQL smoke dependencies.
- `src/test/resources/application.properties` disables Spring Boot Flyway auto-configuration for default tests so `schema.sql` remains the H2 local/test bootstrap.
- `PostgreSqlFlywayMigrationSmokeTest` manually runs Flyway V1/V2 migrations against PostgreSQL Testcontainers when Docker is available, verifies the 27 V1 tables, critical indexes, Flyway history success, and PostgreSQL identity generated-key behavior.
- PostgreSQL `databaseId = "postgresql"` mapper variants replace DATEADD / FORMATDATETIME syntax for AnalysisRun, PushSnapshot, HotResetEvent, PushRecheckLog, and MonitorAlert targeted methods while leaving generic H2 SQL unchanged.
- `UserPositionMapper.insert` specifies `keyColumn = "id"` for generated-key compatibility.
- `PRODUCTION_READINESS_RUNBOOK.md` records pg_dump / pg_restore / psql restore templates and a restore smoke checklist.
- No real PostgreSQL connection, schema.sql change, production config change, deployment script, secret, auth, Telegram send, Push send, order/execution, or auto-trading semantics are added.
- Production readiness remains BLOCKED.

### PDR-M2 Server Deployment + Secrets + Smoke Pack

PDR-M2 adds a Docker Compose server deployment skeleton while preserving the no-trading and blocked-production boundaries.

- `Dockerfile` builds the Spring Boot app with Maven wrapper in a JDK build stage and runs the packaged jar in a JRE runtime stage as a non-root user.
- `docker-compose.yml` defines PostgreSQL, a manual Flyway migration runner profile, and the app service. The host app port binds to `127.0.0.1` by default.
- `.env.example` records placeholder-only app, PostgreSQL, Binance position provider, optional AI, future Telegram, backup, and restore variables. `.env` and secret/backup outputs are ignored.
- `scripts/prod-smoke.sh` performs readonly checks for `/api/dashboard/home` and `/api/review/center`, including safety fields and Telegram non-connected status.
- `scripts/prod-backup.sh` and `scripts/prod-restore.sh` provide PostgreSQL backup/restore templates with required env vars and no hard-coded secrets; restore requires explicit confirmation.
- No real server deployment, real secrets, Java business logic change, schema.sql change, mapper SQL change, Flyway migration change, auth implementation, Telegram send, Push dispatch, order/execution, or auto-trading semantics are added.
- Production readiness remains BLOCKED.

### PDR-M3 Auth + Access Control Gate

PDR-M3 adds a single-operator Spring Security Basic Auth gate while preserving the no-trading and blocked-production boundaries.

- `pom.xml` includes Spring Security and Spring Security test support.
- `SecurityConfig` protects dashboard/review pages and operational/dashboard/review API routes when `trade-model.auth.enabled=true`.
- The operator account is sourced from `APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD` through configuration; broad legacy tests explicitly disable auth through test config.
- `ProductionProfileSafetyGuard` rejects missing admin credentials and unsafe defaults in the prod profile.
- `.env.example`, `docker-compose.yml`, and `scripts/prod-smoke.sh` now include auth credential handling without printing passwords.
- Targeted security tests prove protected routes require Basic Auth, authenticated requests succeed, write endpoints are protected, static resources are not turned into auth challenges, and no buy/sell/order/execute/auto-trading route surface is introduced.
- No real server deployment, real secrets, database user table, signup/login UI, OAuth, role UI, Java trading logic change, schema.sql change, mapper SQL change, Flyway migration change, Telegram send, Push dispatch, order/execution, or auto-trading semantics are added.
- Production readiness remains BLOCKED.

### PDR-M4 Observability + Production Smoke Gate

PDR-M4 adds minimal health/readiness observability and strengthens authenticated production smoke checks while preserving blocked-production and no-trading boundaries.

- `pom.xml` includes Spring Boot Actuator.
- `application.yml` exposes only the `health` actuator endpoint and enables `/actuator/health/liveness` and `/actuator/health/readiness` with details/components hidden.
- `SecurityConfig` permits public minimal health/liveness/readiness and keeps dashboard/review plus operational APIs authenticated.
- `ProductionProfileSafetyGuard` rejects prod actuator exposure wider than `health`, including wildcard exposure.
- `scripts/prod-smoke.sh` checks public health/liveness/readiness, authenticated `/api/dashboard/home`, authenticated `/api/review/center`, no-auto-trading/no-order safety fields, and Telegram non-connected status without printing passwords.
- Targeted health/actuator/security tests prove public minimal health behavior, sensitive actuator non-exposure, auth boundary preservation, prod guard rejection, and smoke script syntax.
- No real server deployment, real secrets, sensitive actuator endpoints, Prometheus/Grafana, alerting stack, Java business logic change, schema.sql change, mapper SQL change, Flyway migration change, Telegram send, Push dispatch, order/execution, or auto-trading semantics are added.
- Production readiness remains BLOCKED.

### PDR-M5 Real Data Provider Readiness Pack

PDR-M5 adds readonly provider-readiness status and production smoke checks while preserving blocked-production and no-trading boundaries.

- `ProviderReadinessService` maps Binance public market data, AI providers, and external context placeholders to safe statuses: `CONFIGURED`, `WAITING_SYNC`, `NOT_CONFIGURED`, `FAIL_CLOSED`, or `UNKNOWN`.
- `CONNECTED` is not reported from config-only fields; live-provider proof remains deferred.
- `/api/dashboard/home.header.dataSourceText` and `/api/dashboard/home.diagnostics` expose provider readiness without external calls.
- `.env.example` records placeholder-only Binance, AI, news, macro calendar, ETF flow, and smoke external-call variables; no real secrets are committed.
- `ProductionProfileSafetyGuard` rejects explicitly enabled production AI providers with missing key/model/base URL while preserving local/dev compatibility.
- `scripts/prod-smoke.sh` checks dashboard provider readiness and defaults `SMOKE_ALLOW_EXTERNAL_CALLS=false`, so smoke does not call live providers by default.
- Targeted provider/config/production tests prove config-only is not `CONNECTED`, simulated fallback remains local/dev only, AI missing keys fail closed when explicitly enabled, and smoke syntax/provider checks remain safe.
- No real server deployment, real secrets, live external provider call in default tests/smoke, Binance private trading, Telegram send, Push dispatch, schema change, mapper SQL change, dashboard/review template change, order/execution, or auto-trading semantics are added.
- Production readiness remains BLOCKED.

### PDR-M6A Real Server Acceptance Evidence Gate

PDR-M6A adds the real-server acceptance evidence gate framework while preserving blocked-production and no-trading boundaries.

- `docs/PRODUCTION_ACCEPTANCE_EVIDENCE_TEMPLATE.md` records required redacted evidence for Docker Compose config, PostgreSQL startup, Flyway migration, app prod startup, authenticated smoke, backup drill, restore drill, HTTPS/reverse-proxy/auth smoke, provider live smoke, and safety boundary checks.
- `scripts/prod-release-gate.sh` orchestrates only safe checks: Docker Compose config, `scripts/prod-smoke.sh`, and an optional backup drill when explicitly enabled. It does not run restore automatically and does not print secrets.
- `.env.example` documents conservative release gate flags: `RELEASE_GATE_REQUIRE_DOCKER=true`, `RELEASE_GATE_REQUIRE_BACKUP=false`, and `RELEASE_GATE_ALLOW_EXTERNAL_CALLS=false`.
- `PRODUCTION_READINESS_RUNBOOK.md` now includes the release gate checklist and server evidence collection process.
- No real server deployment, real secrets, production DB connection from Codex, restore execution against a real DB, Telegram send, Push dispatch, Push Recheck execution, Binance private trading, Java/test/schema/mapper/template change, order/execution, or auto-trading semantics are added.
- Production readiness remains BLOCKED until the user supplies completed real-server evidence and explicitly approves a release-gate status closure.

### PDR-M7 Real Provider Live Smoke Harness

PDR-M7 adds an opt-in live provider smoke harness while preserving blocked-production and no-trading boundaries.

- `scripts/prod-provider-smoke.sh` defaults to `PROVIDER_LIVE_SMOKE: SKIPPED` with no network calls unless `PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=true` is explicitly set.
- Binance public market smoke is controlled by `PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED=true` and uses a public market endpoint only, with no trading, withdrawal, or private order permission required.
- OpenAI, Gemini, and XAI smoke checks are controlled by provider-specific flags and only run when server-side keys are configured; keys and response bodies are not printed.
- `scripts/prod-release-gate.sh` can require provider smoke only with `RELEASE_GATE_REQUIRE_PROVIDER_SMOKE=true`; otherwise provider smoke remains incomplete evidence, not a readiness signal.
- `.env.example`, `PRODUCTION_READINESS_RUNBOOK.md`, and `PRODUCTION_ACCEPTANCE_EVIDENCE_TEMPLATE.md` document the provider smoke env contract and redacted evidence fields.
- `ProdSmokeScriptHealthTest` adds static/default checks proving shell syntax, default skipped behavior, no Binance order/withdraw endpoints, no obvious secret echo, and optional release-gate integration.
- No real server deployment, real secrets, committed `.env`, real network calls in tests/default smoke, Telegram send, Push dispatch, Push Recheck execution, Binance private trading, schema/mapper/template change, order/execution, or auto-trading semantics are added.
- Production readiness remains BLOCKED until the user supplies completed real-server and provider-live-smoke evidence and explicitly approves a release-gate status closure.

---

## Derived / Compatibility Sources

`docs/ACTIVE_MAINLINE_STATUS.yml` and `docs/CODEX_NEXT_TASK.yml` are derived compatibility files only.
They do not override the Delivery Contract, Delivery Progress Matrix, or this Current State file.

Legacy V1 documents remain historical audit and asset evidence only.
Review-only slice count is no longer a delivery completion standard.

---

## Rule

No production deployment approval or runtime production implementation package may start until a separate explicit production release gate addresses the blocked runtime/config evidence and preserves the permanent no auto-trading / no order-execution safety boundaries. Docs-only production-readiness decision packs may record decisions while keeping deployment readiness BLOCKED.

## Workflow PR Status

- CURRENT_PACKAGE_PR: none
- UNRELATED_OPEN_PRS: none
