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

Generate a bootstrap password with
`bash scripts/generate-runtime-password.sh --env-file <new-private-path>`.
The tool generates exactly 8 characters, never prints the value, refuses
overwrite and creates the file with mode 0600. Validate the full target
configuration with `bash scripts/target-runtime-preflight.sh`.

## Provider And AI Enablement

These switches fail closed when disabled or when their matching secret is
missing:

- `TRADE_MODEL_PROVIDER_CALL_ENABLED`
- `TRADE_MODEL_PROVIDER_EXTERNAL_CALLS_ENABLED`
- `TRADE_MODEL_PUBLIC_OHLCV_PROVIDER_ENABLED`
- `TRADE_MODEL_PUBLIC_OHLCV_EXTERNAL_CALLS_ENABLED`
- `TRADE_MODEL_KRAKEN_OHLCV_ENABLED`
- `TRADE_MODEL_KRAKEN_OHLCV_EXTERNAL_CALLS_ENABLED`
- `TRADE_MODEL_KRAKEN_OHLCV_BASE_URL`
- `TRADE_MODEL_BINANCE_OHLCV_ENABLED`
- `TRADE_MODEL_BINANCE_OHLCV_EXTERNAL_CALLS_ENABLED`
- `TRADE_MODEL_OHLCV_PRIMARY_PROVIDER`, `TRADE_MODEL_OHLCV_FALLBACK_PROVIDER`,
  and `TRADE_MODEL_OHLCV_FALLBACK_ENABLED`
- `TRADE_MODEL_AI_ENABLED`
- `TRADE_MODEL_AI_OPENAI_ENABLED` and `OPENAI_API_KEY`
- `TRADE_MODEL_AI_GEMINI_ENABLED` and `GEMINI_API_KEY`
- `TRADE_MODEL_AI_XAI_ENABLED` and `XAI_API_KEY`
- optional Coinglass: `TRADE_MODEL_COINGLASS_ENABLED`,
  `TRADE_MODEL_COINGLASS_EXTERNAL_CALLS_ENABLED`, `COINGLASS_API_KEY`,
  `COINGLASS_ADVERTISED_RPM`, `COINGLASS_API_BASE_URL`,
  `COINGLASS_API_AUTH_HEADER` (must be `CG-API-KEY`)
- optional context providers: `NEWS_API_KEY`, `MACRO_CALENDAR_API_KEY`,
  `ETF_FLOW_API_KEY`

Provider base URLs, models, timeouts, request limits and budgets have explicit
`TRADE_MODEL_*` configuration keys in `application.yml`. Defaults do not make
an unconfigured provider ready.

AI readiness additionally requires positive daily/per-analysis budgets and,
for each enabled provider, positive RPM and input/output cost configuration.
Missing and explicit zero are distinct fail-closed states. Frozen exact models
are `gpt-5.6-sol`, `gemini-3.5-flash`, and `grok-4.5`; a fallback model never
counts as Ready. `TRADE_MODEL_AI_READINESS_VERIFICATION_TTL_SECONDS` controls
the cached explicit verification window.

## Telegram High-Value Alert Channel

Telegram defaults fail closed. Both `TRADE_MODEL_TELEGRAM_ENABLED` and
`TRADE_MODEL_TELEGRAM_EXTERNAL_CALLS_ENABLED` must be true before external
delivery can be attempted. `TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_ID` are
required runtime secrets and must never be persisted, returned, or printed.

Provider and link configuration:

- `TELEGRAM_API_BASE_URL`
- `TRADE_MODEL_PUBLIC_BASE_URL`

Durable delivery controls:

- `TRADE_MODEL_TELEGRAM_CONNECT_TIMEOUT_MS`
- `TRADE_MODEL_TELEGRAM_READ_TIMEOUT_MS`
- `TRADE_MODEL_TELEGRAM_MAX_ATTEMPTS`
- `TRADE_MODEL_TELEGRAM_RETRY_BASE_SECONDS`
- `TRADE_MODEL_TELEGRAM_RETRY_MAX_SECONDS`
- `TRADE_MODEL_TELEGRAM_DELIVERY_BATCH_SIZE`
- `TRADE_MODEL_TELEGRAM_DISPATCH_ENABLED`
- `TRADE_MODEL_TELEGRAM_DISPATCH_FIXED_DELAY_MS`
- `TRADE_MODEL_TELEGRAM_CLAIM_LEASE_SECONDS`
- `TRADE_MODEL_TELEGRAM_COOLDOWN_MINUTES`
- `TRADE_MODEL_TELEGRAM_ALLOW_HIGH_QUALITY_REDUCED`

The public base URL is optional. It must be public HTTPS before a recheck or
position link is emitted; otherwise Telegram remains text-only. The dispatcher
also requires the global scheduler gate. Missing configuration cannot produce
`SENT` and does not prevent canonical Message persistence.

The operator-managed private Telegram environment file stays outside the
repository. Preflight checks presence only, and an explicit provider probe is
allowed only after separate operator authorization.

## CoinGlass Configuration Presence

`COINGLASS_ADVERTISED_RPM` has no production default. It must be an explicit
positive integer when CoinGlass and its external calls are enabled. Any
positive provider-plan value is accepted by the local budget contract; `80`
and `300` are tested examples, not implicit defaults.

| Configuration | Runtime state |
|---|---|
| provider disabled or external calls disabled | `NOT_CONFIGURED` |
| enabled + external + key missing | `KEY_MISSING` |
| key present + RPM missing | `RPM_NOT_CONFIGURED` |
| RPM zero or negative | `INVALID_RPM` |
| key present + explicit positive RPM | `CONFIGURED` (other gates still apply) |

An enabled external CoinGlass configuration with missing/invalid RPM blocks
target-runtime preflight. A disabled CoinGlass provider does not block the
overall preflight, but its own state remains `NOT_CONFIGURED`. Missing config
never becomes zero-valued OI, funding, liquidation, or long/short evidence.

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
