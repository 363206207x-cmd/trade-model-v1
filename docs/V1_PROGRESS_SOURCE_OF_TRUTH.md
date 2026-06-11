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

Baseline sync packages are no longer required for ordinary source-of-truth lag. If merged `main` is clean, synced, and has no open PR, actual HEAD is the effective execution baseline. The next scoped business package updates `ACTIVE_MAINLINE_STATUS.yml`, `CODEX_NEXT_TASK.yml`, and related source-of-truth docs as part of its normal handoff.

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

- Current merged main: `a8acc70 chore(workflow): eliminate baseline sync packages`
- Current active block: `Next minimal runtime slice selection after RuleConfig closure`
- Current level: `REVIEW_ONLY_RUNTIME partial`
- Capability movement from the RuleConfig visual closure package: environment-limited visual closure only. RuleConfig runtime audit / rule explainability has implementation wiring, verification, and visual closure evidence; it is now the 9th completed review-only runtime partial slice after this package is accepted. Overall level remains `REVIEW_ONLY_RUNTIME partial`, not Production Wiring
- Next required action: `Next minimal runtime slice selection`
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
- `001cbf7` completes the V1 One-Command Codex Runner workflow improvement. It adds one-command Codex runner and PR completion helpers only; it does not change business capability.
- `5da301b` completes Review / Replay Result Status Visual Verification / Closure with browser verification, endpoint/dashboard smoke, source-of-truth alignment, and no business code changes.
- `91613bb` completes the V1 One-Command Runner Hotfix. It fixes `gh pr checks` state parsing, duplicate quality-gate handling, and `--open-pr-none-confirmed` Codex GitHub status handoff without changing business capability.
- This selection package chooses `Data Source Health dashboard/API status` as the next minimal source-read target after seven completed review-only runtime slices. It may add only selection docs and source-of-truth updates.
- `5534b52` completes Next Minimal Runtime Slice Selection After Review / Replay Closure and selects `Data Source Health dashboard/API status` as the next source-read target.
- `6343a60` completes Source Read for Data Source Health dashboard/API status: it confirms `DataSourceHealthDO` exists as an unwired carrier, local `sourceHealth` signals already exist across MarketQuote / Evidence-Score / DecisionResult / ExecutionPlan-BoundaryCandidate / Review-Replay status endpoints and dashboard panels, and no dedicated aggregate data-source-health API/panel/schema owner was found.
- `c90fe98` completes Minimal Review-Only Data Source Health Dashboard/API Status Runtime Wiring Design: it selects a thin review-only aggregate over existing slice-local source-health status surfaces, keeps `DataSourceHealthDO` inventory-only, rejects new mapper/service/schema/DTO ownership, defines rollup/status mapping and fail-closed rules, and returns GO to implementation readiness gate without implementation.
- `62843de` completes the workflow phase normalization that makes the Data Source Health readiness-gate handoff runnable by `v1-auto.sh next`.
- `9290c1b` completes the Data Source Health implementation readiness gate and returns GO to a minimal review-only Data Source Health Dashboard/API status implementation.
- `2984e48` completes the Data Source Health implementation package with one minimal read-only `/api/dashboard/data-source-health-status` endpoint, a dashboard `dataSourceHealthStatusPanel`, targeted `DashboardControllerTest` coverage, and implementation/source-of-truth docs. It does not trigger external API refresh, scheduler, collector, API client reads, Push, Candidate generation, Decision generation, Point generation, replay execution, review result generation, or trading.
- `85e8182` completes Data Source Health runtime wiring verification: compile, test-compile, `DashboardControllerTest` 45 tests, MockMvc/template endpoint-dashboard behavior, forbidden semantics grep, forbidden path check, and `git diff --check` all passed. Live HTTP smoke was attempted but sandbox socket bind was blocked with `Operation not permitted`.
- `c6b35b5` completes Data Source Health visual closure with environment-limited evidence: `dataSourceHealthStatusPanel` DOM/copy/safety copy are present, no live screenshot or live UI smoke success is claimed, review-only/fail-closed/not executable semantics are clear, and no Push / Candidate generation / Decision generation / Point / trading action semantics are present.
- Any B-risk workflow usability hotfix is workflow tooling history only in this handoff. It does not change Data Source Health business behavior and is not the current active package.
- Data Source Health dashboard/API status is the 8th completed review-only runtime partial slice on merged main; RuleConfig runtime audit / rule explainability becomes the 9th completed review-only runtime partial slice after this closure package is accepted.
- `d9f7817` completes the V1 status summary accuracy fix. It aligns the workflow/status reporting baseline after Data Source Health visual closure and does not change business capability.
- `ed6def3` completes Next Minimal Runtime Slice Selection After Data Source Health Closure and selects `RuleConfig runtime audit / rule explainability` as the next source-read-only target after eight completed review-only runtime slices.
- `5903409` completes Source Read for RuleConfig runtime audit / rule explainability. It confirms existing RuleConfig / Watchlist owner assets, the current Watchlist status panel/API pattern, adjacent RuleVersionLog audit context, and generic RuleConfig audit/explainability gaps; it returns GO to design only, with no implementation and no capability movement.
- `b4497e1` completes the V1 Operator One-Command Orchestrator workflow-only package; it does not execute RuleConfig design and does not change business capability.
- `2778b82` completes Minimal Review-Only RuleConfig Runtime Audit / Rule Explainability Runtime Wiring Design on main. It preserves the existing RuleConfig / Watchlist owner path, keeps RuleVersionLog as context-only audit evidence, defines review-only status mapping, dashboard/API boundary, fail-closed rules, and readiness checklist, and returns GO to implementation readiness gate without implementation.
- `b298ee9` completes RuleConfig runtime audit / rule explainability implementation readiness gate on main. It returns GO for one minimal read-only `RuleController` `Map` status endpoint plus minimal dashboard status/copy/DOM and targeted tests over existing RuleConfig / Watchlist owner assets. It keeps RuleVersionLog context-only, disabled-vs-missing ambiguity partial/fail-closed, and forbids DTO/Validator/Assembler, schema/service ownership, Push, Candidate generation, Decision generation, Point, trading, replay execution, review result generation, P359, and P360.
- `abc9d40` completes Minimal Review-Only RuleConfig Runtime Audit / Rule Explainability Runtime Wiring Implementation on main. It adds one minimal read-only `/api/rule/config-audit-status` endpoint, dashboard `ruleConfigAuditStatusPanel`, targeted `RuleControllerTest` / `DashboardControllerTest` coverage, implementation docs, and source-of-truth updates over existing RuleConfig / Watchlist owner assets. It does not add DTO/Validator/Assembler, schema/config/pom, Push, Candidate generation, Decision generation, Point, trading, replay execution, review result generation, P359, or P360.
- `028c598` completes Minimal Review-Only RuleConfig Runtime Audit / Rule Explainability Runtime Wiring Verification on main. It verifies workflow contract, compile, test-compile, targeted `RuleControllerTest` 9 tests, targeted `DashboardControllerTest` 46 tests, MockMvc/template endpoint-dashboard behavior, RuleConfig owner path, Watchlist key status, RuleVersionLog context-only boundary, forbidden semantics grep, forbidden path check, and source-of-truth alignment.
- `e568ded` is the last baseline sync package before baseline sync packages are removed from the normal workflow.
- `a8acc70` completes V1 Eliminate Baseline Sync Packages; clean / synced main with no open PR now uses actual HEAD as the effective execution baseline, and the next business package updates source-of-truth opportunistically.
- RuleConfig Runtime Audit / Rule Explainability Visual Verification / Closure is completed by this package when merged. Environment-limited evidence confirms `ruleConfigAuditStatusPanel` DOM/copy/safety copy, RuleVersionLog context-only copy, `/api/rule/reload` boundary copy, and no Push / Candidate generation / Decision generation / Point / trading semantics. Live Spring Boot bind and Browser backend were unavailable in the sandbox, so no live screenshot or live UI smoke success is claimed.
- Current active package after this visual closure is `Next minimal runtime slice selection after RuleConfig closure`. It may add only selection docs and source-of-truth updates.

## Runtime Slice History

Completed review-only runtime slices:

1. `PositionSync + Dashboard review-only status`: `REVIEW_ONLY_RUNTIME partial`
2. `Watchlist + RuleConfig + Dashboard/API review-only status`: `REVIEW_ONLY_RUNTIME partial`
3. `MarketQuote freshness / fallback / dashboard API status`: `REVIEW_ONLY_RUNTIME partial`
4. `Evidence / Score review-only runtime status`: `REVIEW_ONLY_RUNTIME partial`
5. `DecisionResult review-only dashboard/API status`: `REVIEW_ONLY_RUNTIME partial`
6. `ExecutionPlan / BoundaryCandidate review-only runtime status`: `REVIEW_ONLY_RUNTIME partial`
7. `Review / Replay result status`: `REVIEW_ONLY_RUNTIME partial`
8. `Data Source Health dashboard/API status`: `REVIEW_ONLY_RUNTIME partial`
9. `RuleConfig runtime audit / rule explainability`: `REVIEW_ONLY_RUNTIME partial`

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
- V1 One-Command Codex Runner workflow improvement is completed on main as `001cbf7`.
- Review / Replay Result Status Visual Verification / Closure is completed on main as `5da301b`.
- V1 One-Command Runner Hotfix is completed on main as `91613bb`; it fixes `gh pr checks` state parsing and adds `--open-pr-none-confirmed` without changing business capability.
- Next Minimal Runtime Slice Selection After Review / Replay Closure selects `Data Source Health dashboard/API status` as the next source-read target.
- Source Read for Data Source Health dashboard/API status is completed on main as `6343a60` and returned GO to design only.
- Minimal Review-Only Data Source Health Dashboard/API Status Runtime Wiring Design is completed on main as `c90fe98` and returns GO to implementation readiness gate.
- Data Source Health implementation readiness gate is completed on main as `9290c1b`.
- Data Source Health implementation is completed on main as `2984e48`.
- Data Source Health runtime wiring verification is completed on main as `85e8182`.
- Data Source Health visual closure is completed on main as `c6b35b5`; it is the eighth completed review-only runtime partial slice.
- Source Read for RuleConfig runtime audit / rule explainability is completed on main as `5903409` and returned GO to design only.
- Minimal Review-Only RuleConfig Runtime Audit / Rule Explainability Runtime Wiring Design is completed on main as `2778b82` and returns GO to implementation readiness gate.
- RuleConfig runtime audit / rule explainability implementation readiness gate is completed on main as `b298ee9` and returns GO to minimal implementation.
- Minimal Review-Only RuleConfig Runtime Audit / Rule Explainability Runtime Wiring Implementation is completed on main as `abc9d40`.
- Minimal Review-Only RuleConfig Runtime Audit / Rule Explainability Runtime Wiring Verification is completed on main as `028c598`.
- RuleConfig Runtime Audit / Rule Explainability Visual Verification / Closure is completed by this package when merged; current active package is Next minimal runtime slice selection after RuleConfig closure.

Historical PRs are history only. They do not define the current active block unless `docs/ACTIVE_MAINLINE_STATUS.yml` and `scripts/v1-state.sh` agree.

P359 remains not completed progress because PR #829 was closed unmerged. P360 is not allowed to start.

## Fixed Workflow Commands

- Bootstrap: `bash scripts/v1-session-bootstrap.sh`
- State: `bash scripts/v1-state.sh`
- Next task prompt: `bash scripts/codex-next-task.sh`
- Chinese operator entry: `bash scripts/v1-auto.sh next`
- One-command Codex runner: `bash scripts/v1-codex-run-next.sh`
- One-command Codex runner with Open PR none handoff evidence: `bash scripts/v1-codex-run-next.sh --open-pr-none-confirmed`
- PR completion helper: `bash scripts/v1-pr-complete.sh <PR_NUMBER> <A|B|C> "<SUBJECT>" [--confirm-reviewed]`
- Dirty-work package helper: `bash scripts/v1-package-dirty-work.sh`
- One-command operator orchestrator: `bash scripts/v1-operator.sh`
- B-risk reviewed merge handoff: `bash scripts/v1-operator.sh --confirm-reviewed <PR_NUMBER>`
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
