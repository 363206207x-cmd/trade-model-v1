# Session Bootstrap

Use this file first in every new window.

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
