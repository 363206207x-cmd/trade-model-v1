# Global Missing Capability Register

## Priority Definitions

- P0: current producer/consumer contract can display the wrong meaning or block the core decision workspace.
- P1: major V1 model or runtime guarantee is partial and must be closed before claiming full alignment.
- P2: operational/product completeness gap that can follow core contract repair.

## P0 Register

| ID | Missing capability | Evidence | Classification | Closure criterion |
|---|---|---|---|---|
| GAP-P0-001 | Structured AI role-result persistence contract (`CLOSED`) | `schemaVersion=v1` producer -> `ai_role_results` -> mapper/VO -> strict Home consumer; real producer-to-Home integration covers separate roles and empty fallback | `IMPLEMENTED_AND_TRACED` | Closed by `AI_ROLE_STRUCTURED_CONTRACT_ALIGNMENT.md`; retain regression and sanitization guards. |
| GAP-P0-002 | Production persisted-OHLCV ingestion (`CLOSED`) | authoritative public-provider adapter -> validated/idempotent writer -> persisted freshness/provenance -> plan and OpportunityLog consumers; integration no longer seeds plan bars with test SQL | `IMPLEMENTED_AND_TRACED` | Closed by `PRODUCTION_OHLCV_INGESTION_ALIGNMENT.md`; retain default-off provider/scheduler gates and fail-closed regression coverage. |
| GAP-P0-003 | UserPosition source provenance | VO hardcodes every row to `MANUAL` | `WRONG_SOURCE_MAPPING` | Persisted source passes through mapper/service and Home excludes non-manual rows in a regression test. |
| GAP-P0-004 | Fail-closed external-context no-data state | absent imports return `READY/OK/LOW` | `SEMANTIC_DRIFT` | No-config, waiting, empty-confirmed, stale, degraded, and error states are typed and decision/monitor behavior is conservative. |
| GAP-P0-005 | Product-contract multi-timeframe decision | Decision engine reads only `1m`/`5m` and ignores requested primary set | `SEMANTIC_DRIFT` | `5m/15m/1h/4h` source inputs and convergence are traceable, configurable, and tested. |

## P1 Register

| ID | Missing capability | Evidence | Classification | Closure criterion |
|---|---|---|---|---|
| GAP-P1-001 | Declared eight-score influence contract | only trend score enters decision | `BACKEND_FIELD_UNUSED` | Each category is explicitly consumed or explicitly diagnostic-only in code, docs, API, and tests. |
| GAP-P1-002 | Stable score summary ordering | top three ordered by UUID-like ID | `SEMANTIC_DRIFT` | deterministic business priority/score ordering with test. |
| GAP-P1-003 | Clean plan readiness contract | readiness sentence is stored in `validPeriod` | `WRONG_SOURCE_MAPPING` | dedicated readiness/status field; `validPeriod` contains period only. |
| GAP-P1-004 | Source-backed fallback plan values | generic leverage and position text persists without boundary trace | `PLACEHOLDER_ONLY` | values remain null until authoritative calculation or are typed as non-plan policy hints outside the plan contract. |
| GAP-P1-005 | Asset transition authority | no legal transition graph; waiting/triggered lack writers | `NOT_IMPLEMENTED` | central transition rules, illegal-transition rejection, persistence, and full-state tests. |
| GAP-P1-006 | Unified Opportunity review truth | ReviewAggregate reads legacy missed table | `DEAD_OR_LEGACY_CODE` | all review APIs use `tm_opportunity_log`; legacy endpoints/table are retired or explicitly archived. |
| GAP-P1-007 | Global request/trace correlation | ApiResponse ID can diverge from request/run IDs | `WRONG_SOURCE_MAPPING` | one request context ID is propagated through header, envelope, run, decision, critical events, and tests. |
| GAP-P1-008 | PostgreSQL runtime-query proof | Dashboard overview has unadapted H2 `DATEDIFF` | `MISSING_TEST_COVERAGE` | PostgreSQL variant/portable query and bounded runtime test. |
| GAP-P1-009 | Unified UTC contract | local `LocalDateTime` plus timestamp-without-zone | `SEMANTIC_DRIFT` | documented UTC clock, DB/session policy, serialization, and cross-timezone tests. |
| GAP-P1-010 | Manual optional-field truth | browser silently sends quantity/leverage `1` | `RENDERED_NOT_BACKED` | omit unsupported values or visibly require/default them with an explicit backend contract. |
| GAP-P1-011 | Opportunity real outcome automation | evaluation is explicit and OHLCV-blocked | `BLOCKED_NO_REAL_DATA` | bounded scheduler/operator policy evaluates due rows from authoritative bars without trading effects. |
| GAP-P1-012 | Rule-config consumption completeness | confused/AI/missed groups exist but are not consumed | `BACKEND_FIELD_UNUSED` | policies load active version fail-closed and tests prove threshold changes. |

## P2 Register

| ID | Missing capability | Evidence | Classification | Closure criterion |
|---|---|---|---|---|
| GAP-P2-001 | Adjudication consistency synthesis | score/level/summary always null | `PLACEHOLDER_ONLY` | backend computes or intentionally removes fields; no fake score. |
| GAP-P2-002 | Push position-risk aggregate | Home hardcodes zero | `PLACEHOLDER_ONLY` | real aggregate source or null/not-available state. |
| GAP-P2-003 | Verified Telegram status source | fixed `WAITING_SYNC` | `NOT_IMPLEMENTED` | verified readonly connection source; config alone must not imply connected. |
| GAP-P2-004 | Complete user-config workflow | service/mapper and ping only | `IMPLEMENTED_NOT_RENDERED` | authenticated read/update API, validation, audit, and UI if in V1 scope. |
| GAP-P2-005 | Shared no-data state model | status strings differ across providers/modules | `SEMANTIC_DRIFT` | shared typed contract and renderer for all required states. |
| GAP-P2-006 | Distributed claims for write schedulers | only analysis has strong lease/fencing | `MISSING_TEST_COVERAGE` | per-job claim/idempotency strategy with multi-instance tests. |
| GAP-P2-007 | High-growth data retention | no complete archive/retention policy | `NOT_IMPLEMENTED` | retention windows, safe cleanup, audit preservation, and operations evidence. |
| GAP-P2-008 | Browser-level workflow evidence | template assertions dominate | `MISSING_TEST_COVERAGE` | Playwright/browser tests for Home, manual lifecycle, fail-closed plan, AI roles, and Review Center. |
| GAP-P2-009 | Real/versioned historical fixture | P4 reports missing fixture | `BLOCKED_NO_REAL_DATA` | licensed/versioned fixture, checksum, direct pipeline replay, deterministic evidence report. |
| GAP-P2-010 | API envelope consolidation | modern envelope and raw legacy VOs coexist | `SEMANTIC_DRIFT` | primary contracts standardized; legacy routes isolated/deprecated without breaking safety anchors. |

## Capabilities Intentionally Not Missing

The following are prohibited scope, not backlog items:

- auto-open
- auto-close
- auto-reverse
- order execution
- auto-trading
- external push send
- fake positions
- fake review records

They must not be introduced as a way to close any gap in this register.

## Exit Condition for Global Alignment

Global alignment can be called `FULLY_PROVEN` only after all P0 gaps are closed, applicable P1 gaps are closed, primary UI fields are source-traced, a real/versioned replay passes through the direct pipeline, and PostgreSQL runtime/API/browser tests pass without weakening no-trading guards. P2 operational items may remain only when explicitly classified as out of the claimed release scope rather than silently shown as complete.
