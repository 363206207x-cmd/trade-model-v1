# PHASE P245 - Scheduler Trigger Authorization Gate

## Stage Position

P245 is the Scheduler Trigger Authorization Gate.

P245 does not implement Java.

P245 does not connect scheduler behavior.

P245 does not enable scheduled tasks.

P245 does not create batch scan.

P245 does not connect `MarketQuoteClient`.

P245 does not create a real scan loop.

## Future P246 Scope Candidates

Future P246 may consider a disabled scheduler wiring skeleton.

That future skeleton must be disabled-by-default.

It may only call the single-symbol orchestrator.

It must not create batch scan.

It must not automatically scan the full watchlist.

It must not use default-six assets.

It must not use Display Slots.

It must not trigger push, readiness, or entry / stop / TP / RR.

It must not trigger order or execution.

It must not connect `MarketQuoteClient`.

It must not generate `ScanScore`.

It must not create Candidate Attention or Promote To Home.

## Future Scheduler Trigger Must Preserve

Any future scheduler trigger must preserve:

- explicit config flag
- fail-closed on missing config
- fail-closed when disabled
- fail-closed on missing orchestrator
- fail-closed on orchestrator exception
- no batch
- no market read
- no push
- no readiness
- no point generation
- no trading action

## Future P246 Candidate Files

These files are only an authorization plan:

- `src/main/java/org/example/trademodel/service/watchlist/WatchlistLowFrequencyScanScheduler.java`

If the existing scheduler file cannot be modified safely, a future gate may instead consider:

- `src/main/java/org/example/trademodel/service/watchlistscan/DisabledLowFrequencyScanSchedulerWiring.java`
- `src/test/java/org/example/trademodel/service/watchlistscan/DisabledLowFrequencyScanSchedulerWiringTest.java`
- `docs/PHASE_P246_DISABLED_SCHEDULER_WIRING_SKELETON_VERIFICATION.md`

## Conclusion

P245 only authorizes future consideration of a disabled scheduler wiring skeleton.

P245 does not authorize scheduler activation.

P245 does not authorize batch, market-read, score, push, readiness, or point generation.
