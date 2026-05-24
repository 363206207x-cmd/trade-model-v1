# P234 Runtime Source Service Plan

## 1. Phase Position

P234 is the Runtime Source Service plan document.

P234 does not implement Java.

P234 does not connect `MarketQuoteClient`.

P234 does not enable scheduler behavior.

P234 does not create a real scan.

## 2. Future Runtime Source Service Responsibility

The future Runtime Source Service may only serve Watchlist Pool inputs.

The future service may receive `RuntimeSourceReadRequestDTO`.

The future service may call `RuleConfigWatchlistPoolReadAdapter`.

The future service must call or preserve `WatchlistRuntimeSourceGuardValidator`.

The future service may output `RuntimeSourceReadResultDTO` or `WatchlistRuntimeSourceDTO`.

Every future output must keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `opportunityPushAllowed=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`

## 3. Out Of Scope

Runtime Source Service is not responsible for reading market data.

Runtime Source Service is not responsible for calling `MarketQuoteClient`.

Runtime Source Service is not responsible for scheduler triggering.

Runtime Source Service is not responsible for scan loop execution.

Runtime Source Service is not responsible for generating `WatchlistScanResultDTO`.

Runtime Source Service is not responsible for generating `ScanScore`.

Runtime Source Service is not responsible for Candidate Attention.

Runtime Source Service is not responsible for Promote To Home.

Runtime Source Service is not responsible for Opportunity Push.

Runtime Source Service is not responsible for generating entry / stop / TP / RR.

Runtime Source Service is not responsible for readiness.

Runtime Source Service is not responsible for trading actions.

## 4. Future Data Flow

```text
RuntimeSourceReadRequestDTO
-> RuleConfigWatchlistPoolReadAdapter
-> WatchlistRuntimeSourceGuardValidator
-> RuntimeSourceReadResultDTO / WatchlistRuntimeSourceDTO
-> review-only output
```

## 5. Future Fail-Closed Rules

Future Runtime Source Service behavior must remain fail-closed:

- `null request` => `INCOMPLETE`
- non-watchlist / not Watchlist Pool => `INCOMPLETE`
- missing config => `INCOMPLETE`
- unavailable source => `SOURCE_UNAVAILABLE`
- stale source => `STALE_REVIEW_ONLY`
- valid membership => `AVAILABLE_REVIEW_ONLY`
- any exception => `SOURCE_UNAVAILABLE` / `INCOMPLETE`
- all outputs must keep no push / no readiness / no trading / no `entryStopTpRrGenerated`

## 6. Conclusion

P234 does not authorize P235 to directly connect market data, scheduler behavior, or scan loop execution.

If P235 moves forward, it should first create a Runtime Source Service Java authorization gate or a minimum service skeleton, and it must still avoid market data integration.
