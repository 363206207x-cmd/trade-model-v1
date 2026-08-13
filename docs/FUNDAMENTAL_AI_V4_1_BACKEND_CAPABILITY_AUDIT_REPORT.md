# Fundamental AI v4.1 Backend Capability Audit Report

## 1. Audit Summary

- Audit target: PR `#1177`
- PR state at audit: `OPEN / DRAFT / MERGEABLE / UNMERGED`
- Head: `c4462e8f98a19d37328da29783d98fb696befba3`
- Base: `fb2722c7daa3acaa528131928222fcbbdc079081`
- Audit mode: `READ_ONLY`
- Business code changed by this audit: `NO`
- API changed by this audit: `NO`
- Schema changed by this audit: `NO`
- Figma changed by this audit: `NO`
- Mobile changed by this audit: `NO`

Sources of truth:

1. `docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`
2. `Fundamental_AI_v4_1_Codex_v2.docx`
3. `docs/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN_AUTHORIZATION.md`
4. `docs/FUNDAMENTAL_AI_V4_1_OBJECT_OWNERSHIP_MAP.md`
5. PR `#1177` implementation and tests

The implementation establishes most of the intended chain: persistent Asset
Pool, Analysis/Evidence/Score/Decision integration, separated Candidate and
Final plan persistence, three isolated AI roles, conflict resolution, Rule
Validation, manual-only UserPosition creation, and Review provenance for new
records. The build and CI are green.

The frozen contract is nevertheless not fully implemented. Four blocking
findings remain:

1. The canonical Opportunity state entry cannot perform required
   `CONFUSED -> COOLING` or `INVALIDATED -> COOLING` recovery and its debounce
   identity omits timeframe.
2. Several AI failure paths do not create a complete AITrace, timeout/error
   traces can omit latency, and the trace query API omits the persisted role
   output.
3. New manual UserPositions may be created without the contract-mandatory
   `final_plan_id` association.
4. The v4.1 Conflict Resolver persists `NONE/MINOR/MAJOR/EXTREME` instead of
   the frozen and already canonical Level 1-4 conflict contract.

**Merge Recommendation: `REQUEST_CHANGES`**

## 2. Capability Matrix

| Module | Status | Evidence | Finding |
|---|---|---|---|
| Asset Pool | PASS | `PersistentAssetPoolService.java:38-155`; `AssetPoolController.java:33-68`; `BinanceMarketAssetCatalog.java:49-138` | System/user pools, fuzzy market search, add, remove, restore, focus list, and scan exist. |
| Home focus assets | PASS | `DashboardHomeServiceImpl.java:217-240,627-656,716-730` | The Spring runtime path obtains focus symbols from Asset Pool; hard-coded defaults are confined to the legacy direct-constructor compatibility path. |
| Asset Pool-only Opportunity source | PASS | `DecisionChainServiceImpl.java:75-86`; `AnalysisSchedulerService.java:188-195` | Non-pool symbols fail closed before Candidate creation; production scheduler symbols come from Asset Pool. |
| Opportunity chain | PASS | `AnalysisAssemblerServiceImpl.java:337-425,770-889`; `DecisionChainServiceImpl.java:75-136` | Analysis, Evidence, Score, Decision, Opportunity, Candidate, Resolver, Validation, and Final are connected by analysis/trace identifiers. |
| Opportunity state model | PASS | `V11__fundamental_ai_v4_1_decision_chain.sql:38-102` | All eight frozen states and persisted transition audit fields exist. |
| State transition behavior | FAIL | `AssetStateServiceImpl.java:95-173,243-279`; `AssetStateServiceImplTest.java:160-218` | Cooling recovery and asset+timeframe debounce do not satisfy the frozen contract. See `V41-AUDIT-B01`. |
| AI role separation | PASS | `AiDecisionChainSchema.java`; `DecisionChainAiOrchestratorServiceImpl.java:91-104`; `V11__fundamental_ai_v4_1_decision_chain.sql:272-309` | GPT can produce a Candidate; Gemini/Grok remain review/challenge-only; all roles are blocked from Final, state, position, and order authority. |
| AI trace and fallback | FAIL | `DecisionChainAiOrchestratorServiceImpl.java:46-88`; `AbstractSafeAiProviderClient.java:141-184`; `AiOrchestratorController.java:72-85,110-139` | Not every attempted role/fallback has a complete, queryable trace. See `V41-AUDIT-B02`. |
| Candidate/Final separation | PASS | `DecisionChainServiceImpl.java:96-136,321-370`; `V11__fundamental_ai_v4_1_decision_chain.sql:104-159,209-253` | Candidate and Final are separate identities and storage; Final requires Rule Validation PASS. |
| Rule Validation authority | PASS | `DecisionChainRuleValidatorImpl.java:25-106`; `DecisionChainServiceImpl.java:128-136` | Direction, confidence, risk, state, source gate, boundaries, and automatic-action restrictions are validated before Final. |
| Conflict Resolver structure | FAIL | `AiConflictResolverServiceImpl.java:72-187`; `AiDecisionChainSchema.java:48-66`; `AiConflictLevelEnum.java:1-2` | Inputs/outputs are structurally complete, but conflict level semantics diverge from the frozen Level 1-4 contract. See `V41-AUDIT-B04`. |
| Opportunity preservation | PASS | `OpportunityTransitionResult.java`; `DecisionChainServiceImpl.java:115-136`; `AiConflictResolverServiceImpl.java:141-186` | Opportunity state and execution permission remain separate; AI changes confidence/risk/mode/confused rather than deleting the Opportunity. |
| Position Monitoring safety boundary | PASS | `UserPositionServiceImpl.java:48-89`; `DecisionChainRuleValidatorImpl.java:55-58,103-105` | There is no plan-to-position conversion, automatic open, automatic close, automatic reverse, or order path. Existing P2 monitoring is reused. |
| Final Plan to UserPosition traceability | FAIL | `UserPositionServiceImpl.java:63,250-265`; `V11__fundamental_ai_v4_1_decision_chain.sql:255-261` | `finalPlanId` is validated only when supplied and remains optional for new manual positions. See `V41-AUDIT-B03`. |
| Object ownership | PASS | `FUNDAMENTAL_AI_V4_1_OBJECT_OWNERSHIP_MAP.md`; V11 migration | Existing Analysis, Evidence, Score, DecisionBundle, ExecutionPlan, UserPosition, PositionMonitorLog, Review, and AI call-log owners are reused. |
| Schema/API contract | FAIL | `V11__fundamental_ai_v4_1_decision_chain.sql`; `AiOrchestratorController.java:72-139`; `UserPositionServiceImpl.java:250-265` | Candidate is not exposed as Final and Final is DB-constrained, but trace output is not queryable and mandatory Final-to-Position linkage is not enforced. |
| Review extension | PASS | `ReviewServiceImpl.java:65-118,197-217`; `ReviewResultMapper.java:44-60` | New Review rows can capture Final/Candidate/Trace provenance. A legacy-update caveat is recorded as non-blocking debt. |
| No automatic trading | PASS | V11 safety constraints; `DecisionChainRuleValidatorImpl.java:102-105,148-150`; repository path scan | No automatic order, open, close, or reverse capability was added. |
| Test execution | PASS | Local Maven and PR CI evidence | The executed tests are green. |
| Required contract coverage | FAIL | `AssetStateServiceImplTest.java:160-218`; `DecisionChainAiOrchestratorServiceImplTest.java:70-85`; `UserPositionServiceImplTest.java:53-83` | Critical contract failures are asserted as valid behavior or are not covered. See Section 6. |

Requested status summary:

- `ASSET_POOL_STATUS: PASS`
- `OPPORTUNITY_CHAIN_STATUS: PASS`
- `STATE_MACHINE_STATUS: FAIL`
- `AI_ROLE_SEPARATION_STATUS: PASS`
- `AI_TRACE_STATUS: FAIL`
- `EXECUTION_PLAN_STATUS: PASS`
- `CONFLICT_RESOLVER_STATUS: FAIL`
- `OPPORTUNITY_PRESERVATION_STATUS: PASS`
- `POSITION_BOUNDARY_STATUS: FAIL`
- `SCHEMA_API_STATUS: FAIL`
- `TEST_EXECUTION_STATUS: PASS`
- `TEST_CONTRACT_COVERAGE_STATUS: FAIL`

## 3. Blocking Findings

### V41-AUDIT-B01 - Canonical state machine blocks required cooling recovery and omits timeframe from debounce identity

- `FINDING_ID: V41-AUDIT-B01`
- `BLOCKER_CLASS: PRODUCT_SEMANTIC_BLOCKER`
- `BLOCKS_CURRENT_STAGE: YES`

Frozen contract:

- Opportunity has exactly eight states.
- State mutation uses one canonical entry.
- Debounce applies to the same asset plus the same timeframe.
- Invalidated enters Cooling; Cooling suppresses ordinary promotion.
- Priority remains Hot Reset > Confused > Invalidated > ordinary transitions.

Current implementation:

- `AssetStateService.transition(...)` accepts `symbol` but no `timeframe`
  (`AssetStateServiceImpl.java:95-103`).
- The authoritative row is selected by symbol only
  (`AssetStateServiceImpl.java:109`).
- Debounce is based on the symbol row's `lastUpdateTime` only
  (`AssetStateServiceImpl.java:270-279`).
- `applyPrecedence` returns the current `CONFUSED` or `INVALIDATED` state before
  a requested `COOLING` can be applied
  (`AssetStateServiceImpl.java:251-267`).
- `ConfusedStateServiceImpl` explicitly requests Cooling after the required
  low-score streak (`ConfusedStateServiceImpl.java:69-82`), but the canonical
  transition entry suppresses that request.
- The state tests assert the absorbing behavior and only test suppression once
  already Cooling (`AssetStateServiceImplTest.java:160-218`).

`DIRECT_PRODUCT_IMPACT:` A Confused or Invalidated Opportunity cannot recover
through the frozen Cooling lifecycle except by Hot Reset. Analyses for two
timeframes of the same asset can also debounce one another.

`REPRODUCTION_EVIDENCE:` With a current `CONFUSED` or `INVALIDATED` row and an
ordinary request for `COOLING`, `applyPrecedence` returns the current state.
With a recent update for one timeframe, a second timeframe shares the same
symbol row and debounce timestamp.

Required contract correction:

1. Make debounce identity include symbol and timeframe.
2. Permit the contract-defined Confused/Invalidated recovery into Cooling
   without weakening the declared priority rules.
3. Add regression tests for both recovery paths and cross-timeframe isolation.

### V41-AUDIT-B02 - AI failure/fallback paths do not always produce a complete, queryable AITrace

- `FINDING_ID: V41-AUDIT-B02`
- `BLOCKER_CLASS: REAL_DATA_INTEGRITY_BLOCKER`
- `BLOCKS_CURRENT_STAGE: YES`

Frozen contract:

- Every role call stores trace ID, analysis ID, input hash, model, output,
  token cost, and latency.
- AI failure or timeout follows the rule fallback and records that fallback.
- The complete GPT -> Gemini -> Grok -> Resolver -> Validation -> Final audit
  chain is queryable.

Current implementation:

- Missing configured role clients return `NOT_CONFIGURED` before
  `startDecisionChainCall` (`DecisionChainAiOrchestratorServiceImpl.java:52-56`).
- The corresponding test explicitly asserts that no call log is started
  (`DecisionChainAiOrchestratorServiceImplTest.java:70-85`).
- A call-log start failure also returns without a persisted fallback audit row
  (`DecisionChainAiOrchestratorServiceImpl.java:60-65`).
- Prompt-too-large, timeout, interruption, and generic exception paths return
  failed results without assigning elapsed latency
  (`AbstractSafeAiProviderClient.java:148-151,174-183`).
- `AiCallLogServiceImpl` stores `output_payload`, Candidate ID, and contract
  type (`AiCallLogServiceImpl.java:60-124`), and the mapper reads them
  (`AiCallLogMapper.java:16-32`), but `/api/ai/call-logs` omits all three from
  its response (`AiOrchestratorController.java:110-139`).

`DIRECT_PRODUCT_IMPACT:` The audit chain has invisible role attempts and
fallbacks, timeout/error rows can lack required latency evidence, and an
authorized auditor cannot query the persisted full role output needed to
reconstruct the decision chain.

`REPRODUCTION_EVIDENCE:` Invoke `GEMINI_REVIEW` with no Gemini client. The
result is `NOT_CONFIGURED`, but `startDecisionChainCall` is never invoked.
Query an otherwise completed call through `/api/ai/call-logs`; the stored
`outputPayload`, `candidateId`, and `contractType` are absent.

Required contract correction:

1. Persist a fallback trace for missing providers and log-start failures.
2. Record latency for every attempted provider path, including timeout/error.
3. Expose the complete authorized trace payload and chain identifiers through
   the trace query contract, subject to existing authentication/safety rules.
4. Add tests for each failure branch and complete trace retrieval.

### V41-AUDIT-B03 - New manual UserPosition creation does not require a validated FinalExecutionPlan link

- `FINDING_ID: V41-AUDIT-B03`
- `BLOCKER_CLASS: PRODUCT_SEMANTIC_BLOCKER`
- `BLOCKS_CURRENT_STAGE: YES`

Frozen contract:

- FinalExecutionPlan and UserPosition remain different objects.
- A UserPosition is created only by an explicit manual user action.
- The resulting UserPosition must associate with `final_plan_id`.

Current implementation:

- A supplied Final Plan ID is correctly checked for same symbol, Final status,
  and Rule Validation PASS (`UserPositionServiceImpl.java:250-264`).
- When the request omits `finalPlanId`, validation returns `null` and the
  position is inserted with no plan association
  (`UserPositionServiceImpl.java:63,82,250-254`).
- V11 keeps `tm_user_position.final_plan_id` nullable
  (`V11__fundamental_ai_v4_1_decision_chain.sql:255-261`). Nullable storage is
  reasonable for historical rows, but the new v4.1 write path does not enforce
  the mandatory association.
- The primary manual-open test creates a position without a Final Plan and
  expects success (`UserPositionServiceImplTest.java:53-83`).

`DIRECT_PRODUCT_IMPACT:` A newly entered real position can reach Position
Monitoring without a traceable validated Final plan, breaking the frozen
Plan -> explicit manual action -> UserPosition -> Monitoring -> Review chain.

`REPRODUCTION_EVIDENCE:` Submit a valid manual-open request without
`finalPlanId`; `validateFinalPlanReference` returns null and insertion proceeds.

Required contract correction:

1. Require `finalPlanId` on the authorized v4.1 manual-open path.
2. Preserve nullable historical storage only as a migration compatibility
   concern, not as permission for new unlinked writes.
3. Test missing, invalid, cross-symbol, non-Final, blocked, and valid Final Plan
   references.

### V41-AUDIT-B04 - Conflict level does not use the frozen Level 1-4 semantic contract

- `FINDING_ID: V41-AUDIT-B04`
- `BLOCKER_CLASS: PRODUCT_SEMANTIC_BLOCKER`
- `BLOCKS_CURRENT_STAGE: YES`

Frozen contract:

- Conflict Level is a four-level `1-4` result.
- Existing owned business concepts must be reused rather than duplicated.

Current implementation:

- The existing canonical enum is `LEVEL_1_CONSISTENT` through
  `LEVEL_4_EXTREME_DIVERGENCE` (`AiConflictLevelEnum.java:1-2`).
- The new v4.1 resolver derives and persists
  `NONE/MINOR/MAJOR/EXTREME`
  (`AiConflictResolverServiceImpl.java:150-175`).
- The new Gemini/Grok schema uses the same alternate names
  (`AiDecisionChainSchema.java:58-60`).
- V11 stores an unconstrained string for the resolver conflict level
  (`V11__fundamental_ai_v4_1_decision_chain.sql:161-185`).

`DIRECT_PRODUCT_IMPACT:` The new resolver has a second conflict-level semantic
model that does not match the frozen contract or the existing canonical
DecisionResult conflict model. API, dashboard, Review, and audit consumers
cannot rely on one stable Level 1-4 meaning.

`REPRODUCTION_EVIDENCE:` A low-score v4.1 resolution persists `NONE`, while the
owned business enum represents the corresponding level as
`LEVEL_1_CONSISTENT`; there is no persisted or API mapping contract between
them.

Required contract correction:

1. Reuse the canonical Level 1-4 conflict model or define one explicit,
   enforced mapping at the v4.1 boundary.
2. Constrain persistence and tests to the frozen four-level semantics.
3. Verify all four levels through Resolver, Final, Dashboard, Review, and trace
   projections.

## 4. Non-Blocking Findings

### V41-AUDIT-N01 - Existing Review rows are not enriched when updated

- `BLOCKER_CLASS: NON_BLOCKING_TECHNICAL_DEBT`
- `BLOCKS_CURRENT_STAGE: NO`

`applyDecisionChainTrace` runs when a new Review row is inserted, while update
paths only update review content (`ReviewServiceImpl.java:68-76,103-113`;
`ReviewResultMapper.java:50-60`). New Review rows are traceable, but an existing
legacy row updated after v4.1 remains without Final/Candidate/Trace provenance.
This does not invalidate new-chain execution, but it should be covered by a
backfill or update-path rule before claiming complete historical auditability.

### V41-AUDIT-N02 - Market catalog degradation is silent

- `BLOCKER_CLASS: NON_BLOCKING_TECHNICAL_DEBT`
- `BLOCKS_CURRENT_STAGE: NO`

When Binance exchange-info cannot be loaded, the full-market catalog silently
falls back to configured symbol mappings (`BinanceMarketAssetCatalog.java:70-86,126-138`).
The fallback contains real configured symbols and does not fabricate market
data, but API consumers cannot distinguish full-market search from a reduced
catalog. A future readiness/degradation indicator would make the capability
observable without changing the frozen business flow.

### V41-AUDIT-N03 - Machine-readable authorization state is inconsistent with the audited base

- `BLOCKER_CLASS: GOVERNANCE_STATE_DISCREPANCY`
- `BLOCKS_CURRENT_STAGE: NO` for this read-only audit; reconcile before merge gate.

The PR base is merge commit `fb2722c7` containing the v4.1 authorization, and
Product Source Gate plus Workflow Contract pass. However,
`scripts/v1-state.sh --request-package FUNDAMENTAL_AI_V4_1_DECISION_CHAIN_IMPLEMENTATION`
reports `V4_1_DECISION_CHAIN_AUTHORIZATION_STATUS=PENDING_MERGED_MAIN` and
`CURRENT_PACKAGE_ACTION_ALLOWED=NO`. This report does not modify governance
state; the merge gate should reconcile the stale machine-readable projection.

## 5. Schema and API Audit

### Schema

PASS evidence:

- V11 reuses `tm_asset_state` for Opportunity state ownership.
- Candidate and Resolver are separately persisted.
- Existing `tm_execution_plan` is extended as Final rather than duplicated.
- Final rows require Candidate, Opportunity, Resolver, trace, finalized time,
  validation PASS, and a valid chain status
  (`V11__fundamental_ai_v4_1_decision_chain.sql:209-240`).
- AI role authority and no-trading boundaries are DB constrained
  (`V11__fundamental_ai_v4_1_decision_chain.sql:272-309`).
- PostgreSQL 16 CI migration smoke passes with historical-state normalization
  and V11 constraints.

FAIL evidence:

- `final_plan_id` is not enforced on new UserPosition writes.
- Resolver conflict level is unconstrained and does not use Level 1-4.
- State ownership has no timeframe dimension despite timeframe-specific
  debounce requirements.

### API

PASS evidence:

- Asset Pool list/search/add/remove/restore/scan endpoints exist.
- No Candidate endpoint exposes an unvalidated Candidate as a Final plan.
- Supplied Final Plan references are validated against same-symbol Final rows
  with Rule Validation PASS.
- AI call logs can be queried by analysis ID and trace ID.

FAIL evidence:

- AI trace query omits persisted full output and Candidate/contract identity.
- The manual UserPosition write contract permits an absent Final Plan link.

`SCHEMA_API_STATUS: FAIL`

## 6. Test Audit

### Executed evidence

Local full Maven validation on the exact PR head:

- command: `./mvnw test -q`
- suites: `398`
- tests: `4382`
- passed: `4368`
- failures: `0`
- errors: `0`
- skipped: `14`
- exit code: `0`

Local PostgreSQL Testcontainers execution was skipped because no Docker socket
was available. PR CI independently ran PostgreSQL 16 Flyway V11 smoke:

- tests: `1`
- failures: `0`
- errors: `0`
- skipped: `0`

PR CI evidence:

- `quality-gate: PASS`
- `workflow-contract: PASS`
- Maven CI: `770 tests, 0 failures, 0 errors, 0 skipped`
- Product Source Gate: `PASS`

### Missing or incorrect contract coverage

1. No test proves `CONFUSED -> COOLING` through the canonical transition entry.
2. No test proves `INVALIDATED -> COOLING` through the canonical transition
   entry.
3. No test proves debounce independence for the same symbol on different
   timeframes.
4. The missing-provider test asserts that no AI trace is created.
5. No test requires elapsed latency on timeout/error role calls.
6. No API test proves full AI role output can be queried from the trace.
7. The manual-open test accepts a new UserPosition without `finalPlanId`.
8. No test enforces one canonical Level 1-4 mapping across v4.1 Resolver,
   DecisionResult, Dashboard, Review, and trace output.

`TEST_STATUS: FAIL` for required contract coverage, despite green executed
tests.

## 7. Merge Recommendation

`REQUEST_CHANGES`

Rationale:

- The build is stable, Candidate/Final separation is real, the Asset Pool gate
  is real, and the automatic-trading safety boundary is intact.
- The four blocking findings violate frozen behavior or auditability contracts
  and can affect real Opportunity recovery, decision trace reconstruction,
  Position provenance, and conflict semantics.
- Green CI cannot substitute for the missing contract behavior because some
  tests currently assert the non-compliant behavior.

PR `#1177` must not enter Merge Gate until all four blockers are corrected and
the capability audit is rerun against the corrected head.

## 8. Current Phase Status

- `BACKEND_CAPABILITY_AUDIT: COMPLETE`
- `BACKEND_CAPABILITY_STATUS: BLOCKED_REQUEST_CHANGES`
- `PR_1177_MERGE_GATE: BLOCKED`
- `CURRENT_PHASE_DONE: NO`
- `NEXT_ALLOWED_ACTION: Remediate only the confirmed audit blockers, rerun tests, then rerun this audit.`
- `NEXT_BLOCKED_ACTION: Merge PR #1177; start any successor product package; modify Figma or Mobile; add automatic trading.`
