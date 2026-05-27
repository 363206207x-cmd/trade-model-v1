# P284 Readiness Point Generation Still Blocked

P284 does not authorize Readiness, point generation, entry-stop-TP-RR, order, execution, or auto-trading.

P283 was only test-only / targeted-test-only. P284 is docs-only closure and market-read boundary scope.

## Still Blocked

The following remain blocked:

- Readiness upgrade.
- Point generation.
- Real entry / stop / TP / RR.
- Real risk-reward generation.
- Order API wiring.
- Execution API wiring.
- Auto-trading.
- Automatic close / reverse / buy / sell.
- Automatic stop modification.

Future real scan input assembly may only define protected input assembly boundaries unless separately authorized. It must not turn review-only input into executable readiness or trading instructions.

## Required Safety Invariants

The guard path must preserve:

- `manualReviewRequired=true`.
- `notTradeInstruction=true`.
- Review-only outputs are not trade instructions.
- Blocked inputs cannot be upgraded to `REVIEW_ONLY`.

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary. Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
