# Fundamental AI v4.1 Telegram Configuration

Status: `IMPLEMENTATION_CANDIDATE_PENDING_INDEPENDENT_AUDIT`

All secret values are operator-owned runtime inputs. This document records
names and behavior only.

## Variables

| Variable | Default | Contract |
|---|---:|---|
| `TRADE_MODEL_TELEGRAM_ENABLED` | `false` | Master channel enablement |
| `TRADE_MODEL_TELEGRAM_EXTERNAL_CALLS_ENABLED` | `false` | Explicit external-call gate |
| `TELEGRAM_BOT_TOKEN` | empty | Secret; never persisted, logged, returned, or committed |
| `TELEGRAM_CHAT_ID` | empty | Secret recipient identity; only a one-way fingerprint may persist |
| `TELEGRAM_API_BASE_URL` | official HTTPS API base | Telegram provider base URL |
| `TRADE_MODEL_PUBLIC_BASE_URL` | empty | Optional public HTTPS application base for safe links |
| `TRADE_MODEL_TELEGRAM_CONNECT_TIMEOUT_MS` | `3000` | Positive connection timeout |
| `TRADE_MODEL_TELEGRAM_READ_TIMEOUT_MS` | `5000` | Positive read timeout |
| `TRADE_MODEL_TELEGRAM_MAX_ATTEMPTS` | `5` | Bounded delivery attempts |
| `TRADE_MODEL_TELEGRAM_RETRY_BASE_SECONDS` | `5` | Exponential retry base |
| `TRADE_MODEL_TELEGRAM_RETRY_MAX_SECONDS` | `300` | Retry-delay ceiling |
| `TRADE_MODEL_TELEGRAM_DELIVERY_BATCH_SIZE` | `20` | Due-delivery claim batch |
| `TRADE_MODEL_TELEGRAM_DISPATCH_ENABLED` | `false` | Dispatcher gate |
| `TRADE_MODEL_TELEGRAM_DISPATCH_FIXED_DELAY_MS` | `5000` | Scheduled dispatch interval |
| `TRADE_MODEL_TELEGRAM_CLAIM_LEASE_SECONDS` | `60` | Crash-recovery lease |
| `TRADE_MODEL_TELEGRAM_COOLDOWN_MINUTES` | `15` | Central event cooldown |
| `TRADE_MODEL_TELEGRAM_ALLOW_HIGH_QUALITY_REDUCED` | `false` | Explicit opt-in for otherwise qualified REDUCED plans |

The global scheduler gate must also permit scheduled work. Channel defaults
are fail closed: Telegram delivery and dispatch are off unless explicitly
enabled.

## Readiness States

| Condition | State |
|---|---|
| channel or external calls disabled | `NOT_CONFIGURED` |
| token absent | `TOKEN_MISSING` |
| chat ID absent | `CHAT_ID_MISSING` |
| complete configuration before a successful provider observation | `DEGRADED` |
| successful sanitized provider observation | `READY` |
| provider authentication rejected | `AUTH_FAILED` |
| recipient unavailable | `CHAT_UNAVAILABLE` |
| provider rate-limited | `RATE_LIMITED` |
| network/provider unavailable | `PROVIDER_UNAVAILABLE` |
| other recoverable degradation | `DEGRADED` |

Telegram readiness does not control application liveness. When the channel is
not ready, Message persistence continues and delivery records truthfully use
`NOT_CONFIGURED`, `RETRYING`, or `FAILED`; they never synthesize `SENT`.

## Preflight

`bash scripts/target-runtime-preflight.sh` reports presence and normalized
state only. It never prints values. An explicit `--telegram-probe` performs a
provider `getMe` check without creating a business Message, Opportunity,
Candidate, Final plan, or UserPosition. The probe requires separate operator
authorization and was not run during implementation.

The operator-managed private environment file remains outside the repository.
Its contents were not read, copied, hashed, printed, or used by this task.
