# V1 Auto Operator Workflow

## 1. Executive Summary

`scripts/v1-auto.sh` 是 Trade Model V1 的中文工作流操作台入口。

以后用户优先运行：

```bash
bash scripts/v1-auto.sh next
```

它会用中文输出当前模块、当前阶段、已完成内容、下一步、项目阶段、Worktree Clean（工作区干净）、Open PR（未合并 PR）、Main Sync（主分支同步）、是否能继续、Blockers（阻塞）。

本包只是 workflow efficiency package（工作流提效包）。它不提升业务 capability level，不改变 Java、tests、dashboard business logic、schema/config/pom，也不接 Push / Candidate / Decision generation / Point / Trading。

## 2. Common Commands

| Command | Purpose |
|---|---|
| `bash scripts/v1-auto.sh next` | 日常入口：状态 + 白话摘要 + 下一步 Codex 任务。 |
| `bash scripts/v1-auto.sh status` | 读取 `v1-state.sh`、`ACTIVE_MAINLINE_STATUS.yml`、`CODEX_NEXT_TASK.yml` 并输出中文状态。 |
| `bash scripts/v1-auto.sh summary` | 输出项目总进度白话摘要。 |
| `bash scripts/v1-auto.sh task` | 调用 `codex-next-task.sh`，把下一步任务写入临时文件并尽量复制到剪贴板。 |
| `bash scripts/v1-auto.sh pr <branch> "<title>" <risk>` | 通过 `v1-open-pr.sh` 创建 Pull Request（拉取请求）；如果 Remote Branch（远端分支）缺失且当前本地分支就是目标分支，会先 Git Push（Git 分支推送）。 |
| `bash scripts/v1-auto.sh check-pr <PR_NUMBER>` | 检查 PR 状态、checks、changed files 和 A-risk 文件边界。 |
| `bash scripts/v1-auto.sh merge <PR_NUMBER> "<title>" <risk> [--confirm]` | 通过 `v1-merge-sync.sh` 合并并同步。 |
| `bash scripts/v1-auto.sh help` | 显示中文帮助和当前摘要。 |

## 3. Delegation Rules

`v1-auto.sh` 不能绕过已有固定脚本：

- 状态必须通过 `bash scripts/v1-state.sh`。
- 下一任务必须通过 `bash scripts/codex-next-task.sh`。
- 创建 PR 必须通过 `bash scripts/v1-open-pr.sh`。
- 合并同步必须通过 `bash scripts/v1-merge-sync.sh`。

`v1-auto.sh pr` 在创建 Pull Request（拉取请求）前会检查 Remote Branch（远端分支）。如果远端分支不存在，但当前本地分支正是目标分支，它会先执行 `git push -u origin <branch>`，再调用 `v1-open-pr.sh`。如果当前本地分支不是目标分支，它会 STOP（停止）并提示用户切换分支。

如果 Codex shell 输出 `GH_NOT_AVAILABLE`，它只表示 Codex GitHub 状态未知，不等于项目失败。用户本机 `gh` 或 GPT connector 可以作为 handoff evidence。

## 4. Risk Rules

- A-risk: docs/scripts workflow-only 或 docs-only 低风险包；仍必须检查 changed files，不能包含 Java/tests/dashboard/schema/config/pom 业务路径。
- B-risk: Java/test/dashboard implementation；默认不自动合并，必须用户明确批准。
- B/C-risk: 升级风险；默认不自动合并，必须用户明确批准。
- C-risk: 高风险或默认禁用范围；默认不自动合并。

`v1-auto.sh merge` 对 B/B/C/C 默认停止；只有用户明确同意后才可带 `--confirm`。

## 5. Auto Stop Conditions

以下情况必须 STOP（停止）或输出阻塞，不得继续自动流程：

- Open PR（未合并 PR）不是 `none`，除非正在执行 `check-pr` / `merge` 当前 PR。
- Main Sync（主分支同步）不是 `OK`。
- Worktree Clean（工作区干净）不是 `Yes`。
- Blockers（阻塞）不是 `none`。
- A-risk 自动流程的 changed files 包含 Java/tests/dashboard/schema/config/pom。
- PR 不是 OPEN（打开）状态。
- PR 不是 MERGEABLE（可合并）。
- B/B/C/C merge 没有用户明确 `--confirm`。

## 6. Non-Scope

`v1-auto.sh` 不授权也不会实现：

- Java business code
- tests
- dashboard business logic
- schema/config/pom
- Push / external channel
- Candidate generation
- Decision generation
- Point generation
- final direction / entry / stop / TP / RR
- order / execution / auto-trading
- DTO / Validator / Assembler / Orchestrator
- P359 / P360

## 7. Completion Rule

`v1-auto.sh` 不改变 merged main 才算完成的规则。

Branch pushed、PR created、CI green、Codex output 都不算完成。只有包合并到 `main`，本地 main 同步，worktree clean，且无 open blocker，才可以继续下一包。

## 8. Current Project Handoff

当前已完成四个 review-only runtime 小闭环：

1. PositionSync（持仓同步）
2. Watchlist + RuleConfig（观察列表 + 规则配置）
3. MarketQuote（行情报价）
4. Evidence / Score（证据 / 评分）

DecisionResult（决策结果）已完成 implementation（实现）和 verification（验证）。V1 Auto Operator Pack 合并后，业务下一步恢复 `DecisionResult Visual Verification / Closure`。
