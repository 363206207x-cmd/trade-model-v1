# RINE LOGIC v4.1 Non-CoinGlass Blocker Closure

Date: 2026-08-23
Start Head: `c80af6bf20c1135e174ef636f28abd5f8e7f97af`
Validation implementation Head: `8c5f6f11`
Final evidence Head: the commit containing this document, reported in the PR and task result
PR: #1195, open, Draft, unmerged
Release scope: `PRIVATE_SINGLE_USER_WEB`

> **2026-08-23 safe-default superseding correction:** production now keeps
> Kraken as the required release OHLCV source while defaulting both
> `TRADE_MODEL_KRAKEN_OHLCV_ENABLED` and
> `TRADE_MODEL_KRAKEN_OHLCV_EXTERNAL_CALLS_ENABLED` to `false`. A deployment
> must explicitly inject both values as `true`; otherwise preflight and the
> production scheduler gate fail closed. Existing local-live Kraken evidence
> remains valid, but it is not staging evidence.

## Decision

The two implementation defects proven during Phase A are closed: the isolated
Three-AI smoke no longer inherits the target PostgreSQL Flyway override, and it
performs formal exact-model readiness verification before orchestration.
Kraken is now the required release OHLCV source and Binance is disabled due to
HTTP 451. No Binance bypass or replacement provider was added.

The package remains blocked for release. Gemini is rejected by the provider's
account/location/region policy and no authorized remote P3H staging identity is
configured. CoinGlass remains separately deferred because its private key is
missing and was not called.

```text
NON_COINGLASS_READINESS=BLOCKED
PRODUCTION_READINESS=BLOCKED_MULTIPLE
CURRENT_PHASE_DONE=NO
MERGE=NO
DEPLOYMENT_ALLOWED=NO
```

## Phase A Findings

### Gemini

One controlled live request was reproduced before any production change.

| Evidence | Sanitized result |
|---|---|
| Endpoint path | `/v1/interactions` |
| Configured model | `gemini-3.5-flash` |
| Effective model | `models/gemini-3.5-flash` |
| Request content type | `application/json` |
| HTTP / response MIME | `400` / `application/json` |
| Authentication | accepted; not 401/403 |
| Request ID | absent |
| Error category | account/location/region restriction |

Differential probes against v1/v1beta, full/minimal bodies, and generation
configuration variants produced the same sanitized provider error fingerprint.
The response named location/region and did not identify the model, body,
response format, authentication, or permissions as the cause. VPN, proxy,
provider substitution, fabricated Gemini output, and fallback impersonation
are forbidden. Therefore no Gemini production code was changed.

Every formal model lock remains `gemini-3.5-flash`; contract tests cover the
normalized effective model, request body, response parser, fail-closed paths,
and smoke defaults.

### Three-AI database and orchestration

The original controlled smoke failed during Spring startup before any Provider
call. The exception chain was Flyway/H2 SQL initialization, caused by the
private target environment's PostgreSQL Flyway override entering an isolated
H2 harness that owns `schema.sql`. A second omission left all providers
`NOT_CONFIGURED`: the harness never invoked the formal exact-model readiness
verification used by the runtime guard.

The isolated harness now pins `SPRING_FLYWAY_ENABLED=false` and executes
`AiProviderReadinessService.reverify` for OpenAI, Gemini, and xAI before the
orchestrator. This does not disable Flyway in release or production profiles.
The bounded transport permits one readiness call and one role call per
provider and still refuses any third call.

Post-fix controlled live result:

| Provider | Result | Calls |
|---|---:|---:|
| OpenAI | 2xx, parsed | 2 |
| Gemini | fail closed at readiness, region restriction | 1 |
| xAI | 2xx, parsed | 2 |

The orchestrator completed with two successful role results and one explicit
failed role, preserving deterministic final role order and partial fallback.
This proves the startup/orchestration repair. It does not prove complete
three-role lineage because real Gemini output is unavailable.

### Kraken and Binance

The release profile keeps Kraken as the primary and required OHLCV source, but
defaults Kraken enablement and external calls to disabled. A deployment must
explicitly inject both Kraken flags as true. Binance and provider fallback stay
disabled by default. Target preflight and production safety validation reject
missing Kraken opt-in and any enabled Binance path. A Kraken failure returns
fail closed and performs zero Binance calls. Persisted Kraken close time remains
the single global Home data timestamp, and changing selected assets does not
alter Provider readiness.

The historical Binance 451 capability tests remain normal regression evidence;
the release policy does not attempt to bypass the regional restriction.

## Scope and ownership corrections

- `HighValueAlertMessageService.currentRecheckId = null` is an authorized
  Message/Recheck identity correction. Telegram delivery, binding, session,
  webhook, and O10 were not changed in this package.
- Release, rollback, and incident owner: repository Owner `363206207x-cmd`.
- Current Go/No-Go: `NO_GO_PENDING_TECHNICAL_GATES_AND_COINGLASS`.
- Recheck: `DEFERRED_UNTIL_LEGAL_SOURCE` when no natural in-app
  `PUSH_SNAPSHOT` Message exists. No raw push ID is fabricated.
- Legacy routes are retained, authenticated, and absent from formal navigation.
- Full close remains supported; partial-close writes remain outside scope.

## Phase B authorization

All 13 required remote P3H inputs were absent: target confirmation, server and
secret attestations, SSH host/port/user/key/fingerprint, staging hostname, TLS
mode/CA, secret backend, and runtime secret mount. No target action was run.
Local H2, disposable PostgreSQL, or a local P3H template cannot replace this
evidence.

| Target gate | Status |
|---|---|
| Staging user paths and full close | `NOT_VERIFIED_NO_AUTHORIZED_STAGING` |
| Recheck real path | `NOT_VERIFIED_NO_LEGAL_SOURCE` |
| PostgreSQL staging upgrade / least privilege | `NOT_VERIFIED_NO_AUTHORIZED_STAGING` |
| Backup / restore / rollback | `NOT_VERIFIED_NO_AUTHORIZED_STAGING` |
| Server / HTTPS / session / restart | `NOT_VERIFIED_NO_AUTHORIZED_SERVER` |
| Secret injection / rotation | `NOT_VERIFIED_NO_AUTHORIZED_STAGING` |
| Scheduler runtime / observability | `NOT_VERIFIED_NO_AUTHORIZED_STAGING` |

## Validation

- Java 17 compile: PASS.
- Directed provider/orchestrator/Home contract tests: PASS.
- Frontend state matrix: PASS.
- Header/status timestamp matrix: PASS in UTC and Asia/Shanghai.
- Latest safe-default local Maven: 4,793 tests, 0 failures, 0 errors, 14 controlled skips.
- Product Source Gate: PASS.
- Workflow Contract: PASS on the clean implementation Head.
- JavaScript syntax: PASS.
- `git diff --check`: PASS.
- No production AI role semantics, schema, Telegram, Figma, Mobile, or
  automatic-trading capability changed.

## Final flags

```text
GATE_0_SCOPE=PASS_WITH_AUTHORIZED_MESSAGE_IDENTITY_FIX
TELEGRAM_DELIVERY_CHANGED=NO
OPENAI_LIVE_CALL=PASS
GEMINI_LIVE_CALL=BLOCKED_ACCOUNT_OR_REGION
GEMINI_EFFECTIVE_MODEL=models/gemini-3.5-flash
GEMINI_MODEL_LOCK_CONSISTENCY=PASS
XAI_LIVE_CALL=PASS
THREE_AI_PROVIDER_CALLS=PASS_PARTIAL_REAL_PROVIDERS
THREE_AI_NON_COINGLASS_LINEAGE=BLOCKED_GEMINI_ACCOUNT_OR_REGION
THREE_AI_COMPLETE_RELEASE_CHAIN=BLOCKED_BY_GEMINI_AND_COINGLASS
KRAKEN_RELEASE_SOURCE=PASS
KRAKEN_PROD_DEFAULT=DISABLED
KRAKEN_RELEASE_REQUIREMENT=EXPLICIT_DEPLOYMENT_INJECTION
KRAKEN_LOCAL_LIVE_EVIDENCE=PASS_EXISTING_EVIDENCE
KRAKEN_STAGING_RUNTIME=NOT_VERIFIED
BINANCE_RELEASE_POLICY=DISABLED_DUE_451
BINANCE_RELEASE_BLOCKER=CLOSED_BY_OWNER_POLICY
NORMAL_RUNTIME_ACCEPTANCE=PASS_LOCAL_H2
UI_REVIEW_FIXTURE_ISOLATION=PASS
STAGING_AUTHORIZATION=NOT_VERIFIED_MISSING_CONFIGURATION
POSITION_FULL_CLOSE_E2E=NOT_VERIFIED_NO_AUTHORIZED_STAGING
RECHECK_RELEASE_POLICY=DEFERRED_UNTIL_LEGAL_SOURCE
RECHECK_REAL_PATH=NOT_VERIFIED_NO_LEGAL_SOURCE
POSTGRESQL_MIGRATION=PASS_LOCAL_DISPOSABLE_ONLY
POSTGRESQL_UPGRADE=NOT_VERIFIED_NO_AUTHORIZED_STAGING
POSTGRESQL_LEAST_PRIVILEGE=NOT_VERIFIED_NO_AUTHORIZED_STAGING
DATABASE_BACKUP=NOT_VERIFIED_NO_AUTHORIZED_STAGING
DATABASE_RESTORE=NOT_VERIFIED_NO_AUTHORIZED_STAGING
ROLLBACK_PROCEDURE=NOT_VERIFIED_NO_AUTHORIZED_STAGING
REAL_SERVER_SMOKE=NOT_VERIFIED_NO_AUTHORIZED_SERVER
HTTPS_PROXY_SESSION=NOT_VERIFIED_NO_AUTHORIZED_SERVER
SECRET_INJECTION=NOT_VERIFIED_NO_AUTHORIZED_STAGING
SECRET_REDACTION=PASS_LOCAL
SECRET_ROTATION_DRILL=NOT_VERIFIED_NO_AUTHORIZED_STAGING
SCHEDULER_RUNTIME=NOT_VERIFIED_NO_AUTHORIZED_STAGING
OBSERVABILITY=NOT_VERIFIED_NO_AUTHORIZED_STAGING
LEGACY_ROUTE_POLICY=PASS_RETAINED_AUTHENTICATED_UNLINKED
COINGLASS_LIVE_CALL=NOT_EXECUTED_MISSING_PRIVATE_KEY
COINGLASS_AI_RUN_CONSUMPTION=NOT_VERIFIED
NON_COINGLASS_READINESS=BLOCKED
PRODUCTION_READINESS_AUDIT_DONE=YES
PRODUCTION_READINESS=BLOCKED_MULTIPLE
CURRENT_PHASE_DONE=NO
MERGE=NO
DEPLOYMENT_ALLOWED=NO
```

Evidence index:
`docs/evidence/non_coinglass_production_final_gate/README.md`.
