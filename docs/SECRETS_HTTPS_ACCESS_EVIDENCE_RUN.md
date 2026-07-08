# Secrets HTTPS Access Evidence Run

Branch: `codex/pdr-live10-secrets-https-access-evidence`
Base main commit: `c039f3caf7e0951499493f7696ad1c56dc54e256`
Package: PDR-LIVE10 Secrets HTTPS Access Logging Rate Limit Evidence
Status: CONTROLLED EVIDENCE RECORDED
Production deployment readiness: BLOCKED

## Scope

This package records the current evidence status for production secrets handling, HTTPS/reverse proxy, access logging, authentication audit logging, rate limiting, actuator exposure, and production-profile fail-closed guards. It is not production deployment, does not access a production server, does not access a production database, does not print or commit secrets, and does not add runtime trading behavior.

## Commands And Sources Inspected

- `src/main/resources/application-prod.yml`
- `.env.example`
- `src/main/java/org/example/trademodel/config/ProductionProfileSafetyGuard.java`
- `src/main/java/org/example/trademodel/config/SecurityConfig.java`
- `src/test/java/org/example/trademodel/config/ProductionProfileSafetyGuardTest.java`
- `src/test/java/org/example/trademodel/security/AuthAccessControlSecurityTest.java`
- `src/test/java/org/example/trademodel/actuator/ActuatorHealthSecurityTest.java`
- `src/test/java/org/example/trademodel/health/ProdSmokeScriptHealthTest.java`
- `docs/SECRETS_AND_ACCESS_HARDENING.md`
- `docs/PRODUCTION_READINESS_RUNBOOK.md`
- `docs/PRODUCTION_READINESS_PREFLIGHT_AUDIT.md`

No `.env` file was read. No environment secret values were printed. `git ls-files` shows `.env.example` is tracked; no real `.env` file is tracked.

## Evidence Status Summary

| Area | Status | Evidence / Finding |
|---|---|---|
| Production profile secret/env requirements | GUARD_PASS | `application-prod.yml` reads required datasource/admin/Binance values from env placeholders. `ProductionProfileSafetyGuardTest` proves fail-closed behavior for H2 memory DB, blank datasource password, simulated provider, missing Binance key/secret, missing/unsafe admin credentials, unsafe public bind, sensitive actuator exposure, missing scheduler policy, and unsafe scheduler opt-in. |
| Secret handling / committed secrets | GUARD_PASS | `.env.example` contains placeholders and says not to commit `.env` or real secrets. `git ls-files` only reports `.env.example` for env files. No real secrets were printed or committed in this package. |
| Secrets manager integration | MISSING_EVIDENCE | `docs/SECRETS_AND_ACCESS_HARDENING.md` defines a secrets manager plan, but no secrets manager integration or server-side injection evidence exists. |
| Credential rotation | MISSING_EVIDENCE | Rotation requirements are documented, but no rotation drill evidence exists for admin, datasource, Binance, AI, external-context, or future Telegram credentials. |
| HTTPS / reverse proxy | DOCUMENTED_NOT_EVIDENCED | The runbook/checklist documents HTTPS, HTTP-to-HTTPS redirect, HSTS, proxy route, and certificate renewal requirements. No reverse proxy config, TLS proof, HTTPS smoke, or certificate renewal evidence exists in this package. |
| Access logging | MISSING_EVIDENCE | `docs/SECRETS_AND_ACCESS_HARDENING.md` defines access-log requirements. No enabled access-log config, retention policy evidence, or redacted sample access log exists. |
| Auth audit logging | MISSING_EVIDENCE | Basic Auth protection exists, but no explicit auth success/failure audit log implementation or redacted sample evidence exists. |
| Rate limiting / brute-force protection | MISSING_EVIDENCE | Rate-limit requirements are documented. No app/proxy rate-limit configuration, tests, or redacted rate-limit event evidence exists. |
| Actuator exposure | GUARD_PASS | `ActuatorHealthSecurityTest` proves public health/liveness/readiness are minimal, and sensitive actuator endpoints such as env/beans/configprops/mappings/loggers are not exposed even with auth. `ProductionProfileSafetyGuardTest` rejects wildcard/sensitive actuator exposure. |
| Auth access control | GUARD_PASS | `AuthAccessControlSecurityTest` proves dashboard/review/API/write/recheck routes require Basic Auth and representative authenticated access works. Static resource misses are not auth challenges. |
| No executable trading route surface | GUARD_PASS | `AuthAccessControlSecurityTest.noExecutableTradingRouteSurfaceIsIntroduced` asserts no `/buy`, `/sell`, `/order`, `/execute`, or `/auto-trading` route surface exists. |

## Secret Handling Evidence Status

Status: GUARD_PASS for local repository hygiene and production fail-closed guard, MISSING_EVIDENCE for real secret-store operation.

Evidence:

1. `application-prod.yml` references secrets through environment placeholders rather than literals.
2. `.env.example` contains placeholder values such as `change-me` and explicitly says not to commit real `.env` or real secrets.
3. Production guard rejects missing datasource password, missing Binance credentials, blank admin credentials, and unsafe admin password defaults.
4. Real secrets manager injection, redacted server-side env proof, and credential rotation evidence are still missing.

## HTTPS / Reverse Proxy Status

Status: DOCUMENTED_NOT_EVIDENCED.

Evidence:

1. Existing docs define HTTPS/reverse proxy requirements.
2. No production server was accessed.
3. No reverse proxy config, TLS certificate evidence, redirect/HSTS evidence, or HTTPS authenticated smoke evidence was collected.

## Access Logging Status

Status: MISSING_EVIDENCE.

Evidence:

1. Access logging requirements are documented in `docs/SECRETS_AND_ACCESS_HARDENING.md`.
2. No enabled access-log configuration or redacted sample access log exists.
3. No retention, rotation, or deletion evidence exists.

## Auth Audit Logging Status

Status: MISSING_EVIDENCE.

Evidence:

1. Basic Auth route protection is tested.
2. No explicit auth success/failure audit log implementation or redacted sample exists.
3. No auth audit retention policy evidence exists beyond documentation.

## Rate Limit Status

Status: MISSING_EVIDENCE.

Evidence:

1. Rate limiting requirements are documented.
2. No app/proxy rate limit implementation, tests, or redacted event sample exists.
3. Production readiness cannot advance until this is implemented, evidenced, or explicitly accepted by the release gate.

## Actuator Exposure Status

Status: GUARD_PASS.

Evidence:

1. Only health endpoints are exposed by policy.
2. Health/liveness/readiness are public and minimal.
3. Sensitive actuator endpoints are not exposed even with authentication.
4. Production guard rejects wildcard and sensitive actuator endpoint exposure.

## Production Profile Guard Status

Status: GUARD_PASS.

Evidence:

1. Production guard rejects unsafe datasource, provider, admin credential, public bind, actuator, and scheduler settings.
2. AI provider secrets are required only when the matching provider is explicitly enabled.
3. Position Monitor scheduler remains default-off under production guard policy.

## Safety Confirmation

- No production server was accessed.
- No production DB was accessed.
- No real secrets were printed.
- No real secrets were committed.
- No `.env` was committed.
- No destructive operation was run.
- No orders were placed.
- No order execution path was introduced.
- No auto-open, auto-close, or auto-reverse behavior was introduced.
- No auto-trading behavior was introduced.
- No external Push was sent.
- No fake positions were created.
- No fake review records were created.

## Production Readiness Decision

Production readiness remains BLOCKED.

This package confirms useful production-profile and access-control guard evidence, but it does not provide real secrets manager, credential rotation, HTTPS/reverse proxy, access logging, auth audit logging, rate limiting, real server smoke, or production release-gate approval evidence. Production deployment cannot proceed.

## Remaining Blockers

1. Secrets manager integration and redacted server-side secret injection evidence are missing.
2. Credential rotation drill evidence is missing.
3. HTTPS/reverse proxy, redirect/HSTS, certificate renewal, and authenticated HTTPS smoke evidence are missing.
4. Access logging implementation/evidence and redacted sample logs are missing.
5. Auth audit logging implementation/evidence and redacted samples are missing.
6. Rate limiting / brute-force protection implementation/evidence is missing.
7. Real server production smoke and final release-gate evidence remain incomplete.
8. AI and external-context provider PASS evidence remains missing.

## Next Recommendation

Proceed only to the next explicitly scoped remediation package, recommended as `PDR-LIVE11 HTTPS / Access Logging / Rate Limit Remediation`, or a controlled secrets-manager/credential-rotation evidence package. The next package must not be production deployment.
