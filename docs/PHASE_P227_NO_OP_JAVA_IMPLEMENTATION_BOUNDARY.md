# PHASE P227 No-Op Java Implementation Boundary

## 1. Phase Position

This document defines the boundary for a future no-op Java implementation.

P227 does not implement Java.

## 2. Only Responsibility Of The No-Op

A future no-op adapter may only:

- receive `RuntimeSourceReadRequestDTO`.
- return `RuntimeSourceReadResultDTO`.
- always fail closed.
- express that real adapter reads are not implemented yet.
- preserve manual review semantics.
- preserve not-trade-instruction semantics.

## 3. No-Op Must Not Be Responsible For

A future no-op adapter must not:

- read Watchlist DB.
- read config DB.
- read market data.
- read cache data.
- call `MarketQuoteClient`.
- call scheduler behavior.
- call mapper.
- call controller / API.
- generate `WatchlistScanResultDTO`.
- generate ScanScore.
- generate Candidate Attention.
- generate Promote To Home.
- generate Opportunity Push.
- generate entry / stop / TP / RR.
- upgrade readiness.
- create trading action.

## 4. Tests Must Prove

Future tests must prove:

- null request fails closed.
- incomplete request fails closed.
- normal Watchlist Pool request still only returns `INCOMPLETE` / `SOURCE_UNAVAILABLE`.
- every output keeps `manualReviewRequired=true`.
- every output keeps `notTradeInstruction=true`.
- every output keeps `opportunityPushAllowed=false`.
- every output keeps `readinessUpgraded=false`.
- every output keeps `tradingActionCreated=false`.
- every output keeps `entryStopTpRrGenerated=false`.
- no forbidden fields exist: `MarketQuoteClient` / `BinanceMarketQuoteClient` / `Mapper` / `Controller` / `Scheduler` / `DataSource` / `JdbcTemplate`.

## 5. Conclusion

No-op is a protective implementation, not production read.

After no-op is merged, the project still must not enter real scan without a separate authorization gate.
