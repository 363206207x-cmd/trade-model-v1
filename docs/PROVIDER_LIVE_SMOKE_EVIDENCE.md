# Provider Live Smoke Evidence

Package: PDR-PF6 Provider Live Smoke Evidence
Date: 2026-07-07
Branch: `codex/pdr-pf6-provider-live-smoke-evidence`
Current main commit reviewed: `cf152314995aab810628b82234b1e106537e588b`
Production readiness: BLOCKED
Production deployment: cannot proceed

## Scope

This package records provider live-smoke readiness evidence and the safe evidence collection policy after PDR-PF5. It does not enable default live external calls, does not access real secrets, does not connect to a production server or production database, and does not approve production deployment.

No real secrets were accessed. No secret values were printed. No production server was accessed. No production DB was accessed. No destructive operation was run. No runtime trading behavior was changed.

## Providers Reviewed

| Provider area | Current path reviewed | Result | Notes |
|---|---|---|---|
| Binance public market data | `scripts/prod-provider-smoke.sh` public futures time endpoint path | SKIPPED_DISABLED_BY_DEFAULT | The script supports an explicit opt-in public check with `PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=true` and `PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED=true`; this package did not run a live network call. |
| OpenAI | `scripts/prod-provider-smoke.sh` models endpoint path | SKIPPED_DISABLED_BY_DEFAULT | Requires explicit OpenAI smoke flag plus `OPENAI_API_KEY`; this package did not inspect or print key values. |
| Gemini | `scripts/prod-provider-smoke.sh` models endpoint path | SKIPPED_DISABLED_BY_DEFAULT | Requires explicit Gemini smoke flag plus `GEMINI_API_KEY`; this package did not inspect or print key values. |
| xAI / Grok | `scripts/prod-provider-smoke.sh` models endpoint path | SKIPPED_DISABLED_BY_DEFAULT | Requires explicit xAI smoke flag plus `XAI_API_KEY`; this package did not inspect or print key values. |
| External context / news / macro / ETF flow | `application.yml`, `.env.example`, and provider smoke placeholder behavior | SKIPPED_DISABLED_BY_DEFAULT | Keys are config placeholders only. The current provider smoke harness reports configured/skipped status and does not implement a live external-context call. |

## Commands Inspected

- `scripts/prod-provider-smoke.sh`
- `scripts/prod-smoke.sh`
- `scripts/prod-release-gate.sh`
- `.env.example`
- `docs/PRODUCTION_READINESS_RUNBOOK.md`
- `docs/SECRETS_AND_ACCESS_HARDENING.md`
- `docs/PRODUCTION_READINESS_PREFLIGHT_AUDIT.md`

## Commands Run

Safe no-call provider smoke was run with every external-call switch explicitly disabled:

```bash
PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=false PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED=false PROVIDER_SMOKE_OPENAI_ENABLED=false PROVIDER_SMOKE_GEMINI_ENABLED=false PROVIDER_SMOKE_XAI_ENABLED=false perl -e 'alarm 300; exec @ARGV' bash scripts/prod-provider-smoke.sh
```

Output summary:

```text
PROVIDER_LIVE_SMOKE: SKIPPED - set PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=true and per-provider flags to run live checks
BINANCE_PUBLIC_SMOKE: SKIPPED
OPENAI_SMOKE: SKIPPED
GEMINI_SMOKE: SKIPPED
XAI_SMOKE: SKIPPED
```

Package validation commands:

```text
git diff --check: PASS
bash scripts/check-workflow-contract.sh: PASS
bash scripts/v1-delivery-check.sh: PASS
bash scripts/v1-state.sh: PASS (script exits successfully; branch-local dirty/unmerged blockers are expected until this PR is merged/effective)
YAML parse: PASS
Changed shell scripts: none; bash -n not applicable
```

## Evidence Summary

1. Provider smoke defaults are disabled and do not call live external providers.
2. The no-call mode returns SKIPPED statuses instead of fake PASS.
3. Provider scripts do not print passwords or API key values in the inspected paths.
4. `prod-smoke.sh` rejects provider `CONNECTED` status unless external calls are explicitly allowed by `SMOKE_ALLOW_EXTERNAL_CALLS=true` and a verified source exists.
5. `prod-release-gate.sh` treats provider live smoke as incomplete unless `RELEASE_GATE_REQUIRE_PROVIDER_SMOKE=true` is explicitly set and provider smoke produces PASS.
6. `.env.example` documents provider smoke flags as false by default and warns that real `.env` files and secrets must not be committed.
7. No live provider PASS is claimed by this package.

## Result Per Provider

| Provider | Result | Evidence |
|---|---|---|
| Binance public market data | SKIPPED_DISABLED_BY_DEFAULT | Default no-call smoke returned `BINANCE_PUBLIC_SMOKE: SKIPPED`; live public endpoint was not called. |
| OpenAI | SKIPPED_DISABLED_BY_DEFAULT | Default no-call smoke returned `OPENAI_SMOKE: SKIPPED`; key presence was not inspected and endpoint was not called. |
| Gemini | SKIPPED_DISABLED_BY_DEFAULT | Default no-call smoke returned `GEMINI_SMOKE: SKIPPED`; key presence was not inspected and endpoint was not called. |
| xAI / Grok | SKIPPED_DISABLED_BY_DEFAULT | Default no-call smoke returned `XAI_SMOKE: SKIPPED`; key presence was not inspected and endpoint was not called. |
| External context / news / macro / ETF flow | SKIPPED_DISABLED_BY_DEFAULT | No live external-context harness is implemented; keys remain optional/config-only placeholders. |

## Redaction Policy

Provider smoke evidence may record only provider name, status, timestamp, command shape, and redacted status lines. Evidence must not include:

- API key values
- authorization headers
- datasource URLs with credentials
- response bodies from provider APIs
- `.env` contents
- screenshots or terminal transcripts containing secrets
- Binance keys with withdrawal, trading, or order permissions

Future live provider evidence must be collected only in a controlled local/server environment with explicit external-call approval and timeouts. Missing credentials or network failures must be recorded as `SKIPPED_MISSING_SECRET`, `SKIPPED_TIMEOUT`, `BLOCKED_PROVIDER_UNAVAILABLE`, or `FAIL`; never as PASS.

## Remaining Blockers

1. Binance public provider live smoke has not been run in this package.
2. AI provider live smoke has not been run because real provider secrets were not accessed or inspected.
3. External context live provider smoke is not implemented by the current harness.
4. Secrets manager integration and rotation evidence are still missing.
5. HTTPS/reverse-proxy, rate limiting, audit/access logging, and real server smoke evidence are still missing.
6. PostgreSQL empty migration evidence remains `BLOCKED_TIMEOUT` from PDR-PF3.
7. Current-state migration, backup, restore, rollback, and post-restore smoke evidence remain unproven in a server/staging environment.
8. No completed production release-gate evidence bundle exists.

## Production Readiness Decision

Production readiness remains BLOCKED.

Production deployment cannot proceed.

PDR-PF6 proves that provider live smoke remains opt-in, redacted, and default-disabled, and records safe no-call evidence. It does not prove provider connectivity or production deployment readiness.

## Next Remediation Recommendation

Recommended next package: `PDR-PF7 Push Recheck Quote-Unavailable Guard`, or a separately scoped controlled-server provider evidence rerun if the operator has secrets manager / server env in place and explicitly approves live external calls.

Before any live provider evidence can count toward production readiness, the run must provide redacted output, explicit timeout behavior, no secret printing, and no trading/order/external-push side effects.

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
