# Product-First Workflow Entry

The standard command order for every editable task is:

```bash
bash scripts/product-source-gate.sh
git branch --show-current
git status --short
git rev-parse HEAD
# task-specific read/audit checks
# authorized implementation only after the gate passes
# task-specific validation, product gate, workflow contract, diff and scope checks
```

`scripts/check-workflow-contract.sh` invokes the Product Source Gate first. Before editing, implementation, test modification, PR creation, Ready transition, merge gate, or deployment, the gate must pass for the declared task mapping.

If the gate fails, the workflow must report:

```text
WORKFLOW_STATUS:
BLOCKED_BY_PRODUCT_SOURCE_GATE
```

and must not enter editing.

The gate only verifies registered files/hashes, task mappings, and explicit hard boundaries. It must not parse all natural-language semantics, enumerate synonyms, build semantic inventories/digests, create a new governance mainline, or claim that an agent understood the product. A read-only product-gap audit remains fail closed for a missing/failed source gate, dirty worktree, unsynced main, any active non-current open PR, or editable scope. Closed unmerged technical debt is not an active blocker and is never effective/current content.

---

# Contract-First Workflow Automation

Workflow automation must read facts in this priority order:

1. `docs/PRODUCT_SOURCE_OF_TRUTH.md` for product direction and registered product sources
2. `docs/PROJECT_DELIVERY_CONTRACT.md` for delivery controls
3. `docs/DELIVERY_PROGRESS_MATRIX.md`
4. `docs/PROJECT_CURRENT_STATE.md`
5. derived compatibility files (`docs/ACTIVE_MAINLINE_STATUS.yml`, `docs/CODEX_NEXT_TASK.yml`)
6. legacy V1 documents as historical audit and asset evidence only

`v1-auto.sh`, `v1-state.sh`, and `codex-next-task.sh` must not use review-only slice count as delivery progress or to select the next business package.
`ACTIVE_MAINLINE_STATUS.yml` no longer independently defines the current task; it is a derived compatibility mirror.
Fixed PR and merge helpers remain available, but they do not mark phases DONE.
Token leakage remains a hard stop. No auto-trading is allowed.

---

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
- `ACTIVE_CONFLICTING_OPEN_PRS:`
- `CLOSED_TECHNICAL_DEBT_STATUS:`
- `CLOSED_TECHNICAL_DEBT_EFFECTIVE:`
- `CLOSED_TECHNICAL_DEBT_BLOCKS_AUDIT:`
- `MAIN_SYNC:`
- `CURRENT_TASK_MODE:`
- `AUTHORIZED_NEXT_TASK_MODE:`
- `NEXT_TASK_AUTHORIZATION_STATUS:`
- `P1A_TRANSITION_ALLOWED:`
- `PRODUCT_AUDIT_ALLOWED:`
- `READ_ONLY_PRODUCT_AUDIT_STATUS:`
- `CAN_CONTINUE_NEXT_PACKAGE:`
- `BLOCKERS:`

脚本输出当前分支、工作区是否干净、HEAD、最近提交、open PR、main 同步状态、是否可以进入下一包，以及阻塞原因。

If `gh` is unavailable, it prints `GH_NOT_AVAILABLE` in the open PR field and must not produce unreadable errors.

如果 `gh` 不可用，脚本在 open PR 字段输出 `GH_NOT_AVAILABLE`，不得输出不可读错误。

When `gh CLI` is available and open PR count is `0`, `GH_NOT_AVAILABLE` must not remain as a next-business-phase blocker. The state output must report `OPEN_PR_CHECK_SOURCE=gh CLI`, `OPEN_PR_COUNT=0`, and `OPEN_PR_STATUS=NONE`. If open-PR state is unavailable, audit and implementation gates remain fail closed. Every active non-current open PR is conflicting. Closed unmerged technical debt is not in the active open-PR set, does not block a read-only audit, and is never effective/current content.

当 `gh CLI` 可用且 open PR（未合并 PR）数量为 `0` 时，`GH_NOT_AVAILABLE`（GitHub 状态不可用）不得继续阻塞下一业务阶段。状态输出必须报告 `OPEN_PR_CHECK_SOURCE=gh CLI`、`OPEN_PR_COUNT=0`、`OPEN_PR_STATUS=NONE`。若 open PR 状态不可用，审计与实现都失败关闭。任何非当前活动 open PR 都按冲突处理；已关闭且未合并的技术债不阻塞只读审计，也绝不作为已生效或当前内容。

### Read-Only Product Audit Boundary / 只读产品审计边界

The persisted task contract separates `current_task_mode=PRODUCT_FOUNDATION_REMEDIATION` from `authorized_next_task_mode=READ_ONLY_PRODUCT_AUDIT`. P0 open or Ready/unmerged remains blocked. After P0 is effective on clean/synced merged main, merged-main validation and Product Source Gate pass, `v1-state.sh` derives the effective mode as P1A without a second YAML edit. P1A remains read-only and P1B remains unauthorized.

`PRODUCT_AUDIT_ALLOWED=YES` requires all of the following:

- effective `TASK_MODE: READ_ONLY_PRODUCT_AUDIT` after the explicit P0-to-P1A transition;
- Product Source Gate `PASS`;
- the exact `read_only_product_audit_scope_contract` forbidding code/test changes, business PR creation, and closed-debt recovery-content changes;
- `p1a_repository_edits_allowed=false`, `p1a_implementation_allowed=false`, and `p1a_implementation_pr_allowed=false`;
- clean worktree and clean/synced `main`;
- no current business-package PR;
- no `ACTIVE_CONFLICTING_PR`.

The state output reports `CLOSED_TECHNICAL_DEBT_BLOCKS_AUDIT=NO` and `CLOSED_TECHNICAL_DEBT_EFFECTIVE=NO`. Those fields record non-effectivity only; audit permission never authorizes implementation, Ready transition, merge, deployment, code/test changes, reopening debt, or applying its stash/patch.

Run the deterministic transition and active-open/closed-debt boundary cases with:

```bash
bash scripts/v1-state.sh --self-test-product-audit-policy
```

The self-test covers P0 open, Ready/unmerged, merged/unsynced, merged/validated, P1B blocking, closed-unmerged technical debt, current and active-conflicting PRs, dirty worktree, failed Product Source Gate, implementation attempts, and attempted editable scope. It is a small state-policy test, not a natural-language parser, inventory, digest, or governance engine.

## V1 Auto Operator / V1 自动操作台

Primary human-facing command:

```bash
bash scripts/v1-go.sh
```

`v1-go.sh` is the default human operator UX entry（人工总控体验入口）. It reuses the existing fixed scripts instead of replacing them:

- clean `main` + no Open PR（未合并 PR）: delegates to `v1-operator.sh`, generates the next Codex task, and copies the full task to the macOS clipboard with `pbcopy` if Codex CLI cannot start;
- dirty non-main task branch（脏任务分支）: delegates to `v1-package-dirty-work.sh`, reads the created Pull Request（拉取请求）number, and continues the A/B risk flow automatically;
- existing Open PR（已有未合并 PR）: reads PR number/title/risk, auto-completes A-risk, and prints a B-risk GPT review summary without requiring manual PR-number substitution.

For reviewed B-risk merges, use:

```bash
bash scripts/v1-go.sh --confirm-reviewed <PR_NUMBER>
```

For read-only status:

```bash
bash scripts/v1-go.sh --status
```

Users should not copy long commands, manually `cat` task files, or manually replace `<PR_NUMBER>` in the normal path. Codex task text is copied to the clipboard when possible; if `pbcopy` is unavailable, the full task text is printed directly.

Underlying operator command:

```bash
bash scripts/v1-operator.sh
```

`v1-operator.sh` is the preferred one-command terminal orchestrator. It checks state, reads `docs/CODEX_NEXT_TASK.yml`, creates or switches the task branch from clean `main`, starts Codex when available or prints the full task, packages dirty work, opens PRs, and delegates PR completion to `v1-pr-complete.sh`.

`v1-operator.sh` 是首选一键总控入口。它检查状态、读取 `docs/CODEX_NEXT_TASK.yml`、从干净 `main` 创建或切换任务分支、在可用时启动 Codex 或打印完整任务、打包 dirty worktree（脏工作区）、创建 PR，并把 PR 完成流程交给 `v1-pr-complete.sh`。

For reviewed B-risk merges:

```bash
bash scripts/v1-operator.sh --confirm-reviewed <PR_NUMBER>
```

B-risk（实现包）经 GPT / 人工明确复核后，使用该命令继续合并。总控入口不会绕过 `v1-merge-sync.sh`。

Older direct status command:

```bash
bash scripts/v1-auto.sh next
```

`v1-auto.sh` is the Chinese operator entry for routine status, summary, next-task generation, PR checking, and merge handoff. It does not replace the fixed scripts; it delegates to `v1-state.sh`, `codex-next-task.sh`, `v1-open-pr.sh`, and `v1-merge-sync.sh`.

`v1-auto.sh` 不绕过固定脚本，只把状态、下一步、PR 检查和合并交接变成用户可读的中文操作台。

`v1-auto.sh summary` and `v1-auto.sh next` read the Project Delivery Contract, Delivery Progress Matrix, Project Current State, and derived task handoff. They do not use Review-Only Runtime partial slice count as delivery progress or next-business-package selection.

`v1-auto.sh summary` 和 `v1-auto.sh next` 读取项目交付契约、交付进度矩阵、当前状态和派生任务交接文件，不再用 Review-Only Runtime partial（只读运行时部分完成）小闭环数量判定交付进度或选择下一业务包。

`v1-auto.sh summary`, `v1-auto.sh next`, and `codex-next-task.sh` must surface the Fixed Codex Output Contract（Codex 固定输出契约） hints:

- `WHAT_THIS_STEP_DOES（这一步在做什么）`
- `CURRENT_PROGRESS（当前进度）`
- `NEXT_ALLOWED_ACTION（下一允许动作）`
- `NEXT_BLOCKED_ACTION（下一禁止动作）`

These hints are output guidance only. They must not change gate（门禁）判断规则, P0-2 allowed（允许）判断, merge rules, or business capability.

Baseline sync PRs（基线同步 PR） are no longer part of the normal workflow.
If `Source of Truth current_head` lags behind the actual clean / synced `main` HEAD, and `Open PR` is `none`, operators use actual HEAD as the Effective execution baseline（实际执行基线）.
The next business package updates source-of-truth files opportunistically while doing its scoped work.

baseline sync PR（基线同步 PR）不再作为常规流程。只要当前在 clean / synced main、Open PR none，脚本使用 actual HEAD（实际 HEAD）作为实际执行基线；后续业务包在自身范围内顺手更新 source-of-truth（事实源），不再单独创建同步小包。

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

A-risk Auto Merge Rule（A-risk 自动合并规则）:

```bash
bash scripts/v1-pr-complete.sh 1005 A "P0-0 Contract Delivery Package and Workflow Migration"
bash scripts/v1-auto.sh complete-pr 1005 A "P0-0 Contract Delivery Package and Workflow Migration"
```

For A-risk docs / contract / workflow packages, the helper may complete PR check, CI wait, merge, local main sync, and effective-state verification only when the PR is not Draft, the target PR is the current package PR, Maven / workflow / task validation passed, PR checks passed, and changed files are limited to docs / workflow scripts / contract files. Java, tests, schema, dashboard, pom, runtime config, business logic, external channel, Push send, order, execution, and auto-trading remain forbidden.

Unrelated Draft PRs do not block the current package merge. Unrelated Draft PRs still block the next business phase. PR #1004 is unrelated to the P0-0 package; it must not be modified, merged, closed, or used as the target PR, and it must still block P0-1 while open.

A-risk 文档 / 契约 / 工作流包可以通过以上固定命令完成 PR 检查、CI 等待、合并、本地 main 同步和生效验证。无关 Draft PR 不阻止当前包合并，但仍阻止下一业务阶段；PR #1004 不得被处理。

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

When the current branch is not `main` and the worktree is dirty, `v1-package-dirty-work.sh` treats the current branch as the current package branch. A dirty `CODEX_NEXT_TASK.yml` may already point to the next phase and must not override the current package branch.

当当前分支不是 `main` 且工作区为 dirty（脏）时，`v1-package-dirty-work.sh` 优先把当前分支识别为当前包分支。dirty 的 `CODEX_NEXT_TASK.yml` 可能已经指向下一阶段，不得覆盖当前包分支。

## Autonomous Delivery / 自主交付

Use the autonomous delivery helpers when Codex has completed a scoped task and the user wants the fastest safe path from local changes to merged `main`.

```bash
bash scripts/v1-delivery-check.sh
```

`v1-delivery-check.sh` is read-only. It refuses the wrong project path, runs `./mvnw test -q`, runs `bash scripts/v1-state.sh`, and prints a compact machine-readable status. It does not stage, commit, push, create PRs, merge, delete branches, reset, clean, or edit files.

Normal full delivery command:

```bash
bash scripts/v1-autodeliver.sh full \
  --branch codex/p3-2b-dashboard-manual-userposition-binding \
  --commit "feat(dashboard): bind manual user positions to dashboard" \
  --title "feat(dashboard): bind manual user positions to dashboard" \
  --body-file /tmp/pr-body.md \
  --allow src/main/java \
  --allow src/test/java
```

Supported modes:

- `check`: delegates to `v1-delivery-check.sh` and performs no mutation.
- `ship`: validates, stages only allowlisted files, commits, pushes, and creates a ready PR.
- `merge`: waits for GitHub checks, refuses Draft/conflict/blocked PRs, squash merges, deletes the remote branch through GitHub, switches to `main`, pulls `origin main`, and runs final `v1-state`.
- `full`: runs `ship` and then `merge`.
- `resume`: resumes a branch after commit / push / PR creation / checks / merge interruption, reuses an existing PR when present, creates the PR when missing, waits for checks, merges, syncs `main`, and runs final delivery check.

### Full Mode / full 模式

Use `full` only before the task branch has been successfully pushed and handed to GitHub PR flow.

只在任务分支尚未成功 push 并进入 GitHub PR 流程前使用 `full`。

`full` is allowed to commit local allowlisted changes, push the branch, create or reuse the PR, wait for checks, squash merge, delete the remote branch through GitHub, switch to `main`, pull `origin main`, and run the final delivery check.

`full` 可以提交本地 allowlist（允许范围）内变更、push 分支、创建或复用 PR、等待 checks、squash merge、通过 GitHub 删除远端分支、切回 `main`、pull `origin main`，并运行最终 delivery check。

### Resume Mode / resume 模式

If the branch has already been pushed, do not rerun `full`. Use:

```bash
bash scripts/v1-autodeliver.sh resume --branch <branch>
```

如果分支已经 push，不要重新跑 `full`，改用 `resume`。

`resume` is idempotent for the common interrupted states:

- committed but not pushed: pushes the branch;
- pushed but PR missing: creates the PR using provided `--title` / `--body-file` or safe fallback text;
- PR already open: reuses the PR and waits for checks;
- checks already passed: proceeds to merge after draft / mergeability / conflict checks;
- PR already merged: switches to `main`, pulls `origin main`, runs delivery check, and reports done;
- remote branch already deleted after merge: treats the merged PR as done and syncs `main`.

`resume` 针对常见中断状态是幂等的：已 commit 未 push、已 push 但 PR 缺失、PR 已存在、checks 已通过、PR 已合并、远端分支合并后已删除，都应继续或报告完成，而不是重复 commit 或重复创建 PR。

### GitHub EOF / Transient API Failure

`v1-autodeliver.sh` wraps GitHub CLI API calls with bounded retry for:

- `gh pr create`
- `gh pr list`
- `gh pr view`
- `gh pr checks`
- `gh pr merge`

It retries transient EOF / timeout / GitHub API 5xx style failures, prints the attempt count, sleeps between attempts, and stops after the configured retry limit.

如果 GitHub 返回 `Post "https://api.github.com/graphql": EOF` 或类似 timeout / transient API failure（临时 API 失败），不要手动去浏览器创建 PR 或合并。直接运行：

```bash
bash scripts/v1-autodeliver.sh resume --branch <branch>
```

Rule: do not rerun `full` after a branch has already been pushed; use `resume`.

规则：分支已经 push 之后不要重跑 `full`，必须用 `resume`。

### Output To Paste Back / 需要贴回的输出

When automation stops or succeeds, paste back only the compact summary fields:

- `AUTODELIVER_STATUS`
- `MODE`
- `BRANCH`
- `COMMIT`
- `PR_NUMBER`
- `PR_URL`
- `SHIP_STATUS`
- `CHECKS_STATUS`
- `MERGE_STATUS`
- `FINAL_BRANCH`
- `DELIVERY_CHECK_STATUS`
- `NEXT_STEP`
- `RESUME_COMMAND` when present

What Codex should do automatically:

- run `bash scripts/v1-delivery-check.sh` before or after implementation when a compact local status is useful;
- after completing a scoped task on a non-main branch, prepare `/tmp/pr-body.md`;
- run `v1-autodeliver.sh ship` or `full` only when the user has authorized autonomous local delivery for that task;
- pass every intended changed-file prefix through repeated `--allow`;
- paste back only the compact summary, PR number, failing check tail, or final state fields.

What the user needs to paste back:

- nothing when `full` succeeds;
- the `AUTODELIVER_STATUS`, `REASON`, `PR_NUMBER`, and failing log tail when it stops;
- any manual GitHub review or merge approval that the current task explicitly requires.

Automation must stop when:

- current path is not `/Users/xuchao/Documents/trade-model-v1`;
- ship mode is running on `main`;
- Maven tests fail;
- `v1-state` reports blockers other than expected dirty-worktree blockers before commit;
- changed files are outside the explicit `--allow` prefixes;
- `gh` is unavailable or unauthenticated for PR/push/merge steps;
- GitHub checks fail;
- the PR is Draft;
- the PR is not mergeable or has merge conflict / blocked merge state;
- force push would be required but `--force-with-lease` was not explicitly passed.

Resume after failure:

- wrong path: `cd /Users/xuchao/Documents/trade-model-v1` and rerun.
- tests failed: keep the same branch, fix the scoped files, rerun `ship` or `full`.
- disallowed path: inspect `git status --short`, decide whether to revert manually or add an explicit safe `--allow`.
- push/PR creation failed after commit or GitHub returned EOF: run `resume --branch <branch>`.
- checks failed after PR creation: fix on the same branch, push, then run `resume --branch <branch>`.
- merge failed because PR is Draft, conflicted, or blocked: resolve that GitHub state, then run `resume --branch <branch>` or `merge --pr <number>`.

Safety boundaries:

- no `git reset`;
- no `git clean`;
- no local file deletion;
- no default force push;
- no auto-trading, order, execution, Push send, external channel, or business-capability change.

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
