# PHASE P217 Runtime Source Guard Authorization Gate

## 1. Phase Position

P217 only defines the authorization gate for a future runtime source guard / validator.

P217 does not implement a guard / validator.

P217 does not write Java.

## 2. Future P218 May Consider

A future P218 may consider only a maximum-safe pure guard / validator / tests package.

Allowed future concepts:

- A pure guard / validator / tests implementation.
- Consuming `WatchlistRuntimeSourceDTO`.
- Returning a safe `WatchlistRuntimeSourceDTO` or `WatchlistScanResultDTO`.
- A no-runtime-read / no-score / no-push / no-readiness / no-trading guard.
- Fail-closed decisions only.

## 3. Future Guard May Judge

A future guard may judge:

- non-watchlist blocked.
- unknown membership incomplete.
- missing source incomplete.
- source unavailable incomplete.
- stale source review-only / incomplete.
- unknown source type incomplete.
- `FRESH` but no push / readiness / trading.

## 4. Future Guard Must Not Do

A future guard must not:

- read DB.
- read runtime / live / external data.
- connect MarketQuoteClient.
- connect scheduler.
- connect mapper / service / controller / API.
- create scan loop.
- create real scan.
- compute ScanScore.
- create Candidate Attention workflow.
- create Promote To Home workflow.
- create Opportunity Push execution.
- generate entry / stop / TP / RR.
- upgrade readiness.
- create trading action.

## 5. Future Guard Must Keep

Every output must keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `opportunityPushAllowed=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`

## 6. Conclusion

If P218 implements anything, it must first be pure guard / validator / tests.

P218 must not directly read DB, market data, scheduler state, or runtime data.
