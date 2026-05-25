# PHASE P249 - Batch Envelope DTO Authorization Gate

## 1. Phase Position

P249 is the Batch Envelope DTO Authorization Gate.

P249 does not implement a DTO.

P249 does not modify `WatchlistScanResultDTO`.

P249 does not add Java.

P249 does not create a batch implementation.

## 2. Future DTO Scope To Consider

Future DTO work may consider:

- `BatchWatchlistScanResultEnvelopeDTO`.
- `BatchWatchlistScanRequestDTO`.
- `BatchWatchlistScanSymbolResultDTO`.

Future work may also decide not to add a new DTO and instead safely reuse a list of existing `WatchlistScanResultDTO` values plus metadata.

Future DTOs may only express review-only / blocked / incomplete results.

Future DTOs must not carry real `ScanScore`.

Future DTOs must not carry Candidate Attention.

Future DTOs must not carry Promote To Home.

Future DTOs must not carry Push execution.

Future DTOs must not carry Readiness upgrade.

Future DTOs must not carry entry / stop / TP / RR.

Future DTOs must not carry order / execution / trading action.

## 3. Future DTO Must Preserve

Future DTOs must keep all of the following safe defaults:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `opportunityPushAllowed=false`
- `candidateAttentionAllowed=false`
- `promoteToHomeAllowed=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`
- `watchlistPoolOnly=true`
- `disabledByDefault=true` or explicit upper-layer control
- `blockingReasons` must be preserved

## 4. Future DTO Prohibitions

Future DTOs are not allowed to include:

- Score fields used as real scores.
- Actionable trade fields.
- Order / execution fields.
- `readiness=true`.
- `push allowed=true`.
- `promote-to-home=true`.

## 5. Conclusion

P249 does not authorize DTO implementation.

Future DTO Java requires a separate B/C authorization gate.
