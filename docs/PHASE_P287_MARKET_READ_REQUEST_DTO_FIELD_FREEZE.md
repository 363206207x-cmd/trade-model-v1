# P287 Market-Read Request DTO Field Freeze

P287 freezes the exact allowed fields for a future `MarketReadRequestDTO` skeleton.

## Frozen Field List

The future DTO may contain only:

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
- `reviewOnly`;
- `notTradeInstruction`;
- `guardValidationStatus`;
- `blockingReasons`.

No additional market data, price, entry, stop, TP, RR, order, execution, provider, channel, push, readiness, scheduler, or dashboard fields are authorized.

## Field Meaning Boundaries

The frozen fields describe a review-only request contract. They are not scan output, not provider response, not score, not Candidate, not push message, not readiness state, and not a trading instruction.

The future DTO must preserve:

- `reviewOnly=true`;
- `notTradeInstruction=true`;
- fail-closed stale policy;
- fail-closed missing-data policy;
- missing Watchlist Pool proof blocked;
- missing source contract blocked;
- missing timestamp/timeframe blocked;
- blocking reasons carried forward.

## Source Requirement

Only a GuardValidator-approved `RealScanInputContractDTO` can source the future DTO.

The future DTO must not infer source eligibility from Display Slots / 默认六币, dashboard order, scheduler trigger, API request, provider symbol, market data availability, or external data response.

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary.

## No Behavior From Fields

The field freeze does not authorize:

- `MarketQuoteClient` / `BinanceMarketQuoteClient` wiring;
- runtime/live/external data read;
- scan output creation;
- real scan loop;
- production ScanScore computation;
- Candidate production workflow;
- Opportunity Push execution;
- scheduler/API/dashboard wiring;
- external channel behavior / provider credentials / live provider call / message rendering / sending;
- Readiness / point generation / entry-stop-TP-RR / order / execution / auto-trading.
