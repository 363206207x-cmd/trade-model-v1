# V1 Minimal Review-Only MarketQuote Freshness Runtime Wiring Implementation

## 1. Executive Summary

本包执行最小 MarketQuote freshness / fallback / source-health review-only runtime implementation。

- Current Mainline（当前主线）: Readiness / Point Mainline.
- Current Block（当前模块）: Minimal Review-Only MarketQuote Freshness Runtime Wiring Implementation.
- Capability Movement（能力层级变化）: `REVIEW_ONLY_RUNTIME partial` remains; this package prepares the MarketQuote slice for verification.
- User-visible Output（用户可见输出）: `/api/market/quote-status` exposes read-only quote freshness status, and dashboard shows MarketQuote source / freshness / fallback / source health / last update / safety labels.
- Overreach Boundary（越界边界）: no DTO / Validator / Assembler / Orchestrator, no schema/config/pom, no Push, no Candidate, no Decision, no Point, no direction, no order/execution, no all-market scan, no P359/P360.

The implementation reuses the existing `MarketQuoteClient` / `BinanceMarketQuoteClient` / `MarketQuoteSnapshot` owner assets and adds no new wrapper owner.

## 2. API Surface

Added minimal read-only endpoint:

```text
GET /api/market/quote-status?symbol=BTCUSDT
```

Returned review-only status fields:

| Field | Meaning |
|---|---|
| `status` | One of the MarketQuote review-only statuses. |
| `sampleSymbol` / `symbols` | Dashboard-only sample symbol, normalized to USDT pair. |
| `source` / `sourceType` | Quote provider identity and source type. |
| `lastQuoteTime` / `lastUpdatedAt` | Fetched timestamp when available. |
| `freshnessSeconds` / `staleThresholdSeconds` | Minimal freshness calculation. |
| `fresh` | Whether the fetched timestamp is inside the threshold. |
| `fallbackActive` | True when the quote is missing or blocked. |
| `sourceHealth` | `OK`, `STALE`, `MISSING`, or `PARTIAL`. |
| `reason` / `message` | Display-only explanation. |
| `reviewOnly` | Always `true`. |
| `notTradingSignal` | Always `true`. |
| `dashboardOnlySample` | Always `true` for this slice. |
| `watchlistBounded` | `false`; this slice does not prove candidate/push eligibility. |
| `displaySlotsAreCandidatePool` | Always `false`. |

Allowed statuses implemented:

- `MARKETQUOTE_REVIEW_ONLY_READY`
- `MARKETQUOTE_STALE_FAIL_CLOSED`
- `MARKETQUOTE_MISSING_FAIL_CLOSED`
- `MARKETQUOTE_SOURCE_HEALTH_PARTIAL`
- `MARKETQUOTE_BLOCKED_FAIL_CLOSED`

`MARKETQUOTE_FALLBACK_ACTIVE` remains a designed status for future fallback-specific evidence, but this minimal endpoint currently expresses missing/fallback through `fallbackActive=true` and fail-closed statuses.

## 3. Dashboard Surface

`dashboard.html` now includes a minimal `marketQuoteStatusPanel` below the Watchlist Pool status block.

The panel displays:

- MarketQuote review-only status;
- sample symbol;
- quote source and source type;
- freshness / stale / missing text;
- fallback active;
- source health;
- last update;
- review-only / not trading signal copy;
- Watchlist boundary copy.

The dashboard fetches:

```text
/api/market/quote-status?symbol=<selected dashboard symbol or BTCUSDT>
```

The copy explicitly states:

- `只读行情状态，不是交易信号`
- `dashboard-only sample`
- `Watchlist Pool 才是候选边界`
- `Display Slots 不是行情候选池`

## 4. Tests

Targeted tests added / strengthened:

| Test | Coverage |
|---|---|
| `MarketControllerTest` | Verifies `reviewOnly=true`, `notTradingSignal=true`, fresh status, missing fail-closed status, and absence of point/trading fields. |
| `DashboardControllerTest` | Verifies dashboard contains MarketQuote endpoint wiring, DOM ids, status constants, and safety copy. |

## 5. Boundary Confirmation

- No DTO / Validator / Assembler / Orchestrator was created.
- No schema/config/pom was changed.
- No Push external channel was connected.
- No Candidate / Decision / Point chain was connected.
- No executable numeric trade levels, direction output, order, execution, or automation output was generated.
- No all-market scan was added.
- Display Slots remain homepage display only and are not promoted to candidate pool.
- P359 / P360 remain frozen.

## 6. Next Required Action

Next required action: `Minimal Review-Only MarketQuote Freshness Runtime Wiring Verification`.

Verification must run compile, test-compile, targeted controller/dashboard tests, forbidden path checks, forbidden semantics grep, and API/dashboard smoke if applicable.
