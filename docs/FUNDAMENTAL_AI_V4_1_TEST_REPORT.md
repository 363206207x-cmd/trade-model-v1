# Fundamental AI v4.1 Target Runtime Remediation Test Report

Status: `FINAL_HTTP451_LOCAL_VALIDATION_COMPLETE_PENDING_EXACT_HEAD_CI`

Exact implementation Head and final CI evidence are recorded after commit and
push. No live provider secret is used by this report.

## Implemented Test Coverage

| Area | Coverage |
|---|---|
| Standard release | Flyway runtime/JAR resources, no special profile, V1-V13 packaged smoke, existing V13 restart, checksum fail closed, SQL-init ownership |
| Provider capability | Exact ADAUSDT, quote/market/contract/dataset isolation, dataset-specific enablement, stale directory-only revalidation, actual HTTP 451 write/read/call counts, independently authorized fallback, zero-call assertions, direct-client architecture guard, all-unavailable and per-asset isolation |
| CoinGlass | Disabled/key/RPM state matrix, explicit 80/300 RPM, repository implicit-default guard, exact auth header arguments, secret redaction, 200/401/403/429/5xx/timeout, rate budget, freshness and provenance |
| AI readiness | Missing vs explicit zero, RPM/cost/budget states, exact-model authorize, fallback not ready, cache, auth/rate/model/provider failure and secret redaction |
| Auth/bootstrap | Password generation/policy, missing/weak rejection, existing-user no overwrite, readiness/liveness split, login/session/logout regression |
| Regression | Product Source, Workflow Contract, authorization, duplicate-skeleton, full H2/Maven, PostgreSQL package smoke, secret and automatic-trading scans |

## Current Evidence

- HTTP 451 focused suite: `34 tests / 34 passed / 0 skipped`.
- Current-price, funding, open-interest, aggregate derivatives, OHLCV,
  CoinGlass, provider architecture/call-count and per-asset suites: `PASS`.
- B03 AI readiness and B04 auth/bootstrap focused suites: `PASS`.
- Focused B01-B04 suites: `PASS`.
- Java 17 standard clean package: `PASS`.
- Standard JAR contents: Flyway Core, Flyway PostgreSQL support, and V1-V13
  migration resources: `PASS`.
- PostgreSQL 16 empty database migration: `13/13 PASS`.
- Packaged JAR restart against an existing V13 database: `PASS`.
- Corrupted V13 checksum startup and readiness fail closed: `PASS`.
- Packaged JAR form login, CSRF Session, logout, and pre-logout Session
  invalidation: `PASS`.
- Standard PostgreSQL 16 packaged smoke: `PASS`.
- Full Maven/H2 suite: `4626 tests / 4612 passed / 0 failed / 0 errors /
  14 skipped`.
- Product Source Gate: `PASS`.
- Workflow Contract: `PASS`.
- Exact authorization validation: `PASS`.
- Production implicit CoinGlass RPM default count: `0`.
- Direct production provider bypass count: `0`.
- HTTP 451 direct empty-success collapse count: `0`.
- HTTP 451 numeric-zero and fake-Evidence count: `0`.
- Automatic-trading capability count: `0`.
- Exact-head required CI: `PENDING_DRAFT_PR`.

No live provider or AI secret was used. The PostgreSQL smoke used an isolated
container, generated one-time credentials, disabled external provider calls,
and removed temporary artifacts during cleanup.

## Exact HTTP 451 Contracts

| Contract | Executable evidence | Result |
|---|---|---|
| Current price first 451 writes exact state | `BinanceHttp451CapabilityPropagationTest.currentPrice451WritesRegionRestricted` | PASS |
| Current price next exact call is suppressed | `BinanceHttp451CapabilityPropagationTest.currentPrice451BlocksSubsequentCall` | PASS |
| Funding first write and next-call suppression | `BinanceHttp451CapabilityPropagationTest.funding451WritesRegionRestrictedAndBlocksSubsequentCall` | PASS |
| Open-interest first write and next-call suppression | `BinanceHttp451CapabilityPropagationTest.openInterest451WritesRegionRestrictedAndBlocksSubsequentCall` | PASS |
| Dataset isolation and no cross-authorization | `BinanceHttp451CapabilityPropagationTest.regionRestrictionCapabilityScopeIsDatasetSpecific` | PASS |
| CoinGlass OI/funding/liquidation/long-short | `CoinGlassHttp451CapabilityPropagationTest` | PASS |
| 451 is not a retryable 5xx or stale fallback | `ProviderCallCoordinatorTest.http451IsNotRetriedAndCannotReturnStaleFallback` | PASS |
| Exact supported fallback only | `ProviderCapabilityPreCallGateTest.primary451UsesOnlyIndependentlySupportedFallback` | PASS |
| Unsupported fallback call count zero | `ProviderCapabilityPreCallGateTest.primary451DoesNotCallUnsupportedFallback` | PASS |
| Reverification required after expiry | `ProviderCapabilityRegistryTest.regionRestrictionExpiresOnlyToReverificationRequiredState` | PASS |
| All production paths use canonical classifier | `ProviderCapabilityGateArchitectureTest.allProduction451PathsUseCanonicalClassifierAndStructuredResults` | PASS |
| Direct 451-to-empty collapse count zero | `ProviderCapabilityGateArchitectureTest.direct451CollapseToEmptyResultCountIsZero` | PASS |
| Five-success/one-restricted scan isolation | `PersistentAssetPoolServiceTest` mixed batch contract | PASS |

The actual-client tests assert the first external call is `1`, the canonical
registry write is `1`, a later exact registry lookup occurs, and the later
external call is `0`. Fail-closed results have null payload and preserve
`REGION_RESTRICTED`; no funding, open-interest, quote or Evidence value is
fabricated.

## Skipped Test Inventory

| Test group | Count | Skip reason | Critical contract | Equivalent evidence | Blocks re-audit |
|---|---:|---|---|---|---|
| `ControlledCurrentStateCloneFlywayActionTest` | 1 | Explicit P3 controlled PostgreSQL action not enabled | No | Standard isolated PostgreSQL V1-V13 smoke | NO |
| `ControlledCurrentStateContentFingerprintTest` | 7 | P3 content-fingerprint environment gate | No | Existing static/unit fingerprint contracts; no changed owner in this closure | NO |
| `ControlledGeneratedReleaseLikeFixtureFlywayTest` | 1 | P3 generated-fixture action not enabled | No | Standard isolated PostgreSQL V1-V13 smoke | NO |
| `ControlledGreenfieldFlywayV7ActionTest` | 1 | Historical P3-G action not enabled | No | Current standard empty V1-V13 migration | NO |
| `ControlledP3hComposeOfflineSmokeTest` | 1 | Explicit Docker contract opt-in not enabled | No | Standard packaged-JAR PostgreSQL smoke | NO |
| `ControlledPostgreSqlFlywaySmokeTest` | 1 | External controlled PostgreSQL environment absent | No | Isolated PostgreSQL 16 script | NO |
| `PostgreSqlFlywayMigrationSmokeTest` | 1 | Docker/Testcontainers unavailable to the ordinary Maven process | B01 | Isolated PostgreSQL 16 V1-V13/restart/checksum run | NO |
| `CoinGlassControlledSmokeTest` | 1 | Live external-call opt-in absent | No; live acceptance is forbidden here | RPM/client/451 contracts with zero secrets | NO |

No critical HTTP 451 test was skipped. Live CoinGlass and AI calls were
intentionally not run because this package forbids live-secret acceptance.
