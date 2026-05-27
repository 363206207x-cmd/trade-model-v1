# P286 Market-Read Request Contract Fields

P286 defines the future `MarketReadRequestDTO` field plan only. It does not create a DTO.

## Baseline

P285 merged as `c64c8b8`.

P285 was docs-only / boundary-only and kept market-read Java blocked. P286 continues that boundary by planning the request contract before any DTO skeleton or market-read Java work is authorized.

## Future Field Plan

Future `MarketReadRequestDTO` fields to consider:

- `symbol`;
- `requestId`;
- `sourceContractId`;
- `watchlistPoolProof`;
- `watchlistConfigVersion`;
- `requestedScanReason`;
- `requestedTimeframes`;
- `scanTimestamp`;
- `dataAvailabilityExpectation`;
- `stalePolicy`;
- `missingDataPolicy`;
- `riskBlockers`;
- `reviewOnly` flag;
- `notTradeInstruction` flag;
- `guardValidationStatus`;
- `blockingReasons`.

## Source Contract Requirement

The future request must originate only from a GuardValidator-approved `RealScanInputContractDTO`.

GuardValidator-approved means the source contract has already passed `RealScanInputContractGuardValidator`, remains `REVIEW_ONLY`, carries Watchlist Pool proof, is a Watchlist Pool member, preserves `manualReviewRequired=true`, and preserves `notTradeInstruction=true`.

Any source contract that is null, incomplete, blocked, missing proof, non-watchlist, or missing review-only safety flags must not produce a market-read request.

## Field Boundaries

The future field plan is not a runtime request implementation. The planned fields cannot:

- call `MarketQuoteClient` or `BinanceMarketQuoteClient`;
- read runtime/live/external data;
- create scan output;
- create a real scan loop;
- compute production ScanScore;
- create Candidate workflow;
- trigger Opportunity Push;
- upgrade Readiness;
- generate point generation, entry, stop, TP, RR, order, execution, or auto-trading.

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary.
