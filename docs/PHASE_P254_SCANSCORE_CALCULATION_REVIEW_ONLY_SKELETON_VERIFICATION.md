# PHASE P254 ScanScore Calculation Review-Only Skeleton Verification

## 1. Phase Positioning

P254 implements only a review-only ScanScore calculation skeleton.

P254 adds only:

- `WatchlistScanScoreCalculator`
- `DefaultWatchlistScanScoreCalculator`
- `DefaultWatchlistScanScoreCalculatorTest`

P254 only calls `WatchlistScanScoreRule.evaluate(...)`.

P254 remains review-only only.

P254 does not implement real score computation.

P254 is not candidate generation, push execution, or readiness.

## 2. Implemented Behavior

`WatchlistScanScoreCalculator` is an interface with one method:

- `calculate(String symbol, BatchWatchlistScanResultEnvelopeDTO batchEnvelope)`

`DefaultWatchlistScanScoreCalculator`:

- requires a `WatchlistScanScoreRule`.
- fails closed when the rule is missing.
- fails closed when the batch envelope is missing.
- fails closed when the symbol is missing.
- calls `WatchlistScanScoreRule.evaluate(...)` once on the normal path.
- fails closed when the rule returns null.
- fails closed when the rule throws.
- fails closed when the rule returns unsafe output.
- returns safe review-only output unchanged.

All safe outputs must preserve:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `opportunityPushAllowed=false`
- `candidateAttentionAllowed=false`
- `promoteToHomeAllowed=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`

## 3. Safety Boundary

P254 does not include:

- real score computation.
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
./mvnw -q -Dtest=DefaultWatchlistScanScoreCalculatorTest test
```

Result: PASS.

The test proves:

- missing scoreRule fails closed.
- null batch envelope fails closed.
- blank symbol fails closed.
- scoreRule returns null fails closed.
- scoreRule throws fails closed.
- unsafe score result fails closed.
- safe review-only score result is returned.
- all outputs preserve no-execution defaults.
- calculator only calls rule once.
- calculator declares no forbidden fields.
- calculator has no forbidden scheduler / market / push / readiness / order / execute / trade method names.

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

P254 is a review-only ScanScore calculation skeleton.

P254 is not real scoring.

P254 is not candidate generation.

P254 is not push execution or readiness.

P254 does not authorize production ScanScore computation, `MarketQuoteClient`, `BinanceMarketQuoteClient`, scheduler, scan loop, Candidate Attention, Promote To Home, Opportunity Push, readiness, point generation, order, execution, or auto-trading.
