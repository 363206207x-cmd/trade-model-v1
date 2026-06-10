# V1 Review / Replay Result Status Runtime Wiring Verification

## 1. Executive Summary

本包只做 `Minimal Review-Only Review / Replay Result Status Runtime Wiring Verification`，不做新功能实现。

结论：`2f98fc3 feat(review-replay): show review-only runtime status` 的最小 Review / Replay result status 只读接线验证通过。实现中的 `GET /api/dashboard/review-replay-result-status` endpoint 存在，dashboard `reviewReplayStatusPanel` 存在，状态映射完整，targeted tests 和 full tests 通过。

本次确认：

- endpoint 是 read-only status path，不触发 replay execution。
- endpoint 不生成 review result。
- endpoint 不生成 Candidate / Decision generation / Point。
- endpoint 不输出 final direction / entry / stop / TP / RR。
- endpoint 不接 Push / external channel / order / execution / auto-trading。
- dashboard panel 仅显示 Review / Replay 状态和安全边界文案。
- 未新增 DTO / Validator / Assembler / Orchestrator。
- 未改 schema / config / pom。

Capability level 不提升，仍为 `REVIEW_ONLY_RUNTIME partial`。下一允许动作：`Review / Replay Result Status Visual Verification / Closure`。

## 2. Verification Commands

| Command | Result |
|---|---|
| `bash scripts/check-workflow-contract.sh` | Passed: `WORKFLOW_CONTRACT_OK`. |
| `bash scripts/v1-state.sh` | Passed for branch context; branch is verification branch, main sync OK, Codex GitHub status unknown only. |
| `bash scripts/codex-next-task.sh` | Passed; handoff pointed to this verification package before source-of-truth refresh. |
| `bash scripts/v1-auto.sh next` | Passed; surfaced `current_head` drift from `650816c` to actual `2f98fc3`, handled in this package. |
| `./mvnw -q -DskipTests compile` | Passed. |
| `./mvnw -q -DskipTests test-compile` | Passed. |
| `./mvnw -q -Dtest=DashboardControllerTest test` | Passed. |
| `./mvnw -q test` | Passed. |
| `grep -R "review-replay-result-status\|reviewReplayStatusPanel\|REVIEW_REPLAY_\|REPLAY_EXECUTION_BLOCKED" ...` | Passed; endpoint, panel, status mapping, tests, and docs found. |
| Forbidden semantics grep over `src` and `docs` | Reviewed; hits are historical docs/tests/fixtures, forbidden-scope copy, or negative safety assertions. |
| Local HTTP smoke on port `8081` | Not completed: sandbox blocked server port binding with `SocketException: Operation not permitted`; controller/API tests cover endpoint behavior. |
| `git diff --check` | Passed after doc/source-of-truth edits. |

## 3. Endpoint Verification

| Endpoint | Method | Purpose | Trigger generation? | Trading semantics? | Result |
|---|---|---|---:|---:|---|
| `/api/dashboard/review-replay-result-status?symbol=BTCUSDT` | GET | Review / Replay result status only | No | No | Verified by `DashboardControllerTest` and source grep. |

Verified response fields from targeted tests and implementation record:

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

The endpoint is read-only: it reads `ReviewService#getStateByAnalysisId` and `ReviewAggregateService#getAggregateSummaryByAnalysisId`; it does not call review save/update or replay execution paths.

## 4. Dashboard Verification

| DOM id | Location | Shows status? | Shows review-only copy? | Shows boundary copy? | Shows forbidden action? | Result |
|---|---|---:|---:|---:|---:|---|
| `reviewReplayStatusPanel` | After `executionPlanBoundaryStatusPanel`, before the main workbench | Yes | Yes | Yes | No | Verified by template grep and `DashboardControllerTest`. |

Verified DOM ids:

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

Dashboard copy confirms review-only status, not trading signal, not Candidate, not Decision generation, not Point, not replay execution, not executable, upstream boundary still applies, and Display Slots are not a candidate pool.

## 5. Status Mapping Verification

| Status | Verified? | Fail-closed? | Source | Notes |
|---|---:|---:|---|---|
| `REVIEW_REPLAY_REVIEW_ONLY_READY` | Yes | No | `DashboardControllerTest`, `DashboardController` | Ready only when review state, aggregate, closure, replay summary metadata, and source trace are present. |
| `REVIEW_RESULT_MISSING_FAIL_CLOSED` | Yes | Yes | `DashboardControllerTest` | Missing review result does not fabricate result. |
| `REVIEW_AGGREGATE_MISSING_FAIL_CLOSED` | Yes | Yes | `DashboardControllerTest` | Missing aggregate fails closed. |
| `REPLAY_SUMMARY_MISSING_FAIL_CLOSED` | Yes | Yes | `DashboardControllerTest` | Missing replay summary metadata does not trigger replay. |
| `REVIEW_REPLAY_SOURCE_TRACE_PARTIAL` | Yes | Yes | `DashboardController`, dashboard template | Partial provenance remains display-only. |
| `REPLAY_EXECUTION_BLOCKED_FAIL_CLOSED` | Yes | Yes | `DashboardControllerTest` | Missing replay owner metadata blocks replay execution. |
| `REVIEW_REPLAY_BLOCKED_FAIL_CLOSED` | Yes | Yes | `DashboardController`, dashboard template | Ambiguous analysis context blocks escalation. |

## 6. Test Coverage Verification

Confirmed coverage:

- controller/API smoke through `DashboardControllerTest`
- dashboard template/panel existence test
- ready status mapping test
- missing ReviewResult fail-closed test
- missing ReviewAggregate fail-closed test
- missing replay summary fail-closed test
- replay execution blocked test
- forbidden-field absence test for entry / stop / TP / RR / final direction / order / execution / auto-trading style fields
- no Push / Candidate / Decision generation / Point / Trading grep classification

Full `./mvnw -q test` also passed.

## 7. Forbidden Semantics Classification

Forbidden grep over `src` and `docs` produced many hits. They are classified as:

- historical SourceTrace / BoundaryCandidate / point-package docs and tests;
- existing fixture files and guard tests that intentionally mention entry / stop / TP / RR / order / execution as blocked or non-production;
- workflow and source-of-truth forbidden-scope copy;
- this slice's negative safety flags such as `notReplayExecution`, `notTradingSignal`, `notCandidateSignal`, `notDecisionGeneration`, `notPointSignal`, and `notExecutable`;
- targeted tests asserting forbidden fields are absent from the Review / Replay status response.

No positive Review / Replay runtime output field was added for final direction, entry, stop, TP, RR, order action, execution action, auto-trading action, Push send state, Candidate generation, Decision generation, or Point generation.

## 8. Boundary Verification

- 是否接 Push：No.
- 是否接 external channel：No.
- 是否生成 Candidate：No.
- 是否生成新的 Decision：No.
- 是否生成 Point：No.
- 是否生成 final direction：No.
- 是否输出 entry/stop/TP/RR：No.
- 是否执行 replay / recheck mutation：No.
- 是否保存或生成 ReviewResult：No.
- 是否接 order/execution/auto-trading：No.
- 是否新增 DTO/Validator/Assembler：No.
- 是否改 schema/config/pom：No.
- 是否继续 P359/P360：No.
- 是否提升 capability level：No.

## 9. Source-Of-Truth Drift Check

Before this verification package, `docs/ACTIVE_MAINLINE_STATUS.yml`, `docs/CODEX_NEXT_TASK.yml`, and progress docs still listed `650816c` as the current main baseline even though merged `main` is `2f98fc3 feat(review-replay): show review-only runtime status`.

This package updates source-of-truth handoff to:

- current merged main: `2f98fc3`
- implementation status: completed on main as `2f98fc3`
- verification status: completed by this package
- next required action: `Review / Replay Result Status Visual Verification / Closure`

## 10. Capability-Level Conclusion

- Current level: `REVIEW_ONLY_RUNTIME partial`.
- PositionSync slice: `REVIEW_ONLY_RUNTIME partial`.
- Watchlist slice: `REVIEW_ONLY_RUNTIME partial`.
- MarketQuote slice: `REVIEW_ONLY_RUNTIME partial`.
- Evidence / Score slice: `REVIEW_ONLY_RUNTIME partial`.
- DecisionResult slice: `REVIEW_ONLY_RUNTIME partial`.
- ExecutionPlan / BoundaryCandidate slice: `REVIEW_ONLY_RUNTIME partial`.
- Review / Replay result status slice after `2f98fc3` plus this verification can be treated as `REVIEW_ONLY_RUNTIME partial`, pending visual closure.

This still does not equal Production Wiring, Push, Candidate generation, Decision generation, Point generation, replay execution, or Trading.

## 11. Final Recommendation

Verification passes. The next allowed action is `Review / Replay Result Status Visual Verification / Closure`.

That next package must stay A-risk and docs/visual-verification only unless explicitly authorized otherwise. It should browser-verify dashboard visibility, safety copy, no replay execution semantics, no trading/action language, and layout quality.
