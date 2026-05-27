# P285 Runtime Market Read Still Blocked

P285 keeps runtime market reads blocked.

## Blocked Runtime Reads

The following remain blocked:

- `MarketQuoteClient` wiring;
- `BinanceMarketQuoteClient` wiring;
- runtime data read;
- live data read;
- external data read;
- provider credential handling;
- live provider call;
- any market-data request triggered by real scan input assembly;
- any market-data request triggered by scheduler/API/dashboard/controller/service wiring.

P285 does not read market data and does not create a runtime reader.

## Data Availability Boundary

Future design may name data availability as an input to consider for protected `RealScanInputContractDTO` assembly. P285 does not implement that input.

If future proof/source/timeframe/timestamp/data availability is missing, stale, contradictory, or unauthorized, the future flow must fail closed.

Fail-closed means:

- no runtime/live/external data read;
- no scan output;
- no real scan loop;
- no production ScanScore computation;
- no Candidate production workflow;
- no Opportunity Push execution;
- no Readiness;
- no point generation;
- no entry / stop / TP / RR;
- no order / execution / auto-trading.

## Watchlist Boundary

Display Slots / 默认六币 cannot prove scan eligibility and cannot become scan universe or batch universe. Watchlist Pool remains the scan candidate boundary.

Risk Action Guard must remain before delivery / Push / Readiness. 踩踏状态禁止机会推送。插针不等于趋势反转。强反转不等于直接反手。
