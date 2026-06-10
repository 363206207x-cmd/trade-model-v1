# V1 Minimal Review-Only ExecutionPlan / BoundaryCandidate Runtime Wiring Verification

## 1. Executive Summary

#876 后续的 ExecutionPlan / BoundaryCandidate 最小只读 runtime wiring 验证通过。验证对象是 `60e034a feat(wiring): show executionplan boundarycandidate review-only status` 中落地的只读 endpoint、dashboard panel、status mapping 和 targeted tests；当前 verification 分支基线按已合并 main `85fb7ad chore(workflow): refresh executionplan boundarycandidate post-implementation state` 对齐。

`GET /api/dashboard/execution-plan-boundary-status?symbol=BTCUSDT` 已存在并保持只读，只展示 ExecutionPlan / BoundaryCandidate runtime status，不触发 Candidate、新 Decision、Point、Push、order/execution/auto-trading，也不输出 final direction / entry / stop / TP / RR。

dashboard panel `executionPlanBoundaryStatusPanel` 已存在，并显示 review-only、not trading、not Candidate、not Decision generation、not Point、not executable、Display Slots 不是候选池、Watchlist / MarketQuote / Evidence-Score / DecisionResult 边界仍适用等安全文案。

status mapping 完整覆盖 `EXECUTIONPLAN_BOUNDARY_REVIEW_ONLY_READY`、`PLAN_BOUNDARY_BACKEND_PENDING_FAIL_CLOSED`、`PLAN_BOUNDARY_INCOMPLETE_FAIL_CLOSED`、`PLAN_BOUNDARY_WATCH_ONLY`、`EXECUTIONPLAN_BOUNDARY_PENDING_FAIL_CLOSED`、`EXECUTIONPLAN_SOURCE_TRACE_PARTIAL`、`EXECUTIONPLAN_RISK_GUARD_BLOCKED_FAIL_CLOSED`、`EXECUTIONPLAN_BOUNDARY_BLOCKED_FAIL_CLOSED`。缺少 DecisionResult 或 PlanBoundary owner data 时可验证为 fail-closed。

本 verification 包不新增 Java、不修改测试、不改 dashboard 业务逻辑、不改 schema/config/pom、不新增 DTO / Validator / Assembler / Orchestrator。当前 capability level 不提升，仍为 `REVIEW_ONLY_RUNTIME partial`。下一允许动作是 `ExecutionPlan / BoundaryCandidate Visual Verification / Closure`。

## 2. Verification Commands

| Command | Result |
|---|---|
| `git status --short` | PASS: verification 分支开始时 clean；本包后续只产生 docs/source-of-truth diff。 |
| `git branch --show-current` | PASS: `executionplan-boundarycandidate-runtime-wiring-verification`。 |
| `bash scripts/v1-state.sh` | PASS with expected branch blockers: branch work is not on main; Codex shell reports `GH_NOT_AVAILABLE`, which means GitHub status unknown per #877, not project state failure. Main sync was OK. |
| `bash scripts/check-workflow-contract.sh` | PASS: `WORKFLOW_CONTRACT_OK`。 |
| `./mvnw -q -DskipTests compile` | PASS. |
| `./mvnw -q -DskipTests test-compile` | PASS. |
| `./mvnw -q -Dtest=DashboardControllerTest test` | PASS. |
| `./mvnw -q test` | PASS. |
| `rg -n "execution-plan-boundary-status\|executionPlanBoundaryStatusPanel\|EXECUTIONPLAN_BOUNDARY\|PLAN_BOUNDARY_BACKEND\|notExecutable\|displaySlotsAreCandidatePool" src/main/java src/main/resources src/test/java docs` | PASS: endpoint, panel, status constants, safety flags, and tests are present. |
| `grep -RInE "entry\|stop\|take profit\|tp\|RR\|risk.reward\|final direction\|finalDirection\|order\|execution\|auto.trade\|autoTrading\|push\|external channel\|candidate\|point" src docs --exclude-dir=target --exclude-dir=.git \|\| true` | PASS after classification: hits are historical docs, guardrails/forbidden copy, existing tests, or tests asserting executable fields are absent. No new verification-package overreach. |
| `grep -RInE "class .*DTO\|Validator\|Assembler\|Orchestrator" src/main/java/org/example/trademodel \| grep -Ei "executionplan\|execution-plan\|boundarycandidate\|boundary-candidate" \|\| true` | PASS after classification: hits are existing owner assets such as `BoundaryCandidateDTO` / existing assembler usage, not new files from this package. |
| `git diff --check` | PASS. |

## 3. Endpoint Verification

| Endpoint | Method | Purpose | Trigger generation? | Trading semantics? | Result |
|---|---|---|---|---|---|
| `/api/dashboard/execution-plan-boundary-status?symbol=BTCUSDT` | GET | Review-only ExecutionPlan / BoundaryCandidate runtime status. | No. It reads existing dashboard detail / display adapter owner path and does not create Candidate, new Decision, or Point. | No. It returns status and guardrail flags only; no entry / stop / TP / RR, order action, Push send state, or auto-trading action. | PASS |

Verified fields include:

- `status`
- `symbol`
- `analysisId`
- `planBoundaryStatus`
- `executionPlanStatus`
- `sourceTraceStatus`
- `sourceTraceComplete`
- `sourceHealth`
- `riskActionGuardStatus`
- `notExecutableReason`
- `incompleteReasons`
- `blockingReasons`
- `reason`
- `message`
- `reviewOnly = true`
- `notTradingSignal = true`
- `notCandidateSignal = true`
- `notDecisionGeneration = true`
- `notPointSignal = true`
- `notExecutable = true`
- `watchlistBounded = true`
- `marketQuoteChecked = true`
- `evidenceScoreChecked = true`
- `decisionResultChecked = true`
- `displaySlotsAreCandidatePool = false`
- `failClosed`

Forbidden endpoint fields are covered by `DashboardControllerTest`: no `finalDirection`, `entry`, `stop`, `takeProfit`, `tp`, `rr`, `riskReward`, `positionSize`, `leverage`, `orderAction`, `pushSendState`, or `autoTradingAction`.

## 4. Dashboard Verification

| DOM id | Location | Shows status? | Shows review-only copy? | Shows boundary copy? | Shows forbidden action? | Result |
|---|---|---:|---:|---:|---:|---|
| `executionPlanBoundaryStatusPanel` | After DecisionResult status panel and before the main workbench/detail sections. | Yes | Yes | Yes | No | PASS |

Verified dashboard DOM / copy includes:

- `executionPlanBoundaryRuntimeStatusValue`
- `executionPlanBoundarySymbolValue`
- `executionPlanBoundaryAnalysisIdValue`
- `planBoundaryStatusValue`
- `executionPlanStatusValue`
- `executionPlanSourceTraceValue`
- `executionPlanSourceHealthValue`
- `executionPlanRiskGuardValue`
- `executionPlanNotExecutableReasonValue`
- `executionPlanBoundaryReviewOnlyValue`
- `executionPlanBoundarySignalBoundaryValue`
- `executionPlanBoundaryUpstreamValue`
- `executionPlanBoundaryReasonValue`

The panel shows ExecutionPlan / BoundaryCandidate status, symbol, analysisId when available, PlanBoundary status, ExecutionPlan status, source trace / source health, RiskActionGuard status, not executable reason, review-only label, not trading label, not Candidate / not new Decision generation / not Point label, upstream boundary label, and Display Slots boundary copy. It does not add Push buttons, order actions, executable point controls, final direction, entry, stop, TP, RR, position sizing, leverage, or trading action semantics.

## 5. Status Mapping Verification

| Status | Verified? | Fail-closed? | Source | Notes |
|---|---:|---:|---|---|
| `EXECUTIONPLAN_BOUNDARY_REVIEW_ONLY_READY` | Yes | No | `DashboardController` status mapping and `DashboardControllerTest`. | Ready state remains review-only and non-executable. |
| `PLAN_BOUNDARY_BACKEND_PENDING_FAIL_CLOSED` | Yes | Yes | Missing/pending PlanBoundary owner path mapping. | Backend pending status fails closed. |
| `PLAN_BOUNDARY_INCOMPLETE_FAIL_CLOSED` | Yes | Yes | PlanBoundary / ExecutionPlan incomplete mapping. | Incomplete owner data fails closed. |
| `PLAN_BOUNDARY_WATCH_ONLY` | Yes | Yes | Watch-only PlanBoundary / ExecutionPlan mapping. | Watch-only remains non-executable and fail-closed. |
| `EXECUTIONPLAN_BOUNDARY_PENDING_FAIL_CLOSED` | Yes | Yes | ExecutionPlan missing or pending mapping. | Pending ExecutionPlan fails closed. |
| `EXECUTIONPLAN_SOURCE_TRACE_PARTIAL` | Yes | Yes | SourceTrace completeness mapping. | Partial source trace fails closed. |
| `EXECUTIONPLAN_RISK_GUARD_BLOCKED_FAIL_CLOSED` | Yes | Yes | RiskActionGuard status mapping. | Unsafe or pending risk guard fails closed. |
| `EXECUTIONPLAN_BOUNDARY_BLOCKED_FAIL_CLOSED` | Yes | Yes | Missing DecisionResult / unknown fallback mapping. | Blocked fallback keeps the endpoint review-only and fail-closed. |

## 6. Test Coverage Verification

Verification confirmed targeted coverage in `DashboardControllerTest`:

- controller/API smoke test for `/api/dashboard/execution-plan-boundary-status`
- endpoint returns `reviewOnly=true`
- endpoint returns `notTradingSignal=true`
- endpoint returns `notCandidateSignal=true`
- endpoint returns `notDecisionGeneration=true`
- endpoint returns `notPointSignal=true`
- endpoint returns `notExecutable=true`
- endpoint returns `displaySlotsAreCandidatePool=false`
- missing DecisionResult fails closed
- missing PlanBoundary owner data fails closed
- forbidden executable fields are absent
- dashboard template contains `executionPlanBoundaryStatusPanel`
- dashboard template contains required DOM ids
- dashboard copy states review-only / not Candidate / not Decision generation / not Point / not executable boundaries

Full test suite also passed with `./mvnw -q test`.

## 7. Boundary Verification

| Boundary | Result |
|---|---|
| 是否接 Push | No |
| 是否接 external channel | No |
| 是否生成 Candidate | No |
| 是否生成新的 Decision | No |
| 是否生成 Point | No |
| 是否生成 final direction | No |
| 是否输出 entry/stop/TP/RR | No |
| 是否接 order/execution/auto-trading | No |
| 是否新增 DTO/Validator/Assembler/Orchestrator | No |
| 是否改 schema/config/pom | No |
| 是否继续 P359/P360 | No |
| 是否提升 capability level | No |

Forbidden semantics grep classification:

- Historical docs and roadmap entries mention forbidden domains as guardrails; these are not implementation behavior.
- Test hits in `DashboardControllerTest` assert forbidden fields are absent or belong to pre-existing fixture/source-trace tests.
- Implementation-related hits are status names, DOM ids, review-only guardrail labels, or explicit non-executable copy.
- This verification package introduces no Java/test/dashboard business diff and therefore introduces no new forbidden runtime behavior.

## 8. Source-of-Truth Drift Check

Source-of-truth drift was found and corrected:

- Verified implementation commit remains `60e034a feat(wiring): show executionplan boundarycandidate review-only status`.
- Current merged main baseline is `85fb7ad chore(workflow): refresh executionplan boundarycandidate post-implementation state`.
- `docs/ACTIVE_MAINLINE_STATUS.yml`, `docs/CODEX_NEXT_TASK.yml`, `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`, and `docs/V1_CURRENT_STATE.md` are updated to distinguish the current main baseline from the implementation commit under verification.
- The verification package is not marked completed on main before merge. It remains the active A-risk package until this branch merges.

## 9. Final Recommendation

Verification passes. The next allowed action is `ExecutionPlan / BoundaryCandidate Visual Verification / Closure`.

This remains `REVIEW_ONLY_RUNTIME partial` because it only verifies safe, non-executable runtime/dashboard/API status. It is not Production Wiring, not Push, not Candidate generation, not Decision generation, not Point generation, and not trading.
