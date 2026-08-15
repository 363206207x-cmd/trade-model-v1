# Fundamental AI v4.1 Target Runtime Root-Cause Matrix

## Scope

- Package: `FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION`
- Final HTTP 451 closure baseline: PR #1187 at
  `e82ba8888da596ac67c871b4cb4b03b2ec5191b3`
- Product source: `docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`
- Safety boundary: real data only, fail closed, no automatic order/open/close/reverse, no Figma or Mobile change.

The original inventory below was produced before production-code remediation
and is retained as root-cause evidence. The final state is the post-remediation
closure matrix at the end of this document. No row in the original inventory
authorizes a second owner or a new business skeleton.

## A. Release And Database

| Entry point | Owner | Capability gate | Fail-closed state | Test/evidence | Current status | Remediation required |
|---|---|---|---|---|---|---|
| Standard Java 17 package | `pom.xml`, Spring Boot Maven plugin | Java/Maven build | Build failure | clean package and executable JAR smoke | PASS/REGRESSION | No logic change; rerun from clean tree. |
| Runtime PostgreSQL driver | `pom.xml` runtime dependencies | classpath resolution | Startup failure | executable JAR dependency inspection | PASS/REGRESSION | None. |
| Flyway PostgreSQL support | `flyway-core`, `flyway-database-postgresql` | Flyway startup migration | Readiness down/startup blocked | PostgreSQL V1-to-V13 smoke | PASS/REGRESSION | None. |
| Empty PostgreSQL database | `db/migration/V1..V13` | Flyway schema history | Startup blocked | empty-db migration smoke | PASS/REGRESSION | None. |
| Existing V13 restart | Flyway schema history | checksum validation | Startup blocked on mismatch | restart/checksum smoke | PASS/REGRESSION | None. |
| Liveness/readiness | Actuator/Spring Boot readiness | application and DB health | readiness not UP | deployment smoke | PASS/REGRESSION | None. |
| Production SQL/H2 isolation | `application-prod.yml`, profile safety guard | production profile guard | startup blocked | production safety tests | PASS/REGRESSION | None. |

## B. Market Provider Capability And Routing

| Entry point | Owner | Capability gate | Fail-closed state | Test/evidence | Current status | Remediation required |
|---|---|---|---|---|---|---|
| Canonical symbol and exact provider identity | `ProviderSymbolMappingRegistry` | exact canonical/market/contract mapping | `UNSUPPORTED_SYMBOL`/`NOT_CONFIGURED` | exact identity contract tests | PARTIAL | Add canonical lookup API that never guesses quote, market, or contract. |
| Capability state and freshness | `ProviderCapabilityRegistry` | configured/observed capability | unsupported/stale/region/disabled/unavailable | registry tests | PARTIAL | Extend the existing owner with pre-call decisions and explicit observations; no second registry. |
| Routed OHLCV primary | `RoutedPublicOhlcvProvider` | currently post-call registry record | provider error after an unnecessary call | independent reproduction | GAP P1 | Authorize exact provider+symbol+timeframe before calling. |
| Routed OHLCV fallback | `RoutedPublicOhlcvProvider` | currently reason-only fallback | fallback can call unsupported/region provider | fallback call-count tests | GAP P1 | Require independent `SUPPORTED` exact fallback decision before calling. |
| HTTP 451 | canonical classifier + capability owner | runtime observation | `REGION_RESTRICTED` | 451 repeat-call/write/read tests | PASS/FINAL | All production clients normalize through `ProviderFailureClassifier`; exact observations block subsequent calls until authorized revalidation. |
| Stale capability | capability owner + directory revalidator | no pre-call revalidation path | `STALE_CAPABILITY` | stale revalidation call-order test | GAP | Revalidate through provider directory/capability call; never probe market data. |
| Kraken exact pair directory | `KrakenPairResolver` | AssetPairs directory | unsupported/source unavailable | resolver tests | PARTIAL | Expose a directory-only capability revalidation result to the unified gate. |
| Binance exact mapping/directory | mapping registry / Binance catalog | configured exact mapping | unsupported/stale/source unavailable | exact mapping tests | PARTIAL | Route catalog/directory verification through the same capability owner; no market-data probe. |
| Asset Pool manual scan | `PersistentAssetPoolService` -> analysis chain | downstream snapshots | per-asset failed/partial | 5-success-1-unsupported test | PARTIAL | Ensure every downstream market request is gated and one asset failure is isolated. |
| Asset Pool scheduled scan | `AnalysisSchedulerService` | downstream snapshots | per-asset failed/partial | scheduler path test | PARTIAL | Same unified gate; preserve per-asset isolation. |
| Analysis preview | `AnalysisRunOrchestrator` -> assembler | downstream snapshots | preview failed/partial | preview path test | PARTIAL | Same unified gate; no preview bypass. |
| Market data scheduler | `MarketDataScheduler` | analysis scheduler | skipped/failed asset | scheduler tests | PASS/REGRESSION | Protect downstream gate behavior. |
| Persisted OHLCV ingestion | `PersistedOhlcvIngestionScheduler` | routed public provider | waiting/error | ingestion tests | PARTIAL | Gate is enforced in routed provider before any provider client. |
| Coordinated OHLCV refresh | `CoordinatedOhlcvSnapshotService` | hard-coded Binance mapping before router | not configured | coordinated snapshot tests | GAP | Remove the Binance-only precondition and use the authorized routed provider identity. |
| Provider dataset OHLCV refresh | `DefaultProviderDatasetRefreshPort` | hard-coded Binance mapping | not configured | provider scan tests | GAP | Delegate to the gated OHLCV service and record actual provider identity. |
| Current-price refresh | `MarketPriceSnapshotService` | exact `PRICE/SPOT/NONE/GLOBAL` registry key | `REGION_RESTRICTED`/unavailable | actual HTTP 451 write/read/call-count tests | PASS/FINAL | Typed quote result writes before return; second exact request makes zero external calls. |
| Derivatives refresh | `BinanceDerivativesSnapshotService` | aggregate plus exact `FUNDING` and `OPEN_INTEREST` keys | `REGION_RESTRICTED`/unavailable | actual HTTP 451 component and aggregate tests | PASS/FINAL | Component states are isolated; either required 451 fails the aggregate closed with no zero payload. |
| Push recheck price | `PushRecheckServiceImpl` -> price snapshot | downstream `PRICE` gate | quote unavailable | Push Recheck regression | PASS/FINAL | No direct provider bypass; restricted quote remains fail closed. |
| Position monitoring price | `PositionMonitorServiceImpl` -> price snapshot | downstream `PRICE` gate | current data unavailable | Position Monitoring regression | PASS/FINAL | No direct provider bypass; no fabricated mark price or PnL. |
| Decision/read-only quote | `DecisionServiceImpl` -> price snapshot cache | no-call peek | null | decision tests | PASS/REGRESSION | Query-only path must not initiate external calls. |
| Direct market controller refresh | `MarketController` -> compatibility analysis/quote client | quote endpoint can bypass snapshot gate | unavailable | architecture test | GAP | Remove production direct provider-client dependency and delegate to gated services. |
| Production direct-client architecture | production controllers/services | none | n/a | static dependency guard | GAP | Permit provider clients only inside router/gate-owned adapters and provider-directory owners. |
| Partial scan isolation | Asset Pool scan loop | exception isolation | failed asset only | mixed six-asset test | PASS/REGRESSION | Add contract test with one unsupported asset and five successful assets. |

## C. CoinGlass Configuration And Runtime

| Entry point | Owner | Capability gate | Fail-closed state | Test/evidence | Current status | Remediation required |
|---|---|---|---|---|---|---|
| Bound CoinGlass RPM | `CoinGlassProperties` | configuration presence | missing/invalid | property-binding tests | GAP P1 | Replace primitive default 300 with nullable explicit value. |
| Shared provider budget RPM | `ProviderCallProperties.ProviderBudgets` | budget configuration | unregistered/unavailable | budget tests | GAP | Remove implicit 300 and preserve missing state. |
| Base configuration | `application.yml` | environment binding | RPM missing | static guard | GAP P1 | Remove `${COINGLASS_ADVERTISED_RPM:300}` in both property trees. |
| Production configuration | `application-prod.yml` | preflight and profile guard | RPM missing/invalid | production guard tests | GAP P1 | Remove implicit default and require explicit positive RPM only when enabled for external calls. |
| Local-real configuration | `application-local-real.yml`, `.env.example` | explicit opt-in | not configured | local-real contract test | PARTIAL | Document explicit RPM; keep disabled path valid. |
| CoinGlass state classification | CoinGlass configuration owner | enabled/external/key/RPM precedence | `NOT_CONFIGURED`, `KEY_MISSING`, `RPM_NOT_CONFIGURED`, `INVALID_RPM` | state matrix tests | GAP | Add one canonical configuration-state function and reuse it in health, preflight, and runtime calls. |
| Target runtime preflight | `TargetRuntimePreflight` | environment contract | blocked when enabled but incomplete | preflight tests | GAP P1 | Distinguish disabled, key missing, RPM missing, zero/negative, configured. |
| Runtime dataset call | `AbstractCoinGlassDatasetSnapshotService` | configuration plus exact dataset capability | typed unavailable/region state | RPM and actual 451 call-count tests | PASS/FINAL | OI, funding, liquidation and long/short write exact registry observations and suppress later calls. |
| V4 client validation | `CoinGlassV4Client` | host/header/path allowlist | invalid configuration | client tests | PARTIAL | Reuse configuration-state gate; preserve official host/header/path checks. |
| Rate budget registration | `ProviderRateBudgetManager` | registration map | budget unavailable | budget tests | GAP | Do not clamp missing/invalid CoinGlass RPM to one request. Register only positive explicit value. |
| Provider health | `CoinGlassProviderHealthService` | configuration state then endpoint health | explicit configuration state | health tests | GAP | Surface missing/invalid RPM semantics before endpoint health. |
| Auth header protocol | `JdkCoinGlassV4HttpTransport` | exact `CG-API-KEY` | invalid configuration | transport argument/protocol test | PARTIAL | Add repository test proving exact header name/key arguments without logging secret. |
| Implicit default guard | source/config static scan | no implicit production default | build failure | architecture/static test | GAP | Assert `PRODUCTION_IMPLICIT_COINGLASS_RPM_DEFAULT_COUNT=0`. |

## D. AI Runtime Readiness

| Entry point | Owner | Capability gate | Fail-closed state | Test/evidence | Current status | Remediation required |
|---|---|---|---|---|---|---|
| OpenAI/Gemini/xAI key presence | `AiProviderProperties`, readiness service | configuration presence | unavailable/fallback | AI readiness tests | PASS/REGRESSION | No semantic change. |
| Per-provider RPM | AI properties/readiness | positive explicit value | unavailable/fallback | missing/zero/positive tests | PASS/REGRESSION | No semantic change. |
| Input/output cost | AI properties/readiness | positive explicit value | unavailable/fallback | cost contract tests | PASS/REGRESSION | No semantic change. |
| Daily/per-analysis budget | `AiUsageGuard` | budget check | rule fallback | budget tests | PASS/REGRESSION | No threshold change. |
| Exact model identity | model router/readiness | exact frozen model | unavailable/fallback | exact model tests | PASS/REGRESSION | No model change. |
| Failure/fallback | safe provider clients/orchestrator | rule-layer fallback | role fallback/unavailable | failure tests | PASS/REGRESSION | No authority change. |
| Readiness cache/reverify | `AiProviderReadinessService` | expiry and explicit reverify | stale/unavailable | readiness tests | PASS/REGRESSION | No logic change. |
| Secret redaction | provider clients/logging | redaction | redacted error | token leakage scans | PASS/REGRESSION | Hard-stop regression scan. |
| Target preflight | `TargetRuntimePreflight` | key/RPM/cost/model/budget | `PREFLIGHT=BLOCKED` | preflight tests | PASS/REGRESSION | Preserve while extending CoinGlass states. |

## E. Authentication And Bootstrap

| Entry point | Owner | Capability gate | Fail-closed state | Test/evidence | Current status | Remediation required |
|---|---|---|---|---|---|---|
| Initial password policy | `InitialPasswordPolicy` | policy validation | bootstrap/preflight blocked | policy tests | PASS/REGRESSION | No policy change. |
| Password generator | `RuntimePasswordTool` | generated-value policy check | command failure | generator tests | PASS/REGRESSION | No change. |
| Initial user bootstrap | `PersonalUserBootstrap` | auth enabled + valid credentials | startup blocked/no overwrite | bootstrap tests | PASS/REGRESSION | Preserve no-overwrite behavior. |
| Existing user | bootstrap owner | existing record check | unchanged user | bootstrap tests | PASS/REGRESSION | No change. |
| Login/session/logout | Spring Security owners | authentication/session rules | denied/invalidated session | auth integration tests | PASS/REGRESSION | No change. |
| Auth readiness/preflight | `TargetRuntimePreflight`, profile guard | username/password policy | blocked | preflight tests | PASS/REGRESSION | Preserve while extending CoinGlass states. |
| Secret redaction | auth logging/runtime output | no plaintext secret output | redacted | token/password leakage scan | PASS/REGRESSION | Hard-stop regression scan. |

## Executed Closure

1. Centrally extend the existing capability ownership; all market-data adapters consume its pre-call decision.
2. Remove production direct-client bypasses and add a static architecture guard.
3. Introduce explicit CoinGlass configuration presence semantics without adding a second provider model.
4. Add call-order/count, exact-instrument, partial-scan, CoinGlass state, protocol, and static-default tests.
5. Re-run the complete B01-B04 suite, standard executable-JAR smoke,
   PostgreSQL V1-to-V13 migration/restart/checksum validation,
   source/workflow gates, and secret/automatic-trading scans.

## Final HTTP 451 Closure Matrix

| Production path | Dataset/capability | First actual 451 | Registry state/write | Next exact request | Dataset isolation | Final status |
|---|---|---|---|---|---|---|
| Routed OHLCV | `OHLCV` + timeframe | One data call | `REGION_RESTRICTED`, canonical routed write | Data call `0` | Yes | PASS |
| Current price/quote | `PRICE/SPOT/NONE/GLOBAL` | One quote call | One exact write with trace | Quote call `0` | Yes | PASS |
| Binance funding | `FUNDING/PERPETUAL/LINEAR/GLOBAL` | One funding call | One exact write with trace | Funding call `0` | Yes | PASS |
| Binance open interest | `OPEN_INTEREST/PERPETUAL/LINEAR/GLOBAL` | One OI call | One exact write with trace | OI call `0` | Yes | PASS |
| Binance aggregate derivatives | `DERIVATIVES` plus component keys | Stops at restricted component | Component state retained | Restricted component call `0` | Yes | PASS |
| CoinGlass open interest | `OPEN_INTEREST` | One client call | One exact write with trace | Client call `0` | Yes | PASS |
| CoinGlass funding | `FUNDING` | One client call | One exact write with trace | Client call `0` | Yes | PASS |
| CoinGlass liquidation | `LIQUIDATION` | One client call | One exact write with trace | Client call `0` | Yes | PASS |
| CoinGlass long/short | `LONG_SHORT` | One client call | One exact write with trace | Client call `0` | Yes | PASS |
| Push Recheck price | downstream `PRICE` | Uses price owner | Uses price owner | Direct provider call `0` | Yes | PASS |
| Position Monitoring price | downstream `PRICE` | Uses price owner | Uses price owner | Direct provider call `0` | Yes | PASS |
| Decision/dashboard query | cached projection | No provider call | No duplicate write owner | Direct provider call `0` | Yes | PASS |

Canonical ownership is:

`HTTP response -> ProviderFailureClassifier -> ProviderAdapterResponse ->
ProviderCapabilityRegistry.record -> structured fail-closed result`.

`HTTP_451_EMPTY_SUCCESS_COLLAPSE_COUNT=0`. Region-restricted responses never
produce a numeric zero, Evidence, freshness success or stale payload fallback.
Fallback remains independently authorized for the exact provider identity and
dataset.

## Post-remediation Closure

| Root area | Final status | Evidence |
|---|---|---|
| A. Release/database | PASS | Java 17 standard JAR, Flyway content, PostgreSQL 16 empty V1-V13, existing V13 restart, checksum/readiness fail closed, packaged login/Session/logout |
| B. Provider capability | PASS | Unified pre-call owner, canonical 451 classifier, exact dataset observation, independent fallback, stale directory-only revalidation, first-call-one/next-call-zero matrix, architecture guard, truthful partial scan |
| C. CoinGlass | PASS | Nullable explicit RPM, five-state configuration contract, preflight/health/runtime/budget reuse, exact auth arguments, repository production-default guard |
| D. AI readiness | PASS | Full Maven B03 regression; exact model, budget, cache, fallback and secret-redaction behavior unchanged |
| E. Auth/bootstrap | PASS | Full Maven B04 regression plus packaged form-login/CSRF Session/logout smoke |

All GAP/PARTIAL entries above were either remediated in the existing owner or
protected by a passing regression test. No second capability registry,
instrument directory, CoinGlass owner, preflight owner, provider API, or
business skeleton was introduced.

`HTTP_451_PATH_MATRIX=COMPLETE`

`TARGET_RUNTIME_ROOT_CAUSE_MATRIX=COMPLETE`
