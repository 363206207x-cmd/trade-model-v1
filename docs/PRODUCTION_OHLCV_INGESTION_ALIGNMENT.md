# Production OHLCV Ingestion Alignment

## Decision

- Base main commit: `97a31fa7` or newer
- Package: P0 Production OHLCV Ingestion Ownership, Provenance and Fail-Closed Contract
- `OHLCV_AUTHORITATIVE_WRITER`: `PASS`
- `OHLCV_PROVENANCE_STATUS`: `PASS`
- `OHLCV_IDEMPOTENCY_STATUS`: `PASS`
- `OHLCV_FRESHNESS_STATUS`: `PASS`
- `EXECUTION_PLAN_RUNTIME_OHLCV_STATUS`: `PASS`
- `OPPORTUNITY_RUNTIME_OHLCV_STATUS`: `PASS`
- `PUBLIC_PROVIDER_LIVE_SMOKE`: `SKIPPED`
- `GAP_P0_002`: `CLOSED`
- `PRODUCTION_READINESS`: `BLOCKED`

This package closes the missing runtime-writer contract. It does not prove live provider operation, four-timeframe decision convergence, production PostgreSQL V4 execution, or production deployment readiness.

## Authoritative Chain

The normal runtime path is now:

1. `BinancePublicOhlcvProvider` calls only the public spot `/api/v3/klines` source through `RealMarketDataFetcherService`.
2. The adapter maps closed provider rows into typed `OhlcvBarInput` values and returns an explicit source state.
3. `PersistedOhlcvIngestionService` is the authoritative normal runtime writer for `tm_persisted_ohlcv_bar`.
4. The writer validates the complete batch before writes, calculates deterministic content hashes, records provenance/freshness, and persists through `PersistedOhlcvBarMapper`.
5. `PersistedOhlcvQueryService` and `RuntimeKlineContextAssemblyService` admit only source-owned, quality-OK, fresh, closed, geometrically valid bars.
6. The execution-plan boundary integration consumes bars written by the authoritative writer rather than test SQL.
7. `OpportunityLogService` consumes the same runtime-written bars and rejects missing provenance, stale source status, invalid geometry, invalid timestamps, or non-OK quality.

No controller, Dashboard service, execution-plan service, or opportunity evaluator writes OHLCV rows.

## Persisted Contract

Every authoritative row contains the existing provider/symbol/timeframe/OHLCV fields plus:

- `fetch_time`
- `source_status`
- `freshness_status`
- `provenance_version`
- `ingestion_run_id`
- source endpoint, batch, trace, numeric source version, ingestion time, quality state, and deterministic raw-content hash

Flyway `V4__ohlcv_ingestion_provenance.sql` adds the audit columns and ingestion-run index without rewriting V1. The H2 bootstrap schema has the same columns.

## Validation and Fail-Closed Rules

The writer rejects a batch before normal writes when any required provider/provenance value is absent, the symbol is blank, the timeframe is outside `5m/15m/1h/4h`, timestamp order is invalid, a bar is too far in the future, a bar is open, a price is non-positive, volume is negative/missing, or OHLC geometry is inconsistent.

Explicit source states are:

`NOT_CONFIGURED`, `WAITING_SYNC`, `READY`, `EMPTY_CONFIRMED`, `STALE`, `DEGRADED`, `ERROR`, and `DISABLED`.

Only a `READY` provider batch can be written. A bar fetched outside the freshness policy is persisted with `freshness_status=STALE`, returned as `STALE`, and cannot become a fresh plan or opportunity input. Missing, disabled, empty, degraded, and error provider results write no bars and never become healthy evidence.

## Idempotency

The database unique key remains equivalent to provider + symbol + timeframe + open time, with market type included to preserve source ownership.

- Identical repeated content produces no duplicate row and returns an idempotent count.
- Conflicting content for an existing source key returns `CONFLICTING_DUPLICATE_CONTENT` and writes no replacement.
- Conflicting duplicates inside one batch fail before persistence.
- Historical authoritative content is never silently overwritten.

## Scheduler and Provider Safety

- Dedicated OHLCV ingestion scheduler default: `OFF` in default and production config.
- Public provider external calls default: `OFF`.
- Production opt-in requires the global scheduler switch, scheduler classification, provider enablement, and external-call opt-in.
- Symbol allowlist is bounded to one or two symbols.
- Timeframes must be exactly `5m,15m,1h,4h`.
- Bar requests are bounded to at most 500 rows.
- Same symbol/timeframe overlap is rejected as `INGESTION_ALREADY_RUNNING`.
- In-memory source health records last success, last failure, failure reason, current state, and next-run waiting state.

The adapter has no API-key constructor and contains no private account, position, withdrawal, or order endpoint.

## Test Evidence

Focused tests prove:

- public provider mapping and disabled no-call behavior
- authoritative persistence and decimal mapping
- identical-content idempotency and conflicting-content rejection
- geometry, timestamp, future/provenance, and freshness fail-closed behavior
- all four product timeframes persist
- execution-plan boundary generation reads authoritative runtime-ingested bars
- opportunity evaluation reads authoritative runtime-ingested bars
- no Controller or Dashboard direct OHLCV writer
- production scheduler default-off and same-key overlap prevention
- public ingestion requires no API key and references no private/order endpoint
- PostgreSQL V4 SQL uses PostgreSQL-compatible types/syntax and the migration smoke tests require the new index/history version when PostgreSQL is available

Validation result:

- focused OHLCV/provider/scheduler/plan/opportunity tests: `PASS`
- `AnalysisDecisionExecutionPlanIntegrationTest`: `PASS`
- `DashboardControllerTest`: `PASS`
- full `./mvnw test -q`: `PASS`
- PostgreSQL/Testcontainers runtime execution: `SKIPPED` because Docker is unavailable in this environment

The PostgreSQL skip is not reported as PASS. Historical controlled PostgreSQL evidence covers V1/V2/V3; V4 requires a later controlled migration evidence run.

## Live Smoke

`PUBLIC_PROVIDER_LIVE_SMOKE` is `SKIPPED`. No opt-in live flag was supplied, no external provider call was made, no raw response was committed, and no secret was read or printed. This does not weaken the code-contract closure and does not count as real provider runtime evidence.

## Safety Boundary

- no auto-open
- no auto-close
- no auto-reverse
- no order execution
- no auto-trading
- no external Push/Telegram send
- no private Binance endpoint
- no trading or withdrawal permission
- no production server or production database access
- no secret committed or printed

Production deployment remains blocked.
