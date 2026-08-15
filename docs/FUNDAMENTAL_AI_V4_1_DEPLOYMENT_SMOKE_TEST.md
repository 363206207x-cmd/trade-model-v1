# Fundamental AI v4.1 Deployment Smoke Test

Status: executable smoke contract exists in `scripts/prod-smoke.sh`. This
document does not claim a production deployment occurred.

## Preconditions

- Approved merged-main artifact running with Java 17.
- PostgreSQL backup confirmed and Flyway at V13.
- Standard `clean package` artifact passed
  `scripts/standard-release-postgresql-smoke.sh`; no special Flyway profile was
  used. The same script also verifies packaged-JAR login, CSRF Session, logout,
  old-Session invalidation, existing V13 restart and checksum fail closed.
- `TRADE_MODEL_SMOKE_USERNAME` and `TRADE_MODEL_SMOKE_PASSWORD` injected from a
  secret store.
- `APP_URL` points to the trusted local endpoint or the HTTPS reverse-proxy
  endpoint. Public mode must never use bare HTTP.
- Set `SMOKE_ALLOW_EXTERNAL_CALLS=true` only when the release owner has enabled
  and authorized real provider calls.

## Command

Run `bash scripts/prod-smoke.sh` with the variables above. The script stores
temporary responses with restricted permissions and removes them on exit.

## Required Checks

1. `/actuator/health`, `/actuator/health/liveness`, and
   `/actuator/health/readiness` return `UP` without exposing component details.
2. An unauthenticated `/api/dashboard/home` request returns `401`.
3. `/login` provides CSRF, valid credentials establish a Session, and
   authenticated `/dashboard` loads the canonical Workspace Home.
4. `/api/dashboard/home` and `/api/review/center` return their required
   contracts and safety flags.
5. Logout invalidates the pre-logout Session.
6. Provider health values remain truthful. `CONNECTED` is invalid unless real
   external calls are explicitly enabled and a verified source exists.
   HTTP 451 is `REGION_RESTRICTED`; unavailable ADAUSDT remains unavailable and
   must not erase successful assets from a truthful `PARTIAL` scan.
7. Dynamic Top6, Position Monitoring, Final-only Execution Plan and one visible
   AI role render without fake values or raw enum primary copy.
8. All fourteen product routes return the expected authenticated page and no
   route activates the superseded Home.
9. Push Recheck scheduler status is `SUCCEEDED`, `PARTIAL`, or an explicit
   `FAILED` with trace/error evidence; query failures cannot appear successful.
10. Logs contain no key, password, Session cookie or unhandled exception.
11. AI readiness is `AUTHORIZED` only for the exact frozen model after an
    explicit cached verification; fallback output is not Ready.
12. CoinGlass disabled is `NOT_CONFIGURED`. Enabled external CoinGlass with a
    missing key, missing RPM, or non-positive RPM is respectively
    `KEY_MISSING`, `RPM_NOT_CONFIGURED`, or `INVALID_RPM`; none may be connected
    or emit zero-valued evidence.
13. Unsupported/disabled/region-restricted provider identities produce no
    market-data invocation. A fallback is used only with its own exact
    `SUPPORTED` capability.

## Result Rules

- Any failed liveness/readiness/auth/session/data-safety check blocks release.
- Provider unavailability does not fail application liveness, but it blocks the
  provider-dependent decision chain and must render fail closed.
- A local controlled fixture may validate UI behavior but cannot satisfy target
  runtime provider acceptance.
