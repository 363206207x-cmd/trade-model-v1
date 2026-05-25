# PHASE P244 - Batch Scan Authorization Gate Plan

## Stage Position

This document only plans a batch scan authorization gate.

P244 does not create batch scan.

## Future Batch Requirements

Any future batch scan must require:

- batch universe only from Watchlist Pool
- Display Slots never used as batch universe
- default-six assets never used as batch universe
- non-watchlist assets fail-closed
- batch results only as review-only / blocked / incomplete
- no push trigger
- no readiness upgrade
- no entry / stop / TP / RR generation

## Future Batch Java Still Forbidden

Future batch Java must still not:

- connect `MarketQuoteClient`
- generate `ScanScore`
- create Candidate Attention
- create Opportunity Push
- connect scheduler unless a separate scheduler authorization gate exists

## Conclusion

Batch scan needs an independent PR.

Batch scan must not be combined with scheduler or market-read work.
