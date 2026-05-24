# P236 Watchlist Scan Result Assembly Authorization Gate

## 1. Phase Position

P236 only defines the future authorization gate for Watchlist Scan Result Assembly.

P236 does not write Java.

P236 does not connect `MarketQuoteClient`.

P236 does not enable scheduler behavior.

P236 does not create a real scan.

P236 does not generate `ScanScore`.

## 2. Future P237 Candidate Scope

Future P237 may consider:

- docs-only Watchlist Scan Result Assembly plan
- docs-only `WatchlistScanResultDTO` usage audit
- Java only under a separate B/C authorization gate
- assembly that consumes only `RuntimeSourceReadResultDTO` / `WatchlistRuntimeSourceDTO`
- assembly that outputs only review-only / blocked / incomplete scan result skeleton

Future assembly must not calculate real `ScanScore`.

Future assembly must not trigger Candidate Attention, Promote To Home, Push, readiness, or point generation.

## 3. Questions P237 Must Answer First

P237 must answer:

- Are current `WatchlistScanResultDTO` fields enough to express `INCOMPLETE` / `BLOCKED` / `REVIEW_ONLY`?
- Is an existing `WatchlistScanGuardValidator` reusable?
- How should Runtime Source Service output map to scan result?
- How should missing / unavailable / stale be represented?
- How should non-watchlist fail closed?
- Is a new assembler needed? Default answer should be plan first, no Java.
- Is `ScanScore` output allowed? No.
- Are Candidate Attention / Promote To Home / Push allowed? No.
- Are entry / stop / TP / RR allowed? No.

## 4. Future P237 Still Forbidden

Future P237 must still not:

- connect `MarketQuoteClient`
- connect `BinanceMarketQuoteClient`
- connect scheduler
- create scan loop
- create real scan
- calculate `ScanScore`
- create Candidate Attention
- create Promote To Home
- create Opportunity Push
- generate entry / stop / TP / RR
- upgrade readiness
- create trading action

## 5. Conclusion

P237 should not directly write scan result assembly Java.

P237 should first do a Watchlist Scan Result Assembly plan / DTO usage audit.
