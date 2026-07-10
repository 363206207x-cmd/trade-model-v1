# Provider Call Orchestration Contract

## Scope and Safety

This package adds the bounded coordination foundation for shared provider reads. It does not enable a live provider, connect CoinGlass, call an AI/news provider, send Push/Telegram, create a position, mutate a position, create an order, or execute a trade. All new runtime switches are disabled by default and production readiness remains `BLOCKED`.

## Previous Call-Path Audit

Before this package, provider-facing reads were owned independently by several paths:

- Dashboard Home, Decision summary, Position Monitor, Push Recheck, and its scheduler read `MarketQuoteClient` independently.
- the controlled OHLCV scheduler called `PublicOhlcvProvider` and then the sole `PersistedOhlcvIngestionService` writer;
- market environment reads funding/open interest independently;
- AI providers already had separate review-only controls but no shared provider-request key or shared snapshot cache.

The package introduces the target path:

```text
Business Service
  -> Snapshot Service
  -> ProviderCallCoordinator
  -> Provider Adapter
```

`MarketPriceSnapshotService` and `CoordinatedOhlcvSnapshotService` implement the new path. Existing business consumers are not all migrated in this package, so repository-wide single-entry adoption remains `PARTIAL`. The architecture guard prevents business services from importing concrete Binance/CoinGlass/News adapter implementations, and the next wiring package must replace remaining direct quote-reader calls with snapshot reads.

## Request Identity

`ProviderRequestKey` contains:

- provider;
- dataset type;
- symbol;
- timeframe;
- time bucket.

Its canonical form is `PROVIDER|DATASET|SYMBOL|TIMEFRAME|BUCKET`. Dataset types are intentionally separate: `PRICE`, `OHLCV`, `DERIVATIVES`, `EXTERNAL_CONTEXT`, and `AI_REVIEW`.

## Cache and Freshness

`SnapshotCacheService` stores a fresh deadline and a separate stale-readable deadline per request key. Lookups are typed as:

- `FRESH`;
- `STALE`;
- `UNAVAILABLE`;
- `ERROR`;
- `REFRESH_IN_PROGRESS`.

Stale fallback is never returned as healthy: metadata changes to source status `STALE`, freshness `STALE`, `cacheHit=true`, and `fallbackUsed=true`. Missing/failed provider data cannot become low risk or a confirmed empty response.

Configured cadence provides independent TTL ownership for position/core/candidate/pool price, derivatives, OHLCV references, external context, and later AI review snapshots. A price refresh does not refresh derivatives.

## Single Flight

`ProviderSingleFlightGuard` allows one owner for an identical `ProviderRequestKey`. Concurrent waiters receive the same completed result. The concurrency test uses latches and a waiter counter, not timing sleeps, and proves one adapter invocation.

## Provider Budget

`ProviderRateBudgetManager` maintains an independent fixed-minute budget per provider:

- advertised RPM;
- effective RPM (`advertised * internalBudgetRatio`);
- emergency reserve;
- current usage and remaining budget;
- Retry-After;
- circuit state;
- last rejected priority.

Defaults are `internalBudgetRatio=0.80` and `emergencyReserveRatio=0.20`. CoinGlass has a contract-only advertised default of 300 RPM; no CoinGlass endpoint or key is present. Lower priorities are rejected earlier: pool, candidate, core, then position. The P0 position tier can use the emergency reserve, and a depleted derivatives-provider budget does not consume the independent Binance price budget.

## Retry and Circuit Policy

The coordinator enforces:

- `401/403`: fail closed, no retry;
- `429`: record Retry-After and return without a busy loop;
- `5xx`: at most two retries after the initial attempt;
- timeout: at most one retry;
- malformed/null response: contract error, no blind retry.

`ProviderCircuitBreaker` supports `CLOSED`, `OPEN`, and bounded `HALF_OPEN` probing. Provider errors produce explicit fail-closed metadata and may use a stale-readable snapshot when available.

## Unified Source Status

The only provider source states in the new contract are:

- `NOT_CONFIGURED`
- `WAITING_SYNC`
- `READY`
- `EMPTY_CONFIRMED`
- `STALE`
- `DEGRADED`
- `ERROR`
- `DISABLED`

Configuration presence is not connection proof. Missing derivatives/news data remains unavailable; no `0`, `LOW`, `NORMAL`, or `HEALTHY` value is synthesized.

## Snapshot Contracts

The package defines:

- `MarketPriceSnapshot`;
- `OhlcvSnapshotReference`;
- `DerivativesRiskSnapshot`;
- `ExternalContextSnapshot`;
- `ProviderSnapshotMetadata`;
- `AnalysisInputBundle`.

Metadata includes provider, dataset, symbol/timeframe, provider data time, fetch/expiry times, source/freshness states, trace ID, canonical request key, cache/fallback flags, error code, and reason codes. Derivatives fields are nullable by design because no real CoinGlass source is connected.

`AnalysisInputBundleAssembler` requires authoritative `5m`, `15m`, `1h`, and `4h` references and one trace ID across OHLCV, price, derivatives, and external context. It rejects mixed-trace inputs. Evidence, scores, decisions, and plans can therefore consume one locked snapshot set in the later business-wiring package.

## Asset Priority and Bounded Universe

Priorities are:

1. `P0_POSITION`
2. `P1_CORE`
3. `P2_CANDIDATE`
4. `P3_POOL`

Duplicate symbols collapse to their highest priority. The resolver caps core assets at 6, candidates at 20, and the pool at 20. Only `OPEN` and `PARTIALLY_CLOSED` manual positions receive P0. A `CLOSED` position immediately loses the position floor.

## Manual Profiles and Position Safety Floor

Authenticated `GET/PUT /api/config/scan-profile` reads and updates:

- base profile: `AUTO`, `LOW`, `STANDARD`, `HIGH`;
- position monitor profile;
- pool profile;
- auto-escalation flag;
- bounded manual override time;
- update reason.

The settings reuse `tm_user_config`; profile changes are audited through `tm_rule_version_log`. Position/pool subprofiles cannot be `AUTO`. The response exposes effective profile/reason, cadence, and provider budget state.

The matrix is configuration-bound. Position price intervals are 15s/10s/5s for LOW/STANDARD/HIGH and 3s for an affected EMERGENCY symbol. Derivatives retain their independent 120s/60s/60s cadence, with an emergency minimum refresh gap of 40s. Unaffected pool symbols are not elevated to EMERGENCY.

## Automatic Escalation and Hysteresis

`ScanProfileTransitionService` reads every trigger and hysteresis value from versioned `tm_rule_config`. Inputs cover price/ATR/volume/spread, stop/target distance, open interest, liquidations, funding, events, confused score, Hot Reset, reversal, and data quality.

Missing or malformed rule config keeps the current profile and returns `PROFILE_RULE_CONFIG_UNAVAILABLE`. It does not silently use a permissive Java threshold.

Transitions record symbol, previous/new profile, reason/value, rule version, effective time, next downgrade time, and trace ID in `tm_rule_version_log`. Downgrades require configured recovery cycles and cooldown, and move one level at a time. A manual HIGH profile is a floor and cannot be automatically downgraded.

## AI Invocation Policy

`AiInvocationPolicy` is a decision-only contract. It never calls a provider. Routine scans and profile changes return `SKIP_NOT_TRIGGERED`. A supported checkpoint must also pass data quality, evidence hash, budget, rate-limit, and provider availability gates. Results are restricted to the requested `RUN_*` and `SKIP_*` statuses.

## OHLCV Writer Reuse

`CoordinatedOhlcvSnapshotService` calls:

```text
ProviderCallCoordinator
  -> PublicOhlcvProvider
  -> PersistedOhlcvIngestionService
```

`PersistedOhlcvIngestionService` remains the sole authoritative writer to `tm_persisted_ohlcv_bar`. No second mapper/writer was added. The existing controlled 1-2 symbol scheduler and its four-timeframe allowlist remain unchanged and semantically separate from the new provider-scan scheduler.

## Production Defaults

The following production switches default to `false`:

- `trade-model.provider-call.enabled`;
- `trade-model.provider-call.scheduler-enabled`;
- `trade-model.provider-call.profile-escalation-enabled`;
- `trade-model.provider-call.external-calls-enabled`.

`ProductionProfileSafetyGuard` requires coordinator enablement before any child feature and explicit external-call opt-in before the provider-scan scheduler. Position Monitor remains default-off. The scheduler has bounded universe, due-dataset, priority, and overlap guards, and has no direction, plan, position, order, Push, or Telegram surface.

## Persistence and Migration Evidence

`V5__provider_scan_profile_orchestration.sql` adds only user scan-profile settings and provider scan rule defaults. It does not change trading data or add an executable action. Local H2 bootstrap and PostgreSQL migration SQL stay aligned.

Historical controlled PostgreSQL evidence covers V1-V3, with static evidence for V4. V5 has not been rerun against a controlled PostgreSQL instance in this package. This is an explicit production-readiness blocker, not a PASS claim.

## Test Evidence

Focused tests cover bounded assets, priority deduplication, position cadence, closed-position removal, per-dataset cadence, shared cache, deterministic single flight, priority budget/reserve, Retry-After, stale/error metadata, transition thresholds/hysteresis/audit, authenticated profile update, consistent analysis bundles, AI skip/run policy, authoritative OHLCV writer reuse, architecture guards, production defaults, and no trading/external-send surfaces.

The full Maven suite passes with no live provider calls. Production readiness remains `BLOCKED`.
