# P284 Real Scan Input Assembly Scope

P284 defines only the next-stage real scan input assembly scope.

The next-stage real scan input assembly scope may define how a protected `RealScanInputContractDTO` may be assembled in the future, but P284 does not implement it.

## Future Assembly Inputs To Define

A future docs-only authorization gate may describe:

- Symbol normalization source.
- Watchlist Pool membership proof source.
- Watchlist config version source.
- Requested scan reason source.
- Requested timeframes source.
- Scan timestamp ownership.
- Market-read-required and data-availability-expected semantics.
- Stale/missing input behavior.
- Risk blocker propagation.
- Review-only safety flag propagation.
- Guard validation before downstream use.

## Required Assembly Invariants

Future assembly must remain:

- Watchlist Pool bounded.
- Fail-closed when proof is missing.
- Fail-closed when input is non-watchlist.
- Review-only.
- Not a trade instruction.
- Blocked from upgrading invalid inputs to `REVIEW_ONLY`.

Outputs must preserve `manualReviewRequired=true` and `notTradeInstruction=true`.

## Still Blocked

P284 does not implement production assembly, service wiring, controller/API/scheduler wiring, DTO changes, tests, market reads, scan output, scan loop, production ScanScore computation, Candidate production workflow, Opportunity Push execution, Readiness, point generation, entry-stop-TP-RR, order/execution, or auto-trading.

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary.
