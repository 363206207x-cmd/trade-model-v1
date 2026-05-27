# P286 Runtime Market Read Still Blocked

P286 keeps runtime market reads blocked.

## Runtime Read Boundary

The future `MarketReadRequestDTO` plan is only a request contract plan. It does not execute the request.

The following remain blocked:

- `MarketQuoteClient` wiring;
- `BinanceMarketQuoteClient` wiring;
- runtime data read;
- live data read;
- external data read;
- provider credential handling;
- live provider call;
- any scheduler/API/dashboard-triggered market read;
- any real scan loop.

## Request Does Not Equal Read

Even a future valid-looking request must remain review-only and not a trade instruction.

The request cannot produce scan output, score, Candidate, Push, Readiness, point generation, or trading action. A future DTO skeleton, if separately authorized, must still not call a provider or read market data.

## Fail-Closed Behavior

When required field/source/proof/timeframe/timestamp/stale policy/missing-data policy is missing or invalid, the future request must fail closed:

- no runtime/live/external data read;
- no scan output creation;
- no real scan loop;
- no production ScanScore computation;
- no Candidate production workflow;
- no Opportunity Push execution;
- no scheduler/API/dashboard wiring;
- no external channel behavior;
- no Readiness / point generation / entry-stop-TP-RR / order / execution / auto-trading.

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary. Risk Action Guard must remain before delivery / Push / Readiness.
