# Session Bootstrap

Use this file first in every new window.

Default workflow is GPT + Codex + GitHub-native.
（默认工作流是 GPT + Codex + GitHub 原生。）

Terminal scripts are fallback only except local main sync after merge.
（终端脚本除合并后同步 main 外，只作为兜底。）

Codex must output PR number and stop.
（Codex 必须输出 PR 编号并停止。）

Fallback bootstrap command:

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

- Default workflow: GPT decides the next pack, Codex creates Issue / branch / Draft PR, GPT reviews the GitHub PR.
- Fallback new window command: `bash scripts/v1-session-bootstrap.sh`
- Fallback status check: `bash scripts/v1-status.sh`
- Fallback PR review input: `bash scripts/v1-pr-review-input.sh <PR_NUMBER>`
- Local merge sync after explicit approval: `bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SUBJECT>"`
- Fallback Codex completion safe check: `bash scripts/v1-safe-check.sh`
