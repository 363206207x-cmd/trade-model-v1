# Fundamental AI v4.1 Environment And Secret Contract

Secrets are names only in this document. Values belong in the host secret
store and must never enter Git, frontend code, logs, screenshots or reports.

## Required Production Runtime

| Area | Variables | Rule |
|---|---|---|
| PostgreSQL | `PROD_DATASOURCE_URL`, `PROD_DATASOURCE_USERNAME`, `PROD_DATASOURCE_PASSWORD` | Required; no H2 production fallback |
| Login | `TRADE_MODEL_INITIAL_USERNAME`, `TRADE_MODEL_INITIAL_PASSWORD` | Required when auth is enabled |
| Bind/TLS | `SERVER_ADDRESS`, `SERVER_PORT`, `TRADE_MODEL_SESSION_COOKIE_SECURE`, `TRADE_MODEL_PRODUCTION_ALLOW_PUBLIC_BIND` | Public mode requires reverse proxy and secure cookie |
| Position provider | `POSITION_PROVIDER_TYPE`, `BINANCE_API_BASE_URL`, `BINANCE_API_KEY`, `BINANCE_API_SECRET` | Keep read/monitor semantics; no order capability is authorized |

## Provider And AI Enablement

These switches fail closed when disabled or when their matching secret is
missing:

- `TRADE_MODEL_PROVIDER_CALL_ENABLED`
- `TRADE_MODEL_PROVIDER_EXTERNAL_CALLS_ENABLED`
- `TRADE_MODEL_PUBLIC_OHLCV_PROVIDER_ENABLED`
- `TRADE_MODEL_PUBLIC_OHLCV_EXTERNAL_CALLS_ENABLED`
- `TRADE_MODEL_AI_ENABLED`
- `TRADE_MODEL_AI_OPENAI_ENABLED` and `OPENAI_API_KEY`
- `TRADE_MODEL_AI_GEMINI_ENABLED` and `GEMINI_API_KEY`
- `TRADE_MODEL_AI_XAI_ENABLED` and `XAI_API_KEY`
- optional Coinglass: `TRADE_MODEL_COINGLASS_ENABLED`,
  `TRADE_MODEL_COINGLASS_EXTERNAL_CALLS_ENABLED`, `COINGLASS_API_KEY`
- optional context providers: `NEWS_API_KEY`, `MACRO_CALENDAR_API_KEY`,
  `ETF_FLOW_API_KEY`

Provider base URLs, models, timeouts, request limits and budgets have explicit
`TRADE_MODEL_*` configuration keys in `application.yml`. Defaults do not make
an unconfigured provider ready.

## Scheduler Controls

Production schedulers default off. Enable only the required scheduler and its
matching production approval variable:

- master: `TRADE_MODEL_SCHEDULERS_ENABLED`
- Push Recheck: `TRADE_MODEL_PUSH_RECHECK_SCHEDULER_ENABLED` and
  `TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_PUSH_RECHECK`
- Position sync: `TRADE_MODEL_POSITION_SYNC_SCHEDULER_ENABLED` and
  `TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_POSITION_SYNC`
- Position monitor: `TRADE_MODEL_POSITION_MONITOR_SCHEDULER_ENABLED` and
  `TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_POSITION_MONITOR`
- market/OHLCV: `TRADE_MODEL_MARKET_DATA_SCHEDULER_ENABLED`,
  `TRADE_MODEL_OHLCV_INGESTION_SCHEDULER_ENABLED`, and their matching approval
  variables
- analysis/provider scan/watchlist: their `*_SCHEDULER_ENABLED` and matching
  `TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_*` variables

`TRADE_MODEL_PRODUCTION_SCHEDULER_POLICY` must identify the approved operating
policy. Push Recheck remains review-only and is never trading authorization.

## Smoke And Backup Variables

Smoke credentials are separate from application bootstrap credentials:
`TRADE_MODEL_SMOKE_USERNAME`, `TRADE_MODEL_SMOKE_PASSWORD`, `APP_URL`, and
optional `TRADE_MODEL_SMOKE_CA_CERT`.

Backup uses `PROD_DATASOURCE_HOST`, `PROD_DATASOURCE_PORT`,
`PROD_DATASOURCE_DATABASE`, `PROD_DATASOURCE_USERNAME`,
`PROD_DATASOURCE_PASSWORD`, `BACKUP_DIR`, and optional `BACKUP_FILE`.

Restore uses the corresponding `RESTORE_DATASOURCE_*`,
`RESTORE_BACKUP_FILE`, and the explicit destructive confirmation
`RESTORE_CONFIRM`.

Controlled migration evidence uses `CONTROLLED_POSTGRESQL_JDBC_URL`,
`CONTROLLED_POSTGRESQL_USERNAME`, `CONTROLLED_POSTGRESQL_PASSWORD` and two
explicit confirmation variables. These are acceptance-only and are not
production application configuration.

## Rotation

1. Create the replacement secret in the provider/host secret store.
2. Update the environment reference without printing the value.
3. Restart the bounded service and run readiness/provider smoke.
4. Revoke the prior secret after the new secret is verified.
5. Review logs and evidence for accidental disclosure; stop the release if any
   token appears.

Missing required configuration must produce `NOT_CONFIGURED`, `WAITING`, or
another fail-closed state. It must never synthesize provider success.
