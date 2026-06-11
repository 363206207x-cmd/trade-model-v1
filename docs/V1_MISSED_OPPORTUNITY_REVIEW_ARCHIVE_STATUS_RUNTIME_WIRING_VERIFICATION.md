# Minimal Review-Only Missed Opportunity / Review Archive Status Runtime Wiring Verification

## 1. Executive Summary

本包验证 `452a8ac feat(missed): show review archive review-only status` 已落地的 Missed Opportunity / Review Archive status 最小只读运行时接线。

- verification result: PASS
- endpoint: `GET /api/missed-opportunity/review-archive-status`
- dashboard panel: `missedArchiveStatusPanel`
- endpoint/dashboard behavior: verified through `MissedOpportunityControllerTest`, `DashboardControllerTest`, endpoint/status grep, and dashboard template assertions
- compile/test status: compile, test-compile, targeted tests, and full tests passed
- runtime behavior: review-only only; no missed-opportunity generation/write behavior; no review result generation; no replay/recheck execution
- overreach: no Java/test/dashboard/schema/config/pom edits in this verification package
- capability movement: none; still `REVIEW_ONLY_RUNTIME partial`
- next allowed action: `Missed Opportunity / Review Archive Status Visual Verification / Closure`

## 2. Verification Commands

| Command | Result |
|---|---|
| `bash scripts/check-workflow-contract.sh` | PASS |
| `bash scripts/v1-state.sh` | PASS for branch-local state; expected blockers are `NOT_ON_MAIN` and Codex `GH_NOT_AVAILABLE` while on verification branch |
| `bash scripts/codex-next-task.sh` | PASS; confirmed stale handoff before this package pointed to verification |
| `bash scripts/v1-auto.sh next` | PASS; confirmed stale source-of-truth baseline before this package and generated verification handoff |
| `./mvnw -q -DskipTests compile` | PASS |
| `./mvnw -q -DskipTests test-compile` | PASS |
| `./mvnw -q -Dtest=MissedOpportunityControllerTest test` | PASS |
| `./mvnw -q -Dtest=DashboardControllerTest test` | PASS |
| `./mvnw -q test` | PASS |
| `grep -RIn "review-archive-status\|missedArchiveStatusPanel\|MISSED_ARCHIVE_\|notMissedOpportunityGeneration\|notReviewResultGeneration\|notReplayExecution\|notRecheckExecution\|notExecutable" src/main/java src/main/resources src/test/java docs` | PASS; endpoint, dashboard panel, status constants, and safety fields are present |
| forbidden semantics grep over `src docs` | PASS after classification; hits are historical docs, negative guardrails, existing plan-boundary fixtures/tests, and safety assertions, not new verification-package business behavior |
| `git diff --check` | PASS |
| `git diff --cached --check` | PASS |

## 3. Endpoint Verification

| Endpoint | Method | Purpose | Trigger generation? | Trading semantics? | Result |
|---|---|---|---:|---:|---|
| `/api/missed-opportunity/review-archive-status` | GET | Review-only archive status over existing MissedOpportunity read/query/count owner path | No | No | PASS |

Verified response/safety fields:

- `status`
- `scope`
- `analysisId`
- `symbol`
- `bizDate`
- `missedArchiveAvailable`
- `missedArchiveCount`
- `latestMissedOpportunity`
- `reasonParseStatus`
- `reviewAggregateMissedAvailable`
- `sourceTraceComplete`
- `sourceHealth`
- `reason`
- `message`
- `reviewOnly=true`
- `notTradingSignal=true`
- `notCandidateSignal=true`
- `notDecisionGeneration=true`
- `notPointSignal=true`
- `notReplayExecution=true`
- `notRecheckExecution=true`
- `notMissedOpportunityGeneration=true`
- `notReviewResultGeneration=true`
- `notExecutable=true`
- `displaySlotsAreCandidatePool=false`
- `failClosed`

`MissedOpportunityControllerTest` verifies the ready path, empty fail-closed path, count-only partial path, parse-failure fail-closed path, and forbidden field absence. It also verifies the endpoint does not expose `entry`, `stop`, `TP`, `RR`, `finalDirection`, `orderAction`, `executionAction`, `autoTradingAction`, `pushSend`, `candidateRanking`, `pointSignal`, `replayExecution`, `recheckExecution`, `missedOpportunityGeneration`, or `reviewResultGeneration` as positive output fields.

## 4. Dashboard Verification

| DOM id | Location | Shows status? | Shows review-only copy? | Shows boundary copy? | Shows forbidden action? | Result |
|---|---|---:|---:|---:|---:|---|
| `missedArchiveStatusPanel` | Dashboard module status area near other review-only runtime panels | Yes | Yes | Yes | No | PASS |

Dashboard/template evidence:

- `/api/missed-opportunity/review-archive-status` is referenced by `dashboard.html`.
- `missedArchiveStatusPanel` exists.
- Status value DOM exists through `missedArchiveRuntimeStatusValue`.
- Count/detail/source health DOM exists through `missedArchiveCountValue`, `missedArchiveLatestValue`, `missedArchiveReasonParseValue`, and `missedArchiveSourceHealthValue`.
- Safety copy DOM exists through `missedArchiveReviewOnlyValue`, `missedArchiveSignalBoundaryValue`, and `missedArchiveGenerationBoundaryValue`.
- `DashboardControllerTest` verifies the panel, endpoint URL, DOM ids, status constants, and safety copy are present in the template.

This verification package did not run a live browser visual closure. Visual verification remains the next package.

## 5. Status Mapping Verification

| Status | Verified? | Fail-closed? | Source | Notes |
|---|---:|---:|---|---|
| `MISSED_ARCHIVE_REVIEW_ONLY_READY` | Yes | No for display; no downstream action | Controller status mapping and tests | Read-only archive row/count and parser OK path |
| `MISSED_ARCHIVE_COUNT_ONLY_PARTIAL` | Yes | Yes for downstream action | Controller status mapping and tests | Count exists but detail/linkage remains partial |
| `MISSED_ARCHIVE_EMPTY_FAIL_CLOSED` | Yes | Yes | Controller status mapping and tests | No scoped archive row proven |
| `MISSED_REASON_EMPTY_OR_PARSE_PARTIAL` | Yes | Yes for downstream action | Controller status mapping and dashboard constants | Reason parser input is empty/partial |
| `MISSED_REASON_PARSE_FAILED_FAIL_CLOSED` | Yes | Yes | Controller status mapping and tests | Reason parse failed |
| `MISSED_ARCHIVE_LINKAGE_PARTIAL` | Yes | Yes for downstream action | Controller status mapping and dashboard constants | Archive row exists while linkage/source trace is partial |
| `MISSED_ARCHIVE_QUERY_UNAVAILABLE_FAIL_CLOSED` | Yes | Yes | Controller exception/default mapping and dashboard constants | Read owner unavailable or cannot answer safely |
| `MISSED_ARCHIVE_BLOCKED_FAIL_CLOSED` | Yes | Yes | Dashboard/default status mapping | Reserved blocked boundary; implementation avoids forbidden calls instead of triggering them |

## 6. Test Coverage Verification

- controller/API smoke test: PASS via `MissedOpportunityControllerTest`
- status mapping test: PASS via `MissedOpportunityControllerTest`
- missing archive fail-closed test: PASS
- count-only partial test: PASS
- parse-failure fail-closed test: PASS
- forbidden positive field absence test: PASS
- dashboard template/model existence test: PASS via `DashboardControllerTest`
- no Push/Candidate/Decision generation/Point/trading grep check: PASS after classification

Full `./mvnw -q test` also passed. Existing historical tests may log DecisionEngine generation paths, but this verification package did not add or trigger new production generation behavior.

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
| 是否触发 replay execution | No |
| 是否触发 recheck execution | No |
| 是否生成 missed-opportunity / review archive write behavior | No |
| 是否生成 review result | No |
| 是否新增 DTO/Validator/Assembler | No |
| 是否改 schema/config/pom | No |
| 是否继续 P359/P360 | No |
| 是否提升 capability level | No |

Forbidden semantics grep produced expected repository-wide hits from historical docs, existing plan-boundary fixtures/tests, and negative safety assertions. This verification package adds only docs/source-of-truth content and does not introduce positive trading, candidate, point, push, replay/recheck, missed-opportunity generation, or review-result generation semantics.

## 8. Final Recommendation

Verification 通过，可以进入 `Missed Opportunity / Review Archive Status Visual Verification / Closure`。当前仍是 `REVIEW_ONLY_RUNTIME partial`：它验证了只读 endpoint、dashboard panel、安全字段、fail-closed 状态和禁止语义边界，但不等于 Production Wiring，不等于 Push，不等于 Candidate generation，不等于 Decision generation，不等于 Point generation，不等于 replay/recheck execution，不等于 missed-opportunity generation，不等于 review result generation，也不等于 Trading。
