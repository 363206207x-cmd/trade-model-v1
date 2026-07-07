# Secrets And Access Hardening

Package: PDR-PF5 Secrets and Access Hardening
Date: 2026-07-07
Branch: `codex/pdr-pf5-secrets-access-hardening`
Current main commit reviewed: `a3cf5a1bd8dbedfe1896419b82a8c6a9f458a537`
Production readiness: BLOCKED
Production deployment: cannot proceed

## Scope

This package defines and validates the production secrets/access hardening requirements that still block production deployment. It is a planning and safe guard-evidence package only.

No real secrets were accessed. No production server was accessed. No production DB was accessed. No destructive operation was run. No runtime trading behavior was changed.

## Existing Secret-Related Guards

| Area | Existing guard / evidence | Status |
|---|---|---|
| Prod datasource URL | `application-prod.yml` reads `PROD_DATASOURCE_URL`; `ProductionProfileSafetyGuard` rejects missing URL and H2 memory URL. | Guard exists |
| Prod datasource username | `application-prod.yml` reads `PROD_DATASOURCE_USERNAME`; guard rejects blank username. | Guard exists |
| Prod datasource password | `application-prod.yml` reads `PROD_DATASOURCE_PASSWORD`; guard rejects blank password. | Guard exists |
| H2 console | `application-prod.yml` disables H2 console; guard rejects enabled H2 console. | Guard exists |
| Position provider | `POSITION_PROVIDER_TYPE` defaults to `BINANCE` in prod; guard rejects missing, `SIMULATED`, and unsupported provider values. | Guard exists |
| Binance key/secret | `BINANCE_API_KEY` and `BINANCE_API_SECRET` are required when production position provider is `BINANCE`; guard rejects missing values. | Guard exists |
| AI provider keys | OpenAI/Gemini/xAI keys are required only when the matching AI provider is explicitly enabled. Disabled providers may omit keys. | Guard exists |
| Admin username | `APP_ADMIN_USERNAME` feeds the Basic Auth operator account; guard rejects blank username. | Guard exists |
| Admin password | `APP_ADMIN_PASSWORD` feeds the Basic Auth operator account; guard rejects blank and unsafe defaults including `password`, `admin`, `change-me`, `changeme`, `123456`, and `dev-local-password`. | Guard exists |
| Public bind | `server.address` public bind requires `trade-model.production.allow-public-bind=true`; default prod binding is localhost. | Guard exists |
| Actuator exposure | Only `health` web exposure is allowed; guard rejects wildcard and sensitive endpoint exposure. Health details/components are hidden. | Guard exists |
| Scheduler policy | Production scheduler policy and per-scheduler classification are required; unsafe scheduler opt-in fails closed. | Guard exists |
| Smoke scripts | `prod-smoke.sh` and `prod-release-gate.sh` require auth env vars and intentionally do not print passwords. | Guard exists |
| Provider live smoke | `prod-provider-smoke.sh` defaults to no external calls and prints status summaries, not keys. | Guard exists |
| Restore script | `prod-restore.sh` refuses to run without explicit restore env vars and `RESTORE_CONFIRM=I_UNDERSTAND_RESTORE_CAN_OVERWRITE_DATA`. | Guard exists |

## Missing Hardening Evidence

The following remain blockers before production deployment can proceed:

1. No real secrets manager integration is implemented or evidenced.
2. No credential rotation runbook or rotation drill evidence exists for admin, datasource, Binance, AI, external-context, or future Telegram credentials.
3. No real server evidence proves env injection without committing `.env` or exposing values in logs.
4. No HTTPS/reverse proxy configuration is implemented or evidenced.
5. No HTTP-to-HTTPS redirect / HSTS / TLS certificate renewal evidence exists.
6. No real server auth smoke through the intended HTTPS entrypoint exists.
7. No access log / auth audit log retention policy is implemented or evidenced.
8. No rate limiting / brute-force protection is implemented or evidenced.
9. No centralized log redaction policy or secret-scanning gate is proven for release evidence.
10. No completed production release-gate bundle exists.

## Required Environment Variables

### Required For Production App Startup

| Variable | Purpose | Required when |
|---|---|---|
| `SPRING_PROFILES_ACTIVE=prod` | Enables prod profile. | Production deployment |
| `PROD_DATASOURCE_URL` | PostgreSQL JDBC URL. | Production deployment |
| `PROD_DATASOURCE_USERNAME` | PostgreSQL app username. | Production deployment |
| `PROD_DATASOURCE_PASSWORD` | PostgreSQL app password. | Production deployment |
| `APP_ADMIN_USERNAME` | Single-operator Basic Auth username. | Production deployment |
| `APP_ADMIN_PASSWORD` | Single-operator Basic Auth password. | Production deployment |
| `POSITION_PROVIDER_TYPE=BINANCE` | Production position provider selection. | Production deployment |
| `BINANCE_API_BASE_URL` | Binance base URL, default `https://fapi.binance.com`. | Production deployment / provider smoke |
| `BINANCE_API_KEY` | Binance readonly position/provider credential. | Production position provider `BINANCE` |
| `BINANCE_API_SECRET` | Binance readonly position/provider credential. | Production position provider `BINANCE` |
| `TRADE_MODEL_PRODUCTION_SCHEDULER_POLICY` | Production scheduler policy. | Production deployment |
| `TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_*` | Per-scheduler classification. | Production deployment |

### Required Only When Explicitly Enabled

| Variable | Purpose | Required when |
|---|---|---|
| `TRADE_MODEL_AI_OPENAI_ENABLED=true`, `OPENAI_API_KEY`, model/base-url vars | OpenAI provider. | OpenAI explicitly enabled |
| `TRADE_MODEL_AI_GEMINI_ENABLED=true`, `GEMINI_API_KEY`, model/base-url vars | Gemini provider. | Gemini explicitly enabled |
| `TRADE_MODEL_AI_XAI_ENABLED=true`, `XAI_API_KEY`, model/base-url vars | xAI/Grok provider. | xAI explicitly enabled |
| `NEWS_API_KEY`, `MACRO_CALENDAR_API_KEY`, `ETF_FLOW_API_KEY` | External context placeholders. | Future verified provider package only |
| `PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=true` and per-provider smoke flags | Live provider smoke. | Human-approved live provider smoke only |
| `RESTORE_*` and `RESTORE_CONFIRM` | Restore drill. | Controlled recovery restore only |
| `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID` | Future Telegram integration placeholders. | Not active in V1 |

## Secrets Manager And Rotation Plan

Before production readiness can move beyond `BLOCKED`, the deployment must adopt a secrets manager or equivalent controlled server secret store. Acceptable options include cloud secrets manager, Vault, SOPS/age with restricted deploy access, or a platform-native encrypted secret store.

Minimum plan:

1. Store datasource, admin, Binance, AI, external-context, future Telegram, backup, and restore secrets outside Git.
2. Inject secrets into the runtime environment without printing values.
3. Document who can read, rotate, and revoke each secret class.
4. Rotate `APP_ADMIN_PASSWORD` before first production exposure and after any operator handoff.
5. Rotate datasource credentials on a defined interval and after suspected exposure.
6. Rotate Binance/API provider keys before live provider smoke if they were ever copied outside the secret store.
7. Record redacted rotation evidence: secret name, owner, old version revoked, new version active, validation command, and timestamp.
8. Keep `.env`, dumps, screenshots with credentials, and terminal transcripts containing secrets out of Git and PR evidence.

## HTTPS / Reverse Proxy Checklist

Production deployment remains blocked until HTTPS/reverse-proxy evidence is complete:

1. Public entrypoint terminates TLS with a valid certificate.
2. HTTP-to-HTTPS redirect policy is documented and tested.
3. HSTS policy is decided and recorded.
4. Reverse proxy forwards only required app routes.
5. `/actuator/health`, `/actuator/health/liveness`, and `/actuator/health/readiness` expose no details/components.
6. Dashboard, review, and API surfaces require authentication through the public entrypoint.
7. The app is not exposed directly on an unprotected public port.
8. Proxy and app logs do not print authorization headers, passwords, API keys, or DB credentials.
9. Certificate renewal path is documented and tested.
10. Authenticated smoke through the HTTPS entrypoint is recorded with secrets redacted.

## Audit Logging / Access Logging Checklist

Production readiness remains blocked until an access/audit logging plan is implemented or explicitly accepted by the release gate:

1. Access logs are enabled at the reverse proxy or app boundary.
2. Auth failures and successes are observable without logging passwords.
3. Manual user-position writes and manual close actions are auditable through existing app logs/data paths or a future audit package.
4. Push/Recheck, Review Center, and dashboard API access can be traced by timestamp and operator source.
5. Logs have retention, rotation, and deletion policies.
6. Logs are redacted for `Authorization`, cookies, API keys, tokens, datasource passwords, and provider secrets.
7. Release evidence includes a redacted sample of access logs.

## Rate Limiting Checklist

Production readiness remains blocked until rate limiting / abuse protection is implemented or explicitly accepted by the release gate:

1. Authentication endpoints and protected APIs have brute-force protection at reverse proxy or app level.
2. `/api/**` routes have sane request-rate limits for a single-operator deployment.
3. Health endpoints are allowed but bounded by proxy-level limits.
4. Provider smoke/live external calls remain opt-in and cannot be triggered anonymously.
5. Rate-limit events are observable in access logs.
6. Rate limiting does not expose secrets in error responses or logs.

## Actuator Exposure Policy

Current policy:

- HTTP exposure is limited to `health`.
- Liveness/readiness are health groups under the health endpoint.
- Details and components are hidden.
- Prod guard rejects wildcard exposure and sensitive endpoints such as `env`, `beans`, `configprops`, and `mappings`.
- Dashboard, Review Center, and operational APIs remain behind Basic Auth.

This is acceptable as a minimal local/acceptance guard. Production still needs HTTPS/reverse-proxy and access-log evidence before deployment approval.

## Prohibited Secret Handling

The following are prohibited:

- committing `.env`
- committing real secrets or database dumps
- pasting API keys, passwords, tokens, authorization headers, or datasource URLs with credentials into docs, PRs, screenshots, or chat
- printing secrets from scripts
- running live provider smoke from Codex with real secrets
- using Binance withdrawal/trading/order permissions
- sending Telegram or external Push as part of this package
- treating config-only provider status as verified connected
- claiming production readiness without complete release-gate evidence

## Local Package Evidence

This PF5 package is documentation/status-source hardening only.

- No real secrets were accessed.
- No production server was accessed.
- No production DB was accessed.
- No destructive operation was run.
- No live provider call was made.
- No Java business logic changed.
- No schema or migration SQL changed.
- No runtime trading behavior changed.

## Production Readiness Decision

Production readiness remains BLOCKED.

Production deployment cannot proceed.

The repository has useful fail-closed prod guards for local configuration mistakes, but production readiness still lacks secrets manager integration, rotation evidence, HTTPS/reverse-proxy proof, audit/access logging evidence, rate limiting evidence, and real server release-gate evidence.

## Next Remediation Recommendation

Recommended next package: `PDR-PF6 Provider Live Smoke Evidence` only after secrets are stored in a controlled server secret store and the operator approves live external checks.

If secrets manager / HTTPS / rate limiting implementation is prioritized first, open a separate scoped package for implementation and tests. That package must still preserve no auto-open, no auto-close, no auto-reverse, no order execution, no auto-trading, no external push send, no fake positions, and no fake review records.

## Prohibited Items Preserved

- no auto-open
- no auto-close
- no auto-reverse
- no order execution
- no auto-trading
- no external push send
- no fake positions
- no fake review records
- no production-ready claim
