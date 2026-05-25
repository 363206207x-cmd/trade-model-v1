# PHASE P244 - Scheduler Batch Market-Read Scope Audit

## Stage Position

P244 is the scheduler / batch / market-read scope audit.

P244 does not implement Java.

P244 does not enable scheduler behavior.

P244 does not create batch scan.

P244 does not connect `MarketQuoteClient`.

P244 does not create a real scan loop.

## Current State Audit

This audit is based only on repository files. P244 does not run services, query databases, read runtime data, read live data, or read external data.

- `LowFrequencyWatchlistScanOrchestrator` exists.
- `DefaultLowFrequencyWatchlistScanOrchestrator` exists.
- `WatchlistLowFrequencyScanScheduler` exists.
- `WatchlistLowFrequencyScanScheduler` is disabled by default through its default constructor.
- `WatchlistLowFrequencyScanScheduler.runScheduledScan()` returns `DISABLED` when `enabled=false`.
- `WatchlistLowFrequencyScanScheduler.runScheduledScan()` returns `NOT_IMPLEMENTED` when `enabled=true`.
- The scheduler does not call `LowFrequencyWatchlistScanOrchestrator`.
- The scheduler-to-orchestrator path is not authorized.
- No batch scan exists.
- No `MarketQuoteClient` read exists in this path.
- No real scan loop exists.
- No `ScanScore` production output exists.
- No Candidate Attention workflow exists.
- No Promote To Home workflow exists.
- No Opportunity Push execution exists.
- No readiness upgrade exists.
- No real entry / stop / TP / RR generation exists.

## Scope Conclusion

Scheduler, batch, and market-read must remain three separate lines.

They must not be implemented together in one Java PR.

Scheduler must not directly trigger push, readiness, or point generation.

Batch must not use default-six assets or Display Slots as its universe.

Market-read must not bypass Watchlist Pool.

## Recommended Future Order

- P245 scheduler trigger authorization gate
- P246 disabled scheduler wiring skeleton
- P247 batch scan authorization gate
- P248 single-symbol to batch orchestrator skeleton
- P249 `MarketQuoteClient` read-only authorization gate
- P250 `MarketQuoteClient` adapter skeleton
- P251 `ScanScore` authorization gate
- P252 Candidate Attention / Promote To Home authorization gate
- P253 Opportunity Push authorization gate
- P254 Readiness / point generation authorization gate

The exact numbers are advisory, but the separation is mandatory.

## Conclusion

P244 does not authorize implementation.

P244 only provides audit evidence for future layered gates.
