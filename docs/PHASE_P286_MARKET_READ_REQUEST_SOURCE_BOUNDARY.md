# P286 Market-Read Request Source Boundary

P286 defines the source boundary for future `MarketReadRequestDTO` assembly.

## Allowed Source

The only allowed future source is a GuardValidator-approved `RealScanInputContractDTO`.

The future request must carry forward from that approved source:

- symbol;
- Watchlist Pool proof;
- watchlist config version;
- requested scan reason;
- requested timeframes;
- scan timestamp;
- risk blockers;
- review-only flag;
- not-trade-instruction flag;
- guard validation status;
- blocking reasons.

The future request must not mint new Watchlist Pool proof. It must not infer proof from display state, market data, external provider response, scheduler input, API request, dashboard state, or default-symbol configuration.

## Disallowed Sources

The following cannot originate a future `MarketReadRequestDTO`:

- Display Slots;
- 默认六币;
- dashboard display order;
- scheduler trigger;
- API/controller request;
- external provider symbols;
- market-data availability alone;
- runtime/live/external data response;
- non-watchlist user-facing list;
- incomplete or blocked `RealScanInputContractDTO`;
- `RealScanInputContractDTO` that has not passed `RealScanInputContractGuardValidator`.

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary.

## Boundary Enforcement

If source, proof, timeframe, timestamp, stale policy, missing-data policy, or guard validation status is missing or invalid, future request assembly must fail closed.

Fail-closed means no market-read Java, no runtime/live/external data read, no scan output, no real scan loop, no production ScanScore computation, no Candidate production workflow, no Opportunity Push execution, no scheduler/API/dashboard wiring, no Readiness, no point generation, and no trading action.
