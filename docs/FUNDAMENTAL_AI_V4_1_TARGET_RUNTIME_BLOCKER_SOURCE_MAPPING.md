# Fundamental AI v4.1 Target Runtime Blocker Source Mapping

Status: `AUTHORIZATION_CANDIDATE`

Canonical product source:
`docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`

Acceptance evidence:
`FUNDAMENTAL_AI_V4_1_MERGED_MAIN_TARGET_RUNTIME_ACCEPTANCE.md`, captured
against exact merged-main commit
`3a6f56afaf6fbba3d094d532f7f9555a23ac30a1` on 2026-08-15.

This mapping authorizes no implementation by itself. It maps four reproduced
target-runtime blockers to the existing owners that a later exact package may
extend. The canonical product semantics, data-quality algorithm, thresholds,
AI authority, Candidate/Final boundary, UserPosition boundary and zero-trading
boundary remain unchanged.

## Blocker Mapping

| Blocker | Acceptance Evidence | Product Contract | Existing Owner | Authorized Action | Forbidden Shortcut |
|---|---|---|---|---|---|
| B01 - standard packaged JAR Flyway | Acceptance sections 3 and 16: `./mvnw clean package` produced a JAR without runtime Flyway; V1-V13 worked only after using the optional `flyway-migration` profile. | Product Source sections 1, 11 and Appendix D require traceable, fail-closed persistence; deployment runbook requires standard `clean package`, packaged startup and Flyway V1-V13. | Existing Maven/Spring Boot build, current Flyway V1-V13 chain, `application-prod.yml`, existing liveness/readiness. | Extend the standard release artifact so ordinary `clean package` contains the existing Flyway runtime and PostgreSQL support; verify packaged startup and migration failure fail closed. | No special-profile-only release, no `spring-boot:run` substitute, no second migration mechanism, and no rewrite of V1-V13. |
| B02 - provider capability and exact instrument coverage | Acceptance sections 6 and 16: default `ADAUSDT` lacked a Kraken mapping, Binance fallback returned HTTP 451, and complete Pool scans remained `PARTIAL`. | Product Source sections 3, 4 and Appendix D require Asset Pool-only discovery, truthful provenance, exact source state and fail-closed missing/unavailable data. | Existing canonical asset identity, instrument mapping, provider adapters, provider health, Asset Pool scan and AnalysisRun. | Extend the existing capability/mapping owner with exact canonical-instrument support state, classify HTTP 451 as `REGION_RESTRICTED`, and isolate per-asset scan failures. | No removal of ADA to hide the gap, no ADA/USD as ADA/USDT, no silent USD/USDT substitution, no fake provider success, no whole-scan erasure after one asset failure, and no second instrument directory. |
| B03 - application AI provider readiness | Acceptance sections 4, 7 and 16: exact model probes succeeded, while application providers remained not ready because RPM/cost/model-verification contracts were incomplete. | Product Source sections 7-9 and Appendix D require exact role authority, explicit failure/fallback traces, no fabricated role success and fail-closed configuration. | Existing AI Orchestrator, provider adapters, AITrace, budget/cost gate, provider health and application configuration. | Extend the existing readiness owner with explicit RPM/cost/budget configuration state, cached exact-model preflight and secret-safe target-runtime validation. | No silent model switch, no fallback presented as exact-model ready, no default zero treated as configured cost, no permanent price hardcoding, no key/full-prompt logging, and no second AI Orchestrator. |
| B04 - password bootstrap and readiness | Acceptance sections 2 and 16: the supplied initial password was rejected by the application policy and prevented a clean login bootstrap. | Product Source sections 1 and 15 require authenticated explicit user actions and fail-closed security; the deployment smoke requires login/session/logout. | Existing PasswordPolicy, PersonalUser, authentication bootstrap, Session login/logout and readiness/health. | Reuse the single PasswordPolicy for bootstrap and preflight, add a secret-safe compliant password generator, and align bootstrap failure with readiness. | No weaker password rules, no policy bypass, no secret in Git/logs, no overwrite of an existing user, no readiness UP after required bootstrap failure, and no second Auth owner. |

## Product-First Stop Rule Classification

| Finding | Class | Direct product impact | Reproduction evidence | Blocks current runtime stage |
|---|---|---|---|---|
| B01 | `BUILD_OR_RUNTIME_BLOCKER` | The documented release artifact cannot initialize the target PostgreSQL schema. | Standard JAR startup versus profile-built JAR in acceptance section 3. | YES |
| B02 | `REAL_DATA_INTEGRITY_BLOCKER` | A frozen default asset cannot obtain the exact instrument and one geo-restricted fallback keeps scans partial. | Per-symbol scan and HTTP 451 evidence in acceptance section 6. | YES |
| B03 | `BUILD_OR_RUNTIME_BLOCKER` | Exact-model account access does not make application providers ready or auditable. | Provider table and readiness reasons in acceptance section 4. | YES |
| B04 | `SECURITY_OR_PRIVACY_BLOCKER` | Secure bootstrap cannot create the real login user with the supplied configuration. | Password-policy rejection in acceptance section 2. | YES |

## Invariants

- `DATA_QUALITY_ALGORITHM_CHANGED=NO`
- `DATA_QUALITY_THRESHOLD_CHANGED=NO`
- `MARKET_BIAS_CONTRACT_CHANGED=NO`
- `OPPORTUNITY_STATE_CONTRACT_CHANGED=NO`
- `PLAN_MODE_CONTRACT_CHANGED=NO`
- `FAKE_DATA_ALLOWED=NO`
- `AUTOMATIC_TRADING_ALLOWED=NO`
