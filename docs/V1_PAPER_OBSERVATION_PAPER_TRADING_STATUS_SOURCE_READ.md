# V1 Paper Observation / Paper Trading Status Source Read

## 1. Current Merged Main

- Current merged main: `df05213 docs(runtime): select paper observation status next slice`
- Current module: `Paper Observation / Paper Trading Status review-only status`
- Current phase: `Source Read`
- Risk level: `A`
- Capability movement: none; the project remains `REVIEW_ONLY_RUNTIME partial`.
- Completed review-only runtime partial slices: 13.

This package only reads existing source paths and records the next design handoff. It does not implement a new endpoint, panel, order path, paper order, paper PnL, simulated execution, or Position Monitor behavior.

## 2. Source Read Files

| Area | Files read | Findings |
|---|---|---|
| Display adapter interface | `src/main/java/org/example/trademodel/service/dashboard/PaperObservationDisplayAdapter.java` | Existing read-only adapter contract. The class comment explicitly says it must not create real positions or trading instructions. |
| Default adapter | `src/main/java/org/example/trademodel/service/dashboard/DefaultPaperObservationDisplayAdapter.java` | Existing fail-closed display adapter. It reads upstream display state and forces paper observation safety flags. |
| Dashboard detail VO | `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java` | Existing nested `PaperObservationDisplayVO` carries status, counts, review summary, `notRealPosition`, `notTradeInstruction`, `manualReviewRequired`, and backend status. |
| Dashboard detail owner path | `src/main/java/org/example/trademodel/controller/DashboardController.java` | `/api/dashboard/detail` builds safe defaults, reads DecisionResult, SourceTrace, PlanBoundary, ExecutionPlan, RiskActionGuard, and then invokes `paperObservationDisplayAdapter.build(...)`. |
| Dashboard template | `src/main/resources/templates/dashboard.html` | Existing dashboard reads `detail.paperObservationDisplay`, renders Paper Observation status inside the display status / risk guard / workbench surfaces, and keeps non-trading safety copy. |
| Adapter tests | `src/test/java/org/example/trademodel/service/dashboard/DefaultPaperObservationDisplayAdapterTest.java` | Existing tests cover missing inputs, fallback safety flags, PlanBoundary block, ExecutionPlan block, RiskActionGuard block, manual review status, and missed-opportunity flag preservation. |
| VO tests | `src/test/java/org/example/trademodel/vo/DashboardDetailResponseVOTest.java` | Existing tests confirm safe default displays include `paperObservationDisplay`. |
| Controller tests | `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java` | Existing controller test setup wires `PaperObservationDisplayAdapter` in the dashboard detail owner path. |
| Historical docs | `docs/PHASE_PAPER_OBSERVATION_DISPLAY_ADAPTER_PLAN.md`, `docs/PHASE_PAPER_OBSERVATION_DISPLAY_API_SMOKE_RESULT.md`, `docs/PHASE_DASHBOARD_DISPLAY_ADAPTER_CHAIN_CLOSURE.md`, `docs/PHASE_DASHBOARD_DISPLAY_STATUS_LOCAL_SMOKE_RESULT.md`, `docs/PHASE_DASHBOARD_DISPLAY_STATUS_API_CURL_RESULT.md` | Historical records show the adapter plan, API smoke, fail-closed output, and display-chain closure for PaperObservation display. |

## 3. Reusable Assets

| Reusable asset | Current status | Reuse for future status slice |
|---|---|---|
| `PaperObservationDisplayAdapter` | Existing interface | Use as canonical display owner path, not a new service family. |
| `DefaultPaperObservationDisplayAdapter` | Existing component | Reuse fail-closed rules and upstream display dependencies. |
| `DashboardDetailResponseVO.PaperObservationDisplayVO` | Existing nested VO | Reuse as current read model; no new DTO by default. |
| `/api/dashboard/detail` | Existing dashboard detail endpoint | Existing owner path already exposes `paperObservationDisplay`; design must decide whether this is enough or whether a thin read-only status endpoint is justified. |
| `dashboard.html` paper observation rendering | Existing display surface | Can be used as evidence of current dashboard read path, but no dedicated runtime status panel exists yet. |
| `DefaultPaperObservationDisplayAdapterTest` | Existing targeted tests | Reuse as baseline for fail-closed and safety flags. |
| Historical API smoke docs | Existing verification records | Confirm API response includes `paperObservationDisplay` with fail-closed flags. |

## 4. Existing Safety Semantics

The current owner path already contains these review-only safety semantics:

- `paperObservationAvailable=false`
- `manualReviewEntryAvailable=false`
- `notRealPosition=true`
- `notTradeInstruction=true`
- `manualReviewRequired=true`
- `backendConnectionStatus=BACKEND_PENDING` when not wired
- `reviewSummary=DECISION_MISSING`, `PLAN_BOUNDARY_NOT_VALID`, `EXECUTION_PLAN_NOT_READY`, or `RISK_ACTION_GUARD_BLOCKED`

The default adapter preserves `missedOpportunityFlag` as read-only context, but still keeps the entry unavailable and fail-closed. It never creates a real position, trade instruction, paper order, paper PnL, or simulated execution.

## 5. Current Gaps

| Gap | Impact | Design note |
|---|---|---|
| Dedicated runtime status endpoint is missing | `/api/dashboard/detail` exposes the display object, but there is no compact review-only status endpoint for Paper Observation / Paper Trading status. | Design must decide whether to reuse dashboard detail only or allow one thin read-only Map endpoint. |
| Dedicated dashboard runtime status panel is missing | Existing dashboard surfaces show Paper Observation status inside display cards and workbench shells, not as a completed runtime status panel. | Design can consider a minimal panel / DOM only if implementation readiness later approves it. |
| Paper observation is not backed by a paper trading execution model | Good for safety, but status must not pretend paper orders or paper PnL exist. | Keep paper trading status wording as observation/readiness only. |
| Real review/paper observation backend counts are currently defaulted | `linkedPaperObservationCount` and `linkedReviewCount` exist but default to zero in the display object. | Future design should treat counts as read-only and fail-closed when absent. |
| Position Monitor boundary is adjacent | Dashboard workbench mentions real-position monitoring surfaces separately. | Future design must explicitly block Position Monitor execution and real position monitoring. |

## 6. Boundary Risks

Paper Observation / Paper Trading wording can be misread as executable if a later package crosses these lines:

- paper order creation;
- simulated execution;
- paper PnL calculation;
- real position monitoring;
- Position Monitor execution;
- order / execution / auto-trading;
- final direction / entry / stop / TP / RR output;
- Candidate generation;
- Decision generation;
- Point generation;
- Push send or external channel.

No such behavior is added by this source-read package. Existing occurrences of these terms in grep results are historical docs, existing guardrail text, or negative safety assertions.

## 7. DTO / Schema / Config Decision

Default decision for the next design:

- New DTO: No by default. Reuse `DashboardDetailResponseVO.PaperObservationDisplayVO`.
- New Validator: No by default.
- New Assembler: No by default.
- New Orchestrator: No by default.
- Schema/config/pom changes: No by default.

Any future request that requires new DTO / Validator / Assembler / Orchestrator, schema/config/pom, paper order, simulated execution, or real position monitoring must return NO-GO at readiness.

## 8. Source-Read Conclusion

Source-read result: GO to design.

Reason:

- Existing owner path is present: `DashboardController.dashboardDetail` -> safe default displays -> PlanBoundary -> ExecutionPlan -> RiskActionGuard -> PaperObservation display adapter.
- Existing display object already carries `notRealPosition`, `notTradeInstruction`, and `manualReviewRequired`.
- Existing tests lock fail-closed safety flags.
- No implementation is needed in this package.

The design phase must decide whether the minimal runtime slice should reuse `/api/dashboard/detail` only or add one thin review-only status endpoint / panel later. It must also define a status mapping that keeps Paper Observation / Paper Trading as read-only, manual-review, fail-closed display state.

## 9. Next Allowed Action

- Next allowed action: `Minimal Review-Only Paper Observation / Paper Trading Status Runtime Wiring Design`
- Next branch: `minimal-review-only-paper-observation-paper-trading-status-runtime-wiring-design`
- Next risk: `A`
- Allowed changes next: design docs and source-of-truth updates only.

## 10. Forbidden Scope

The next package must still forbid:

- Java business code changes;
- tests;
- dashboard business logic;
- schema/config/pom;
- paper trading execution;
- paper backtest execution;
- generated paper trading result;
- paper order;
- paper PnL;
- simulated execution;
- real position monitoring;
- Position Monitor execution;
- Candidate generation;
- Decision generation;
- Point generation;
- final direction / entry / stop / TP / RR;
- Push send / external channel;
- order / execution / auto-trading;
- new DTO / Validator / Assembler / Orchestrator;
- replay / recheck execution;
- P359 / P360.
