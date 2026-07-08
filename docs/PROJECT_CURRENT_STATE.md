# Trade Model V1 Current State

Contract: docs/PROJECT_DELIVERY_CONTRACT.md
Contract Version: v1.0
Current Phase: P0-0 Contract Lock + Baseline + Dead Code Candidate Report
Current Phase Status: DONE
Completion Effective State: derived by v1 state runtime
Existing Module Maturity: PARTIAL
Current Work Package: PDR-LIVE12 Access Logging Auth Audit Rate Limit Evidence Remediation
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


## PDR-PF2 Production Scheduler Policy

PDR-PF2 is DONE/effective on merged main. It defines production scheduler policy, requires explicit production scheduler classification, and keeps production deployment fail-closed unless each scheduler is explicitly approved.

Current production scheduler policy status:

1. `application-prod.yml` exposes production scheduler flags as explicit default-off settings.
2. `ProductionProfileSafetyGuard` rejects missing production scheduler policy and missing scheduler classifications.
3. `LOCKED_DOWN` mode requires all production schedulers to stay disabled.
4. `EXPLICIT_OPT_IN` mode requires scheduler-specific `PROD_ALLOWED_EXPLICIT_OPT_IN` classification before an effective scheduler can run.
5. Position Monitor scheduler remains default-off and production guard rejects enabling it in this package.
6. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommended remediation package after PDR-PF2: `PDR-PF3 PostgreSQL Migration Evidence`.

---

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


## PDR-PF3 PostgreSQL Migration Evidence

PDR-PF3 is DONE/effective on merged main by PR #1071. It records PostgreSQL migration evidence for current Flyway files without changing business runtime behavior.

Current PDR-PF3 evidence status:

1. Reviewed migration files: `V1__baseline_schema_tables.sql`, `V2__baseline_schema_indexes.sql`, and `V3__scheme_rule_config_defaults.sql`.
2. Static scan found no H2-only `AUTO_INCREMENT`, `MERGE INTO`, `ON DUPLICATE`, `DATEADD`, `FORMATDATETIME`, or `CLOB` usage inside Flyway migration SQL.
3. `schema.sql` remains H2 local/test bootstrap and still contains H2-specific syntax; it is not production Flyway migration SQL.
4. `PostgreSqlFlywayMigrationSmokeTest` is present and designed to verify empty PostgreSQL migration with Testcontainers.
5. Empty PostgreSQL migration result is `BLOCKED_TIMEOUT`: the PostgreSQL migration evidence run lasted approximately 1h27m before manual interruption and produced no completed trustworthy migration success log.
6. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation: first resolve Docker/Testcontainers availability or rerun PDR-PF3 in a controlled server-backed PostgreSQL test environment. Proceed to `PDR-PF4 Current-State Migration + Rollback Drill` only after empty migration evidence is PASS.

---

## PDR-PF4 Current-State Migration + Rollback Drill

PDR-PF4 is DONE/effective on merged main by PR #1072. It defines safe current-state migration and rollback rehearsal requirements without accessing production DB, running destructive DB operations, changing runtime behavior, or claiming production readiness.

Current PDR-PF4 status:

1. `docs/CURRENT_STATE_MIGRATION_ROLLBACK_DRILL.md` defines required preconditions, backup plan, restore drill plan, current-state migration rehearsal plan, rollback decision tree, evidence bundle, and safe staging/server-backed command templates.
2. Existing `scripts/prod-backup.sh` requires explicit database environment variables and has no hardcoded DB URL or secrets.
3. Existing `scripts/prod-restore.sh` requires explicit restore environment variables and refuses to run without `RESTORE_CONFIRM=I_UNDERSTAND_RESTORE_CAN_OVERWRITE_DATA`.
4. Existing `scripts/prod-release-gate.sh` does not run restore automatically and keeps release-gate status incomplete until restore/human evidence exists.
5. No production DB was accessed and no destructive DB operation was run by this package.
6. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation: collect PDR-PF4 evidence in a safe staging/server-backed PostgreSQL environment, or first resolve Docker/Testcontainers/PostgreSQL availability. After backup, restore, migration, rollback, and smoke evidence exists, continue to the next explicitly scoped remediation package.

---

## PDR-PF5 Secrets and Access Hardening

PDR-PF5 is DONE/effective on merged main by PR #1073. It defines production secrets/access hardening requirements and safe guard evidence without accessing real secrets, production servers, or production DB.

Current PDR-PF5 status:

1. `docs/SECRETS_AND_ACCESS_HARDENING.md` records existing secret-related guards, missing hardening evidence, required env vars, secrets manager / rotation plan, HTTPS / reverse proxy checklist, audit/access logging checklist, rate limiting checklist, actuator exposure policy, and prohibited secret handling.
2. Existing `ProductionProfileSafetyGuard` rejects missing/unsafe production datasource, admin, Binance, public bind, actuator, and scheduler policy settings.
3. AI provider secrets are required only when the matching provider is explicitly enabled.
4. Existing scripts avoid printing passwords and default live provider smoke to skipped/no external calls.
5. No real secrets were accessed, no production server was accessed, and no production DB was accessed by this package.
6. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation: implement or collect evidence for secrets manager integration, credential rotation, HTTPS/reverse-proxy hardening, access/audit logging, and rate limiting before any release-gate decision.

---

## PDR-PF6 Provider Live Smoke Evidence

PDR-PF6 is DONE/effective on merged main by PR #1074. It records provider live-smoke readiness evidence, safe default-disabled smoke behavior, provider result statuses, and redaction policy without accessing real secrets or making unapproved live external calls.

Current PDR-PF6 status:

1. `docs/PROVIDER_LIVE_SMOKE_EVIDENCE.md` records provider paths reviewed, safe no-call smoke output, result per provider, redaction policy, remaining blockers, and blocked production readiness.
2. `scripts/prod-provider-smoke.sh` defaults to `PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=false` and returns SKIPPED without provider network calls.
3. Binance public, OpenAI, Gemini, xAI/Grok, and external context provider evidence is recorded as skipped/disabled by default unless explicitly approved and configured in a safe environment.
4. No real secrets were accessed, no secret values were printed, no production server was accessed, and no production DB was accessed by this package.
5. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation: `PDR-PF7 Push Recheck Quote-Unavailable Guard`, or a separately scoped controlled-server provider evidence rerun if secrets manager / server env exists and the operator explicitly approves live external calls.


---

## PDR-PF7 Push Recheck Quote-Unavailable Guard

PDR-PF7 is DONE/effective on merged main by PR #1075. It adds focused Push Recheck quote-unavailable guard tests and records evidence that missing `currentPrice` plus unavailable quote data fails closed as review-only/non-executable.

Current PDR-PF7 status:

1. `PushRecheckServiceImplTest` covers `Optional.empty`, null `lastPrice`, quote client exception, missing snapshot symbol, and provided valid current price behavior.
2. Missing quote paths write `fail_reason_json.code = QUOTE_UNAVAILABLE` and `RecheckStatusEnum.INVALIDATED`.
3. Missing snapshot symbol writes `fail_reason_json.code = PRICE_REQUIRED` and `RecheckStatusEnum.INVALIDATED`.
4. Fail-closed paths update push status to `RECHECK_INVALIDATED` and keep `RecheckResult` review-only, non-executable, not order execution, not auto-trading, not user-position creation, and not position mutation.
5. No production code change was required; this package is test/docs/status-source guard evidence.
6. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after PF7: run PDR-PF8 Production Release Gate Closure to aggregate PF1-PF7 evidence and record the final release-gate decision.

---

## PDR-PF8 Production Release Gate Closure

PDR-PF8 is DONE/effective on merged main by PR #1076. It aggregates PF1-PF7 production-readiness evidence and records the final production release-gate decision without changing runtime behavior.

Current PDR-PF8 status:

1. `docs/PRODUCTION_RELEASE_GATE_DECISION.md` records the gate decision as `BLOCKED` and production deployment decision as `DO NOT DEPLOY`.
2. PDR-PF3 PostgreSQL empty migration evidence remains `BLOCKED_TIMEOUT` and not proven.
3. PDR-PF4 current-state migration plus rollback drill is documented but not executed against a production-like database.
4. PDR-PF5 secrets/access hardening is documented but lacks a complete secrets manager, rotation, HTTPS/reverse-proxy, audit logging, and rate limiting evidence bundle.
5. PDR-PF6 provider live smoke was `SKIPPED_DISABLED_BY_DEFAULT` for that release-gate closure; later LIVE8 adds controlled Binance public `PASS`, while AI/external provider proof remains missing.
6. PDR-PF7 quote-unavailable guard is PASS safety evidence, but it is not enough to unlock production deployment.
7. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after PF8: `PDR-PF9 PostgreSQL Migration Evidence Recovery`.

---

## PDR-PF9 PostgreSQL Migration Evidence Recovery

PDR-PF9 is DONE/effective on merged main by PR #1077. It reran only a bounded targeted PostgreSQL/Flyway smoke path and recorded the real local evidence status.

Current PDR-PF9 status:

1. Existing targeted test exists: `PostgreSqlFlywayMigrationSmokeTest`.
2. Existing skip condition is present and works when Docker/Testcontainers is unavailable.
3. Local `timeout` command was unavailable, so the targeted Maven command was run through a Python 600-second timeout wrapper.
4. Local `docker` command was unavailable and Testcontainers could not find `/var/run/docker.sock`.
5. Bounded command completed in 1.61 seconds with JUnit skip: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 1`.
6. Result is `BLOCKED_ENV_UNAVAILABLE`, not PASS, because no PostgreSQL container ran and no Flyway V1/V2/V3 success log exists.
7. No production DB was accessed and no destructive DB operation was run.
8. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after PF9: `PDR-PF10 PostgreSQL Environment Provisioning Evidence`.

---

## PDR-PF10 PostgreSQL Environment Provisioning Evidence

PDR-PF10 is DONE/effective on merged main by PR #1078. It checks whether this local/server environment can support PostgreSQL migration smoke evidence before any migration smoke is attempted.

Current PDR-PF10 status:

1. `docker version` and `docker info` return `zsh:1: command not found: docker`.
2. `command -v docker` returns no path.
3. `/var/run/docker.sock` is unavailable.
4. `~/.docker/run/docker.sock` is unavailable.
5. Existing PostgreSQL/Flyway smoke tests are present, including `PostgreSqlFlywayMigrationSmokeTest`.
6. Migration smoke was not run because Docker availability was not confirmed.
7. Docker availability result is `DOCKER_MISSING`.
8. Migration smoke result is `SKIPPED_ENV_UNAVAILABLE`.
9. No Flyway V1/V2/V3 PostgreSQL success evidence exists.
10. No production DB was accessed and no destructive DB operation was run.
11. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after PF10: `PDR-PF11 Controlled PostgreSQL Migration Smoke Evidence`.

---

## PDR-PF11 Controlled PostgreSQL Migration Smoke Evidence

PDR-PF11 is DONE/effective on merged main by PR #1079. It attempts to obtain a trustworthy bounded Flyway V1/V2/V3 success log only if Docker/Testcontainers or a controlled non-production PostgreSQL environment is available.

PDR-PF11 status:

1. `command -v docker` returns no path.
2. `docker version || true` and `docker info || true` return `zsh:1: command not found: docker`.
3. `/var/run/docker.sock` is unavailable.
4. `~/.docker/run/docker.sock` is unavailable.
5. Existing PostgreSQL/Flyway smoke tests are present, including `PostgreSqlFlywayMigrationSmokeTest`.
6. Bounded Maven/Testcontainers migration smoke was not run because Docker/Testcontainers availability was not confirmed.
7. Docker availability result is `DOCKER_MISSING`.
8. Migration smoke result is `BLOCKED_ENV_UNAVAILABLE`.
9. Flyway V1/V2/V3 PostgreSQL success is not proven.
10. No production DB was accessed and no destructive DB operation was run.
11. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after PF11: `PDR-LIVE1 Controlled Live Dependency Acceptance`, focused on controlled DB/provider/app-prod/scheduler/Push Recheck evidence without production deployment approval.

---

## PDR-LIVE1 Controlled Live Dependency Acceptance

PDR-LIVE1 is DONE/effective on merged main by PR #1080. It prepares or records real dependency acceptance evidence in a controlled staging, pre-prod, or local-controlled environment. It is not production deployment, not public release, and not auto-trading.

PDR-LIVE1 status:

1. Controlled PostgreSQL DB result is `SKIPPED_MISSING_CONTROLLED_DB` because no disposable non-production PostgreSQL URL was present in environment.
2. Binance public market data smoke result is `SKIPPED_DISABLED` because live external calls were not explicitly enabled.
3. AI provider results are `SKIPPED_MISSING_SECRET` for OpenAI, Gemini, and xAI because keys were not present.
4. External context/news/macro provider result is `SKIPPED_MISSING_SECRET` because keys/configuration were not present.
5. `ProductionProfileSafetyGuardTest`, `PositionMonitorSchedulerTest`, and `PushRecheckServiceImplTest` passed in a bounded run.
6. application-prod safety remains fail-closed.
7. Production scheduler policy remains fail-closed and Position Monitor scheduler remains default-off.
8. Push Recheck quote-unavailable behavior remains fail-closed/review-only.
9. No production DB was accessed, no destructive DB operation was run, and no secrets were printed or committed.
10. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after LIVE1: `PDR-LIVE2 Controlled PostgreSQL Evidence Setup`. The next package must not be deployment.

---

## PDR-LIVE2 Controlled PostgreSQL Evidence Setup

PDR-LIVE2 is DONE/effective on merged main by PR #1081. It prepares the next concrete path to obtain PostgreSQL Flyway migration PASS evidence in a controlled environment. It is not production deployment and does not run a migration in this package.

PDR-LIVE2 status:

1. Existing migration files V1/V2/V3 are present and reviewed.
2. Existing `PostgreSqlFlywayMigrationSmokeTest` is present and remains the Docker/Testcontainers path.
3. Docker result is `DOCKER_MISSING`.
4. Controlled PostgreSQL DB env result is `SKIPPED_MISSING_CONTROLLED_DB`.
5. No external controlled-DB Flyway runner exists yet.
6. `scripts/controlled-postgresql-evidence-plan.sh` is added as a no-op dry-run setup helper only; it never connects to DB or runs Flyway.
7. No production DB was accessed and no destructive DB operation was run.
8. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after LIVE2: `PDR-LIVE3 Controlled PostgreSQL Flyway Smoke Runner`. The next package must not be deployment.

---

## PDR-LIVE3 Controlled PostgreSQL Flyway Smoke Runner

PDR-LIVE3 is DONE/effective on merged main by PR #1082. It adds a guarded external controlled-DB runner and a test-only external smoke path. It is not production deployment and does not run a migration unless explicit disposable non-production DB env and run confirmations are supplied.

Current PDR-LIVE3 status:

1. `scripts/controlled-postgresql-flyway-smoke.sh` is added as a guarded bounded runner.
2. `ControlledPostgreSqlFlywaySmokeTest` is added as test-only external PostgreSQL Flyway smoke.
3. Missing controlled DB env produces `SKIPPED_MISSING_CONTROLLED_DB` and no database access.
4. Runner requires explicit non-production confirmation and explicit Flyway run confirmation.
5. Runner refuses production-like JDBC URL indicators and does not print DB URL, username, password, host, or database name.
6. No production DB was accessed and no destructive DB operation was run by the runner package.
7. Production readiness remains BLOCKED and production deployment cannot proceed.

## PDR-LIVE4 Controlled PostgreSQL Flyway Evidence Run

PDR-LIVE4 is DONE/effective on merged main by PR #1083. It records operator-provided PASS evidence from a disposable local Docker PostgreSQL run. It is not production deployment and does not access production DB.

Current PDR-LIVE4 status:

1. The operator ran `scripts/controlled-postgresql-flyway-smoke.sh` with explicit disposable non-production and schema-write confirmations.
2. The controlled PostgreSQL target was `localhost:55432` / `trade_model_smoke`.
3. PostgreSQL version was `16.14`.
4. Flyway validated 3 migrations.
5. Applied migrations: V1 baseline schema tables, V2 baseline schema indexes, and V3 scheme rule config defaults.
6. Final schema version was `v3`.
7. `CONTROLLED_POSTGRESQL_FLYWAY_RESULT: PASS`.
8. No production DB was accessed, no destructive operation outside the disposable controlled DB was run, and no secrets were printed.
9. Production readiness remains BLOCKED and production deployment cannot proceed.

## PDR-LIVE5 Controlled Current-State Migration + Restore Drill Evidence

PDR-LIVE5 is DONE/effective on merged main by PR #1084. It adds a safe no-op/dry-run default helper and records the actual local skipped evidence status. It is not production deployment and does not access production DB.

Current PDR-LIVE5 status:

1. `scripts/controlled-current-state-migration-restore-drill.sh` is added as a guarded helper with no-op default behavior.
2. Controlled source DB env is missing in this execution environment.
3. Controlled recovery DB env is missing in this execution environment.
4. Local PostgreSQL client tools `pg_dump`, `pg_restore`, and `psql` are missing.
5. Backup result is `SKIPPED_MISSING_CONTROLLED_DB`.
6. Restore result is `SKIPPED_MISSING_RECOVERY_DB`.
7. Current-state migration rehearsal result is `SKIPPED`.
8. No production DB was accessed, no destructive operation outside a disposable controlled DB was run, and no secrets were printed.
9. Production readiness remains BLOCKED and production deployment cannot proceed.

## PDR-LIVE6 Controlled Backup Restore Evidence Run

PDR-LIVE6 is DONE/effective on merged main by PR #1085. It records operator-provided disposable local `pg_dump` / `pg_restore` evidence. It is not production deployment and does not access production DB.

Current PDR-LIVE6 status:

1. Source DB was disposable local PostgreSQL at `localhost:55432` / `trade_model_smoke`.
2. Restore DB was disposable local PostgreSQL at `localhost:55433` / `trade_model_restore`.
3. `pg_dump` custom-format backup completed with result `PASS`.
4. `pg_restore` completed with `transaction_timeout` compatibility warning and result `PASS_WITH_WARNING`.
5. Restored `tm_*` table count is `27`.
6. Restored successful Flyway migration count is `3`.
7. Restore validation result is `PASS`.
8. No production DB was accessed, no destructive operation outside disposable controlled DB was run, and no secrets were printed or committed.
9. Production readiness remains BLOCKED and production deployment cannot proceed.

## PDR-LIVE7 PostgreSQL 16-aligned Clean Restore Evidence

PDR-LIVE7 is DONE/effective on merged main by PR #1086. It records operator-provided PostgreSQL 16 container-native backup/restore evidence. It is not production deployment and does not access production DB.

Current PDR-LIVE7 status:

1. Source container was `trade-model-pg-smoke`; restore container was `trade-model-pg-restore`.
2. Source DB was `trade_model_smoke`; restore DB was `trade_model_restore`.
3. Backup used `pg_dump` from `postgres:16-alpine` and completed with result `PASS`.
4. Restore used `pg_restore` from `postgres:16-alpine` and completed with result `PASS_CLEAN`.
5. Restored `tm_*` table count is `27`.
6. Restored successful Flyway migration count is `3`.
7. Restore validation result is `PASS`.
8. No production DB was accessed, no destructive operation outside disposable controlled DB was run, and no secrets were printed or committed.
9. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after LIVE7: `PDR-LIVE8 Controlled Provider Live Smoke Evidence Run`. The next package must not be deployment.

## PDR-LIVE8 Controlled Provider Live Smoke Evidence Run

PDR-LIVE8 is DONE/effective on merged main by PR #1087. It records controlled provider live-smoke evidence after the PostgreSQL clean restore evidence packages. It is not production deployment, does not place orders, does not send external Push, and does not print or commit secrets.

Current PDR-LIVE8 status:

1. `scripts/prod-provider-smoke.sh` was inspected and remains opt-in for live external calls.
2. Default no-call provider smoke returned `PROVIDER_LIVE_SMOKE: SKIPPED` and skipped Binance/OpenAI/Gemini/xAI without network calls.
3. Controlled Binance public smoke was run with explicit opt-in flags and returned `BINANCE_PUBLIC_SMOKE: PASS` against the public futures time endpoint.
4. OpenAI, Gemini, and xAI were not called because keys were missing and provider flags remained disabled; results are recorded as `SKIPPED_MISSING_SECRET`.
5. External context/news/macro provider smoke is recorded as `SKIPPED_MISSING_SECRET` because keys/configuration were missing and no live external-context call is implemented by the harness.
6. No secret values were printed, no `.env` was committed, no orders were placed, and no external Push was sent.
7. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after LIVE8: `PDR-LIVE9 Controlled AI Provider Smoke Evidence Run`. The next package must not be deployment.

## PDR-LIVE9 Controlled AI Provider Smoke Evidence Run

PDR-LIVE9 is DONE/effective on merged main by PR #1088. It records controlled AI provider smoke evidence for OpenAI, Gemini, and xAI/Grok. It is not production deployment, does not place orders, does not send external Push, and does not print or commit secrets.

Current PDR-LIVE9 status:

1. `scripts/prod-provider-smoke.sh`, AI provider config, `.env.example`, and provider smoke docs were inspected.
2. AI key presence was checked as boolean-only redacted status: OpenAI, Gemini, and xAI keys were missing.
3. Controlled AI provider smoke was run with AI provider flags enabled and Binance disabled through a bounded 300-second wrapper.
4. OpenAI returned `NOT_CONFIGURED - OPENAI_API_KEY missing`, recorded as `SKIPPED_MISSING_SECRET`.
5. Gemini returned `NOT_CONFIGURED - GEMINI_API_KEY missing`, recorded as `SKIPPED_MISSING_SECRET`.
6. xAI/Grok returned `NOT_CONFIGURED - XAI_API_KEY missing`, recorded as `SKIPPED_MISSING_SECRET`.
7. No AI provider endpoint was called, no secret values were printed, no `.env` was committed, no orders were placed, and no external Push was sent.
8. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after LIVE9: `PDR-LIVE10 Secrets HTTPS Access Logging Rate Limit Evidence`. The next package must not be deployment.

## PDR-LIVE10 Secrets HTTPS Access Logging Rate Limit Evidence

PDR-LIVE10 is DONE/effective on merged main by PR #1089. It records production security evidence status for secrets, HTTPS/reverse proxy, access logging, auth audit logging, rate limiting, actuator exposure, and production guard behavior. It is not production deployment and does not access production server, production DB, or real secrets.

Current PDR-LIVE10 status:

1. `application-prod.yml`, `.env.example`, `ProductionProfileSafetyGuard`, `SecurityConfig`, relevant security/actuator tests, and existing hardening docs were inspected.
2. Production profile guard evidence is `GUARD_PASS` for datasource/admin/Binance/AI provider fail-closed settings, public bind, actuator exposure, and scheduler policy.
3. Auth access-control evidence is `GUARD_PASS`; dashboard/review/API/write/recheck routes require Basic Auth and no executable trading route surface is introduced.
4. Actuator exposure evidence is `GUARD_PASS`; health/liveness/readiness are minimal and sensitive actuator endpoints are not exposed.
5. Secret handling has repository hygiene `GUARD_PASS` because `.env.example` is placeholder-only and no real `.env` is tracked, but real secrets manager injection is `MISSING_EVIDENCE`.
6. HTTPS/reverse proxy is `DOCUMENTED_NOT_EVIDENCED`.
7. Access logging, auth audit logging, and rate limiting are `MISSING_EVIDENCE`.
8. No production server was accessed, no production DB was accessed, no real secrets were printed or committed, and production readiness remains BLOCKED.

## PDR-LIVE11 Release Evidence Bundle + Remaining Blockers Closure

PDR-LIVE11 is DONE/effective on merged main by PR #1090. It aggregates controlled PostgreSQL, provider, AI-provider, and security/access evidence into a single release-gate status report. It is not production deployment and does not access production server, production DB, or real secrets.

Current PDR-LIVE11 status:

1. `docs/RELEASE_EVIDENCE_BUNDLE_CURRENT_STATUS.md` records the current release evidence table and blocker closure status.
2. PostgreSQL controlled evidence is materially improved: empty Flyway V1/V2/V3 is `PASS`, PostgreSQL 16 backup is `PASS`, clean restore is `PASS_CLEAN`, restored `tm_*` table count is `27`, and restored Flyway success count is `3`.
3. Provider evidence is partial: Binance public smoke is `PASS`, while OpenAI/Gemini/xAI and external context providers remain `SKIPPED_MISSING_SECRET`.
4. Security/access evidence at LIVE11 was partial: production profile guard, auth access control, actuator exposure, and repository secret hygiene were `GUARD_PASS`; secrets manager and credential rotation were `MISSING_EVIDENCE`; HTTPS/reverse proxy was `DOCUMENTED_NOT_EVIDENCED`; access logging, auth audit logging, and rate limiting were still missing before LIVE12.
5. Production readiness remains `BLOCKED`; production deployment decision remains `DO NOT DEPLOY`.
6. No production server was accessed, no production DB was accessed, no secrets were printed or committed, and no runtime trading behavior changed.

## PDR-LIVE12 Access Logging Auth Audit Rate Limit Evidence Remediation

PDR-LIVE12 is the current controlled security evidence and remediation package. It adds application-level access logging, authentication failure audit logging, sensitive-data redaction, and rate-limit guard evidence. It is not production deployment and does not access production server, production DB, or real secrets.

Current PDR-LIVE12 status:

1. `docs/ACCESS_AUDIT_RATE_LIMIT_EVIDENCE_RUN.md` records access logging, auth audit logging, rate limiting, and sensitive-data redaction evidence.
2. `AccessLoggingFilter` emits sanitized `ACCESS_LOG` records without request bodies, query strings, Authorization headers, cookies, API keys, passwords, tokens, datasource URLs, or provider secrets.
3. `AuthAuditAuthenticationEntryPoint` emits `AUTH_AUDIT outcome=FAILURE` records for authentication challenges without credential values.
4. `RequestRateLimitFilter` returns HTTP 429 with `Retry-After` after configured per-client/path thresholds are exceeded.
5. `ProductionProfileSafetyGuard` rejects prod config when rate limiting is disabled or thresholds are invalid.
6. Targeted security tests prove access log presence, auth audit presence, sensitive header/query redaction, rate-limit blocking, and production rate-limit fail-closed validation.
7. Access logging, auth audit logging, rate limiting, and sensitive-data redaction move to `GUARD_PASS` for controlled application-level evidence.
8. Production readiness remains `BLOCKED`; production deployment decision remains `DO NOT DEPLOY`.

Next recommendation after LIVE12: `PDR-LIVE13 HTTPS / Reverse Proxy Evidence`, or a controlled secrets-manager / credential-rotation evidence package. The next package must not be deployment.

---

## Current Allowed Work

Only the following work is allowed during and after PDR-LIVE12 Access Logging Auth Audit Rate Limit Evidence Remediation:

1. Controlled access logging, auth audit logging, sensitive-data redaction, and rate-limit guard evidence.
2. Safe tests and production guard validation for the access/audit/rate-limit security package.
3. Status-source and production-readiness evidence documentation updates.
4. Keeping production readiness BLOCKED and production deployment blocked.
5. The next explicitly scoped remediation package after LIVE12 evidence is reviewed.
6. Production-readiness remediation only when explicitly scoped.

PDR-M7 is the historical latest production-readiness package, not the only currently allowed work. Production deployment remains BLOCKED until a separate production release gate clears every required gate with PASS evidence.

---

## Current Forbidden Work

The following work remains blocked during and after PDR-LIVE12 Access Logging Auth Audit Rate Limit Evidence Remediation:

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
11. Treating PF8 release-gate closure as deployment approval.
12. Treating LIVE4 Flyway PASS, LIVE6 backup/restore warning evidence, LIVE7 clean local restore evidence, LIVE8 Binance public PASS evidence, LIVE9 AI skipped evidence, LIVE10 guard-pass evidence, LIVE11 release evidence bundle status, or LIVE12 access/audit/rate-limit guard evidence as full production deployment approval; they are not release-gate approval.

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
- Minimal public health/readiness endpoints and authenticated smoke checks exist after PDR-M4, readonly provider readiness checks exist after PDR-M5, the PDR-M6A acceptance evidence framework exists, and the PDR-M7 opt-in provider live smoke harness exists, but no completed real-server evidence, metrics dashboards, log aggregation, alerting, full provider connection proof, or production release approval exists yet.
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
- Docker Compose deployment skeleton, `.env.example`, smoke/backup/restore scripts, the PDR-M6A evidence template, the conservative release gate runner, and the opt-in provider live smoke harness exist, but real server deployment smoke, AI/external provider smoke evidence, and production-like restore drill evidence are still missing.
- Auth/access control baseline exists as single-operator Basic Auth, but real server auth smoke, HTTPS/reverse-proxy hardening, credential rotation, audit logging, rate limiting, and secrets manager integration remain missing.
- Observability is minimal: health/readiness exists, but metrics dashboards, log aggregation, alerting, real server smoke evidence, and restore drill evidence remain missing.
- Deployment packaging is skeletal only and not release-gated.
- Secrets contract exists as placeholders only, including admin credentials; secrets manager integration is missing.

Next production-readiness packages:

1. PDR-LIVE11 HTTPS / Access Logging / Rate Limit Remediation, or another explicitly scoped provider/secrets/access evidence package.
2. Secrets/access/HTTPS evidence only after explicitly scoped controlled environment evidence is available.
3. Production release-gate status closure only after completed redacted evidence and explicit approval.

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
