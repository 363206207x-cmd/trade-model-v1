# P286 Market-Read Request Safety Defaults

P286 defines future `MarketReadRequestDTO` safety defaults. It does not create Java or tests.

## Required Defaults

Future `MarketReadRequestDTO` must default to safe, review-only behavior:

- `reviewOnly=true`;
- `notTradeInstruction=true`;
- manual review remains required through the source `RealScanInputContractDTO`;
- stale policy defaults to fail-closed;
- missing-data policy defaults to fail-closed;
- missing request id is blocked;
- missing source contract id is blocked;
- missing symbol is blocked;
- missing Watchlist Pool proof is blocked;
- missing watchlist config version is blocked;
- missing requested scan reason is blocked;
- missing requested timeframes are blocked;
- missing scan timestamp is blocked;
- missing guard validation status is blocked;
- risk blockers are preserved;
- blocking reasons are preserved and expanded rather than discarded.

## Fail-Closed Behavior

Future request assembly must fail closed when required field/source/proof/timeframe/timestamp/stale policy/missing-data policy is missing or invalid.

Fail-closed request state cannot:

- read market data;
- create scan output;
- compute score;
- create Candidate;
- trigger Push;
- upgrade Readiness;
- generate point;
- generate entry / stop / TP / RR;
- place order;
- call execution API;
- auto-trade.

## No Upgrade Rule

A valid-looking request remains review-only and not a trade instruction.

A blocked or incomplete source contract cannot be upgraded into a valid market-read request. A blocked request cannot produce scan output, score, Candidate, Push, Readiness, point generation, or trading action.

Risk Action Guard must remain before delivery / Push / Readiness. 踩踏状态禁止机会推送。插针不等于趋势反转。强反转不等于直接反手。
