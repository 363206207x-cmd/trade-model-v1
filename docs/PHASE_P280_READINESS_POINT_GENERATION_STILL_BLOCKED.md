# P280 Readiness Point Generation Still Blocked

P280 keeps Readiness, point generation, and trading path blocked.

## Still Blocked

The following remain blocked:

- Readiness upgrade.
- Point generation.
- Real entry / stop / TP / RR.
- Order API.
- Execution API.
- Auto-trading.

P280 does not authorize any future validator to generate points, produce trading instructions, or create executable plans.

`RealScanInputContractDTO` remains not a trade instruction. Future validation must preserve `manualReviewRequired=true` and `notTradeInstruction=true`.

## Safety Reminders

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary.

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
