# V1 RiskActionGuard Read-Only Status Implementation Readiness Gate

## 1. Executive Summary

Decision: **GO** to `Minimal Review-Only RiskActionGuard Read-Only Status Runtime Wiring Implementation`.

This package is readiness-gate only. It does not implement Java business code, tests, dashboard business logic, schema/config/pom changes, endpoint/panel wiring, external API refresh, scheduler/collector/API client triggers, Push, Candidate generation, Decision generation, Point generation, final direction, entry/stop/TP/RR, order/execution, auto-trading, DTO, Validator, Assembler, Orchestrator, Position Monitor execution, replay/recheck, P359, or P360.

GO is narrow:

- reuse the existing Dashboard detail owner path as the canonical read source;
- prefer `/api/dashboard/detail` plus `riskActionGuardDisplay` as the first implementation source;
- allow one minimal dedicated read-only status endpoint only if implementation proves the detail path is not enough for a compact status surface;
- use `RiskActionGuardDisplayVO` fields as read-only status sources;
- expose reduce / close / reverse / move stop / open / execute wording only as negative guardrail or manual-review copy;
- require targeted controller/dashboard tests in the implementation package;
- forbid new DTO / Validator / Assembler / Orchestrator and schema/config/pom changes.

Current capability level does not move. The project remains `REVIEW_ONLY_RUNTIME partial`, and completed review-only runtime partial slices remain 10.

## 2. Current Baseline

- Current merged main baseline: `5d25e53 docs(risk): design risk action guard read-only wiring (#931)`
- Current module: `RiskActionGuard read-only status`
- Current phase: `Implementation readiness gate`
- Risk level: `A` for this docs-only readiness gate
- Next implementation risk: `B`, because implementation may touch existing controller/dashboard/test files

## 3. Source-Read And Design Summary

`docs/V1_RISKACTIONGUARD_READ_ONLY_STATUS_SOURCE_READ.md` confirmed reusable owner assets:

```text
DashboardController /api/dashboard/detail
  -> DashboardDetailResponseVO.withSafeDefaultDisplays()
  -> PlanBoundaryDisplayAdapter
  -> ExecutionPlanDisplayAdapter
  -> RiskActionGuardDisplayAdapter / DefaultRiskActionGuardDisplayAdapter
  -> DashboardDetailResponseVO.RiskActionGuardDisplayVO
  -> dashboard.html riskActionGuardPlaceholderCard / executionPlanRiskGuardValue / Risk Reminder copy
```

`docs/V1_MINIMAL_REVIEW_ONLY_RISKACTIONGUARD_READ_ONLY_STATUS_RUNTIME_WIRING_DESIGN.md` fixed the minimal status mapping and rejected these paths for the minimal slice:

```text
new RiskActionGuard DTO / Validator / Assembler / Orchestrator family
new schema/config/pom ownership
Position Monitor execution
Push / external channel
Candidate generation
Decision generation
Point generation
final direction / entry / stop / TP / RR
order / execution / auto-trading
replay / recheck
P359 / P360
```

The design allows a minimal status surface only if it stays on existing read/display owners and fails closed when status cannot be shown without action wording ambiguity.

## 4. Endpoint Decision

Readiness decision: **reuse Dashboard detail owner path first**.

`/api/dashboard/detail` already exposes `riskActionGuardDisplay`, which is built by the existing adapter chain. Therefore the implementation package must first try to derive the status from the existing detail owner path.

A dedicated endpoint is allowed only as a minimal convenience/status surface:

```text
GET /api/dashboard/risk-action-guard-status?symbol=BTCUSDT
```

If added, it must:

- live in existing `DashboardController`;
- be `GET` and read-only;
- derive values from existing `dashboardDetail(...)`, adapter output, or existing VO projection;
- return a `Map<String, Object>` or existing object projection;
- avoid creating DTO / Validator / Assembler / Orchestrator;
- avoid service expansion or Position Monitor execution;
- avoid Push, Candidate generation, Decision generation, Point generation, final direction, entry/stop/TP/RR, order/execution, and trading.

## 5. Readiness Questions

| Gate question | Decision | Reason |
|---|---|---|
| Is an existing owner path present? | **Yes** | `DashboardController`, `/api/dashboard/detail`, `RiskActionGuardDisplayAdapter`, `DefaultRiskActionGuardDisplayAdapter`, and `RiskActionGuardDisplayVO` exist. |
| Can implementation reuse Dashboard detail? | **Yes** | `dashboardDetail` already builds `riskActionGuardDisplay` after PlanBoundary and ExecutionPlan displays. |
| Is a dedicated endpoint mandatory? | **No** | It is optional. Use only if a compact status endpoint is needed for dashboard/status tests. |
| Can implementation avoid DTO / Validator / Assembler / Orchestrator? | **Yes** | A `Map` response or existing VO projection is enough. |
| Can implementation avoid schema/config/pom changes? | **Yes** | Existing display objects and dashboard detail path carry enough information. |
| Can fail-closed be proven? | **Yes** | `DefaultRiskActionGuardDisplayAdapter` enforces action flags false, manual review true, and `notTradeInstruction=true`. |
| Can reduce / close / reverse / move stop / open / execute stay non-executable? | **Yes, with strict copy rules** | They may appear only as negative/manual-review explanations and must never become field names or commands. |
| Can dashboard remain outside Position Monitor / Trading? | **Yes** | Future dashboard work is limited to status/copy/DOM and must not add controls, buttons, or action paths. |
| Can tests cover the boundary? | **Yes** | Existing `DefaultRiskActionGuardDisplayAdapterTest`, `DashboardControllerTest`, and static dashboard patterns can be extended narrowly. |

## 6. Existing Source Fields Allowed

The next implementation may read these existing `RiskActionGuardDisplayVO` fields:

| Field | Allowed use | Guardrail |
|---|---|---|
| `riskActionGuardStatus` | Primary owner status source. | Must not mean executable readiness. |
| `riskActionGuardStatusLabel` | Display label. | Must stay manual-review / status label only. |
| `riskActionAdvice` | Manual-review explanation summary. | Must not be exposed as command text. |
| `riskActionBlockingReason` | Fail-closed reason source. | May drive status mapping. |
| `liquidityState` | Liquidity context. | Missing/degraded values fail closed. |
| `stampedeDetected` | Stampede risk context. | True blocks downstream action. |
| `wickOnlyRisk` | Wick-only risk context. | True blocks trend-reversal/action implication. |
| `opportunityPushAllowed` | Existing action flag evidence. | Must be false; true is fail-closed. |
| `reverseTradeAllowed` | Existing action flag evidence. | Must be false; true is fail-closed. |
| `newPositionAllowed` | Existing action flag evidence. | Must be false; true is fail-closed. |
| `marketOrderExitAllowed` | Existing action flag evidence. | Must be false; true is fail-closed. |
| `manualRiskReviewRequired` | Safety flag. | Must be true. |
| `notTradeInstruction` | Existing non-trade flag. | Map to `notTradingSignal=true`. |
| `updatedAt` | Optional freshness hint. | Missing is partial/unknown, not executable. |

## 7. Required Status Mapping

| Status | Existing asset judgment | Data source | Gap | Implementation allowed? | Fail-closed? |
|---|---|---|---|---:|---:|
| `RISK_ACTION_GUARD_REVIEW_ONLY_READY` | Implementable | `riskActionGuardStatus`, action flags false, manual-review fields | Needs compact status projection | Yes | No for display; still not executable |
| `RISK_ACTION_GUARD_BACKEND_PENDING_FAIL_CLOSED` | Implementable | Missing/blank/`BACKEND_PENDING` display | Needs endpoint/dashboard mapping | Yes | Yes |
| `RISK_ACTION_GUARD_DECISION_MISSING_FAIL_CLOSED` | Implementable | `riskActionBlockingReason=DECISION_MISSING` | None | Yes | Yes |
| `RISK_ACTION_GUARD_PLAN_BOUNDARY_FAIL_CLOSED` | Implementable | `riskActionBlockingReason=PLAN_BOUNDARY_NOT_VALID` | None | Yes | Yes |
| `RISK_ACTION_GUARD_EXECUTION_PLAN_NOT_READY_FAIL_CLOSED` | Implementable | `riskActionBlockingReason=EXECUTION_PLAN_NOT_READY` | None | Yes | Yes |
| `RISK_ACTION_GUARD_LIQUIDITY_CONTEXT_MISSING_FAIL_CLOSED` | Implementable | `liquidityState`, `riskActionBlockingReason` | None | Yes | Yes |
| `RISK_ACTION_GUARD_LIQUIDITY_DETERIORATION_REVIEW_ONLY` | Implementable | `liquidityState`, adapter blocking reason | Copy must remain review-only | Yes | Yes for downstream action |
| `RISK_ACTION_GUARD_STAMPEDE_REVIEW_ONLY_FAIL_CLOSED` | Implementable | `stampedeDetected`, blocking reason | None | Yes | Yes |
| `RISK_ACTION_GUARD_WICK_ONLY_REVIEW_ONLY_FAIL_CLOSED` | Implementable | `wickOnlyRisk`, blocking reason | None | Yes | Yes |
| `RISK_ACTION_GUARD_HIGH_RISK_REVIEW_ONLY` | Implementable | adapter high-risk path | Copy must remain review-only | Yes | Yes for downstream action |
| `RISK_ACTION_GUARD_ACTION_FLAGS_BLOCKED_FAIL_CLOSED` | Implementable | any action flag unexpectedly true | Needs explicit guard/test | Yes | Yes |
| `RISK_ACTION_GUARD_ACTION_WORDING_BLOCKED_FAIL_CLOSED` | Implementable | unsafe action wording classification | Needs explicit copy/test rule | Yes | Yes |

## 8. Action Wording Readiness

The following words are unsafe unless framed as negative/manual-review guardrail copy:

- reduce / 减仓
- close / 平仓
- reverse / 反手
- move stop / 移动止损
- open / 开仓
- execute / 执行
- order / 下单
- auto-trading / 自动交易

Implementation must guarantee:

- these words never become commands, buttons, endpoint field names, or positive action states;
- existing action flags may be displayed only as disabled/false evidence;
- if wording cannot be safely summarized, status must be `RISK_ACTION_GUARD_ACTION_WORDING_BLOCKED_FAIL_CLOSED`;
- copy must include review-only, manual review required, not trading signal, not candidate, not decision generation, not point, not executable.

## 9. Allowed Future Implementation Scope

If this readiness gate is merged, the next implementation package may change only:

- `src/main/java/org/example/trademodel/controller/DashboardController.java` for one minimal read-only Map endpoint if needed;
- `src/main/resources/templates/dashboard.html` for one minimal `riskActionGuardStatusPanel` or safe upgrade of `riskActionGuardPlaceholderCard` status/copy/DOM;
- `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java` for targeted endpoint/dashboard assertions;
- `src/test/java/org/example/trademodel/service/dashboard/DefaultRiskActionGuardDisplayAdapterTest.java` only if a tiny assertion is needed around existing adapter behavior;
- implementation report documentation;
- source-of-truth documents.

Allowed future status fields:

- `status`
- `symbol`
- `riskActionGuardStatus`
- `riskActionGuardStatusLabel`
- `riskActionAdviceSummary`
- `riskActionBlockingReason`
- `liquidityState`
- `stampedeDetected`
- `wickOnlyRisk`
- `manualRiskReviewRequired`
- `actionFlagsAllFalse`
- `opportunityPushAllowed`
- `reverseTradeAllowed`
- `newPositionAllowed`
- `marketOrderExitAllowed`
- `sourceHealth`
- `sourceTraceComplete`
- `reason`
- `message`
- `updatedAt`
- `failClosed`
- `reviewOnly=true`
- `notTradingSignal=true`
- `notCandidateSignal=true`
- `notDecisionGeneration=true`
- `notPointSignal=true`
- `notExecutable=true`
- `positionMonitorNotExecuted=true`
- `displaySlotsAreCandidatePool=false`

## 10. Forbidden Future Implementation Scope

The next implementation must not:

- add DTO / Validator / Assembler / Orchestrator;
- change schema/config/pom;
- add a new RiskActionGuard service/domain ownership family;
- run Position Monitor;
- call order/execution/auto-trading paths;
- connect Push or external channels;
- generate Candidate;
- generate Decision;
- generate Point;
- output final direction;
- output entry / stop / TP / RR;
- output position size, leverage, order action, execution action, or executable action;
- treat Display Slots as candidate pool;
- trigger external API refresh, scheduler, collector, or API client calls;
- execute replay/recheck;
- continue P359 or start P360.

## 11. Required Future Tests

The B-risk implementation package must include targeted tests for:

- endpoint returns `reviewOnly=true` if a dedicated endpoint is added;
- endpoint returns `notTradingSignal=true`, `notCandidateSignal=true`, `notDecisionGeneration=true`, `notPointSignal=true`, `notExecutable=true`, and `positionMonitorNotExecuted=true`;
- endpoint returns `displaySlotsAreCandidatePool=false`;
- endpoint returns fail-closed when `riskActionGuardDisplay` is missing or `BACKEND_PENDING`;
- endpoint maps `DECISION_MISSING`, `PLAN_BOUNDARY_NOT_VALID`, `EXECUTION_PLAN_NOT_READY`, liquidity missing/degraded, stampede, wick-only, and high-risk states;
- endpoint fail-closes if any action flag is true;
- response does not expose final direction / entry / stop / TP / RR / position size / leverage / order action / execution action / auto-trading fields;
- dashboard contains `riskActionGuardStatusPanel` or upgraded `riskActionGuardPlaceholderCard` status DOM;
- dashboard copy clearly says review-only / manual review / not trading / not candidate / not decision generation / not point / not executable;
- dashboard does not add close / reverse / move stop / open / execute controls;
- forbidden semantics grep classifies negative safety copy separately from positive action output.

## 12. NO-GO Conditions

The next implementation must stop if:

- it requires DTO / Validator / Assembler / Orchestrator;
- it requires schema/config/pom;
- it requires a new RiskActionGuard service/domain ownership family;
- `/api/dashboard/detail` and existing `RiskActionGuardDisplayVO` are insufficient unless new owner objects are added;
- action wording cannot be shown safely without sounding executable;
- it needs Position Monitor execution;
- it needs Push, Candidate generation, Decision generation, Point generation, final direction, entry/stop/TP/RR, order/execution, or auto-trading;
- it needs external API refresh, scheduler, collector, or API client calls;
- dashboard has no safe insertion point;
- tests cannot prove action flags stay false and forbidden fields stay absent.

## 13. Readiness Result

Result: **GO**.

GO rationale:

- Existing owner path is real and already used by dashboard detail.
- Existing `RiskActionGuardDisplayVO` fields are enough for a minimal read-only status.
- Existing adapter already enforces fail-closed defaults and action flags false.
- Existing dashboard placeholder and Risk Reminder copy provide a safe insertion neighborhood.
- Existing tests cover adapter fail-closed behavior and forbidden method absence; implementation can add focused controller/dashboard tests.
- No new DTO / Validator / Assembler / Orchestrator, schema/config/pom, service ownership, Push, Candidate generation, Decision generation, Point, or trading is required.

## 14. Capability Movement

- Current level: `REVIEW_ONLY_RUNTIME partial`.
- This package raises capability level: No, readiness gate only.
- Completed review-only runtime partial slices remain 10.
- RiskActionGuard read-only status is not implemented by this package.
- Future implementation target: remain `REVIEW_ONLY_RUNTIME partial`, not Production Wiring.

## 15. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 资产: Yes
- 是否减少重复: Yes, by forcing reuse of existing Dashboard detail / adapter / VO owner assets
- 是否提升 capability level: No, readiness gate only
- 是否接 service/runtime/dashboard/API: No in this package; future implementation may minimally connect existing dashboard/API review-only status
- 是否符合 #830 审计建议: Yes

## 16. Next Allowed Action

Next allowed action: **Minimal Review-Only RiskActionGuard Read-Only Status Runtime Wiring Implementation**.

Next implementation risk: **B**.

The next package may add only one minimal read-only status endpoint if needed, minimal dashboard status/copy/DOM, targeted tests, implementation docs, and source-of-truth updates over existing Dashboard detail / RiskActionGuard display assets. It must not add DTO / Validator / Assembler / Orchestrator, schema/config/pom, Position Monitor execution, Push, Candidate generation, Decision generation, Point generation, final direction, entry/stop/TP/RR, order/execution, auto-trading, replay/recheck, P359, or P360.
