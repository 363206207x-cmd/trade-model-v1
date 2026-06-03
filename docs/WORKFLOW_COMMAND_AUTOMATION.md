# Workflow Command Automation

This document keeps the P291D / P291E / P291G scripts available, but P291H changes the priority.

Default workflow is GPT + Codex + GitHub-native.
（默认工作流是 GPT + Codex + GitHub 原生。）

Terminal scripts are fallback only except local main sync after merge.
（终端脚本除合并后同步 main 外，只作为兜底。）

Codex must output PR number and stop.
（Codex 必须输出 PR 编号并停止。）

## Default Priority

Default:

- GPT decides the next pack.
- Codex checks duplicate Issue / PR / branch.
- Codex creates Issue, branch, commit, push, and Draft PR.
- GPT reviews the GitHub PR.
- The user only confirms B/C or C merge.
- Terminal is used for local main sync after merge, or when GitHub tools are unavailable.

Do not default to asking the user to run a menu, find a PR number, judge mergeability, or inspect CI.

## Fallback Scripts

Use these only when the GitHub-native path is unavailable or a local diagnostic is needed:

- `bash scripts/v1-pr-review-input.sh <PR_NUMBER>`
- `bash scripts/v1-status.sh`
- `bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SUBJECT>"`
- `bash scripts/v1-safe-check.sh`
- `bash scripts/v1-session-bootstrap.sh`
- `bash scripts/v1-next-pack-context.sh`

## Not Recommended As Default

- `bash scripts/v1.sh` menu is a fallback menu, not the default entry point.
- `bash scripts/v1-auto.sh` is a fallback diagnostic, not the default workflow.
- `bash scripts/v1-merge-current.sh` must not guess the PR from branch or stale status.
- `bash scripts/v1-merge-current.sh` requires an explicit PR number.

Valid explicit forms:

```bash
bash scripts/v1-merge-current.sh <PR_NUMBER>
APPROVED_PR_NUMBER=<PR_NUMBER> bash scripts/v1-merge-current.sh
```

Without an explicit PR number, it must stop with:

```text
MERGE_CURRENT_DISABLED_REQUIRE_EXPLICIT_PR
```

## Merge Safety

For B/C PRs and C PRs, the user must explicitly say the equivalent of:

```text
同意合并 PR #<PR_NUMBER>
```

before any merge script may run.

`v1-merge-sync.sh` and `v1-merge-current.sh` check PR state, mergeability, and CI status before merging, but they do not replace human approval rules.

`v1-merge-sync.sh` also supports already-merged PRs. If the PR state is `MERGED`, it must not attempt another merge; it enters already-merged sync mode, switches to `main`, pulls `origin main`, and prints `MERGE_SYNC_DONE`.

`v1-merge-sync.sh` 也支持已合并 PR。如果 PR state 是 `MERGED`，脚本不得再次尝试 merge；它进入 already-merged sync mode，切回 `main`，pull `origin main`，并输出 `MERGE_SYNC_DONE`。

If a PR is closed without being merged, `v1-merge-sync.sh` must stop with `PR_CLOSED_NOT_MERGED`.

如果 PR 已关闭但未合并，`v1-merge-sync.sh` 必须以 `PR_CLOSED_NOT_MERGED` 停止。

## Script Inventory

| Script | Purpose | Default? | May merge |
|---|---|---:|---:|
| `scripts/v1-pr-review-input.sh <PR_NUMBER>` | Fallback PR metadata and changed-file report. | fallback | no |
| `scripts/v1-status.sh` | Fallback local status and open GitHub item list. | fallback | no |
| `scripts/v1-merge-sync.sh <PR_NUMBER> "<SUBJECT>"` | Explicit PR merge and local main sync after approval. | sync helper | yes |
| `scripts/v1-safe-check.sh` | Fallback local safety check after Codex work. | fallback | no |
| `scripts/v1-session-bootstrap.sh` | Fallback new-window context. | fallback | no |
| `scripts/v1-next-pack-context.sh` | Fallback next-pack context. | fallback | no |
| `scripts/v1-auto.sh` | Fallback diagnostic that prints current status and next step. | fallback | no |
| `scripts/v1.sh` | Fallback interactive menu. | fallback | only by explicit merge choice |
| `scripts/v1-merge-current.sh <PR_NUMBER>` | Explicit current PR merge helper after approval. | fallback | yes |

## Boundaries

These scripts do not authorize:

- Java changes;
- tests or DTO changes;
- dashboard, schema, config, or resource changes;
- runtime/live/external data reads;
- `MarketQuoteClient` / `BinanceMarketQuoteClient` wiring;
- scan output / score / Candidate / Push / Readiness / point generation;
- entry / stop / TP / RR implementation;
- order / execution / auto-trading.

If a script reports failure, do not continue feature work until the failure is understood.
