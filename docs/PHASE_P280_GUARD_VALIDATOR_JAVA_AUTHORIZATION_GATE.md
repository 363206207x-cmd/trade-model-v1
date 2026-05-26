# P280 Guard Validator Java Authorization Gate

P280 does not write Java. It only defines the authorization gate for P281.

## P281 May Do

P281 may implement `RealScanInputContractGuardValidator` skeleton plus targeted test only if separately authorized.

The future P281 Java scope must remain narrow:

- Java skeleton only.
- Targeted test only.
- Validate `RealScanInputContractDTO` safety / Watchlist Pool proof / fail-closed states / review-only flags only.
- No production scan output.
- No market-read wiring.
- No service wiring unless separately authorized.
- No Spring component/service/repository/controller annotations unless separately authorized.

## P281 Must Not Do

P281 must not read market data, call `MarketQuoteClient`, call `BinanceMarketQuoteClient`, create scan output, compute score, create Candidate, trigger Push, upgrade Readiness, generate point, generate entry / stop / TP / RR, or create order/execution/auto-trading behavior.

P281 must not add controller, endpoint, API, scheduler, dashboard wiring, external channel behavior, provider credentials, live provider call, message rendering, or message sending.

## Gate Result

This gate authorizes only a future guard-validator skeleton. It does not authorize real scan implementation, real score implementation, Candidate production workflow, Opportunity Push execution, Readiness, point generation, or trading path.

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary. Risk Action Guard must remain before delivery / Push / Readiness.
