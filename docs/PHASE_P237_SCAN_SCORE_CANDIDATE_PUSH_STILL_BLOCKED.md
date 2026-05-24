# P237 ScanScore Candidate Push Still Blocked

## 1. 阶段定位

本文明确 ScanScore / Candidate Attention / Promote To Home / Push / Readiness / point generation 仍阻断。

P237 不解除任何实现禁令。

## 2. 当前已有但不是真实扫描

当前已有但不是真实扫描：

- `RuleConfigWatchlistPoolReadAdapter`
- `DefaultWatchlistRuntimeSourceService`
- `WatchlistRuntimeSourceService`
- `RuntimeSourceReadRequestDTO`
- `RuntimeSourceReadResultDTO`
- `WatchlistRuntimeSourceDTO`
- `WatchlistRuntimeSourceGuardValidator`
- `WatchlistRuntimeSourceWiringAssembler`
- `WatchlistScanResultDTO`
- `WatchlistScanGuardValidator`
- Low-Frequency Scan Scheduler disabled-by-default skeleton

## 3. 仍然没有

以下仍然没有：

- Watchlist Scan Result Assembly Java
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

## 4. 必须继续阻断

以下必须继续阻断：

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

## 5. 结论

后续任何 ScanScore / Candidate / Push / Readiness / point generation implementation 必须另开授权门。

不能把 Watchlist Scan Result Assembly plan 误判为真实扫描。
