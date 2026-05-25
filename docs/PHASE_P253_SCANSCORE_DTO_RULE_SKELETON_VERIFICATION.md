# PHASE P253 ScanScore DTO Rule Skeleton Verification

## 1. Phase Positioning

P253 implements only a review-only ScanScore DTO / enum / rule skeleton.

P253 adds only:

- `WatchlistScanScoreDTO`
- `WatchlistScanScoreStatusEnum`
- `WatchlistScanScoreRule`
- `DefaultWatchlistScanScoreRule`
- `DefaultWatchlistScanScoreRuleTest`

P253 is review-only only.

P253 does not implement real score calculation.

P253 is not candidate generation, push execution, or readiness.

## 2. Implemented Behavior

`WatchlistScanScoreStatusEnum` includes only:

- `INCOMPLETE`
- `REVIEW_ONLY`
- `BLOCKED`
- `DISABLED`

It does not include `BUY`, `SELL`, `LONG`, `SHORT`, `READY`, or `EXECUTABLE`.

`WatchlistScanScoreDTO` provides safe factories:

- `incomplete(...)`
- `disabled(...)`
- `reviewOnly(...)`

All DTO outputs preserve:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `opportunityPushAllowed=false`
- `candidateAttentionAllowed=false`
- `promoteToHomeAllowed=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`

`DefaultWatchlistScanScoreRule`:

- consumes only `symbol` and `BatchWatchlistScanResultEnvelopeDTO`.
- fails closed when batch envelope is missing.
- fails closed when symbol is missing.
- fails closed when batch envelope contains disabled / incomplete critical reasons.
- fails closed when batch results are missing.
- fails closed when the symbol result is missing.
- fails closed when the symbol result is unsafe.
- returns a review-only score DTO only when the symbol result is safe `REVIEW_ONLY`.
- uses placeholder zero score values only for review-only skeleton output.
- does not use score to trigger push, readiness, point generation, or trading.

## 3. Safety Boundary

P253 does not include:

- real score calculation.
- real `MarketQuoteClient`.
- real `BinanceMarketQuoteClient`.
- runtime / live / external data reads.
- scheduler.
- scan loop.
- real scan.
- Candidate Attention workflow.
- Promote To Home workflow.
- opportunity push execution.
- readiness.
- real entry / stop / TP / RR.
- order / execution / auto-trading.
- API / controller / dashboard wiring.
- schema change.
- mapper change.

## 4. Test Coverage

Targeted test:

```bash
./mvnw -q -Dtest=DefaultWatchlistScanScoreRuleTest test
```

Result: PASS.

The test proves:

- null batch envelope fails closed.
- blank symbol fails closed.
- batch disabled / incomplete fails closed.
- empty results fail closed.
- symbol not found fails closed.
- unsafe symbol result fails closed.
- safe review-only symbol result returns a review-only score DTO.
- scan score / confidence / data quality are review-only placeholders and do not enable anything.
- all outputs preserve no-execution defaults.
- DTO list fields use defensive copies.
- enum has no `BUY` / `SELL` / `LONG` / `SHORT` / `READY` / `EXECUTABLE`.
- default rule declares no forbidden fields.
- rule methods do not reference scheduler / market / push / readiness / trading.

Compile:

```bash
./mvnw -q -DskipTests compile
```

Result: PASS.

Test compile:

```bash
./mvnw -q -DskipTests test-compile
```

Result: PASS.

Diff check:

```bash
git diff --check
```

Result: PASS.

## 5. Current Conclusion

P253 is a review-only ScanScore skeleton.

P253 is not real scoring.

P253 is not candidate generation.

P253 is not push execution or readiness.

P253 does not authorize real ScanScore computation, `MarketQuoteClient`, `BinanceMarketQuoteClient`, scheduler, scan loop, Candidate Attention, Promote To Home, Opportunity Push, readiness, point generation, order, execution, or auto-trading.
