# Global Database Usage Matrix

## Schema and Migration Baseline

- `schema.sql` and Flyway `V1__baseline_schema_tables.sql` define the same 27 business tables.
- Flyway V2 adds indexes, V3 adds scheme rule-config defaults, and V4 adds additive OHLCV ingestion provenance fields.
- Repository evidence records controlled PostgreSQL 16 V1/V2/V3 migration PASS, backup PASS, and clean restore PASS with 27 `tm_*` tables and three successful Flyway rows.
- That evidence proves schema migration mechanics, not every PostgreSQL runtime query or production deployment.

## Table Ownership Matrix

| Table | Authoritative writer(s) | Main reader/product use | Classification / gap |
|---|---|---|---|
| `tm_analysis_run` | Analysis run orchestrator | trace APIs, dashboard overview | `IMPLEMENTED_AND_TRACED` |
| `tm_evidence_item` | Evidence service/assembler | decision input, diagnostics | `IMPLEMENTED_AND_TRACED` |
| `tm_score_item` | Score service/assembler | decision trend extraction, score diagnostics | `BACKEND_FIELD_UNUSED` for seven categories |
| `tm_macro_event` | external-context import API | context snapshot/review | `IMPLEMENTED_AND_TRACED` |
| `tm_news_event` | external-context import API | context snapshot/review | `IMPLEMENTED_AND_TRACED` |
| `tm_decision_result` | Decision service/assembler | Home, plan, review, trace | `IMPLEMENTED_AND_TRACED` |
| `tm_execution_plan` | Plan service/assembler | Home suggestion, monitor, push recheck | `BLOCKED_NO_REAL_DATA` for complete boundaries |
| `tm_market_environment_snapshot` | assembler/market environment service | trace/review | `IMPLEMENTED_AND_TRACED` |
| `tm_persisted_ohlcv_bar` | `PersistedOhlcvIngestionService` only for normal runtime writes | plan boundary and opportunity evaluation | `IMPLEMENTED_AND_TRACED`; public provider and scheduler default off |
| `tm_rule_config` | seeded migration/admin mapper | Hot Reset, Push Recheck, audit | `BACKEND_FIELD_UNUSED` for confused/AI/missed groups |
| `tm_user_config` | mapper/service only | no complete product flow | `IMPLEMENTED_NOT_RENDERED` |
| `tm_real_position` | Position Sync import path | system position status | `IMPLEMENTED_AND_TRACED` but separate from manual UI |
| `tm_user_position` | manual-open/manual-close | Home monitor, risk, review | `WRONG_SOURCE_MAPPING` because VO source is hardcoded |
| `tm_position_monitor_log` | Position Monitor | Home latest monitor, review history | `IMPLEMENTED_AND_TRACED` |
| `tm_push_snapshot` | analysis/push snapshot, recheck updates | Home inbox, Review Center | `IMPLEMENTED_AND_TRACED` |
| `tm_account_risk_snapshot` | analysis/account-risk service | Home/decision/review | `IMPLEMENTED_AND_TRACED` |
| `tm_push_recheck_log` | Push Recheck | Home/review/ops | `IMPLEMENTED_AND_TRACED` |
| `tm_push_recheck_dispatch_config` | dispatch config API/service | internal no-op dispatch policy | `IMPLEMENTED_AND_TRACED` |
| `tm_push_recheck_dispatch_config_audit` | dispatch config service | operations audit | `IMPLEMENTED_AND_TRACED` |
| `tm_monitor_alert` | monitor/alert service | Home alerts/review | `IMPLEMENTED_AND_TRACED` |
| `tm_opportunity_log` | analysis/opportunity service/evaluator | Review Center/stats | `IMPLEMENTED_AND_TRACED` |
| `tm_missed_opportunity` | legacy producer frozen/no-op | legacy ReviewAggregate/query | `DEAD_OR_LEGACY_CODE` |
| `tm_review_result` | explicit review save/feedback | Review Center/aggregate | `IMPLEMENTED_AND_TRACED` |
| `tm_rule_version_log` | review/rule governance | Review Center/rule history | `IMPLEMENTED_AND_TRACED` |
| `tm_asset_state` | Decision/Confused/Hot Reset | Home state, transition history | `IMPLEMENTED_AND_TRACED` storage; transition guard absent |
| `tm_hot_reset_event` | Hot Reset service | diagnostics/review/rebuild trace | `IMPLEMENTED_AND_TRACED` |
| `tm_ai_call_log` | AI orchestrator | AI audit/status | `IMPLEMENTED_AND_TRACED` |

## Producer/Consumer Gaps

| Gap | Evidence | Classification |
|---|---|---|
| OHLCV producer/consumer chain | public adapter -> validated/idempotent writer -> plan boundary and OpportunityLog integrations | `IMPLEMENTED_AND_TRACED` |
| AI role payload is not structured | decision row stores compact text; Home parser requires JSON | `WRONG_SOURCE_MAPPING` |
| Opportunity truth is split | Review Center reads `tm_opportunity_log`; aggregate reads legacy `tm_missed_opportunity` | `SEMANTIC_DRIFT` |
| Manual source is not preserved | persisted UserPosition source becomes hardcoded `MANUAL` in VO | `WRONG_SOURCE_MAPPING` |
| Duplicate account-risk models | two Java DO shapes target the same table | `DEAD_OR_LEGACY_CODE` |
| `DataSourceHealthDO`/`AssetDO` table ownership | no matching schema table or active mapper flow found | `DEAD_OR_LEGACY_CODE` |

## Transactions and Idempotency

| Flow | Transaction/idempotency evidence | Classification |
|---|---|---|
| Analysis run claim | canonical key, DB lease, fencing token, retry ownership | `IMPLEMENTED_AND_TRACED` |
| Analysis assembly | run/evidence/scores/decision/plan/snapshots/events persisted transactionally | `IMPLEMENTED_AND_TRACED` |
| Hot Reset | event-key idempotency and transactional invalidation; rebuild after commit | `IMPLEMENTED_AND_TRACED` |
| Opportunity evaluation | final-state/idempotency guard | `IMPLEMENTED_AND_TRACED` |
| Review save | result and rule-version log transaction | `IMPLEMENTED_AND_TRACED` |
| Manual open | no external request idempotency contract | `MISSING_TEST_COVERAGE` |
| Push recheck/replay | each invocation intentionally writes an audit log; no global request key | `MISSING_TEST_COVERAGE` |
| Position monitor scheduler | repeated scans can append logs; no distributed claim proven | `MISSING_TEST_COVERAGE` |
| OHLCV ingestion | deterministic source key, full-batch preflight, identical-content idempotency, conflicting-content rejection, and same-key overlap guard | `IMPLEMENTED_AND_TRACED` for a single process; distributed scheduling remains outside this closure |

## PostgreSQL Runtime Compatibility

| Area | Finding | Classification |
|---|---|---|
| Upserts/mapper variants | major H2 upserts have PostgreSQL database-id variants | `IMPLEMENTED_AND_TRACED` |
| Dashboard overview latency | `DashboardAggregationFacade` uses H2 `DATEDIFF('MILLISECOND', ...)` without a PostgreSQL variant | `MISSING_TEST_COVERAGE` |
| Migration SQL | controlled PostgreSQL 16 migration/restore evidence exists | `IMPLEMENTED_AND_TRACED` |
| OHLCV V4 migration | additive PostgreSQL-compatible columns/index plus bounded static SQL guard; controlled PostgreSQL V4 execution not rerun here | `MISSING_TEST_COVERAGE` for live PostgreSQL V4 evidence |
| Full runtime SQL | no all-mapper PostgreSQL execution suite | `MISSING_TEST_COVERAGE` |

## Time and Identifier Semantics

| Topic | Finding | Classification |
|---|---|---|
| Database time columns | migrations use `TIMESTAMP WITHOUT TIME ZONE` | `SEMANTIC_DRIFT` |
| Java time | most services use system-local `LocalDateTime.now()`; only analysis clock is explicitly normalized | `SEMANTIC_DRIFT` |
| API time | `serverTime` and record times do not share a documented global UTC serialization contract | `MISSING_TEST_COVERAGE` |
| IDs | string trace/domain IDs and BIGINT row IDs are intentionally mixed | `IMPLEMENTED_AND_TRACED` |
| Request correlation | response and persisted request IDs are not guaranteed identical | `WRONG_SOURCE_MAPPING` |

## Index and Retention Notes

V2 supplies indexes for primary lookup paths, but this audit found no complete retention/archival policy for high-growth tables such as analysis runs, evidence, scores, AI call logs, position monitor logs, push recheck logs, and Hot Reset events. Capacity behavior is therefore `NOT_IMPLEMENTED` as an operational data-lifecycle capability, even though current indexes are present.
