# P238 Watchlist Scan Result Assembly Java Authorization Gate

## 1. Phase Position

P238 is the authorization gate for a future Watchlist Scan Result Assembly Java skeleton.

P238 does not implement Java.
P238 does not connect `MarketQuoteClient`.
P238 does not enable a scheduler.
P238 does not create a real scan.
P238 does not generate `ScanScore`.

## 2. Future P239 Candidate Scope

Future P239 may consider only the following minimum skeleton:

- `WatchlistScanResultAssembler` interface.
- `DefaultWatchlistScanResultAssembler` implementation.
- `DefaultWatchlistScanResultAssemblerTest`.
- Consume only `RuntimeSourceReadResultDTO` / `WatchlistRuntimeSourceDTO`.
- Call only `WatchlistScanGuardValidator`.
- Output only `WatchlistScanResultDTO` review-only / blocked / incomplete skeleton.

Future P239 must not output real `ScanScore`.
Future P239 must not create Candidate Attention.
Future P239 must not create Promote To Home.
Future P239 must not create Opportunity Push.
Future P239 must not upgrade readiness.
Future P239 must not generate entry / stop / TP / RR.

## 3. Future P239 Recommended Candidate Files

The following files are only an authorization plan, not implementation in P238:

- `src/main/java/org/example/trademodel/service/watchlistscan/WatchlistScanResultAssembler.java`
- `src/main/java/org/example/trademodel/service/watchlistscan/DefaultWatchlistScanResultAssembler.java`
- `src/test/java/org/example/trademodel/service/watchlistscan/DefaultWatchlistScanResultAssemblerTest.java`
- `docs/PHASE_P239_WATCHLIST_SCAN_RESULT_ASSEMBLY_JAVA_SKELETON_VERIFICATION.md`

## 4. Future P239 Must Preserve

Future P239 must keep every output safe:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `opportunityPushAllowed=false`
- `candidateAttentionAllowed=false`
- `promoteToHomeAllowed=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`
- `ScanScore` absent / not generated
- Watchlist Pool only
- non-watchlist fail-closed
- missing source fail-closed
- source unavailable fail-closed
- stale source review-only only
- available source review-only only

## 5. Future P239 Still Forbidden

Future P239 remains forbidden from:

- Reading market data.
- Connecting `MarketQuoteClient`.
- Connecting `BinanceMarketQuoteClient`.
- Connecting scheduler.
- Connecting controller / API.
- Changing dashboard.
- Creating scan loop.
- Creating real scan.
- Computing `ScanScore`.
- Creating Candidate Attention.
- Creating Promote To Home.
- Creating Opportunity Push execution.
- Generating entry / stop / TP / RR.
- Upgrading readiness.
- Creating trading action.

## 6. Conclusion

P238 only authorizes future consideration of a minimum Watchlist Scan Result Assembly Java skeleton.

P238 does not authorize Market / Scheduler / `ScanScore` / Candidate / Push / Readiness / point generation.
