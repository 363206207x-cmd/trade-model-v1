# Push Recheck Quote-Unavailable Guard

Package: PDR-PF7 Push Recheck Quote-Unavailable Guard
Date: 2026-07-07
Branch: `codex/pdr-pf7-push-recheck-quote-unavailable-guard`
Current main commit reviewed: `8b5ad3000f254939e1e453ce93f8871da321ed93`
Production readiness: BLOCKED
Production deployment: cannot proceed

## Scope

This package locks the Push Recheck quote-unavailable behavior with focused tests and status documentation. It proves that when a caller omits `currentPrice` and `MarketQuoteClient` cannot provide a usable price, Push Recheck fails closed as review-only/non-executable and never promotes a push snapshot to an executable state.

No production code change was required. The implementation already resolved unavailable quote paths to `QUOTE_UNAVAILABLE` or `PRICE_REQUIRED` and wrote an `INVALIDATED` recheck result. This package adds regression coverage and records the evidence.

No production DB was accessed. No real secrets were accessed. No provider live call was made. No destructive operation was run. No runtime trading behavior was added.

## Current Behavior Reviewed

`PushRecheckServiceImpl#recheck(pushId, currentPrice, command)` behaves as follows when `currentPrice` is null or non-positive:

1. Attempts to resolve a current price through `MarketQuoteClient#fetch24hTicker(symbol)` when the push snapshot has a symbol and a quote client is available.
2. If the quote is unavailable, empty, invalid, or throws, records `fail_reason_json.code = QUOTE_UNAVAILABLE`.
3. If the push snapshot symbol is missing, records `fail_reason_json.code = PRICE_REQUIRED` without calling the quote client.
4. Produces `RecheckStatusEnum.INVALIDATED` and updates `push_snapshot.push_status` to `RECHECK_INVALIDATED`.
5. Keeps `RecheckResult.valid` false, `reviewPassed` false, `reviewOnly` true, `notExecutable` true, `notOrderExecution` true, `notAutoTrading` true, `notUserPositionCreation` true, and `notPositionMutation` true.

## Test Cases Added Or Confirmed

| Case | Test method | Expected code | Recheck status | Push status | Result |
|---|---|---|---|---|---|
| `currentPrice` null + `MarketQuoteClient` returns `Optional.empty()` | `missingCurrentPriceQuoteEmptyFailsClosedWithQuoteUnavailable` | `QUOTE_UNAVAILABLE` | `INVALIDATED` | `RECHECK_INVALIDATED` | Added |
| `currentPrice` null + quote snapshot has null `lastPrice` | `missingCurrentPriceQuoteNullLastPriceFailsClosedWithQuoteUnavailable` | `QUOTE_UNAVAILABLE` | `INVALIDATED` | `RECHECK_INVALIDATED` | Added |
| `currentPrice` null + `MarketQuoteClient` throws `RuntimeException` | `missingCurrentPriceQuoteThrowsFailsClosedWithQuoteUnavailable` | `QUOTE_UNAVAILABLE` | `INVALIDATED` | `RECHECK_INVALIDATED` | Added |
| `currentPrice` null + snapshot symbol missing | `missingCurrentPriceSnapshotSymbolMissingFailsClosedWithPriceRequired` | `PRICE_REQUIRED` | `INVALIDATED` | `RECHECK_INVALIDATED` | Added |
| Valid `currentPrice` provided | `providedCurrentPriceKeepsExistingBehaviorAndDoesNotFetchQuote` | none | existing review-passed behavior | `RECHECK_REVIEW_PASSED` | Added |
| Valid quote fallback when `currentPrice` missing | `missingCurrentPriceFetchesCurrentQuoteReadOnly` | none | existing review-passed behavior | unchanged | Existing test strengthened to use `Optional` import |

## Exact Fail Reason Codes

The guard locks the following `fail_reason_json` codes:

- `QUOTE_UNAVAILABLE`: quote client unavailable, empty quote, null/non-positive quote last price, or quote client exception.
- `PRICE_REQUIRED`: push snapshot lacks a symbol, so a quote lookup cannot be safely attempted and caller did not provide a valid `currentPrice`.

Both codes produce `RecheckStatusEnum.INVALIDATED`, `reviewPassed=false`, and `push_snapshot.push_status=RECHECK_INVALIDATED` in the tested missing-price paths.

## Non-Executable Safety Confirmation

The added tests assert `assertSafeReviewOnlyResult(...)`, which verifies:

- `valid=false`
- `reviewOnly=true`
- `manualReviewOnly=true`
- `notTradeInstruction=true`
- `notExecutable=true`
- `notAutoTrading=true`
- `notOrderExecution=true`
- `notUserPositionCreation=true`
- `notPositionMutation=true`
- `notTradingAuthorization=true`

The package adds no order API, no auto-open, no auto-close, no auto-reverse, no auto-trading, no external Push send, no fake positions, and no fake review records.

## Validation Evidence

Targeted validation run before this document was created:

```text
./mvnw -q -Dtest=PushRecheckServiceImplTest test: PASS
```

Final package validation:

```text
./mvnw -q -Dtest=PushRecheckServiceImplTest test: PASS
./mvnw test -q: PASS
git diff --check: PASS
bash scripts/check-workflow-contract.sh: PASS
bash scripts/v1-delivery-check.sh: PASS
bash scripts/v1-state.sh: PASS (script exits successfully; branch-local dirty/unmerged blockers are expected until this PR is merged/effective)
YAML parse: PASS
```

## Production Readiness Decision

Production readiness remains BLOCKED.

Production deployment cannot proceed.

PDR-PF7 closes one focused Push Recheck quote-unavailable guard, but production readiness still lacks complete production database, rollback, secrets manager, HTTPS/reverse-proxy, live provider, observability, rate limiting, and release-gate evidence.

## Next Remediation Recommendation

Recommended next package: `PDR-PF8 Production Release Gate Closure` only after the remaining evidence packages are supplied, or a narrower explicitly scoped package for one unresolved blocker such as secrets manager implementation, HTTPS/reverse proxy, backup/restore evidence, live provider PASS evidence, rate limiting, or observability.

## Prohibited Items Preserved

- no auto-open
- no auto-close
- no auto-reverse
- no order execution
- no auto-trading
- no external push send
- no fake positions
- no fake review records
- no production-ready claim
