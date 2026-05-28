# P290 MarketReadRequestGuardValidator Skeleton Closure

P290 implements the `MarketReadRequestGuardValidator` Java skeleton authorized by P289.

## Completed Scope

P290 adds:

- `src/main/java/org/example/trademodel/service/watchlistscan/MarketReadRequestGuardValidator.java`;
- `src/main/java/org/example/trademodel/dto/marketread/MarketReadRequestGuardValidationResult.java`;
- `src/main/java/org/example/trademodel/dto/marketread/MarketReadRequestGuardValidationStatusEnum.java`;
- `src/test/java/org/example/trademodel/service/watchlistscan/MarketReadRequestGuardValidatorTest.java`.

The validator only validates `MarketReadRequestDTO`. It does not execute market reads, call providers, read runtime/live/external data, create scan output, compute score, create Candidate workflow, execute Push, upgrade Readiness, generate points, or create trading actions.

## Fail-Closed Rules

The validator blocks:

- null request;
- missing `sourceContractId`;
- missing `watchlistPoolProof`;
- missing `requestedTimeframes`;
- missing `scanTimestamp`;
- missing or invalid `stalePolicy`;
- missing or invalid `missingDataPolicy`;
- `reviewOnly` not true;
- `notTradeInstruction` not true.

The validator preserves `blockingReasons` and `riskBlockers`.

## Result Boundary

`MarketReadRequestGuardValidationResult` can only express blocked / review-only / fail-closed validation state.

It always keeps:

- `reviewOnly=true`;
- `failClosed=true`;
- `manualReviewRequired=true`;
- `notTradeInstruction=true`.

It is not a scan output, score, Candidate, Push payload, Readiness state, point-generation output, entry / stop / TP / RR, order instruction, execution instruction, or trading action.

## Still Blocked

P290 does not authorize:

- `MarketQuoteClient` / `BinanceMarketQuoteClient`;
- runtime/live/external data read;
- provider credentials;
- live provider calls;
- scan output;
- real scan loop;
- production ScanScore;
- Candidate Attention;
- Promote To Home;
- Opportunity Push;
- external channel;
- message rendering;
- message sending;
- Readiness;
- point generation;
- entry / stop / TP / RR;
- order / execution / auto-trading;
- controller / endpoint / API;
- mapper / repository / DB write / migration;
- dashboard.html;
- schema / config;
- scheduler.

## Validation

P290 targeted tests cover valid review-only DTO, null request, missing source contract id, missing Watchlist Pool proof, missing requested timeframes, missing scan timestamp, missing/invalid stale policy, missing/invalid missing-data policy, impossible or blocked unsafe safety flag paths, preserved blocking reasons, preserved risk blockers, absence of market client dependencies, absence of runtime/live/external reads, and absence of scan output / score / Candidate / Push / Readiness / point / trading behavior.

## Boundary Rules

Watchlist Pool remains the scan candidate boundary.

Display Slots / 默认六币 are not the scan universe, not the batch universe, not the push universe, and not the Watchlist Pool proof source.

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
