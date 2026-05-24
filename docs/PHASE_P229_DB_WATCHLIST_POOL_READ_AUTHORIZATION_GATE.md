# PHASE P229 DB Watchlist Pool Read Authorization Gate

## 1. Phase Position

P229 only defines the future authorization gate for DB-backed Watchlist Pool read.

P229 does not implement DB read.

P229 does not write Java.

## 2. Future P230 Candidate Scope

Future P230 may only consider a docs-only DB read plan, mapper audit, or schema audit.

If future work enters Java, it needs a separate B/C authorization gate.

Future DB read must read only Watchlist Pool.

Future DB read must not read Display Slots as the scan universe.

Future DB read must not use the default six symbols as the scan universe.

Future DB read must not generate ScanScore, Candidate Attention, push, readiness, or entry / stop / TP / RR.

## 3. Questions P230 Must Answer First

Before any DB read implementation, P230 must answer:

- Is the current Watchlist Pool source in `RuleConfig` or an independent table?
- Does a watchlist mapper / service already exist?
- Does the read need `tm_rule_config` / `push.watchlist.symbols`?
- How does an empty watchlist fail closed?
- How does a disabled watchlist fail closed?
- How is stale watchlist read expressed?
- How is missing watchlist read expressed?
- How is DB unavailable expressed?
- Does it still return `RuntimeSourceReadResultDTO.sourceUnavailable(...)` or `incomplete(...)`?
- Does it call `WatchlistRuntimeSourceGuardValidator`?
- Could it trigger scheduler? It must not trigger scheduler.

## 4. Future DB Read Still Prohibits

Future DB read still must not:

- Directly read market data.
- Directly connect `MarketQuoteClient`.
- Directly trigger scheduler.
- Enter scan loop.
- Generate `WatchlistScanResultDTO`.
- Generate ScanScore.
- Trigger Candidate Attention.
- Promote To Home.
- Create Opportunity Push execution.
- Generate entry / stop / TP / RR.
- Upgrade readiness.
- Create trading action.

## 5. Conclusion

P230 should not directly write DB read Java.

P230 should first do a DB Watchlist Pool read plan / audit.
