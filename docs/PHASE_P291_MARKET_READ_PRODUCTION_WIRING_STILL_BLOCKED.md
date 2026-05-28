# P291 MarketRead Production Wiring Still Blocked

P291 keeps all production market-read wiring blocked.

## Still Blocked

The following remain blocked after P291:

- production wiring;
- service wiring;
- scheduler;
- API / controller;
- mapper / repository / DB write;
- migration;
- dashboard-triggered market read;
- schema / config;
- `MarketQuoteClient`;
- `BinanceMarketQuoteClient`;
- runtime/live/external data read;
- provider credentials;
- live provider calls;
- scan output;
- real scan loop.

## Test-Only Boundary

Future test-only wiring may only exist inside tests and may only feed `MarketReadRequestDTO` into `MarketReadRequestGuardValidator`.

Test-only wiring is not authorization for production wiring, service wiring, scheduler activation, API exposure, dashboard behavior, provider calls, runtime data reads, scan output, real scan loops, or trading behavior.

## Watchlist Boundary

Watchlist Pool remains the scan candidate boundary.

Display Slots / 默认六币 are not the scan universe, not the batch universe, not the push universe, and not the Watchlist Pool proof source.

Any future production market-read path must preserve Watchlist Pool proof and must pass through a separately authorized guard boundary before delivery / Push / Readiness.

## Risk Boundary

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
