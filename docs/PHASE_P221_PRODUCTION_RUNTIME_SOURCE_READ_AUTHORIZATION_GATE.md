# PHASE P221 Production Runtime Source Read Authorization Gate

## 1. Phase Position

P221 only defines the authorization gate for future production runtime source read.

P221 does not implement production read.

P221 does not write Java.

## 2. Future P222 May Consider

A future P222 may only consider a docs-only authorization gate or read-only adapter skeleton plan first.

If future work enters Java, it must open a separate B or C authorization gate.

Production read includes:

- DB-backed watchlist read.
- MarketQuoteClient read.
- scheduler-triggered read.
- runtime source service.
- production source assembler.
- API / dashboard read path.

## 3. Questions P222 Must Answer First

Before any production read implementation, P222 or a later authorization gate must answer:

- Is the read source Watchlist Pool or Display Slots? It must be Watchlist Pool.
- Do non-watchlist assets fail closed? They must fail closed.
- How is read failure expressed? It must be `INCOMPLETE` / `SOURCE_UNAVAILABLE`.
- How is stale data expressed? It must be `STALE_REVIEW_ONLY` / `INCOMPLETE`.
- Is push allowed? No.
- Is readiness allowed? No.
- Is entry / stop / TP / RR allowed? No.
- Is any trading action allowed? No.

## 4. Future Production Read Must Not Do

Future production read must not:

- use default fixed-six scan behavior.
- treat Display Slots as scan universe.
- allow non-watchlist assets into opportunity candidates.
- bypass `WatchlistRuntimeSourceGuardValidator`.
- allow source read to directly trigger Candidate Attention.
- allow source read to directly trigger Promote To Home.
- allow source read to directly trigger Opportunity Push.
- allow source read to directly generate ScanScore.
- allow source read to directly upgrade readiness.
- allow source read to generate entry / stop / TP / RR.

## 5. Conclusion

P222 must not directly start production reads.

P222 must first define a production read authorization gate / adapter plan.
