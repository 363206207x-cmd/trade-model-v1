# Fundamental AI v4.1 Provider Capability Matrix

Status: `FINAL_HTTP451_GATE_IMPLEMENTED_PENDING_ONE_EXACT_REAUDIT`

## Canonical Capability Record

Each provider capability carries provider, canonical asset identity, base and
quote asset, market/contract type, exact provider symbol, dataset/capability
type, supported timeframes, capability state, source version, verification and
observation time, request/trace identity, expiry metadata and a sanitized
failure reason.

States:
`SUPPORTED`, `UNSUPPORTED_SYMBOL`, `UNSUPPORTED_TIMEFRAME`,
`REGION_RESTRICTED`, `PROVIDER_DISABLED`, `SOURCE_UNAVAILABLE`,
`STALE_CAPABILITY`, and `NOT_CONFIGURED`.

## Provider Matrix

| Provider | Instrument source | Exactness | Runtime behavior |
|---|---|---|---|
| Kraken | Official `AssetPairs` directory | Only an exact USDT pair is accepted. USD is never treated as USDT. | A verified exact pair may serve as primary; missing pair/timeframe fails closed and may route to another verified provider. |
| Binance | Existing exact mappings plus verified `exchangeInfo` directory | Exact symbol/quote/market/contract only, including `ADAUSDT`; no string-generated new mapping. | HTTP 451 becomes `REGION_RESTRICTED` and is not repeatedly called; catalog fallback is never treated as verified capability. |
| CoinGlass | Existing v4 derivatives adapter | Evidence-only derivatives identity | Missing config is `NOT_CONFIGURED`; never used as OHLCV or mark-price replacement. |

`ADA/USD` cannot satisfy `ADAUSDT`. If Binance is region restricted and no
other exact enabled provider supports `ADAUSDT`, that asset fails closed while
other Pool assets continue.

## Asset Pool Scan Contract

Batch output includes `overallState`, `successCount`, `partialCount`,
`failedCount`, and `perAssetResults`. Each asset result includes asset identity,
provider, state, analysis id, data quality, failure reason and observation time.

One failed asset never rolls back a successful AnalysisRun. A mix of successes
and failures truthfully returns overall `PARTIAL`; failed assets cannot enter
Opportunity or Dynamic Top6 through a successful-analysis path.

## Retry Contract

- Retryable transport failures remain bounded by the existing provider path.
- `REGION_RESTRICTED`, `UNSUPPORTED_SYMBOL`, `UNSUPPORTED_TIMEFRAME`,
  `PROVIDER_DISABLED`, and `NOT_CONFIGURED` are conclusive and are not
  high-frequency retry states.
- Capability verification expiry becomes `STALE_CAPABILITY`, never success.

## Unified Pre-call Contract

`CanonicalInstrumentId -> ProviderCapabilityRegistry.authorize -> exact
identity/timeframe decision -> provider call -> response validation` is the
only production market-data sequence. `STALE_CAPABILITY` may call a capability
directory only. It cannot use a data endpoint as a support probe.

| Production path | Gate owner | External-call owner | Result |
|---|---|---|---|
| Manual/scheduled Asset Pool scan and Analysis Preview/Scheduler | Downstream coordinated OHLCV, price and derivatives snapshots | Existing provider adapters | GATED |
| Persisted OHLCV ingestion | `RoutedPublicOhlcvProvider` + registry | Kraken/Binance OHLCV adapters | GATED |
| Provider scan/OHLCV refresh | `CoordinatedOhlcvSnapshotService` + registry | Routed OHLCV | GATED |
| Current price, Push Recheck and Position Monitoring | `MarketPriceSnapshotService` + registry | Binance quote adapter | GATED |
| Derivatives refresh | `BinanceDerivativesSnapshotService` + registry | Binance funding/OI adapters | GATED |
| Market quote-status controller | `MarketPriceSnapshotService` + registry | Binance quote adapter | GATED |
| Binance asset directory search | Catalog configuration gate | Binance `exchangeInfo` only | GATED |

Primary and fallback capability decisions are independent. Quote, market type,
contract type, provider symbol and timeframe must all match. `SPOT` cannot
satisfy `PERPETUAL`, and USD/USDC cannot satisfy USDT.

Architecture and call-order tests prove
`CAPABILITY_REGISTRY_READ_COUNT >= 1`, blocked provider call count `0`, and
`DIRECT_PROVIDER_BYPASS_COUNT=0`.

## HTTP 451 Dataset Matrix

| Entry point | Exact dataset key | Canonical classifier | Registry write | Next exact call | Result contract |
|---|---|---|---|---|---|
| Routed Kraken/Binance OHLCV | `OHLCV` + requested timeframe | `ProviderFailureClassifier` | Existing routed provider write | External call `0` while restricted | Typed OHLCV provider error |
| Binance current price | `PRICE` + `SPOT/NONE/GLOBAL` | `ProviderFailureClassifier` | `MarketPriceSnapshotService` | External call `0` | Payload null, `REGION_RESTRICTED` |
| Binance perpetual funding | `FUNDING` + `PERPETUAL/LINEAR/GLOBAL` | `ProviderFailureClassifier` | `BinanceDerivativesSnapshotService` | Funding call `0` | Aggregate payload null, no zero |
| Binance perpetual open interest | `OPEN_INTEREST` + `PERPETUAL/LINEAR/GLOBAL` | `ProviderFailureClassifier` | `BinanceDerivativesSnapshotService` | OI call `0` | Aggregate payload null, no zero |
| CoinGlass open interest | `OPEN_INTEREST` + exact market contract | `ProviderFailureClassifier` | CoinGlass snapshot base owner | External call `0` | Typed fail-closed result |
| CoinGlass funding | `FUNDING` + exact market contract | `ProviderFailureClassifier` | CoinGlass snapshot base owner | External call `0` | Typed fail-closed result |
| CoinGlass liquidation | `LIQUIDATION` + exact market contract | `ProviderFailureClassifier` | CoinGlass snapshot base owner | External call `0` | Typed fail-closed result |
| CoinGlass long/short | `LONG_SHORT` + exact market contract | `ProviderFailureClassifier` | CoinGlass snapshot base owner | External call `0` | Typed fail-closed result |
| Push Recheck mark price | Downstream `PRICE` key | Downstream price snapshot owner | Downstream price snapshot owner | External call `0` | Current data unavailable |
| Position Monitoring mark price | Downstream `PRICE` key | Downstream price snapshot owner | Downstream price snapshot owner | External call `0` | Waiting/current unavailable |
| Decision/dashboard read-only views | Existing cached snapshot key | No direct provider call | No direct writer | External call `0` | Null/fail-closed projection |

For every network-backed row, the first actual 451 produces exactly one
canonical observation before the caller returns. HTTP 451 is not retried as a
5xx and cannot unlock stale cached payload. A different dataset remains
independent unless an existing provider-health rule explicitly marks the
provider itself unavailable.

## Fallback Matrix

| Primary result | Fallback capability | External behavior |
|---|---|---|
| `REGION_RESTRICTED` | exact `SUPPORTED` | Primary remains blocked; fallback may be called once |
| `REGION_RESTRICTED` | unsupported/stale/not configured | Primary and fallback calls are both suppressed after their registry decisions |
| `REGION_RESTRICTED` | quote/market/contract mismatch | No substitution and no fallback call |

`ADA/USD` never satisfies `ADA/USDT`; `SPOT` never satisfies `PERPETUAL`; and
OHLCV/price support never grants funding, open-interest, liquidation or
long/short permission.
