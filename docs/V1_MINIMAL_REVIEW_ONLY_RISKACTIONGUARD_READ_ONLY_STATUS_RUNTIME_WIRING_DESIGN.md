# V1 Minimal Review-Only RiskActionGuard Read-Only Status Runtime Wiring Design

## 1. Executive Summary

本包只做 design（设计），不做 implementation（实现）。

最小目标：把现有 `RiskActionGuardDisplayAdapter` / `DefaultRiskActionGuardDisplayAdapter` / `RiskActionGuardDisplayVO` / `DashboardController` dashboard detail owner path 设计成一个用户可见、只读、失败即关闭的 RiskActionGuard runtime status（风险动作守卫运行时状态）小闭环。它只回答“当前风险动作守卫为什么只能人工复核 / 为什么保持关闭”，不回答“应该执行什么交易动作”。

Owner path 固定为：

```text
DashboardController /api/dashboard/detail
  -> DecisionResult / PlanBoundaryDisplay / ExecutionPlanDisplay
  -> RiskActionGuardDisplayAdapter
  -> DefaultRiskActionGuardDisplayAdapter
  -> DashboardDetailResponseVO.RiskActionGuardDisplayVO
  -> dashboard riskActionGuardPlaceholderCard / Risk Reminder copy
  -> future minimal review-only RiskActionGuard status surface
```

未来最小实现不需要新增 DTO / Validator / Assembler / Orchestrator，也不需要 schema/config/pom。默认优先复用现有 `RiskActionGuardDisplayVO` 和 dashboard detail read model；如果 readiness gate 证明现有 detail API 不足以形成独立状态小闭环，可以允许一个 dedicated read-only status endpoint，但只能返回 `Map` / existing object projection，不能创建新对象家族。

本设计不接 Push，不接 Candidate，不接 Decision generation，不接 Point，不生成 final direction / entry / stop / TP / RR，不接 order / execution / auto-trading，不执行 Position Monitor，不执行 replay / recheck，不继续 P359 / P360。

## 2. Owner Path And Endpoint Decision

未来最小实现应把 `/api/dashboard/detail` 作为 canonical owner/read source。理由：

- `/api/dashboard/detail` 已经构建 `riskActionGuardDisplay`。
- `DefaultRiskActionGuardDisplayAdapter` 已经把上游缺失、PlanBoundary 无效、ExecutionPlan 未就绪、流动性缺失、踩踏、插针、高风险等状态映射为 fail-closed/manual-review display。
- dashboard 已有 `riskActionGuardPlaceholderCard`、`executionPlanRiskGuardValue`、`executionPlanNotExecutableReasonValue` 和 Risk Reminder copy。

Dedicated endpoint 的设计结论：

- 不在 design 包实现。
- Readiness gate 应优先判断复用 `/api/dashboard/detail` 是否足够。
- 如果需要独立小闭环，可允许一个最小只读 endpoint，例如：

```text
GET /api/dashboard/risk-action-guard-status?symbol=BTCUSDT
```

该 endpoint 必须：

- 位于现有 `DashboardController` owner path。
- 只读。
- 只从 existing detail/adapter/VO projection 取值。
- 返回 `Map` 或 existing VO projection。
- 不调用 Position Monitor、order/execution、Push、Candidate、Decision generation、Point、replay/recheck。
- 不新增 DTO / Validator / Assembler / Orchestrator。

## 3. RiskActionGuardDisplayVO Status Source Fields

可作为 status source 的现有字段：

| Field | Design use | Boundary |
|---|---|---|
| `riskActionGuardStatus` | Primary existing guard status. | Must be normalized into review-only status labels, not executable readiness. |
| `riskActionGuardStatusLabel` | Human-readable display label. | Must keep manual-review wording. |
| `riskActionAdvice` | Explanation source for manual review reason. | Must be displayed as risk reminder only; action words are not commands. |
| `riskActionBlockingReason` | Fail-closed / partial reason source. | May drive status mapping. |
| `liquidityState` | Liquidity source-health context. | Missing or degraded liquidity fails closed. |
| `stampedeDetected` | Extreme pressure / stampede context. | True keeps status blocked/manual review only. |
| `wickOnlyRisk` | Wick-only / false reversal context. | True keeps status blocked/manual review only. |
| `opportunityPushAllowed` | Existing action flag. | Must remain false; true is blocked/fail-closed. |
| `reverseTradeAllowed` | Existing action flag. | Must remain false; true is blocked/fail-closed. |
| `newPositionAllowed` | Existing action flag. | Must remain false; true is blocked/fail-closed. |
| `marketOrderExitAllowed` | Existing action flag. | Must remain false; true is blocked/fail-closed. |
| `manualRiskReviewRequired` | Safety field. | Must be true. |
| `notTradeInstruction` | Existing non-trade safety field. | Map to `notTradingSignal=true`. |
| `updatedAt` | Optional display freshness hint. | Missing is partial, not a reason to execute anything. |

Fields requiring derived status names:

- `notTradingSignal=true` derived from `notTradeInstruction=true`.
- `notCandidateSignal=true` fixed true.
- `notDecisionGeneration=true` fixed true.
- `notPointSignal=true` fixed true.
- `notExecutable=true` fixed true.
- `displaySlotsAreCandidatePool=false` fixed false.

## 4. Minimal Status Mapping

Allowed future statuses:

| Status | Trigger condition | Dashboard/API copy intent | Candidate/Decision/Point/Push allowed? | Review-only? | Fail-closed? |
|---|---|---|---|---|---|
| `RISK_ACTION_GUARD_REVIEW_ONLY_READY` | `riskActionGuardStatus=MANUAL_REVIEW_REQUIRED`, upstream DecisionResult / PlanBoundary / ExecutionPlan context exists, action flags are all false, `manualRiskReviewRequired=true`, and `notTradeInstruction=true`. | RiskActionGuard is readable for manual review only; no action is authorized. | No | Yes | No for display; still no downstream action |
| `RISK_ACTION_GUARD_BACKEND_PENDING_FAIL_CLOSED` | Existing VO is absent, status blank, or status remains `BACKEND_PENDING`. | Backend status is not proven; keep risk guard closed. | No | Yes | Yes |
| `RISK_ACTION_GUARD_DECISION_MISSING_FAIL_CLOSED` | `riskActionBlockingReason=DECISION_MISSING`. | Decision context missing; risk guard cannot be evaluated. | No | Yes | Yes |
| `RISK_ACTION_GUARD_PLAN_BOUNDARY_FAIL_CLOSED` | `riskActionBlockingReason=PLAN_BOUNDARY_NOT_VALID`. | PlanBoundary is not valid; risk action remains closed. | No | Yes | Yes |
| `RISK_ACTION_GUARD_EXECUTION_PLAN_NOT_READY_FAIL_CLOSED` | `riskActionBlockingReason=EXECUTION_PLAN_NOT_READY`. | ExecutionPlan is not review-only ready; risk action remains closed. | No | Yes | Yes |
| `RISK_ACTION_GUARD_LIQUIDITY_CONTEXT_MISSING_FAIL_CLOSED` | `riskActionBlockingReason=LIQUIDITY_CONTEXT_MISSING` or `liquidityState` blank/`BACKEND_PENDING` when required. | Liquidity context missing; manual review only. | No | Yes | Yes |
| `RISK_ACTION_GUARD_LIQUIDITY_DETERIORATION_REVIEW_ONLY` | `riskActionBlockingReason=LIQUIDITY_DETERIORATION_REVIEW_ONLY` or liquidity state contains deterioration/stress wording. | Liquidity degraded; display risk reminder only. | No | Yes | Yes for downstream action |
| `RISK_ACTION_GUARD_STAMPEDE_REVIEW_ONLY_FAIL_CLOSED` | `stampedeDetected=true` or `riskActionBlockingReason=STAMPEDE_REVIEW_ONLY`. | Stampede pressure detected; all action paths remain closed. | No | Yes | Yes |
| `RISK_ACTION_GUARD_WICK_ONLY_REVIEW_ONLY_FAIL_CLOSED` | `wickOnlyRisk=true` or `riskActionBlockingReason=WICK_ONLY_REVIEW_ONLY`. | Wick-only risk is not trend reversal proof; manual review only. | No | Yes | Yes |
| `RISK_ACTION_GUARD_HIGH_RISK_REVIEW_ONLY` | `riskActionBlockingReason=HIGH_RISK_REVIEW_ONLY`. | High risk state requires manual review; no execution. | No | Yes | Yes for downstream action |
| `RISK_ACTION_GUARD_ACTION_FLAGS_BLOCKED_FAIL_CLOSED` | Any action flag is true: `opportunityPushAllowed`, `reverseTradeAllowed`, `newPositionAllowed`, or `marketOrderExitAllowed`. | Unexpected action permission detected; block and fail closed. | No | Yes | Yes |
| `RISK_ACTION_GUARD_ACTION_WORDING_BLOCKED_FAIL_CLOSED` | Copy/status cannot be shown without sounding like reduce/close/reverse/move stop/open/execute instructions. | Wording is unsafe; status remains blocked. | No | Yes | Yes |

Status precedence:

1. `RISK_ACTION_GUARD_ACTION_FLAGS_BLOCKED_FAIL_CLOSED`
2. `RISK_ACTION_GUARD_ACTION_WORDING_BLOCKED_FAIL_CLOSED`
3. `RISK_ACTION_GUARD_BACKEND_PENDING_FAIL_CLOSED`
4. `RISK_ACTION_GUARD_DECISION_MISSING_FAIL_CLOSED`
5. `RISK_ACTION_GUARD_PLAN_BOUNDARY_FAIL_CLOSED`
6. `RISK_ACTION_GUARD_EXECUTION_PLAN_NOT_READY_FAIL_CLOSED`
7. `RISK_ACTION_GUARD_LIQUIDITY_CONTEXT_MISSING_FAIL_CLOSED`
8. `RISK_ACTION_GUARD_STAMPEDE_REVIEW_ONLY_FAIL_CLOSED`
9. `RISK_ACTION_GUARD_WICK_ONLY_REVIEW_ONLY_FAIL_CLOSED`
10. `RISK_ACTION_GUARD_LIQUIDITY_DETERIORATION_REVIEW_ONLY`
11. `RISK_ACTION_GUARD_HIGH_RISK_REVIEW_ONLY`
12. `RISK_ACTION_GUARD_REVIEW_ONLY_READY`

`READY` means the risk reminder is readable for manual review only. It is not a trading readiness state.

## 5. Minimal Future Fields

Allowed future fields:

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
- `reviewOnly = true`
- `notTradingSignal = true`
- `notCandidateSignal = true`
- `notDecisionGeneration = true`
- `notPointSignal = true`
- `notExecutable = true`
- `positionMonitorNotExecuted = true`
- `displaySlotsAreCandidatePool = false`

Forbidden future fields:

- candidate ranking
- generated decision
- final direction
- entry
- stop
- TP
- RR
- position size
- leverage
- order action
- Push send state
- external channel state
- Position Monitor action
- close / reverse / move stop / open / execute command
- replay / recheck execution action

## 6. Manual Review Only And Action Wording Guardrails

The following must always be manual review only:

- `riskActionAdvice`
- `riskActionBlockingReason`
- liquidity deterioration / missing liquidity copy
- stampede copy
- wick-only copy
- high-risk copy
- strong reversal / moving stop copy
- action flag display

Action wording rules:

- Words such as reduce, close, reverse, move stop, open, execute, 减仓, 平仓, 反手, 移动止损, 开仓, 执行 may appear only inside negative/manual-review copy.
- Dashboard copy must attach these words to “不是交易指令 / only manual review / not executable / action disabled”.
- Future endpoint must not expose these words as field names that imply command state.
- If copy cannot be safely summarized, use `RISK_ACTION_GUARD_ACTION_WORDING_BLOCKED_FAIL_CLOSED`.
- Existing action flags may be shown only to prove they are false.

Safe wording examples:

- “RiskActionGuard 是只读风险提醒，不是交易信号。”
- “动作标志保持关闭，仅允许人工复核。”
- “强反转 / 移动止损文案只说明风险原因，不生成执行动作。”

Unsafe wording examples:

- “close position recommended”
- “move stop now”
- “reverse allowed”
- “open new position”
- “execute guard action”

## 7. Dashboard/API Surface

Future dashboard/API surface may show:

- RiskActionGuard status.
- status label.
- blocking reason.
- advice summary with manual-review-only wording.
- liquidity state.
- stampede / wick-only flags.
- action flags all false.
- source trace / source health summary if available from existing detail context.
- fail-closed reason.
- review-only label.
- not trading / not candidate / not decision generation / not point / not executable labels.
- Position Monitor not executed label.
- Display Slots not candidate pool label.

Dashboard placement options for readiness gate:

1. Reuse existing `riskActionGuardPlaceholderCard` and strengthen its copy/DOM only if safe.
2. Add one minimal `riskActionGuardStatusPanel` near existing ExecutionPlan / BoundaryCandidate / Risk Reminder surfaces if a dedicated panel is required.

Dashboard must not:

- add Position Monitor controls;
- add close / reverse / move stop / open / execute buttons;
- show entry / stop / TP / RR;
- show final direction;
- show Push action;
- show Candidate ranking;
- show Point generation;
- imply order/execution/auto-trading.

## 8. Fail-Closed Rules

Future status must fail closed when:

- `riskActionGuardDisplay` is missing.
- `riskActionGuardStatus` is blank or `BACKEND_PENDING`.
- DecisionResult context is missing.
- PlanBoundary is not valid.
- ExecutionPlan is not `READY_REVIEW_ONLY`.
- liquidity state is missing when needed.
- liquidity deterioration / stress is detected.
- stampede is detected.
- wick-only risk is detected.
- high-risk state requires manual review.
- any action flag is true.
- manual review flag is not true.
- `notTradeInstruction` is not true.
- action wording can be misunderstood as executable instruction.
- answering would require Position Monitor execution, Push, Candidate generation, Decision generation, Point generation, final direction, entry / stop / TP / RR, order/execution/auto-trading, replay/recheck, schema/config/pom, or new DTO / Validator / Assembler / Orchestrator.

Fail-closed means the status can stay visible, but every downstream action implication remains closed.

## 9. Boundary With Position Monitor / ExecutionPlan / Point / Trading

RiskActionGuard status may explain why ExecutionPlan or BoundaryCandidate should remain not executable. It must not become:

- Position Monitor action source.
- ExecutionPlan command source.
- BoundaryCandidate validity upgrade.
- Point generator.
- Candidate generator.
- Push permission.
- trading signal.
- order or execution authorization.

ExecutionPlan / BoundaryCandidate may consume RiskActionGuard as an upstream blocker, but this slice only exposes the guard state for review. It cannot promote a guard state into final direction, entry/stop/TP/RR, close/reverse/open/move-stop, or trading execution.

## 10. Readiness Checklist

The next readiness gate must check:

- Can `/api/dashboard/detail` alone satisfy the first RiskActionGuard status surface?
- If not, can one minimal `DashboardController` read-only Map endpoint be added safely?
- Can the endpoint be derived from `RiskActionGuardDisplayVO` without new DTO / Validator / Assembler / Orchestrator?
- Which `RiskActionGuardDisplayVO` fields are sufficient for status mapping?
- Can missing decision, invalid PlanBoundary, not-ready ExecutionPlan, missing/degraded liquidity, stampede, wick-only, and high-risk states map to fail-closed statuses?
- Can action flags be shown only as false/disabled evidence?
- Can unsafe action wording be summarized without turning into commands?
- Does dashboard have a safe insertion position near existing `riskActionGuardPlaceholderCard`?
- Which targeted tests already exist, and which controller/dashboard tests would be needed if implementation proceeds?
- Can future implementation avoid Java service expansion beyond minimal controller/dashboard glue?
- Can future implementation avoid schema/config/pom changes?
- Can forbidden semantics grep distinguish negative safety copy from positive action output?

## 11. Minimal Future Implementation Boundary

If readiness gate returns GO, future implementation may modify at most:

- `src/main/java/org/example/trademodel/controller/DashboardController.java` only if a dedicated minimal read-only endpoint is required.
- `src/main/resources/templates/dashboard.html` only for minimal status/copy/DOM.
- `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java` only for targeted endpoint/dashboard assertions.
- Existing RiskActionGuard adapter tests only if the implementation reuses existing adapter behavior and needs a tiny assertion adjustment.
- Source-of-truth docs.
- One implementation report doc.

Future implementation must not modify:

- schema/config/pom.
- new DTO / Validator / Assembler / Orchestrator.
- RiskActionGuard service ownership beyond existing adapter/controller path.
- Position Monitor implementation.
- order/execution/auto-trading.
- Push/external channel.
- Candidate / Decision generation / Point.
- final direction / entry / stop / TP / RR.
- replay/recheck.
- P359/P360.

## 12. Capability-Level Movement

- Current level: `REVIEW_ONLY_RUNTIME partial`.
- This package raises capability level: No, design only.
- Future minimal implementation target: keep `REVIEW_ONLY_RUNTIME partial` by adding review-only visibility over existing RiskActionGuard display owner assets.
- It is not Production Wiring.
- It is not Push.
- It is not Candidate generation.
- It is not Decision generation.
- It is not Point generation.
- It is not Trading.

## 13. Freeze Rule Compliance

- 是否创建新骨架: No.
- 是否复用 Cursor-era / V1 资产: Yes.
- 是否减少重复: Yes. The design keeps the existing RiskActionGuard display adapter / VO owner path.
- 是否提升 capability level: No, design only.
- 是否接 service/runtime/dashboard/API: No, design only; future readiness may authorize minimal read-only DashboardController/dashboard wiring.
- 是否符合 #830 审计建议: Yes.

## 14. Final Recommendation

GO to `Implementation readiness gate for RiskActionGuard read-only status`.

下一步只能做 readiness gate，不是 implementation。Readiness gate 应优先复用 `/api/dashboard/detail`、`DefaultRiskActionGuardDisplayAdapter`、`RiskActionGuardDisplayVO` 和 dashboard placeholder/copy；如需 dedicated endpoint，只允许最小 read-only `DashboardController` Map projection。禁止 Push、Candidate、Decision generation、Point、Position Monitor execution、final direction、entry / stop / TP / RR、order/execution/auto-trading、DTO / Validator / Assembler / Orchestrator、schema/config/pom、P359 / P360。
