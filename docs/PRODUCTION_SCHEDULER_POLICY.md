# Production Scheduler Policy

Status: PDR-PF2 current package.
Production deployment readiness: BLOCKED.

This policy makes production scheduler activation explicit and fail-closed. It does not add trading runtime behavior, order execution, external push send, auto-open, auto-close, or auto-reverse capability.

## Policy Modes

`trade-model.production.scheduler-policy` is required in the `prod` profile.

| Mode | Meaning | Deployment impact |
|---|---|---|
| `LOCKED_DOWN` | All production schedulers must be disabled. | Safest baseline. Startup fails if the global scheduler switch or any effective scheduler is enabled. |
| `EXPLICIT_OPT_IN` | A scheduler may run only when its own production classification explicitly allows opt-in and the matching scheduler flag is enabled. | Startup fails for missing classifications, blocked/local-only schedulers, or enabled schedulers without explicit opt-in classification. |

Missing or unsupported policy values fail closed during `ProductionProfileSafetyGuard` startup validation.

## Scheduler Classification Contract

Every production scheduler must declare one classification through `trade-model.production.scheduler-approval.*`.

| Classification | Meaning |
|---|---|
| `PROD_ALLOWED_DEFAULT_OFF` | The scheduler is recognized in production but must remain disabled by default. |
| `PROD_ALLOWED_EXPLICIT_OPT_IN` | The scheduler may run only when the production policy is `EXPLICIT_OPT_IN` and its scheduler flag is explicitly enabled. |
| `PROD_BLOCKED` | The scheduler is not allowed to run in production. |
| `LOCAL_ONLY` | The scheduler is local/test-only and must not run in production. |

## Current Scheduler Policy Matrix

| Scheduler | Runtime surface | Production classification | Production default | Notes |
|---|---|---|---|---|
| Push Recheck scheduler | Reads pending push snapshots, reads quotes, writes recheck review state/logs. | `PROD_ALLOWED_EXPLICIT_OPT_IN` | Off | Does not send external push messages. Must stay review-only/fail-closed. |
| Position Sync scheduler | Calls provider-backed position sync path. | `PROD_ALLOWED_EXPLICIT_OPT_IN` | Off | Requires explicit production approval and provider evidence. Must not create fake positions. |
| Market Data scheduler | Triggers scheduled analysis cycle through market-data scheduler wrapper. | `PROD_ALLOWED_EXPLICIT_OPT_IN` | Off | May call provider/readiness paths and write analysis records. Requires explicit opt-in. |
| Watchlist scheduler | Low-frequency watchlist scan skeleton. | `LOCAL_ONLY` | Off | Remains disabled in production unless a future package changes the contract. |
| Position Monitor scheduler | Batch scans open manual user positions and writes monitor logs. | `PROD_ALLOWED_DEFAULT_OFF` | Off | PDR-PF2 guard rejects production enablement so it remains default-off. No auto-close or order action. |
| Analysis scheduler | Analysis scheduler service execution. | `PROD_ALLOWED_EXPLICIT_OPT_IN` | Off | May write analysis/decision artifacts. Requires explicit opt-in and provider budget/readiness evidence. |

## Production Configuration Keys

`application-prod.yml` exposes safe default-off scheduler flags and blank required policy/classification fields:

- `TRADE_MODEL_PRODUCTION_SCHEDULER_POLICY`
- `TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_PUSH_RECHECK`
- `TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_POSITION_SYNC`
- `TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_MARKET_DATA`
- `TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_WATCHLIST`
- `TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_POSITION_MONITOR`
- `TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_ANALYSIS`
- `TRADE_MODEL_SCHEDULERS_ENABLED`
- `TRADE_MODEL_PUSH_RECHECK_SCHEDULER_ENABLED`
- `TRADE_MODEL_POSITION_SYNC_SCHEDULER_ENABLED`
- `TRADE_MODEL_POSITION_MONITOR_SCHEDULER_ENABLED`
- `TRADE_MODEL_MARKET_DATA_SCHEDULER_ENABLED`
- `TRADE_MODEL_WATCHLIST_SCHEDULER_ENABLED`
- `TRADE_MODEL_ANALYSIS_SCHEDULER_ENABLED`

Because policy and classifications default to blank, production startup fails closed until the operator declares them explicitly.

## Safety Boundaries

This package does not add or approve:

- auto-open
- auto-close
- auto-reverse
- order execution
- auto-trading
- external push send
- fake positions
- fake review records
- production-ready claim

Scheduler opt-in never implies trading execution. Production deployment remains BLOCKED until the separate production release gate proves every required production gate.

## Next Remediation Package

After PDR-PF2 is merged/effective, the next recommended scoped remediation package is `PDR-PF3 PostgreSQL Migration Evidence`.
