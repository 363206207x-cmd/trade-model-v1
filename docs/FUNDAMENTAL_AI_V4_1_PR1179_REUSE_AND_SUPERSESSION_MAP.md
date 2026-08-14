# Fundamental AI v4.1 PR #1179 Reuse and Supersession Map

Status: `FROZEN_IMPLEMENTATION_HANDOFF`

PR: `#1179`

Audited Head: `198fc0ff545240a1b89dbbbfb1a3e642648d4f45`

Audited Base: `edc3615c03c9b71763c32574f1d811c1d9a8954d`

PR state at reconciliation: `OPEN / DRAFT / UNMERGED`

Registered disposition:
`REUSABLE_IMPLEMENTATION_BASE_PENDING_AUTHORIZATION_AND_REBASE`

This map preserves validated work in PR #1179 while aligning its remaining
scope with the unified v4.1 Product Source. It does not modify, merge or close
the PR, and it does not treat audit fixtures as runtime evidence.

## 1. Protected Passing Capabilities

The implementation package must preserve these already-audited capabilities:

- Final-only rendering;
- all five Plan Mode renderings;
- Candidate / Final UI isolation;
- one Three-AI workspace with role tabs;
- structured Three-AI semantics and role/collection states;
- explicit manual UserPosition lifecycle;
- Position Monitoring VERIFIED/FRESH trust gate;
- fail-closed rendering and no fake data;
- automatic trading capability count `0`;
- latest approved Desktop visual components.

## 2. Production File Mapping

| File / Group | Disposition | Protected Asset | Required Final-Spec Work |
|---|---|---|---|
| `src/main/java/org/example/trademodel/service/impl/DashboardHomeServiceImpl.java` | REUSE_WITH_EXTENSION | dynamic projection assembly, Final/position/AI separation | true persisted dynamic Top6 scenarios, multi-timeframe one-slot aggregation, selected-context exit data, lifecycle/message/task fields; no semantic fallback |
| `src/main/java/org/example/trademodel/vo/DashboardHomeVO.java` | REUSE_WITH_EXTENSION | frontend contract fields already used by approved Desktop | add only canonical projection fields and exact states required by the new source |
| `src/main/resources/templates/dashboard.html` | REUSE_WITH_EXTENSION | approved Desktop shell, Focus Assets, Final-only plan, Position Monitoring, single AI workspace | selected URL context, lifecycle/revalidation, async and fail-closed states; link all final routes without redesigning frozen business |
| `src/main/resources/static/css/dashboard-latest.css` | REUSE_WITH_EXTENSION | latest Desktop visual system | cover approved new routed/overlay states; no Mobile CSS |
| `src/main/resources/static/js/frontend-contract.js` | REUSE_WITH_EXTENSION | field isolation, enum labels, fail-closed helpers | add analysis mode, lifecycle, task, message and context mappings without fallback |
| `src/main/resources/templates/analysis-detail.html` | REPLACE_BY_FINAL_SPEC | usable analysis layout and single AI workspace | become R08 dual-mode surface; remove Candidate/Final semantics from Preview |
| `src/main/resources/static/js/analysis-detail.js` | REPLACE_BY_FINAL_SPEC | structured role rendering and analysis binding | enforce `ANALYSIS_PREVIEW` versus `OPPORTUNITY_DECISION`; preserve empty arrays/states |
| `src/main/resources/static/css/analysis-detail.css` | REUSE_WITH_EXTENSION | approved analysis visual foundation | support mode banner, state matrix and routed responsive Desktop behavior |
| `src/main/resources/templates/login.html` | REUSE_AS_IS | approved login shell | extend only if session-expired/deep-link recovery is not yet represented |

## 3. Test and Fixture Mapping

| File / Group | Disposition | Use |
|---|---|---|
| `scripts/dashboard-visual-acceptance-fixture.py` | TEST_ONLY | extend deterministic acceptance coverage to the 81 Desktop frames; never use fixture output as runtime proof |
| `AnalysisDetailFrontendContractTest.java` | TEST_ONLY | preserve role semantics; add strict Preview prohibition and route states |
| `DashboardControllerTest.java`, `DashboardLocalRealBindingContractTest.java`, `Fe04ShellHomeDashboardContractTest.java`, `FrontendImplementationFoundationContractTest.java` | TEST_ONLY | preserve shell/real-binding contracts; extend selected context and route wiring |
| `FundamentalAiV41ExecutionPlanSemanticAlignmentContractTest.java` | TEST_ONLY | protect Final-only and five modes; add lifecycle/revalidation assertions |
| `FundamentalAiV41FrontendRuntimeAlignmentContractTest.java` | TEST_ONLY | extend for complete route/data-state runtime scenarios |
| `FundamentalAiV41ProductizedDesktopUiContractTest.java` | TEST_ONLY | protect approved Desktop expression; extend all frozen page states |
| `PositionMonitoringFrontendContractTest.java`, `DashboardHomeServiceImplTest.java` | TEST_ONLY | preserve P2 trust gate, position independence and Home projection lineage |
| `StaticNoTradeInstructionGuardTest.java` | TEST_ONLY | keep automatic trading capability at zero |

## 4. Documentation and Evidence Mapping

| File / Group | Disposition | Rule |
|---|---|---|
| `docs/CODEX_NEXT_TASK.yml`, `docs/DELIVERY_PROGRESS_MATRIX.md`, `docs/PROJECT_CURRENT_STATE.md` in PR #1179 | REPLACE_BY_FINAL_SPEC | rebase must take the newer merged authorization state; PR-local stale authorization must not win conflicts |
| `docs/FUNDAMENTAL_AI_V4_1_*` reports in PR #1179 | EVIDENCE_ONLY | retain as implementation/audit history; none can override the unified Product Source |
| `docs/evidence/v4_1_execution_plan_semantics/**` | EVIDENCE_ONLY | preserve Final/Plan Mode evidence and comparison images |
| `docs/evidence/v4_1_frontend_runtime_alignment/**` | EVIDENCE_ONLY | preserve controlled states while clearly distinguishing fixture/runtime provenance |
| `docs/evidence/v4_1_latest_ui/**` | EVIDENCE_ONLY | preserve approved Figma-to-runtime visual evidence; Figma remains unchanged |
| `docs/evidence/v4_1_productized_ui/**` | EVIDENCE_ONLY | preserve visual history and rerun against current main after implementation |
| `scripts/v1-state.sh` in PR #1179 | REPLACE_BY_FINAL_SPEC | rebase onto the merged exact-package resolver; do not restore stale package names |

## 5. Required Extension / Replacement Work

| Final-Spec Capability | Current PR Status | Disposition |
|---|---|---|
| AI Analysis dual modes and strict on-demand Preview | incomplete / prior audit blocker | REPLACE_BY_FINAL_SPEC while reusing structured AI renderer |
| Asset Pool complete interaction, top-up/reset and removal effects | partial | REUSE_WITH_EXTENSION |
| real dynamic Top6 from persisted Pool-wide opportunities | not fully runtime-verified | REUSE_WITH_EXTENSION |
| multi-timeframe one-slot aggregation and conflict state | missing | REUSE_WITH_EXTENSION |
| Message Center single owner and exact filters | missing | REPLACE_BY_FINAL_SPEC |
| Telegram binding/delivery/filtering | absent or NoOp/review-only | REPLACE_BY_FINAL_SPEC |
| Push Recheck routed page | partial backend assets only | REUSE_WITH_EXTENSION |
| Plan Revalidation and plan lifecycle/version | missing | REUSE_WITH_EXTENSION around Final owner plus one independent revalidation record |
| scoped Hot Reset / Confused recovery UI | partial | REUSE_WITH_EXTENSION |
| Review / Missed Opportunity reason-outcome split | incomplete | REUSE_WITH_EXTENSION |
| My / Settings | missing | REPLACE_BY_FINAL_SPEC using UserConfig/provider owners |
| Event Calendar | summary only | REUSE_WITH_EXTENSION |
| Full Audit Chain route | missing | REUSE_WITH_EXTENSION through aggregate query, no new audit store |
| cross-page selected asset context | missing | REUSE_WITH_EXTENSION |
| 14 routes, 11 overlays and 81 Desktop states | incomplete | REPLACE_BY_FINAL_SPEC at page-composition level, not a rewrite of passing components |

## 6. Legacy Paths to Remove

`REMOVE_LEGACY_PATH` applies only after replacement tests pass:

- Preview paths that expose Candidate, Candidate Plan Mode, Final, entry, stop
  or target semantics;
- fixed/default-symbol Top6 or Pool-first-six backfill;
- old Three-AI multi-card or voting renderers;
- cross-field semantic fallback and fake placeholder success values;
- generic recheck logic that merges Push Recheck with Plan Revalidation;
- Telegram paths that establish a second message truth;
- stale machine-state/package declarations superseded by the authorization PR.

No code deletion is authorized without dead-code evidence.

## 7. Rebase and Immutability Contract

The authorization task leaves PR #1179 and its audited Head unchanged. After
the authorization PR is merged, the implementation package may synchronize or
rebase branch `codex/v4-1-frontend-runtime-alignment` onto latest `main`, then
continue in that branch. The rebase may change its future Head, but it must
preserve the protected passing capabilities and record the pre-rebase audited
Head above.
