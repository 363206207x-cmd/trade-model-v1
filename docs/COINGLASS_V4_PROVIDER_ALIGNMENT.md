# CoinGlass v4 Provider Alignment

## Scope

CG-1 adds a CoinGlass v4 adapter below the existing unified provider coordinator:

```text
DefaultProviderDatasetRefreshPort
  -> CoinGlassDerivativesSnapshotService
  -> four dataset snapshot services
  -> ProviderCallCoordinator
  -> CoinGlassV4ProviderAdapter
  -> CoinGlassV4Client
```

The four independent request/cache identities are:

- `COINGLASS_OPEN_INTEREST`
- `COINGLASS_FUNDING`
- `COINGLASS_LIQUIDATION`
- `COINGLASS_LONG_SHORT_RATIO`

CG-1 normalizes provider data into `DerivativesRiskSnapshot` and remains free of business judgment. The later, separately scoped BIZ-1 package is now implemented pending merge and owns all downstream score/decision/state/plan/monitor/push interpretation without changing CG-1 transport behavior.

## Authentication and Secret Safety

- Environment key: `COINGLASS_API_KEY`.
- Official request header: `CG-API-KEY`.
- The key is sent only as a request header by the JDK transport.
- The key is ignored by Jackson serialization and absent from request URIs, logs, audit metadata, errors, fixtures, and docs.
- Complete response bodies are transient only and are not persisted.
- Fixtures are fixed synthetic contract fixtures without account data or headers.

## Dataset Isolation

Each dataset has its own `ProviderRequestKey`, cache entry, freshness result, endpoint capability ID, and error state. Calls share the provider's real aggregate rate budget, so every endpoint request increments the same CoinGlass API-key budget without pretending each endpoint has an independent external quota.

One failed dataset does not erase another successful dataset. Assembly records:

- `availableDatasets`
- `missingDatasets`
- `degradedDatasets`
- aggregate source/freshness status
- provider/fetch/expiry times
- trace ID
- per-field endpoint/response-field conversion sources

A partial snapshot is `DEGRADED`, never full `READY`. Missing OI, Funding, liquidation, or long/short data remains null and never becomes zero, low risk, normal, or connected.

## Cache, Cadence, and Budget

- P0 position derivatives: 60s under normal profiles.
- P1 core derivatives: 60s under Standard/High.
- P2 candidate: 60-120s by active profile.
- P3 pool: 300-900s by active profile.
- Emergency minimum refresh gap: 40s.
- Stable `LATEST` keys allow Dashboard, Decision, Position Monitor, and Push Recheck consumers to reuse one dataset snapshot within their requested freshness window.
- Position price scans remain independent and cannot trigger CoinGlass refreshes.
- Default advertised budget: 300 RPM, internal ratio 0.80, normal internal budget 240 RPM, reserved capacity 60 RPM.
- Pool is rejected before candidate/core/position; the price-provider budget remains separate.

## Retry and Health

The existing coordinator owns retry, single-flight, stale fallback, circuit breaker, audit, and rate-budget behavior:

- authentication errors do not retry;
- 429 records `Retry-After` and does not busy-loop;
- 5xx receives at most two retries;
- timeout receives at most one retry;
- same symbol/dataset shares cache and single-flight work;
- stale fallback is explicitly `STALE`.

`CoinGlassProviderHealthService` stores sanitized endpoint status, HTTP status, provider status code, fetch time, reason, and parsed rate-limit metadata in memory. It stores no key, request header, complete raw response, or account information.

## Production Policy

Default and production configuration keep both CoinGlass switches off. A production external call requires:

1. provider enablement;
2. provider external-call enablement;
3. global coordinator and external-call enablement;
4. a nonblank API key;
5. a valid HTTPS base URL;
6. the official `CG-API-KEY` header name;
7. a positive advertised RPM and ratio below 1;
8. the existing explicit provider-scan scheduler policy when scheduling is involved.

The production safety guard fails closed on an unsafe active configuration. CoinGlass configuration alone is not connection proof.

## Controlled Smoke

`scripts/coinglass-provider-smoke.sh` defaults to:

```text
COINGLASS_LIVE_SMOKE: SKIPPED_EXTERNAL_CALLS_DISABLED
```

It can invoke one bounded aggregate snapshot for one symbol only when every explicit gate is present. That snapshot makes at most one request per verified dataset, does not start schedulers, prints only status/count summaries, and runs through the same coordinator path. CG-1 did not enable the smoke and made zero live provider calls.

## Test Evidence

Deterministic tests cover all four official response shapes, six-symbol mapping, invalid symbols, UTC timestamps, missing numeric values, empty and malformed data, partial/complete/stale assembly, authentication, 429, bounded 5xx/timeout retry, cache, single flight, scan separation, 40-second emergency floor, priority budget, refresh-port routing, architecture boundaries, production defaults, script default skip, and no trading/external-send surfaces.

Fixtures under `src/test/resources/provider/coinglass/v4` are explicitly synthetic contract fixtures. They are not live-provider evidence.

## Known Unsupported Fields and Follow-up

- OI 1m change: unavailable from the selected verified endpoint, remains null.
- Funding extremity score: not calculated in the adapter.
- Liquidation spike score: not calculated in the adapter.
- Funding next-settlement time: not exposed by the selected verified endpoint.
- Alternate long/short ratio families: not mixed into the selected global account ratio.
- Business scoring/decision/plan/monitor/push integration: `IMPLEMENTED_PENDING_MERGE`, owned by BIZ-1 and documented in `COINGLASS_DERIVATIVES_BUSINESS_INTEGRATION.md`.
- Live account entitlement and authenticated provider evidence: `SKIPPED`, no key present.

Production readiness remains `BLOCKED`.
