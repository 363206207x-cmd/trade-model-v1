# V1 Minimal Review-Only Review / Replay Result Status Runtime Wiring Design

## 1. Executive Summary

本任务只做 design，不实现功能。

最小目标：未来用既有 ReviewResult / ReviewService / ReviewController / ReviewAggregate / replay summary 只读资产，展示 Review / Replay result status 是否可读、是否完整、是否 fail-closed、source trace / source health 是否足够，并明确它只是 Review-Only Runtime partial 状态，不是交易信号，不触发 replay execution，不生成 Candidate / Decision / Point。

Owner path:

```text
Completed upstream review-only runtime slices
  -> ReviewResult / tm_review_result / ReviewResultMapper
  -> ReviewService#getStateByAnalysisId
  -> ReviewController GET review state / aggregate / summary / detail paths
  -> ReviewAggregateService / ReviewAggregateVO
  -> optional PushRecheck replay summary read path
  -> future minimal review-only Review / Replay result status API/dashboard panel
```

本设计不需要新增 DTO / Validator / Assembler / Orchestrator，不需要改 schema/config/pom，不需要接 Push / external channel / Candidate / Decision generation / Point / Trading，也不会生成 final direction / entry / stop / TP / RR。

下一步应该进入 `Minimal Review-Only Review / Replay Result Status Runtime Wiring Implementation Readiness Gate`，只判断能否实现，不直接写功能。

## 2. Owner Path To Preserve

必须保留的 owner path:

```text
Watchlist / MarketQuote / Evidence-Score / DecisionResult / ExecutionPlan-BoundaryCandidate completed slices
  -> ReviewResult / tm_review_result / ReviewResultMapper
  -> ReviewService#getStateByAnalysisId
  -> ReviewController GET /api/review/state/{analysisId}
  -> ReviewController GET /api/review/aggregate/{analysisId}/summary and /detail
  -> ReviewAggregateService / ReviewAggregateVO
  -> optional PushRecheck replay summary read path
  -> future minimal Review / Replay result status
```

未来实现不得绕过现有 Review / ReviewAggregate owner path，不允许新增 Review / Replay wrapper owner。

Future status must not call:

- `POST /api/review/save`
- `POST /api/push/recheck/replay`
- `PushRecheckService#replayByDispatch`
- any replay / recheck mutation path

Future status may consider read-only replay summary only if readiness gate confirms it can be isolated from replay execution.

Display Slots 仍不是候选池。Review / Replay status 也不得把 Display Slots、ReviewResult、Replay summary 提升为 Candidate / Decision / Point 来源。所有资产边界必须服从已完成的 Watchlist / MarketQuote / Evidence-Score / DecisionResult / ExecutionPlan-BoundaryCandidate review-only slices。

## 3. Minimal Future Status Mapping

Allowed status:

| Status | Trigger condition | Dashboard/API copy | Candidate/Decision/Point/Push allowed? | Review-only? | Fail-closed? |
|---|---|---|---|---|---|
| `REVIEW_REPLAY_REVIEW_ONLY_READY` | ReviewResult or review state is readable, aggregate summary/detail is readable, source trace is sufficient, and no replay execution is needed. | Review / Replay 状态可读，仅用于复盘状态展示。 | No | Yes | No |
| `REVIEW_RESULT_MISSING_FAIL_CLOSED` | ReviewResult / review state is missing for the requested analysis id. | ReviewResult 缺失，状态 fail-closed，不生成候选/决策/点位。 | No | Yes | Yes |
| `REVIEW_AGGREGATE_MISSING_FAIL_CLOSED` | ReviewAggregate summary/detail is missing or empty. | Review aggregate 缺失，无法确认复盘上下文，状态 fail-closed。 | No | Yes | Yes |
| `REPLAY_SUMMARY_MISSING_FAIL_CLOSED` | Replay summary is required by future readiness criteria but unavailable or ambiguous. | Replay summary 不完整，仅显示缺失状态，不执行 replay。 | No | Yes | Yes |
| `REVIEW_REPLAY_SOURCE_TRACE_PARTIAL` | Review / aggregate data exists but source trace, provenance, or source health is partial. | Source trace 部分可见，保持只读并 fail-closed。 | No | Yes | Yes |
| `REPLAY_EXECUTION_BLOCKED_FAIL_CLOSED` | The only way to answer would require replay execution or recheck mutation. | Replay execution 被阻断；此状态只能读取，不执行回放。 | No | Yes | Yes |
| `REVIEW_REPLAY_BLOCKED_FAIL_CLOSED` | Unsupported input, ambiguous analysis id, owner path unavailable, or any safety boundary is unclear. | Review / Replay 状态被阻断，保持 fail-closed。 | No | Yes | Yes |

## 4. Minimal Future Fields

Allowed future fields:

- `status`
- `analysisId`
- `symbol` if available
- `reviewResultAvailable`
- `reviewStateAvailable`
- `reviewAggregateAvailable`
- `reviewClosureAvailable`
- `replaySummaryAvailable`
- `replaySummary`
- `sourceTraceComplete`
- `sourceHealth`
- `failClosed`
- `reason`
- `message`
- `reviewOnly = true`
- `notTradingSignal = true`
- `notCandidateSignal = true`
- `notDecisionGeneration = true`
- `notPointSignal = true`
- `notReplayExecution = true`
- `watchlistBounded = true`
- `marketQuoteChecked = true`
- `evidenceScoreChecked = true`
- `decisionResultChecked = true`
- `executionPlanBoundaryChecked = true`
- `displaySlotsAreCandidatePool = false`

Forbidden future fields:

- candidate ranking
- generated decision
- executable final direction
- entry
- stop
- TP
- RR
- position size
- leverage
- order action
- Push send state
- replay execution action
- rule auto-correction action

## 5. Dashboard/API Minimal Surface

Future minimal API/dashboard surface should show only:

- ReviewResult availability
- review state availability
- ReviewAggregate availability
- replay summary availability, only as read-only summary
- source trace completeness
- source health
- fail-closed reason
- review-only label
- not trading / not candidate / not decision generation / not point label
- not replay execution label
- completed upstream boundary label: Watchlist / MarketQuote / Evidence-Score / DecisionResult / ExecutionPlan-BoundaryCandidate

If an existing read endpoint can safely carry the status, readiness gate should prefer reuse. If no safe endpoint exists, readiness gate may allow one minimal read-only endpoint, for example:

```text
GET /api/dashboard/review-replay-status?analysisId=...
```

The endpoint must return a minimal map or existing VO shape only if implementation later proceeds; no new DTO / Validator / Assembler is allowed.

Dashboard must not add a complex review editor, replay button, Push button, order action, or auto-correction action. Any panel must be a minimal status/copy/DOM addition only after readiness gate.

## 6. Replay Execution Boundary

Replay summary is not replay execution.

Future implementation may only read already-existing replay summary data if readiness gate confirms it is safe. It must not:

- call replay execution
- create replay batch/logs
- trigger recheck mutation
- send Push
- open an external channel
- write review result
- write rule correction
- generate Candidate / Decision / Point

If replay status cannot be determined without execution, the required status is `REPLAY_EXECUTION_BLOCKED_FAIL_CLOSED`.

## 7. Completed Slice Boundary

Review / Replay status must not bypass:

- Watchlist / RuleConfig boundary
- MarketQuote freshness/fallback status
- Evidence / Score review-only status
- DecisionResult review-only status
- ExecutionPlan / BoundaryCandidate review-only status
- Display Slots boundary
- source trace / source health
- fail-closed ambiguity rule

Missing, stale, ambiguous, or partial upstream data must fail closed. Review / Replay status is a visibility layer, not a new source of trading intent.

## 8. Minimal Future Implementation Boundary

If readiness gate returns GO, future implementation must stay within:

- existing `ReviewService`
- existing `ReviewController` GET read paths
- existing `ReviewResultMapper` / `tm_review_result`
- existing `ReviewAggregateService` / aggregate VO assets
- existing read-only replay summary assets, only if isolated from execution
- optional minimal status endpoint after readiness gate
- optional minimal dashboard status/copy/DOM after readiness gate
- targeted tests only
- source-of-truth docs

Future implementation must not:

- add DTO / Validator / Assembler / Orchestrator
- change schema/config/pom
- call review save path
- call replay execution path
- connect Push / external channel
- generate Candidate
- generate a new Decision
- generate Point
- output final direction / entry / stop / TP / RR
- connect order / execution / auto-trading
- continue P359 / P360

## 9. Readiness Checklist

The next readiness gate must check:

- Whether an existing read endpoint is sufficient.
- Whether a new minimal endpoint is truly needed.
- Whether the status can use Map / existing object / existing VO without a new DTO.
- Whether `ReviewService#getStateByAnalysisId` fields are sufficient.
- Whether ReviewAggregate summary/detail fields are sufficient.
- Whether replay summary fields are sufficient without executing replay.
- Whether source trace / source health fields are sufficient.
- Whether dashboard has a safe insertion slot.
- Whether targeted tests already exist and what minimal tests are missing.
- Whether missing ReviewResult / aggregate / replay summary can fail closed.
- Whether implementation can stay free of Push / Candidate / Decision generation / Point / Trading.

## 10. Capability-Level Movement

- Current level: `REVIEW_ONLY_RUNTIME partial`.
- This package raises capability level: No, design only.
- Future minimal Review / Replay implementation target: `REVIEW_ONLY_RUNTIME partial` for Review / Replay result status.
- Not Production Wiring.
- Not Push.
- Not Candidate generation.
- Not Decision generation.
- Not Point generation.
- Not replay execution.
- Not Trading.

## 11. Freeze Rule Compliance

- 是否创建新骨架: No.
- 是否复用 Cursor-era 资产: Yes, `ReviewService`, `ReviewController`, `ReviewResultMapper`, `tm_review_result`, `ReviewAggregateService`, review page, and replay summary assets.
- 是否减少重复: Yes, the design fixes existing owner paths and rejects wrapper owners.
- 是否提升 capability level: No, design only.
- 是否接 service/runtime/dashboard/API: No, design only.
- 是否符合 #830 审计建议: Yes.

## 12. Final Recommendation

可以进入 `Minimal Review-Only Review / Replay Result Status Runtime Wiring Implementation Readiness Gate`。

最小实现如果未来被允许，只能复用现有 ReviewResult / ReviewService / ReviewController / ReviewAggregate / read-only replay summary owner assets，最多增加一个只读 status endpoint 和最小 dashboard status panel。禁止 replay execution、Push、Candidate、Decision generation、Point、final direction、entry/stop/TP/RR、order/execution/auto-trading、P359/P360，也不需要新 DTO / Validator / Assembler。
