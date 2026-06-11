# V1 Minimal Review-Only Missed Opportunity / Review Archive Status Runtime Wiring Implementation

## 1. Executive Summary

本包实现 `Missed Opportunity / Review Archive status` 的最小 review-only runtime status。实现只复用既有 `MissedOpportunityController`、`MissedOpportunityService` 读路径、`MissedOpportunityDO`、`MissedReasonViewParser`、`tm_missed_opportunity` owner path，以及 dashboard 既有状态面板模式。

本包新增一个只读 status endpoint、一个 dashboard 最小状态面板、定向 controller/dashboard 测试、实现记录和 source-of-truth handoff。它不触发 missed-opportunity generation/write，不生成 review result，不触发 replay/recheck execution，不接 Push、Candidate generation、Decision generation、Point、order/execution/auto-trading，也不新增 DTO / Validator / Assembler。

当前 capability level 仍为 `REVIEW_ONLY_RUNTIME partial`。Missed Opportunity / Review Archive 需要后续 verification 和 visual closure 后，才能计入完整 review-only runtime partial 小闭环。

## 2. Implemented Endpoint / Path

| Area | Implemented path | Behavior |
|---|---|---|
| API | `GET /api/missed-opportunity/review-archive-status` | Read-only `Map<String,Object>` status projection. |
| Controller owner | `MissedOpportunityController` | Reuses existing missed archive controller owner; no wrapper owner added. |
| Read owner | `MissedOpportunityService.countByBizDate`, `query`, `findByMissedId` | Reads count/detail only; no write/generation call. |
| Parser | `MissedReasonViewParser.parse` | Converts existing `reasonJson` into parser health status. |
| Dashboard | `missedArchiveStatusPanel` in `dashboard.html` | Shows review-only archive status, counts, latest row, reason parse/source health, and safety copy. |

The minimal endpoint does not call `ReviewAggregateService` and does not claim aggregate linkage as proven. `reviewAggregateMissedAvailable` remains false unless a later verification/design package explicitly adds a safe read proof.

## 3. Status Mapping

| Status | Implementation source | Fail-closed? | Notes |
|---|---|---:|---|
| `MISSED_ARCHIVE_REVIEW_ONLY_READY` | Count/query row exists, parser `OK`, linkage/trace present | No for display | Manual review only; no downstream action. |
| `MISSED_ARCHIVE_COUNT_ONLY_PARTIAL` | Count exists but scoped detail row is unavailable | Yes | Count signal only; detail/archive status remains partial. |
| `MISSED_ARCHIVE_EMPTY_FAIL_CLOSED` | No scoped archive row is proven | Yes | Empty status; no archive conclusion is generated. |
| `MISSED_REASON_EMPTY_OR_PARSE_PARTIAL` | Row exists but `reasonJson` is empty | Yes for downstream action | Displays partial source health only. |
| `MISSED_REASON_PARSE_FAILED_FAIL_CLOSED` | `MissedReasonViewParser` returns `PARSE_FAILED` | Yes | Blocks any downstream interpretation. |
| `MISSED_ARCHIVE_LINKAGE_PARTIAL` | Row exists but `analysisId` / trace linkage is partial | Yes for downstream action | Shows archive presence only. |
| `MISSED_ARCHIVE_QUERY_UNAVAILABLE_FAIL_CLOSED` | Read owner throws or cannot answer safely | Yes | Endpoint returns fail-closed status. |
| `MISSED_ARCHIVE_BLOCKED_FAIL_CLOSED` | Forbidden-call boundary | Yes | Reserved in dashboard/default mapping; implementation avoids the blocked calls instead of triggering them. |

## 4. Safety Fields

The endpoint always returns:

- `reviewOnly = true`
- `notTradingSignal = true`
- `notCandidateSignal = true`
- `notDecisionGeneration = true`
- `notPointSignal = true`
- `notReplayExecution = true`
- `notRecheckExecution = true`
- `notMissedOpportunityGeneration = true`
- `notReviewResultGeneration = true`
- `notExecutable = true`
- `displaySlotsAreCandidatePool = false`
- `failClosed` according to status

The endpoint does not expose candidate ranking, final direction, entry, stop, TP, RR, position size, leverage, order action, execution action, Push send state, replay/recheck execution action, missed-opportunity generation action, review-result generation action, or auto-trading action.

## 5. Dashboard Panel

Dashboard panel DOM id: `missedArchiveStatusPanel`.

The panel displays:

- `missedArchiveRuntimeStatusValue`
- `missedArchiveScopeValue`
- `missedArchiveCountValue`
- `missedArchiveLatestValue`
- `missedArchiveReasonParseValue`
- `missedArchiveSourceHealthValue`
- `missedArchiveReviewOnlyValue`
- `missedArchiveSignalBoundaryValue`
- `missedArchiveGenerationBoundaryValue`
- `missedArchiveUpstreamValue`
- `missedArchiveReasonValue`

The visible copy states that Missed Opportunity / Review Archive is review-only, not a trading signal, not Candidate, not new Decision generation, not Point, not executable, does not trigger missed-opportunity generation/write, does not generate review results, and does not trigger replay/recheck execution.

## 6. Tests

Targeted coverage added:

- `MissedOpportunityControllerTest`
  - ready status and safety fields
  - empty archive fail-closed
  - count-only partial
  - reason parse failure fail-closed
  - forbidden executable/trading/candidate/point fields absent
- `DashboardControllerTest`
  - dashboard template contains endpoint, panel DOM ids, status constants, and safety copy

## 7. Forbidden Scope Confirmation

- No DTO / Validator / Assembler / Orchestrator added.
- No schema/config/pom changes.
- No external API refresh, scheduler, collector, or API-client trigger.
- No Push external channel.
- No Candidate generation.
- No Decision generation.
- No Point generation.
- No final direction, entry, stop, TP, RR, position size, leverage, order action, execution action, or auto-trading action.
- No replay/recheck execution.
- No missed-opportunity generation/write behavior.
- No review result generation.
- No P359 / P360 continuation.

## 8. Next Allowed Action

Next allowed action: `Minimal Review-Only Missed Opportunity / Review Archive Status Runtime Wiring Verification`.

Risk: A for verification docs/source-of-truth only. The implementation PR itself remains B-risk and must not be auto-merged without review.
