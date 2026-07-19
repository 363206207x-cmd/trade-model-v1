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

## Persisted OHLCV Due State

The scan refresh port uses a dedicated authoritative-read query for due-state
decisions. It binds provider symbol, timeframe, persisted source provider, and
`provider_market_type`; the existing generic query remains unchanged for its
other consumers. A row can produce `NO_NEW_CLOSED_BAR_DUE / READY / FRESH`
only when it is closed, non-deleted, source `READY`, freshness `FRESH`, quality
`OK`, has a positive persisted source version, matches the requested provider
and market type, and its next timeframe close is not yet due.

Persisted market types are `SPOT` and `USDT_PERP`. A recent `SPOT` row cannot
suppress a canonical `PERPETUAL` refresh. `ProviderRefreshObservation` retains
canonical instrument, provider, provider market type, timeframe, and mapping
source version for every OHLCV timeframe, including unavailable results.

Logical caller timeout is not a provider snapshot failure. It cannot update
the shared flight's physical timeout state or remote health. Local admission,
budget, concurrency, and configuration failures likewise do not increment the
remote provider circuit. Remote transport, physical timeout, `5xx`, malformed
response, and invalid payload remain fail-closed circuit inputs.

Circuit admission is attempt-owned. A HALF_OPEN physical attempt carries one
idempotent permit until the attempt actually ends. Local rejection releases
the probe, `429` or auth settles it as remote-reachable, remote failure reopens
the circuit, and READY/`EMPTY_CONFIRMED` closes it. Caller timeout and joined
waiters cannot release the owner permit. Thus a completed snapshot path cannot
strand the provider in a permanently claimed HALF_OPEN state.

Decision Cutoff Time is deferred to P3-I1. Production readiness remains
`BLOCKED`.
