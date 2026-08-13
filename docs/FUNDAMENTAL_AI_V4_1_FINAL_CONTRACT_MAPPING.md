# Fundamental AI v4.1 Final Contract Mapping

Status: `ALL_CLAUSES_PASS_PENDING_INDEPENDENT_REAUDIT`

Authoritative source: `/Users/xuchao/Documents/唯一产品开发方案_最终冻结版.docx`

Authoritative source SHA-256:
`91bcfbd154bc43b2176107bfc65a948271e10e3e9862027f3647dc13bf5e0900`

This is the final clause-to-implementation matrix for PR #1177. `PASS` means
the candidate branch has an owned implementation and executable evidence. It
does not mean the branch is merged or effective on `main`.

| Final contract clause | Current owner / code | Status | Change in this implementation | Verification |
|---|---|---|---|---|
| 1. Product position and principles | `DecisionChainServiceImpl`, `DecisionChainRuleValidatorImpl`, existing P2 position owners | PASS | Preserved rule authority, fail-closed data behavior, manual-only position creation, and zero automatic trading capability | `DecisionChainRuleValidatorImplTest`, `UserPositionServiceImplTest`, forbidden-path scan |
| 2. Complete business loop | Analysis, Evidence, Score, Decision, Candidate, Resolver, Validation, Final, Position, Monitor, Review canonical owners | PASS | Connected the full identifier-bearing chain without a duplicate business stack | `AnalysisDecisionExecutionPlanIntegrationTest`, `DecisionChainPersistenceIntegrationTest`, `DecisionChainAuditQueryServiceImplTest` |
| 3. Asset Pool and dynamic Top 6 | `PersistentAssetPoolService`, `AssetPoolService`, `OpportunityPriorityRankingServiceImpl`, `HomeTopAssetProjection` | PASS | Enforced Pool-only persistent discovery, unbounded Pool management, isolated search preview, eligible/fresh/configured ranking, and no fixed-symbol backfill | `PersistentAssetPoolServiceTest`, `AssetPoolBackedUniverseSourceTest`, `DecisionChainSourceGateTest`, `OpportunityPriorityRankingServiceImplTest`, `DashboardHomeServiceImplTest` |
| 4. Data, evidence, eight scores, multi-timeframe | Existing provider/analysis/evidence/score owners plus AI input contract | PASS | Added evidence identity, provenance, numeric strength/confidence, observation/freshness, exact 4h/1h/15m/5m roles, and fail-closed quality gates | `EvidenceServiceImplTest`, `ScoreServiceImplTest`, `AiDecisionChainContractTest`, `DecisionChainAiOrchestratorServiceImplTest` |
| 5. Market Bias, Opportunity State, Plan Mode | `MarketBiasEnum`, `PlanModeEnum`, `MarketBiasPolicy`, resolver/validator/final owners | PASS | Normalized exact eight Bias values and five Plan Modes; separated state, direction, and permission; allowed only same-family downgrade | `MarketBiasPolicyTest`, `AiConflictResolverServiceImplTest`, `DecisionChainRuleValidatorImplTest`, migration smoke |
| 6. Opportunity state machine, debounce, cooling, Hot Reset | `AssetStateServiceImpl`, `AssetStateDO`, `OpportunityStateTransitionDO` | PASS | Kept one write entry, eight states, owner+symbol+timeframe identity, configured debounce/cooling, complete transition audit, and required precedence | `AssetStateServiceImplTest`, `HotResetServiceImplTest`, `AssetStateMapperIntegrationTest` |
| 7. Three-AI permissions and invocation gates | `DecisionChainAiOrchestratorServiceImpl`, role schemas/parser, `AiCallLogServiceImpl` | PASS | GPT creates Candidate only; Gemini reviews; Grok challenges; significant-change/quality gates control calls; all terminal paths create role-owned AITrace | `AiDecisionChainContractTest`, `AiRoleResultsCodecTest`, `DecisionChainAiOrchestratorServiceImplTest`, `AiCallLogServiceImplTest` |
| 8. Three-AI workspace explanation semantics | `AiDecisionChainSchema`, `AiRoleResultsPayload`, `DashboardHomeVO` | PASS | Added complete GPT/Gemini/Grok structures, metadata, evidence references, role state, collection states, and consistency summary without a fourth role | AI schema/codec/orchestrator/dashboard contract tests |
| 9. Four conflict levels, Confused, opportunity preservation | `AiConflictResolverServiceImpl`, `ConflictResolverResultDO`, state owner | PASS | Aligned Level 1-4, preserved Level 2/3 opportunities, allowed pause only for Level 4/independent confused/rule veto, and persisted before/after/recovery data | `AiConflictResolverServiceImplTest`, `ConfusedStateServiceImplTest`, `DecisionChainServiceImplTest` |
| 10. ExecutionPlanCandidate generation | Existing `ExecutionPlanCandidateDO` and mapper | PASS | Persisted complete rule/input lineage, plan body, source-backed numeric fields, risk, time, validity, and Candidate-only authority | `DecisionChainAiOrchestratorServiceImplTest`, `DecisionChainPersistenceIntegrationTest`, `DecisionChainRuleValidatorImplTest` |
| 11. Rule Validation and Final Execution Plan | `DecisionChainRuleValidatorImpl`, existing `ExecutionPlanDO` / mapper / VO | PASS | Kept Candidate and Final separate, made Rule Validation a non-AI owner, fail-closed legacy Finals, and required complete source/feasibility/risk/time contracts before Final | validator, persistence, controller source-gate, and PostgreSQL V12 tests |
| 12. Push Recheck and opportunity validity | Existing push/recheck owners | PASS | Preserved recheck as review-only, retained validity/recovery semantics, and prevented any trading authorization | `PushSnapshotServiceTest`, `MessagePushContractIntegrationTest` |
| 13. UserPosition and Position Monitoring | Existing P2 `UserPosition` and `PositionMonitorLog` owners | PASS | Preserved explicit manual/system-plan provenance, validated-Final association, per-position monitoring, verified/fresh trust gate, and null-on-missing behavior | lifecycle, ownership, position-monitor, and risk adapter suites |
| 14. Review, missed opportunities, rule feedback | Existing `ReviewResultDO` / service plus review policies and metrics contract | PASS | Added review type/outcome, resolver/validation provenance, deviation and role/rule assessments, metrics, and queryable responsibility chain | `ReviewServiceImplTest`, `ReviewFeedbackOwnershipIntegrationTest`, `ExecutionPlanReviewPolicyTest`, `ReviewMetricsContractTest` |
| 15. Home and frozen interaction data | Existing Dashboard Home contract | PASS | Bound dynamic Top 6, Final-only Execution Plan, independent Position data, one Three-AI workspace payload, and compact consistency data without Figma/Mobile changes | `DashboardHomeServiceImplTest`, Dashboard controller/shell contract tests |
| 16. Core objects, ownership, persistence | Existing canonical entities plus V11/V12 extension | PASS | Reused Analysis/Evidence/Score/Decision/Plan/Position/Monitor/Review; added only the authorized Asset, Candidate, Resolver and audit extensions; no duplicate owner | object ownership scan, mapper round trips, V12 migration smoke |
| 17. API and response contracts | `ApiResponse`, Asset Pool, Opportunity, AI audit, Plan and Dashboard controllers | PASS | Added safe grouped reads, strict field isolation, trace aggregation, Final-only responses, and `code/msg/request_id/server_time` envelope | `ApiResponseContractTest`, controller tests, source-gate tests |
| 18. Scheduling, idempotency, cache, quota, audit | analysis scheduler/idempotency owners and AI orchestration cache/trace path | PASS | Configured thresholds/windows, stable content cache with current-ID rebinding, cache-hit trace, request/trace/rule metadata, and significant-change call gating | scheduler, idempotency, orchestrator cache, trace tests |
| 19. Testing, acceptance, Capability Audit | Maven, H2 integration, PostgreSQL 16 migration, deterministic governance gates | PASS | Added full contract scenarios, V1-to-V12 historical migration evidence, full regression, Product Source and Workflow checks, and independent re-audit package | 4497-test aggregate, isolated PostgreSQL migration test, gate outputs |
| 20. Reuse, extension, deprecation | Ownership report and code scan | PASS | Extended canonical owners, retained legacy compatibility as fail-closed evidence, deleted no code without dead-code proof, and introduced no second decision stack | ownership/diff review and full regression |
| Appendix A. Market Bias x Plan Mode | `MarketBiasPolicy`, resolver and validator | PASS | Enforced exact legal combinations while keeping directional bias separate from execution permission | bias/resolver/validator matrix tests |
| Appendix B. Opportunity transitions | `AssetStateServiceImpl` and transition mapper | PASS | Covered allowed/blocked paths, precedence, debounce, cooling and full audit metadata | state-machine and mapper integration tests |
| Appendix C. Minimum object fields | Canonical entities, V12, DTO/VO contracts | PASS | Completed identity, source, time, version and cross-chain references for every frozen owner | migration, mapper, serialization and API tests |
| Appendix D. Final acceptance checklist | All owners and final validation artifacts | PASS | Executed the complete decision-chain regression and safety review; produced the remediation, test, schema/API, gap and handoff records | this matrix and `FUNDAMENTAL_AI_V4_1_TEST_REPORT.md` |

## State Semantic Separation

- Role state is exactly `READY`, `PARTIAL`, `FALLBACK`, `UNAVAILABLE`, or
  `ERROR`.
- Collection state is exactly `FOUND`, `NONE_FOUND`, `INSUFFICIENT_DATA`,
  `SOURCE_UNAVAILABLE`, or `STALE`.
- Grok `failurePathState` additionally allows
  `NO_VERIFIABLE_FAILURE_PATH`.
- GPT arrays: `supportingEvidence` and `opposingEvidence` each have their own
  state.
- Gemini arrays: `evidenceGaps`, `logicConflicts`, and
  `underestimatedRisks` each have their own state.
- Grok arrays: `failurePaths`, `opposingScenarios`, `externalEventRisks`,
  `microstructureRisks`, and `watchIndicators` each have their own state.
- Every role result carries `analysisId`, `traceId`, `roleState`, and
  `generatedAt`.
- Empty verified collections remain arrays with an explicit state; unavailable
  or stale sources cannot be represented as successful empty findings.

## Audit Ownership

- `AITrace` / `tm_ai_call_log` contains only GPT, Gemini, and Grok calls.
- `ConflictResolverResult` owns resolver inputs, before/after values,
  downgrade/veto reasons, confused decision, and recovery condition.
- Rule Validation is owned by `validationResultId` and the validated Final
  record, never by a fabricated AI role.
- `analysisId`, `candidateId`, and `traceId` aggregate the ordered chain.

## Final Regression Gate

| Required proof | Status |
|---|---|
| Asset Pool and dynamic Top 6 | PASS |
| Search preview with on-demand Three-AI analysis | PASS |
| Market Bias eight levels | PASS |
| Opportunity State eight states | PASS |
| Plan Mode five levels | PASS |
| Three-AI permissions and structured semantics | PASS |
| Anti-hallucination null/collection-state contract | PASS |
| Candidate/Final isolation | PASS |
| Conflict Resolver | PASS |
| Rule Validation | PASS |
| Plan source gate | PASS |
| Push Recheck is not trading authorization | PASS |
| UserPosition and Position Monitoring separation | PASS |
| Review responsibility chain | PASS |
| Automatic trading capability count | 0 |

## Boundaries

- Figma and Mobile are unchanged.
- No automatic open, close, add, reduce, reverse, order, or exchange execution
  capability exists.
- No fake market, evidence, AI, position, plan, or review data is accepted.
- PR #1177 remains unmerged; effectiveness on `main` is not claimed.

`READY_FOR_INDEPENDENT_FINAL_REAUDIT`
