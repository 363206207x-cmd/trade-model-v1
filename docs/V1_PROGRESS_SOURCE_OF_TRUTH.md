# V1 Progress Source Of Truth

This document defines how Trade Model V1 progress is determined.

New windows must start from `docs/SESSION_BOOTSTRAP.md`.

Default workflow is GPT + Codex + GitHub-native.
（默认工作流是 GPT + Codex + GitHub 原生。）

Terminal scripts are fallback only except local main sync after merge.
（终端脚本除合并后同步 main 外，只作为兜底。）

GitHub auth and GPT / Codex / local `gh` handoff must follow `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`.
（GitHub 认证与 GPT / Codex / 本地 `gh` 交接必须遵守 `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`。）

Fallback bootstrap command:

```bash
bash scripts/v1-session-bootstrap.sh
```

## Completion Rule

Only merged `main` counts as completed project state.

The following do not count as completed:

- open Issue;
- open PR;
- draft PR;
- approved PR that is not merged;
- CI green PR that is not merged;
- local branch;
- remote branch;
- unsynced main after merge;
- dirty worktree;
- unmerged commit;
- chat memory;
- Codex output;
- planned scope;
- docs-only authorization;
- code skeleton without the next capability layer;
- test-only fixture when production wiring is being discussed.

## Required Progress Inputs

Every progress answer must check these sources in this order:

1. `docs/SESSION_BOOTSTRAP.md`
2. `docs/ACTIVE_MAINLINE_STATUS.yml`
3. `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`
4. `git branch --show-current`, `git status --short`, and `git log --oneline -5` on `main`
5. `docs/V1_CURRENT_STATE.md`
6. `docs/PROJECT_PROGRESS_INDEX.md`
7. `docs/V1_CAPABILITY_MATRIX.md`
8. `docs/V1_DUPLICATE_SKELETON_FREEZE_RULE.md`
9. `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`
10. `docs/ANSWER_FORMAT_CONTRACT.md`

If these sources disagree, merged `main` wins and the docs must be corrected.

## Forbidden Progress Shortcuts

Do not use chat memory to determine progress.

Do not treat docs-only work as production completion.

Do not treat a skeleton as production wiring.

Do not treat targeted tests as runtime behavior.

Do not treat an open PR, open branch, or open Issue as already merged.

Do not treat Codex output as completion.

Do not continue to the next package when PR creation, PR review, merge, main sync, or worktree cleanliness is unresolved.

Do not treat legacy runtime clients as proof that the new scan-chain is complete.

Do not count repeated blocked-list documents as product usability progress.

Do not create a new DTO, Validator, Assembler, Orchestrator, docs-only plan, verification-only package, source-binding wrapper, runtime-candidate wrapper, or point-candidate wrapper unless it satisfies `docs/V1_DUPLICATE_SKELETON_FREEZE_RULE.md`.

Do not continue P359 or start P360 by default. P359 is not completed progress unless it is merged into `main`. After the freeze rule, ownership-map track, runtime wiring target selection, source-read verification, wiring design, readiness gate, #839 implementation, #840 verification, #841 visual verification, #842 merge map, and #843 owner source read, the current selected target remains `PositionSync + Dashboard review-only status`, the active stop-loss action is `Minimal Merge Design for BoundaryCandidate / ExecutionPlan owner + safety adapters`, and the default next required action is `Minimal implementation readiness gate for owner-path safety adapter merge`.

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

## Review-Only Principle

Review-only does not mean no output.

Review-only means useful proposals and risk actions may be shown to a human while staying:

- manual-review required;
- not a trade instruction;
- non-executable;
- blocked from automatic order, close, reverse, leverage change, execution, or external send unless separately authorized.

Status and progress answers must use `docs/ANSWER_FORMAT_CONTRACT.md`.

## Duplicate Skeleton Freeze

The #830 audit found a package-count progress trap: repeated skeleton and docs-only packages were increasing surface area without moving user-visible capability toward `REVIEW_ONLY_RUNTIME`.

Future packages must include these extra final-output fields:

- 是否创建新骨架: Yes / No
- 是否复用 Cursor-era 资产: Yes / No
- 是否减少重复: Yes / No
- 是否提升 capability level: Yes / No
- 是否接 service/runtime/dashboard/API: Yes / No
- 是否符合 #830 审计建议: Yes / No

If the package would only add another skeleton or wrapper, it must be rejected and redirected to the ownership-map / wiring-plan tracks.

Current stop-loss sequence:

1. #830 global audit: completed.
2. Global Duplicate Skeleton Freeze Rule: active.
3. Cursor Artifact Inventory + Ownership Map: completed stop-loss audit track.
4. Runtime Wiring Target Selection Plan: completed stop-loss selection track.
5. Selected target: `PositionSync + Dashboard review-only status`.
6. PositionSync/Dashboard Source Read Verification: completed source-read verification track.
7. Minimal Review-Only PositionSync Runtime Wiring Design: completed docs-only wiring design track.
8. Minimal Review-Only PositionSync Runtime Wiring Implementation Readiness Gate: completed docs-only readiness gate.
9. Minimal Review-Only PositionSync Runtime Wiring Implementation: completed on main as #839.
10. Minimal Review-Only PositionSync Runtime Wiring Verification: completed on main as #840.
11. Dashboard PositionSync Visual Verification: completed on main as #841.
12. Source-Owned Runtime vs Existing Point Proposal Merge Map: completed on main as #842.
13. Targeted Source Read for BoundaryCandidate / ExecutionPlan owner: completed on main as #843.
14. Minimal Merge Design for BoundaryCandidate / ExecutionPlan owner + safety adapters: active design track.
15. Next required action: `Minimal implementation readiness gate for owner-path safety adapter merge`.

## Workflow Command Automation

- 默认工作流是 GPT + Codex + GitHub 原生。
- 终端脚本除合并后同步 main 外，只作为兜底。
- 新窗口兜底运行 `bash scripts/v1-session-bootstrap.sh`
- 状态检查兜底运行 `bash scripts/v1-status.sh`
- 审 PR 兜底运行 `bash scripts/v1-pr-review-input.sh <PR_NUMBER>`
- 合并同步运行 `bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SUBJECT>"`
- Codex 完成后兜底运行 `bash scripts/v1-safe-check.sh`
