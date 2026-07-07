# Controlled Live Dependency Acceptance

Package: PDR-LIVE1 Controlled Live Dependency Acceptance
Branch: `codex/pdr-live1-controlled-live-dependency-acceptance`
Current main commit: `7ba8a55d35af09f2562e30be72d374f1c0f1def0`
Evidence date: 2026-07-07

## Scope

This package records controlled live dependency acceptance evidence only. It is not production deployment, not public release, and not auto-trading.

No production database was accessed. No destructive database operation was run. No secrets were committed or printed.

## Environment Type Used

Local-controlled Codex environment created from `main` at `7ba8a55d35af09f2562e30be72d374f1c0f1def0`.

Available environment evidence:

- Controlled PostgreSQL URL/username/password variables: missing.
- Provider external-call opt-in: disabled.
- Binance public smoke opt-in: disabled.
- OpenAI, Gemini, and xAI key presence: missing.
- External context/news/macro provider key presence: missing.

Only presence checks were performed. Secret values were not printed.

## Commands Run

All commands were bounded to five minutes or less.

| Purpose | Command | Result |
|---|---|---|
| Main commit / branch evidence | `git rev-parse main`; `git branch --show-current` | PASS |
| Environment presence check | Presence-only check for controlled DB and provider env names; values redacted/not printed | PASS |
| Provider smoke harness default mode | `bash scripts/prod-provider-smoke.sh` via Python subprocess timeout=300 | PASS command exit; all live checks SKIPPED because external calls were not explicitly enabled |
| Production safety / scheduler / Push Recheck guard tests | `./mvnw -q -Dtest=ProductionProfileSafetyGuardTest,PositionMonitorSchedulerTest,PushRecheckServiceImplTest test` via Python subprocess timeout=300 | PASS in approximately 5.2s |

No Docker/Testcontainers/PostgreSQL smoke was run because no controlled PostgreSQL URL was provided and local Docker/Testcontainers had already been confirmed unavailable by PF9/PF10/PF11.

## Dependency Results

| Dependency / Gate | Result | Evidence |
|---|---|---|
| Controlled PostgreSQL DB | SKIPPED_MISSING_CONTROLLED_DB | No disposable non-production PostgreSQL URL was present in environment. No production DB was accessed. |
| Binance public market data smoke | SKIPPED_DISABLED | `PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS` and `PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED` were not enabled. Default smoke exited successfully without live calls. |
| OpenAI provider smoke | SKIPPED_MISSING_SECRET | No OpenAI key was present. No key value was printed and no live call was made. |
| Gemini provider smoke | SKIPPED_MISSING_SECRET | No Gemini key was present. No key value was printed and no live call was made. |
| xAI provider smoke | SKIPPED_MISSING_SECRET | No xAI key was present. No key value was printed and no live call was made. |
| External context/news/macro provider smoke | SKIPPED_MISSING_SECRET | News/macro/external context keys were missing and no explicit opt-in live smoke was configured for this run. |
| application-prod profile safety | PASS | Focused safety tests passed. Production profile remains fail-closed for unsafe config. |
| Scheduler policy | PASS | Focused scheduler tests passed. Production scheduler policy remains fail-closed and Position Monitor scheduler remains default-off. |
| Push Recheck quote-unavailable behavior | PASS | Focused Push Recheck tests passed. Quote-unavailable paths remain fail-closed/review-only. |

## Secret Redaction Confirmation

PASS. The run checked only whether relevant environment variables were present. It did not print, copy, or commit secret values. No `.env` file was created or committed.

## Safety Confirmation

PASS for this package.

- no auto-open
- no auto-close
- no auto-reverse
- no order execution
- no auto-trading
- no external push send
- no fake positions
- no fake review records

No Java runtime behavior, schema, migration SQL, dashboard template, order/execution behavior, Push send behavior, or Telegram behavior was changed.

## Production Readiness Decision

Production readiness: BLOCKED.

Production deployment cannot proceed.

Reason: controlled live dependency acceptance did not produce PASS evidence for every release gate. The controlled PostgreSQL path was skipped because no non-production database URL was available; provider live smoke remained disabled or missing secrets; and full production release-gate evidence remains incomplete.

## Exact Remaining Blockers

1. No controlled non-production PostgreSQL URL was available, so Flyway V1/V2/V3 success against PostgreSQL is still unproven in this package.
2. Binance public market data live smoke was not run because external calls were not explicitly enabled.
3. AI provider live smoke was not run because provider keys were absent.
4. External context/news/macro live smoke was not run because provider configuration/secrets were absent.
5. Current-state migration and rollback drill still require execution in a safe non-production or production-like environment.
6. Secrets manager, credential rotation, HTTPS/reverse-proxy, access logging, audit logging, and rate limiting still require real environment evidence.
7. A complete production release-gate evidence bundle is still missing.

## Next Recommended Package

`PDR-LIVE2 Controlled Non-Production Dependency Evidence Run`.

Recommended preconditions for PDR-LIVE2:

1. Provide a disposable non-production PostgreSQL URL through local/server environment variables, not chat.
2. Explicitly confirm the database is not production and can be used for bounded migration smoke.
3. Enable provider live smoke only with explicit opt-in env flags.
4. Keep all provider output redacted.
5. Keep every command bounded to five minutes or less.
6. Continue to block production deployment until all production release gates have PASS evidence.
