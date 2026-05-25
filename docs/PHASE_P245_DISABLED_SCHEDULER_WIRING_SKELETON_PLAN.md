# PHASE P245 - Disabled Scheduler Wiring Skeleton Plan

## Stage Position

This document only plans a disabled scheduler wiring skeleton.

P245 does not implement Java.

## Current Scheduler Audit Conclusion

The current scheduler audit confirms:

- `WatchlistLowFrequencyScanScheduler` already exists.
- The scheduler is disabled by default.
- `enabled=false` returns `DISABLED`.
- `enabled=true` returns `NOT_IMPLEMENTED`.
- The scheduler does not call `LowFrequencyWatchlistScanOrchestrator`.
- The scheduler does not connect `MarketQuoteClient`.
- The scheduler does not perform batch scan.
- The scheduler does not connect API or dashboard output.
- The scheduler does not push, upgrade readiness, or generate points.

## Future Wiring Skeleton Minimal Goal

A future wiring skeleton may only:

- call the single-symbol orchestrator when `enabled=true` and config explicitly allows it
- keep batch scan forbidden
- keep `MarketQuoteClient` forbidden
- keep `ScanScore` forbidden
- keep push, readiness, and point generation forbidden
- keep default configuration disabled
- fail closed when dependencies are missing
- fail closed on exceptions
- return only review-only / blocked / incomplete results

## Acceptance Requirements

Future acceptance must prove:

- compile passes
- targeted scheduler wiring test passes
- disabled default test passes
- enabled but missing orchestrator fails closed
- enabled but no symbol / request fails closed
- no forbidden fields
- no batch method
- no `MarketQuoteClient`
- no push, readiness, or point generation

## Conclusion

If P246 enters Java, it can only be a disabled scheduler wiring skeleton.

P246 is not scheduler activation and is not a real scan loop.
