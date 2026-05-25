# P238 Watchlist Scan Result Assembly Boundary

## 1. Phase Position

This document defines the boundary for future Watchlist Scan Result Assembly Java.

P238 does not implement Java.

## 2. Assembly Sole Responsibility

Future assembly may only map source-level DTOs into a safe scan-result DTO:

- Map `RuntimeSourceReadResultDTO` / `WatchlistRuntimeSourceDTO` to `WatchlistScanResultDTO`.
- Preserve review-only / blocked / incomplete semantics.
- Preserve `blockingReasons`.
- Preserve no-score / no-push / no-readiness / no-trading semantics.
- Avoid production opportunity decisions.

## 3. Status Mapping Boundary

Future Java may map statuses only within the following boundary:

- `INCOMPLETE` -> `WatchlistScanResultDTO.incomplete(...)`
- `SOURCE_UNAVAILABLE` -> `WatchlistScanResultDTO.incomplete(...)` or review-only blocked reason
- `STALE_REVIEW_ONLY` -> `WatchlistScanResultDTO.reviewOnly(...)`
- `AVAILABLE_REVIEW_ONLY` -> `WatchlistScanResultDTO.reviewOnly(...)`
- non-watchlist -> `WatchlistScanResultDTO.blockedNotWatchlist(...)` if already available, otherwise incomplete + `BLOCKED_NOT_WATCHLIST`
- guard blocked -> incomplete + `GUARD_BLOCKED`
- exception -> incomplete + `ASSEMBLY_FAILED`

## 4. Assembly Is Not Responsible For

Future assembly must not be responsible for:

- `ScanScore`.
- Candidate Attention.
- Promote To Home.
- Push.
- readiness.
- point generation.
- market data read.
- scheduler trigger.
- API / dashboard output.

## 5. Future Test Boundary

Future tests must prove:

- incomplete source maps to `INCOMPLETE`
- source unavailable maps to `INCOMPLETE` / blocked reason
- stale source maps to `REVIEW_ONLY`
- available source maps to `REVIEW_ONLY`
- non-watchlist maps to fail-closed
- null input fails closed
- exception fails closed
- all outputs keep no push / no readiness / no trading / no `entryStopTpRrGenerated`
- no forbidden fields: `MarketQuoteClient` / `BinanceMarketQuoteClient` / `Controller` / `Scheduler` / `DataSource` / `JdbcTemplate`

## 6. Conclusion

Watchlist Scan Result Assembly is a source-to-scan-result skeleton.

It is not scoring, candidate generation, or push execution.
