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

## V1 Auto Operator / V1 自动操作台

Preferred user-facing command:

```bash
bash scripts/v1-auto.sh next
```

`v1-auto.sh` is the Chinese operator entry for routine status, summary, next-task generation, PR checking, and merge handoff. It does not replace the fixed scripts; it delegates to `v1-state.sh`, `codex-next-task.sh`, `v1-open-pr.sh`, and `v1-merge-sync.sh`.

`v1-auto.sh` 不绕过固定脚本，只把状态、下一步、PR 检查和合并交接变成用户可读的中文操作台。

One-command runner entry:

```bash
bash scripts/v1-codex-run-next.sh
```

This generates the next task through `v1-auto.sh next` and starts Codex CLI when available. It never stages, commits, pushes, creates PRs, or merges.

一键执行入口通过 `v1-auto.sh next` 生成下一任务，并在 Codex CLI 可用时启动 Codex。它不会 stage、commit、push、创建 PR 或合并。

If Codex shell cannot confirm Open PR（未合并 PR）status because local `gh` is unavailable, but GPT connector or the user's terminal has confirmed Open PR none, use:

```bash
bash scripts/v1-codex-run-next.sh --open-pr-none-confirmed
```

This flag only bypasses Codex GitHub status unknown. It does not bypass non-main branch, dirty worktree, explicit open PR, failed Main Sync（主分支同步）, `CAN_CONTINUE_NEXT_PACKAGE: NO` from other blockers, or merge approval rules.

如果 Codex shell 因本地 `gh` 不可用无法确认 Open PR 状态，但 GPT connector 或用户本机 terminal 已确认 Open PR none，可使用 `--open-pr-none-confirmed`。该参数只处理 Codex GitHub 状态未知，不绕过其他安全条件。

One-command PR completion entry:

```bash
bash scripts/v1-pr-complete.sh <PR_NUMBER> <A|B|C> "<SUBJECT>" [--confirm-reviewed]
```

This runs `v1-auto.sh check-pr <PR_NUMBER> <risk>`, waits for required CI, and then uses `v1-merge-sync.sh` when the risk rule allows merge.

PR 完成入口会先运行风险感知 PR 检查，等待必需 CI，再在风险规则允许时通过 `v1-merge-sync.sh` 合并。

`v1-pr-complete.sh` accepts the current `gh pr checks` JSON shape where check results are reported through `state`, and remains compatible with a future `conclusion` field. It requires every matching `quality-gate` check to pass when both push and pull_request entries exist, and requires `workflow-contract` to pass before any A-risk auto merge.

`v1-pr-complete.sh` 兼容当前 `gh pr checks` 使用 `state` 字段的输出，也兼容未来可能出现的 `conclusion` 字段。当 push 和 pull_request 同时产生多个 `quality-gate` 时，所有同名检查都必须成功；`workflow-contract` 也必须成功后才允许 A-risk 自动合并。

B-risk semantic checks distinguish positive forbidden behavior from negative safety assertions.
（B-risk 语义检查会区分正向禁用行为和负向安全断言。）

Allowed negative safety assertions include `.doesNotExist()`, `does not expose`, `does not contain`, `No final direction`, `No entry`, `No stop`, `No TP`, `No RR`, `notTradingSignal`, `notCandidateSignal`, `notDecisionGeneration`, `notPointSignal`, `notExecutable`, `externalRefreshTriggered=false`, `displaySlotsAreCandidatePool=false`, `failClosed`, forbidden-scope copy, and tests that assert forbidden fields are absent.

允许的负向安全断言包括 `.doesNotExist()`、`does not expose`、`does not contain`、`No final direction`、`No entry`、`No stop`、`No TP`、`No RR`、`notTradingSignal`、`notCandidateSignal`、`notDecisionGeneration`、`notPointSignal`、`notExecutable`、`externalRefreshTriggered=false`、`displaySlotsAreCandidatePool=false`、`failClosed`、禁止范围文案，以及测试中的禁用字段不存在断言。

Positive additions such as `finalDirection`, `entryPrice`, `stopPrice`, `takeProfit`, `riskReward`, `positionSize`, `leverage`, `orderAction`, `executionAction`, `autoTradingAction`, `candidateRanking`, `pushSend`, scheduler / collector / API-client triggers, or `externalRefreshTriggered=true` still stop the B-risk flow.

正向新增 `finalDirection`、`entryPrice`、`stopPrice`、`takeProfit`、`riskReward`、`positionSize`、`leverage`、`orderAction`、`executionAction`、`autoTradingAction`、`candidateRanking`、`pushSend`、scheduler / collector / API-client trigger 或 `externalRefreshTriggered=true` 仍会停止 B-risk 流程。

Dirty work package helper:

```bash
bash scripts/v1-package-dirty-work.sh
```

If Codex wrote files but did not create the branch / commit / PR, this helper reads `docs/CODEX_NEXT_TASK.yml`, switches or creates the configured task branch while preserving the dirty worktree, stages only the files allowed by the declared risk, commits, pushes, and calls `v1-auto.sh pr`. It stops for C-risk, forbidden staged paths, open PRs, unknown GitHub PR state, or disallowed changed files.

如果 Codex 已写文件但没有成功创建分支 / commit / PR，该脚本会读取 `docs/CODEX_NEXT_TASK.yml`，保留 dirty worktree 并切换或创建目标任务分支，只 stage 当前 risk 允许的文件，commit、push，并调用 `v1-auto.sh pr`。遇到 C-risk、已 stage 禁止路径、open PR、GitHub PR 状态未知或不允许的变更文件时会停止。

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
