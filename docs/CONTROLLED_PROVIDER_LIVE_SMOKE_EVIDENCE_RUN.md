# Controlled Provider Live Smoke Evidence Run

Branch: `codex/pdr-live8-provider-live-smoke-evidence-run`
Base main commit: `ee3e0a7eb4a06f4b382840e6d79adda692053dd7`
Package: PDR-LIVE8 Controlled Provider Live Smoke Evidence Run
Status: CONTROLLED EVIDENCE RECORDED
Production deployment readiness: BLOCKED

## Scope

This package records controlled provider live-smoke evidence after the PostgreSQL clean restore evidence package merged. It is not production deployment, does not place orders, does not send external Push, does not access production DB, does not print or commit secrets, and does not claim production readiness.

## Provider Commands Inspected

- `scripts/prod-provider-smoke.sh`
- `.env.example` provider smoke flags
- `docs/PROVIDER_LIVE_SMOKE_EVIDENCE.md`
- `docs/PRODUCTION_READINESS_RUNBOOK.md`

The provider smoke harness remains opt-in. It does not call live providers unless `PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=true` and the matching per-provider flag are supplied.

## Commands Run

Default no-call safety smoke:

```bash
PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=false \
PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED=false \
PROVIDER_SMOKE_OPENAI_ENABLED=false \
PROVIDER_SMOKE_GEMINI_ENABLED=false \
PROVIDER_SMOKE_XAI_ENABLED=false \
python3 -c '<bounded 300-second subprocess wrapper>'
```

Output summary:

```text
PROVIDER_LIVE_SMOKE: SKIPPED - set PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=true and per-provider flags to run live checks
BINANCE_PUBLIC_SMOKE: SKIPPED
OPENAI_SMOKE: SKIPPED
GEMINI_SMOKE: SKIPPED
XAI_SMOKE: SKIPPED
```

Controlled Binance public smoke:

```bash
PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=true \
PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED=true \
PROVIDER_SMOKE_OPENAI_ENABLED=false \
PROVIDER_SMOKE_GEMINI_ENABLED=false \
PROVIDER_SMOKE_XAI_ENABLED=false \
python3 -c '<bounded 300-second subprocess wrapper>'
```

Output summary:

```text
Provider live smoke is explicitly enabled. Secrets are intentionally not printed.
BINANCE_PUBLIC_SMOKE: PASS - public futures time endpoint reachable
OPENAI_SMOKE: SKIPPED - set PROVIDER_SMOKE_OPENAI_ENABLED=true to check OpenAI
GEMINI_SMOKE: SKIPPED - set PROVIDER_SMOKE_GEMINI_ENABLED=true to check Gemini
XAI_SMOKE: SKIPPED - set PROVIDER_SMOKE_XAI_ENABLED=true to check XAI
EXTERNAL_CONTEXT_SMOKE: SKIPPED - no external context keys configured
PROVIDER_LIVE_SMOKE: PASS
```

Environment key presence was checked as boolean-only / redacted status. `OPENAI_API_KEY`, `GEMINI_API_KEY`, `XAI_API_KEY`, `NEWS_API_KEY`, `MACRO_CALENDAR_API_KEY`, and `ETF_FLOW_API_KEY` were not present in this run. No key values were printed.

## Provider Results

| Provider | Result | Evidence / Interpretation |
|---|---|---|
| Binance public market data | PASS | `scripts/prod-provider-smoke.sh` reached the Binance public futures time endpoint with explicit opt-in flags. No API key, trading permission, withdrawal permission, order route, or private account endpoint was used. |
| OpenAI | SKIPPED_MISSING_SECRET | `OPENAI_API_KEY` was not present. The OpenAI live flag remained disabled, so no OpenAI endpoint was called. |
| Gemini | SKIPPED_MISSING_SECRET | `GEMINI_API_KEY` was not present. The Gemini live flag remained disabled, so no Gemini endpoint was called. |
| xAI / Grok | SKIPPED_MISSING_SECRET | `XAI_API_KEY` was not present. The xAI live flag remained disabled, so no xAI endpoint was called. |
| External context / news / macro / ETF flow | SKIPPED_MISSING_SECRET | `NEWS_API_KEY`, `MACRO_CALENDAR_API_KEY`, and `ETF_FLOW_API_KEY` were not present; the current harness has no live external-context call. |

## Redaction Confirmation

- No secret values were printed.
- No `.env` file was created, read into evidence, committed, or displayed.
- Provider response bodies were not recorded.
- The only live PASS evidence recorded is the public Binance endpoint reachability line.

## Safety Confirmation

- No orders were placed.
- No order execution path was invoked.
- No auto-open, auto-close, or auto-reverse behavior was introduced.
- No auto-trading behavior was introduced.
- No external Push was sent.
- No Telegram send was triggered.
- No fake positions were created.
- No fake review records were created.
- No production DB was accessed.
- No destructive DB operation was run.

## Production Readiness Decision

Production readiness remains BLOCKED.

This package improves provider evidence by proving controlled Binance public market data reachability, but it does not complete production readiness. Production deployment cannot proceed because AI/external-context provider evidence remains missing, real secrets/access/HTTPS evidence remains incomplete, real server smoke evidence is incomplete, and a final release owner gate has not approved deployment.

## Remaining Blockers

1. AI provider live smoke is not proven because OpenAI, Gemini, and xAI keys were missing and provider flags were not enabled.
2. External context/news/macro provider live smoke is not proven because keys/configuration were missing and no live external-context call exists in the harness.
3. Secrets manager integration, credential rotation, HTTPS/reverse-proxy evidence, access logging, audit logging, and rate limiting evidence remain incomplete.
4. Real server production smoke and final release-gate evidence remain incomplete.
5. Production deployment still requires an explicit later release-gate package and human approval.

## Next Recommendation

Proceed only to the next explicitly scoped remediation package, recommended as `PDR-LIVE9 Secrets / HTTPS / Access Evidence`, or a similarly scoped package that collects redacted server-side secrets/access/HTTPS evidence without production deployment approval.
