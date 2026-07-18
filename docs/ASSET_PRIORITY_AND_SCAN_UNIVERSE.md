# Asset Priority and Scan Universe

## Priority Contract

The strict order is:

1. `P0_POSITION`
2. `P2_CANDIDATE`
3. `P1_WATCHLIST`
4. `P3_DISCOVERY`

`P0_POSITION` contains only manual `OPEN` or `PARTIALLY_CLOSED` positions.
`P1_WATCHLIST` is the replaceable manual watchlist. `P2_CANDIDATE` includes
watchlist candidates/waiting-trigger assets and promoted discovery candidates.
`P3_DISCOVERY` is a bounded low-frequency configured discovery pool.

## Universe Contract

```text
activeManualPositions
union manualWatchlist
union candidateAssets
union configuredDiscoveryUniverse
```

If one canonical instrument is present in multiple sets, the scan plan emits
one row at the highest priority. There is no implicit choice based on input
order and no permanent six-core-asset contract.

The first discovery owner is `ConfiguredDiscoveryUniverseSource`. It is
bounded, replaceable, and can be disabled. P3-CALL1 does not scan all Binance
assets, rank volume, inspect listings, or build a dynamic universe.

Each plan row carries canonical identity, provider symbol, effective priority,
independent due datasets/times, base/effective profile, escalation reasons, and
`frequencyMatrixVersion`.

This is an offline planning foundation. The provider scheduler and external
calls remain disabled by default. Production readiness remains `BLOCKED`.
