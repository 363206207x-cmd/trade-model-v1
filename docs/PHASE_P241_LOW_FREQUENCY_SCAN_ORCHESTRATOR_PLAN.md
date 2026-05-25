# PHASE P241 - Low-Frequency Scan Orchestrator Plan

## Stage Position

P241 is the plan document for the future Low-Frequency Scan Orchestrator.

P241 does not implement Java.

P241 does not connect `MarketQuoteClient`.

P241 does not enable scheduler behavior.

P241 does not create real scanning.

P241 does not generate `ScanScore`.

## Future Orchestrator Only Responsibility

The future orchestrator may only:

- compose `WatchlistRuntimeSourceService`
- compose `WatchlistScanResultAssembler`
- preserve the protection semantics of `WatchlistScanGuardValidator`
- process single symbol / single request only
- output review-only / blocked / incomplete `WatchlistScanResultDTO`
- avoid market data reads
- avoid score computation
- avoid opportunity creation
- avoid push execution
- avoid readiness upgrade
- avoid point generation

The future orchestrator must not become a scoring engine, market reader, push path, readiness upgrader, or trading action creator.

## Future Data Flow

The future safe data flow is:

```text
RuntimeSourceReadRequestDTO
-> WatchlistRuntimeSourceService
-> WatchlistScanResultAssembler
-> WatchlistScanGuardValidator / preserved guard semantics
-> WatchlistScanResultDTO
-> REVIEW_ONLY / BLOCKED / INCOMPLETE output
```

This flow remains source-to-review-output only.

It does not create a real scan loop.

It does not compute `ScanScore`.

It does not create Candidate Attention, Promote To Home, Push, readiness, point generation, or trading action.

## Future Disabled-By-Default Requirements

Any future orchestrator must remain disabled by default:

- orchestrator defaults to disabled
- scheduler does not call it by default
- API does not expose it by default
- dashboard does not display it by default
- only a later explicit authorization gate may open it
- single symbol comes before multi-symbol batch
- batch scan requires a separate authorization gate
- scheduler scan requires a separate authorization gate
- `MarketQuoteClient` scan requires a separate authorization gate

## Conclusion

P241 does not authorize P242 to directly write Orchestrator Java.

P242 should first do an Orchestrator Java authorization gate, or continue with a disabled-by-default skeleton plan.
