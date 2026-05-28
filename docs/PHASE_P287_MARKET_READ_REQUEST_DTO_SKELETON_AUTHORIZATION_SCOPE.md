# P287 Market-Read Request DTO Skeleton Authorization Scope

P287 defines the allowed scope for a future `MarketReadRequestDTO` skeleton.

## Authorization Decision

P287 authorizes P288 to implement a `MarketReadRequestDTO` Java skeleton only as a pure-data DTO with targeted DTO tests.

The authorization is narrow. It does not authorize market-read execution, runtime data access, Spring wiring, production flow, score computation, Candidate workflow, Push, Readiness, point generation, or trading.

## Allowed P288 Skeleton Shape

The future skeleton may:

- define a plain `MarketReadRequestDTO`;
- carry only the frozen fields listed by P287;
- provide safe defaults for review-only and not-trade-instruction flags;
- carry fail-closed stale and missing-data policy values;
- carry source identity and Watchlist Pool proof fields;
- carry blocking reasons and risk blockers;
- provide simple data accessors or constructor/builder patterns consistent with the existing codebase;
- include targeted DTO tests for defaults, source boundary, and fail-closed behavior.

## Required Source Boundary

Only a GuardValidator-approved `RealScanInputContractDTO` can source the future request.

The DTO skeleton must not mint Watchlist Pool proof, infer proof from dashboard state, infer proof from Display Slots / 默认六币, infer proof from provider response, or infer proof from market-data availability.

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary.

## Explicitly Not Authorized

The future DTO skeleton must not have:

- Spring annotations;
- service wiring;
- controller, endpoint, or API;
- scheduler;
- mapper or repository;
- DB read/write;
- schema or migration;
- config changes;
- dashboard changes;
- `MarketQuoteClient` dependency;
- `BinanceMarketQuoteClient` dependency;
- provider dependency;
- live/runtime/external data read;
- scan output creation;
- real scan loop;
- production ScanScore computation;
- Candidate production workflow;
- Candidate Attention production workflow;
- Promote To Home runtime logic;
- Opportunity Push execution;
- external channel behavior;
- provider credentials;
- live provider call;
- message rendering;
- message sending;
- Telegram / email / webhook / app notification / local notification;
- Readiness upgrade;
- point generation;
- entry-stop-TP-RR generation;
- order API;
- execution API;
- auto-trading.

## Fail-Closed Requirement

Missing proof, missing source contract, missing timestamp, missing timeframe, stale policy gaps, missing-data policy gaps, or invalid GuardValidator status must remain blocked.

Risk Action Guard must remain before delivery / Push / Readiness.
