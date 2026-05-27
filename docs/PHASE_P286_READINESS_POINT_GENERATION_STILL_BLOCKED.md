# P286 Readiness Point Generation Still Blocked

P286 does not authorize Readiness, point generation, or trading path behavior.

## Still Blocked

The following remain blocked:

- Readiness upgrade;
- ExecutionPlan readiness;
- point generation;
- real entry generation;
- real stop generation;
- real TP generation;
- real RR generation;
- order API;
- execution API;
- auto-trading;
- automatic close;
- automatic reverse;
- automatic stop modification.

## Request Contract Limit

Future `MarketReadRequestDTO` must remain review-only and not a trade instruction. It cannot generate entry-stop-TP-RR, cannot upgrade Readiness, cannot produce executable plan state, and cannot create order/execution behavior.

If required field/source/proof/timeframe/timestamp/stale policy/missing-data policy is missing or invalid, the future request must fail closed and cannot create a point.

## Safety Rules

- `reviewOnly=true` must be preserved.
- `notTradeInstruction=true` must be preserved.
- Source must be a GuardValidator-approved `RealScanInputContractDTO`.
- Display Slots / 默认六币 cannot be scan universe or batch universe.
- Watchlist Pool remains the scan candidate boundary.
- Risk Action Guard must remain before delivery / Push / Readiness.
- 踩踏状态禁止机会推送。
- 插针不等于趋势反转。
- 强反转不等于直接反手。
