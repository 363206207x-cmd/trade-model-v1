# P235 Runtime Source Service Java Skeleton Verification

## 1. Phase Position

P235 only implements `WatchlistRuntimeSourceService`, `DefaultWatchlistRuntimeSourceService`, and targeted tests.

P235 only composes `RuleConfigWatchlistPoolReadAdapter` and `WatchlistRuntimeSourceGuardValidator`.

P235 is a minimum Runtime Source Service skeleton.

P235 is not a real scan.

P235 is not market data reading.

## 2. Implemented Scope

P235 adds:

- `WatchlistRuntimeSourceService`
- `DefaultWatchlistRuntimeSourceService`
- `DefaultWatchlistRuntimeSourceServiceTest`

The service receives `RuntimeSourceReadRequestDTO`.

The service returns `RuntimeSourceReadResultDTO`.

The service keeps fail-closed behavior for null request, missing adapter, missing guard, missing read result, null runtime source, and missing guard result.

All service outputs preserve:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `opportunityPushAllowed=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`

## 3. Explicit Non-Implementation

P235 has no schema change.

P235 has no mapper change.

P235 has no API / controller / dashboard wiring.

P235 has no `MarketQuoteClient`.

P235 has no `BinanceMarketQuoteClient`.

P235 has no scheduler.

P235 has no scan loop.

P235 has no real scan.

P235 has no real `ScanScore` computation.

P235 has no Candidate Attention workflow.

P235 has no Promote To Home workflow.

P235 has no opportunity push execution.

P235 has no readiness upgrade.

P235 has no real entry / stop / TP / RR.

P235 has no order / execution / auto-trading.

## 4. Verification Commands

```bash
./mvnw -q -Dtest=DefaultWatchlistRuntimeSourceServiceTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
git diff --check
git diff --name-status main...HEAD
git status
```

## 5. Verification Results

- targeted test: passed with `./mvnw -q -Dtest=DefaultWatchlistRuntimeSourceServiceTest test`
- compile: passed with `./mvnw -q -DskipTests compile`
- test-compile: passed with `./mvnw -q -DskipTests test-compile`
- `git diff --check`: passed

## 6. Current Conclusion

P235 is a minimum Runtime Source Service skeleton.

P235 is not a real scan.

P235 is not market data reading.

P235 does not authorize `MarketQuoteClient`, scheduler behavior, scan loop, Opportunity Push, readiness, point generation, order API, execution API, or auto-trading.
