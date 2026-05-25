# PHASE P249 - Batch Fail-Closed Rules

## 1. Phase Position

This document defines batch fail-closed rules.

P249 does not implement Java.

## 2. Batch-Level Fail-Closed

Future batch behavior must fail closed at the batch level:

- `request == null` => INCOMPLETE.
- `requestedSymbols == null` / empty => INCOMPLETE.
- Watchlist disabled => INCOMPLETE.
- Watchlist missing => INCOMPLETE.
- All symbols invalid => INCOMPLETE.
- All symbols non-watchlist => INCOMPLETE.
- Dependencies missing => INCOMPLETE.
- Exception => INCOMPLETE.
- Batch disabled => INCOMPLETE.

## 3. Symbol-Level Fail-Closed

Future batch behavior must fail closed at the symbol level:

- Blank symbol => rejected + `INVALID_SYMBOL`.
- Duplicate symbol => deduped + `DUPLICATE_SYMBOL`.
- Non-watchlist symbol => rejected + `BLOCKED_NOT_WATCHLIST`.
- Single-symbol orchestrator returns null => result incomplete.
- Single-symbol orchestrator throws => result incomplete.
- Unsafe result => result incomplete.
- Stale result => review-only / blocked reason.
- Valid result => review-only only.

## 4. No-Execution Defaults

All batch and symbol outputs must preserve:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `opportunityPushAllowed=false`
- `candidateAttentionAllowed=false`
- `promoteToHomeAllowed=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`

## 5. Conclusion

Fail-closed behavior has priority over opportunity discovery.

Batch must not loosen boundaries to produce opportunities.
