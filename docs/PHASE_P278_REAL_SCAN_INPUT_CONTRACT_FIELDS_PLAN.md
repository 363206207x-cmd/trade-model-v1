# P278 Real Scan Input Contract Fields Plan

This file records the planned field surface for a future real scan input contract DTO. P278 does not implement the DTO.

## Future Candidate Fields

A future DTO may include:

- symbol
- source
- requested scan reason
- Watchlist Pool membership proof
- watchlist config version
- requested timeframe list
- scan timestamp
- market-read requirement flags
- data availability expectations
- stale/missing input behavior
- risk blockers
- review-only safety flags

These fields describe the input contract only. They do not authorize `MarketQuoteClient` / `BinanceMarketQuoteClient` wiring, runtime/live/external data reads, a real scan loop, production ScanScore computation, Candidate production workflow, Opportunity Push execution, scheduler/API/dashboard wiring, external channel behavior, provider credentials, live provider calls, message rendering, message sending, Readiness, point generation, entry-stop-TP-RR, order/execution, or auto-trading.

## Excluded Fields

The future DTO must not contain fields for:

- trade action
- order
- execution
- entry
- stop
- take profit
- RR
- provider
- external channel
- message sending
- readiness

The contract must remain review-only and fail-closed. It may be audit-only where applicable.
