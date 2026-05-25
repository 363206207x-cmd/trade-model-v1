# PHASE P246 - Disabled Scheduler Wiring Skeleton Verification

## Selected Approach

P246 selects approach B.

The existing `WatchlistLowFrequencyScanScheduler` remains unchanged because it already contains an `@Scheduled` method. P246 adds a separate `DisabledLowFrequencyScanSchedulerWiring` skeleton so this wiring layer can remain explicitly disabled-by-default and can prove that no scheduled method is introduced.

## Scope

P246 only implements a disabled scheduler wiring skeleton.

P246 keeps the wiring disabled by default.

P246 is single-symbol only.

P246 does not create batch scan.

P246 does not connect `MarketQuoteClient`.

P246 does not connect `BinanceMarketQuoteClient`.

P246 does not create a real scan loop.

P246 does not create a real scan.

P246 does not generate `ScanScore`.

P246 does not create Candidate Attention.

P246 does not create Promote To Home.

P246 does not create Opportunity Push execution.

P246 does not upgrade readiness.

P246 does not generate real entry / stop / TP / RR.

P246 does not connect order / execution / auto-trading.

## Implemented Files

- `src/main/java/org/example/trademodel/service/watchlistscan/DisabledLowFrequencyScanSchedulerWiring.java`
- `src/test/java/org/example/trademodel/service/watchlistscan/DisabledLowFrequencyScanSchedulerWiringTest.java`

## Verification Coverage

The targeted tests verify:

- disabled default fails closed
- null request fails closed
- non-Watchlist-Pool request fails closed
- missing orchestrator fails closed
- orchestrator exception fails closed
- orchestrator null result fails closed
- normal orchestrator result is returned safely
- unsafe orchestrator output fails closed
- no batch method
- no `@Scheduled` method
- no forbidden fields for `MarketQuoteClient`, `BinanceMarketQuoteClient`, controller, scheduler, push services, runtime data clients, `DataSource`, or `JdbcTemplate`
- all outputs preserve manual review, not-trade-instruction, no push, no candidate attention, no promote-to-home, no readiness, no trading action, and no entry / stop / TP / RR

## Commands

```bash
./mvnw -q -Dtest=*Scheduler*Test test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
git diff --check
git diff --name-status main...HEAD
git status
```

## Results

- `./mvnw -q -Dtest=*Scheduler*Test test`: passed.
- `./mvnw -q -DskipTests compile`: passed.
- `./mvnw -q -DskipTests test-compile`: passed.
- `git diff --check`: passed.
- `git diff --name-status main...HEAD`: confirmed only P246 authorized files are changed.
- `git status`: reviewed before commit and after commit.

## Conclusion

P246 is a disabled scheduler wiring skeleton.

P246 is not scheduler activation.

P246 is not a real scan.

P246 is not scoring, push execution, readiness, point generation, order execution, or auto-trading.
