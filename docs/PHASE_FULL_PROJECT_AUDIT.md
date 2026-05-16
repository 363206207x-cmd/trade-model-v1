# PHASE_FULL_PROJECT_AUDIT

## 1. Audit Scope

This document records the current full-project audit for Trade Model V1.

Audit baseline:

- HEAD: `c6c3c9e test(plan): full verification of SourceTrace, DerivativesRiskContext, BoundaryCandidateService, ExecutionPlan readiness and RiskActionGuard fallback`
- P0-P10 document chain: committed baseline is present.
- SourceTrace / DerivativesRiskContext minimal contract: implemented.
- SourceAssembler minimal production chain: implemented.
- BoundaryCandidateService VALID output constraints: implemented.
- ExecutionPlan readiness + SourceTrace fallback: implemented.
- RiskActionGuard fallback integration: implemented.

Audit boundaries:

- No external API integration.
- No Coinglass integration.
- No executable ExecutionPlan.
- No automatic trading.
- No order API.
- BoundaryCandidate `VALID` remains a review candidate only.
- ExecutionPlan remains advisory / review-only.
- Funding, OI, liquidation, leverage, and long-short ratio do not directly produce trading actions.

## 2. Repository State

Pre-commit workspace state observed for this audit:

| Item | State |
|---|---|
| HEAD | `c6c3c9e` |
| staged | empty |
| `src/main/java` | clean before audit document creation |
| `src/test/java` | clean before audit document creation |
| `src/main/resources` | clean before audit document creation |
| modified docs | none except this audit document being prepared |
| untracked docs | `docs/PHASE_FULL_PROJECT_AUDIT.md` before this audit commit |
| P0-P10 docs | tracked and committed |

After this document is committed, no source, test, resource, config, schema, or dashboard change should be included.

## 3. Module Status Table

| Module | Current Actual Status | Derivatives Risk Completeness | Fallback / Safety Label | Deviation / Risk |
|---|---|---|---|---|
| BoundaryCandidateDTO / Entry / Stop / TP / SourceFields | DTOs exist. `BoundaryCandidateDTO.valid(...)` exists. Entry / Stop / TP can carry numeric source fields. | Structurally complete, source-dependent | `CLOSED_BASIC` | DTO can express a valid review candidate, but does not itself prove production source truth. |
| BoundaryStatusEnum / DTO tests | `VALID`, `INCOMPLETE`, `WATCH_ONLY`, and related DTO tests exist. | Not derivatives-dependent | `CLOSED` | Covers structural and safety defaults, not external derivatives evidence. |
| RuntimeKlineContextDTO | DTO exists with entry / stop / TP / RR / liquidity / multi-timeframe / event / wick source fields and fallback status. | Basic contract complete, real source feed pending | `CLOSED_BASIC_DERIVATIVES_PENDING` | Runtime source object can carry required data, but real data production remains limited by upstream availability. |
| DerivativesRiskContextDTO | DTO exists for OI history, Funding history, liquidation cluster, leverage distribution, long-short ratio, liquidity stress, event blockers, wick confirmations, missing fields, and fallback. | Basic contract complete, external derivatives data pending | `CLOSED_BASIC_DERIVATIVES_PENDING` | No Coinglass or full external derivatives API is connected. |
| SourceTraceDTO / SourceCompletenessContract / fallback enum | DTO and completeness contract exist. `hasRequiredBoundarySources()` gates required boundary sources. | Basic contract complete | `CLOSED_BASIC_DERIVATIVES_PENDING` | Completeness is structural. It depends on trustworthy upstream source population. |
| SourceAssembler / DefaultSourceAssembler | Minimal production chain assembles SourceTrace from RuntimeKlineContext + DerivativesRiskContext. Missing source maps to `INCOMPLETE`, `WATCH_ONLY`, or `SAFE_FAIL_CLOSED_ONLY`. | Basic production chain complete, external inputs pending | `CLOSED_BASIC_DERIVATIVES_PENDING` | Does not fetch external sources and does not create trading actions. |
| BoundaryCandidateService / VALID integration | Service blocks VALID when SourceTrace or boundary numeric sources are missing. Complete source + safe RiskActionGuard can call `BoundaryCandidateDTO.valid(...)`. | Basic gated integration complete | `CLOSED_BASIC_DERIVATIVES_PENDING` | VALID remains review-only. Missing source or RiskActionGuard fail-closed falls back. |
| BoundaryCandidateService tests | Cover missing source, watch-only source, safe-fail source, complete source, RiskActionGuard stampede / liquidity / wick / action flag fallback, and review-only VALID. | Strong current coverage | `CLOSED` for safety behavior | Still does not verify external derivatives API ingestion because none is connected. |
| PlanBoundary SourceTrace Adapter | Fail-closed display adapter exists and reports missing / backend-pending source trace. | Display-level only | `SAFE_FAIL_CLOSED_ONLY` | Safe for display; not a full production source trace generator. |
| PlanBoundary Display Adapter | Display remains fail-closed and manual-review. It prevents display-only state from implying execution. | Display-level only | `SAFE_FAIL_CLOSED_ONLY` | Display layer must not be treated as PlanBoundary production logic. |
| ExecutionPlan Display Adapter | SourceTrace and RiskActionGuard are checked before READY_REVIEW_ONLY. Missing source, high risk, liquidity missing, stampede, wick-only, or guard blocks fallback. | Basic readiness display complete | `SAFE_FAIL_CLOSED_ONLY` / review-only | READY_REVIEW_ONLY is not executable. |
| PlanService / ExecutionPlanVO | Advisory plan includes SourceTrace readiness and RiskActionGuard readiness. Source missing or guard blocked downgrades to incomplete/watch-only. | Basic readiness integration complete | `CLOSED_BASIC_DERIVATIVES_PENDING` / review-only | Plan remains advisory. No order execution is produced. |
| RuleEngineService / PlanReadiness | Default methods accept SourceTrace and RiskActionGuard, force advisory mode and `canExecute=false`. Missing source and fail-closed guard block execution. | Basic fail-closed gate complete | `SAFE_FAIL_CLOSED_ONLY` | Rule engine cannot be claimed production-complete without richer verified risk rules. |
| RiskActionGuard Display | Fail-closed display exists. Action flags remain false in safe paths. Liquidity, stampede, wick, and blocking reasons constrain readiness. | Basic display guard complete, risk sources pending | `SAFE_FAIL_CLOSED_ONLY` | Real liquidation / leverage / liquidity stress sources remain absent. |
| MarketEnvironment | Binance / existing market environment heuristics exist. | Partial | `CLOSED_BASIC_DERIVATIVES_PENDING` | No Coinglass, OI history, full Funding history, liquidation cluster, leverage distribution, or long-short ratio. |
| ScoreService | Basic scoring exists, including limited market evidence. | Partial | `CLOSED_BASIC_DERIVATIVES_PENDING` | Funding/OI-like evidence remains weighting only and must not imply direct action. |
| EvidenceService | Evidence capture exists for available signals. | Partial | `CLOSED_BASIC_DERIVATIVES_PENDING` | Evidence is explanatory and not full SourceTrace proof. |
| DecisionEngine | Decision output exists, but production risk source closure is not complete. | Partial | `WATCH_ONLY` when source confidence is insufficient | `isWorthOpening` or similar decision text must not become execution permission. |
| Push / Recheck / Watchlist | Existing status contracts and services remain. | Indirectly affected | `INCOMPLETE_RISK_LOGIC` / review-only | `VALID_EXECUTABLE` naming remains a semantic risk unless bounded as non-order execution. |
| Position Monitor / Review | Position and review basics exist. | Indirectly affected | `CLOSED_BASIC` | Position liquidation price is not a market liquidation cluster. |
| schema / config / dashboard | Existing schema/config/dashboard remain unchanged. | Partial | dashboard: `SAFE_FAIL_CLOSED_ONLY` | No full derivatives risk schema/API/dashboard production integration in this audit. |
| P0-P10 document chain | P0-P10 planning and verification docs are tracked and committed. | Documentation baseline closed | `DOC_BASELINE_CLOSED` | Current code advanced beyond P10 planning into minimal source/risk gate implementation. |

## 4. Directly Affected By Missing Derivatives Data

| Missing Data | Directly Affected Modules | Required Fallback / Behavior |
|---|---|---|
| Coinglass or equivalent derivatives feed | DerivativesRiskContext, SourceAssembler, SourceTrace, RiskActionGuard | Keep external confirmation absent. Do not infer production risk completeness. |
| OI history | DerivativesRiskContext, MarketEnvironment, ScoreService, DecisionEngine | `CLOSED_BASIC_DERIVATIVES_PENDING`; do not produce directional trade action. |
| Funding history | DerivativesRiskContext, ScoreService, EvidenceService | `WATCH_ONLY` if required for confidence; Funding does not mean long/short. |
| liquidation cluster | RiskActionGuard, ExecutionPlan readiness, Evidence | `SAFE_FAIL_CLOSED_ONLY`; no automatic close, stop, reverse, or forced exit. |
| leverage distribution | RiskActionGuard, ScoreService, PlanReadiness | `SAFE_FAIL_CLOSED_ONLY`; no leverage/action permission. |
| long-short ratio | Market crowding, DecisionEngine, Watchlist | `WATCH_ONLY`; no crowding-based execution. |
| liquidity stress | SourceAssembler, SourceTrace, RiskActionGuard, ExecutionPlan readiness | `SAFE_FAIL_CLOSED_ONLY` or `WATCH_ONLY` depending on layer; never executable. |
| event window blocker | BoundaryCandidateService, ExecutionPlan readiness, Push/Recheck/Watchlist | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY`; block opportunity push when uncertain. |
| wick confirmation source | BoundaryCandidateService, SourceTrace, RiskActionGuard | `WATCH_ONLY` / `SAFE_FAIL_CLOSED_ONLY`; wick-only is not trend reversal. |

## 5. Indirectly Affected Modules

| Module | Impact Level | Limitation |
|---|---|---|
| Dashboard | Medium | Displays fail-closed/review-only state but does not prove source truth. |
| PlanService | Medium | Produces advisory plan only. SourceTrace and RiskActionGuard can downgrade readiness. |
| Review / Monitor | Low to medium | Can record review and position state, but cannot fill missing derivatives market evidence. |
| schema / config | Medium | Current persistence/config is not a complete derivatives risk source contract. |
| Push / Watchlist | High | Missing event, liquidity, and wick source must block opportunity semantics. |
| Decision output consumers | High | Decision text or `worthOpening` fields must not bypass SourceTrace and RiskActionGuard gates. |

## 6. Basically Unaffected Modules

- DTO getter/setter field carriers.
- `BoundaryCandidateDTO.valid(...)` structural factory.
- `manualReviewRequired = true` safety default.
- `notTradeInstruction = true` safety default.
- Boundary DTO unit tests.
- SourceTrace DTO completeness checks.
- Display fail-closed mappings.
- ExecutionPlan review-only / advisory output shape.
- RiskActionGuard action flags staying blocked in fail-closed paths.

## 7. BoundaryCandidate VALID vs ExecutionPlan

| Item | Meaning |
|---|---|
| BoundaryCandidate `VALID` | Candidate boundary has complete structure and traceable source fields sufficient for manual review. |
| BoundaryCandidate `VALID` does not mean | Open position, close position, stop loss execution, reverse, order API, or automatic trading. |
| ExecutionPlan READY_REVIEW_ONLY | Advisory/review state after SourceTrace and RiskActionGuard checks pass. |
| ExecutionPlan READY_REVIEW_ONLY does not mean | Executable order, production execution, automatic entry, automatic exit, or automatic reverse. |
| Executable future requirement | Verified SourceTrace, DerivativesRiskContext, RuntimeKlineContext, PlanReadiness, RuleEngine risk closure, RiskActionGuard, and manual review gate. |

## 8. High-Risk Deviations To Keep Guarded

- BoundaryCandidate `VALID` must not be inferred from missing source data.
- ExecutionPlan must not become executable from display-only, DTO-only, or missing source state.
- Push/Recheck `VALID_EXECUTABLE` / `valid=true` naming can conflict with Risk Action Guard unless explicitly documented as non-order execution.
- Funding / OI / liquidation / leverage / long-short ratio must not directly produce open, close, stop, or reverse actions.
- High risk does not mean direct stop loss.
- High risk does not mean direct reverse.
- Wick/spike does not mean trend reversal.
- Stampede state blocks new position, reverse, and opportunity push.
- Liquidity worsening should fail closed or require manual review, not force market exit.

## 9. Document Baseline Status

| Document Phase | File | Current State | Label |
|---|---|---|---|
| P0 | `docs/PHASE_V1_MODULE_CLOSURE_REBASE.md` | committed | `DOC_BASELINE_CLOSED` |
| P1 | `docs/PHASE_DERIVATIVES_RISK_CONTEXT_PLAN.md` | committed | `DOC_BASELINE_CLOSED` |
| P2 | `docs/PHASE_SOURCE_ASSEMBLER_PLAN.md` | committed | `DOC_BASELINE_CLOSED` |
| P3 | `docs/PHASE_P3_SOURCE_ASSEMBLER_VERIFICATION_PLAN.md` | committed | `DOC_BASELINE_CLOSED` |
| P4 | `docs/PHASE_P4_SOURCE_ASSEMBLER_EXECUTION_VERIFICATION.md` | committed | `DOC_BASELINE_CLOSED` |
| P5 | `docs/PHASE_P5_SOURCE_ASSEMBLER_SMALL_SCALE_VERIFICATION.md` | committed | `DOC_BASELINE_CLOSED` |
| P6 | `docs/PHASE_P6_SOURCE_ASSEMBLER_SMALL_SCALE_TEST_PLAN.md` | committed | `DOC_BASELINE_CLOSED` |
| P7 | `docs/PHASE_P7_SOURCE_ASSEMBLER_SMALL_SCALE_DATA_VERIFICATION.md` | committed | `DOC_BASELINE_CLOSED` |
| P8 | `docs/PHASE_P8_SOURCE_ASSEMBLER_SMALL_SCALE_DATA_ADJUSTMENT.md` | committed | `DOC_BASELINE_CLOSED` |
| P9 | `docs/PHASE_P9_SOURCE_ASSEMBLER_SMALL_SCALE_VERIFICATION.md` | committed | `DOC_BASELINE_CLOSED` |
| P10 | `docs/PHASE_P10_SOURCE_ASSEMBLER_SMALL_SCALE_PRODUCTION_VERIFICATION.md` | committed | `DOC_BASELINE_CLOSED` |
| Full project audit | `docs/PHASE_FULL_PROJECT_AUDIT.md` | prepared by this task | `DOC_BASELINE_TO_COMMIT` |

## 10. Closure Summary

| Category | Modules |
|---|---|
| Fully closed | BoundaryStatusEnum, DTO basics, DTO safety defaults, SourceTrace DTO completeness contract, fail-closed display basics, related unit tests. |
| Basic closed, derivatives pending | RuntimeKlineContextDTO, DerivativesRiskContextDTO, SourceTraceDTO, SourceAssembler, BoundaryCandidateService, PlanService, RuleEngine default readiness, MarketEnvironment, ScoreService, EvidenceService, DecisionEngine. |
| Safe display only | PlanBoundary display, PlanBoundary SourceTrace display adapter, ExecutionPlan display, RiskActionGuard display. |
| Logic still limited by missing data | External derivatives evidence, full Coinglass/OI/Funding/liquidation/leverage/long-short/liquidity source feed, final PlanReadiness production gate. |
| Semantic risk to manage | Push/Recheck `VALID_EXECUTABLE` naming and any `valid=true` wording near user-facing push/recheck flows. |
| Current document baseline | P0-P10 closed; full audit is being closed by this task. |

## 11. P0-P6 Repair / Next Strategy

| Phase | Minimal Action | Boundary |
|---|---|---|
| P0 | Commit this full project audit as the current global baseline. | Docs only. Do not change `src`. |
| P1 | Revisit Push/Recheck naming and document whether `VALID_EXECUTABLE` is internal-only or should be renamed. | No order API. No automatic trading. |
| P2 | Add or refine tests that prove Push/Recheck/Watchlist cannot bypass SourceTrace and RiskActionGuard. | Test-only unless naming correction is explicitly approved. |
| P3 | Confirm SourceTrace / DerivativesRiskContext missing-field propagation across dashboard and service consumers. | No external API. |
| P4 | Define production-readiness gate for external derivatives data ingestion. | Design first; no Coinglass connection yet. |
| P5 | Only after fallback tests pass, design adapter boundary for Coinglass or other derivatives providers. | External API remains deferred. |
| P6 | If external provider is later added, require fail-closed behavior for provider outage, stale data, and partial data. | No execution permission from provider data alone. |

## 12. Verification Already Performed In Current Chain

Current HEAD includes a full verification commit for the implemented chain:

- `./mvnw -q -DskipTests compile` PASS
- `./mvnw -q -DskipTests test-compile` PASS
- targeted SourceTrace / BoundaryCandidateService / ExecutionPlan readiness / RiskActionGuard tests PASS
- Push/Recheck/RiskActionGuard adjacent tests PASS
- `./mvnw -q test` PASS

This audit document does not require another source compilation because it is documentation-only.

## 13. Audit Conclusion

Trade Model V1 is currently in a safer and more coherent intermediate state than the P10 planning baseline:

- P0-P10 documents are committed.
- Minimal SourceTrace / DerivativesRiskContext DTO contracts exist.
- Minimal SourceAssembler production chain exists.
- BoundaryCandidateService can output VALID only after source and risk guard checks pass.
- ExecutionPlan readiness remains advisory / review-only.
- RuleEngine default SourceTrace / RiskActionGuard paths fail closed.
- RiskActionGuard fallback is integrated into BoundaryCandidateService, PlanService, ExecutionPlan display, and RuleEngine default checks.

Remaining boundary:

- The system still does not have full external derivatives evidence.
- Coinglass and equivalent derivatives providers remain disconnected.
- Funding, OI, liquidation, leverage, liquidity, event, and wick signals remain review evidence and gating context, not trade instructions.
- No module may release executable ExecutionPlan or automatic trading from current state.

Final stage conclusion:

- Current project state is suitable for a formal audit baseline commit.
- Next safe action is to review Push/Recheck naming and semantics before any external derivatives data integration.
