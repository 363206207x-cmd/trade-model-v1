# PHASE P244 - Scheduler Trigger Authorization Gate Plan

## Stage Position

This document only plans a scheduler trigger authorization gate.

P244 does not connect scheduler behavior.

## Future Scheduler Trigger Requirements

Any future scheduler trigger must be:

- disabled-by-default
- not running by default
- not automatically scanning the full watchlist
- not automatically scanning default-six assets
- not automatically using Display Slots
- not triggering push
- not triggering readiness
- not triggering entry / stop / TP / RR
- not triggering order / execution
- limited to calling the single-symbol orchestrator unless a separate batch authorization gate exists
- controlled by an explicit config flag
- fail-closed on missing config, disabled config, missing dependencies, exceptions, or incomplete output

## Future Scheduler Java Still Forbidden

Future scheduler Java must still not:

- connect `MarketQuoteClient`
- generate `ScanScore`
- create Candidate Attention
- create Opportunity Push
- upgrade readiness
- generate points

## Conclusion

Scheduler trigger work needs an independent PR.

Scheduler trigger work must not be combined with batch or market-read work.
