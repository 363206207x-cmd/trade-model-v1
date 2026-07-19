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

## Dataset Retention Hard Bound

`DATASET_RETENTION` is a `HARD_UPPER_BOUND`. Cache lookup checks retention
before consumer freshness. At `now >= staleUntil`, the matching entry is
atomically removed and the result is `UNAVAILABLE`; the exact boundary is not
readable. Within retention, `effectiveFreshUntil` is the earlier of the
caller's requested freshness deadline and `staleUntil`.

`CALLER_FRESH_TTL` may shorten freshness or extend it relative to the producer
only within dataset retention. A shorter TTL produces `STALE_READABLE` while
the entry remains retained, and a longer TTL preserves cross-profile sharing
without extending retention. `metadata.expiresAt` is subject to the same cap.
`READY` and `EMPTY_CONFIRMED` snapshots use the same boundary. Lookup and
`purgeExpired` both treat `now >= staleUntil` as expired.

Returned metadata uses the same rule for every refresh, cache hit, read-only
peek, stale fallback, and shared-flight waiter:

```text
expiresAt = min(fetchTime + callerFreshTtl, fetchTime + datasetRetention)
```

An overflowing caller TTL falls back to the retention boundary. Invalid
retention fails closed. The producer's stored short-TTL `expiresAt` does not
permanently shorten a later caller because cache freshness is recalculated from
`fetchTime` with that caller's TTL; the returned metadata is then independently
capped by dataset retention.

## Shared-Flight Caller Ownership

Single Flight shares one physical Provider call/retry chain, one circuit-permit
lifecycle, one budgeted attempt chain, and one cache write. It does not share
caller identity. A successful waiter rereads the shared cache with its own TTL
and receives a newly constructed caller result and request-result audit using
its own trace, priority, base/effective profile, profile reason codes, and
frequency-matrix version. It performs no additional Provider call, budget
reservation, remote-health mutation, circuit settlement, or physical-attempt
audit.

```text
SHARED_FLIGHT_PHYSICAL_RESULT: SHARED_ONCE
SHARED_FLIGHT_CALLER_RESULT: PER_CALLER_REWRAPPED
WAITER_TRACE_OWNERSHIP: CALLER_OWN
WAITER_PROFILE_AUDIT: CALLER_OWN
WAITER_TTL: CALLER_OWN
WAITER_PROVIDER_CALL_COUNT: 0_ADDITIONAL
WAITER_REMOTE_HEALTH_MUTATION_COUNT: 0_ADDITIONAL
WAITER_CIRCUIT_SETTLEMENT_COUNT: 0_ADDITIONAL
READY_EXPIRY: MIN_CALLER_TTL_AND_DATASET_RETENTION
EMPTY_CONFIRMED_EXPIRY: MIN_CALLER_TTL_AND_DATASET_RETENTION
CACHE_HIT_EXPIRY: MIN_CALLER_TTL_AND_DATASET_RETENTION
EXPIRY_AFTER_RETENTION_COUNT: 0
```

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

Physical-attempt timeout classification uses an atomic execution phase. A
timeout in `QUEUED` is `PROVIDER_EXECUTOR_QUEUE_TIMEOUT`; a timeout in
`LOCAL_ADMISSION` is `PROVIDER_PRE_REMOTE_TIMEOUT`. Both remain `DEGRADED`
local admission evidence and cannot mutate remote health, open the circuit,
consume timeout retry, or call the adapter. A pure queued timeout also consumes
zero attempt budget. `PROVIDER_TIMEOUT` remains an `ERROR` remote transport
result only after `REMOTE_IN_FLIGHT` wins immediately before the adapter call.
This preserves fail-closed snapshots without turning local queue pressure into
false provider unavailability.

Circuit admission is attempt-owned. A HALF_OPEN physical attempt carries one
idempotent permit until the attempt actually ends. Local rejection releases
the probe, `429` or auth settles it as remote-reachable, remote failure reopens
the circuit, and READY/`EMPTY_CONFIRMED` closes it. Caller timeout and joined
waiters cannot release the owner permit. Thus a completed snapshot path cannot
strand the provider in a permanently claimed HALF_OPEN state.

Decision Cutoff Time is deferred to P3-I1. Production readiness remains
`BLOCKED`.
