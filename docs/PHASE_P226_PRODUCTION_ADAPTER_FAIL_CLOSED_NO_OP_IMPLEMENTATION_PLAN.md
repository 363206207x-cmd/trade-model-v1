# PHASE P226 Production Adapter Fail-Closed No-Op Implementation Plan

## 1. Phase Position

P226 is the plan document for a future production adapter fail-closed no-op implementation.

P226 does not implement Java.

P226 does not read DB data.

P226 does not connect `MarketQuoteClient`.

P226 does not enable scheduler behavior.

P226 does not create a real scan.

## 2. Recommended Future First Implementation

If a later phase enters Java, the recommended first step is only one fail-closed implementation:

- `DefaultWatchlistPoolRuntimeSourceReadAdapter`
- or `NoOpWatchlistPoolRuntimeSourceReadAdapter`

That implementation may only fail closed.

The default result must be `RuntimeSourceReadResultDTO.incomplete(...)` or `RuntimeSourceReadResultDTO.sourceUnavailable(...)`.

The no-op implementation must not:

- read DB data.
- read market data.
- read runtime data.
- connect scheduler behavior.
- create a scan loop.

## 3. Recommended Future Files

The following are only a later-phase plan:

- `src/main/java/org/example/trademodel/service/watchlistsource/DefaultWatchlistPoolRuntimeSourceReadAdapter.java`
- `src/test/java/org/example/trademodel/service/watchlistsource/DefaultWatchlistPoolRuntimeSourceReadAdapterTest.java`
- `docs/PHASE_P227_PRODUCTION_ADAPTER_FAIL_CLOSED_NO_OP_VERIFICATION.md`

## 4. Future No-Op Implementation Must Keep

A future no-op implementation must keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `opportunityPushAllowed=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`
- `readStatus=INCOMPLETE` or `SOURCE_UNAVAILABLE`
- `blockingReasons` contains `READ_ADAPTER_NOT_IMPLEMENTED` or `NO_RUNTIME_READ_IMPLEMENTED`

## 5. Future No-Op Implementation Must Not Do

A future no-op implementation must not:

- read DB data.
- read `MarketQuoteClient`.
- read `BinanceMarketQuoteClient`.
- be triggered by scheduler behavior.
- enter a scan loop.
- create ScanScore.
- create Candidate Attention.
- Promote To Home.
- create Opportunity Push execution.
- generate entry / stop / TP / RR.
- upgrade readiness.
- create trading action.

## 6. Conclusion

P226 does not authorize P227 to directly connect DB / Market / Scheduler implementation.

If P227 enters Java, it may only be a fail-closed no-op implementation.
