# Fundamental AI v4.1 Knife B Implementation Report

## Scope

Knife B closes Desktop cross-page data ownership and interaction gaps on `codex/frontend-interaction-runtime-closure`. PR #1195 remains Draft and unmerged. Home Top3, Position/Plan separation, AI safety boundaries, login, Telegram, schema and Mobile remain unchanged.

## Contract Mapping

| Package | Owner/source | Implemented boundary | Status |
|---|---|---|---|
| KB-01 | `UserPosition` + each position's latest `PositionMonitorLog` | Owner-scoped full list/detail projection; monitor facts only under verified/fresh trust | VALIDATED_PENDING_EXACT_HEAD_CI |
| KB-02 | Existing closed UserPosition mapper queries | Separate Active and History tabs; Review links require formal `reviewId` | VALIDATED_PENDING_EXACT_HEAD_CI |
| KB-03 | `AnalysisRun.analysisMode` + structured Three-AI payload | Preview/Opportunity gates, unknown-mode fail closed, structured role IA, formal conflict visibility | VALIDATED_PENDING_EXACT_HEAD_CI |
| KB-04 | `Message` → push snapshot identity → `TmPushSnapshotDO.pushId` | Owner-scoped `PUSH_OPEN`, read-only GET, ERROR-only retry, separate AnalysisRun control | VALIDATED_PENDING_EXACT_HEAD_CI |
| KB-05 | Validated Final + Analysis + existing `opened_at` | Plan-context prefill and required user-provided UTC `openedAt` preservation | VALIDATED_PENDING_EXACT_HEAD_CI |
| KB-06 | UserPosition close persistence audit | Full close is retained; no auditable partial-close event/quantity producer exists | BLOCKED_BY_MISSING_PERSISTENCE_SOURCE |
| KB-07 | Internal allowlisted URL context | Messages/Recheck/Plan, Positions/detail and Home/Analysis/Audit return contexts | VALIDATED_PENDING_EXACT_HEAD_CI |
| KB-08 | Existing controllers/templates/references | Exact legacy route matrix; no redirect or retirement mutation | VALIDATED_PENDING_EXACT_HEAD_CI |

## KB-06 Source Audit

Repository search found lifecycle support for `PARTIALLY_CLOSED`, but no existing persisted partial-close event with close quantity, close time, close price and result note. The current UserPosition row only persists the full-close facts (`close_price`, `closed_at`, `close_reason`) and the full-close mapper transition. Reducing `quantity` would erase event history and falsely claim O07 support, so no partial-close mutation was added.

`O07_PARTIAL_CLOSE = BLOCKED_BY_MISSING_PERSISTENCE_SOURCE`

## Safety Proof

- `PUSH_OPEN` has no dispatch batch ID, dispatch instruction ID, max attempts or scheduler backoff.
- `requireInternalScheduledExecution` remains dedicated to real `SCHEDULED` commands.
- `evaluateUserRequest` and the old raw pushId APIs remain denied.
- Browser `currentPrice` is never accepted; the engine resolves price through `MarketPriceSnapshotService`.
- Re-analysis uses `AnalysisRunOrchestrator` and never mutates the immutable push snapshot or existing Recheck row.
- No Plan creates a Position without the explicit O06 user form submission.
- No automatic open, close, reverse, order or broker capability was added.

## Validation

- Java 17 full Maven before handoff commit: 4,737 tests, 0 failures, 0 errors, 14 skipped because Docker/Testcontainers was unavailable.
- Knife B focused tests after the final owner assertion: PASS.
- Product Source Gate: PASS.
- JavaScript syntax (`workspace.js`, `home-runtime.js`): PASS.
- `git diff --check`: PASS.
- Normal runtime: Java 17 standard release JAR, login PASS, `/dashboard` HTTP 200.
- UI-review runtime: Java 17 standard release JAR, login PASS, `/dashboard` HTTP 200.
- Browser at 1,440 and 1,080 widths: document horizontal overflow 0, user-visible text clipping 0, console error count 0.
- Workflow Contract and exact-Head CI remain pending until the application diff is committed and the worktree is clean. The authorization validator correctly rejects an uncommitted application diff.

All visual fixture captures are labelled `FIXTURE / UI-REVIEW` in `docs/evidence/knife_b/README.md`; none is represented as production or live-provider evidence.

## Phase Status

- `CURRENT_PHASE_DONE = NO`
- `GLOBAL_SEMANTIC_RUNTIME_DONE = NO`
- `LIVE_RUNTIME_ACCEPTANCE_DONE = NO`
- PR #1195 remains Draft and unmerged.
