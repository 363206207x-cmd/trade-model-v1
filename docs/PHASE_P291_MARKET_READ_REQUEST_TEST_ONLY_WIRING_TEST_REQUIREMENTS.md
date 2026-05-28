# P291 MarketReadRequest Test-Only Wiring Test Requirements

Future P292 targeted tests must remain test-only and must only validate DTO-to-guard behavior.

## Required Future P292 Test Cases

Future P292 must test:

- valid review-only DTO passes through validator as review-only;
- missing `sourceContractId` remains blocked;
- missing `watchlistPoolProof` remains blocked;
- missing `requestedTimeframes` remains blocked;
- missing `scanTimestamp` remains blocked;
- invalid `stalePolicy` remains blocked;
- invalid `missingDataPolicy` remains blocked;
- `blockingReasons` are preserved;
- `riskBlockers` are preserved;
- no runtime/live/external data read;
- no scan output / score / Candidate / Push / Readiness / point / trading behavior.

## Required Safety Assertions

Future P292 tests must keep every result:

- review-only;
- not trade instruction;
- manual review required;
- fail-closed.

Future P292 tests must verify the wiring helper does not depend on `MarketQuoteClient`, `BinanceMarketQuoteClient`, provider credentials, live provider calls, controller, scheduler, API, mapper, repository, DB write, schema, config, dashboard, scan output, score, Candidate, Push, Readiness, point generation, entry / stop / TP / RR, order, execution, or auto-trading behavior.

## Out Of Scope

Future P292 tests must not start Spring context, scheduler, API, service runtime, production assembler, provider client, database write path, external channel, message rendering, message sending, or trading path.

## Boundary Rules

Watchlist Pool remains the scan candidate boundary.

Display Slots / 默认六币 are not the scan universe, not the batch universe, not the push universe, and not the Watchlist Pool proof source.

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
