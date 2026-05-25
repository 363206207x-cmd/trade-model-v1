# PHASE P248 - Batch Envelope Plan

## 1. Phase Position

This document plans the batch result envelope.

P248 does not implement a DTO.

P248 does not modify `WatchlistScanResultDTO`.

## 2. Batch Envelope Goal

The future batch envelope should express:

- `batchId`.
- `requestId`.
- `source`.
- `requestedSymbols`.
- `acceptedSymbols`.
- `rejectedSymbols`.
- `resultCount`.
- `blockingReasons`.

The envelope should carry each symbol's `WatchlistScanResultDTO`.

The envelope must preserve review-only / blocked / incomplete semantics.

The envelope must not carry real `ScanScore`.

The envelope must not carry Candidate Attention.

The envelope must not carry Promote To Home.

The envelope must not carry Push execution.

The envelope must not carry Readiness upgrade.

The envelope must not carry entry / stop / TP / RR.

## 3. Suggested Fields

The following fields are only a future plan:

- `batchId`
- `requestId`
- `source`
- `watchlistPoolOnly`
- `disabledByDefault`
- `requestedSymbols`
- `acceptedSymbols`
- `rejectedSymbols`
- `missingSymbols`
- `duplicateSymbols`
- `invalidSymbols`
- `nonWatchlistSymbols`
- `results`
- `blockingReasons`
- `manualReviewRequired`
- `notTradeInstruction`
- `opportunityPushAllowed=false`
- `candidateAttentionAllowed=false`
- `promoteToHomeAllowed=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`

## 4. Fail-Closed Rules

Future batch envelope handling must fail closed:

- Empty `requestedSymbols` => INCOMPLETE / batch-level blocked.
- Missing Watchlist Pool => INCOMPLETE.
- Non-watchlist symbols => reject / blocked reason.
- All symbols rejected => batch-level INCOMPLETE.
- Partial valid symbols => only valid watchlist symbols are processed review-only.
- Exception => batch-level INCOMPLETE.
- Stale data => review-only / blocked reason.
- No symbol should be promoted / pushed / upgraded.

## 5. Conclusion

Batch envelope work needs a future DTO authorization gate or Java skeleton authorization gate.

P248 does not add a DTO.
