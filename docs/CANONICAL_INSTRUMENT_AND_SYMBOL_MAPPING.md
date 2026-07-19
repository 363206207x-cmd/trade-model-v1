# Canonical Instrument and Symbol Mapping

## Identity

`CanonicalInstrumentId` contains:

- `baseAsset`
- `quoteAsset`
- `marketType` (`SPOT` or `PERPETUAL`)
- `venue`
- `contractType` (`NONE` or `LINEAR`)

Examples:

```text
BINANCE:SPOT:NONE:BTC/USDT
BINANCE:PERPETUAL:LINEAR:BTC/USDT
```

Those identities are intentionally different. A spot quote cannot satisfy a
perpetual request.

## Mapping Boundary

`ProviderSymbolMappingRegistry` is the only translation boundary between a
canonical instrument and a provider symbol. Mappings include provider,
canonical ID, provider symbol, enabled state, and source version.

Aliases such as `BTCUSDT`, `BTC-USDT`, and `BTC/USDT` resolve to one canonical
instrument only when configuration makes the market type and venue explicit.
No caller may guess by stripping text when the mapping is absent or ambiguous.
The failure code is explicit and fail-closed.

Internal scan plans, cache keys, single-flight keys, candidate records, and
notification events use canonical identity. The provider symbol is retained in
metadata only for adapter calls and evidence.

Two identities are intentionally separate:

- `ProviderSnapshotKey` contains provider, dataset, canonical identity,
  provider symbol, timeframe, and source version. Cache, health, retention,
  stale fallback, and Single Flight use this stable identity.
- `ProviderRequestKey` may additionally contain `timeBucket`, but the bucket is
  limited to refresh planning and audit context. It is not a cache or
  Single-Flight identity and cannot hide the previous stable snapshot.

Consumers with different TTLs therefore share one snapshot. Each consumer
independently decides whether that snapshot is fresh enough; a stricter
consumer may request refresh, and concurrent consumers share the same physical
refresh. Dataset retention, rather than a caller-selected TTL or time bucket,
controls stale readability and eventual eviction.

SPOT and PERPETUAL identities never share a cache entry, gap key, audit
identity, or normalized observation. The current price and OHLCV adapters are
spot-only; a PERPETUAL request returns explicit `NOT_CONFIGURED` evidence until
a futures-capable adapter exists. Funding and open interest accept only a
linear perpetual identity. A spot reference, if introduced later, must be a
separate explicitly labelled input and can never masquerade as an execution
price.

Mappings are configuration/in-memory in P3-CALL1. No database migration and no
dynamic symbol discovery are included. Production readiness remains `BLOCKED`.
