# PHASE P223 Production Runtime Source Adapter Interface Authorization Gate

## 1. Phase Position

P223 is the authorization gate for a future production runtime source adapter interface skeleton.

P223 only defines whether future interface skeleton work may proceed.

P223 does not implement Java.

P223 does not read DB data.

P223 does not connect `MarketQuoteClient`.

P223 does not enable scheduler behavior.

P223 does not create a real scan.

## 2. Future P224 May Consider

A future P224 may consider only interface / DTO-safe contract / tests work.

Future P224 may define:

- `ProductionRuntimeSourceReadAdapter` interface.
- `WatchlistPoolRuntimeSourceReadAdapter` interface.
- `RuntimeSourceReadRequestDTO`, if needed and only as a pure DTO.
- `RuntimeSourceReadResultDTO`, if needed and only as a pure DTO.

Future P224 may only create interface-level skeletons. It must not implement reads.

## 3. Tentative P224 Interface Semantics

The following semantics are only a plan:

- input must be a Watchlist Pool symbol / read request.
- output must be `WatchlistRuntimeSourceDTO` or a safe read result.
- non-watchlist inputs must fail closed.
- read failure must be `SOURCE_UNAVAILABLE` / `INCOMPLETE`.
- stale inputs must be `STALE_REVIEW_ONLY` / `INCOMPLETE`.
- all outputs must keep `manualReviewRequired=true`.
- all outputs must keep `notTradeInstruction=true`.
- all outputs must keep no push / no readiness / no trading.

## 4. P224 Still Must Not Do

Future P224 must not:

- read DB data.
- connect `MarketQuoteClient`.
- connect scheduler behavior.
- connect mapper / service / controller / API.
- create scan loop.
- create real scan.
- compute ScanScore.
- create Candidate Attention.
- create Promote To Home.
- create Opportunity Push.
- generate entry / stop / TP / RR.
- upgrade readiness.
- create trading action.

## 5. Conclusion

P223 only authorizes the possibility of a future interface skeleton.

P223 does not authorize P224 to implement DB / Market / Scheduler reads.
