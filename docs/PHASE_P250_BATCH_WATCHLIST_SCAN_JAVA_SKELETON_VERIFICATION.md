# PHASE P250 Batch Watchlist Scan Java Skeleton Verification

## 1. Phase Positioning

P250 only implements a minimal batch skeleton for Watchlist Pool scan review output.

P250 adds:

- `BatchWatchlistScanResultEnvelopeDTO`
- `BatchWatchlistScanOrchestrator`
- `DefaultBatchWatchlistScanOrchestrator`
- `DefaultBatchWatchlistScanOrchestratorTest`

P250 is not a real scan, not scoring, and not push execution.

## 2. Implemented Boundary

The batch skeleton is disabled-by-default and only accepts explicit `requestedSymbols`.

The implementation:

- Uses `RuntimeSourceReadRequestDTO.forWatchlistPool(...)` for each accepted symbol.
- Calls only `LowFrequencyWatchlistScanOrchestrator.scanSingleSymbol(...)`.
- Normalizes symbols with trim + uppercase.
- Rejects blank symbols as `INVALID_SYMBOL`.
- Deduplicates duplicate symbols and records `DUPLICATE_SYMBOL`.
- Converts null / exception / unsafe single-symbol output into incomplete symbol results.
- Keeps all output as review-only / blocked / incomplete.

## 3. Explicit Non-Goals

P250 does not include:

- schema change
- mapper change
- API / controller / dashboard wiring
- scheduler integration
- MarketQuoteClient
- BinanceMarketQuoteClient
- runtime / live / external data reads
- real scan loop
- real scan
- real ScanScore computation
- Candidate Attention workflow
- Promote To Home workflow
- opportunity push execution
- readiness upgrade
- real entry / stop / TP / RR
- order / execution / auto-trading

## 4. Safety Defaults

All envelope and symbol outputs preserve:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `opportunityPushAllowed=false`
- `candidateAttentionAllowed=false`
- `promoteToHomeAllowed=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`

## 5. Verification

Commands:

```text
./mvnw -q -Dtest=DefaultBatchWatchlistScanOrchestratorTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
git diff --check
git diff --name-status main...HEAD
git status
```

Results:

- Targeted test: passed.
- Compile: passed.
- Test compile: passed.
- `git diff --check`: passed.

## 6. Current Conclusion

P250 is the minimal Batch Watchlist Scan skeleton.

It is not a real scan, not scoring, not scheduler activation, not market-read, and not push execution.
