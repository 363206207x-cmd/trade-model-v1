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

`ProviderRequestKey` includes provider, dataset, canonical identity, provider
symbol, timeframe, time bucket, and source version. Different market types,
timeframes, source versions, or instruments never share a flight or cache
entry.

Mappings are configuration/in-memory in P3-CALL1. No database migration and no
dynamic symbol discovery are included. Production readiness remains `BLOCKED`.
