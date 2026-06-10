# V1 Review / Replay Result Status Runtime Wiring Implementation Readiness Gate

## 1. Executive Summary

结论：**GO**，可以进入 `Minimal Review-Only Review / Replay Result Status Runtime Wiring Implementation`。

为什么 GO：#873 source read 和 #874 design 已确认现有 `ReviewResult` / `ReviewService` / `ReviewController` / `ReviewResultMapper` / `tm_review_result` / `ReviewAggregateService` / review page / replay summary owner assets 存在；其中 GET read path 可复用，missing / partial / blocked 状态可以 fail closed。dedicated review-only endpoint / dashboard panel 仍缺失，但可以用最小 Map / existing VO surface 落地，不需要新 DTO / Validator / Assembler。

最小 implementation 允许做什么：新增或复用一个只读 Review / Replay status endpoint；新增或复用一个最小 dashboard status panel；显示 review result、review state、aggregate、replay summary、source trace/source health、fail-closed、review-only 和 not-trading / not-candidate / not-decision-generation / not-point / not-replay-execution 边界；补 targeted tests；更新 source-of-truth。

最小 implementation 禁止做什么：不得调用 `POST /api/review/save`，不得调用 `POST /api/push/recheck/replay`，不得调用 `PushRecheckService#replayByDispatch`，不得 replay execution / recheck mutation，不得接 Push / external channel / Candidate / Decision generation / Point / final direction / entry / stop / TP / RR / order / execution / auto-trading，不得新增 DTO / Validator / Assembler / Orchestrator，不得改 schema/config/pom，不得继续 P359 / P360。

当前 capability level 不提升：本包是 readiness gate only，仍为 `REVIEW_ONLY_RUNTIME partial`。下一允许动作是 `Minimal Review-Only Review / Replay Result Status Runtime Wiring Implementation`。

## 2. Readiness Gate Result

Decision: **A. GO: Minimal Review-Only Review / Replay Result Status Runtime Wiring Implementation**.

下一步只能是最小 implementation，且仍必须保持 review-only：

- 必须复用 existing `ReviewResult` / `ReviewService` / `ReviewController` GET read paths / `ReviewResultMapper` / `tm_review_result` / `ReviewAggregateService` / review page / replay summary read assets。
- 不得新增 DTO / Validator / Assembler。
- 不得改 schema/config/pom。
- 不得触发 review save、replay execution、recheck mutation。
- 不得接 Push / Candidate / Decision generation / Point / Trading。

No-Go 条件未触发：当前设计不要求新 schema、不要求新 DTO、不要求 replay execution、不要求 Push / Candidate / Decision generation / Point / Trading，也不需要绕过已完成的 upstream review-only slices。

## 3. Required Future Implementation Boundary

未来 implementation 只允许：

- 新增或复用一个最小 Review / Replay review-only status endpoint，例如 `GET /api/dashboard/review-replay-status?analysisId=...`。
- 新增或复用一个最小 dashboard status panel。
- 显示 `status` / `analysisId` / `symbol` if available / `reviewResultAvailable` / `reviewStateAvailable` / `reviewAggregateAvailable` / `reviewClosureAvailable` / `replaySummaryAvailable` / `replaySummary` / `sourceTraceComplete` / `sourceHealth` / `failClosed`。
- 明确 `reviewOnly=true`。
- 明确 `notTradingSignal=true`。
- 明确 `notCandidateSignal=true`。
- 明确 `notDecisionGeneration=true`。
- 明确 `notPointSignal=true`。
- 明确 `notReplayExecution=true`。
- 明确 `watchlistBounded=true`。
- 明确 `marketQuoteChecked=true`。
- 明确 `evidenceScoreChecked=true`。
- 明确 `decisionResultChecked=true`。
- 明确 `executionPlanBoundaryChecked=true`。
- 明确 `displaySlotsAreCandidatePool=false`。
- 补 controller/API smoke、dashboard panel、status mapping、missing/fail-closed、replay execution blocked、forbidden semantics targeted tests。

未来 implementation 不允许：

- 保存或修改 ReviewResult。
- 执行 replay / recheck mutation。
- 生成 Candidate。
- 生成新的 Decision。
- 生成 Point。
- 生成 final direction。
- 输出 entry / stop / TP / RR。
- 输出 position size。
- 输出 leverage。
- 接 Push external channel。
- 接 order / execution / auto-trading。
- 新增 DTO / Validator / Assembler / Orchestrator。
- 修改 schema/config/pom。
- 继续 P359/P360。

## 4. Status Mapping Readiness

| Status | Can use existing assets? | Data source | Gap | Implementation allowed? | Fail-closed? |
|---|---:|---|---|---:|---:|
| `REVIEW_REPLAY_REVIEW_ONLY_READY` | Yes | `ReviewService#getStateByAnalysisId`, `ReviewAggregateService`, ReviewController GET read paths, optional replay summary read | Needs minimal status mapping and dashboard copy | Yes | No |
| `REVIEW_RESULT_MISSING_FAIL_CLOSED` | Yes | Missing/null review state or no `tm_review_result` read result | Needs explicit status field | Yes | Yes |
| `REVIEW_AGGREGATE_MISSING_FAIL_CLOSED` | Yes | `ReviewAggregateService#getAggregate...` returns empty/missing aggregate | Needs explicit status field | Yes | Yes |
| `REPLAY_SUMMARY_MISSING_FAIL_CLOSED` | Partial | Read-only replay summary assets only if isolated from replay execution | Must not call replay execution to fill gap | Yes, read summary only | Yes |
| `REVIEW_REPLAY_SOURCE_TRACE_PARTIAL` | Partial | Review aggregate, rule logs, upstream detail/provenance fields | Source trace completeness needs conservative mapping | Yes | Yes |
| `REPLAY_EXECUTION_BLOCKED_FAIL_CLOSED` | Yes | Detection that only replay execution would answer the status | Needs explicit blocked reason | Yes | Yes |
| `REVIEW_REPLAY_BLOCKED_FAIL_CLOSED` | Yes | Unsupported input, ambiguous analysis id, owner path unavailable, safety boundary unclear | Needs catch-all blocked mapping | Yes | Yes |

## 5. Existing Asset Readiness

| Asset | Exists? | Reusable? | Needs new DTO? | Needs schema change? | Risk | Decision |
|---|---:|---:|---:|---:|---|---|
| `ReviewResult` / `tm_review_result` | Yes | Yes | No | No | Read may be absent | Reuse with fail-closed missing mapping |
| `ReviewService#getStateByAnalysisId` | Yes | Yes | No | No | Must avoid `saveOrUpdate` write path | Reuse read-only method only |
| `ReviewController` GET read endpoints | Yes | Yes | No | No | Must avoid `POST /api/review/save` | Reuse or add minimal read-only status endpoint |
| `ReviewResultMapper` | Yes | Yes | No | No | Read/write mapper exists | Use read-only access only |
| `ReviewAggregateService` | Yes | Yes | No | No | Aggregate may be empty | Reuse as status source; empty means fail closed |
| Review page / dashboard assets | Partial | Yes | No | No | Dedicated dashboard panel missing | Allow minimal status/copy/DOM only |
| Replay summary read assets | Partial | Yes | No | No | Must be isolated from replay execution | Reuse summary only if safe |
| Replay execution assets | Yes | No | No | No | Side-effectful mutation | Explicitly blocked |
| Tests | Yes | Yes | No | No | Dedicated status tests missing | Add targeted tests only in implementation |
| Source trace / provenance | Partial | Yes | No | No | May be incomplete | Map partial as fail-closed |
| Fail-closed flags | Partial | Yes | No | No | Dedicated fields missing | Add minimal mapping only |
| Review-only flags | Partial | Yes | No | No | Dedicated fields missing | Add explicit flags only |

## 6. Test Readiness

Future implementation must include the minimum test scope:

- Controller/API smoke test for the Review / Replay status endpoint.
- Dashboard template/model/panel existence test.
- Status mapping test for ready / missing / partial / blocked states.
- Missing ReviewResult fail-closed test.
- Missing ReviewAggregate fail-closed test.
- Replay summary missing / unavailable fail-closed test.
- Replay execution blocked test or forbidden semantics assertion.
- Forbidden fields test: no candidate ranking, generated decision, executable final direction, entry / stop / TP / RR, order action, Push send state, replay execution action, or rule auto-correction action.
- No DTO / Validator / Assembler check.
- No Push / Candidate / Decision generation / Point / Trading grep or targeted test.

## 7. Boundary With Existing Completed Slices

Future implementation must not bypass:

- Watchlist / RuleConfig boundary.
- MarketQuote freshness/fallback status.
- Evidence / Score review-only status.
- DecisionResult review-only status.
- ExecutionPlan / BoundaryCandidate review-only status.
- Display Slots boundary.
- Source trace / source health.
- Fail-closed ambiguity rule.

Review / Replay status is a visibility layer over existing review/replay read assets. It cannot become a new source of Candidate, Decision generation, Point, replay execution, Push, or trading intent.

## 8. Explicit No-Overreach Confirmation

- 是否接 Push：No.
- 是否接 external channel：No.
- 是否生成 Candidate：No.
- 是否生成新的 Decision：No.
- 是否生成 Point：No.
- 是否生成 final direction：No.
- 是否输出 entry/stop/TP/RR：No.
- 是否执行 replay / recheck mutation：No.
- 是否保存 ReviewResult：No.
- 是否接 order/execution/auto-trading：No.
- 是否继续 P359/P360：No.
- 是否新增 DTO/Validator/Assembler：No.
- 是否改 schema/config/pom：No.
- 是否提升 capability level：No, readiness only.

## 9. Freeze Rule Compliance

- 是否创建新骨架: No.
- 是否复用 Cursor-era 资产: Yes, `ReviewService`, `ReviewController`, `ReviewResultMapper`, `tm_review_result`, `ReviewAggregateService`, review page, and replay summary assets.
- 是否减少重复: Yes, readiness confirms implementation must reuse existing owners and reject wrapper owners.
- 是否提升 capability level: No, readiness gate only.
- 是否接 service/runtime/dashboard/API: No, readiness only.
- 是否符合 #830 审计建议: Yes.

## 10. Final Recommendation

明确结论：**GO**。

下一允许动作：`Minimal Review-Only Review / Replay Result Status Runtime Wiring Implementation`。

下一步 implementation 的最小边界：复用 existing ReviewResult / ReviewService / ReviewController GET read paths / ReviewAggregate / read-only replay summary owner assets，最多新增一个只读 status endpoint、一个最小 dashboard status panel、targeted tests 和 source-of-truth updates。

合并前必须满足：workflow contract、compile、test-compile、targeted/full tests as required、forbidden semantics grep、diff checks、forbidden path checks。

为什么仍是 `REVIEW_ONLY_RUNTIME partial`：本包只确认能否进入最小只读实现；未来实现也只会展示非执行、非交易、非生成状态，不等于 Production Wiring、Push、Candidate generation、Decision generation、Point generation、replay execution 或 Trading。
