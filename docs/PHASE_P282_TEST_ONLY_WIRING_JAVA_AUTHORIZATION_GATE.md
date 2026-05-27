# P282 Test-Only Wiring Java Authorization Gate

P282 opens a narrow authorization gate for P283 only.

Future recommended next package after P282 should be P283 RealScanInputContractGuardValidator Test-Only Wiring Skeleton, but only test-only and targeted-test-only.

## P283 May Do

P283 may enter a test-only wiring skeleton that wires `RealScanInputContractDTO` -> `RealScanInputContractGuardValidator` in tests only.

P283 may use the already existing P281 classes:

- `RealScanInputContractGuardValidator`
- `DefaultRealScanInputContractGuardValidator`
- `RealScanInputContractDTO`

P283 may add only targeted tests or test-owned skeleton code if the P283 issue explicitly lists the files and keeps the work test-only.

## P283 Must Not Do

P283 must not add production service wiring. It must not add Spring annotations, controller, endpoint, API, scheduler, dashboard wiring, mapper, repository, DB write, migration, schema/config changes, or runtime data paths.

P283 must not:

- Wire `MarketQuoteClient` / `BinanceMarketQuoteClient`.
- Read runtime/live/external data.
- Create scan output.
- Create real scan loop.
- Implement production ScanScore computation.
- Implement Candidate production workflow.
- Implement Promote To Home runtime logic.
- Implement Opportunity Push execution.
- Implement external channel behavior.
- Handle provider credentials.
- Make live provider calls.
- Render or send messages.
- Upgrade Readiness.
- Generate point generation.
- Generate real entry / stop / TP / RR.
- Call order or execution APIs.
- Enable auto-trading.

P283 must keep Display Slots / 默认六币 out of scan universe and batch universe. Watchlist Pool remains the scan candidate boundary. Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
