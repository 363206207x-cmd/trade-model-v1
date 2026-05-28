# P289 MarketReadRequestGuardValidator Test Requirements

P289 defines targeted-test requirements for a future P290 `MarketReadRequestGuardValidator` Java skeleton.

P289 does not add or modify tests. These requirements are documentation only.

## Required Future Targeted Tests

Future P290 targeted tests must cover:

- missing `sourceContractId` blocked;
- missing `watchlistPoolProof` blocked;
- missing `requestedTimeframes` blocked;
- missing `scanTimestamp` blocked;
- `stalePolicy` missing or invalid blocked;
- `missingDataPolicy` missing or invalid blocked;
- `reviewOnly=false` path impossible or blocked;
- `notTradeInstruction=false` path impossible or blocked;
- `blockingReasons` preserved;
- `riskBlockers` preserved;
- no `MarketQuoteClient` dependency;
- no `BinanceMarketQuoteClient` dependency;
- no runtime/live/external data read;
- no scan output behavior;
- no score behavior;
- no Candidate behavior;
- no Push behavior;
- no Readiness behavior;
- no point generation behavior;
- no trading behavior.

## Reflection / Dependency Expectations

Future P290 tests should confirm the validator does not expose market client, provider credential, live provider, channel, message, score, Candidate, Push, Readiness, point, order, execution, entry, stop, TP, RR, or trading dependencies.

The tests should stay targeted to validator behavior. They must not start Spring context, wire services, call controllers, activate scheduler behavior, hit dashboard/API paths, read DB/runtime/live/external data, or call providers.

## Expected Validation Semantics

Future P290 tests should assert that the validator returns only blocked / review-only / fail-closed outcomes.

A valid-looking request can at most remain review-only and not-trade-instruction. It cannot become a scan output, trading recommendation, order instruction, execution instruction, Push payload, Readiness upgrade, or point-generation output.
