# PHASE P222 Production Runtime Source Read Adapter Plan

## 1. Phase Position

P222 is the plan document for future production runtime source read adapters.

P222 only defines a future adapter plan.

P222 does not implement Java.

P222 does not read DB data.

P222 does not connect `MarketQuoteClient`.

P222 does not enable scheduler behavior.

P222 does not create a real scan.

## 2. Future Adapter Candidates

The following names are only document-level candidates and are not implemented in P222:

- `WatchlistPoolReadAdapter`
- `RuntimeSourceReadAdapter`
- `MarketQuoteRuntimeSourceAdapter`
- `SchedulerTriggeredRuntimeSourceAdapter`
- `ProductionRuntimeSourceAssembler`

## 3. Future Adapter Input Boundary

Any future adapter work must keep these input boundaries:

- scan universe must come from Watchlist Pool.
- Display Slots are not scan universe.
- the default fixed-six assets are not scan universe.
- non-watchlist assets must fail closed.
- adapters only read or assemble source state.
- adapters do not push.
- adapters do not score.
- adapters do not upgrade readiness.

## 4. Future Adapter Output Boundary

Future adapters may only output safe source state such as:

- `WatchlistRuntimeSourceDTO`
- source unavailable
- incomplete
- stale review-only
- available review-only
- blocking reasons
- missing fields
- stale fields

## 5. Future Adapter Must Not Do

Future adapters must not directly:

- generate `WatchlistScanResultDTO`.
- generate ScanScore.
- create Candidate Attention.
- Promote To Home.
- create Opportunity Push execution.
- generate entry / stop / TP / RR.
- upgrade readiness.
- create trading action.

## 6. Conclusion

P222 does not authorize P223 to directly implement adapter Java.

If P223 continues this track, it should first define an adapter authorization gate / interface skeleton plan.
