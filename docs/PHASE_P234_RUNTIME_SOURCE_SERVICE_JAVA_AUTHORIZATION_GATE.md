# P234 Runtime Source Service Java Authorization Gate

## 1. Phase Position

P234 defines whether future P235 may write Runtime Source Service Java.

P234 does not write Java.

## 2. Future P235 May Consider

Future P235 may consider a `WatchlistRuntimeSourceService` interface.

Future P235 may consider a `DefaultWatchlistRuntimeSourceService` implementation.

Future P235 may consider `DefaultWatchlistRuntimeSourceServiceTest`.

Future P235 may only compose `RuleConfigWatchlistPoolReadAdapter` and `WatchlistRuntimeSourceGuardValidator`.

Future P235 must not connect `MarketQuoteClient`.

Future P235 must not connect scheduler behavior.

Future P235 must not connect controller / API / dashboard paths.

Future P235 must not create a scan loop.

## 3. Future P235 Must Preserve

Future P235 must remain Watchlist Pool only.

Display Slots must not be allowed as input.

The default six assets must not be allowed as input.

All future outputs must preserve:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `opportunityPushAllowed=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`

Future P235 must fail closed on null / missing / unavailable / stale / exception paths.

## 4. Future P235 Prohibited Work

Future P235 must not read market data.

Future P235 must not connect `MarketQuoteClient`.

Future P235 must not connect `BinanceMarketQuoteClient`.

Future P235 must not connect scheduler behavior.

Future P235 must not connect API paths.

Future P235 must not modify dashboard output.

Future P235 must not generate `WatchlistScanResultDTO`.

Future P235 must not generate `ScanScore`.

Future P235 must not create Candidate Attention.

Future P235 must not create Promote To Home.

Future P235 must not create Opportunity Push.

Future P235 must not generate entry / stop / TP / RR.

Future P235 must not upgrade readiness.

Future P235 must not create trading actions.

## 5. Conclusion

If P235 enters Java, it can only be a minimum Runtime Source Service skeleton.

P235 is not a real scan, not a scan loop, and not `MarketQuoteClient` integration.
