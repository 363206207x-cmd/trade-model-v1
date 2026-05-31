# V1 Progress Source Of Truth

This document defines how Trade Model V1 progress is determined.

New windows must start from `docs/SESSION_BOOTSTRAP.md`.

Preferred command:

```bash
bash scripts/v1-session-bootstrap.sh
```

## Completion Rule

Only merged `main` counts as completed project state.

The following do not count as completed:

- open Issue;
- open PR;
- draft PR;
- local branch;
- remote branch;
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
3. `git log --oneline -5` on `main`
4. `docs/V1_CURRENT_STATE.md`
5. `docs/PROJECT_PROGRESS_INDEX.md`
6. `docs/V1_CAPABILITY_MATRIX.md`
7. `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`
8. `docs/ANSWER_FORMAT_CONTRACT.md`

If these sources disagree, merged `main` wins and the docs must be corrected.

## Forbidden Progress Shortcuts

Do not use chat memory to determine progress.

Do not treat docs-only work as production completion.

Do not treat a skeleton as production wiring.

Do not treat targeted tests as runtime behavior.

Do not treat an open PR, open branch, or open Issue as already merged.

Do not treat Codex output as completion.

Do not treat legacy runtime clients as proof that the new scan-chain is complete.

Do not count repeated blocked-list documents as product usability progress.

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

## Workflow Command Automation

- 新窗口优先运行 `bash scripts/v1-session-bootstrap.sh`
- 状态检查优先运行 `bash scripts/v1-status.sh`
- 审 PR 优先运行 `bash scripts/v1-pr-review-input.sh <PR_NUMBER>`
- 合并同步优先运行 `bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SUBJECT>"`
- Codex 完成后优先运行 `bash scripts/v1-safe-check.sh`
