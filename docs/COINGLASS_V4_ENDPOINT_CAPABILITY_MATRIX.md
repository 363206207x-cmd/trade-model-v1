# CoinGlass v4 Endpoint Capability Matrix

Verified on: 2026-07-10

This matrix is the production-code allowlist for CG-1. Only CoinGlass official API v4 documentation is used. The configured base URL is `https://open-api-v4.coinglass.com`, and authentication uses the request header `CG-API-KEY`. The key is never accepted as a query parameter, logged, persisted, or included in provider metadata.

Official references:

- [Authentication](https://docs.coinglass.com/reference/authentication)
- [Exchange List](https://docs.coinglass.com/reference/oi-exchange-list)
- [OI Weight History (OHLC)](https://docs.coinglass.com/reference/oi-weight-ohlc-history)
- [Coin Liquidation History](https://docs.coinglass.com/reference/aggregated-liquidation-history)
- [Global Account Ratio](https://docs.coinglass.com/reference/global-longshort-account-ratio)
- [Responses & Error Codes](https://docs.coinglass.com/reference/responses-error-codes)

## Endpoint Allowlist

| Capability ID | Business purpose | Official title | Version | Method/path | Required query | Response fields used | Time unit | Rate/update behavior | Subscription | Used | Fallback |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `CG_V4_OPEN_INTEREST_EXCHANGE_LIST` | Aggregate OI, supported OI change windows, exchange coverage/concentration | Exchange List | v4 | `GET /api/futures/open-interest/exchange-list` | `symbol` as coin, for example `BTC` | `exchange`, `symbol`, `open_interest_usd`, `open_interest_change_percent_5m`, `_15m`, `_1h` | No provider timestamp in documented row; fetch time is recorded explicitly | Official page states 10-second cache/update; response rate headers parsed when present | Official matrix: Hobbyist through Enterprise, including Standard | Yes | stale cache or typed unavailable/error state |
| `CG_V4_OI_WEIGHTED_FUNDING_HISTORY` | Latest OI-weighted funding raw value | OI Weight History (OHLC) | v4 | `GET /api/futures/funding-rate/oi-weight-history` | `symbol` coin, `interval=1m`, `limit=1` | `time`, `close` | Response `time`: milliseconds UTC | Standard has no interval limit; local provider budget and cache still apply | Official matrix includes Standard | Yes | stale cache or typed unavailable/error state |
| `CG_V4_AGGREGATED_LIQUIDATION_HISTORY` | Long/short liquidation sums for 1m/5m/15m/1h | Coin Liquidation History | v4 | `GET /api/futures/liquidation/aggregated-history` | `exchange_list`, `symbol` coin, `interval=1m`, `limit=60` | `time`, `aggregated_long_liquidation_usd`, `aggregated_short_liquidation_usd` | Response and optional bounds: milliseconds UTC | Standard has no interval limit; one bounded 60-row call per refresh | Official matrix includes Standard | Yes | missing windows remain null; stale cache or typed failure |
| `CG_V4_GLOBAL_ACCOUNT_LONG_SHORT_RATIO` | One explicit long/short source | Global Account Ratio | v4 | `GET /api/futures/global-long-short-account-ratio/history` | `exchange=Binance`, pair `symbol`, `interval=1m`, `limit=1` | `time`, `global_account_long_short_ratio` | Response `time`: milliseconds UTC | Standard has no interval limit; local provider budget and cache apply | Official matrix includes Standard | Yes | stale cache or typed unavailable/error state |

The selected long/short source is only `BINANCE_GLOBAL_ACCOUNT_RATIO`. Taker flow, top-trader account ratio, and top-trader position ratio are not mixed into this field.

## Mapping Rules

| Standard field | Source and conversion |
|---|---|
| `openInterestUsd` | `exchange=All.open_interest_usd`; a single exchange is never treated as aggregate OI |
| `openInterestChange1m` | `null`; the verified endpoint does not document a 1m OI-change field |
| `openInterestChange5m/15m/1h` | Corresponding documented percent fields on the `All` row |
| `exchangeConcentrationScore` | Largest valid exchange OI divided by `All.open_interest_usd`; canonical decimal ratio range is `0.0..1.0`, `0.70` means 70%, and the value is never multiplied by 100; null when the ratio cannot be computed reliably |
| `weightedFundingRate` | Latest non-future `close` from the OI-weighted funding history |
| `fundingExtremityScore` | `null`; threshold ownership is deferred to BIZ-1 rule configuration |
| liquidation 1m/5m/15m/1h | Sum the latest 1/5/15/60 complete 1m records under one latest provider-time boundary; an incomplete window remains null |
| `liquidationSpikeScore` | `null`; threshold ownership is deferred to BIZ-1 |
| `longShortRatio` | Latest `global_account_long_short_ratio` for Binance and the exact pair symbol |
| `longShortRatioSource` | `BINANCE_GLOBAL_ACCOUNT_RATIO` |

## Response and Health Contract

An HTTP 200 response is not automatically healthy. The client also requires API-level `code="0"`, a `data` node of the documented type, valid timestamps, valid numeric ranges, and a verified symbol mapping.

- Missing key: `NOT_CONFIGURED`.
- Provider or external-call gate disabled: `DISABLED`.
- Successful empty array: `EMPTY_CONFIRMED`; values remain null.
- 401/403: `ERROR` with sanitized `AUTHENTICATION_FAILED`, no retry.
- 429: `DEGRADED` with `RATE_LIMITED`; `Retry-After` is honored.
- 5xx: bounded to two retries after the first attempt.
- Timeout: bounded to one retry.
- Partial dataset success: aggregate `DEGRADED`, while successful fields remain available.
- Expired provider time or stale fallback: `STALE`.

Official response headers `API-KEY-MAX-LIMIT` and `API-KEY-USE-LIMIT` are parsed into in-memory health metadata. If absent, `ProviderRateBudgetManager` uses the configured local fixed-minute accounting. `COINGLASS_ADVERTISED_RPM` defaults to 300 for the declared Standard-plan policy but is environment-overridable; no account entitlement is inferred without a controlled authenticated run.

## Not Used

Any CoinGlass v2/v3 path, private account endpoint, position endpoint, order endpoint, trading endpoint, undocumented field, and endpoint not listed above is `UNVERIFIED_NOT_USED`.

No live request was made while producing this matrix. Current account plan/endpoint entitlement is not claimed as runtime evidence because no key is present in the environment.
