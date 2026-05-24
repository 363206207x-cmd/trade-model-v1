# P236 Runtime Source Service Closure

## 1. Phase Position

P236 is the closure for the P235 Runtime Source Service Java skeleton.

P236 only records what P235 completed and where its boundaries remain.

P236 does not implement new functionality.

## 2. P235 Merge Baseline

- PR: #589
- Issue: #588
- merge commit: 973e861
- title: BACKEND-P235 Runtime Source Service Java Skeleton

## 3. P235 Completed Scope

P235 completed:

- added `WatchlistRuntimeSourceService`
- added `DefaultWatchlistRuntimeSourceService`
- added `DefaultWatchlistRuntimeSourceServiceTest`
- added P235 verification documentation
- updated `V1_CURRENT_STATE.md`
- updated `PROJECT_PROGRESS_INDEX.md`

## 4. P235 Test Confirmation

P235 tests proved:

- null request fails closed
- missing readAdapter returns `SOURCE_UNAVAILABLE`
- missing guardValidator returns `INCOMPLETE`
- readAdapter returns null fails closed
- readAdapter result without runtimeSource returns safely
- guardValidator returns null fails closed
- guardValidator returns availableReviewOnly source safely
- service does not create Display Slots or default six by itself
- all outputs keep `manualReviewRequired=true`
- all outputs keep `notTradeInstruction=true`
- all outputs keep `opportunityPushAllowed=false`
- all outputs keep `readinessUpgraded=false`
- all outputs keep `tradingActionCreated=false`
- all outputs keep `entryStopTpRrGenerated=false`
- service declares no forbidden `MarketQuoteClient` / `BinanceMarketQuoteClient` / Controller / Scheduler / `PushRecheckService` / `PushSnapshotService` / `ExternalRuntimeService` / `RuntimeDataClient` / `DataSource` / `JdbcTemplate` fields

## 5. P235 Did Not Complete

P235 did not complete:

- no schema change
- no mapper change
- no API / controller / dashboard wiring
- no `MarketQuoteClient`
- no `BinanceMarketQuoteClient`
- no scheduler
- no scan loop
- no real scan
- no real `ScanScore` computation
- no Candidate Attention workflow
- no Promote To Home workflow
- no opportunity push execution
- no readiness upgrade
- no real entry / stop / TP / RR
- no order / execution / auto-trading

## 6. Current Conclusion

P235 is a minimum Runtime Source Service skeleton.

P235 is not a real scan.

P235 is not market data reading.

P235 does not authorize P237 to directly connect `MarketQuoteClient`, scheduler, scan loop, Push, readiness, or point generation.
