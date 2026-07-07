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

## Current Schema State

- `schema.sql` remains the local/test bootstrap for now.
- A Flyway skeleton exists only behind the explicit `flyway-migration` Maven profile.
- PostgreSQL baseline schema SQL files exist as PDR-2C1 draft migrations.
- PostgreSQL upsert mapper variants exist for asset state and user config.
- PostgreSQL date-function mapper variants exist for the known DATEADD / FORMATDATETIME blockers.
- PostgreSQL JDBC driver is present, but no production database is connected by PDR-M1.
- Testcontainers/Flyway smoke is test-only and does not use real secrets.
- Dockerfile, Docker Compose, `.env.example`, readonly smoke script, and backup/restore template scripts exist after PDR-M2, but no real server is deployed and no public production access is approved.
- Single-operator Basic Auth exists after PDR-M3, but no HTTPS/reverse-proxy, secrets manager, credential rotation, real server auth smoke, or production release approval exists yet.
- Minimal public health/readiness endpoints and authenticated smoke checks exist after PDR-M4, but no real server smoke evidence, log aggregation, metrics dashboards, or alerting exists yet.
- PDR-M6A adds the real-server acceptance evidence template and conservative release gate runner, but no real server evidence has been collected yet.
- PDR-M7 adds the opt-in provider live smoke harness, but no real provider evidence has been collected yet.

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

## Remaining Blockers

- Flyway remains non-default for runtime startup; test-only Flyway smoke exists.
- PostgreSQL baseline schema SQL has a Testcontainers smoke path, but this local environment may skip it when Docker is unavailable.
- Mapper PostgreSQL compatibility covers known upsert and DATEADD / FORMATDATETIME blockers; live PostgreSQL mapper execution remains to be expanded beyond smoke/static guards.
- Docker Compose deployment skeleton, `.env.example`, and smoke/backup/restore scripts exist; real server deployment smoke and real restore drill evidence are still missing.
- Basic Auth access control exists after PDR-M3, but real server auth smoke, HTTPS/reverse-proxy hardening, credential rotation, and secrets manager integration remain missing.
- Observability is minimal after PDR-M4: health/readiness exists, but metrics dashboards, log aggregation, alerting, real server smoke evidence, and restore drill evidence remain missing.
- Provider readiness is readonly after PDR-M5 and PDR-PF6 records safe no-call provider smoke evidence, but live provider connection proof, secrets manager integration, and real server provider smoke still do not exist yet.
- PDR-M6A release-gate framework exists, but the required real-server evidence template is not completed yet.
- PDR-M7 provider live smoke harness exists and PDR-PF6 records default-disabled/no-call evidence, but live checks remain opt-in and no real provider PASS evidence has been collected yet.
- Deployment packaging is skeletal only and not release-gated.
- Secrets contract exists as placeholders only, including admin credentials; no secrets manager integration exists.

## Next Packages

1. User-run real server acceptance evidence collection using `docs/PRODUCTION_ACCEPTANCE_EVIDENCE_TEMPLATE.md` and `scripts/prod-release-gate.sh`.
2. Controlled user-run provider live smoke with redacted evidence after server keys are configured and explicit external-call approval is given.
3. PDR-PF7 Push Recheck Quote-Unavailable Guard or PDR-M8 Secrets Manager / HTTPS / Reverse Proxy / Credential Rotation / Audit Hardening when explicitly scoped.

## Explicit Non-Scope

PDR-M7 does not deploy to a real server, expose sensitive actuator endpoints, add Prometheus/Grafana, commit real secrets, connect Telegram, send Telegram, dispatch Push, trigger Push Recheck, connect Binance private trading execution, call live providers in default tests or smoke, run restore automatically, change schema.sql, change mapper SQL, add order/execution, or add auto-trading semantics. It adds the opt-in provider live smoke harness, optional release-gate integration, env placeholders, static tests, and docs/status updates while preserving Production Deployment Readiness as `BLOCKED`.
