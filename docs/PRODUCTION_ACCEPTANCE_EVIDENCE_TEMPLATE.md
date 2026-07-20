# Production Acceptance Evidence Template

This template is for the human-run real-server acceptance gate. It records evidence only; it is not a production deployment approval by itself.

Production Deployment Readiness remains `BLOCKED` until every required evidence section is completed, reviewed, and explicitly approved by the human release gate.

Current P3-H package status: `BLOCKED_MISSING_CONTROLLED_STAGING_INPUT`.
No controlled server or Secret Store access was attempted. Do not mark any
server section PASS from offline template/guard evidence.

## Evidence Metadata

- Evidence date:
- Operator:
- Server / environment:
- Git commit / image tag:
- `.env` source location, no secret values:
- Release gate reviewer:
- Decision: `BLOCKED` / `CONDITIONALLY_READY_FOR_PRIVATE_SERVER`

## 1. Docker Compose Config

Command:

```bash
docker compose config
```

Required evidence:

- Command exit code is 0.
- Compose config includes `postgres`, `migrate`, and `app` services.
- No real secrets are pasted into this evidence file.

Evidence output summary:

```text
PASTE REDACTED OUTPUT SUMMARY HERE
```

Status: `PASS` / `FAIL` / `NOT_RUN`

## 2. PostgreSQL Service Startup

Commands:

```bash
docker compose up -d postgres
docker compose ps
docker compose logs --tail=100 postgres
```

Required evidence:

- `postgres` container is running.
- PostgreSQL healthcheck is healthy.
- Logs do not expose secrets.

Evidence output summary:

```text
PASTE REDACTED OUTPUT SUMMARY HERE
```

Status: `PASS` / `FAIL` / `NOT_RUN`

## 3. Flyway Migration

Commands:

```bash
docker compose --profile migrate run --rm migrate
docker compose exec postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select version, description, success from flyway_schema_history order by installed_rank;"
```

Required evidence:

- Migration command succeeds.
- `flyway_schema_history` exists.
- V1 through V8 migrations are recorded with `success=true`.
- The exact V8 contract includes `tm_user` and its required columns before the app starts.
- No schema drift or unsafe prod guard failure is present.

Evidence output summary:

```text
PASTE REDACTED OUTPUT SUMMARY HERE
```

Status: `PASS` / `FAIL` / `NOT_RUN`

## 4. App Prod Startup

Commands:

```bash
docker compose up -d app
docker compose ps
docker compose logs --tail=200 app
```

Required evidence:

- App starts with `prod` profile.
- `ProductionProfileSafetyGuard` does not fail.
- App is not publicly exposed unless HTTPS/reverse-proxy readiness is separately approved.
- Logs do not print secrets.

Evidence output summary:

```text
PASTE REDACTED OUTPUT SUMMARY HERE
```

Status: `PASS` / `FAIL` / `NOT_RUN`

## 5. Authenticated Smoke

Commands:

```bash
export APP_URL=http://localhost:8081
export TRADE_MODEL_SMOKE_USERNAME="$TRADE_MODEL_INITIAL_USERNAME"
export TRADE_MODEL_SMOKE_PASSWORD="$TRADE_MODEL_INITIAL_PASSWORD"
bash scripts/prod-smoke.sh
```

Required evidence:

- `/actuator/health` returns HTTP 200 and `status=UP`.
- `/actuator/health/liveness` returns HTTP 200 and `status=UP`.
- `/actuator/health/readiness` returns HTTP 200 and `status=UP`.
- Anonymous `/api/dashboard/home` returns HTTP 401.
- `GET /login` returns the form and a non-hardcoded CSRF token.
- `POST /login` establishes an authenticated server-side Session Cookie.
- `/api/dashboard/home` returns HTTP 200 with expected shape.
- `/api/review/center` returns HTTP 200 with expected shape.
- CSRF-protected `POST /logout` succeeds and the pre-logout Session is rejected afterward.
- `safety.notAutoTrading=true`.
- `safety.notOrderExecution=true`.
- Password, Cookie, Session ID, CSRF token, response body, and request headers are not printed.

Evidence output summary:

```text
PASTE REDACTED OUTPUT SUMMARY HERE
```

Status: `PASS` / `FAIL` / `NOT_RUN`

## 6. Backup Drill

Command:

```bash
bash scripts/prod-backup.sh
```

Required evidence:

- Backup file is created under an ignored backup directory.
- Backup filename includes a timestamp.
- No database password or API secret is printed.
- Backup file path is recorded, but the dump file itself is not committed.

Evidence output summary:

```text
PASTE REDACTED OUTPUT SUMMARY HERE
```

Status: `PASS` / `FAIL` / `NOT_RUN`

## 7. Restore Drill

Restore must target a controlled recovery database, not the live database, unless separately approved by the human release gate.

Commands:

```bash
export RESTORE_CONFIRM=I_UNDERSTAND_RESTORE_CAN_OVERWRITE_DATA
bash scripts/prod-restore.sh

export APP_URL=http://localhost:8081
export TRADE_MODEL_SMOKE_USERNAME="$TRADE_MODEL_INITIAL_USERNAME"
export TRADE_MODEL_SMOKE_PASSWORD="$TRADE_MODEL_INITIAL_PASSWORD"
bash scripts/prod-smoke.sh
```

Required evidence:

- Restore target is explicitly documented.
- Restore command succeeds against the controlled recovery DB.
- Smoke passes after restore.
- RPO / RTO observation is recorded.
- If restore is not run, production readiness cannot advance beyond `BLOCKED`.

Evidence output summary:

```text
PASTE REDACTED OUTPUT SUMMARY HERE
```

Status: `PASS` / `FAIL` / `NOT_RUN`

## 8. HTTPS / Reverse Proxy / Auth Smoke

Commands depend on the server reverse proxy. The current authentication smoke
must use the same form-login/Session/CSRF path as a browser:

```bash
export APP_URL=https://YOUR_HOST
export TRADE_MODEL_SMOKE_CA_CERT=/approved/runtime/ca-bundle.pem
export TRADE_MODEL_SMOKE_USERNAME="$TRADE_MODEL_INITIAL_USERNAME"
export TRADE_MODEL_SMOKE_PASSWORD="$TRADE_MODEL_INITIAL_PASSWORD"
bash scripts/prod-smoke.sh
```

Required evidence:

- HTTPS endpoint is reachable.
- HTTP-to-HTTPS policy is documented.
- Dashboard and review APIs require authentication.
- Login, Session Cookie, CSRF logout, and post-logout invalidation all pass through the proxy.
- Unauthenticated dashboard/review requests return 401 or the documented
  authentication-denied status; they must never return 200.
- A deliberately incorrect staging password is denied without printing the
  password, Cookie, Session ID, CSRF token, header, or response body.
- Password is not printed.
- If HTTPS/reverse proxy is not implemented, this blocker remains open.

Evidence output summary:

```text
PASTE REDACTED OUTPUT SUMMARY HERE
```

Status: `PASS` / `FAIL` / `NOT_RUN`

## 9. Provider Live Smoke

Default smoke does not call live providers. Provider live smoke requires real server env and separate human approval for external calls. Do not paste key values into this evidence file.

Dry-run command, expected to skip without network calls:

```bash
PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=false bash scripts/prod-provider-smoke.sh
```

Live command, enable only providers that are approved for this evidence run:

```bash
PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=true \
PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED=true \
PROVIDER_SMOKE_OPENAI_ENABLED=true \
PROVIDER_SMOKE_GEMINI_ENABLED=true \
PROVIDER_SMOKE_XAI_ENABLED=true \
bash scripts/prod-provider-smoke.sh
```

Required evidence:

- Binance public smoke result: `PASS` / `FAIL` / `SKIPPED` / `NOT_CONFIGURED`
- OpenAI smoke result: `PASS` / `FAIL` / `SKIPPED` / `NOT_CONFIGURED`
- Gemini smoke result: `PASS` / `FAIL` / `SKIPPED` / `NOT_CONFIGURED`
- XAI smoke result: `PASS` / `FAIL` / `SKIPPED` / `NOT_CONFIGURED`
- External context result: `CONFIGURED` / `SKIPPED`
- Final provider result: `PROVIDER_LIVE_SMOKE: PASS` / `FAIL` / `SKIPPED` / `INCOMPLETE`
- Provider readiness shape is present in `/api/dashboard/home.diagnostics.providerReadiness` when app smoke is also run.
- Config-only providers are not marked `CONNECTED` unless a verified source exists.
- Binance smoke uses public/read-only market data only; no private trading or order endpoint is called.
- AI provider smoke is recorded only for keys configured in the server `.env` or future secrets manager.
- Telegram remains not `CONNECTED` unless a verified Telegram status source exists.
- No secrets are printed in logs, screenshots, terminal history, or pasted evidence.
- Binance key, if present for other readonly checks, has no withdrawal permission and no trading/order permission.
- No Telegram send, Push dispatch, Push Recheck execution, buy/sell/order/execute, or auto-trading endpoint was called.

Evidence output summary:

```text
PASTE REDACTED OUTPUT SUMMARY HERE
```

Redaction checklist:

- [ ] API keys are not visible.
- [ ] Authorization headers are not visible.
- [ ] `.env` contents are not pasted.
- [ ] Provider response bodies are not pasted if they include account- or organization-specific data.

Status: `PASS` / `FAIL` / `NOT_RUN`

## 10. Safety Boundary Check

Commands:

```bash
grep -R "@.*Mapping" src/main/java/org/example/trademodel/controller | grep -Ei "buy|sell|order|execute|auto-trading" || true
bash scripts/prod-release-gate.sh
```

Required evidence:

- No buy/sell/order/execute route is present.
- No auto-open/auto-close/auto-trading route is present.
- Telegram send is not enabled by this gate.
- Push dispatch is not run by this gate.
- Push Recheck execution is not run by this gate.
- AI output cannot authorize trades.
- Push/review/decision surfaces remain review-only.

Evidence output summary:

```text
PASTE REDACTED OUTPUT SUMMARY HERE
```

Status: `PASS` / `FAIL` / `NOT_RUN`

## Release Gate Decision

All sections above must be `PASS` before moving beyond `BLOCKED`. If any section is `FAIL` or `NOT_RUN`, keep Production Deployment Readiness as `BLOCKED`.

Final decision:

- Production Deployment Readiness: `BLOCKED` / `CONDITIONALLY_READY_FOR_PRIVATE_SERVER`
- Reviewer approval:
- Follow-up blockers:
