# V1 Review / Replay Result Status Runtime Wiring Implementation

## 1. Executive Summary

本包实现 Minimal Review-Only Review / Replay Result Status Runtime Wiring（复盘 / 回放结果状态最小只读运行时接线实现）。

实现范围仅包括一个最小只读状态 endpoint、dashboard status panel、targeted `DashboardControllerTest` 覆盖和 source-of-truth 更新。该 endpoint 只读取既有 Review / Replay owner path，不触发 replay execution（回放执行），不重新计算 replay，不生成 review result（复盘结果），也不生成交易信号。

当前 capability level 仍为 `REVIEW_ONLY_RUNTIME partial`。本包不等于 Production Wiring，不等于 Push，不等于 Candidate generation，不等于 Decision generation，不等于 Point generation，不等于 Trading。

## 2. Reused Existing Assets

| Asset | Reuse | Notes |
|---|---|---|
| `ReviewService#getStateByAnalysisId` | Yes | 只读读取 `tm_review_result` 的已存在 review state，不调用 `saveOrUpdate`。 |
| `ReviewAggregateService#getAggregateSummaryByAnalysisId` | Yes | 只读读取 aggregate summary / detail-section metadata，不调用 replay execution。 |
| `ReviewController` / review page owner path | Preserved | 不修改 ReviewController，不接 `POST /api/review/save`。 |
| `ReviewResultMapper` / `tm_review_result` | Indirect read | 通过 `ReviewService` 读取，不改 schema。 |
| review aggregate / replay summary assets | Read-only | 使用 aggregate summary 的 detail-section metadata 判断 replay summary 是否存在。 |
| Dashboard owner path | Yes | 在 `DashboardController` 增加最小 status endpoint 和 dashboard DOM。 |

## 3. Implemented Endpoint / Path

| Endpoint | Method | Purpose | Read-only? | Triggers replay? | Generates review result? |
|---|---|---|---:|---:|---:|
| `/api/dashboard/review-replay-result-status` | GET | Review / Replay result status only | Yes | No | No |

Supported query inputs:

- `analysisId`
- `symbol` as a dashboard convenience path; if `analysisId` is missing, it resolves the latest DecisionResult analysis id for the selected symbol and then reads Review / Replay state.

The endpoint returns only review-only status fields:

- `status`
- `symbol`
- `analysisId`
- `reviewResultAvailable`
- `reviewAggregateAvailable`
- `reviewClosureAvailable`
- `replaySummaryAvailable`
- `replaySummaryCount`
- `replaySummaryRecommendedLimit`
- `reviewUpdatedAt`
- `reviewErrorType`
- `sourceTraceComplete`
- `sourceHealth`
- `reason`
- `message`
- `reviewOnly = true`
- `notTradingSignal = true`
- `notCandidateSignal = true`
- `notDecisionGeneration = true`
- `notPointSignal = true`
- `notReplayExecution = true`
- `notExecutable = true`
- `watchlistBounded = true`
- `marketQuoteChecked = true`
- `evidenceScoreChecked = true`
- `decisionResultChecked = true`
- `executionPlanBoundaryChecked = true`
- `displaySlotsAreCandidatePool = false`
- `failClosed`

## 4. Status Mapping

| Status | Trigger | Fail-closed? | Notes |
|---|---|---:|---|
| `REVIEW_REPLAY_REVIEW_ONLY_READY` | review result, aggregate summary, review closure, replay summary metadata, and source trace are present | No | Read-only status only. |
| `REVIEW_RESULT_MISSING_FAIL_CLOSED` | `ReviewService#getStateByAnalysisId` returns missing | Yes | Does not fabricate review result. |
| `REVIEW_AGGREGATE_MISSING_FAIL_CLOSED` | `ReviewAggregateService#getAggregateSummaryByAnalysisId` returns empty | Yes | Does not fabricate aggregate. |
| `REPLAY_SUMMARY_MISSING_FAIL_CLOSED` | replay summary metadata exists but has no rows | Yes | Does not trigger replay execution. |
| `REVIEW_REPLAY_SOURCE_TRACE_PARTIAL` | review/aggregate exists but source trace or review closure is incomplete | Yes | Display-only partial state. |
| `REPLAY_EXECUTION_BLOCKED_FAIL_CLOSED` | replay summary owner path metadata is absent | Yes | Explicitly blocks replay execution. |
| `REVIEW_REPLAY_BLOCKED_FAIL_CLOSED` | analysis context is absent | Yes | Blocks status escalation. |

## 5. Dashboard Panel

Added dashboard panel:

- DOM id: `reviewReplayStatusPanel`
- Location: after `executionPlanBoundaryStatusPanel`, before the main workbench

Key DOM ids:

- `reviewReplayRuntimeStatusValue`
- `reviewReplaySymbolValue`
- `reviewReplayAnalysisIdValue`
- `reviewReplayResultValue`
- `reviewReplayAggregateValue`
- `reviewReplaySummaryValue`
- `reviewReplaySourceTraceValue`
- `reviewReplaySourceHealthValue`
- `reviewReplayReviewOnlyValue`
- `reviewReplaySignalBoundaryValue`
- `reviewReplaySafetyBoundaryValue`
- `reviewReplayUpstreamValue`
- `reviewReplayReasonValue`

Displayed safety copy confirms:

- Review / Replay is review-only.
- It is not a trading signal.
- It is not Candidate / Decision generation / Point.
- It does not trigger replay execution.
- It does not recalculate replay.
- It does not generate review result.
- Upstream Watchlist / MarketQuote / Evidence-Score / DecisionResult / ExecutionPlan-BoundaryCandidate boundaries still apply.
- Display Slots are not a candidate pool.

## 6. Targeted Test Coverage

Updated `DashboardControllerTest` covers:

- dashboard template contains the Review / Replay status endpoint, panel DOM ids, status mapping, and safety copy;
- endpoint returns `REVIEW_REPLAY_REVIEW_ONLY_READY`;
- endpoint returns `reviewOnly=true`;
- endpoint returns `notTradingSignal=true`;
- endpoint returns `notCandidateSignal=true`;
- endpoint returns `notDecisionGeneration=true`;
- endpoint returns `notPointSignal=true`;
- endpoint returns `notReplayExecution=true`;
- endpoint returns `notExecutable=true`;
- missing review result fails closed;
- missing aggregate fails closed;
- missing replay summary fails closed;
- missing replay summary owner path blocks replay execution;
- endpoint does not expose entry / stop / TP / RR / final direction / order / execution / auto-trading positive fields.

## 7. Forbidden Semantics

This package does not:

- trigger replay execution;
- recalculate replay;
- generate review result;
- call review save/update;
- connect Push or external channel;
- generate Candidate;
- generate new Decision;
- generate Point;
- output final direction;
- output entry / stop / TP / RR;
- output position size or leverage;
- connect order / execution / auto-trading;
- add DTO / Validator / Assembler / Orchestrator;
- modify schema / config / pom;
- continue P359 / P360.

Any forbidden terms present in tests or documentation are negative guardrail checks or explicit forbidden-scope copy, not positive runtime behavior.

## 8. Capability-Level Conclusion

- Current level: `REVIEW_ONLY_RUNTIME partial`
- This package: minimal implementation for Review / Replay result status slice
- Future target after verification: Review / Replay result status can be marked `REVIEW_ONLY_RUNTIME partial`
- Still not Production Wiring
- Still not Push
- Still not Candidate generation
- Still not Decision generation
- Still not Point generation
- Still not Trading

## 9. Next Step

Next allowed action: `Minimal Review-Only Review / Replay Result Status Runtime Wiring Verification`.

The next package must verify compile/tests, API/dashboard behavior, forbidden semantics, and source-of-truth alignment. It must not implement additional functionality.
