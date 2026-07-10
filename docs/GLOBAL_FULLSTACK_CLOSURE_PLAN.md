# Global Full-Stack Closure Plan

## Objective

Close V1 source, contract, persistence, API, and rendering gaps in dependency order without introducing order execution, automatic position actions, external push send, or fake data.

## Package Order

### Package A: AI Role Contract Alignment

Priority: P0

Scope:

- Define one typed, versioned structure for GPT final, Gemini review, and Grok challenge outputs.
- Persist the structure in a field that Dashboard Home can parse without guessing.
- Keep rule direction authoritative and AI review-only.
- Add producer -> persistence -> Home integration tests using actual orchestrator output.

Done when:

- all three roles render real role-scoped content or one honest role-level empty state;
- no test seeds an impossible payload shape;
- no score/evidence is fabricated.

### Package B: OHLCV Production Ownership and Source Health

Priority: P0

Depends on: none, but should land before plan/replay claims.

Scope:

- Assign a production owner for validated writes to `tm_persisted_ohlcv_bar`.
- Record provider, symbol, timeframe, open time, freshness, and trace/provenance.
- Make no-data, stale, partial, and provider-error states explicit.
- Keep provider calls bounded and schedulers production-default-off.

Done when:

- a controlled run writes real/versioned bars without test SQL inserts;
- plan boundary and OpportunityLog evaluation read those bars;
- unavailable or stale bars fail closed.

### Package C: UserPosition Provenance and Manual Input Truth

Priority: P0

Scope:

- Preserve persisted `sourceType` through mapper/service/VO.
- Enforce Dashboard Home manual-only filtering from the authoritative field.
- Stop silently converting omitted quantity/leverage into factual `1` values, or make those defaults explicit and required.
- Retain manual-open/manual-close-only behavior.

Done when:

- non-manual rows cannot appear in the manual monitor;
- omission/default tests cover browser payload and backend validation;
- full manual lifecycle remains green.

### Package D: External Context No-Data Contract

Priority: P0

Scope:

- Introduce shared states for `NOT_CONFIGURED`, `WAITING_SYNC`, `READY`, `EMPTY_CONFIRMED`, `STALE`, `DEGRADED`, `ERROR`, and `DISABLED`.
- Stop mapping absent imported data to healthy `READY/OK/LOW`.
- Align decision, monitor, Dashboard Home, and provider diagnostics.

Done when:

- every no-data state has source, API, UI, and fail-closed tests;
- no missing provider is interpreted as low risk.

### Package E: Four-Timeframe Decision and Eight-Score Contract

Priority: P1

Depends on: Package B and D.

Scope:

- Replace the fixed `1m`/`5m` convergence shortcut with source-traced `5m/15m/1h/4h` inputs.
- Declare each of the eight scores as decision input or diagnostic-only.
- Remove neutral-looking facts where data is absent, or type them as explicit defaults.
- Make score ordering deterministic.
- Wire confused and AI conflict thresholds to active RuleConfig with fail-closed reads.

Done when:

- requested timeframe and all input timeframes are traceable;
- every score has a tested owner/use;
- threshold changes are versioned and reproducible.

### Package F: Plan and Asset-State Contract Cleanup

Priority: P1

Depends on: Package B and E.

Scope:

- Add dedicated plan readiness/status instead of overloading `validPeriod`.
- Keep leverage/position/invalid condition null until source-backed, or separate policy hints from a plan.
- Implement an explicit legal asset transition authority.
- Add normal ownership for `WAITING_TRIGGER`/`TRIGGERED` or remove them from the claimed V1 lifecycle.

Done when:

- API fields keep one meaning;
- illegal transitions are rejected and audited;
- complete plan proof uses real/versioned OHLCV.

### Package G: Review and Opportunity Truth Consolidation

Priority: P1

Depends on: Package B.

Scope:

- Move ReviewAggregate to authoritative `tm_opportunity_log`.
- Retire or clearly archive legacy `tm_missed_opportunity` endpoints/table usage.
- Define a bounded, default-off due-opportunity evaluation entry point if required.
- Keep review writes manual and non-trading.

Done when:

- Review Center and aggregate return the same outcome truth;
- no duplicate missed-opportunity producer remains active.

### Package H: API Correlation, PostgreSQL Runtime, and UTC

Priority: P1

Scope:

- Propagate one request ID through header, `ApiResponse`, run, trace, and events.
- Isolate/deprecate raw legacy Dashboard contracts while preserving required test anchors.
- Replace/adapt Dashboard overview `DATEDIFF` for PostgreSQL.
- Define UTC persistence and serialization behavior.

Done when:

- correlation equality and cross-timezone tests pass;
- primary PostgreSQL runtime queries execute in a bounded controlled test;
- no client receives a readiness sentence in a period field.

### Package I: Browser and Real Replay Closure

Priority: P2

Depends on: Packages A-H.

Scope:

- Add a licensed/versioned real historical fixture with checksum and provenance.
- Run it through `V1DirectHistoricalReplayAdapter` and actual assembly path.
- Add browser tests for Home, plan fail-close/complete states, AI roles, manual lifecycle, and Review Center.
- Verify hidden legacy safety anchors remain available without dominating the UI.

Done when:

- direct real/versioned replay is deterministic and traceable;
- browser fields match authoritative backend values;
- no-trading static and runtime guards remain green.

### Package J: Scheduler and Data-Lifecycle Operations

Priority: P2

Depends on: stable writers from earlier packages.

Scope:

- Define distributed claim/idempotency for every enabled write scheduler.
- Keep production policy explicit and fail closed; Position Monitor remains default-off.
- Add retention/archival for high-growth audit tables.

Done when:

- multi-instance tests prove no duplicate business writes;
- retention preserves required audit/replay evidence.

## Dependency Graph

```text
A AI contract -----------------------------> I browser/replay
B OHLCV ownership -> E decision/scores -> F plan/state -> I
B OHLCV ownership -> G review/opportunity -> I
C position provenance --------------------> I
D no-data contract -> E -------------------> I
H API/PostgreSQL/UTC ----------------------> I
A-H stable writers ------------------------> J operations
```

## Package Safety Gates

Every package must prove:

- no auto-open
- no auto-close
- no auto-reverse
- no order execution
- no auto-trading
- no external push send
- no fake positions
- no fake review records
- no production-ready claim from local/test evidence alone

## Validation Ladder

1. Focused unit and contract tests for the changed package.
2. `./mvnw test -q`.
3. `bash scripts/check-workflow-contract.sh`.
4. `bash scripts/v1-delivery-check.sh`.
5. `bash scripts/v1-state.sh`.
6. `git diff --check`.
7. Bounded controlled PostgreSQL/provider/browser evidence only when explicitly scoped.

## Closure Decision Rule

- P0 unresolved: global alignment remains blocked.
- P0 closed but real fixture/replay absent: code alignment may improve, but `REAL_DATA_RUNTIME_EVIDENCE` remains `BLOCKED_NO_REAL_DATA`.
- All P0/P1 closed with browser and real/versioned replay PASS: global alignment may be reconsidered for `FULLY_PROVEN`.
- Production readiness remains a separate release gate and is not granted by this plan.
