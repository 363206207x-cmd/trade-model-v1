# Workflow Command Automation

This document defines the fixed local workflow commands for Trade Model V1 when GitHub connector handoff needs a deterministic shell fallback.

本文档定义 Trade Model V1 在 GitHub connector 交接需要本地兜底时使用的固定命令。

Default workflow remains GPT + Codex + GitHub-native.

默认工作流仍然是 GPT + Codex + GitHub-native。

This is a workflow execution rule only. It does not change business capability, Java code, tests, runtime wiring, dashboard, external channel, Push, order, execution, or auto-trading.

本文件只修复工作流执行入口，不改变业务能力、Java、测试、运行时接线、dashboard、external channel、Push、order、execution 或 auto-trading。

## Status Detection / 状态检测

Use this command before deciding whether the next package may start:

在判断是否可以进入下一包前，使用：

```bash
bash scripts/v1-state.sh
```

The script prints:

- current branch;
- worktree clean status;
- current HEAD;
- recent commits;
- open PR list;
- local `main` sync state against `origin/main`;
- whether a current-package PR exists;
- whether the next package may continue;
- blockers when continuation is not allowed.

脚本输出：

- 当前 branch；
- worktree 是否 clean；
- 当前 HEAD；
- 最近 commits；
- open PR 列表；
- 本地 `main` 相对 `origin/main` 是否同步；
- 当前包 PR 是否存在；
- 是否可以进入下一包；
- 不能继续时的阻塞原因。

If `gh` is unavailable or unauthenticated, the script prints `GH_NOT_AVAILABLE` instead of producing noisy auth errors.

如果 `gh` 不可用或未认证，脚本输出 `GH_NOT_AVAILABLE`，不输出难读的认证错误。

## Draft PR Creation / 创建 Draft PR

Do not handwrite long `gh pr create` commands.

不再手写大段 `gh pr create` 命令。

Use:

使用：

```bash
bash scripts/v1-open-pr.sh <branch> "<title>" <risk>
```

Example:

示例：

```bash
bash scripts/v1-open-pr.sh p336 "BACKEND-P336 RuntimeKlineContextSourceBindingDTO Java Skeleton" B
```

Supported risk values:

支持的风险值：

- `A`
- `B`
- `B/C`
- `C`

The script checks the remote branch and existing open PRs for that head branch. If an open PR already exists, it prints the existing PR URL and does not create a duplicate.

脚本会检查远端分支和该 head branch 是否已有 open PR。如果已经存在 PR，只输出现有 PR URL，不重复创建。

The generated body always includes:

自动生成的 PR body 必须包含：

- no executable point generation;
- no executable entry / stop / TP / RR;
- no final direction;
- no external channel;
- no Push send;
- no order / execution / auto-trading;
- incomplete-safe / fail-closed remains mandatory;
- Risk Action Guard remains mandatory.

## Merge And Main Sync / 合并与 main 同步

Do not handwrite long `gh pr merge` commands.

不再手写大段 `gh pr merge` 命令。

Use only after the merge authorization rule in `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md` is satisfied:

只有满足 `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md` 中的合并授权规则后，才使用：

```bash
bash scripts/v1-merge-sync.sh <pr-number> "<title (#pr)>"
```

The script:

- checks that `gh` is available;
- checks the PR exists;
- prints current PR state;
- marks Draft PRs ready;
- stops when checks are pending or failing;
- squash merges;
- deletes the remote branch;
- switches to `main`;
- pulls `origin main`;
- prints final status and recent commits;
- ends with `MERGE_SYNC_DONE`, `WORKTREE_CLEAN`, and `HEAD`.

脚本会：

- 检查 `gh` 可用；
- 检查 PR 存在；
- 输出 PR 当前状态；
- 将 Draft PR 标记为 ready；
- CI 未完成或失败时停止；
- squash merge；
- 删除远端分支；
- 切回 `main`；
- pull `origin main`；
- 输出最终状态和最近 commits；
- 最后输出 `MERGE_SYNC_DONE`、`WORKTREE_CLEAN` 和 `HEAD`。

## Handoff Rule / 交接规则

When the GitHub connector is unavailable, GPT must provide one of these fixed script commands instead of a custom multi-line `gh` command.

当 GitHub connector 不可用时，GPT 必须提供这些固定脚本命令之一，而不是自定义多行 `gh` 命令。

Codex may run these scripts only when the current task explicitly allows the corresponding action.

Codex 只能在当前任务明确允许相应动作时运行这些脚本。

## Legacy Fallback Inventory / 旧兜底脚本清单

The following scripts may still exist as legacy fallback diagnostics, but they are not the preferred fixed commands for PR creation or merge sync:

以下脚本可能仍作为旧兜底诊断存在，但不再是创建 PR 或合并同步的优先固定命令：

```bash
bash scripts/v1.sh
bash scripts/v1-auto.sh
bash scripts/v1-merge-current.sh <PR_NUMBER>
```

Use `bash scripts/v1-state.sh`, `bash scripts/v1-open-pr.sh <branch> "<title>" <risk>`, and `bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SUBJECT>"` for the fixed workflow command path.

固定工作流命令路径优先使用 `bash scripts/v1-state.sh`、`bash scripts/v1-open-pr.sh <branch> "<title>" <risk>` 和 `bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SUBJECT>"`。

The scripts do not authorize:

脚本不授权：

- business Java changes;
- business test changes;
- schema / config / pom changes;
- dashboard changes;
- service / controller / mapper / repository / scheduler wiring;
- runtime wiring;
- market data reads;
- latest price / latest close reads;
- external provider reads;
- real entry / stop / TP / RR generation;
- final direction generation;
- external channel;
- Push send;
- order;
- execution;
- auto-trading.

## No Skip Rule / 禁止跳包

Do not start the next package while any of these are true:

只要存在以下任一状态，不允许开下一包：

- branch not pushed;
- PR not created;
- PR not reviewed;
- PR not merged;
- main not synced;
- worktree dirty;
- CI not green;
- B / B/C / C merge authorization missing;
- PR is Draft and not ready;
- PR is not mergeable;
- PR conflicts with main.

Use `bash scripts/v1-state.sh` to make the blocker visible.

用 `bash scripts/v1-state.sh` 把阻塞原因显式打出来。

## Main Clean Auto-Next / main clean 后自动下一步

When the current package is merged, `main` is pulled, the worktree is clean, CI passed, and no open blocker exists, GPT must directly generate the next maximum safe Codex prompt.

当当前包已 merge、`main` 已 pull、worktree clean、CI 通过且没有 open blocker 时，GPT 必须直接生成下一包最大安全 Codex prompt。

Do not ask the user whether to continue in that state.

在这种状态下不要再问用户“是否下一步”。
