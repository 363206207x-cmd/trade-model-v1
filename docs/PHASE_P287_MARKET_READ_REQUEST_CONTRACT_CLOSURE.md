# P287 Market-Read Request Contract Closure

P287 closes P286 for the market-read request contract.

## P286 Merge Baseline

P286 merged as `c09a15c`.

P286 was docs-only / contract-only. P286 defined the future `MarketReadRequestDTO` field plan, safety defaults, source boundary, and DTO authorization gate.

P286 kept Java blocked. P286 did not implement Java, tests, DTOs, market-read wiring, runtime/live/external data reads, scan output, real scan loop, score computation, Candidate workflow, Push, Readiness, point generation, or trading behavior.

## P286 Contract Result

P286 established that a future `MarketReadRequestDTO` must:

- remain review-only;
- remain not a trade instruction;
- originate only from a GuardValidator-approved `RealScanInputContractDTO`;
- carry Watchlist Pool proof;
- carry source contract identity;
- carry requested timeframes and scan timestamp;
- fail closed for stale or missing data;
- preserve risk blockers and blocking reasons.

The request contract cannot produce scan output, compute score, create Candidate production workflow, trigger Push execution, upgrade Readiness, generate points, or create trading actions.

## P287 Closure Decision

P287 treats the P286 contract as closed for the purpose of authorizing the next pure-data DTO skeleton.

P287 authorizes P288 to implement `MarketReadRequestDTO` Java skeleton only if P288 is pure data only and remains inside the field, default, source, and test boundaries defined by P287.

If P288 needs any runtime behavior, provider dependency, Spring wiring, or production workflow, P287 authorization no longer applies and another docs-only gate is required.

## Continuing Boundaries

- Display Slots / 默认六币 cannot be scan universe or batch universe.
- Watchlist Pool remains the scan candidate boundary.
- Risk Action Guard must remain before delivery / Push / Readiness.
- 踩踏状态禁止机会推送。
- 插针不等于趋势反转。
- 强反转不等于直接反手。
