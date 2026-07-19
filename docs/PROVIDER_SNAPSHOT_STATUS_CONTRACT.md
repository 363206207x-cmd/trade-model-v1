# Provider Snapshot Status Contract

## Query and Refresh Separation

`ProviderSnapshotQueryService` reads shared snapshots only.
`ProviderSnapshotRefreshService` is the worker/scheduler refresh boundary.

Dashboard Home uses the query path. Opening the page, reading runtime status,
or changing the base profile cannot invoke an adapter. Position Monitor and
Push Recheck may use the controlled refresh path. Consumers can share one
snapshot while applying their own freshness requirement.

`ProviderSnapshotKey` is the stable identity for cache, Single Flight, health,
and retention. It contains provider, dataset, canonical instrument, provider
symbol, timeframe, and source version; it excludes consumer TTL and refresh
`timeBucket`. The latter may appear in planning/audit only. Consequently a
Dashboard 30-second view can read a snapshot refreshed for a stricter 5-second
position consumer, and a cross-bucket provider failure can return the previous
stale snapshot when the formal dataset retention policy still permits it.

Freshness is consumer-specific. Stale retention and eviction are
dataset-specific (`ProviderSnapshotRetentionPolicy`), never extended or
shortened by an arbitrary reader. Expired entries are purged, so advancing
request buckets cannot create an unbounded cache.

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

SPOT and PERPETUAL metadata are exact and isolated. Price/OHLCV requests for an
unsupported perpetual adapter are `NOT_CONFIGURED`; they do not consume a spot
snapshot or relabel spot metadata. Derivatives evidence cannot attach to a spot
instrument. A mixed-market `AnalysisInputBundle` fails closed.

Decision Cutoff Time is deferred to P3-I1. Production readiness remains
`BLOCKED`.
