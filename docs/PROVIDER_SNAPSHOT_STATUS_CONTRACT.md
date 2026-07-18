# Provider Snapshot Status Contract

## Query and Refresh Separation

`ProviderSnapshotQueryService` reads shared snapshots only.
`ProviderSnapshotRefreshService` is the worker/scheduler refresh boundary.

Dashboard Home uses the query path. Opening the page, reading runtime status,
or changing the base profile cannot invoke an adapter. Position Monitor and
Push Recheck may use the controlled refresh path. Consumers can share one
snapshot while applying their own freshness requirement.

## Freshness States

- `FRESH`: return the cached snapshot without a call.
- `STALE_READABLE`: return timestamped stale evidence; a worker may request a refresh.
- `REFRESHING`: an identical key already has a single-flight owner.
- `UNAVAILABLE`: no readable snapshot exists.

## Source States

- `NOT_CONFIGURED`
- `WAITING_SYNC`
- `READY`
- `EMPTY_CONFIRMED`
- `STALE`
- `DEGRADED`
- `ERROR`
- `DISABLED`

Configuration alone is never `READY`. HTTP 200 alone is never `READY`.
Missing values are never converted to zero, normal risk, or low risk. Stale
data retains provider time, fetch time, expiry, age, trace, source version, and
reason codes. Missing/invalid price cannot fabricate PnL.

Every metadata record carries provider, dataset, canonical identity, provider
symbol, timeframe, source/freshness status, provider data time, fetch time,
expiry, snapshot age, reason codes, trace ID, source version, cache use, and
fallback use.

Decision Cutoff Time is deferred to P3-I1. Production readiness remains
`BLOCKED`.
