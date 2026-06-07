# V1 MarketQuote Freshness / Fallback Dashboard Source Read

## 1. Executive Summary

结论：`MarketQuote freshness / fallback / dashboard API status` 适合作为 Watchlist 之后的下一条最小 runtime slice，但下一步只能进入 `Minimal Review-Only MarketQuote Freshness Runtime Wiring Design`，不能直接实现。

- `MarketQuoteClient` / `BinanceMarketQuoteClient` 已真实存在：`MarketQuoteClient.fetch24hTicker(symbol)` 返回 `Optional<MarketQuoteSnapshot>`，`BinanceMarketQuoteClient` 通过 Binance public 24h ticker 获取 last price / high / low / price change，并在成功时写入 `fetchedAtEpochMillis`。
- 已有 service：`RealMarketEnvironmentService` 复用 `MarketQuoteClient` 生成 `MarketEnvironmentVO`；`DecisionServiceImpl` 也会 best-effort 拉 quote 写入 `DecisionResultVO.latestPrice` / `priceUpdateTimeMs`；但这些不是独立 quote freshness status owner。
- 已有 controller/API：`MarketController` 有 `/api/market/real-fetch`，`DashboardController` 有 `/api/dashboard/detail` 并会返回 `marketEnvironmentMini` 与 `sourceTrace.quoteFreshnessStatus`；但没有 dedicated market quote freshness / fallback / source-health status endpoint。
- 已有 dashboard 展示：`dashboard.html` 的 SourceTrace 区域显示 `Quote Latest`、`Quote Freshness`，并明确 `Quote latest` 只是行情元数据；但没有清晰的 `MarketQuote freshness/fallback/source health` 独立状态区。
- 已有 freshness / fallback / source health 字段：`MarketQuoteSnapshot.fetchedAtEpochMillis` 可作为 last update source，`provider` 可作为 source；fallback 当前主要是 `Optional.empty()` + log；quote-specific stale / missing / blocked / source-health status 尚未用户可见。
- 不需要新增 DTO / Validator / Assembler 才能进入下一步设计；后续 design 必须优先复用现有 owner path 与 Map/existing VO。
- 本任务不接 Push、Candidate、Decision、Point、交易方向、entry / stop / TP / RR、order / execution / auto-trading。
- 本任务不提升 capability level；当前仍为 `REVIEW_ONLY_RUNTIME partial`，来自 PositionSync 与 Watchlist 两条已验证小闭环。

下一步：`Minimal Review-Only MarketQuote Freshness Runtime Wiring Design`，只设计 owner path、status mapping、dashboard/API 最小边界、Watchlist boundary 与 fail-closed 规则。

## 2. Source Read Inventory

| Area | Files/classes found | Existing behavior | Runtime/API connection | Dashboard connection | Gap |
|---|---|---|---|---|---|
| MarketQuoteClient | `src/main/java/org/example/trademodel/market/client/MarketQuoteClient.java` | 定义 `fetch24hTicker(String assetSymbol)`，返回 `Optional<MarketQuoteSnapshot>`。 | Runtime client interface exists. | No direct dashboard surface. | No status object, no explicit freshness/fallback/source-health contract. |
| BinanceMarketQuoteClient | `src/main/java/org/example/trademodel/market/client/impl/BinanceMarketQuoteClient.java` | 读取 Binance public `/api/v3/ticker/24hr`；HTTP failure / parse failure / exception 返回 empty。 | Runtime provider exists; no API key required. | Indirect only through services. | Failure reason only logged; fallback not surfaced to dashboard/API. |
| MarketQuoteSnapshot | `src/main/java/org/example/trademodel/market/dto/MarketQuoteSnapshot.java` | Carries provider, normalized symbol, last/high/low price, 24h change, fetched timestamp. | Service input for real market environment and decision read model. | Indirect only. | Has timestamp but no computed freshnessStatus, fallbackOccurred, sourceHealth, reason. |
| quote/market service | `RealMarketEnvironmentService`, `MarketServiceImpl`, `RealMarketDataFetcherService` | `RealMarketEnvironmentService.tryBuildFromRealQuote` maps quote into `MarketEnvironmentVO`; `MarketServiceImpl` and real fetcher use broader analysis assembly paths. | Runtime service exists, but some paths are heavier than a status slice. | `DashboardController` can use `RealMarketEnvironmentService` in detail fallback. | Need define minimal quote-status owner and avoid heavy analysis/fetch-trigger path. |
| controller/API | `MarketController`, `DashboardController` | `/api/market/real-fetch` triggers real fetch/assembly; `/api/dashboard/detail` exposes `marketEnvironmentMini` and `sourceTrace` fields. | Partial. `/api/dashboard/detail` is review surface; `/api/market/real-fetch` is not a safe status endpoint. | Dashboard detail consumes `/api/dashboard/detail`. | No dedicated quote freshness/fallback/source-health endpoint. |
| dashboard | `src/main/resources/templates/dashboard.html` | Shows SourceTrace metrics: `Quote Latest`, `Quote Freshness`; copy says quote latest is metadata only and not entry source. | Uses dashboard detail response. | Partial visible diagnostics. | No standalone market quote status panel; quote fallback/source-health is not clear. |
| tests | `BinanceMarketQuoteClientTest`, `RealMarketEnvironmentServiceTest`, `DashboardControllerTest`, `DefaultDashboardSourceTraceDetailAdapterTest` | Tests client parsing, market env mapping, dashboard market mini fallback, and source trace quote fields. | Good targeted coverage for existing pieces. | Dashboard tests cover source trace and market mini JSON, not standalone quote status. | Need future targeted tests for quote freshness/fallback API/dashboard status if implemented. |
| schema / mapper | `tm_market_environment_snapshot`, `MarketEnvironmentSnapshotDO`, `MarketEnvironmentSnapshotMapper`; `tm_asset_state`; persisted OHLCV tables | Market environment snapshot persists source type and derived market env fields; OHLCV readiness has freshness-like status for kline, not quote. | Mapper supports latest market env snapshot by analysis/symbol/timeframe. | Dashboard detail reads snapshot by analysis. | No `tm_quote` or dedicated data-source-health table found for quote status. |
| fallback | `BinanceMarketQuoteClient` empty result, `RealMarketEnvironmentService` Optional empty, `DashboardController` `PLACEHOLDER_FALLBACK` for market mini | Runtime fallback exists as absence / placeholder. | Partial. | Dashboard can show placeholder fallback in `marketEnvironmentMini.sourceType`. | No user-visible reason for provider failure or stale quote. |
| freshness | `MarketQuoteSnapshot.fetchedAtEpochMillis`, `DecisionResultVO.priceUpdateTimeMs`, `SourceTraceDTO.quoteFreshnessStatus=QUOTE_UPDATE_TIME_ONLY` | Timestamp exists, source trace labels quote update as timestamp-only. | Partial. | SourceTrace displays `Quote Freshness`. | No threshold-based FRESH / STALE / UNKNOWN mapping for quote status. |
| data source health | Logs and existing source trace blocking reasons; persisted OHLCV readiness for kline only | No dedicated quote source health owner found. | Missing for quote. | Missing as dedicated dashboard status. | Need design fail-closed statuses before implementation. |

## 3. Existing Runtime Flow

```text
MarketQuoteClient / BinanceMarketQuoteClient
  -> RealMarketEnvironmentService.tryBuildFromRealQuote
  -> MarketEnvironmentVO / MarketEnvironmentSnapshotDO
  -> DashboardController /api/dashboard/detail marketEnvironmentMini
  -> dashboard.html market environment / source trace diagnostics
```

Segment status:

| Segment | Status | Runtime? | Dashboard visible? | Review-only safe? | Notes |
|---|---|---|---|---|---|
| `MarketQuoteClient` / `BinanceMarketQuoteClient` | exists | Yes | No direct surface | Partial | Real public ticker fetch exists; failures become empty optional. |
| `MarketQuoteSnapshot` | exists | Yes | Indirect | Partial | Provider and `fetchedAtEpochMillis` exist, but no computed status. |
| `RealMarketEnvironmentService.tryBuildFromRealQuote` | exists | Yes | Indirect through dashboard detail | Partial | Maps quote to market environment; not a dedicated quote status service. |
| `MarketEnvironmentSnapshotMapper` / `tm_market_environment_snapshot` | exists | Yes, persisted read model | Yes through detail when analysis snapshot exists | Partial | Persists market env source type, not quote source health. |
| `MarketController /api/market/real-fetch` | exists | Yes, fetch trigger | No normal dashboard status | Not the right minimal status path | It can trigger real fetch/assembly, so next slice should not default to this endpoint. |
| `DashboardController /api/dashboard/detail` | exists | Yes | Yes | Yes, read-only | Exposes `marketEnvironmentMini` and source trace; possible future surface candidate. |
| `dashboard.html` SourceTrace display | partial | No, display only | Yes in diagnostics/detail | Yes | Shows quote latest/freshness but not a dedicated MarketQuote status panel. |
| Dedicated quote freshness/fallback/source-health API | missing | No | No | Unknown | Must be designed before any implementation. |

Important boundary: `DecisionServiceImpl` and `PushRecheckScheduler` also use `MarketQuoteClient`. They are evidence that the legacy client is live, but they must not become the owner path for this slice. Push recheck is especially out of scope for this task and remains frozen for external-channel semantics.

## 4. Freshness / Fallback Readiness

- Can judge quote freshness: partial. `MarketQuoteSnapshot.fetchedAtEpochMillis` and `DecisionResultVO.priceUpdateTimeMs` exist, but only `QUOTE_UPDATE_TIME_ONLY` is exposed today; no threshold-based quote freshness status exists.
- Can judge quote source: partial. Snapshot carries `provider` and normalized symbol; `MarketEnvironmentSnapshotDO.sourceType` exists for market environment, not raw quote health.
- Can judge fallback: partial. `BinanceMarketQuoteClient` returns empty on HTTP / parse / exception, `RealMarketEnvironmentService` returns `Optional.empty()`, and `DashboardController` sets `PLACEHOLDER_FALLBACK` for market mini; but reason and fallback occurrence are not clearly surfaced.
- Can judge last update time: partial. `fetchedAtEpochMillis` and `priceUpdateTimeMs` exist, but dashboard quote status does not expose a clear last-success time for MarketQuote.
- Can judge data source health: no dedicated quote-specific health object/table/API was found.
- Safe stale / missing / blocked status: missing for quote-specific runtime status. Existing RuntimeKline/OHLCV readiness has stale/fresh statuses, but dashboard copy already warns quote freshness is not kline stale status.
- Existing tests: yes for client parsing, real market environment mapping, dashboard detail market mini fallback, and source trace quote fields. Missing targeted quote freshness/fallback/source-health status tests.

## 5. Watchlist Boundary Interaction

MarketQuote status must not reopen the package-count trap by turning quote availability into scan/candidate/point wiring.

- MarketQuote slice must not bypass Watchlist Pool.
- It must not default to all-market scanning.
- It must not treat Display Slots as 行情候选池.
- It must not connect Push or external channel.
- It must not generate Candidate.
- It must not generate point.
- It must not generate final direction.
- Future asset reads must be bounded by Watchlist Pool or be explicitly labeled as dashboard-only sample/status for the currently selected symbol.
- Any quote status that is stale, missing, ambiguous, or provider-failed must remain review-only and fail-closed for candidate/push/point usage.

## 6. Candidate Slice Comparison / Go-NoGo

Decision: **A. GO: Minimal Review-Only MarketQuote Freshness Runtime Wiring Design**.

Reason:

- The raw provider and runtime service assets exist.
- The dashboard already has partial read-only surfaces for market environment and source trace quote metadata.
- Existing tests prove client parsing, market environment mapping, and dashboard source trace quote fields.
- The missing parts are designable without creating a new DTO / Validator / Assembler family: owner path, status mapping, field sufficiency, endpoint/surface choice, and fail-closed copy.
- Direct implementation is not safe yet because there is no dedicated quote freshness/fallback/source-health endpoint or clear dashboard status boundary.

Owner path candidate for the next design:

```text
MarketQuoteClient / BinanceMarketQuoteClient
  -> RealMarketEnvironmentService or a minimal existing controller/service status path
  -> DashboardController / existing dashboard detail or future minimal review-only status endpoint
  -> dashboard.html MarketQuote freshness/fallback/source-health display
```

Minimal dashboard/API status candidates for design:

- provider/source: `BINANCE` / `UNKNOWN`
- symbol
- last price presence: present / missing
- last update time from `fetchedAtEpochMillis` or existing read-model time
- quote freshness status: `FRESH` / `STALE` / `UNKNOWN`
- fallback status: no fallback / provider unavailable / placeholder fallback
- reason/message
- reviewOnly = true
- notTradeInstruction = true
- display/candidate boundary labels

Next step must be design only. It must not implement endpoint, dashboard, tests, or service changes yet.

## 7. Rejected Expansion

The following remain rejected for this track:

- Push external channel.
- MarketQuote full runtime scan.
- Candidate generation.
- Evidence / Score wiring.
- DecisionResult wiring beyond existing read-only source trace evidence.
- point generation.
- order / execution / auto-trading.
- P359 / P360.

## 8. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, source read only
- 是否接 service/runtime/dashboard/API: No, source read only
- 是否符合 #830 审计建议: Yes
