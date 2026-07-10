# Provider Snapshot Adoption Closure

## Scope and Decision

CALL-1B closes primary-business provider snapshot adoption after PR #1110. The runtime boundary is:

```text
business consumer -> snapshot/read service -> ProviderCallCoordinator -> provider adapter
analysis/decision -> authoritative persisted OHLCV query -> sole OHLCV writer/provider path
```

No order, auto-open, auto-close, auto-reverse, auto-trading, external push send, fake position, or fake review capability is introduced. Production readiness remains `BLOCKED`.

## Direct Reader Inventory

| Consumer | Previous direct dependency | Dataset | Fallback | New snapshot/read boundary | Cache key/freshness | Fail-closed behavior | Classification | Tests |
|---|---|---|---|---|---|---|---|---|
| `DashboardHomeServiceImpl` | `MarketQuoteClient` | PRICE | null | read-only `MarketPriceSnapshotService.peek` | Binance/PRICE/symbol/GLOBAL/LATEST; caller freshness | unavailable remains null; never zero; page read never calls provider | `MIGRATED_TO_SNAPSHOT` | Dashboard + architecture guard |
| `DecisionServiceImpl` | `MarketQuoteClient` | PRICE | persisted decision fields | read-only `MarketPriceSnapshotService.peek` | shared LATEST key; 30s caller freshness | unavailable leaves live quote fields null; read model never calls provider | `MIGRATED_TO_SNAPSHOT` | Decision service + architecture guard |
| `RealMarketEnvironmentService` | quote/funding/OI clients | PRICE, DERIVATIVES | placeholder market env | price + minimal Binance derivatives snapshots | independent PRICE/DERIVATIVES keys | absent derivatives remain unavailable; no normal/zero synthesis | `MIGRATED_TO_SNAPSHOT` | market environment tests |
| `PositionMonitorServiceImpl` | `MarketQuoteClient` | PRICE | none | `MarketPriceSnapshotService` | P0 15s freshness | stale/unavailable/non-positive rejects monitor write | `MIGRATED_TO_SNAPSHOT` | position monitor + policy tests |
| `PushRecheckServiceImpl` | `MarketQuoteClient` | PRICE | none | `MarketPriceSnapshotService` | P1 15s freshness | `QUOTE_STALE`/`QUOTE_UNAVAILABLE`/`INVALID_MARKET_PRICE`; result remains invalid/review-only | `MIGRATED_TO_SNAPSHOT` | Push Recheck + policy tests |
| `PushRecheckScheduler` | duplicate quote read before service | PRICE | skip | delegates null price to Push Recheck snapshot resolution | same service/cache path | no duplicate call or direct retry | `MIGRATED_TO_SNAPSHOT` | architecture guard/full suite |
| `DecisionEngineService` | `RealMarketDataFetcherService` 1m/5m | OHLCV | exception | `AuthoritativePersistedDecisionOhlcvSource` 5m/15m/1h/4h | persisted readiness/freshness per timeframe and run trace | missing/stale/incomplete timeframe aborts decision | `AUTHORITATIVE_PERSISTED_READ` | Decision Engine + architecture guard |
| `AnalysisAssemblerServiceImpl` plan boundary | persisted OHLCV | OHLCV | incomplete plan | existing persisted query/assembly | authoritative store freshness | incomplete boundary stays fail-closed | `AUTHORITATIVE_PERSISTED_READ` | existing plan tests |
| `CoordinatedOhlcvSnapshotService` | public OHLCV provider | OHLCV | explicit error/stale metadata | coordinator -> provider -> `PersistedOhlcvIngestionService` | OHLCV symbol/timeframe/minute bucket | writer failure is degraded/error | `ALLOWED_PROVIDER_ADAPTER` | refresh port/writer tests |
| `DefaultProviderDatasetRefreshPort` | persisted latest closed-bar timestamp | OHLCV due state | recovery refresh when missing/read error | `PersistedOhlcvBarMapper` authoritative read + coordinated writer | symbol/timeframe/next close | skips provider when no new closed bar is due | `AUTHORITATIVE_PERSISTED_READ` | refresh-port due-gate tests |
| `DefaultProviderScanUniverseSource` | persisted position/state/monitor/push/config rows | runtime scan signals | bounded empty sets | authoritative mappers/read services | symbol-scoped transition state | unavailable signal remains absent; never healthy-by-default | `AUTHORITATIVE_PERSISTED_READ` | universe/escalation tests |
| `MarketPriceSnapshotService` | Binance quote adapter | PRICE | coordinator stale-readable cache | coordinator adapter boundary | stable LATEST key; per-consumer freshness | explicit metadata/error; read-only peek never invokes adapter | `ALLOWED_PROVIDER_ADAPTER` | coordinator/policy tests |
| `BinanceDerivativesSnapshotService` | Binance funding/OI adapters | DERIVATIVES | not configured | coordinator adapter boundary | independent derivatives key | missing both is `NOT_CONFIGURED` | `ALLOWED_PROVIDER_ADAPTER` | market environment/full suite |
| `BinancePublicOhlcvProvider` | `RealMarketDataFetcherService` | OHLCV | typed provider state | provider adapter feeding sole writer | coordinator-owned upstream | malformed/unavailable bars rejected | `ALLOWED_PROVIDER_ADAPTER` | existing provider tests |
| `MarketController` | legacy fetcher/quote diagnostics | PRICE/OHLCV | diagnostic response | unchanged diagnostic endpoint | none | not used by primary business decisions | `LEGACY_DIAGNOSTIC_ONLY` | controller tests |
| `RealMarketDataFetcherService` | Binance HTTP | OHLCV | empty/error | retained behind OHLCV adapter and diagnostics | adapter only | not injected into decision/business services | `ALLOWED_PROVIDER_ADAPTER` | architecture guard |

No primary business consumer is classified `FORBIDDEN_DIRECT_READER`.

## Closure Status

| Status | Result |
|---|---|
| `PROVIDER_SINGLE_ENTRY_STATUS` | `PASS` |
| `SNAPSHOT_REUSE_STATUS` | `PASS` |
| `AUTO_ESCALATION_STATUS` | `PASS` |
| `POSITION_PRICE_MONITOR_STATUS` | `PASS` |
| `PUSH_RECHECK_SNAPSHOT_STATUS` | `PASS` |
| `DASHBOARD_SNAPSHOT_STATUS` | `PASS` |
| `RUNTIME_SCAN_UNIVERSE_STATUS` | `PASS` |
| `DERIVATIVES_WITHOUT_COINGLASS_STATUS` | `NOT_CONFIGURED` |
| `POSTGRESQL_V5_MIGRATION_STATUS` | `SKIPPED_DOCKER_UNAVAILABLE` |
| `POSTGRESQL_V5_RUNTIME_STATUS` | `SKIPPED_DOCKER_UNAVAILABLE` |
| `LIVE_PROVIDER_CALLS` | `0` |
| `PRODUCTION_READINESS` | `BLOCKED` |

## Runtime Scan Universe

`DefaultProviderScanUniverseSource` provides:

- exactly six configured core assets, with the six V1 defaults as a fail-closed fallback;
- manual `OPEN` and `PARTIALLY_CLOSED` positions only;
- `CANDIDATE` and `WAITING_TRIGGER` persisted asset states;
- bounded recent Push Recheck drift/invalidation/risk/confused-block signals as event candidates;
- bounded, validated, deduplicated `push.watchlist.symbols` pool entries;
- highest-priority deduplication through `AssetPriorityResolver`;
- last-attempt timestamps from `ProviderRefreshStateRegistry`.

Available persisted signals feed `ScanProfileTransitionService`: near stop/target, plan invalidation, Push Recheck drift/invalidation, high risk, reversal, external-context block, confused score, Hot Reset, and data-quality deterioration. These transitions remain symbol-scoped; one asset's event does not promote the global scan profile. Missing ATR/volume/spread and CoinGlass data stay null. Downgrade hysteresis and transition audit remain active.

`DefaultProviderDatasetRefreshPort` routes PRICE through the shared price snapshot and OHLCV through the coordinated four-timeframe writer path. Before each OHLCV refresh it reads the latest authoritative closed bar and calls the coordinator only when the next closed bar is due; missing/read-error state permits a fail-closed recovery attempt. Routine scan never invokes AI. CoinGlass-backed derivatives are explicitly `NOT_CONFIGURED`; external context is explicit `NOT_CONFIGURED` until an authoritative provider snapshot exists.

## Runtime Visibility

`GET /api/config/scan-profile/runtime?symbol=BTCUSDT` is authenticated and read-only. It returns configured/effective profile, priority, reason, effective/next-downgrade times, price/derivatives intervals, last source/freshness observations, and provider budget state. Reading it does not call a provider.

## PostgreSQL V5 Evidence

The bounded Testcontainers test now checks:

- clean Flyway V1-V5;
- seven V5 profile columns;
- sixteen versioned `provider.scan.*` defaults;
- profile save/load and timestamp persistence;
- scan-profile audit insertion;
- rollback atomicity;
- mapper-compatible profile query shape.

Local bounded execution on 2026-07-10 completed with exit code 0 and Testcontainers reported no valid Docker socket. Therefore both the migration and runtime evidence are `SKIPPED_DOCKER_UNAVAILABLE`; the skip is not converted to PASS.

## Remaining Boundaries

- CoinGlass is not connected; liquidation/long-short/full derivatives fields remain unavailable.
- External context provider refresh remains not configured.
- Live provider calls and production deployment evidence are outside this local test package.
- Production readiness remains `BLOCKED`.
