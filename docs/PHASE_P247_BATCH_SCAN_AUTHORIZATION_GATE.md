# PHASE P247 - Batch Scan Authorization Gate

## 1. Phase Position

P247 is the Batch Scan Authorization Gate.

P247 does not implement Java.

P247 does not create batch scan.

P247 does not connect scheduler.

P247 does not connect `MarketQuoteClient`.

P247 does not generate `ScanScore`.

## 2. Future Batch Scan Scope

Future work may consider:

- Batch scan plan.
- Single-symbol orchestrator to batch orchestrator authorization gate.
- Batch result envelope plan.
- If Java is introduced, it requires a separate B/C or C risk authorization gate.
- Future batch can only call the single-symbol orchestrator.
- Future batch can only output review-only / blocked / incomplete results.
- Future batch must not generate score / candidate / push / readiness / point generation.

## 3. Future Batch Must Preserve

Future batch work must preserve:

- Watchlist Pool only.
- No Display Slots universe.
- No default-six universe.
- No arbitrary market universe.
- Non-watchlist fail-closed.
- Empty batch input fail-closed.
- Batch disabled-by-default.
- No scheduler trigger unless a separate scheduler gate authorizes it.
- No `MarketQuoteClient` unless a separate market gate authorizes it.
- No `ScanScore` unless a separate score gate authorizes it.
- No push.
- No readiness.
- No entry / stop / TP / RR.
- No trading action.

## 4. Future Batch Still Forbidden

Future batch work must not:

- Scan the default six symbols.
- Scan Display Slots.
- Scan the full market.
- Auto-push.
- Upgrade readiness.
- Generate point levels.
- Place orders or execute trades.
- Combine scheduler / `MarketQuoteClient` in the same PR.

## 5. Conclusion

P248 should not directly implement batch Java.

P248 should first do a batch scan Java authorization gate or a batch envelope plan.
