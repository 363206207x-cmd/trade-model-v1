# P291 MarketReadRequestGuardValidator Closure

P290 completed the `MarketReadRequestGuardValidator` Java skeleton.

## Completed P290 Scope

P290 added:

- `MarketReadRequestGuardValidator`;
- `MarketReadRequestGuardValidationResult`;
- `MarketReadRequestGuardValidationStatusEnum`;
- `MarketReadRequestGuardValidatorTest`;
- P290 marker and closure documentation.

P290 only validates `MarketReadRequestDTO`.

The validator output can only express blocked / review-only / fail-closed validation state, validation reasons, preserved blocking reasons, preserved risk blockers, manual review required, and not-trade-instruction semantics.

## Safety Semantics

P290 fails closed for:

- null request;
- missing `sourceContractId`;
- missing `watchlistPoolProof`;
- missing `requestedTimeframes`;
- missing `scanTimestamp`;
- missing or invalid `stalePolicy`;
- missing or invalid `missingDataPolicy`;
- `reviewOnly` not true;
- `notTradeInstruction` not true.

P290 preserves `blockingReasons` and `riskBlockers`.

P290 keeps all validator results review-only, fail-closed, manual-review-required, and not trade instruction.

## Not Runtime Market Read

P290 is not a real market read.

P290 does not connect `MarketQuoteClient` or `BinanceMarketQuoteClient`.

P290 does not read runtime/live/external data.

P290 does not create scan output, score, Candidate, Push, Readiness, point, order, execution, or trading behavior.

## Still Blocked

After P290, the following remain blocked:

- production wiring;
- service wiring;
- scheduler;
- controller / endpoint / API;
- mapper / repository / DB write / migration;
- dashboard.html;
- schema / config;
- `MarketQuoteClient` / `BinanceMarketQuoteClient`;
- runtime/live/external data read;
- scan output;
- real scan loop;
- production ScanScore;
- Candidate Attention;
- Promote To Home;
- Opportunity Push;
- Readiness;
- point generation;
- entry / stop / TP / RR;
- order / execution / auto-trading.

## Boundary Rules

Watchlist Pool remains the scan candidate boundary.

Display Slots / 默认六币 are not the scan universe, not the batch universe, not the push universe, and not the Watchlist Pool proof source.

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
