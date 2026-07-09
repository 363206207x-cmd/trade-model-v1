# Real Server Smoke Evidence Gate

Package: PDR-LIVE15 Real Server Smoke Evidence Plan / Gate
Branch: `codex/pdr-live15-real-server-smoke-evidence-gate`
Base main commit: `51b0b699`
Status: CONTROLLED GATE RECORDED
Production deployment readiness: BLOCKED

## Scope

This package records a controlled real-server / staging-server smoke evidence gate. It is not production deployment, does not access a production database, does not place orders, does not send external Push, does not print or commit secrets, and does not add trading runtime behavior.

No controlled non-production server endpoint was present in the local environment for this run, so no server request was made. The package adds a safe gate script that defaults to skip and can be used later only when a controlled endpoint and already-provisioned credentials are present in environment variables.

## Inputs Checked

The following environment variables were checked for presence only. Values were not printed:

1. `CONTROLLED_SERVER_BASE_URL`
2. `CONTROLLED_SERVER_ADMIN_USERNAME`
3. `CONTROLLED_SERVER_ADMIN_PASSWORD`

Observed presence status:

| Variable | Presence status |
|---|---:|
| `CONTROLLED_SERVER_BASE_URL` | MISSING |
| `CONTROLLED_SERVER_ADMIN_USERNAME` | MISSING |
| `CONTROLLED_SERVER_ADMIN_PASSWORD` | MISSING |

## Commands Inspected Or Prepared

1. `scripts/prod-smoke.sh` already performs public health/liveness/readiness checks plus authenticated `/api/dashboard/home` and `/api/review/center` checks.
2. `scripts/prod-release-gate.sh` already records the larger release gate as incomplete unless all required evidence is supplied.
3. `scripts/controlled-real-server-smoke.sh` is added by this package as a redacted controlled gate wrapper.

The controlled wrapper:

1. Skips by default when `CONTROLLED_SERVER_BASE_URL` is missing.
2. Does not print URL, username, password, cookie, authorization header, response body, token, or secret values.
3. Requires HTTPS for non-local endpoints.
4. Runs public health/readiness checks without credentials only after a controlled endpoint is present.
5. Runs authenticated dashboard/review API smoke only when credentials are already present in environment variables.
6. Wraps the authenticated smoke with a 300 second timeout by default.
7. Delegates shape/safety checks to `scripts/prod-smoke.sh` without enabling external calls.

## Evidence Status Summary

| Gate | Status | Evidence / Finding |
|---|---:|---|
| Controlled server env presence | SKIPPED_MISSING_CONTROLLED_SERVER | `CONTROLLED_SERVER_BASE_URL` was not present. No server was contacted. |
| Server smoke status | SKIPPED_MISSING_CONTROLLED_SERVER | No controlled endpoint was available, so health/readiness and authenticated API smoke did not run. |
| HTTPS endpoint status | SKIPPED_MISSING_CONTROLLED_SERVER | No endpoint was present to classify as HTTPS. Existing LIVE13 HTTPS/reverse-proxy template remains `DOCUMENTED_WITH_CONFIG`. |
| Health/readiness smoke | SKIPPED_MISSING_CONTROLLED_SERVER | No endpoint was present. |
| Authenticated dashboard/review API smoke | SKIPPED_MISSING_CONTROLLED_SERVER | No endpoint or credentials were present. |
| Access logging / auth audit / rate-limit through server | SKIPPED_MISSING_CONTROLLED_SERVER | App-level guards are `GUARD_PASS` from LIVE12, but no server/proxy log evidence exists. |
| Order / auto-trading / external Push boundary | GUARD_PASS BY SCOPE | This package only adds a read-only smoke gate and docs; it does not call order, auto-open, auto-close, auto-reverse, auto-trading, Push send, or provider execution paths. |

## Server Smoke Status

Result: SKIPPED_MISSING_CONTROLLED_SERVER.

Reason: no controlled non-production server base URL was present in the environment. This package did not ask the user for secrets, did not print environment values, and did not attempt production server access.

If a controlled non-production server becomes available, the future operator command is:

```bash
CONTROLLED_SERVER_BASE_URL="https://<controlled-non-production-host>" CONTROLLED_SERVER_ADMIN_USERNAME="<redacted>" CONTROLLED_SERVER_ADMIN_PASSWORD="<redacted>" bash scripts/controlled-real-server-smoke.sh
```

The command must be run only outside chat with already-provisioned environment variables. The output must be redacted before being recorded in a later evidence package.

## HTTPS Endpoint Status

Result: SKIPPED_MISSING_CONTROLLED_SERVER.

LIVE13 records HTTPS/reverse-proxy as `DOCUMENTED_WITH_CONFIG`, but real HTTPS evidence is still missing. A future PASS requires redacted proof for:

1. HTTPS endpoint reached through the intended entrypoint.
2. TLS certificate validity and hostname match.
3. HTTP-to-HTTPS redirect behavior.
4. HSTS header policy after HTTPS is stable.
5. Forwarded header behavior.
6. Authenticated dashboard/review API checks through the proxy.

## Health / Readiness Smoke Status

Result: SKIPPED_MISSING_CONTROLLED_SERVER.

A future PASS must prove, without exposing details or secrets:

1. `/actuator/health` returns HTTP 200 and `status=UP`.
2. `/actuator/health/liveness` returns HTTP 200 and `status=UP`.
3. `/actuator/health/readiness` returns HTTP 200 and `status=UP`.
4. Health payloads do not expose sensitive details.

## Authenticated Dashboard / Review API Smoke Status

Result: SKIPPED_MISSING_CONTROLLED_SERVER.

A future PASS must prove, with credentials supplied only through environment variables and never printed:

1. `/api/dashboard/home` returns HTTP 200.
2. `/api/review/center` returns HTTP 200.
3. Dashboard response includes `safety.notAutoTrading=true`.
4. Dashboard response includes `safety.notOrderExecution=true`.
5. Review response remains readonly and does not fabricate records.

## Access Logging / Auth Audit / Rate Limit Through Server Status

Result: SKIPPED_MISSING_CONTROLLED_SERVER.

LIVE12 records application-level access logging, auth audit logging, sensitive-data redaction, and rate limiting as `GUARD_PASS`. Real server evidence remains missing for:

1. Access logs generated through the server/proxy path.
2. Auth failure audit events generated through the server/proxy path.
3. Rate-limit behavior through the server/proxy path.
4. Forwarded client IP handling.
5. Log redaction in real server output.
6. Log retention or aggregation evidence.

## Safety Confirmation

This package confirms:

1. No production server was accessed.
2. No production DB was accessed.
3. No orders were placed.
4. No external Push was sent.
5. No secrets were printed or committed.
6. No `.env` file was committed.
7. No Java business logic changed.
8. No schema or Flyway SQL changed.
9. No auto-open, auto-close, auto-reverse, order execution, auto-trading, fake positions, or fake review records were introduced.

## Production Readiness Decision

Production readiness: BLOCKED.

Production deployment cannot proceed. This package defines and records the real-server smoke gate, but it does not produce real server PASS evidence because no controlled non-production endpoint was available.

## Remaining Blockers

1. Real controlled server smoke is `SKIPPED_MISSING_CONTROLLED_SERVER`.
2. Real HTTPS/reverse-proxy smoke remains missing.
3. Real server access logging / auth audit / rate-limit evidence remains missing.
4. Secrets manager integration is still `DOCUMENTED_WITH_PLAN`; real secret-store injection evidence is missing.
5. Credential rotation is still `DOCUMENTED_WITH_PLAN`; actual rotation drill evidence is missing.
6. AI provider live smoke remains `SKIPPED_MISSING_SECRET`.
7. External context/news/macro provider proof remains missing.
8. Release-owner approval remains missing.

## Next Recommendation

Recommended next package: run this gate in an approved controlled non-production server environment after `CONTROLLED_SERVER_BASE_URL`, `CONTROLLED_SERVER_ADMIN_USERNAME`, and `CONTROLLED_SERVER_ADMIN_PASSWORD` are provisioned outside chat, or continue with `PDR-LIVE16 AI Provider / External Context Release Policy Evidence` if real server infrastructure is still unavailable.

Production deployment must remain blocked until a later release-gate package records complete PASS evidence or explicit release-owner waivers for every required gate.
