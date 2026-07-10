# Global Test Coverage Gap

## Existing Coverage That Carries Real Weight

| Capability | Representative coverage | What it proves | Classification |
|---|---|---|---|
| Analysis idempotency/lease | `AnalysisIdempotencyGuard*`, `AnalysisRunOrchestratorImplTest` | canonical key, lease/fencing/retry ownership | `IMPLEMENTED_AND_TRACED` |
| Analysis assembly | `AnalysisAssemblerServiceImplTest`, `AnalysisDecisionExecutionPlanIntegrationTest` | H2 transaction path with stub market data and seeded OHLCV | `TEST_ONLY` |
| Rule/AI boundary | `AiDecisionOrchestratorServiceImplTest`, `AiConflictResolverServiceImplTest` | review-only roles, fallback, four conflict levels | `IMPLEMENTED_AND_TRACED` |
| Confused recovery | `ConfusedStateServiceImplTest` | fail-close and two-cycle recovery | `IMPLEMENTED_AND_TRACED` |
| Plan boundary | source-trace boundary tests and fixture matrices | primary-timeframe fail-close and trace validation | `TEST_ONLY` |
| Dashboard Home | controller/service/template tests | stable Home shape, safe empty states, no-trade wording | `IMPLEMENTED_AND_TRACED` |
| Manual position lifecycle | controller/service/mapper and full lifecycle E2E | manual open -> monitor -> manual close -> review | `IMPLEMENTED_AND_TRACED` |
| Position monitor | service/log/scheduler tests | active-only logs, PnL, risk states, default-off scheduler | `IMPLEMENTED_AND_TRACED` |
| Push Recheck | service/controller/status tests | quote-unavailable fail-close and review-only states | `IMPLEMENTED_AND_TRACED` |
| Hot Reset | policy/service tests | config thresholds, idempotency, invalidation, after-commit rebuild | `IMPLEMENTED_AND_TRACED` |
| OpportunityLog | controller/service/mapper tests | record/evaluate/final classes/idempotency | `IMPLEMENTED_AND_TRACED` |
| Review Center | service/controller/template tests | four readonly tabs and stable empty lists | `IMPLEMENTED_AND_TRACED` |
| PostgreSQL structure | Flyway smoke, upsert/date-function guards | migrations and selected mapper variants | `IMPLEMENTED_AND_TRACED` |
| Historical replay | P2/P3/P4 tests | synthetic replay works; P4 loader validates absence/blocker | `TEST_ONLY` |
| Safety | `StaticNoTradeInstructionGuardTest` and review-only assemblers | prohibited trading/action surfaces remain absent | `IMPLEMENTED_AND_TRACED` |

## Critical Missing Coverage

| ID | Missing test | Why it matters | Classification |
|---|---|---|---|
| T-001 | Real producer `AiOrchestratorResult.toSanitizedSummary()` -> persisted decision -> `DashboardHomeServiceImpl` -> three role panels | Current Home tests inject JSON the producer never writes. | `MISSING_TEST_COVERAGE` |
| T-002 | Runtime OHLCV ingestion -> plan boundary -> Home complete suggestion | Tests seed OHLCV directly; no production writer is exercised. | `BLOCKED_NO_REAL_DATA` |
| T-003 | Requested `5m/15m/1h/4h` analysis -> real multi-timeframe convergence | Decision tests cover current `1m/5m` heuristic rather than product contract. | `MISSING_TEST_COVERAGE` |
| T-004 | All eight scores influence a documented decision contract | Only trend is consumed. | `MISSING_TEST_COVERAGE` |
| T-005 | Stable score summary ordering by declared priority/value | Current top-three UUID ordering is not asserted as business semantics. | `MISSING_TEST_COVERAGE` |
| T-006 | Non-manual UserPosition row is excluded from Dashboard Home | Hardcoded source can defeat the filter. | `MISSING_TEST_COVERAGE` |
| T-007 | Manual form omits optional quantity/leverage without browser defaults becoming facts | Current browser sends `1`. | `MISSING_TEST_COVERAGE` |
| T-008 | No external-context imports -> typed no-data state, not `READY/LOW` | Current default can mask missing data. | `MISSING_TEST_COVERAGE` |
| T-009 | Full legal/illegal asset transition table including waiting/triggered | No transition authority exists. | `MISSING_TEST_COVERAGE` |
| T-010 | Opportunity automatic evaluation over a real/versioned fixture | Evaluation is explicit and OHLCV-blocked. | `BLOCKED_NO_REAL_DATA` |
| T-011 | ReviewAggregate and ReviewCenter return the same OpportunityLog truth | Current aggregate reads legacy data. | `MISSING_TEST_COVERAGE` |
| T-012 | PostgreSQL execution of Dashboard overview latency SQL | Existing date-function guard omits `DashboardAggregationFacade`. | `MISSING_TEST_COVERAGE` |
| T-013 | Correlation equality across `X-Request-Id`, `ApiResponse.requestId`, run, trace, and critical events | IDs can be independently generated. | `MISSING_TEST_COVERAGE` |
| T-014 | UTC serialization/persistence across non-UTC JVM/DB timezone | `LocalDateTime` and timestamp-without-zone semantics are unproven. | `MISSING_TEST_COVERAGE` |
| T-015 | Multi-instance scheduler duplicate prevention outside analysis | Production policy is safe, but enabled clusters are unproven. | `MISSING_TEST_COVERAGE` |
| T-016 | Browser-level primary Home, legacy fallback, manual close modal, and Review Center navigation | Template string tests do not prove DOM behavior. | `MISSING_TEST_COVERAGE` |
| T-017 | Versioned real historical fixture through `V1DirectHistoricalReplayAdapter` | P4 explicitly reports fixture absence. | `BLOCKED_NO_REAL_DATA` |
| T-018 | Live provider timeout and stale-data behavior with bounded integration harness | No external calls were permitted and evidence is incomplete. | `BLOCKED_NO_REAL_DATA` |

## Tests That Can Mislead If Read as Full Proof

| Test pattern | Limitation | Classification |
|---|---|---|
| Dashboard AI role tests | seed a JSON shape not produced by the runtime decision writer | `TEST_ONLY` |
| Analysis/plan integration | uses stub klines and manually inserted persisted OHLCV | `MOCK_ONLY` |
| Synthetic historical replay | validates deterministic mechanics, not real market data | `MOCK_ONLY` |
| P4 real fixture validation | safely proves fixture is absent; it does not prove replay success | `BLOCKED_NO_REAL_DATA` |
| PostgreSQL Flyway PASS | proves migrations, not every mapper/service SQL statement | `TEST_ONLY` |
| Static no-trade guard | proves forbidden terms/classes/surfaces, not all semantic data correctness | `IMPLEMENTED_AND_TRACED` |

## Recommended Test Order

1. Add an AI producer-to-Home contract integration test before changing the payload.
2. Add UserPosition source-provenance and external-context no-data regression tests.
3. Add PostgreSQL runtime-query guard coverage for `DashboardAggregationFacade`.
4. Add a production OHLCV ingestion contract and bounded integration test.
5. Add four-primary-timeframe decision tests and eight-score consumption assertions.
6. Add legal asset-transition tests.
7. Add versioned real historical fixture replay and browser-level product-flow tests.
8. Add multi-instance scheduler claim tests before any additional production opt-in.

Passing the current suite is necessary but does not make `REAL_DATA_RUNTIME_EVIDENCE` or production readiness fully proven.
