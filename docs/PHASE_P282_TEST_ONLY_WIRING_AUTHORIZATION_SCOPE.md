# P282 Test-Only Wiring Authorization Scope

P282 authorizes only a future test-only wiring skeleton. It does not implement that skeleton.

Future test-only wiring may wire `RealScanInputContractDTO` -> `RealScanInputContractGuardValidator` only in tests, without production service wiring.

## Allowed Future Shape

A future P283 package may introduce minimal test-only wiring that:

- Instantiates the existing `DefaultRealScanInputContractGuardValidator` from tests.
- Passes `RealScanInputContractDTO` into `RealScanInputContractGuardValidator`.
- Asserts the existing fail-closed / review-only / not-trade-instruction semantics.
- Remains targeted-test-only.

The wiring must remain non-runtime and test-owned. It must not create a production service path.

## Required Test-Only Guarantees

Future test-only wiring must preserve:

- `manualReviewRequired=true`.
- `notTradeInstruction=true`.
- Null input fails closed.
- Missing Watchlist Pool proof fails closed.
- Non-watchlist input fails closed.
- Valid-looking input remains review-only and not trade instruction.
- Blocked input cannot be upgraded to `REVIEW_ONLY`.

## Still Not Authorized

Future test-only wiring must not:

- Read market data.
- Call `MarketQuoteClient`.
- Call `BinanceMarketQuoteClient`.
- Read runtime/live/external data.
- Create scan output.
- Create a real scan loop.
- Compute production ScanScore.
- Create Candidate.
- Trigger Push.
- Upgrade Readiness.
- Generate point.
- Generate real entry / stop / TP / RR.
- Create order / execution / auto-trading behavior.

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary. Risk Action Guard must remain before delivery / Push / Readiness.
