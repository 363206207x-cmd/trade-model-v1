# PHASE P220 Watchlist Runtime Source Test-Only Wiring Verification

## 1. Phase Scope

P220 only implements non-runtime WatchlistRuntimeSource wiring / assembler / tests.

P220 added:

- `WatchlistRuntimeSourceWiringAssembler`
- `DefaultWatchlistRuntimeSourceWiringAssembler`
- `DefaultWatchlistRuntimeSourceWiringAssemblerTest`

P220 is test-only wiring skeleton. It is not runtime source implementation.

## 2. Safety Behavior Verified

The assembler only consumes `WatchlistRuntimeSourceDTO`.

The assembler only returns `WatchlistRuntimeSourceDTO`.

The default assembler calls `WatchlistRuntimeSourceGuardValidator.validate(source)` and returns the guard result.

If the guard is missing, the assembler fails closed with `INCOMPLETE` and a `GUARD_MISSING` / `NULL_GUARD` reason.

If the guard returns null, the assembler fails closed with `INCOMPLETE` and a `GUARD_RESULT_MISSING` reason.

Every output keeps:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `opportunityPushAllowed=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`

The tests verify:

- default assembler with null source returns safe `INCOMPLETE`.
- default assembler blocks non-watchlist source with `BLOCKED_NOT_WATCHLIST`.
- default assembler returns `INCOMPLETE` for unknown membership / missing fields.
- default assembler returns source unavailable safely.
- default assembler returns stale review-only safely.
- default assembler returns available review-only safely and does not allow push, readiness, or trading.
- custom local guard can be injected.
- null guard fails closed.
- guard returning null fails closed.
- all outputs preserve no-push, no-readiness, no-trading defaults.
- `DefaultWatchlistRuntimeSourceWiringAssembler` declares no forbidden `MarketQuoteClient`, `BinanceMarketQuoteClient`, Mapper, Controller, Scheduler, `PushRecheckService`, `PushSnapshotService`, `ExternalRuntimeService`, or `RuntimeDataClient` fields.

## 3. Explicit Non-Implementation Boundary

P220 does not do any of the following:

- No DB read.
- No runtime read.
- No live data read.
- No external data read.
- No MarketQuoteClient.
- No BinanceMarketQuoteClient.
- No scheduler.
- No mapper wiring.
- No service wiring.
- No controller wiring.
- No API wiring.
- No dashboard changes.
- No scan loop.
- No real scan.
- No real ScanScore computation.
- No Candidate Attention workflow.
- No Promote To Home workflow.
- No opportunity promote execution.
- No opportunity push execution.
- No readiness upgrade.
- No real entry / stop / TP / RR.
- No order API.
- No execution API.
- No auto-trading.

## 4. Verification Commands

```bash
./mvnw -q -Dtest=DefaultWatchlistRuntimeSourceWiringAssemblerTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
git diff --check
```

Result:

- `./mvnw -q -Dtest=DefaultWatchlistRuntimeSourceWiringAssemblerTest test`: PASS.
- `./mvnw -q -DskipTests compile`: PASS.
- `./mvnw -q -DskipTests test-compile`: PASS.
- `git diff --check`: PASS.

## 5. Current Conclusion

P220 is only runtime source test-only wiring skeleton.

P220 is not runtime source implementation. It does not read DB, runtime, live, or external data. It does not connect MarketQuoteClient, BinanceMarketQuoteClient, scheduler, mapper, service, controller, API, dashboard, scan loop, ScanScore, Candidate Attention, Promote To Home, Opportunity Push, readiness, order execution, or auto-trading.
