# Global V1 Full-Stack Alignment Audit

## Audit Identity

- Audit date: 2026-07-10
- Base commit: `6390e7a1b83faed866165c7c9869d8e67af685d6`
- Branch: `codex/global-v1-fullstack-alignment-audit`
- Scope: read-only code, contract, persistence, UI semantics, tests, and runtime-evidence audit
- Runtime changes: none
- External provider calls: none
- Production access: none

## Primary Evidence Reviewed

- `V1_PROGRESS_SOURCE_OF_TRUTH.md` and `PROJECT_CURRENT_STATE.md`
- `GLOBAL_BACKEND_FRONTEND_ALIGNMENT_AUDIT.md` and `P0_BACKEND_FRONTEND_ALIGNMENT_CLOSURE_REVIEW.md`
- `V1_BUSINESS_STRESS_TEST_PLAN.md` and `V1_BUSINESS_STRESS_TEST_EVIDENCE.md`
- `V1_HISTORICAL_REPLAY_VALIDATION_PLAN.md` and `V1_HISTORICAL_REPLAY_VALIDATION_EVIDENCE.md`
- `V1_REAL_HISTORICAL_FIXTURE_CONTRACT.md`, `V1_REAL_HISTORICAL_REPLAY_VALIDATION_PLAN.md`, and `V1_REAL_HISTORICAL_REPLAY_VALIDATION_EVIDENCE.md`
- Current Java, mapper SQL, migration SQL, templates, scripts, and tests

## Executive Conclusion

V1 is not globally aligned end to end yet. The repository has strong no-trading guards, a real analysis orchestration path, authoritative persistence for many core records, controlled PostgreSQL migration evidence, and useful focused tests. Those strengths do not prove that every user-visible dashboard value is backed by the intended real source.

The largest gaps are concrete producer/consumer breaks rather than cosmetic incompleteness:

1. The analysis producer persists AI role results as a compact plain-text summary, while Dashboard Home expects JSON. Tests inject JSON directly and therefore do not exercise the production contract.
2. Execution-plan boundaries require persisted OHLCV, but no production writer for `tm_persisted_ohlcv_bar` was found. Complete plans are therefore blocked unless data is manually or test seeded.
3. The decision engine calls only `1m` and `5m` candles and uses fixed heuristics, while the product contract names `5m`, `15m`, `1h`, and `4h` as primary execution timeframes.
4. `UserPositionServiceImpl` labels every loaded row as `MANUAL`; Dashboard Home then trusts that label. A non-manual row can cross the manual-only display boundary.
5. Missing external-context rows become `READY` / `OK` / `LOW` instead of a fail-closed no-data state.
6. Eight score rows are persisted, but only the trend score materially enters the decision path and only three unstable rows are selected for summary display.

The correct project statement remains: V1 is local acceptance-ready for its guarded, review-only scope, but full-stack semantic alignment and production readiness are not proven. Production deployment remains `BLOCKED`.

## Final Verdict

| Dimension | Verdict | Reason |
|---|---|---|
| `CODE_PATH_ALIGNMENT` | `PARTIALLY_PROVEN` | Core orchestration, persistence, safety, review, and monitor paths exist, but important source and consumer contracts break. |
| `FRONTEND_SEMANTIC_ALIGNMENT` | `PARTIALLY_PROVEN` | The primary home renderer is guarded and mostly honest, but AI role payloads, execution-period semantics, position provenance, and diagnostic values are not fully aligned. |
| `STATE_MACHINE_ALIGNMENT` | `PARTIALLY_PROVEN` | Required states exist, but legal transitions are not centrally enforced and several states lack authoritative production writers. |
| `LOCAL_END_TO_END_ALIGNMENT` | `PARTIALLY_PROVEN` | Focused H2 integrations pass with stubs and seeded OHLCV; a real fixed fixture through the direct production assembly path is not proven. |
| `REAL_DATA_RUNTIME_EVIDENCE` | `BLOCKED_NO_REAL_DATA` | P4 records no versioned real historical fixture and no direct replay PASS; AI and external-context providers also lack live evidence. |
| `PRODUCTION_READINESS` | `NOT_PROVEN` | Deployment status remains `BLOCKED`; this audit is not a release approval. |

## Priority Findings

### P0 Contract Breaks

| ID | Finding | Classification | User impact |
|---|---|---|---|
| P0-01 | `DecisionEngineService` writes `AiOrchestratorResult.toSanitizedSummary()` as semicolon-delimited text, but `DashboardHomeServiceImpl.parseAiRoleResults()` accepts JSON only. | `WRONG_SOURCE_MAPPING` | Gemini and Grok role panels cannot reliably render real orchestrator results. |
| P0-02 | Plan boundary extraction reads `tm_persisted_ohlcv_bar`; no production ingestion writer was found. Test code inserts the rows directly. | `BLOCKED_NO_REAL_DATA` | Runtime plans normally remain boundary-incomplete even when the rest of an analysis succeeds. |
| P0-03 | `UserPositionServiceImpl.toVo()` hardcodes `sourceType=MANUAL`, defeating Dashboard Home's manual-only filter. | `WRONG_SOURCE_MAPPING` | Non-manual data could be presented as a user-entered position. |
| P0-04 | `ExternalContextEvidenceBuilder` defaults absent imports to `READY`, `OK`, and `LOW`. | `SEMANTIC_DRIFT` | Missing context can look healthy instead of unknown or unavailable. |
| P0-05 | Decision convergence is based on three `1m` and three `5m` candles, not the requested primary timeframe set. | `SEMANTIC_DRIFT` | Direction, confidence, conflict, and confused inputs do not represent the advertised multi-timeframe model. |

### P1 Alignment Gaps

| ID | Finding | Classification | User impact |
|---|---|---|---|
| P1-01 | Eight score categories are persisted with neutral baselines when data is missing; only `趋势结构分` is consumed by the rule decision. | `BACKEND_FIELD_UNUSED` | The score model looks broader than its actual decision influence. |
| P1-02 | `selectTop3BriefByAnalysisId` orders score rows by UUID-like `score_id DESC`, not business priority or score. | `SEMANTIC_DRIFT` | Summary score selection is unstable and not explainable. |
| P1-03 | Incomplete plans persist generic `1-5x` leverage and `单笔风险不超过总资金 2%` position text. | `PLACEHOLDER_ONLY` | The API can carry plan-like values without complete market boundaries. |
| P1-04 | The service writes `边界不足，等待结构确认` into `validPeriod`; the frontend repairs the meaning by treating it as readiness status. | `WRONG_SOURCE_MAPPING` | API consumers receive a status sentence in a period field. |
| P1-05 | Asset states define eight values, but transition legality is not centrally validated; several states lack a normal writer. | `NOT_IMPLEMENTED` | State history can be persisted without a proven legal transition graph. |
| P1-06 | `ReviewCenter` uses `tm_opportunity_log`, while `ReviewAggregateServiceImpl` still reads legacy `tm_missed_opportunity`. | `DEAD_OR_LEGACY_CODE` | Review endpoints can report different opportunity truth. |
| P1-07 | `ApiResponse` creates its own request ID instead of consistently reusing request context; legacy endpoints return raw VOs. | `SEMANTIC_DRIFT` | Header, response body, and persisted trace IDs can diverge. |
| P1-08 | `DashboardAggregationFacade` uses H2 `DATEDIFF` without a PostgreSQL database-id variant. | `MISSING_TEST_COVERAGE` | Flyway PASS does not prove the production overview query works on PostgreSQL. |

### P2 Completeness Gaps

| ID | Finding | Classification | User impact |
|---|---|---|---|
| P2-01 | Consistency score, level, and summary fields are present but always null. | `PLACEHOLDER_ONLY` | The adjudication card remains a waiting shell. |
| P2-02 | Push Inbox `positionRisk` is hardcoded to zero and Telegram remains `WAITING_SYNC`. | `PLACEHOLDER_ONLY` | Diagnostics look structurally complete but are not source-complete. |
| P2-03 | User config has mapper/service support but no complete user-facing read/write flow. | `IMPLEMENTED_NOT_RENDERED` | Config ownership cannot be exercised through the product UI. |
| P2-04 | Analysis has database lease/fencing; other write schedulers do not show equivalent distributed claims. | `MISSING_TEST_COVERAGE` | Multi-instance duplicate writes are not proven safe outside analysis runs. |

## Twenty-Area Alignment Summary

| Area | Classification | Audit conclusion |
|---|---|---|
| 1. Real data providers | `BLOCKED_NO_REAL_DATA` | Binance public quote/kline/OI/funding paths exist; liquidation and live external-context ingestion are absent, and no live provider evidence was produced here. |
| 2. Fixed analysis pipeline | `IMPLEMENTED_AND_TRACED` | Analysis run, idempotency key, lease/fencing, evidence, scores, decision, plan, snapshots, events, and trace fields are persisted transactionally. |
| 3. Core object ownership | `WRONG_SOURCE_MAPPING` | Authoritative models exist, but legacy duplicates and source-provenance errors remain. |
| 4. Eight scores | `BACKEND_FIELD_UNUSED` | All eight are stored; seven do not materially drive the decision and five are not summarized. |
| 5. Multi-timeframe convergence | `SEMANTIC_DRIFT` | Implemented as `1m`/`5m` candle comparison rather than the declared four primary timeframes. |
| 6. Asset state machine | `NOT_IMPLEMENTED` | Enum and persistence exist; an enforced transition authority does not. |
| 7. Rule/AI boundary | `IMPLEMENTED_AND_TRACED` | Rule direction remains authoritative and AI is review-only; structured role output contract is still broken. |
| 8. Confused state | `SEMANTIC_DRIFT` | Score, persistence, fail-closed read error, and two-cycle recovery exist; thresholds and upstream signals remain fixed or synthetic. |
| 9. Execution plan | `BLOCKED_NO_REAL_DATA` | Fail-closed boundary rules exist, but the required persisted OHLCV source is not produced at runtime. |
| 10. User position/monitor | `WRONG_SOURCE_MAPPING` | Manual lifecycle and read-only monitor are implemented; source provenance and frontend defaulted quantity/leverage are not aligned. |
| 11. Push/recheck | `IMPLEMENTED_AND_TRACED` | Review-only snapshots, quote-unavailable fail-close, logs, and tests exist; no external send is present. |
| 12. Hot Reset | `IMPLEMENTED_AND_TRACED` | Config-driven trigger, event idempotency, invalidation, persistence, and after-commit rebuild are covered. |
| 13. Opportunity/review | `SEMANTIC_DRIFT` | Authoritative OpportunityLog is implemented, but aggregate review still reads the legacy missed-opportunity source. |
| 14. Database/migrations | `MISSING_TEST_COVERAGE` | V1/V2/V3 PostgreSQL migration and clean restore evidence exist; runtime SQL and UTC behavior are not fully proven. |
| 15. API contracts | `SEMANTIC_DRIFT` | Modern envelopes coexist with raw legacy responses; request correlation and naming are inconsistent. |
| 16. Frontend mapping | `WRONG_SOURCE_MAPPING` | Home/review use real read-only APIs and safe empty states, but several backend fields are repaired or unused in the browser. |
| 17. Schedulers/locks | `MISSING_TEST_COVERAGE` | Production policy is fail-closed and position monitor is default-off; distributed coordination is not universal. |
| 18. Rule/user config | `IMPLEMENTED_NOT_RENDERED` | Rule config storage/reload is usable; several groups are not consumed and user config lacks a complete UI contract. |
| 19. No-data modes | `SEMANTIC_DRIFT` | `WAITING_SYNC`, `STALE`, `DISABLED`, and errors are fragmented; `EMPTY_CONFIRMED` is not an authoritative shared state. |
| 20. No-trading safety | `IMPLEMENTED_AND_TRACED` | Static guards, DTO flags, review-only APIs, and absence of order capability support the stated safety boundary. |

## Real, Synthetic, Placeholder, and Blocked Data

| Category | Examples | Classification |
|---|---|---|
| Real and traceable | Binance quote/kline/OI/funding clients, manual positions, monitor logs, analysis runs, decision results, push recheck logs, hot-reset events, OpportunityLog | `IMPLEMENTED_AND_TRACED` |
| Test seeded | Persisted OHLCV bars in integration tests, JSON AI role payloads in Dashboard Home tests, synthetic replay fixtures | `TEST_ONLY` |
| Placeholder/default | Neutral eight-score baselines, fallback leverage/position text, consistency fields, push position risk, Telegram waiting state | `PLACEHOLDER_ONLY` |
| Blocked without real data | Complete execution boundary, real four-timeframe convergence, real external-context quality, live AI-role evidence, P4 real fixture replay | `BLOCKED_NO_REAL_DATA` |

## Safety Boundary

This audit found no order execution service and introduced none. The reviewed V1 path remains review-only:

- no auto-open
- no auto-close
- no auto-reverse
- no order execution
- no auto-trading
- no external push send
- no fake positions
- no fake review records

The presence of execution suggestions, monitor advice, push rechecks, and Hot Reset does not change that boundary. They persist analysis, review, and diagnostic state only.

## Audit Limitations

- No external provider or production-server call was made.
- No real historical fixture was available for P4 direct replay.
- No browser screenshot was used as runtime proof in this audit.
- PostgreSQL migration evidence was reviewed from repository evidence; production runtime queries were not executed here.
- This audit classifies code and contract reality. It does not certify production readiness.

## Related Matrices

- `GLOBAL_FRONTEND_BACKEND_FIELD_MATRIX.md`
- `GLOBAL_API_CONTRACT_MATRIX.md`
- `GLOBAL_STATE_TRANSITION_MATRIX.md`
- `GLOBAL_DATABASE_USAGE_MATRIX.md`
- `GLOBAL_TEST_COVERAGE_GAP.md`
- `GLOBAL_MISSING_CAPABILITY_REGISTER.md`
- `GLOBAL_FULLSTACK_CLOSURE_PLAN.md`
