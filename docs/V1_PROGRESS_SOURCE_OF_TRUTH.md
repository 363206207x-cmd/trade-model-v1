# V1 Progress Source Of Truth

This document defines how Trade Model V1 progress is determined. New windows must start from `docs/SESSION_BOOTSTRAP.md`.

Default workflow is GPT + Codex + GitHub-native. Terminal scripts are fixed fallback helpers, except local main sync after merge.

## Current State Rules

Only merged `main` counts as completed project state.

Current state must be read from:

1. `docs/ACTIVE_MAINLINE_STATUS.yml`
2. `bash scripts/v1-state.sh`
3. merged `main`
4. `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`
5. this file and the other source-of-truth docs

If chat memory, branch names, local docs, PR state, and command output conflict, merged `main` plus `scripts/v1-state.sh` wins.

Codex shell `GH_NOT_AVAILABLE` means Codex GitHub status unknown. It is not, by itself, proof that project state failed. GPT connector evidence or the user's local terminal `gh` evidence may be used as handoff evidence when it explicitly confirms open PR none, main sync, and clean worktree.

The following do not count as completed:

- open Issue
- open PR
- Draft PR
- approved PR that is not merged
- CI green PR that is not merged
- local branch or remote branch
- unsynced main after merge
- dirty worktree
- unmerged commit
- chat memory
- Codex output
- docs-only authorization

## Current Active Block

- Current merged main: `791260f docs(review-replay): verify review-only runtime wiring`
- Current active block: `Review / Replay Result Status Visual Verification / Closure`
- Current level: `REVIEW_ONLY_RUNTIME partial`
- Capability movement from this pack: verification only; overall level remains `REVIEW_ONLY_RUNTIME partial`, not Production Wiring
- Next required action: `Review / Replay Result Status Visual Verification / Closure`
- #876 is completed and synced on main by user terminal handoff evidence.
- #877 is completed and synced on main; workflow drift repair is now history, not the active package.
- DecisionResult runtime wiring verification is completed on main as `a0a432b`.
- V1 Auto Operator Pack is completed on main as `b30c30e`; it adds the Chinese workflow operator script and does not change business capability.
- DecisionResult Visual Verification / Closure is completed on main as `baa5cfe`; it browser-verifies the #876 / `a0a432b` DecisionResult dashboard status panel and read-only safety copy.
- V1 Auto Operator Post-Merge State Refresh is completed on main as `1b12cd5`; it refreshes the Chinese auto-operator summary and next-task handoff after DecisionResult visual closure without changing business capability.
- `c75919c` completed Next Minimal Runtime Slice Selection After DecisionResult Closure and selected `ExecutionPlan / BoundaryCandidate review-only display continuation` as the next source-read target.
- `8f404cd` completed Source Read for ExecutionPlan / BoundaryCandidate review-only display continuation and confirmed the existing owner path, dashboard detail/display adapters, dashboard DOM slot, schema/mapper, and targeted tests before design.
- `b3e6d71` completed Minimal Review-Only ExecutionPlan / BoundaryCandidate Runtime Wiring Design and fixed future owner path, status mapping, dashboard/API surface, fail-closed rules, completed-slice boundaries, and readiness checklist without implementation.
- `a84a4aa` completed Minimal Review-Only ExecutionPlan / BoundaryCandidate Runtime Wiring Implementation Readiness Gate and returned GO for minimal implementation.
- `60e034a` completed Minimal Review-Only ExecutionPlan / BoundaryCandidate Runtime Wiring Implementation on main with one read-only status endpoint, dashboard panel, targeted tests, and source-of-truth updates.
- `85fb7ad` completed the post-implementation state refresh and points the handoff to this verification package without changing business capability.
- `4a278b0` completed Minimal Review-Only ExecutionPlan / BoundaryCandidate Runtime Wiring Verification and confirmed the endpoint/dashboard review-only behavior, tests, forbidden semantics, and source-of-truth alignment.
- `caa45c9` completed the post-verification state refresh and pointed the handoff to visual closure.
- `d907719` completed ExecutionPlan / BoundaryCandidate Visual Verification / Closure on main: browser verification confirmed the panel is visible, review-only and not executable copy is present, entry / stop / TP / RR are absent from the panel, trading/candidate/point signal language appears only as negative guardrail copy, and no sibling layout overlap was found.
- ExecutionPlan / BoundaryCandidate is now the sixth completed Review-Only Runtime partial slice, not an in-progress module.
- `86b3ff3` completed the A-risk next minimal runtime slice selection after ExecutionPlan / BoundaryCandidate closure and selected `Review / Replay result status` for the next source-read-only package.
- `fb0263e` completed Source Read for Review / Replay result status: it confirms the existing ReviewResult / ReviewService / ReviewController / ReviewResultMapper / `tm_review_result` / ReviewAggregate / review page / replay summary owner assets, while dedicated review-only status endpoint and dashboard panel remain missing and replay execution must be excluded.
- `4d17081` completed Minimal Review-Only Review / Replay Result Status Runtime Wiring Design. It fixes owner path, status mapping, dashboard/API surface, replay execution boundary, fail-closed rules, completed-slice boundaries, and readiness checklist without implementation.
- `650816c` completed Minimal Review-Only Review / Replay Result Status Runtime Wiring Implementation Readiness Gate and returned GO for minimal implementation.
- `2f98fc3` completed Minimal Review-Only Review / Replay Result Status Runtime Wiring Implementation. It adds one minimal read-only `/api/dashboard/review-replay-result-status` endpoint, a dashboard Review / Replay result status panel, targeted `DashboardControllerTest` coverage, and source-of-truth updates without replay execution, review result generation, DTO/Validator/Assembler, schema/config/pom, Push, Candidate, Decision generation, Point, or trading.
- `791260f` completes Minimal Review-Only Review / Replay Result Status Runtime Wiring Verification. It verifies workflow contract, compile, test-compile, targeted `DashboardControllerTest`, full tests, endpoint/panel/status mapping, forbidden semantics classification, and source-of-truth drift from `650816c` to `2f98fc3`.
- This workflow improvement package adds one-command Codex runner and PR completion helpers only; it does not complete Review / Replay visual closure and does not change business capability.
- Current active package after this workflow improvement remains `Review / Replay Result Status Visual Verification / Closure`. It may add only visual verification docs and source-of-truth updates.

## Runtime Slice History

Completed review-only runtime slices:

1. `PositionSync + Dashboard review-only status`: `REVIEW_ONLY_RUNTIME partial`
2. `Watchlist + RuleConfig + Dashboard/API review-only status`: `REVIEW_ONLY_RUNTIME partial`
3. `MarketQuote freshness / fallback / dashboard API status`: `REVIEW_ONLY_RUNTIME partial`
4. `Evidence / Score review-only runtime status`: `REVIEW_ONLY_RUNTIME partial`
5. `DecisionResult review-only dashboard/API status`: `REVIEW_ONLY_RUNTIME partial`
6. `ExecutionPlan / BoundaryCandidate review-only runtime status`: `REVIEW_ONLY_RUNTIME partial`

DecisionResult chain history:

- #872 selected `DecisionResult review-only dashboard/API status`.
- #873 completed DecisionResult source read.
- #874 completed DecisionResult review-only runtime wiring design.
- #875 completed DecisionResult implementation readiness gate.
- #876 completed DecisionResult minimal review-only implementation.
- #877 completed workflow drift repair and fixed the Codex handoff/task template flow.
- `a0a432b` completed DecisionResult review-only runtime wiring verification.
- `b30c30e` completed the V1 Auto Operator Pack.
- `baa5cfe` completed DecisionResult Visual Verification / Closure.
- `1b12cd5` completed the V1 Auto Operator Post-Merge State Refresh.
- `c75919c` completed Next Minimal Runtime Slice Selection After DecisionResult Closure and selected `ExecutionPlan / BoundaryCandidate review-only display continuation`.
- `8f404cd` completed Source Read for ExecutionPlan / BoundaryCandidate review-only display continuation.
- `b3e6d71` completed the minimal review-only ExecutionPlan / BoundaryCandidate runtime wiring design.
- `a84a4aa` completed the implementation readiness gate and allowed minimal review-only implementation.
- `60e034a` completed the minimal review-only implementation for ExecutionPlan / BoundaryCandidate review-only display continuation.
- `85fb7ad` completed the source-of-truth refresh after that implementation and kept the next package as verification.
- `4a278b0` completed the minimal review-only verification for ExecutionPlan / BoundaryCandidate review-only display continuation.
- `caa45c9` completed the post-verification source-of-truth refresh.
- `d907719` completed ExecutionPlan / BoundaryCandidate visual closure.
- `Review / Replay result status` was selected on main as `86b3ff3` as the seventh minimal source-read-only runtime slice target.
- Source Read for Review / Replay result status is completed on main as `fb0263e` and returns GO to design.
- Minimal Review-Only Review / Replay Result Status Runtime Wiring Design is completed on main as `4d17081`.
- Minimal Review-Only Review / Replay Result Status Runtime Wiring Implementation Readiness Gate is completed on main as `650816c`.
- Minimal Review-Only Review / Replay Result Status Runtime Wiring Implementation is completed on main as `2f98fc3`.
- Minimal Review-Only Review / Replay Result Status Runtime Wiring Verification is completed on main as `791260f`.
- Current active package is Review / Replay Result Status Visual Verification / Closure.

Historical PRs are history only. They do not define the current active block unless `docs/ACTIVE_MAINLINE_STATUS.yml` and `scripts/v1-state.sh` agree.

P359 remains not completed progress because PR #829 was closed unmerged. P360 is not allowed to start.

## Fixed Workflow Commands

- Bootstrap: `bash scripts/v1-session-bootstrap.sh`
- State: `bash scripts/v1-state.sh`
- Next task prompt: `bash scripts/codex-next-task.sh`
- Chinese operator entry: `bash scripts/v1-auto.sh next`
- One-command Codex runner: `bash scripts/v1-codex-run-next.sh`
- PR completion helper: `bash scripts/v1-pr-complete.sh <PR_NUMBER> <A|B|C> "<SUBJECT>" [--confirm-reviewed]`
- PR helper: `bash scripts/v1-pr-flow-helper.sh --branch <branch> --title "<title>" --risk <risk>`
- Open PR: `bash scripts/v1-open-pr.sh <branch> "<title>" <risk> [--body-file <file>] [--draft|--ready]`
- Merge sync: `bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SUBJECT>" --risk <risk> [--confirm]`
- Safe check: `bash scripts/v1-safe-check.sh`

Do not write `scripts/v1-status.sh`; the fixed state script is `scripts/v1-state.sh`.

## Forbidden Shortcuts

Do not:

- use chat memory to determine progress
- treat branch pushed / PR created / CI green / Codex output as completion
- handwrite long `gh pr create` or `gh pr merge` commands when fixed scripts cover the case
- open the next package before current package is merged on `main`
- continue P359 or start P360 by default
- add new DTO / Validator / Assembler / Orchestrator by default
- connect Push, external channel, Candidate generation, Decision generation, Point generation, order, execution, or auto-trading
- describe this workflow repair as a business capability increase

## Capability Language

Progress must be described by capability level:

| Level | Name | Meaning |
|---:|---|---|
| 0 | NOT_STARTED | No usable project artifact for this capability. |
| 1 | DOCS_ONLY_GATE | Documentation, scope, authorization, or closure only. |
| 2 | SKELETON | Code shape exists but does not yet prove behavior with targeted tests. |
| 3 | TARGETED_TEST | Skeleton plus focused tests or safe rule tests. |
| 4 | TEST_ONLY_WIRING | Components are connected only in tests or fixtures. |
| 5 | REVIEW_ONLY_RUNTIME | Runtime or UI can show safe, non-executable, manual-review output. |
| 6 | PRODUCTION_WIRING | Real runtime wiring exists, still not necessarily production-ready. |
| 7 | PRODUCTION_READY | Production-ready behavior with complete safety, observability, and review requirements. |

`REVIEW_ONLY_RUNTIME partial` remains the current level.
