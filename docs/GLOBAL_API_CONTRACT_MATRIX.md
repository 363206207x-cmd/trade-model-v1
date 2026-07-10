# Global API Contract Matrix

## Contract Baseline

Modern V1 APIs generally use `ApiResponse<T>` and authenticated access. Legacy dashboard diagnostics retain raw response contracts. Request context, response-body `requestId`, analysis `requestId`, and `X-Request-Id` are not globally guaranteed to be the same value.

## Primary Product APIs

| Route | Method | Contract and behavior | Classification |
|---|---|---|---|
| `/api/dashboard/home` | GET | `ApiResponse<DashboardHomeVO>`; primary Dashboard renderer | `IMPLEMENTED_AND_TRACED` |
| `/api/review/center` | GET | `ApiResponse<ReviewCenterVO>`; primary Review Center renderer | `IMPLEMENTED_AND_TRACED` |
| `/api/analysis/runs` | POST | Canonical persisted run with idempotency key, DB lease, fencing, and retry | `IMPLEMENTED_AND_TRACED` |
| `/api/analysis/runs/{analysisId}` | GET | Run lookup | `IMPLEMENTED_AND_TRACED` |
| `/api/analysis/runs/by-request/{requestId}` | GET | Request lookup | `IMPLEMENTED_AND_TRACED` |
| `/api/analysis/traces/{traceId}` | GET | Trace aggregate | `IMPLEMENTED_AND_TRACED` |
| `/api/analysis/scheduler/status` | GET | Scheduler status | `IMPLEMENTED_AND_TRACED` |
| `/api/user-positions/manual-open` | POST | Creates a manual row; no client idempotency key | `MISSING_TEST_COVERAGE` |
| `/api/user-positions/{id}/manual-close` | POST | State-guarded manual close | `IMPLEMENTED_AND_TRACED` |
| `/api/user-positions/open`, `/{id}` | GET | Position reads; service loses persisted source provenance | `WRONG_SOURCE_MAPPING` |
| `/api/position-monitor/user-positions/{positionId}/run` | POST | Writes monitor log only; never closes or trades | `IMPLEMENTED_AND_TRACED` |
| `/api/position-monitor/user-positions/open/run` | POST | Scans open rows and writes monitor logs only | `IMPLEMENTED_AND_TRACED` |
| `/api/push/recheck/{pushId}` | POST | Review-only recheck; writes log/snapshot and never becomes executable | `IMPLEMENTED_AND_TRACED` |
| `/api/push/recheck/{pushId}/latest`, `/{pushId}/logs` | GET | Recheck reads | `IMPLEMENTED_AND_TRACED` |
| `/api/opportunity-log/{opportunityId}`, `/query` | GET | Authoritative OpportunityLog reads | `IMPLEMENTED_AND_TRACED` |
| `/api/opportunity-log/{opportunityId}/evaluate` | POST | Idempotent final evaluation; requires persisted OHLCV | `BLOCKED_NO_REAL_DATA` |

## Review and Governance APIs

| Route group | Behavior | Classification |
|---|---|---|
| `/api/review/aggregate/{analysisId}` plus summary/detail | Legacy aggregate reads legacy missed-opportunity source | `DEAD_OR_LEGACY_CODE` |
| `/api/review/rule-version-logs` | Rule-version history | `IMPLEMENTED_AND_TRACED` |
| `/api/review/positions/{positionId}/monitor-logs` | Monitor history | `IMPLEMENTED_AND_TRACED` |
| `/api/review/user-positions/{positionId}/summary` | Position review summary | `IMPLEMENTED_AND_TRACED` |
| `/api/review/user-positions/{positionId}/feedback` | Persists manual feedback only | `IMPLEMENTED_AND_TRACED` |
| `/api/review/opportunities/stats`, `/state/{analysisId}` | Readonly review statistics/state | `IMPLEMENTED_AND_TRACED` |
| `/api/review/save` | Transactional review result and rule-version log; no automatic rule update | `IMPLEMENTED_AND_TRACED` |
| `/api/missed-opportunity/query`, `/review-archive-status` | Legacy missed-opportunity reads | `DEAD_OR_LEGACY_CODE` |

## Provider and External Context APIs

| Route group | Behavior | Classification |
|---|---|---|
| `/api/market/real-fetch` | External Binance kline read | `IMPLEMENTED_AND_TRACED` |
| `/api/market/quote-status` | Quote provider/config status | `IMPLEMENTED_AND_TRACED` |
| `/api/external-context/*/import` | Persists caller-supplied macro/news events | `IMPLEMENTED_AND_TRACED` |
| `/api/external-context/macro-events`, `/news-events`, `/events/...` | Imported-event reads | `IMPLEMENTED_AND_TRACED` |
| `/api/external-context/current` | Missing imports default to healthy/low-risk context | `SEMANTIC_DRIFT` |
| `/api/external-context/dashboard-status` | Readiness/config projection | `IMPLEMENTED_AND_TRACED` |
| `/api/ai/orchestrator/status`, `/call-logs` | AI readiness and call audit | `IMPLEMENTED_AND_TRACED` |

## Direct Build and Legacy Contracts

| Route | Contract concern | Classification |
|---|---|---|
| `/api/evidence/build` | Can build a partial object flow outside canonical analysis orchestration | `DEAD_OR_LEGACY_CODE` |
| `/api/score/build` | Bypasses canonical run idempotency/trace ownership | `DEAD_OR_LEGACY_CODE` |
| `/api/score/list` | Diagnostic score read, not primary product contract | `IMPLEMENTED_NOT_RENDERED` |
| `/api/plan/generate` | Can bypass canonical pipeline preconditions | `DEAD_OR_LEGACY_CODE` |
| `/api/rule/reload` | GET mutates in-memory rule cache | `SEMANTIC_DRIFT` |
| `/api/rule/push-watchlist`, `/config-audit-status` | Readonly rule projections | `IMPLEMENTED_AND_TRACED` |
| `/user-config/ping` | Connectivity only; no complete product config CRUD | `PLACEHOLDER_ONLY` |

## Dashboard Contracts

| Route group | Finding | Classification |
|---|---|---|
| `/dashboard` | Primary HTML page | `IMPLEMENTED_AND_TRACED` |
| `/api/dashboard/summary`, `/detail`, `/refresh` | Raw legacy contracts used only by fallback/diagnostics | `SEMANTIC_DRIFT` |
| `/api/dashboard/overview` | PostgreSQL runtime query includes unqualified H2 `DATEDIFF` | `MISSING_TEST_COVERAGE` |
| `/api/dashboard/analysis-status`, `/scheduler-status`, `/trace-summary` | Readonly operational views | `IMPLEMENTED_AND_TRACED` |
| evidence/decision/plan/risk/alert/push/recheck/review diagnostic status routes | Hidden or legacy diagnostics, not primary Home contract | `IMPLEMENTED_NOT_RENDERED` |
| data-source/source-quality status routes | Fragmented source and no-data semantics | `SEMANTIC_DRIFT` |
| paper observation/account risk/Hot Reset status routes | Readonly safety diagnostics | `IMPLEMENTED_NOT_RENDERED` |

## Operations Contracts

| Route group | Finding | Classification |
|---|---|---|
| `/api/system/health`, `/position-sync-status`, `/runtime-readiness-guardrail-status` | Readonly health/safety | `IMPLEMENTED_AND_TRACED` |
| `/api/system/run-baseline` | GET naming masks service work initiation | `SEMANTIC_DRIFT` |
| `/api/monitor/status`, `/api/account-risk/user-positions/current` | Readonly operational views | `IMPLEMENTED_AND_TRACED` |
| `/api/push/recheck/dispatch/config` GET/POST | Internal no-op dispatch config, not external send | `IMPLEMENTED_AND_TRACED` |
| `/api/push/recheck/replay` | Writes replay recheck logs; no trading effect | `IMPLEMENTED_AND_TRACED` |
| replay summary/ops overview | Readonly operations | `IMPLEMENTED_AND_TRACED` |

## Cross-Cutting Contract Findings

| Question | Finding | Classification |
|---|---|---|
| Common envelope | Modern `ApiResponse` and raw legacy VOs coexist. | `SEMANTIC_DRIFT` |
| Request correlation | `ApiResponse.success/fail` can generate an ID independently of `RequestIdSupport`. | `WRONG_SOURCE_MAPPING` |
| Naming | Java serialization exposes `requestId` / `serverTime`; no global snake-case contract exists. | `SEMANTIC_DRIFT` |
| Empty collections | Home and Review Center return stable empty lists/objects. | `IMPLEMENTED_AND_TRACED` |
| Provider errors | Several paths collapse errors into empty/unknown without a shared typed state. | `SEMANTIC_DRIFT` |
| Mutation idempotency | Analysis, Hot Reset, and opportunity evaluation are guarded; manual-open/replay/recheck lack a common request-key policy. | `MISSING_TEST_COVERAGE` |
