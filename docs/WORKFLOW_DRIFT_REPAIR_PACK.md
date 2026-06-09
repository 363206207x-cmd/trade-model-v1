# Workflow Drift Repair Pack

## 1. Executive Summary

本包修复 workflow 漂移，不改变业务 runtime 能力。

修复内容：

- 手写长 `gh pr create` / `gh pr merge` 命令漂移：固定到 `v1-open-pr.sh` 和 `v1-merge-sync.sh`。
- 当前状态入口漂移：以 `ACTIVE_MAINLINE_STATUS.yml`、`v1-state.sh`、merged `main` 为准。
- `v1-status.sh` / `v1-state.sh` 命名漂移：统一为 `scripts/v1-state.sh`。
- A/B/C merge rule 没有脚本化：`v1-merge-sync.sh` 增加 risk / confirm 参数。
- task prompt 太长：新增 `CODEX_NEXT_TASK.yml`、任务模板、`codex-next-task.sh`。
- source-of-truth 历史日志与当前状态混杂：拆出 Current State Rules、Current Active Block、Runtime Slice History、Fixed Workflow Commands、Forbidden Shortcuts。
- Codex shell `GH_NOT_AVAILABLE` 误判：规则明确它只是 Codex GitHub status unknown，可由 GPT connector 或用户本机 terminal handoff evidence 补足项目状态判断。

## 2. Root Cause

| Drift | Evidence | Root cause | Fix |
|---|---|---|---|
| 手写长 gh 命令漂移 | `WORKFLOW_COMMAND_AUTOMATION.md` 要求固定命令，但脚本不支持 body-file / ready / risk confirm | 固定脚本能力不足 | 增强 `v1-open-pr.sh`、`v1-merge-sync.sh`、新增 PR flow helper |
| 当前状态入口漂移 | `AGENTS.md` / `SESSION_BOOTSTRAP.md` 仍写旧 track | 历史阶段硬编码未移除 | 改为 active block / next action 由 `ACTIVE_MAINLINE_STATUS.yml` 决定 |
| v1-status/v1-state 命名漂移 | source-of-truth 写 `scripts/v1-status.sh` | 脚本改名后文档未同步 | 统一为 `scripts/v1-state.sh` |
| A/B/C merge rule 未脚本化 | merge 脚本无 risk / confirm 参数 | merge 风险规则只在文档 | `v1-merge-sync.sh` 增加 `--risk` / `--confirm` |
| task prompt 太长 | 用户在 GPT / Codex / terminal 间重复复制长提示 | 缺少机器可读 next-task handoff | 新增 `CODEX_NEXT_TASK.yml`、模板、`codex-next-task.sh` |
| 当前状态和历史流水混杂 | source-of-truth 长句串联历史 PR | 当前态与历史态未分层 | 重排 `V1_PROGRESS_SOURCE_OF_TRUTH.md` |
| Codex GH_NOT_AVAILABLE 误判 | Codex shell 可能无法读本机 keyring | Codex 环境 auth 与项目 GitHub 状态混为一谈 | handoff 规则区分 Codex status unknown 与项目 blocker |

## 3. New Fixed Flow

1. `bash scripts/v1-state.sh`
2. `bash scripts/codex-next-task.sh > /tmp/codex-task.md`
3. Codex 执行任务并 push
4. `bash scripts/v1-open-pr.sh <branch> "<title>" <risk> --body-file <file>`
5. `gh pr checks <PR>`
6. `bash scripts/v1-merge-sync.sh <PR> "<title (#PR)>" --risk <risk> --confirm` for B / B/C / C
7. main clean 后继续下一包

If Codex shell cannot run `gh`, treat that as Codex GitHub status unknown. GPT connector or user terminal evidence can satisfy open PR / main sync / clean worktree handoff.

## 4. Risk Rules

- A docs-only: CI green, technical review, docs-only changed files, and no overreach may be merged by GPT.
- B Java/test/dashboard: CI green is not enough; explicit user merge approval is required.
- B/C elevated: explicit user approval is required.
- C high-risk / disabled: explicit user approval is required; disabled scopes remain disabled unless separately authorized.
- Push, external channel, order, execution, and auto-trading remain disabled by default.

## 5. What This Pack Does Not Change

- Java business behavior
- dashboard business function
- schema/config/pom
- DecisionResult runtime capability
- Push/Candidate/Decision generation/Point/Trading
- capability level

## 6. Verification

| Check | Result |
|---|---|
| User terminal precondition handoff | Pass: user terminal confirmed `BRANCH: main`, `WORKTREE_CLEAN: Yes`, `HEAD: 0c7d4d4`, `OPEN_PRS: none`, `MAIN_SYNC: OK`, `CAN_CONTINUE_NEXT_PACKAGE: YES`, `BLOCKERS: none`. |
| `bash scripts/check-workflow-contract.sh` | Pass: `WORKFLOW_CONTRACT_OK`. |
| `bash scripts/v1-state.sh` | Expected working-branch result: reports `workflow-drift-repair`, dirty/not-main, and Codex `GH_NOT_AVAILABLE`; this is not project blocker for the repair branch. |
| `bash scripts/codex-next-task.sh >/tmp/codex-next-task-check.md` | Pass. |
| `bash scripts/v1-pr-flow-helper.sh --help` | Pass. |
| `bash scripts/v1-open-pr.sh --help` | Pass. |
| `bash scripts/v1-merge-sync.sh --help` | Pass. |
| `bash scripts/v1-open-pr.sh ... --dry-run` | Pass. |
| `bash scripts/v1-merge-sync.sh ... --risk B --dry-run` without `--confirm` | Pass guard: rejects with `MERGE_REQUIRES_USER_CONFIRMATION`. |
| `bash scripts/v1-merge-sync.sh ... --risk B --confirm --dry-run` | Pass. |
| `git diff --check` | Pass. |
| `git diff --cached --check` | Pass. |
| `git diff --check main...HEAD` | Pass. |
| Forbidden business path grep | Pass: no `src/main/java`, `src/test/java`, `dashboard.html`, schema, application config, or `pom.xml` changes. |
| Forbidden semantics grep | Pass: no new Push / Candidate / Decision generation / Point / Trading semantics in changed workflow scope. |
