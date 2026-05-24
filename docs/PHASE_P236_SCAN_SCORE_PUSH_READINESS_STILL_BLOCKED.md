# P236 ScanScore Push Readiness Still Blocked

## 1. Phase Position

This document confirms that ScanScore / Push / Readiness / point generation remain blocked.

P236 does not lift any implementation ban.

## 2. Current Pieces That Are Not A Real Scan

The following pieces exist, but they are not a real scan:

- `RuleConfigWatchlistPoolReadAdapter`
- `DefaultWatchlistRuntimeSourceService`
- `WatchlistRuntimeSourceService`
- `DefaultWatchlistPoolRuntimeSourceReadAdapter`
- `RuntimeSourceReadRequestDTO`
- `RuntimeSourceReadResultDTO`
- `WatchlistRuntimeSourceDTO`
- `WatchlistRuntimeSourceGuardValidator`
- `WatchlistRuntimeSourceWiringAssembler`
- `WatchlistScanResultDTO`
- `WatchlistScanGuardValidator`
- `RuleConfigService`
- `RuleConfigMapper`
- `tm_rule_config`
- Low-Frequency Scan Scheduler disabled-by-default skeleton

## 3. Still Missing

The following remain missing:

- Watchlist Scan Result Assembly
- Production Source Assembler
- `MarketQuoteClient` adapter implementation
- `BinanceMarketQuoteClient` adapter implementation
- scheduler-triggered adapter implementation
- scan loop
- real low-frequency scan
- real `ScanScore`
- Candidate Attention workflow
- Promote To Home workflow
- Opportunity Push execution
- readiness
- real entry / stop / TP / RR
- order / execution / auto-trading

## 4. Must Remain Blocked

The following must continue to be blocked:

- `ScanScore` production output
- Candidate Attention
- Promote To Home
- Opportunity Push execution
- Readiness upgrade
- entry / stop / TP / RR
- `MarketQuoteClient` implementation
- `BinanceMarketQuoteClient` implementation
- scheduler activation
- scan loop
- production service wiring into dashboard/API

## 5. Conclusion

Any future `ScanScore`, Push, Readiness, or point generation implementation must open a separate authorization gate.

The Runtime Source Service skeleton must not be mistaken for a real low-frequency scan.
