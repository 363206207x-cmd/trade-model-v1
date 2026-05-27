# P285 Watchlist Proof Source Audit

P285 audits which future sources may prove Watchlist Pool membership. It does not read from any source.

## Proof Requirement

Future market-read or real scan input assembly work must prove Watchlist Pool membership before any protected `RealScanInputContractDTO` can be treated as review-only eligible.

The proof must name:

- symbol;
- membership state;
- proof source;
- watchlist config version;
- proof timestamp;
- source owner;
- stale/missing behavior.

If proof is missing, stale, contradictory, or not owned by an authorized source, the future flow must fail closed.

## Sources That May Prove Membership In Future Work

Potential future proof sources may include only sources separately authorized for proof:

- canonical Watchlist Pool membership source or read model;
- DB-backed Watchlist Pool read after a separate mapper/repository/DB read authorization gate;
- reviewed watchlist config snapshot with version, timestamp, and owner;
- targeted test fixture for test-only packages.

The test fixture source can prove behavior only inside tests. It cannot prove production scan eligibility.

P285 does not authorize DB read/write, mapper, repository, config read, runtime read, service wiring, or production proof retrieval.

## Sources That Cannot Prove Membership

The following cannot prove Watchlist Pool membership:

- Display Slots;
- 默认六币;
- dashboard display order;
- push display order;
- external provider symbols;
- market-data availability alone;
- any runtime/live/external data response;
- any non-watchlist user-facing list.

Display Slots / 默认六币 cannot prove scan eligibility and cannot become scan universe or batch universe.

## Boundary Rules

- Watchlist Pool remains the scan candidate boundary.
- Missing Watchlist Pool proof fails closed.
- Non-watchlist input fails closed.
- Proof cannot be inferred from market-read success.
- Proof cannot be inferred from scan output because P285 creates no scan output.
- Proof cannot be inferred from Candidate, Push, Readiness, or point generation because all remain blocked.

## Still Blocked

P285 does not authorize `MarketQuoteClient` / `BinanceMarketQuoteClient`, runtime/live/external data reads, scan output, real scan loop, production ScanScore computation, Candidate production workflow, Opportunity Push execution, scheduler/API/dashboard wiring, external channel behavior, provider credentials, live provider calls, message rendering, sending, Readiness, point generation, entry-stop-TP-RR, order, execution, or auto-trading.
