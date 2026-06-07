# V1 MarketQuote Freshness Runtime Wiring Implementation Readiness Gate

## 1. Executive Summary

结论：**GO: Minimal Review-Only MarketQuote Freshness Runtime Wiring Implementation**。

允许进入最小 implementation，但范围必须极窄：只能把现有 `MarketQuoteClient` / `BinanceMarketQuoteClient` / `MarketQuoteSnapshot` / `RealMarketEnvironmentService` / SourceTrace quote metadata 组织成只读 quote freshness / fallback / source-health status。现有 `/api/dashboard/detail` 可复用部分 `sourceTrace` 与 `marketEnvironmentMini` 信息；`/api/market/real-fetch` 是 fetch/assembly 触发入口，不适合作为 review-only status endpoint。如果现有 endpoint 不足，未来实现允许新增一个最小只读 quote freshness endpoint。

- 最小 implementation 允许新增/复用 endpoint：可复用 `/api/dashboard/detail` 的 quote metadata；如不足，可在 existing controller 中新增最小 read-only status endpoint。
- 不允许新增 DTO / Validator / Assembler。
- 不允许改 schema。
- 允许最小 dashboard status/copy/DOM，但只能显示 quote source / freshness / fallback / source health / last update / review-only / not trading signal / Watchlist boundary。
- 不允许接 Push / Candidate / Point / Trading。
- 未来最多允许改：existing Market/Dashboard controller 中最小 endpoint、existing MarketQuote/RealMarketEnvironment owner path 中必要的最小 status mapping、`dashboard.html` 最小状态显示、targeted controller/dashboard tests、source-of-truth docs。
- 当前 capability level 不提升；本包是 readiness gate only。
- 下一步：`Minimal Review-Only MarketQuote Freshness Runtime Wiring Implementation`。

## 2. Implementation Permission Matrix

| Area | Allowed? | Allowed files | Reason | Guardrail |
|---|---|---|---|---|
| MarketQuoteClient / BinanceMarketQuoteClient | Reuse only | Existing `MarketQuoteClient`, `BinanceMarketQuoteClient` only if absolutely necessary | Provider exists and can supply quote metadata. | No provider expansion, no new exchange, no Push, no scan loop. |
| MarketQuoteSnapshot / source trace | Reuse only | Existing `MarketQuoteSnapshot`, SourceTrace quote metadata path only if absolutely necessary | Snapshot has provider/symbol/last price/fetched timestamp; SourceTrace has quote latest/freshness metadata. | No new DTO; no entry/stop/TP/RR fields. |
| RealMarketEnvironmentService | Reuse / minimal status mapping only | Existing `RealMarketEnvironmentService` only if necessary | Existing owner path maps quote into market environment; may provide quote availability evidence. | No broad analysis wiring; no candidate/decision/point expansion. |
| controller/API | Allowed, minimal | Existing `MarketController` or `DashboardController` only | No dedicated quote status endpoint exists; `/api/market/real-fetch` is not a safe status endpoint. | Read-only endpoint only; Map/existing object/existing VO only; no writes, no fetch-trigger semantics beyond status. |
| dashboard.html | Allowed, minimal | `src/main/resources/templates/dashboard.html` | Dashboard has partial SourceTrace quote display; may need small status/copy surface. | No large layout change, no complex quote card, no all-market scan, no Display Slots as candidate pool. |
| controller/dashboard tests | Allowed, targeted | Existing controller/dashboard tests only; add targeted tests only if implementation touches endpoint/dashboard | Must prove review-only response, labels, and forbidden semantics. | No broad test suite expansion; no test-only wrapper owner. |
| source-of-truth docs | Allowed | Existing source-of-truth docs and one implementation note if needed | Keep workflow state aligned. | No docs-only plan chain beyond required implementation record. |
| schema.sql | No | None | Quote freshness status can be derived/displayed without schema change. | Any schema requirement is No-Go. |
| config / pom | No | None | No dependency or config change needed for readiness. | Any config/pom need is No-Go. |
| DTO / Validator / Assembler | No | None | Freeze rule blocks new skeleton families; Map/existing object/existing VO is enough for minimal status. | Any new DTO/Validator/Assembler is No-Go. |
| Push / Candidate / Point / Trading | No | None | Out of scope and explicitly frozen. | No Push send, no candidate generation, no point generation, no direction, no order/execution. |

## 3. Minimal Endpoint Readiness

- Existing reusable endpoint: partial. `/api/dashboard/detail` already exposes `sourceTrace.quoteLatestPrice`, `sourceTrace.quoteFreshnessStatus`, and `marketEnvironmentMini`; it can be reused as evidence but is not a dedicated MarketQuote status endpoint.
- Existing endpoint not suitable: `/api/market/real-fetch` triggers real fetch / analysis assembly and should not become the minimal status surface.
- Minimal endpoint allowed: Yes, if implementation cannot safely use `/api/dashboard/detail` alone. The endpoint must be read-only and minimal.
- DTO requirement: No. The endpoint may return `Map`, existing object, or existing VO-derived fields.
- Endpoint must be read-only.
- Endpoint must not send Push.
- Endpoint must not generate Candidate.
- Endpoint must not generate Point.
- Endpoint must not generate direction.
- Endpoint must not call order/execution.
- Endpoint must not claim that Binance provider presence equals trading authorization.

Allowed endpoint fields should stay close to #860 design:

- `status`
- `symbol` or bounded `symbols`
- `source`
- `sourceType`
- `lastQuoteTime` / `lastUpdatedAt`
- `fresh`
- `fallbackActive`
- `sourceHealth`
- `reason`
- `message`
- `reviewOnly = true`
- `notTradingSignal = true`
- `watchlistBounded` or `dashboardOnlySample`

## 4. Minimal Dashboard Readiness

- Minimal dashboard status/copy/DOM is allowed.
- It may only display quote source / freshness / fallback / source health / last update / review-only / not trading signal / Watchlist boundary.
- It must not significantly alter layout.
- It must not add a complex market card.
- It must not trigger all-market scan.
- It must not treat Display Slots as 行情候选池.
- It must not turn quote freshness into entry / stop / TP / RR evidence.
- It must not expose Push / Candidate / Point / Trading semantics.

If touched, dashboard copy must make clear:

- quote status is review-only;
- quote status is not a trading signal;
- stale/missing/fallback status remains fail-closed;
- Display Slots are not a quote candidate pool;
- Watchlist Pool or dashboard-only sample boundaries control interpretation.

## 5. Required Test Scope For Implementation

Future minimal implementation must add or update targeted tests for:

- controller endpoint test for quote freshness status;
- dashboard static test for quote freshness labels;
- no DTO / Validator / Assembler check;
- no Push / Candidate / Point / Trading semantics check;
- Watchlist boundary copy check if dashboard is touched;
- endpoint response includes `reviewOnly=true`;
- endpoint response includes `notTradingSignal=true`;
- stale/missing/fallback response is fail-closed;
- no `candidateRanking`, `entry`, `stop`, `TP`, `RR`, `final direction`, `order`, or `execution` output.

## 6. No-Go Conditions

Implementation must not proceed if it requires:

- new DTO / Validator / Assembler;
- major schema change;
- direct Push connection;
- Candidate generation;
- Point generation;
- direction or trading action generation;
- all-market scanning;
- bypassing Watchlist Pool or failing to mark dashboard-only sample;
- dashboard with no safe insertion position;
- API fields that are insufficient unless a complex endpoint/object is created;
- changing config or pom;
- treating `/api/market/real-fetch` as the status endpoint.

## 7. Go / No-Go Decision

Decision: **A. GO: Minimal Review-Only MarketQuote Freshness Runtime Wiring Implementation**.

Allowed implementation scope:

- existing `MarketController` or `DashboardController` only if a minimal read-only endpoint is needed;
- existing `MarketQuoteClient` / `BinanceMarketQuoteClient` / `MarketQuoteSnapshot` / `RealMarketEnvironmentService` only as owner-path inputs;
- `dashboard.html` only for minimal status/copy/DOM;
- targeted controller/dashboard tests;
- source-of-truth docs.

Forbidden implementation scope:

- new DTO / Validator / Assembler / Orchestrator;
- schema/config/pom;
- Push / external channel;
- Candidate / Decision / Point expansion;
- all-market scan;
- entry / stop / TP / RR;
- final direction;
- order / execution / auto-trading;
- P359 / P360.

The next package may implement, but it must not auto-merge and must prove boundaries through targeted tests and forbidden-path checks.

## 8. Capability-Level Statement

- Current level: `REVIEW_ONLY_RUNTIME partial`.
- 本包是否提升 level: No, readiness gate only.
- Future MarketQuote minimal implementation target: `REVIEW_ONLY_RUNTIME partial` for MarketQuote slice.
- It is not Production Wiring.
- It is not Push.
- It is not Candidate generation.
- It is not Point generation.
- It is not Trading.

## 9. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, readiness gate only
- 是否接 service/runtime/dashboard/API: No, readiness only
- 是否符合 #830 审计建议: Yes

## 10. Final Recommendation

允许进入最小 implementation。允许改 existing controller 的最小只读 quote freshness endpoint、existing owner path 的必要 status mapping、`dashboard.html` 的最小 status/copy/DOM、targeted controller/dashboard tests、source-of-truth docs。禁止 Push、Candidate、Point、P359/P360、全市场扫描、交易方向、entry / stop / TP / RR、order / execution / auto-trading；不需要也不允许新增 DTO / Validator / Assembler。
