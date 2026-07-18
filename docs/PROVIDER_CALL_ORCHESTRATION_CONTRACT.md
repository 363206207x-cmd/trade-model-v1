# Provider Call Orchestration Contract

## Scope and Safety

This contract began as the bounded coordination foundation for shared provider reads. CG-1 adds the default-off CoinGlass v4 adapter below that foundation. BIZ-1 consumes only the coordinator-owned cached snapshot for downstream evidence/rule integration; it adds no default live call, UserPosition mutation, order, or trading behavior. Production readiness remains `BLOCKED`.

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

`MarketPriceSnapshotService`, `BinanceDerivativesSnapshotService`, `CoordinatedOhlcvSnapshotService`, and the four CoinGlass dataset snapshot services implement the provider boundary. Dashboard Home and Decision read models use the read-only snapshot peek; Position Monitor and Push Recheck may refresh through the coordinator. Decision Engine and plan assembly use authoritative persisted OHLCV. Repository-wide primary-business single-entry adoption is `PASS`; remaining direct clients are provider adapters or the legacy diagnostic market endpoint only.

## Request Identity

`ProviderRequestKey` contains:

- provider;
- dataset type;
- canonical instrument identity;
- provider-boundary symbol;
- timeframe;
- time bucket;
- mapping/source version.

Its canonical form is `PROVIDER|DATASET|CANONICAL_INSTRUMENT|PROVIDER_SYMBOL|TIMEFRAME|BUCKET|SOURCE_VERSION`. The canonical identity keeps spot and perpetual contracts distinct while normalizing aliases such as `BTCUSDT`, `BTC-USDT`, and `BTC/USDT` through an explicit mapping. Unknown or ambiguous mappings fail closed. Cache lookup applies each consumer's freshness requirement, so a strict monitor cannot accept the looser age of a read model while all consumers still share one stored snapshot. Dataset types are intentionally separate: `PRICE`, `OHLCV`, aggregate `DERIVATIVES`, four `COINGLASS_*` datasets, `EXTERNAL_CONTEXT`, and `AI_REVIEW`.

## Cache and Freshness

`SnapshotCacheService` stores a fresh deadline and a separate stale-readable deadline per request key. Lookups are typed as:

- `FRESH`;
- `STALE_READABLE`;
- `REFRESHING`;
- `UNAVAILABLE`.

Stale fallback is never returned as healthy: metadata changes to source status `STALE`, freshness `STALE`, `cacheHit=true`, and `fallbackUsed=true`. Missing/failed provider data cannot become low risk or a confirmed empty response.

Configured cadence provides independent TTL ownership for position/core/candidate/pool price, derivatives, OHLCV references, external context, and later AI review snapshots. A price refresh does not refresh derivatives.

## Single Flight

`ProviderSingleFlightGuard` allows one owner for an identical `ProviderRequestKey`. Concurrent waiters receive the same completed result. The concurrency test uses latches and a waiter counter, not timing sleeps, and proves one adapter invocation.

## Provider Budget

`ProviderRateBudgetManager` maintains fixed-minute global and per-provider budgets plus a per-symbol minimum gap:

- advertised RPM;
- effective RPM (`advertised * internalBudgetRatio`);
- emergency reserve;
- current usage and remaining budget;
- Retry-After;
- circuit state;
- last rejected priority.

Defaults are `internalBudgetRatio=0.80` and `emergencyReserveRatio=0.20`. CoinGlass has an environment-overridable advertised default of 300 RPM. Four isolated dataset requests share the actual provider/API-key quota and each request increments it; no key is present in the current environment. Lower priorities are rejected earlier: `P3_DISCOVERY`, `P1_WATCHLIST`, `P2_CANDIDATE`, then `P0_POSITION`. Emergency reserve is available only to a system-selected `EMERGENCY` profile; normal Discovery/Watchlist/Candidate calls cannot consume it. A depleted derivatives-provider budget does not consume the independent Binance price budget. Concurrency uses bounded admission with reserved higher-priority slots and explicit rejection rather than unbounded thread creation; AI has its own concurrent-call limit.

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

Metadata includes provider, dataset, symbol/timeframe, provider data time, fetch/expiry times, source/freshness states, trace ID, canonical request key, cache/fallback flags, error code, and reason codes. CG-1 fills source-backed CoinGlass fields and keeps unsupported/missing fields null. Partial success is `DEGRADED`; adapter-level risk scores remain null for BIZ-1 rule ownership.

`AnalysisInputBundleAssembler` requires authoritative `5m`, `15m`, `1h`, and `4h` references and one trace ID across OHLCV, price, derivatives, and external context. It rejects mixed-trace inputs. `AuthoritativePersistedDecisionOhlcvSource` now supplies the four decision timeframes from the persisted readiness boundary; `1m` is no longer used as the formal decision or push-invalidation timeframe.

## Asset Priority and Bounded Universe

Priorities are:

1. `P0_POSITION`
2. `P2_CANDIDATE`
3. `P1_WATCHLIST`
4. `P3_DISCOVERY`

Duplicate canonical instruments collapse to their highest priority. The scan universe is the bounded union of active manual positions, replaceable manual watchlist entries, candidate assets, and a configured discovery universe. There is no permanent six-asset scan contract. Default watchlist/discovery entries are replaceable configuration, with independent caps. Only `OPEN` and `PARTIALLY_CLOSED` manual positions receive P0. A `CLOSED` position immediately loses the position floor.

## Manual Profiles and Position Safety Floor

Authenticated `GET/PUT /api/config/scan-profile` reads and updates:

- base profile: `AUTO`, `LOW`, `STANDARD`, `HIGH`;
- position monitor profile;
- pool profile;
- auto-escalation flag;
- bounded manual override time;
- update reason.

The settings reuse `tm_user_config`; profile changes are audited through `tm_rule_version_log`. Position/pool subprofiles cannot be `AUTO`. The response exposes effective profile/reason, cadence, and provider budget state.

Authenticated `GET /api/config/scan-profile/runtime?symbol=BTCUSDT` exposes the configured/effective profile, priority, reason, transition timing, price/derivatives cadence, last refresh statuses, and budget state without triggering a provider call.

P3-CALL1 additionally exposes the narrower Dashboard contract:

- `GET /api/provider-call/base-profile`;
- `PUT /api/provider-call/base-profile`;
- `GET /api/provider-call/runtime-status`.

It accepts only `AUTO`, `LOW`, `STANDARD`, and `HIGH`. `EMERGENCY` remains system-only. Reading or changing a profile does not call a provider.

The matrix is configuration-bound. Position price intervals are 15s/10s/5s for LOW/STANDARD/HIGH and 3s for an affected EMERGENCY symbol. Derivatives retain their independent 120s/60s/60s cadence, with an emergency minimum refresh gap of 40s. Unaffected pool symbols are not elevated to EMERGENCY.

## Automatic Escalation and Hysteresis

`ScanProfileTransitionService` reads every trigger and hysteresis value from versioned `tm_rule_config`. The production universe source connects available persisted signals: active manual positions, monitor near-stop/near-target reasons, plan invalidation/high risk/reversal, Push Recheck drift/invalidation/blocking, external-context blocking, confused score, Hot Reset, and latest data quality. Runtime events are applied per symbol and are not promoted into a global profile. ATR/volume/spread and CoinGlass-only inputs remain null until authoritative producers exist; null is never interpreted as normal.

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

The provider-scan refresh port checks the latest persisted closed-bar timestamp per timeframe and enters the coordinated writer path only when the next `5m`/`15m`/`1h`/`4h` close is due. A missing or failed persisted-read check permits a recovery refresh; it never marks missing bars healthy.

## Production Defaults

The following production switches default to `false`:

- `trade-model.provider-call.enabled`;
- `trade-model.provider-call.scheduler-enabled`;
- `trade-model.provider-call.profile-escalation-enabled`;
- `trade-model.provider-call.external-calls-enabled`.
- `trade-model.providers.coinglass.enabled`;
- `trade-model.providers.coinglass.external-calls-enabled`.

All newly introduced provider adapter interfaces are backed by NoCall implementations in this package. Startup, Dashboard reads, runtime-status reads, profile changes, tests, and fixture notification collection perform zero real provider, CoinGlass, AI, or external-message calls.

`ProductionProfileSafetyGuard` requires coordinator enablement before any child feature and explicit external-call opt-in before the provider-scan scheduler. An active CoinGlass call additionally requires provider enablement, key presence, a valid HTTPS base URL, official `CG-API-KEY` authentication, and valid rate settings. Position Monitor remains default-off. The scheduler has bounded universe, due-dataset, priority, and overlap guards, and has no direction, plan, position, order, Push, or Telegram surface.

## Persistence and Migration Evidence

`V5__provider_scan_profile_orchestration.sql` adds only user scan-profile settings and provider scan rule defaults. It does not change trading data or add an executable action. Local H2 bootstrap and PostgreSQL migration SQL stay aligned.

`PostgreSqlFlywayMigrationSmokeTest` now validates clean V1-V5 migration, V5 columns/defaults, profile save/load, transition-audit-compatible insertion, timestamp persistence, transaction rollback atomicity, and mapper-compatible profile reads when Docker/Testcontainers is available. The 2026-07-10 local bounded run found no valid Docker socket, so migration/runtime evidence is `SKIPPED_DOCKER_UNAVAILABLE`, never PASS. Production readiness remains a separate blocked release gate.

## Test Evidence

Focused tests cover bounded assets, priority deduplication, position cadence, closed-position removal, per-dataset cadence, shared cache, deterministic single flight, priority budget/reserve, Retry-After, stale/error metadata, transition thresholds/hysteresis/audit, authenticated profile update, consistent analysis bundles, AI skip/run policy, authoritative OHLCV writer reuse, architecture guards, production defaults, and no trading/external-send surfaces.

CG-1 deterministic tests cover official response mapping, null/no-data behavior, partial/stale assembly, auth/rate/retry bounds, cache/single flight, scan isolation, production defaults, and safety boundaries. The full Maven suite runs with the CoinGlass smoke disabled and no live provider calls. Production readiness remains `BLOCKED`.
