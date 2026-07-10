# V1 Real Historical Fixture Contract

## Scope

This contract defines the only local data that may count as real historical replay evidence. It does not authorize downloads, provider calls, production access, trading, or profitability claims.

## Allowed Locations

The loader may read only:

1. `src/test/resources/replay/real/`
2. `data/replay/real/`
3. An explicitly supplied local directory in `V1_REAL_HISTORICAL_FIXTURE_DIR`

The environment path must be local, must not be a URL, and must not identify a production/live/primary/main environment. No unrelated user directory may be searched.

## Required CSV Columns

| Column | Rule |
| --- | --- |
| `timestamp_utc` | ISO-8601 UTC instant |
| `symbol` | Non-empty normalized market symbol |
| `timeframe` | Supported declared interval: 1m, 5m, 15m, 1h, or 4h |
| `open` | Positive decimal |
| `high` | Positive and not below open, close, or low |
| `low` | Positive and not above open, close, or high |
| `close` | Positive decimal |
| `volume` | Non-negative decimal |

Optional source columns may include quote volume, trade count, open interest, funding, liquidation values, and `event_tag`. The replay candle passed to business code deliberately omits `event_tag` and all expected labels. Labels are stored and evaluated separately after the current replay decision is captured.

## Integrity Rules

- Rows are chronological by `timestamp_utc`.
- `(symbol, timeframe, timestamp_utc)` is unique.
- Timeframe continuity is checked per symbol/timeframe series.
- Gaps are recorded in `known_gaps`; no candle is filled or interpolated silently.
- Row count, UTC date range, symbols, timeframes, and SHA-256 are recorded.
- Prices must be positive and volumes non-negative.
- Invalid OHLC boundaries fail validation.

## Provenance Rules

The manifest must use exactly one source classification:

- `REAL_HISTORICAL_LOCAL_FIXTURE`
- `REAL_HISTORICAL_PRIVATE_LOCAL_FIXTURE`
- `INVALID_OR_UNVERIFIED_FIXTURE`
- `MISSING_REAL_HISTORICAL_FIXTURE`

`REAL_HISTORICAL_LOCAL_FIXTURE` requires a source reference, export/download date, redistribution decision, hash, date range, row count, symbols, timeframes, and provenance note. A private fixture must remain in an ignored local directory unless redistribution permission is explicit.

## No-Lookahead Rule

At replay time `T`, an adapter frame may contain only candles whose `timestamp_utc <= T`. The clock may move only forward. Expected labels, future candles, future highs/lows/closes, and event tags must not enter evidence, score, decision, plan, or monitor inputs.

## Current Contract State

No qualifying fixture is currently available. The committed manifest therefore records `MISSING_REAL_HISTORICAL_FIXTURE`, zero rows, and no hash. This is a fail-closed state, not a failed market result.
