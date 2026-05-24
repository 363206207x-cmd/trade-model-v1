# PHASE P231 DB Read Market Scheduler Still Blocked

## 1. Phase Position

This document confirms Market / Scheduler / runtime scan remains blocked.

P231 does not lift any implementation prohibition.

## 2. Existing But Not Complete Production Read

The project may currently contain:

- `DefaultWatchlistPoolRuntimeSourceReadAdapter`
- `RuntimeSourceReadRequestDTO`
- `RuntimeSourceReadResultDTO`
- `WatchlistRuntimeSourceDTO`
- `WatchlistRuntimeSourceGuardValidator`
- `WatchlistRuntimeSourceWiringAssembler`
- `RuleConfigService`
- `RuleConfigMapper`
- `tm_rule_config`
- Low-Frequency Scan Scheduler disabled-by-default skeleton

These are not complete DB-backed production runtime reads.

## 3. Still Missing

The project still does not have:

- DB-backed watchlist read implementation.
- confirmed `push.watchlist.symbols` runtime config.
- `MarketQuoteClient` adapter implementation.
- `BinanceMarketQuoteClient` adapter implementation.
- scheduler-triggered adapter implementation.
- production runtime source assembler.
- production runtime source service.
- scan loop.
- real low-frequency scan.
- real ScanScore.
- Candidate Attention workflow.
- Promote To Home workflow.
- Opportunity Push execution.
- readiness.
- real entry / stop / TP / RR.
- order / execution / auto-trading.

## 4. Must Remain Blocked

The following remain blocked:

- `MarketQuoteClient` implementation.
- `BinanceMarketQuoteClient` implementation.
- scheduler activation.
- scan loop.
- production service wiring.
- API response.
- dashboard display.
- observability logging / metrics implementation.

## 5. Conclusion

Any future Market / Scheduler / runtime scan implementation must open a separate authorization gate.

The DB read Java authorization gate must not be mistaken for real scan authorization.
