# Production Readiness Runbook

This runbook records production-readiness decisions and remaining gates. It is not a deployment approval.

## Status

Production Deployment Readiness is `BLOCKED`.

V1 is locally acceptance-ready. Production deployment remains blocked until real database migration validation, rollback drills, secrets management, observability evidence, real data-provider readiness evidence, real server smoke testing, HTTPS/reverse-proxy readiness, and an explicit human production release gate are complete. PDR-M7 adds an opt-in real provider live smoke harness, but it is not production deployment approval and does not make the system conditionally ready.

## PDR-2A Database Migration + Rollback Decision

PDR-2A records decisions only. It does not change runtime behavior.

- Production database target: PostgreSQL.
- Migration framework target: Flyway, SQL-first.
- Migration execution model: manual pre-deploy migration.
- Rollback policy: forward-only migrations plus pre-migration backup and restore.
- Initial recovery target: RPO 24h and RTO 4h.
- Current local/test bootstrap: `src/main/resources/schema.sql`.

## PDR-2B Flyway Baseline Skeleton

PDR-2B adds a non-default Flyway project skeleton only. It does not create an executable migration and does not change local/test runtime behavior.

- Flyway dependency scope: isolated behind the explicit Maven profile `flyway-migration`.
- Migration directory: `src/main/resources/db/migration/` exists with a README placeholder only.
- Executable migration files: none; do not add `V*.sql` until PDR-2C.
- Current local/test bootstrap: `src/main/resources/schema.sql` remains unchanged.
- Production database target: PostgreSQL.
- Real PostgreSQL-compatible baseline migration: deferred to PDR-2C.

## PDR-2C1 PostgreSQL Baseline Schema SQL

PDR-2C1 adds PostgreSQL-compatible baseline schema SQL files only. It does not validate the files against a live PostgreSQL database and does not change default local/test behavior.

- Table migration: `src/main/resources/db/migration/V1__baseline_schema_tables.sql`.
- Index migration: `src/main/resources/db/migration/V2__baseline_schema_indexes.sql`.
- Scope: current V1 tables and indexes only; no seed data and no foreign keys beyond current V1 semantics.
- Type strategy: generated identity columns, `TEXT` for JSON-like text fields, `TIMESTAMP WITHOUT TIME ZONE` for current `LocalDateTime` semantics, and PostgreSQL-compatible floating-point types.
- Deferred: mapper PostgreSQL compatibility, PostgreSQL driver, Testcontainers/Flyway smoke validation, real database connection, and backup/restore validation.

## PDR-2C2A Mapper PostgreSQL Upsert Variants

PDR-2C2A adds mapper-level PostgreSQL upsert variants only. It does not change default H2 behavior and does not validate against a live PostgreSQL database.

- MyBatis database-id detection: `DatabaseIdProvider` maps PostgreSQL to `postgresql`, H2 to `h2`, and MySQL to `mysql`.
- Asset state upsert: `AssetStateMapper.mergeUpsertCore` keeps the generic H2 `MERGE INTO ... KEY` fallback and adds a PostgreSQL `ON CONFLICT (symbol) DO UPDATE` variant.
- User config upsert: `UserConfigMapper.saveOrUpdate` keeps the generic MySQL/H2 `ON DUPLICATE KEY UPDATE` fallback and adds a PostgreSQL `ON CONFLICT (user_id) DO UPDATE` variant.
- Deferred: DATEADD / FORMATDATETIME mapper compatibility, PostgreSQL driver, Testcontainers/Flyway smoke validation, real database connection, and backup/restore validation.

## PDR-M1 PostgreSQL Runtime Pack

PDR-M1 adds a PostgreSQL runtime-readiness smoke layer. It does not deploy to production and does not connect to any real database.

- PostgreSQL JDBC driver is available as a runtime dependency.
- Testcontainers PostgreSQL and Flyway PostgreSQL smoke tests are available in test scope.
- Default Spring Boot H2 tests keep Flyway auto-configuration disabled through test-only config so `schema.sql` remains the local/default bootstrap path.
- Flyway V1/V2 migration smoke runs manually in a PostgreSQL Testcontainer when Docker is available; when Docker is unavailable, the smoke test skips gracefully.
- PostgreSQL mapper variants exist for DATEADD / FORMATDATETIME methods in AnalysisRun, PushSnapshot, HotResetEvent, PushRecheckLog, and MonitorAlert mapper paths.
- `UserPositionMapper` generated-key metadata now specifies `keyColumn = "id"`; PostgreSQL identity generated-key behavior is covered by the PostgreSQL smoke when Docker is available.
- Deferred: real production database connection, deployment packaging, auth/access control, secrets manager, observability, real server smoke, and production release-gate approval.

## PDR-M2 Server Deployment + Secrets + Smoke Pack

PDR-M2 adds a server deployment skeleton. It does not deploy to a real server and does not approve public production access.

- `Dockerfile` builds a generic application image with Maven wrapper in a build stage and runs the packaged jar in a JRE runtime stage as a non-root user.
- `docker-compose.yml` defines PostgreSQL, a manual Flyway migration runner profile, and the application service.
- The application port is bound to `${APP_BIND_ADDRESS:-127.0.0.1}:${APP_PORT:-8081}:8081` by default so it is not publicly exposed unless explicitly changed.
- `.env.example` documents required placeholder environment variables. Real `.env` files and local secret files are ignored and must not be committed.
- `scripts/prod-smoke.sh` performs readonly smoke checks for `/api/dashboard/home` and `/api/review/center`, including dashboard/review response shape and no-order/no-auto-trading safety fields.
- `scripts/prod-backup.sh` and `scripts/prod-restore.sh` provide PostgreSQL backup/restore templates that require explicit environment variables; restore also requires an explicit confirmation variable.
- Deferred: observability, real server deployment smoke, real restore drill evidence, secrets manager integration, external integration readiness, HTTPS/reverse-proxy readiness, and production release-gate approval.

## PDR-M3 Auth + Access Control Gate

PDR-M3 adds a single-operator Spring Security Basic Auth gate. It does not add a database user table, signup, roles UI, OAuth, public production exposure, or production release approval.

- Auth model: one in-memory operator account from `APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD`.
- Local/test compatibility: tests can disable the gate with `trade-model.auth.enabled=false`; local development keeps an explicit fallback account unless overridden.
- Protected surfaces: dashboard and review pages, dashboard/review APIs, manual user-position APIs, push recheck APIs, opportunity log APIs, rule/system/external-context/market/AI API surfaces, and write endpoints are behind authentication when the gate is enabled.
- Production credential guard: prod startup rejects missing admin credentials and unsafe defaults such as `password`, `admin`, `change-me`, `changeme`, `123456`, and the local fallback password.
- Smoke behavior: readonly production smoke checks require auth credentials through `SMOKE_AUTH_USERNAME` / `SMOKE_AUTH_PASSWORD` or `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD`; scripts must not print passwords.
- Deferred: HTTPS / reverse proxy configuration, secrets manager integration, credential rotation, audit logging, rate limiting, real server auth smoke evidence, and production release-gate approval.

## PDR-M4 Observability + Production Smoke Gate

PDR-M4 adds minimal Spring Boot Actuator health/readiness observability and strengthens the production smoke gate. It does not add a metrics stack, alerting platform, real server deployment, or production release approval.

- Actuator dependency: `spring-boot-starter-actuator` is present.
- Exposed actuator endpoints: only `health` is exposed over HTTP; liveness and readiness health groups are enabled under `/actuator/health/liveness` and `/actuator/health/readiness`.
- Health detail policy: details and components are hidden (`show-details=never`, `show-components=never`).
- Auth policy: minimal health/liveness/readiness endpoints are public; dashboard/review and operational APIs remain behind Basic Auth.
- Production guard: prod startup rejects actuator web exposure that is wider than `health`, including wildcard exposure.
- Smoke behavior: `scripts/prod-smoke.sh` checks public health/liveness/readiness, authenticated `/api/dashboard/home`, authenticated `/api/review/center`, dashboard safety fields, and Telegram non-connected status without printing passwords.
- Deferred: real server smoke evidence, log aggregation, metrics dashboards, alerting, HTTPS/reverse-proxy hardening, secrets manager integration, restore drill evidence, and production release-gate approval.

## PDR-M5 Real Data Provider Readiness Pack

PDR-M5 adds readonly provider-readiness status for real data-provider configuration. It does not call live providers by default and does not approve production deployment.

- Market data readiness: Binance public market data reports `CONFIGURED` only from config presence; local/dev `SIMULATED` remains `WAITING_SYNC` and is not production-ready.
- AI readiness: OpenAI, Gemini, and xAI report `CONFIGURED` only when explicitly enabled and configured; config-only status is not `CONNECTED`. Missing config for an explicitly enabled provider fails closed.
- External context readiness: optional news, macro calendar, and ETF flow keys are placeholders only. Config-only status is not `CONNECTED`; without verified sources the dashboard remains `WAITING_SYNC`.
- Dashboard status: `/api/dashboard/home.header.dataSourceText` and `/api/dashboard/home.diagnostics` expose readonly provider readiness without external calls.
- Smoke behavior: `scripts/prod-smoke.sh` checks provider readiness shape and rejects provider `CONNECTED` status unless `SMOKE_ALLOW_EXTERNAL_CALLS=true` and a verified source exists.
- Production guard: prod startup keeps rejecting simulated position provider and now rejects explicitly enabled AI providers with missing key/model/base URL.
- Deferred: live provider probes, secrets manager integration, real server provider smoke, HTTPS/reverse proxy hardening, Telegram send, Push dispatch, order/execution, and production release-gate approval.

## PDR-M6A Real Server Acceptance Evidence Gate

PDR-M6A adds a disciplined evidence framework for the human-run real-server acceptance gate. It does not deploy to a real server, use real secrets, connect Telegram, dispatch Push, trigger Push Recheck, call Binance private trading, or approve production deployment.

- Evidence template: `docs/PRODUCTION_ACCEPTANCE_EVIDENCE_TEMPLATE.md` records the required real-server outputs for Docker Compose config, PostgreSQL startup, Flyway migration, app prod startup, authenticated smoke, backup, restore, HTTPS/reverse-proxy/auth smoke, provider live smoke, and safety boundary checks.
- Release gate runner: `scripts/prod-release-gate.sh` orchestrates safe checks only. It can run `docker compose config`, `scripts/prod-smoke.sh`, and an optional backup drill when explicitly enabled. It never runs restore automatically and never prints secrets.
- Release gate smoke integrity: `scripts/prod-release-gate.sh` forces `SMOKE_PHASE=FETCH_AND_VALIDATE`, clears `SMOKE_RESPONSE_DIR`, and clears split-phase confirmation. Inherited split phases or canned response files cannot be counted as production server smoke.
- Local split smoke boundary: `FETCH` and `VALIDATE` require `SMOKE_SPLIT_PHASE_CONFIRM=I_CONFIRM_LOCAL_CONTROLLED_SPLIT_SMOKE`, an existing non-symlink response directory, and non-symlink JSON artifacts. Their output is labeled `LOCAL_CONTROLLED_SPLIT_ONLY`, not production release evidence.
- Release gate status policy: the script outputs `PRODUCTION_RELEASE_GATE: PASS / FAIL / INCOMPLETE`, but defaults to `INCOMPLETE` when real-server evidence is missing. A script PASS alone is not enough to change readiness.
- Environment contract: `.env.example` documents `RELEASE_GATE_REQUIRE_DOCKER`, `RELEASE_GATE_REQUIRE_BACKUP`, and `RELEASE_GATE_ALLOW_EXTERNAL_CALLS`; all default to conservative behavior.
- Required human evidence: production readiness cannot advance unless the evidence template is completed with redacted server outputs and reviewed by the release gate owner.
- Deferred: real server execution, secrets manager integration, restore drill execution, HTTPS/reverse-proxy implementation, live provider verification, metrics/log aggregation/alerting, and production release approval.

## PDR-M7 Real Provider Live Smoke Harness

PDR-M7 adds an explicit opt-in live provider smoke harness. It does not run live external calls by default, commit secrets, enable trading, send Telegram, dispatch Push, trigger Push Recheck, or approve production deployment.

- Script: `scripts/prod-provider-smoke.sh`.
- Default behavior: `PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=false` returns `PROVIDER_LIVE_SMOKE: SKIPPED` and exits successfully without network calls.
- Binance public market data: enable with `PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=true` and `PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED=true`. The check uses a Binance futures public market endpoint only; it does not require trading, withdrawal, or private order permissions.
- OpenAI: enable with `PROVIDER_SMOKE_OPENAI_ENABLED=true` and `OPENAI_API_KEY` in the server `.env`; the script performs a minimal models endpoint readiness call and does not print the key or response body.
- Gemini: enable with `PROVIDER_SMOKE_GEMINI_ENABLED=true` and `GEMINI_API_KEY` in the server `.env`; the script performs a minimal models endpoint readiness call and does not print the key or response body.
- XAI / Grok: enable with `PROVIDER_SMOKE_XAI_ENABLED=true` and `XAI_API_KEY` in the server `.env`; the script performs a minimal models endpoint readiness call and does not print the key or response body.
- External context placeholders: `NEWS_API_KEY`, `MACRO_CALENDAR_API_KEY`, and `ETF_FLOW_API_KEY` are reported as configured/skipped only; no live external-context provider call is implemented by this harness.
- Release gate integration: `scripts/prod-release-gate.sh` can require provider smoke only when `RELEASE_GATE_REQUIRE_PROVIDER_SMOKE=true`; otherwise provider smoke remains incomplete evidence, not a production-ready signal.
- Key policy: real keys must live only in the server `.env` or future secrets manager. Never commit `.env`, paste keys into evidence, or enable Binance withdrawal/trading/order permissions.
- Evidence policy: record only redacted output lines such as `BINANCE_PUBLIC_SMOKE: PASS`, `OPENAI_SMOKE: PASS`, and `PROVIDER_LIVE_SMOKE: PASS/INCOMPLETE/FAIL`.

## PDR-PF4 Current-State Migration + Rollback Drill

PDR-PF4 defines the current-state migration and rollback rehearsal process. It does not run migration, backup, restore, or release-gate commands, and it does not approve production deployment.

- Evidence doc: `docs/CURRENT_STATE_MIGRATION_ROLLBACK_DRILL.md`.
- Required preconditions: staging/clone database, redacted env-only secrets, pre-migration backup, separate recovery restore drill, human rollback owner, and no prohibited trading/push/fake-record surfaces.
- Backup path: `scripts/prod-backup.sh` requires explicit PostgreSQL environment variables and writes a local dump file.
- Restore path: `scripts/prod-restore.sh` requires explicit restore environment variables and refuses to run without `RESTORE_CONFIRM=I_UNDERSTAND_RESTORE_CAN_OVERWRITE_DATA`.
- Release gate path: `scripts/prod-release-gate.sh` can require backup evidence but never runs restore automatically; restore and human evidence remain external evidence requirements.
- Current package evidence: no production DB was accessed, no destructive DB operation was run, and no long PostgreSQL/Docker/Testcontainers smoke was rerun.
- Production readiness remains BLOCKED until a complete staging/server-backed evidence bundle is reviewed and every remaining production gate is proven.

## PDR-PF5 Secrets and Access Hardening

PDR-PF5 defines production secrets/access hardening requirements and records existing fail-closed guard evidence. It does not access real secrets, production servers, or production DB, and it does not approve production deployment.

- Evidence doc: `docs/SECRETS_AND_ACCESS_HARDENING.md`.
- Existing guards: prod startup rejects missing/H2 datasource, blank datasource password, simulated position provider, missing Binance credentials, blank/unsafe admin credentials, unsafe public bind, sensitive actuator exposure, and missing scheduler policy/classifications.
- Provider keys: AI provider keys are required only when the provider is explicitly enabled; provider live smoke defaults to no external calls and must not print keys.
- Secret handling: `.env`, real secrets, database dumps, screenshots with credentials, and terminal transcripts containing secrets must not be committed or pasted into evidence.
- Required future evidence: secrets manager integration, credential rotation drill, HTTPS/reverse-proxy proof, access/auth audit logging, rate limiting, and real server auth smoke.
- Current package evidence: no real secrets were accessed, no production server was accessed, and no production DB was accessed.
- Production readiness remains BLOCKED until the hardening evidence is implemented/collected and every remaining production gate is proven.


## PDR-PF6 Provider Live Smoke Evidence

PDR-PF6 records provider live-smoke evidence policy and safe no-call output. It does not access real secrets, run unapproved live external calls, connect to production servers, connect to production DB, or approve production deployment.

- Evidence doc: `docs/PROVIDER_LIVE_SMOKE_EVIDENCE.md`.
- Default smoke evidence: `PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=false` returns provider `SKIPPED` statuses without external network calls.
- Provider result policy: missing secrets or unavailable providers must be recorded as `SKIPPED_MISSING_SECRET`, `SKIPPED_DISABLED_BY_DEFAULT`, `SKIPPED_TIMEOUT`, `BLOCKED_PROVIDER_UNAVAILABLE`, or `FAIL`; never as fake PASS.
- Redaction policy: evidence must not include API key values, authorization headers, datasource URLs with credentials, response bodies, `.env` contents, or screenshots/transcripts containing secrets.
- Live rerun policy: run live provider smoke only after server/env secrets are ready, the operator explicitly approves external calls, and each command has a timeout.
- Current package evidence: no real secrets were accessed, no secret values were printed, no production server was accessed, and no production DB was accessed.
- Production readiness remains BLOCKED and production deployment cannot proceed.

## PDR-PF7 Push Recheck Quote-Unavailable Guard

PDR-PF7 records focused fail-closed evidence for Push Recheck when callers omit `currentPrice` and market quotes are unavailable. It does not approve production deployment.

- Evidence doc: `docs/PUSH_RECHECK_QUOTE_UNAVAILABLE_GUARD.md`.
- Missing quote behavior: unavailable, empty, null, non-positive, or throwing quote sources produce `fail_reason_json.code = QUOTE_UNAVAILABLE` and `RecheckStatusEnum.INVALIDATED`.
- Missing symbol behavior: snapshot symbol missing produces `fail_reason_json.code = PRICE_REQUIRED` and `RecheckStatusEnum.INVALIDATED`.
- Safety boundary: fail-closed paths remain review-only/non-executable and do not create orders, auto-open, auto-close, auto-reverse, auto-trade, send external push, create fake positions, or create fake review records.
- Production readiness remains BLOCKED and production deployment cannot proceed.

## PDR-PF8 Production Release Gate Closure

PDR-PF8 aggregates PF1-PF7 evidence and records the production release-gate decision. It does not access production DB, real secrets, or production servers, and it does not approve production deployment.

- Evidence doc: `docs/PRODUCTION_RELEASE_GATE_DECISION.md`.
- Final gate decision: `BLOCKED`.
- Production deployment decision: `DO NOT DEPLOY`.
- V1 status: local acceptance-ready only, not production-ready.
- Blocking evidence: PDR-PF3 PostgreSQL migration evidence remains `BLOCKED_TIMEOUT`; PDR-PF4 rollback/current-state migration drill is documented but not executed; PDR-PF5 secrets/access hardening evidence is incomplete; PDR-PF6 provider live smoke is `SKIPPED_DISABLED_BY_DEFAULT`; production release evidence bundle is incomplete.
- Next package must be explicit remediation, not deployment.

## PDR-PF9 PostgreSQL Migration Evidence Recovery

PDR-PF9 recovers the PostgreSQL migration evidence trail after PDR-PF3 `BLOCKED_TIMEOUT`. It does not access production DB, run destructive DB operations, change Flyway SQL, or approve production deployment.

- Evidence doc: `docs/POSTGRESQL_MIGRATION_EVIDENCE_RECOVERY.md`.
- Bounded command: `./mvnw -q -Dtest=PostgreSqlFlywayMigrationSmokeTest test` run through a Python 600-second timeout wrapper.
- Result: `BLOCKED_ENV_UNAVAILABLE`.
- Evidence: command completed in 1.61 seconds with `Tests run: 1, Failures: 0, Errors: 0, Skipped: 1`; Testcontainers could not find Docker or `/var/run/docker.sock`.
- Migration proof status: no PostgreSQL container ran, no Flyway V1/V2/V3 success log exists, and no PostgreSQL migration PASS is claimed.
- Production readiness remains BLOCKED and production deployment cannot proceed.

## PDR-PF10 PostgreSQL Environment Provisioning Evidence

PDR-PF10 records whether the current local/server environment can support Docker/Testcontainers-backed PostgreSQL migration smoke evidence. It does not access production DB, run destructive DB operations, change Flyway SQL, or approve production deployment.

- Evidence doc: `docs/POSTGRESQL_ENVIRONMENT_PROVISIONING_EVIDENCE.md`.
- Docker availability result: `DOCKER_MISSING`.
- Socket result: `/var/run/docker.sock` and `~/.docker/run/docker.sock` unavailable.
- Migration smoke result: `SKIPPED_ENV_UNAVAILABLE` because Docker availability was not confirmed.
- Migration proof status: no PostgreSQL container ran, no Flyway V1/V2/V3 success log exists, and no PostgreSQL migration PASS is claimed.
- Production readiness remains BLOCKED and production deployment cannot proceed.

## PDR-PF11 Controlled PostgreSQL Migration Smoke Evidence

PDR-PF11 attempts controlled PostgreSQL migration smoke evidence using Docker/Testcontainers or a controlled non-production PostgreSQL environment. It does not access production DB, run destructive DB operations, change Flyway SQL, or approve production deployment.

- Evidence doc: `docs/CONTROLLED_POSTGRESQL_MIGRATION_SMOKE_EVIDENCE.md`.
- Docker availability result: `DOCKER_MISSING`.
- Socket result: `/var/run/docker.sock` and `~/.docker/run/docker.sock` unavailable.
- Migration smoke result: `BLOCKED_ENV_UNAVAILABLE` because Docker/Testcontainers availability was not confirmed.
- Migration proof status: no PostgreSQL container ran, no controlled non-production PostgreSQL server was provided, no Flyway V1/V2/V3 success log exists, and no PostgreSQL migration PASS is claimed.
- Production readiness remains BLOCKED and production deployment cannot proceed.

## PDR-LIVE1 Controlled Live Dependency Acceptance

PDR-LIVE1 records controlled live dependency acceptance evidence. It is not production deployment, not public release, and not auto-trading. It does not access production DB, run destructive DB operations, print secrets, or approve production deployment.

- Evidence doc: `docs/CONTROLLED_LIVE_DEPENDENCY_ACCEPTANCE.md`.
- Controlled PostgreSQL result: `SKIPPED_MISSING_CONTROLLED_DB` because no disposable non-production PostgreSQL URL was present in environment.
- Binance public smoke result: `SKIPPED_DISABLED` because live external calls were not explicitly enabled.
- AI provider results: `SKIPPED_MISSING_SECRET` for OpenAI, Gemini, and xAI.
- External context/news/macro provider result: `SKIPPED_MISSING_SECRET`.
- application-prod safety result: PASS from focused safety tests.
- Scheduler policy result: PASS; Position Monitor scheduler remains default-off.
- Push Recheck quote-unavailable result: PASS; behavior remains fail-closed/review-only.
- Production readiness remains BLOCKED and production deployment cannot proceed.

## PDR-LIVE2 Controlled PostgreSQL Evidence Setup

PDR-LIVE2 prepares the next concrete path to PostgreSQL Flyway PASS evidence. It does not run Flyway, access production DB, run destructive DB operations, print secrets, or approve production deployment.

- Evidence doc: `docs/CONTROLLED_POSTGRESQL_EVIDENCE_SETUP.md`.
- Docker/Testcontainers path: `DOCKER_MISSING` in the current environment.
- Controlled external PostgreSQL path: `SKIPPED_MISSING_CONTROLLED_DB` because no disposable non-production PostgreSQL URL was present.
- Setup helper: `scripts/controlled-postgresql-evidence-plan.sh` is no-op/dry-run only; it redacts env presence, refuses production-like indicators, and never connects to a database.
- Migration proof status: no Flyway V1/V2/V3 PostgreSQL PASS evidence is claimed.
- Production readiness remains BLOCKED and production deployment cannot proceed.

## PDR-LIVE3 Controlled PostgreSQL Flyway Smoke Runner

PDR-LIVE3 adds a guarded controlled PostgreSQL Flyway smoke runner. It does not access production DB, run destructive DB operations, print secrets, or approve production deployment.

- Evidence doc: `docs/CONTROLLED_POSTGRESQL_FLYWAY_SMOKE_RUNNER.md`.
- Runner script: `scripts/controlled-postgresql-flyway-smoke.sh`.
- Test-only external smoke: `ControlledPostgreSqlFlywaySmokeTest`.
- Current local result: `SKIPPED_MISSING_CONTROLLED_DB` because no disposable non-production PostgreSQL URL was present.
- Runner requirements: explicit non-production confirmation, explicit Flyway run confirmation, no production-like JDBC URL indicators, timeout 300 seconds.
- Migration proof status: no Flyway V1/V2/V3 PostgreSQL PASS evidence is claimed until the runner passes against a disposable controlled PostgreSQL DB.
- Production readiness remains BLOCKED and production deployment cannot proceed.

## PDR-LIVE4 Controlled PostgreSQL Flyway Evidence Run

PDR-LIVE4 records operator-provided evidence from the guarded controlled PostgreSQL Flyway smoke runner.

Recorded evidence:

1. Command used explicit disposable non-production and schema-writing confirmations.
2. PostgreSQL target was disposable local Docker PostgreSQL at `localhost:55432` / `trade_model_smoke`.
3. PostgreSQL version was `16.14`.
4. Flyway validated 3 migrations.
5. Applied V1 baseline schema tables, V2 baseline schema indexes, and V3 scheme rule config defaults.
6. Final schema version was `v3`.
7. `CONTROLLED_POSTGRESQL_FLYWAY_RESULT: PASS`.
8. No production DB was accessed, no destructive operation outside the disposable controlled DB was run, and no secrets were printed.
9. Production readiness remains BLOCKED and production deployment cannot proceed.

This evidence closes the disposable controlled empty PostgreSQL Flyway migration gate only. It does not close current-state migration, rollback, restore, provider live smoke, secrets/access, real server smoke, release-owner approval, or full production release-gate evidence.


## PDR-LIVE5 Controlled Current-State Migration + Restore Drill Evidence

PDR-LIVE5 adds a guarded controlled current-state migration + restore drill helper and records the local evidence status. It does not access production DB or run destructive operations outside a disposable controlled DB.

Helper script:

```bash
bash scripts/controlled-current-state-migration-restore-drill.sh
```

Default/no-env result recorded by LIVE5:

1. Controlled source DB env is missing.
2. Controlled recovery DB env is missing.
3. Local PostgreSQL client tools `pg_dump`, `pg_restore`, and `psql` are missing.
4. Backup result is `SKIPPED_MISSING_CONTROLLED_DB`.
5. Restore result is `SKIPPED_MISSING_RECOVERY_DB`.
6. Current-state migration rehearsal result is `SKIPPED`.
7. No database access was attempted.
8. No production DB was accessed, no destructive operation outside a disposable controlled DB was run, and no secrets were printed.
9. Production readiness remains BLOCKED and production deployment cannot proceed.

Actual controlled execution requires disposable source/recovery DB env, PostgreSQL client tools, explicit non-production confirmation, explicit backup confirmation, and explicit restore confirmation.

## PDR-LIVE6 Controlled Backup Restore Evidence Run

PDR-LIVE6 records operator-provided disposable local PostgreSQL backup/restore evidence. It does not access production DB, commit secrets, or approve production deployment.

Recorded evidence:

1. Source DB was disposable local PostgreSQL at `localhost:55432` / `trade_model_smoke`.
2. Restore DB was disposable local PostgreSQL at `localhost:55433` / `trade_model_restore`.
3. `pg_dump` custom-format backup result: `PASS`.
4. `pg_restore` result: `PASS_WITH_WARNING`.
5. Restore warning: `unrecognized configuration parameter "transaction_timeout"`; `errors ignored on restore: 1`.
6. Restored `tm_*` table count: `27`.
7. Restored successful Flyway migration count: `3`.
8. Restore validation result: `PASS`.
9. No production DB was accessed, no destructive operation outside disposable controlled DB was run, and no secrets were printed or committed.
10. Production readiness remains BLOCKED and production deployment cannot proceed.

The `transaction_timeout` warning must be remediated or explicitly accepted by a controlled restore policy before this evidence can be used in a later production release gate.

## PDR-LIVE7 PostgreSQL 16-aligned Clean Restore Evidence

PDR-LIVE7 records operator-provided disposable local PostgreSQL backup/restore evidence using PostgreSQL 16 container-native tools. It does not access production DB, commit secrets, or approve production deployment.

Recorded evidence:

1. Source container: `trade-model-pg-smoke`.
2. Restore container: `trade-model-pg-restore`.
3. Source DB: `trade_model_smoke`.
4. Restore DB: `trade_model_restore`.
5. Backup tool: `pg_dump` from `postgres:16-alpine`; result `PASS`.
6. Restore tool: `pg_restore` from `postgres:16-alpine`; result `PASS_CLEAN`.
7. Restored `tm_*` table count: `27`.
8. Restored successful Flyway migration count: `3`.
9. Restore validation result: `PASS`.
10. No production DB was accessed, no destructive operation outside disposable controlled DB was run, and no secrets were printed or committed.
11. Production readiness remains BLOCKED and production deployment cannot proceed.

This resolves the prior local `transaction_timeout` warning for PostgreSQL 16-aligned disposable restore evidence only. It does not complete production-like current-state migration, rollback, provider, secrets/access, real server, or release-owner evidence.

## PDR-LIVE8 Controlled Provider Live Smoke Evidence Run

PDR-LIVE8 records controlled provider live-smoke evidence. It is not production deployment, does not place orders, does not send external Push, does not print or commit secrets, and does not approve production deployment.

Recorded evidence:

1. Default no-call provider smoke returned `PROVIDER_LIVE_SMOKE: SKIPPED` and skipped Binance/OpenAI/Gemini/xAI without external network calls.
2. Controlled Binance public smoke was run with `PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=true` and `PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED=true` through a bounded 300-second wrapper.
3. Binance public result: `PASS`; the public futures time endpoint was reachable.
4. OpenAI result: `SKIPPED_MISSING_SECRET`; no OpenAI endpoint was called.
5. Gemini result: `SKIPPED_MISSING_SECRET`; no Gemini endpoint was called.
6. xAI / Grok result: `SKIPPED_MISSING_SECRET`; no xAI endpoint was called.
7. External context/news/macro result: `SKIPPED_MISSING_SECRET`; keys/configuration were absent and no live external-context call is implemented by the harness.
8. No secret values were printed, no orders were placed, no external Push was sent, and no production DB was accessed.
9. Production readiness remains BLOCKED and production deployment cannot proceed.

This closes only the controlled Binance public provider smoke evidence gap. It does not close AI provider proof, external context proof, secrets manager, HTTPS/reverse-proxy, real server smoke, or release-owner evidence.

## PDR-LIVE9 Controlled AI Provider Smoke Evidence Run

PDR-LIVE9 records controlled AI provider smoke evidence for OpenAI, Gemini, and xAI/Grok. It is not production deployment, does not place orders, does not send external Push, does not print or commit secrets, and does not approve production deployment.

Recorded evidence:

1. AI provider config and `scripts/prod-provider-smoke.sh` were inspected.
2. Redacted key presence check returned missing for `OPENAI_API_KEY`, `GEMINI_API_KEY`, and `XAI_API_KEY`.
3. Controlled AI provider smoke was run with `PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=true`, OpenAI/Gemini/xAI flags enabled, and Binance disabled through a bounded 300-second wrapper.
4. OpenAI result: `SKIPPED_MISSING_SECRET` from script output `OPENAI_SMOKE: NOT_CONFIGURED - OPENAI_API_KEY missing`.
5. Gemini result: `SKIPPED_MISSING_SECRET` from script output `GEMINI_SMOKE: NOT_CONFIGURED - GEMINI_API_KEY missing`.
6. xAI / Grok result: `SKIPPED_MISSING_SECRET` from script output `XAI_SMOKE: NOT_CONFIGURED - XAI_API_KEY missing`.
7. No AI provider endpoint was called, no secret values were printed, no orders were placed, no external Push was sent, and no production DB was accessed.
8. Production readiness remains BLOCKED and production deployment cannot proceed.

This records exact AI provider skipped reasons only. It does not close AI provider PASS evidence, external context proof, secrets manager, HTTPS/reverse-proxy, real server smoke, or release-owner evidence.

## PDR-LIVE10 Secrets HTTPS Access Logging Rate Limit Evidence

PDR-LIVE10 records controlled production security evidence status. It is not production deployment, does not access production server, does not access production DB, does not print or commit secrets, and does not approve production deployment.

Recorded evidence:

1. Production profile guard status: `GUARD_PASS` for unsafe datasource, provider, admin credential, public bind, actuator, and scheduler settings.
2. Auth access-control status: `GUARD_PASS`; dashboard/review/API/write/recheck routes require Basic Auth and no executable trading route surface is present.
3. Actuator exposure status: `GUARD_PASS`; public health/liveness/readiness are minimal and sensitive actuator endpoints are not exposed.
4. Secret handling repository hygiene: `GUARD_PASS`; `.env.example` is placeholder-only and no real `.env` is tracked.
5. Secrets manager and credential rotation status: `MISSING_EVIDENCE`.
6. HTTPS/reverse proxy status: `DOCUMENTED_NOT_EVIDENCED`.
7. Access logging, auth audit logging, and rate limiting status: `MISSING_EVIDENCE`.
8. No production server was accessed, no production DB was accessed, no secrets were printed or committed, and production readiness remains BLOCKED.

This confirms fail-closed local/acceptance guard evidence only. It does not complete real server secrets manager, credential rotation, HTTPS/reverse proxy, access logging, auth audit logging, rate limiting, real server smoke, or release-owner evidence.

## PDR-LIVE11 Release Evidence Bundle + Remaining Blockers Closure

PDR-LIVE11 recorded the release evidence bundle status after LIVE10. It is not production deployment, does not access production server, does not access production DB, does not print or commit secrets, and does not approve production deployment.

Recorded evidence bundle:

1. PostgreSQL empty Flyway migration status: `PASS`; V1/V2/V3 applied and final schema version is v3 in a disposable controlled DB.
2. PostgreSQL 16 backup status: `PASS`.
3. PostgreSQL 16 clean restore status: `PASS_CLEAN`.
4. Restore validation status: `PASS`; restored `tm_*` table count is `27` and restored Flyway success count is `3`.
5. Binance public provider smoke status: `PASS`.
6. OpenAI, Gemini, and xAI/Grok provider smoke status: `SKIPPED_MISSING_SECRET`.
7. External context/news/macro provider smoke status: `SKIPPED_MISSING_SECRET`.
8. Production profile guard, auth access control, actuator exposure, and repository secret hygiene are `GUARD_PASS`.
9. At the time of LIVE11, secrets manager, credential rotation, access logging, auth audit logging, and rate limiting were missing; HTTPS/reverse proxy had requirements-only documentation before later LIVE13 template evidence.
10. Production readiness remains `BLOCKED`, and production deployment cannot proceed.

This bundle improved evidence visibility but did not close release readiness. Later LIVE12 and LIVE13 packages address app-level access/audit/rate-limit evidence and HTTPS/reverse-proxy template evidence; secrets manager, credential rotation, real HTTPS proxy smoke, and release-owner evidence remain blockers.

## PDR-LIVE12 Access Logging Auth Audit Rate Limit Evidence Remediation

PDR-LIVE12 records controlled application-level access/audit/rate-limit evidence. It is not production deployment, does not access production server, does not access production DB, does not print or commit secrets, and does not approve production deployment.

Recorded evidence:

1. Access logging status: `GUARD_PASS`; `ACCESS_LOG` records include method, sanitized path, status, duration, request id, and remote address without request body, query string, Authorization, Cookie, API key, password, token, datasource URL, or provider secret values.
2. Auth audit logging status: `GUARD_PASS`; `AUTH_AUDIT outcome=FAILURE` records authentication challenges without credential values.
3. Rate limiting status: `GUARD_PASS`; excessive requests return HTTP 429 with `Retry-After` and a compact JSON body.
4. Sensitive data redaction status: `GUARD_PASS`; sensitive headers are redacted by `SensitiveLogSanitizer` and covered by tests.
5. Production config guard status: `GUARD_PASS`; prod startup rejects disabled or invalid rate-limit settings.
6. Production readiness remains `BLOCKED`, and production deployment cannot proceed.

This closes the controlled application-level evidence gap for access logging, auth audit logging, and rate limiting. It does not close HTTPS/reverse-proxy evidence, real server log retention/aggregation evidence, secrets manager integration, credential rotation, AI provider PASS evidence, external-context provider proof, real server smoke, or release-owner approval.

## PDR-LIVE13 HTTPS Reverse Proxy Evidence

PDR-LIVE13 records controlled HTTPS/reverse-proxy evidence. It is not production deployment, does not access production server, does not access production DB, does not print or commit secrets, and does not approve production deployment.

Evidence doc: `docs/HTTPS_REVERSE_PROXY_EVIDENCE_RUN.md`.

Current LIVE13 evidence:

1. HTTPS / reverse proxy status: `DOCUMENTED_WITH_CONFIG`; the package records a template-only Nginx-style reverse proxy configuration and evidence checklist.
2. TLS termination status: `DOCUMENTED_WITH_CONFIG`; certificate material remains external and no certificates were inspected or committed.
3. HTTP-to-HTTPS redirect status: `DOCUMENTED_WITH_CONFIG`; real redirect smoke is still missing.
4. HSTS status: `DOCUMENTED_WITH_CONFIG`; real HSTS header evidence is still missing.
5. Proxy / forwarded header status: `DOCUMENTED_WITH_CONFIG`; `application-prod.yml` does not yet explicitly set `server.forward-headers-strategy`, so release evidence must decide and prove forwarded-header behavior.
6. Actuator exposure behind proxy: app-level `GUARD_PASS`, proxy route smoke still missing.
7. Auth smoke through proxy: `MISSING_EVIDENCE`; no controlled HTTPS endpoint was run in this package.
8. Access logging / auth audit / rate limiting behind proxy: app-level `GUARD_PASS`, proxy retention/aggregation and forwarded-IP evidence still missing.

This moves HTTPS/reverse-proxy evidence from requirements-only documentation to `DOCUMENTED_WITH_CONFIG`, but it does not close production readiness. Real HTTPS reverse-proxy smoke, secrets manager integration, credential rotation, AI provider PASS evidence, external-context provider proof, real server smoke, and release-owner approval remain incomplete.

## PDR-LIVE14 Secrets Manager Credential Rotation Evidence

PDR-LIVE14 records controlled secrets manager / credential rotation evidence. It is not production deployment, does not access production server, does not access production DB, does not access a real secret manager, does not print or commit secrets, and does not approve production deployment.

Evidence doc: `docs/SECRETS_MANAGER_CREDENTIAL_ROTATION_EVIDENCE_RUN.md`.

Current LIVE14 evidence:

1. Repo secret hygiene: `GUARD_PASS` for tracked-file hygiene; `.env.example` is placeholder-only and no real `.env` is tracked.
2. `application-prod.yml` env / secret requirements: `GUARD_PASS`; prod values are referenced through environment placeholders.
3. `ProductionProfileSafetyGuard` secret validation: `GUARD_PASS`; missing datasource/admin/Binance credentials and unsafe admin defaults fail closed.
4. Secrets manager integration: `DOCUMENTED_WITH_PLAN`; no real secret-store injection evidence exists.
5. Credential rotation: `DOCUMENTED_WITH_PLAN`; admin, datasource, Binance/API provider, and AI provider rotation checklists exist, but no rotation drill was run.
6. Secret redaction: app-level access/auth/rate-limit log redaction is `GUARD_PASS` from LIVE12; real server evidence still needs redacted proof.

This moves secrets manager / credential rotation from pure `MISSING_EVIDENCE` toward `DOCUMENTED_WITH_PLAN`, but it does not close production readiness. Real secret-store injection, actual credential rotation drills, AI/external provider release policy, real server smoke, and release-owner approval remain incomplete.

## PDR-LIVE15 Real Server Smoke Evidence Plan / Gate

PDR-LIVE15 records a controlled real-server / staging-server smoke evidence gate. It is not production deployment, does not access production DB, does not place orders, does not send external Push, does not print or commit secrets, and does not approve production deployment.

Evidence doc: `docs/REAL_SERVER_SMOKE_EVIDENCE_GATE.md`.

Current LIVE15 evidence:

1. Controlled server env presence is `SKIPPED_MISSING_CONTROLLED_SERVER`; no controlled endpoint was present and no server was contacted.
2. Server smoke, HTTPS endpoint classification, health/readiness smoke, and authenticated dashboard/review smoke are all `SKIPPED_MISSING_CONTROLLED_SERVER`.
3. Access logging / auth audit / rate-limit through server remains `SKIPPED_MISSING_CONTROLLED_SERVER`; LIVE12 app-level guards remain `GUARD_PASS`.
4. `scripts/controlled-real-server-smoke.sh` provides a default-skip wrapper that checks controlled env presence without printing values, requires HTTPS for non-local endpoints, and delegates authenticated checks to `scripts/prod-smoke.sh` only when credentials are already present.
5. Production readiness remains BLOCKED and production deployment cannot proceed.

Future controlled server smoke must be run only with a non-production endpoint and credentials provisioned outside chat. The evidence bundle must redact URL, username, password, tokens, cookies, response bodies, and secret values.

## PDR-LIVE16 Final Conditional Readiness Review

PDR-LIVE16 aggregates LIVE1-LIVE15 evidence and records the final conditional readiness decision for the current evidence bundle. It is not production deployment, does not access production server, production DB, real secrets, or provider endpoints, and does not approve production deployment.

Evidence doc: `docs/FINAL_CONDITIONAL_READINESS_REVIEW.md`.

Current LIVE16 decision:

1. Readiness decision: `BLOCKED`.
2. Deployment decision: `DO NOT DEPLOY`.
3. The system cannot move to `CONDITIONALLY_READY_CANDIDATE` because real server smoke is skipped, HTTPS/proxy auth smoke is missing, real secret-store injection and credential rotation drills are missing, AI/external provider proof is skipped or missing, and release-owner approval is missing.
4. `DOCUMENTED_WITH_PLAN`, `DOCUMENTED_WITH_CONFIG`, `SKIPPED`, and `MISSING_EVIDENCE` remain non-PASS statuses.
5. Production deployment cannot proceed until a later release-gate package records complete PASS or approved conditional evidence and release-owner approval.

## Current Schema State

- `schema.sql` remains the local/test bootstrap for now.
- A Flyway skeleton exists only behind the explicit `flyway-migration` Maven profile.
- PostgreSQL baseline schema SQL files exist as PDR-2C1 draft migrations.
- PostgreSQL upsert mapper variants exist for asset state and user config.
- PostgreSQL date-function mapper variants exist for the known DATEADD / FORMATDATETIME blockers.
- PostgreSQL JDBC driver is present, but no production database is connected by PDR-M1.
- Testcontainers/Flyway smoke is test-only and does not use real secrets.
- Dockerfile, Docker Compose, `.env.example`, readonly smoke script, and backup/restore template scripts exist after PDR-M2, but no real server is deployed and no public production access is approved.
- Single-operator Basic Auth exists after PDR-M3, and PDR-LIVE13 records HTTPS/reverse-proxy template evidence, but no real HTTPS proxy smoke, real secrets manager injection, actual credential rotation drill, real server auth smoke, or production release approval exists yet.
- Minimal public health/readiness endpoints and authenticated smoke checks exist after PDR-M4, but no real server smoke evidence, log aggregation, metrics dashboards, or alerting exists yet.
- PDR-M6A adds the real-server acceptance evidence template and conservative release gate runner, but no real server evidence has been collected yet.
- PDR-M7 adds the opt-in provider live smoke harness, PDR-LIVE8 records controlled Binance public provider PASS evidence, PDR-LIVE9 records OpenAI/Gemini/xAI as SKIPPED_MISSING_SECRET, PDR-LIVE10 records guard-pass plus missing security evidence status, PDR-LIVE11 aggregates the release evidence bundle as BLOCKED / DO NOT DEPLOY, and PDR-LIVE12 records controlled access/audit/rate-limit `GUARD_PASS`; PDR-LIVE13 records HTTPS/reverse-proxy `DOCUMENTED_WITH_CONFIG`; PDR-LIVE14 records secrets manager / credential rotation `DOCUMENTED_WITH_PLAN`; PDR-LIVE15 records real-server smoke as `SKIPPED_MISSING_CONTROLLED_SERVER`; PDR-LIVE16 keeps the final conditional readiness review BLOCKED; AI PASS evidence, external provider proof, real HTTPS proxy smoke, real secret-store injection, actual rotation drill, and real server hardening evidence remain missing.

## Migration Execution Policy

Production migrations must run as an explicit pre-deploy step.

Application startup must not silently mutate the production schema without a controlled migration process.

PDR-2C1 adds schema SQL drafts only. PDR-2C2A adds mapper upsert compatibility. PDR-M1 adds PostgreSQL driver, test-only Testcontainers/Flyway smoke, date-function mapper variants, and backup/restore command templates. PDR-M2 adds a manual Docker Compose migration runner entry. Production migrations are still not deployable until real environment validation and the production release gate are complete.

## Server Deployment Skeleton

The PDR-M2 Compose layout is intended for a controlled server rehearsal only:

```bash
cp .env.example .env
# edit .env and replace every change-me placeholder with deployment secrets
docker compose build
docker compose up -d postgres
docker compose --profile migrate run --rm migrate
docker compose up -d app
docker compose logs -f app
bash scripts/prod-smoke.sh
bash scripts/prod-release-gate.sh
# Optional provider live smoke, only after server keys are configured and approved for testing.
PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=true PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED=true bash scripts/prod-provider-smoke.sh
```

Health and smoke checks:

```bash
export APP_URL=http://localhost:8081
curl -fsS "$APP_URL/actuator/health"
curl -fsS "$APP_URL/actuator/health/liveness"
curl -fsS "$APP_URL/actuator/health/readiness"
APP_ADMIN_USERNAME=operator APP_ADMIN_PASSWORD='replace-me' bash scripts/prod-smoke.sh
```

Stop/restart commands:

```bash
docker compose stop app
docker compose restart app
docker compose down
```

Log and troubleshooting commands:

```bash
docker compose ps
docker compose logs --tail=200 app
docker compose logs --tail=200 postgres
docker compose logs --tail=200 migrate
```

Troubleshooting checklist:

- If `/actuator/health/readiness` is not `UP`, inspect app logs and database connectivity first.
- If smoke auth fails, rotate or re-enter `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD`; do not print secrets in logs.
- If dashboard/review smoke fails but readiness is `UP`, inspect application logs, database schema/migration state, and API response body locally.
- If safety fields fail, stop the deployment rehearsal and do not proceed to release-gate review.
- If rollback is needed, stop the app, restore to a controlled recovery database, start the app against the restored database, then rerun readiness and smoke checks.
- Record every server command result in `docs/PRODUCTION_ACCEPTANCE_EVIDENCE_TEMPLATE.md` or a copy of that template; redact secrets before sharing.

Security notes:

- The host app port binds to `127.0.0.1` by default.
- Do not expose the app publicly until HTTPS/reverse-proxy readiness, secrets manager integration, real server auth smoke, and the production release gate are complete.
- Do not commit `.env`, real secrets, database dumps, or local secret files.
- The Flyway migration runner is manual: `docker compose --profile migrate run --rm migrate`.
- This skeleton does not add Telegram send, Push send, order execution, auto-open, auto-close, or auto-trading.

Required environment categories:

- App bind/profile: `SPRING_PROFILES_ACTIVE`, `APP_PORT`, `APP_BIND_ADDRESS`, `SERVER_ADDRESS`, `TRADE_MODEL_PRODUCTION_ALLOW_PUBLIC_BIND`.
- Auth: `APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD`; optional smoke overrides `SMOKE_AUTH_USERNAME`, `SMOKE_AUTH_PASSWORD`.
- Database: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `PROD_DATASOURCE_URL`, `PROD_DATASOURCE_USERNAME`, `PROD_DATASOURCE_PASSWORD`.
- Position provider: `POSITION_PROVIDER_TYPE`, `BINANCE_API_BASE_URL`, `BINANCE_API_KEY`, `BINANCE_API_SECRET`.
- Optional AI provider toggles/keys: `TRADE_MODEL_AI_ENABLED`, provider-specific enabled flags, `OPENAI_API_KEY`, `GEMINI_API_KEY`, `XAI_API_KEY`.
- Optional external context provider placeholders: `NEWS_API_KEY`, `MACRO_CALENDAR_API_KEY`, `ETF_FLOW_API_KEY`.
- Smoke external-call policy: `SMOKE_ALLOW_EXTERNAL_CALLS=false` by default; default smoke does not call live providers.
- Provider live smoke: `PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=false`, `PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED=false`, `PROVIDER_SMOKE_OPENAI_ENABLED=false`, `PROVIDER_SMOKE_GEMINI_ENABLED=false`, and `PROVIDER_SMOKE_XAI_ENABLED=false` by default.
- Release gate runner: `RELEASE_GATE_REQUIRE_DOCKER=true`, `RELEASE_GATE_REQUIRE_BACKUP=false`, `RELEASE_GATE_ALLOW_EXTERNAL_CALLS=false`, and `RELEASE_GATE_REQUIRE_PROVIDER_SMOKE=false` by default.
- Future Telegram placeholders: `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`; these do not activate Telegram send.

## Real Provider Live Smoke

Run provider live smoke only on the server after keys are placed in the server `.env` or future secrets manager. Do not run it from Codex with real secrets.

Minimum first-run examples:

```bash
# Dry run: no network calls, should return SKIPPED.
PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=false bash scripts/prod-provider-smoke.sh

# Binance public market data only; no API key required and no trading permission required.
PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=true \
PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED=true \
bash scripts/prod-provider-smoke.sh

# AI providers; enable only providers that have server-side keys configured.
PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=true \
PROVIDER_SMOKE_OPENAI_ENABLED=true \
PROVIDER_SMOKE_GEMINI_ENABLED=true \
PROVIDER_SMOKE_XAI_ENABLED=true \
bash scripts/prod-provider-smoke.sh
```

Provider live smoke key rules:

- `OPENAI_API_KEY`, `GEMINI_API_KEY`, and `XAI_API_KEY` are optional until their smoke flags are enabled.
- `BINANCE_API_KEY` / `BINANCE_API_SECRET` are not required for the public Binance smoke. If Binance keys are configured for other readonly position checks, they must not have withdrawal permission or trading/order permission.
- Do not paste key values into terminal transcripts, screenshots, issue comments, PRs, or evidence docs.
- Telegram remains disabled unless a future package explicitly approves send behavior.
- Provider live smoke does not change decision semantics and cannot authorize trades.

## Production Release Gate Checklist

The real-server release gate must collect redacted evidence for every item below. If any item is missing, Production Deployment Readiness remains `BLOCKED`.

1. Docker Compose config: `docker compose config` succeeds and shows the expected `postgres`, `migrate`, and `app` services.
2. PostgreSQL startup: `docker compose up -d postgres`, `docker compose ps`, and PostgreSQL logs show a healthy database without printing secrets.
3. Flyway migration: `docker compose --profile migrate run --rm migrate` succeeds and `flyway_schema_history` records successful migrations.
4. App prod startup: `docker compose up -d app` starts the app with the prod profile and no unsafe production guard failure.
5. Authenticated smoke: `scripts/prod-smoke.sh` passes health, liveness, readiness, dashboard home, review center, and safety-field checks.
6. Backup drill: `scripts/prod-backup.sh` creates a timestamped ignored backup file without printing secrets.
7. Restore drill: `scripts/prod-restore.sh` restores into a controlled recovery database and smoke passes after restore. If restore is not run, readiness cannot advance beyond `BLOCKED`.
8. HTTPS / reverse proxy / auth smoke: HTTPS access and authenticated dashboard/review API checks pass through the intended server entrypoint.
9. Provider live smoke: `scripts/prod-provider-smoke.sh` evidence is recorded without printing secrets, without fake `CONNECTED` statuses, and without private trading calls.
10. Safety boundary check: no buy/sell/order/execute/auto-open/auto-close/auto-trading route or workflow is introduced.

Use `docs/PRODUCTION_ACCEPTANCE_EVIDENCE_TEMPLATE.md` as the evidence record.

## Rollback Policy

Migrations are forward-only.

Rollback is performed through a verified pre-migration backup and restore procedure.

No destructive migration is allowed without backup and restore validation.

## Backup And Restore Policy

Initial targets:

- RPO: 24h.
- RTO: 4h.

PDR-2A does not implement backup or restore commands. PDR-2D must define PostgreSQL-specific backup and restore commands and validation evidence.

### PostgreSQL Backup / Restore Draft

These commands are templates. Replace environment variables through the production secrets manager / deployment environment; do not hard-code secrets.

Pre-migration backup requirement:

```bash
PGPASSWORD="$PROD_DATASOURCE_PASSWORD" pg_dump \
  --host="$PROD_DATASOURCE_HOST" \
  --port="${PROD_DATASOURCE_PORT:-5432}" \
  --username="$PROD_DATASOURCE_USERNAME" \
  --dbname="$PROD_DATASOURCE_DATABASE" \
  --format=custom \
  --file="backup/trade_model_v1_$(date +%Y%m%d_%H%M%S).dump"
```

Restore template for a controlled recovery database:

```bash
PGPASSWORD="$RESTORE_DATASOURCE_PASSWORD" pg_restore \
  --host="$RESTORE_DATASOURCE_HOST" \
  --port="${RESTORE_DATASOURCE_PORT:-5432}" \
  --username="$RESTORE_DATASOURCE_USERNAME" \
  --dbname="$RESTORE_DATASOURCE_DATABASE" \
  --clean \
  --if-exists \
  --no-owner \
  --no-acl \
  --exit-on-error \
  "backup/trade_model_v1_YYYYMMDD_HHMMSS.dump"
```

Plain SQL restore alternative when using `pg_dump --format=plain`:

```bash
PGPASSWORD="$RESTORE_DATASOURCE_PASSWORD" psql \
  --host="$RESTORE_DATASOURCE_HOST" \
  --port="${RESTORE_DATASOURCE_PORT:-5432}" \
  --username="$RESTORE_DATASOURCE_USERNAME" \
  --dbname="$RESTORE_DATASOURCE_DATABASE" \
  --file="backup/trade_model_v1_YYYYMMDD_HHMMSS.sql"
```

Restore smoke checklist:

- DB connection succeeds with the restored credentials.
- `flyway_schema_history` exists and reports expected successful migrations.
- `/actuator/health/readiness` returns HTTP 200 with `status=UP` and no health detail payload.
- `/api/dashboard/home` returns HTTP 200 in the restored environment.
- `/api/review/center` returns HTTP 200 in the restored environment.
- `scripts/prod-smoke.sh` passes with authenticated dashboard/review API checks and safety-field checks.
- RPO 24h / RTO 4h target evidence is recorded for the restore drill.

Template scripts:

```bash
bash scripts/prod-backup.sh
RESTORE_CONFIRM=I_UNDERSTAND_RESTORE_CAN_OVERWRITE_DATA bash scripts/prod-restore.sh
```

The restore script must only target a controlled recovery database until a separate production restore drill has been approved.

## P3 Generated Rehearsal And Greenfield Provenance

P3 provides `scripts/generate-p3-release-like-fixture.sh` for deterministic
generated P3.1 input and retains
`scripts/controlled-current-state-clone-rehearsal-p3.sh` for generated or a
future separately sanctioned sanitized custom dump. The runner
fixes the target to digest-pinned localhost PostgreSQL on `127.0.0.1:55433`
and the three database names `trade_model_v1_p3_source`,
`trade_model_v1_p3_rehearsal`, and `trade_model_v1_p3_recovery`. It records
only aggregate/redacted evidence under ignored
`.runtime/postgresql-p3-rehearsal/`.

P3.1 is `PASS_GENERATED_RELEASE_LIKE_REHEARSAL`: source inventory,
container-native PostgreSQL 16 backup/restore, source/recovery fingerprints,
V6-to-V7 migration, historical inventory, app smoke, and cleanup passed with
zero unexpected business writes. Its status is
`GENERATED_RELEASE_LIKE_NOT_SANITIZED_CLONE`.

Approved decision `TMV1-GREENFIELD-20260715-001` selects
`GREENFIELD_NEW_DATABASE`: no historical business data must be preserved, no
existing formal business database exists, and the go-live initial state is
`EMPTY`. Local H2, test fixtures, and generated PostgreSQL rows are not
production history.

The P3.2 sanitized-clone route and final gate are
`NOT_APPLICABLE_BY_APPROVED_GREENFIELD_DECISION`, not PASS. No sanitized clone
will be sought or fabricated. The support code remains for recovery/incident
use or a future separately approved migration mode. Generated evidence must
not be described as existing-data migration, production history, writer
cutover, permission to start P4, or production readiness.

See `docs/GREENFIELD_DATABASE_PROVENANCE_DECISION.md`,
`docs/POSTGRESQL_CURRENT_STATE_CLONE_REHEARSAL_P3.md`, and
`docs/HISTORICAL_TIME_WRITER_CUTOVER_REGISTER.md`. PR #1127 and the separately
scoped P3-G package are merged/effective on main. P3-G remains local controlled
evidence and does not authorize P4. P4 remains blocked.

## Remaining Blockers

- Flyway remains non-default for runtime startup; test-only Flyway smoke exists.
- PostgreSQL baseline schema SQL has a Testcontainers smoke path, but this local environment may skip it when Docker is unavailable.
- Mapper PostgreSQL compatibility covers known upsert and DATEADD / FORMATDATETIME blockers; live PostgreSQL mapper execution remains to be expanded beyond smoke/static guards.
- Docker Compose deployment skeleton, `.env.example`, and smoke/backup/restore scripts exist; real server deployment smoke and real restore drill evidence are still missing.
- Basic Auth access control exists after PDR-M3, and PDR-LIVE13 records HTTPS/reverse-proxy template evidence, but real server auth smoke, real HTTPS proxy smoke, actual credential rotation drill, and real secrets manager integration remain missing.
- Observability is minimal after PDR-M4: health/readiness exists, but metrics dashboards, log aggregation, alerting, real server smoke evidence, and restore drill evidence remain missing.
- Provider readiness is readonly after PDR-M5, PDR-PF6 records safe no-call provider smoke evidence, PDR-LIVE8 records controlled Binance public provider PASS evidence, PDR-LIVE9 records AI provider missing-secret skipped evidence, PDR-LIVE11 records the aggregate release evidence bundle as BLOCKED / DO NOT DEPLOY, PDR-LIVE12 records application-level access/audit/rate-limit `GUARD_PASS`, PDR-LIVE13 records HTTPS/reverse-proxy `DOCUMENTED_WITH_CONFIG`, PDR-LIVE14 records secrets manager / credential rotation `DOCUMENTED_WITH_PLAN`, and PDR-LIVE15 records real-server smoke as `SKIPPED_MISSING_CONTROLLED_SERVER`; AI/external provider live PASS proof, real secrets manager injection, actual credential rotation drill, real HTTPS proxy smoke, and real server provider smoke still do not exist yet.
- PDR-M6A release-gate framework exists, but the required real-server evidence template is not completed yet.
- PDR-M7 provider live smoke harness exists, PDR-PF6 records default-disabled/no-call evidence, PDR-LIVE8 records controlled Binance public PASS evidence, PDR-LIVE9 records AI providers as SKIPPED_MISSING_SECRET, PDR-LIVE17 records that missing AI/external provider proof requires explicit release-owner classification, and PDR-LIVE18 records the decision register / waiver policy while approving no waiver. AI/external PASS checks remain opt-in and not proven.
- Deployment packaging is skeletal only and not release-gated.
- Secrets contract exists as placeholders and LIVE14 documents secret-store and rotation plans; no real secret-store injection or rotation drill exists.

## Greenfield P3-G Local Rehearsal

P3-G uses only disposable localhost resources and the exact confirmation
`I_CONFIRM_LOCAL_GREENFIELD_EMPTY_DATABASE_REHEARSAL`. Its guarded runner:

1. proves the database is empty before migration;
2. applies and validates Flyway V1-V7 without baseline, repair, or clean;
3. confirms repeat migrate applies zero migrations;
4. separates migration, backup, recovery, and application roles;
5. executes `prod-backup.sh` and `prod-restore.sh` in a digest-pinned
   PostgreSQL 16 Ops container;
6. compares Primary and Recovery structure, full content, Flyway history,
   schema types, sequence state, and historical-time inventory;
7. builds the application from an exact committed `git archive` context;
8. runs the `prod` profile with a read-only database role against Primary,
   Primary restart, and Recovery, with `prod-smoke.sh` fetching through a
   digest-pinned client on the internal network and validating only transient
   responses on the host under the explicit local split-smoke confirmation;
   it separately verifies empty asset cards and system-state cards fail closed,
   restricts empty market bias to `WAIT`/empty, rejects non-enum asset states,
   requires `assets` to be an actual array, and requires each placeholder's
   `timeframeFreshness` to be an exact four-key `NO_DATA` object;
   and
9. keeps Flyway, schedulers, provider/AI calls, trading, and external sends
   disabled throughout.

The observed merged-main result is
`PASS_LOCAL_CONTROLLED_GREENFIELD_REHEARSAL`. It is not a server smoke, Secret
Store/rotation drill, live-provider result, production cutover, or release
approval. Production Deployment Readiness remains `BLOCKED` and production
deployment cannot proceed.

## Controlled Staging P3-H

P3-H adds a safe-by-default non-production staging harness and deployment
templates. Its offline harness is `PASS`, and the exact disposable local
template completed as `PASS_LOCAL_DISPOSABLE_P3H_TEMPLATE_SMOKE`. Its overall
result remains `NOT_COMPLETE`: presence-only checks found none of the required
server/Secret Store inputs, so real staging is
`BLOCKED_MISSING_AUTHORIZED_INPUT`, while `SERVER_ACCESS` and `SECRET_ACCESS`
are `NOT_ATTEMPTED`.

The repository now has explicit `INITIALIZE_GREENFIELD`,
`RECOVER_GREENFIELD_INITIALIZATION`, and `STEADY_STATE_START` modes.
Initialization uses the deterministic empty-DB ->
role bootstrap -> Flyway V1-V7 -> grants -> Secret materialization -> app
health -> proxy health chain. Recovery requires a separate exact confirmation,
a continuous checksum-valid V1-VN prefix or V7 pre-grant state, exact
versioned rule rows and normalized schema fingerprints, exact P3-H
identity/objects, and zero business rows. Recovery and steady state validate
core state before refreshing grants, then require the full read-only contract;
neither requires an empty database or runs baseline, repair, or clean.
It also has strict attestation and canonical-file guards, an implemented
`SYSTEMD_CREDENTIALS` adapter, effective runtime-mount verification, fixed
non-root Config Tree materialization, pinned SSH identity, proxy-only ingress,
internal networking, fixed Host routing, strict TLS target/TLS 1.3 checks,
read-only application probing, rotation, backup/restore, reboot, redaction,
and leak-scan contracts. Round 2 local evidence includes retained-volume and
reboot-like restarts, zero repeated migrations, matching content fingerprints,
V2 preservation/V1 denial, injected-failure cleanup, strict object inventory,
exact SSH-line pinning, and exact Git archive image attribution. Local results
prove the template only; they do not prove a server deployment or server reboot.
Round 3 local evidence additionally covers V3-to-V7 recovery and V7 grant
recovery, rejection of invalid recovery states, measured zero-container
failed-start cleanup with persistent volumes retained, zero app/backup role
memberships, exact SELECT-only default/Sequence ACLs, and post-reboot V2
admin/database success with V1 denial.

Round 4 local evidence additionally covers exact V1-V7 rule-default and schema
contracts, fail-closed rule/schema mutation fixtures, effective SELECT-only
table privileges, absence of PUBLIC and column-level writes, and strict
pre-network grammar for staging hostname, SSH host, and SSH user. These remain
disposable local template results, not authorized-server evidence.

A future authorized run must provide the complete P3-H environment contract
outside chat and GitHub. It must collect redacted evidence from one approved
non-production Linux server, use full `FETCH_AND_VALIDATE` HTTPS smoke, restore
only to an independent recovery database, complete real secret rotations and
an explicitly authorized server reboot, and report zero secret leak
candidates. Missing or invalid input remains a blocker; it is never a skipped
PASS.

See `docs/CONTROLLED_STAGING_READONLY_TLS_SECRETSTORE_P3H.md`. P4 remains
`NO`, Production Deployment Readiness remains `BLOCKED`, and production
deployment cannot proceed.

## Next Packages

1. Complete Reviewer P3-H Offline Harness Round 5 re-review, preserving the
   distinction between local template PASS and missing real-staging evidence.
2. Obtain separately authorized controlled staging inputs before any real
   P3-H execution; never send secret values through chat, GitHub, or docs.
3. Execute server, Secret Store, TLS, HTTPS, rotation, backup/restore, reboot,
   and leak-scan gates only after all attestations validate.
4. Keep P4 blocked; neither P3-G nor an unexecuted P3-H package authorizes P4.
5. Production release-gate status closure only after completed redacted
   evidence and explicit approval.

## Explicit Non-Scope

PDR-M7 does not deploy to a real server, expose sensitive actuator endpoints, add Prometheus/Grafana, commit real secrets, connect Telegram, send Telegram, dispatch Push, trigger Push Recheck, connect Binance private trading execution, call live providers in default tests or smoke, run restore automatically, change schema.sql, change mapper SQL, add order/execution, or add auto-trading semantics. It adds the opt-in provider live smoke harness, optional release-gate integration, env placeholders, static tests, and docs/status updates while preserving Production Deployment Readiness as `BLOCKED`.
