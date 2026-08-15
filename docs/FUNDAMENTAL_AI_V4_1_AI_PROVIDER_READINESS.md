# Fundamental AI v4.1 AI Provider Readiness

Status: `IMPLEMENTED_PENDING_INDEPENDENT_AUDIT`

## Exact Model Contract

| Provider | Frozen exact model |
|---|---|
| OpenAI | `gpt-5.6-sol` |
| Gemini | `gemini-3.5-flash` |
| xAI | `grok-4.5` |

Fallback output never authorizes an exact model. Status reads are cache-only;
they do not create paid calls. An authenticated operator explicitly invokes
`POST /api/ai/providers/{provider}/reverify` to refresh the bounded cache.

## Configuration Presence

RPM, input cost, output cost, daily budget and per-analysis budget distinguish
`MISSING`, `EXPLICIT_ZERO`, and `POSITIVE_VALUE`. The standard configuration no
longer converts a missing environment variable to zero. For these configured
paid providers, only positive RPM, cost and budget values satisfy readiness.

## Canonical States

`DISABLED`, `KEY_MISSING`, `COST_NOT_CONFIGURED`, `RPM_NOT_CONFIGURED`,
`BUDGET_NOT_CONFIGURED`, `MODEL_NOT_VERIFIED`, `AUTHORIZED`, `RATE_LIMITED`,
`BUDGET_BLOCKED`, `AUTH_FAILED`, `MODEL_UNAVAILABLE`, and
`PROVIDER_UNAVAILABLE`.

Each runtime state includes provider, exact model, verified/expires times,
sanitized reason/request id and a configuration hash version. It excludes API
keys, authorization headers, base URL secrets and full prompts.

AI provider outage does not fail application liveness. It does prevent the
Three-AI path from claiming role success. Existing rule fallback remains
traceable and cannot appear as exact-model Ready.

## Preflight

`scripts/target-runtime-preflight.sh` validates configuration presence,
PostgreSQL, bootstrap password policy, provider enablement and exact configured
models without printing values. Optional `--probe` uses an already-authenticated
Session and CSRF header to invoke the explicit exact-model reverify endpoints.
A probe success is provider readiness evidence, not full business-chain
acceptance.
