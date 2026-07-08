# Controlled AI Provider Smoke Evidence Run

Branch: `codex/pdr-live9-ai-provider-smoke-evidence-run`
Base main commit: `5949c23e102d21b031c71888494a3a32cb300993`
Package: PDR-LIVE9 Controlled AI Provider Smoke Evidence Run
Status: CONTROLLED EVIDENCE RECORDED
Production deployment readiness: BLOCKED

## Scope

This package records controlled AI provider smoke evidence after PDR-LIVE8 proved Binance public market data reachability. It is not production deployment, does not place orders, does not send external Push, does not print or commit secrets, and does not claim production readiness.

## Commands Inspected

- `scripts/prod-provider-smoke.sh`
- `src/main/resources/application.yml`
- `src/main/resources/application-prod.yml`
- `.env.example`
- `docs/CONTROLLED_PROVIDER_LIVE_SMOKE_EVIDENCE_RUN.md`
- `docs/PROVIDER_LIVE_SMOKE_EVIDENCE.md`
- `docs/PRODUCTION_READINESS_RUNBOOK.md`

The existing provider smoke harness supports OpenAI, Gemini, and xAI/Grok checks only when live external calls are explicitly enabled, the provider-specific smoke flag is enabled, and the corresponding key is present in environment. Key values are never printed by the harness.

## Environment Presence Check

Environment key presence was checked as boolean-only redacted status:

```text
OPENAI_API_KEY: MISSING
GEMINI_API_KEY: MISSING
XAI_API_KEY: MISSING
```

No environment values were printed.

## Commands Run

Controlled AI provider smoke was run with AI provider flags enabled and Binance disabled, using a bounded 300-second wrapper:

```bash
PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=true \
PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED=false \
PROVIDER_SMOKE_OPENAI_ENABLED=true \
PROVIDER_SMOKE_GEMINI_ENABLED=true \
PROVIDER_SMOKE_XAI_ENABLED=true \
python3 -c '<bounded 300-second subprocess wrapper>'
```

Output summary:

```text
Provider live smoke is explicitly enabled. Secrets are intentionally not printed.
BINANCE_PUBLIC_SMOKE: SKIPPED - set PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED=true to check Binance public market data
OPENAI_SMOKE: NOT_CONFIGURED - OPENAI_API_KEY missing
GEMINI_SMOKE: NOT_CONFIGURED - GEMINI_API_KEY missing
XAI_SMOKE: NOT_CONFIGURED - XAI_API_KEY missing
EXTERNAL_CONTEXT_SMOKE: SKIPPED - no external context keys configured
PROVIDER_LIVE_SMOKE: INCOMPLETE
```

For this evidence package, script-level `NOT_CONFIGURED` due a missing key is recorded as `SKIPPED_MISSING_SECRET`. This is a fail-closed / no-call result, not a provider failure.

## Provider Results

| Provider | Result | Evidence / Interpretation |
|---|---|---|
| OpenAI | SKIPPED_MISSING_SECRET | `OPENAI_API_KEY` was missing. The smoke harness did not call OpenAI and returned `OPENAI_SMOKE: NOT_CONFIGURED`. |
| Gemini | SKIPPED_MISSING_SECRET | `GEMINI_API_KEY` was missing. The smoke harness did not call Gemini and returned `GEMINI_SMOKE: NOT_CONFIGURED`. |
| xAI / Grok | SKIPPED_MISSING_SECRET | `XAI_API_KEY` was missing. The smoke harness did not call xAI and returned `XAI_SMOKE: NOT_CONFIGURED`. |

## Redaction Confirmation

- No secret values were printed.
- No `.env` file was created, read into evidence, committed, or displayed.
- Provider response bodies were not recorded.
- No AI provider endpoint was called because all AI keys were missing.

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

This package records exact AI provider skipped reasons. It does not complete production readiness because OpenAI, Gemini, and xAI/Grok live proof remains missing until controlled server-side secrets are available and smoke is explicitly approved. Production deployment cannot proceed.

## Remaining Blockers

1. OpenAI live provider smoke is not proven because `OPENAI_API_KEY` was missing.
2. Gemini live provider smoke is not proven because `GEMINI_API_KEY` was missing.
3. xAI/Grok live provider smoke is not proven because `XAI_API_KEY` was missing.
4. External context/news/macro provider live smoke remains unproven because keys/configuration are missing and no live external-context harness exists.
5. Secrets manager integration, credential rotation, HTTPS/reverse-proxy evidence, access logging, audit logging, and rate limiting evidence remain incomplete.
6. Real server production smoke and final release-gate evidence remain incomplete.

## Next Recommendation

Proceed only to the next explicitly scoped remediation package, recommended as `PDR-LIVE10 Secrets / HTTPS / Access Evidence`, or a controlled AI provider rerun after server-side secrets are available through approved secret handling. The next package must not be production deployment.
