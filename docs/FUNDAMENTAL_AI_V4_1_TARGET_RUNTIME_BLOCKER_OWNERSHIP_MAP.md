# Fundamental AI v4.1 Target Runtime Blocker Ownership Map

Status: `AUTHORIZATION_CANDIDATE`

This map applies only to
`FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION`. It prevents a later
implementation from creating a parallel build, migration, provider, AI or
authentication stack.

## Ownership Decisions

| Capability | Canonical existing owner | Decision | Permitted extension | Forbidden duplicate |
|---|---|---|---|---|
| Release packaging | Existing Maven and Spring Boot build | `EXTEND` | Runtime dependency scope and packaged-artifact verification | Second build/release system |
| Database migration | Existing Flyway V1-V13 chain | `REUSE` | Standard-JAR runtime wiring and fail-closed readiness evidence | Second migration mechanism or rewritten applied migration |
| Production configuration | Existing `application.yml` / `application-prod.yml` | `EXTEND` | Explicit target-runtime flags required by the four blockers | Parallel config loader |
| Asset identity | Existing canonical Asset/AssetPool identity | `REUSE` | None beyond existing identity references | Second Asset owner |
| Instrument directory | Existing instrument mapping and provider symbol owners | `EXTEND` | Capability state, exact quote/market mapping and verified timestamp | Second instrument catalog |
| Provider execution | Existing provider adapters/orchestration | `EXTEND` | Capability selection and region-restriction handling | Second provider system |
| Provider health | Existing provider health/readiness | `EXTEND` | Exact capability/readiness reasons | Parallel health service |
| Pool scan | Existing Asset Pool scan and AnalysisRun | `EXTEND` | Per-asset result isolation and aggregate counts | Second scan/analysis pipeline |
| CoinGlass | Existing CoinGlass provider | `REUSE` | Config/readiness/provenance verification only | Second CoinGlass adapter |
| AI orchestration | Existing AI Orchestrator and role adapters | `REUSE` | No authority change | Second orchestrator or role family |
| AI audit | Existing AITrace/call-log owner | `EXTEND` | Readiness/preflight status references, never secrets | Parallel AI trace family |
| AI cost/budget | Existing budget/cost gate | `EXTEND` | Explicit missing-versus-zero configuration and version | Parallel budget owner |
| Password policy | Existing PasswordPolicy | `REUSE` | Shared invocation from bootstrap/preflight/generator validation | Script-local copied policy |
| User/bootstrap | Existing PersonalUser and bootstrap service | `EXTEND` | Explicit preflight state and readiness outcome | Second user/auth bootstrap |
| Login/session | Existing Spring Security login/session/logout | `REUSE` | Integration validation only | Second authentication stack |
| Readiness | Existing actuator/readiness contributors | `EXTEND` | Migration/bootstrap/provider blocker state | Parallel readiness endpoint |

## Supporting Semantics

The later implementation may introduce a supporting object only when the
existing owner cannot express an independent queryable state, the object has a
single write owner and tests, and it does not become a second business system.

| Supporting semantic | Classification | Owner |
|---|---|---|
| Provider capability state/record | `NEW_SUPPORTING_OBJECT` only if existing mapping cannot carry the state; otherwise `EXTEND` | Existing instrument/provider capability owner |
| AI provider readiness state | `NEW_SUPPORTING_OBJECT` only if current provider health cannot carry exact reasons; otherwise `EXTEND` | Existing AI provider health owner |
| Target runtime preflight result | `NEW_SUPPORTING_OBJECT` as a non-business validation result | Existing deployment/preflight workflow |
| Bootstrap preflight result | `NEW_SUPPORTING_OBJECT` as a security/readiness result | Existing auth bootstrap owner |

## Forbidden Duplicate Families

`Asset`, `Instrument directory`, `Provider system`, `Analysis`, `AI
Orchestrator`, `Auth`, `Migration`, `Opportunity`, `Execution Plan`,
`Position`, and `Review` are all `FORBIDDEN_DUPLICATE`.

## Duplicate Skeleton Gate

- Creates a new DTO/Validator/Assembler/Orchestrator by authorization: NO.
- Reuses existing owners: YES.
- Increases duplicate skeleton surface: NO.
- Capability movement in this authorization package: NONE.
- Later remediation must connect real packaged/runtime behavior: YES.
- Compliance with the #830 freeze recommendation: PASS.
