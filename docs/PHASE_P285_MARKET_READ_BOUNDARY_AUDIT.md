# P285 Market-Read Boundary Audit

P285 keeps market-read work in audit-only documentation form.

## Closure Baseline

P284 merged as `cbcc34d`.

P284 was docs-only / boundary-only. It closed the P283 test-only wiring and defined that market-read boundary work must begin with a docs-only boundary audit / authorization gate, not implementation.

P283 test-only wiring remains completed and safe: it added `RealScanInputContractGuardValidatorTestOnlyWiringTest`, CI passed before merge, and the work remained test-only / targeted-test-only. The test-only wiring proves that valid-looking `RealScanInputContractDTO` input can pass into `RealScanInputContractGuardValidator` and remain `REVIEW_ONLY`, while missing Watchlist Pool proof remains `BLOCKED_MISSING_WATCHLIST_PROOF`, non-watchlist input remains `BLOCKED_NOT_WATCHLIST`, null input remains `INCOMPLETE`, blocked input cannot be upgraded to `REVIEW_ONLY`, and outputs preserve `manualReviewRequired=true` and `notTradeInstruction=true`.

## Audit Boundary

The market-read boundary audit asks what must be proven before any future runtime market read can be considered:

- the symbol is inside Watchlist Pool;
- Watchlist Pool membership proof exists and names its source;
- watchlist config version is present;
- requested scan reason is present and review-only;
- requested timeframes are explicit and authorized;
- scan timestamp exists and is not stale;
- stale/missing behavior is defined;
- risk blockers are carried forward;
- review-only flags are preserved;
- guard validation result remains authoritative.

This audit does not create a runtime reader, service, adapter, mapper, repository, scheduler, controller, endpoint, dashboard path, scan output, or scan loop.

## Fail-Closed Rule

Future market-read design must fail closed when any of the following is missing, stale, contradictory, or unauthorized:

- Watchlist Pool proof;
- proof source;
- watchlist config version;
- requested timeframe;
- scan timestamp;
- data availability state;
- risk blocker state;
- review-only flag;
- guard validation result.

Fail-closed means no market read, no scan output, no score, no Candidate, no Push, no Readiness, no point generation, and no trading action.

## Non-Negotiable Boundaries

- No `MarketQuoteClient` / `BinanceMarketQuoteClient` wiring.
- No runtime/live/external data read.
- No scan output creation.
- No real scan loop.
- No production ScanScore computation.
- No Candidate production workflow.
- No Opportunity Push execution.
- No scheduler/API/dashboard wiring.
- No external channel behavior, provider credentials, live provider call, message rendering, or sending.
- No Readiness / point generation / entry-stop-TP-RR / order / execution / auto-trading.
- Display Slots / 默认六币 cannot prove scan eligibility and cannot be scan universe or batch universe.
- Watchlist Pool remains the scan candidate boundary.
- Risk Action Guard must remain before delivery / Push / Readiness.
- 踩踏状态禁止机会推送。
- 插针不等于趋势反转。
- 强反转不等于直接反手。
