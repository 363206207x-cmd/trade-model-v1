# Session Bootstrap

Use this file first in every new window.

For terminal workflows, prefer bash scripts/v1.sh as the single entry point.  
（终端工作流优先使用 bash scripts/v1.sh 作为单一入口。）

For terminal workflows, prefer `bash scripts/v1-auto.sh` as the default non-interactive entry point.
（终端工作流默认优先使用 `bash scripts/v1-auto.sh` 作为非交互入口。）

Keep `bash scripts/v1.sh` only as a fallback menu.
（`bash scripts/v1.sh` 只作为备用菜单。）

Preferred command:

```bash
bash scripts/v1-session-bootstrap.sh
```

1. Read `docs/ACTIVE_MAINLINE_STATUS.yml`.
2. Read `docs/V1_CAPABILITY_MATRIX.md`.
3. Read `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`.
4. Run `git log --oneline -5`.
5. Never use chat memory as progress.
6. Reply using `docs/ANSWER_FORMAT_CONTRACT.md`.
7. Do not continue to next package unless current PR is merged.
8. Open PR / branch / Issue does not count as done.

## Workflow Command Shortcuts

- New window: `bash scripts/v1-session-bootstrap.sh`
- Status check: `bash scripts/v1-status.sh`
- PR review input: `bash scripts/v1-pr-review-input.sh <PR_NUMBER>`
- Merge and sync after explicit approval: `bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SUBJECT>"`
- Codex completion safe check: `bash scripts/v1-safe-check.sh`
