# P285 Real Scan Input Assembly Authorization Gate

P285 defines the authorization gate for future protected `RealScanInputContractDTO` assembly. It does not implement assembly.

## Purpose

Future real scan input assembly may only be considered after a separate gate defines exactly how a protected `RealScanInputContractDTO` can be assembled without opening market-read runtime behavior.

The future gate must keep the DTO review-only and fail-closed. It must not convert a valid-looking scan input into a trade instruction, score, Candidate, Push, Readiness state, point, order, execution, or auto-trading action.

## Future Entry Conditions

Future assembly entry conditions must include:

- explicit Watchlist Pool membership proof;
- proof source and source ownership;
- watchlist config version;
- requested scan reason;
- requested timeframes;
- scan timestamp;
- stale/missing behavior;
- risk blockers;
- review-only flags;
- guard validation result from `RealScanInputContractGuardValidator`.

The gate must define how each input is created, validated, and rejected. P285 does not create any Java, tests, DTO changes, service wiring, mapper, repository, DB read/write, scheduler, API, dashboard, or runtime data read for these inputs.

## Required Assembly Inputs To Consider

Future assembly design must consider:

- symbol;
- Watchlist Pool proof;
- watchlist config version;
- requested scan reason;
- requested timeframes;
- scan timestamp;
- stale/missing behavior;
- risk blockers;
- review-only flags;
- guard validation result.

Future assembly may also define a data availability state, but missing or stale data availability must fail closed and must not trigger a market read in P285.

## Guard Validation

Any future assembled DTO must be passed through `RealScanInputContractGuardValidator`.

The validator result remains authoritative:

- missing Watchlist Pool proof must not proceed;
- non-watchlist input must not proceed;
- null input must remain incomplete / fail-closed;
- blocked input cannot be upgraded to `REVIEW_ONLY`;
- output must preserve `manualReviewRequired=true`;
- output must preserve `notTradeInstruction=true`.

## Fail-Closed Assembly Rules

Future assembly must fail closed when:

- symbol is missing or unsupported;
- Watchlist Pool proof is missing;
- proof source is missing;
- watchlist config version is missing;
- requested scan reason is missing;
- requested timeframe is missing, unsupported, or not authorized;
- scan timestamp is missing or stale;
- data availability is missing or stale;
- risk blocker state is unknown;
- review-only flags are missing or false;
- guard validation is missing, skipped, or blocked.

Fail-closed output cannot read market data, create scan output, compute score, create Candidate, trigger Push, upgrade Readiness, generate point, generate entry/stop/TP/RR, place order, call execution API, or auto-trade.

## Next Authorization

Future recommended next package after P285 should be P286 Real Scan Input Assembly Plan or Market-Read Request Contract Authorization Gate, docs-only only unless separately authorized.
