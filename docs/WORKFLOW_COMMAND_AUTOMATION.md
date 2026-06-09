# Workflow Command Automation

This document defines the fixed local workflow commands for Trade Model V1 while keeping the default workflow GPT + Codex + GitHub-native.

本文档定义 Trade Model V1 的固定本地工作流命令，同时保留默认工作流 GPT + Codex + GitHub-native。

It is a workflow execution helper only. It does not change business capability, Java, tests, runtime wiring, dashboard, external channel, Push, order, execution, or auto-trading.

它只用于工作流执行辅助，不改变业务能力、Java、测试、运行时接线、dashboard、external channel、Push、order、execution 或 auto-trading。

## Status Check / 状态检测

Use:

```bash
bash scripts/v1-state.sh
```

The script prints:

- `BRANCH:`
- `WORKTREE_CLEAN:`
- `HEAD:`
- `RECENT_COMMITS:`
- `OPEN_PRS:`
- `MAIN_SYNC:`
- `CAN_CONTINUE_NEXT_PACKAGE:`
- `BLOCKERS:`

脚本输出当前分支、工作区是否干净、HEAD、最近提交、open PR、main 同步状态、是否可以进入下一包，以及阻塞原因。

If `gh` is unavailable, it prints `GH_NOT_AVAILABLE` in the open PR field and must not produce unreadable errors.

如果 `gh` 不可用，脚本在 open PR 字段输出 `GH_NOT_AVAILABLE`，不得输出不可读错误。

## Create Draft PR / 创建 Draft PR

Use:

```bash
bash scripts/v1-open-pr.sh <branch> "<title>" <risk>
```

Preferred extended form when a package needs a dedicated PR body:

```bash
bash scripts/v1-open-pr.sh <branch> "<title>" <risk> --body-file <file>
```

Example:

```bash
bash scripts/v1-open-pr.sh p337 "BACKEND-P337 RuntimeKlineContextSourceBindingValidator Java Skeleton" "B"
```

Supported risks:

- `A`
- `B`
- `B/C`
- `C`

The script checks that the remote branch exists and that no PR already exists for the same head branch. If a PR already exists, it prints the PR URL and does not create a duplicate.

脚本会检查远端分支存在，并检查同一 head branch 是否已有 PR。如果已有 PR，直接输出 PR URL，不重复创建。

The generated PR body must include:

- no executable point generation;
- no executable entry / stop / TP / RR;
- no external channel;
- no Push send;
- no order / execution / auto-trading;
- incomplete-safe / fail-closed;
- Risk Action Guard remains mandatory.

生成的 PR body 必须包含以上安全边界。

The script must not mark ready, merge, switch to the next package, edit files, stage files, or commit.

脚本不得 mark ready、merge、切下一包、改文件、stage 或 commit。

By default the script creates a Draft PR. Use `--ready` only for A-risk docs-only PRs or when the handoff rule explicitly authorizes a ready PR. Use `--draft` to force Draft mode. Use `--base <branch>` to override the default base `main`. Use `--dry-run` to print the intended action without creating a PR.

默认创建 Draft PR。只有 A-risk docs-only 或明确授权时才使用 `--ready` 创建非 Draft PR。`--draft` 强制 Draft，`--base <branch>` 覆盖默认 base `main`，`--dry-run` 只打印动作。

## Merge And Sync / 合并并同步

Use:

```bash
bash scripts/v1-merge-sync.sh <pr-number> "<title (#pr)>"
```

Risk-aware form:

```bash
bash scripts/v1-merge-sync.sh <pr-number> "<title (#pr)>" --risk <A|B|B/C|C> [--confirm]
```

The script checks PR state, draft status, mergeability, and checks before merging an open PR.

脚本在合并 open PR 前检查 PR 状态、Draft 状态、可合并状态和 checks。

If the PR is already `MERGED`, the script enters already-merged sync mode: it switches to `main`, pulls `origin main`, prints status/log, and finishes with `MERGE_SYNC_DONE`.

如果 PR 已经是 `MERGED`，脚本进入已合并同步模式：切回 `main`，pull `origin main`，输出状态和日志，并以 `MERGE_SYNC_DONE` 收尾。

If a PR is closed without being merged, the script must stop with `PR_CLOSED_NOT_MERGED`.

如果 PR 已关闭但未合并，脚本必须以 `PR_CLOSED_NOT_MERGED` 停止。

Risk rules:

- `--risk A`: may merge after checks, mergeability, changed-file scope, and technical review are satisfied.
- `--risk B`, `--risk B/C`, or `--risk C`: must include `--confirm` after the user explicitly approves merge.
- Implementation packages that touch Java, tests, or dashboard behavior must wait for explicit user merge approval.

风险规则：

- `--risk A`：checks、mergeability、changed-file 范围和技术审查满足后可合并。
- `--risk B`、`--risk B/C`、`--risk C`：用户明确同意后必须带 `--confirm`。
- 涉及 Java、测试或 dashboard 行为的 implementation 包必须等待用户明确同意合并。

## Fixed Command Rule / 固定命令规则

Do not handwrite long `gh pr create` commands when `scripts/v1-open-pr.sh` covers the case.

不再手写大段 `gh pr create` 命令。

Do not handwrite long `gh pr merge` commands when `scripts/v1-merge-sync.sh` covers the case.

不再手写大段 `gh pr merge` 命令。

When the GitHub connector is unavailable, GPT should give only these fixed local script commands.

GitHub connector 不可用时，GPT 只给这些固定本地脚本命令。

If a handwritten `gh` command is truly required, the handoff must state why the fixed script does not cover the case.

如果确实必须手写 `gh` 命令，交接说明必须写明固定脚本无法覆盖的原因。

Do not open the next package before the current package is completed on merged `main`.

当前包没有进入 merged `main` 前，不允许开下一包。

When the current package is merged, `main` is synced, the worktree is clean, checks passed, and no blocker exists, GPT should directly provide the next Codex task prompt instead of asking whether to continue.

当前包已 merge、main 已同步、worktree clean、checks 通过且无 blocker 时，GPT 直接给下一包 Codex 提示词，不再问是否继续。

## Legacy Fallback Inventory / 旧兜底脚本清单

The following legacy scripts remain fallback diagnostics or explicit helpers, but they are not the default path for creating PRs or merging PRs:

- `bash scripts/v1.sh`
- `bash scripts/v1-auto.sh`
- `bash scripts/v1-merge-current.sh`

以下旧脚本仍可作为兜底诊断或显式辅助，但不是创建 PR 或合并 PR 的默认路径。

## Non-Scope / 非范围

These scripts do not authorize:

- Java business changes;
- test business changes;
- resources, dashboard, schema, config, or pom changes;
- runtime/live market reads;
- latest price or latest close reads;
- external provider reads;
- RuntimeKlineContext / DataQuality / MultiTimeframe / RiskActionGuard runtime wiring;
- executable entry / stop / TP / RR generation;
- final direction generation;
- external channel;
- Push send;
- order / execution / auto-trading.

这些脚本不授权以上任何业务或运行时能力。
