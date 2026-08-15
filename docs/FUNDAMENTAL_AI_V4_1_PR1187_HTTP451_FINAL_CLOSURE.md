# Fundamental AI v4.1 PR #1187 HTTP 451 Final Closure

Status: `FINAL_HTTP451_CLOSURE_COMPLETE_PENDING_ONE_EXACT_REAUDIT`

## Baseline And Boundary

- Main: `b1b49a0de4090fd93a12b14e18c1c980669d0162`
- Closure base Head: `e82ba8888da596ac67c871b4cb4b03b2ec5191b3`
- Branch: `codex/v4-1-target-runtime-blocker-remediation`
- PR: `#1187`, Draft, open, unmerged
- Package: `FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION`

This closure fixes independent finding `P1-001-451-QUOTE-DERIVATIVES` without
changing Figma, Desktop, Mobile, Schema, public HTTP API, product thresholds,
Three-AI authority, Position Monitoring semantics, or automatic-trading
boundaries.

## Canonical Ownership

The single normalized path is:

`HTTP response -> ProviderFailureClassifier -> ProviderAdapterResponse ->
ProviderCapabilityRegistry.record -> structured fail-closed result`.

- `ProviderFailureClassifier` is the sole HTTP 451 classifier. Status 451 and
  existing legal/region aliases normalize to `REGION_RESTRICTED` with
  `REMOTE_CAPABILITY` origin.
- `ProviderCapabilityRegistry` remains the sole capability-state owner. It
  records exact provider, canonical instrument, provider symbol, market,
  contract, dataset, timeframe, request key, trace id, verified/observed time,
  source version and expiry metadata.
- A runtime region restriction overrides static support for only that exact
  capability. Expiry yields `STALE_CAPABILITY`, not success. Only the existing
  provider directory/reverification path may restore support; market data is
  never used as a capability probe.
- No second classifier, capability registry, provider-health owner,
  derivatives owner or provider API was introduced.

## HTTP 451 Path Matrix

| Entry point | Provider | Dataset key | Pre-call owner | 451 write owner | Current result | Later exact call | Test evidence |
|---|---|---|---|---|---|---|---|
| Routed OHLCV | Kraken/Binance | `OHLCV` + exact timeframe | Routed provider + registry | Routed provider | Typed region-restricted provider error | External call `0` | Existing OHLCV 451 and pre-call gate regression |
| Current price | Binance | `PRICE/SPOT/NONE/GLOBAL` | Price snapshot + registry | Price snapshot | Payload null, exact reason | Quote call `0` | `BinanceHttp451CapabilityPropagationTest` |
| Funding | Binance | `FUNDING/PERPETUAL/LINEAR/GLOBAL` | Derivatives snapshot + registry | Derivatives snapshot | Aggregate payload null, exact reason | Funding call `0` | `BinanceHttp451CapabilityPropagationTest` |
| Open interest | Binance | `OPEN_INTEREST/PERPETUAL/LINEAR/GLOBAL` | Derivatives snapshot + registry | Derivatives snapshot | Aggregate payload null, exact reason | OI call `0` | `BinanceHttp451CapabilityPropagationTest` |
| Aggregate derivatives | Binance | `DERIVATIVES` plus component keys | Derivatives snapshot + registry | Restricted component owner | Fail closed if either required component is restricted | Restricted component call `0` | Binance component/aggregate tests |
| Open interest | CoinGlass | `OPEN_INTEREST` + exact contract | CoinGlass snapshot base + registry | CoinGlass snapshot base | Typed region-restricted result | Client call `0` | `CoinGlassHttp451CapabilityPropagationTest` |
| Funding | CoinGlass | `FUNDING` + exact contract | CoinGlass snapshot base + registry | CoinGlass snapshot base | Typed region-restricted result | Client call `0` | `CoinGlassHttp451CapabilityPropagationTest` |
| Liquidation | CoinGlass | `LIQUIDATION` + exact contract | CoinGlass snapshot base + registry | CoinGlass snapshot base | Typed region-restricted result | Client call `0` | `CoinGlassHttp451CapabilityPropagationTest` |
| Long/short ratio | CoinGlass | `LONG_SHORT` + exact contract | CoinGlass snapshot base + registry | CoinGlass snapshot base | Typed region-restricted result | Client call `0` | `CoinGlassHttp451CapabilityPropagationTest` |
| Push Recheck price | Downstream Binance price | Exact `PRICE` key | Price snapshot + registry | Price snapshot | Quote unavailable/fail closed | Direct provider call `0` | Push Recheck and price snapshot regressions |
| Position Monitoring mark price | Downstream Binance price | Exact `PRICE` key | Price snapshot + registry | Price snapshot | Waiting/current unavailable | Direct provider call `0` | Position Monitor and price snapshot regressions |
| Decision read-only quote | Cached price snapshot | Existing cache key | No-call query | None | Null when unavailable | Provider call `0` | Decision regression |
| Dashboard projection | Existing service projections | Downstream exact keys | Downstream gated owners | Downstream gated owners | Truthful unavailable/partial state | Direct provider call `0` | Dashboard/full Maven regression |
| Market controller quote status | Binance price snapshot | Exact `PRICE` key | Price snapshot + registry | Price snapshot | Structured unavailable state | Direct client bypass `0` | Architecture and controller regressions |
| Asset Pool manual/scheduled scan | All downstream providers | Exact per-dataset keys | Downstream gated owners | Downstream gated owners | Restricted asset failed/partial only | Restricted endpoint call `0` | `PersistentAssetPoolServiceTest` |

`ALL_PRODUCTION_451_PATHS_AUDITED=PASS`.

## Current And Subsequent Request Contract

For current price, funding and open interest, actual mocked `HttpClient` 451
tests prove:

| Assertion | Expected | Result |
|---|---:|---|
| First exact external request | 1 | PASS |
| Exact canonical registry write | 1 | PASS |
| Written capability state | `REGION_RESTRICTED` | PASS |
| Structured payload | null | PASS |
| Second exact registry authorization | present | PASS |
| Second exact external request | 0 | PASS |

CoinGlass tests prove the same write-then-suppress behavior for all four
production datasets. HTTP 451 is not retried as 5xx and cannot return a stale
cached payload.

## Dataset Scope And Fallback

- Funding, open interest, price, OHLCV, liquidation and long/short have distinct
  registry keys. A restriction in one does not silently authorize or block
  another.
- The Binance aggregate derivatives snapshot checks both aggregate and exact
  component capability. It never treats generic perpetual/quote support as
  funding or open-interest support.
- Primary 451 is written before fallback evaluation. Fallback is called only
  after its own exact provider/instrument/market/contract/dataset/timeframe
  decision returns `SUPPORTED`.
- Unsupported, stale, disabled, not-configured or mismatched fallback has
  external call count `0` and the request remains fail closed.
- `ADA/USD` cannot satisfy `ADA/USDT`; `SPOT` cannot satisfy `PERPETUAL`; one
  contract type cannot satisfy another.

## No-Fabrication Contract

- `HTTP_451_EMPTY_SUCCESS_COLLAPSE_COUNT=0`
- `HTTP_451_FAKE_ZERO_VALUE_COUNT=0`
- `HTTP_451_FAKE_EVIDENCE_COUNT=0`
- `HTTP_451_5XX_RETRY_COUNT=0`

Restricted current price, funding or open interest is never emitted as zero,
fresh, successful no-data, or Evidence. In the mixed Asset Pool scenario, five
supported assets continue Analysis while the restricted asset has no analysis
id, no data-quality value, no Evidence and no Opportunity; the batch remains
truthfully `PARTIAL`.

## Executable Evidence

- `BinanceHttp451CapabilityPropagationTest`
- `CoinGlassHttp451CapabilityPropagationTest`
- `ProviderCapabilityPreCallGateTest`
- `ProviderCallCoordinatorTest`
- `ProviderCapabilityRegistryTest`
- `ProviderCapabilityGateArchitectureTest`
- `ProviderConsumerCapabilityGateTest`
- `PersistentAssetPoolServiceTest`
- existing OHLCV, price, derivatives, Push Recheck, Position Monitoring,
  Decision and Dashboard regressions

Local results:

- HTTP 451 focused contracts: `34/34 PASS`.
- Full Maven/H2: `4626 total / 4612 passed / 14 environment-gated skipped /
  0 failures / 0 errors`.
- Java 17 standard clean package and executable JAR: `PASS`.
- Standard JAR Flyway Core, Flyway PostgreSQL and V1-V13 resources: `PASS`.
- Isolated PostgreSQL 16 empty V1-V13, existing V13 restart,
  checksum/migration fail closed, packaged login/Session/logout: `PASS`.
- Product Source Gate, Workflow Contract and exact authorization validator:
  `PASS`.
- CoinGlass explicit RPM regression: `PASS`.
- Automatic-trading static guard and provider architecture guard: `PASS`.

No critical HTTP 451 test was skipped. The 14 skipped Maven tests are listed in
`docs/FUNDAMENTAL_AI_V4_1_TEST_REPORT.md` with reasons and equivalent evidence.

## Frozen Boundary Evidence

- `DATA_QUALITY_ALGORITHM_CHANGED=NO`
- `DATA_QUALITY_THRESHOLD_CHANGED=NO`
- `CANDIDATE_PROMOTION_THRESHOLD_CHANGED=NO`
- `MARKET_BIAS_CHANGED=NO`
- `OPPORTUNITY_STATE_CHANGED=NO`
- `PLAN_MODE_CHANGED=NO`
- `SCHEMA_CHANGED=NO`
- `FIGMA_CHANGED=NO`
- `DESKTOP_CHANGED=NO`
- `MOBILE_CHANGED=NO`
- `NO_FAKE_DATA=PASS`
- `NO_AI_FABRICATION=PASS`
- `AUTO_TRADING_CAPABILITY_COUNT=0`
- `DUPLICATE_OWNER_COUNT=0`
- `LIVE_PROVIDER_OR_AI_SECRET_USED=NO`

The internal typed-result defaults are backward compatible and expose no new
external endpoint. They exist only to preserve normalized failure state through
the existing market-client owner.

## Next Gate

Commit and push this closure to the existing PR #1187, keep the PR Draft, wait
for exact-head CI, and then perform one independent HTTP 451 closure re-audit.
This document does not authorize self-approval, merge, deployment, live-secret
acceptance or a new implementation package.
