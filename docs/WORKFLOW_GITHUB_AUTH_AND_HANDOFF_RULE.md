# Workflow GitHub Auth And Handoff Rule

This document is the single handoff rule for GitHub connector, Codex, and local `gh` responsibilities in Trade Model V1.

本文档是 Trade Model V1 中 GitHub connector、Codex、本地 `gh` 的统一交接规则。

It does not change business capability, runtime wiring, Java code, tests, dashboard, schema, config, external channels, Push, order, execution, or auto-trading.

它不改变业务能力、运行时接线、Java、测试、dashboard、schema、config、external channel、Push、order、execution 或 auto-trading。

## Completion State / 完成态规则

Only merged `main` counts as completed.

只有 merged `main` 才算完成。

The following are not completed states:

- branch pushed;
- PR created;
- PR approved;
- CI green but not merged;
- main not pulled / synced after merge;
- worktree not clean;
- Issue created;
- Draft PR;
- Codex output;
- GPT plan;
- chat memory.

以下都不算完成：

- branch 已 push；
- PR 已创建；
- PR 已 approve；
- CI 通过但未 merge；
- merge 后 main 未 pull / sync；
- worktree 不 clean；
- Issue 已创建；
- Draft PR；
- Codex 输出；
- GPT 计划；
- 聊天记忆。

If a new window sees conflict between chat memory, `ACTIVE_MAINLINE_STATUS.yml`, open PRs, or local branches, merged `main` wins.

如果新窗口发现聊天记忆、`ACTIVE_MAINLINE_STATUS.yml`、open PR 或本地 branch 相互冲突，以 merged `main` 为准。

## Responsibility Split / Codex、GPT、本地 gh 分工

Codex is responsible by default for:

- editing files inside the allowed scope;
- running allowed checks;
- committing;
- pushing the branch;
- reporting branch / commit / changed files / checks / overreach status;
- stopping when asked not to create Issue or PR.

Codex 默认负责：

- 在允许范围内改文件；
- 运行允许的检查；
- commit；
- push branch；
- 输出 branch / commit / changed files / checks / 是否越界；
- 当任务要求不创建 Issue 或 PR 时停止。

Codex is not always responsible for creating Issues, creating PRs, marking PRs ready, or merging.

Codex 不总是负责创建 Issue、创建 PR、标记 ready 或 merge。

GPT is responsible for:

- deciding the current package;
- checking whether an existing PR / branch / Issue blocks progress;
- reviewing PRs;
- deciding whether a PR can merge under A/B/C rules;
- generating the next Codex task prompt only after the current package is completed on merged `main`;
- creating PRs / merging through the GitHub connector when available;
- giving the user one local `gh` command when the connector is unavailable.

GPT 负责：

- 判断当前包；
- 检查是否存在阻塞推进的 PR / branch / Issue；
- 审查 PR；
- 按 A/B/C 规则判断是否可 merge；
- 只有当前包进入 merged `main` 后才生成下一包 Codex prompt；
- GitHub connector 可用时通过 connector 创建 PR / merge；
- connector 不可用时给用户一条本地 `gh` 命令。

The user's local `gh` is the fallback when:

- GitHub connector is unavailable;
- Codex GitHub auth is invalid or unavailable;
- a command such as `gh pr create`, `gh pr ready`, or `gh pr merge` is needed;
- GPT provides a single command for the user to run.

用户本地 `gh` 是以下情况的兜底：

- GitHub connector 不可用；
- Codex GitHub auth 失效或不可用；
- 需要 `gh pr create`、`gh pr ready` 或 `gh pr merge`；
- GPT 给出一条命令让用户执行。

The user should not manually judge mergeability, CI, changed files, or workflow order.

用户不需要手动判断 mergeability、CI、changed files 或流程顺序。

## PR Creation Priority / PR 创建优先级

PR creation priority is:

1. If GPT GitHub connector is available, GPT creates the Issue / Draft PR.
2. If GPT connector is unavailable but local `gh` is available, GPT gives one `gh pr create` command.
3. If Codex GitHub auth is available and the task explicitly asks Codex to create the Issue / PR, Codex may create them.
4. If Codex GitHub auth is unavailable, Codex must not force Issue / PR creation.
5. If both connector and local `gh` are unavailable, pause the current package and do not open the next package.

PR 创建优先级：

1. GPT GitHub connector 可用时，由 GPT 创建 Issue / Draft PR。
2. GPT connector 不可用但本地 `gh` 可用时，由 GPT 给一条 `gh pr create` 命令。
3. Codex GitHub auth 可用且任务明确要求 Codex 创建 Issue / PR 时，Codex 可以创建。
4. Codex GitHub auth 不可用时，Codex 不得硬创建 Issue / PR。
5. connector 和本地 `gh` 都不可用时，暂停当前包，不开下一包。

## Merge Priority / 合并优先级

Merge priority is:

1. If GPT GitHub connector is available, GPT reviews the PR and merges according to A/B/C rules.
2. If connector is unavailable but local `gh` is available, GPT gives one command that performs the approved ready / merge / pull sequence.
3. If both connector and local `gh` are unavailable, pause and do not continue to the next package.

合并优先级：

1. GPT GitHub connector 可用时，GPT 审 PR 并按 A/B/C 规则 merge。
2. connector 不可用但本地 `gh` 可用时，GPT 给一条完成 ready / merge / pull 的命令。
3. connector 和本地 `gh` 都不可用时，暂停，不继续下一包。

## A/B/C Merge Authorization / A/B/C 合并授权

A-risk docs-only PRs may be merged directly after technical review if all of the following are true:

- CI is green;
- changed files match the declared docs-only scope;
- no overreach exists;
- no forbidden runtime / service / dashboard / external / execution logic is touched.

A 档 docs-only PR 在以下条件全部满足时，技术审查后可直接合并，不再问用户确认：

- CI 通过；
- changed files 符合声明的 docs-only 范围；
- 无越界；
- 未触碰 runtime / service / dashboard / external / execution 等禁止逻辑。

B-risk Java/test PRs require explicit user approval before merge, even if CI is green.

B 档 Java/test PR 即使 CI 通过，也必须等用户明确同意后才能 merge。

B/C or C-risk PRs require the user to say exactly or equivalently: `同意合并 PR #xxx`.

B/C 或 C 档 PR 必须等用户明确说 `同意合并 PR #xxx` 或等价确认。

External channel, Push send, order, execution, and auto-trading are C-risk or disabled by default.

external channel、Push send、order、execution、auto-trading 默认属于 C 档或禁用。

Auto-trading is not part of current V1.

自动交易不属于当前 V1。

## No Skip Rule / 禁止跳包规则

Do not open the next package if any of these are true:

- branch not pushed;
- PR not created;
- PR not reviewed;
- PR not merged;
- main not synced after merge;
- worktree not clean;
- CI not green;
- B/C or C-risk merge lacks explicit user approval;
- PR is Draft and not ready;
- PR is not cleanly mergeable;
- PR conflicts with main;
- current package status is unclear;
- current package exists only in chat memory.

只要存在以下任一状态，不允许开下一包：

- branch 未 push；
- PR 未创建；
- PR 未审；
- PR 未合并；
- merge 后 main 未同步；
- worktree 不 clean；
- CI 未通过；
- B/C 或 C 档未获用户明确合并授权；
- PR 仍是 Draft 且未 ready；
- PR 不是 cleanly mergeable；
- PR 与 main 有冲突；
- 当前包状态不清楚；
- 当前包只存在于聊天记忆。

## Main Clean Auto-Next Rule / main clean 后自动下一步

When all of the following are true, GPT must not ask "whether to continue":

- current package is merged;
- main is pulled / synced;
- worktree is clean;
- CI passed for the merged PR;
- no open blocker exists.

满足以下全部条件时，GPT 不再问“是否下一步”：

- 当前包已 merge；
- main 已 pull / sync；
- worktree clean；
- 已合并 PR 的 CI 通过；
- 没有 open blocker。

In that state, GPT must directly generate the next maximum safe Codex task prompt.

在该状态下，GPT 必须直接生成下一包最大安全 Codex 任务提示词。

## Token Exposure Rule / token 泄露规则

If a GitHub token is pasted into chat, logs, PR comments, terminal output, or any shared transcript:

- immediately treat it as compromised;
- do not repeat the token;
- revoke it in GitHub;
- create a new token only outside chat;
- do not send the new token to chat.

如果 GitHub token 被粘贴到聊天、日志、PR 评论、终端输出或任何共享文本中：

- 立即视为泄露；
- 不得复述 token；
- 必须在 GitHub 中 revoke；
- 只能在聊天外重新创建 token；
- 新 token 不得发送到聊天。

Minimum scopes for the normal fallback token are:

- `repo`;
- `workflow`;
- `read:org` if `gh auth` reports `missing required scope read:org`.

正常兜底 token 的最小 scope：

- `repo`；
- `workflow`；
- 如果 `gh auth` 报 `missing required scope read:org`，补 `read:org`。

Recommended safe login patterns:

```bash
read -s GH_TOKEN
printf "%s" "$GH_TOKEN" | gh auth login --with-token
unset GH_TOKEN
```

or:

```bash
pbpaste | gh auth login --with-token
```

推荐使用以上方式登录，避免 token 出现在 shell history 或聊天中。

## New Window Handoff Rule / 新窗口续接规则

Every new window must verify:

- current branch;
- worktree clean state;
- `git log --oneline -5`;
- current open PR, if any;
- last merged PR;
- whether the current package is merged;
- whether main is synced;
- whether there is an open blocker.

每个新窗口必须确认：

- current branch；
- worktree 是否 clean；
- `git log --oneline -5`；
- 当前 open PR，如有；
- last merged PR；
- 当前包是否已 merge；
- main 是否已同步；
- 是否有 open blocker。

Do not rely only on `docs/ACTIVE_MAINLINE_STATUS.yml` to mark an unmerged package as completed.

不得只凭 `docs/ACTIVE_MAINLINE_STATUS.yml` 把未合并包当完成。

Merged `main` is the only source of truth.

merged `main` 是唯一 source of truth。

## Forbidden Scope / 禁止范围

This workflow rule does not authorize:

- Java changes;
- test changes;
- resources changes;
- dashboard changes;
- schema / config / pom changes;
- service / controller / mapper / repository / scheduler changes;
- runtime wiring;
- market data reads;
- external channel;
- Push send;
- order;
- execution;
- auto-trading.

本工作流规则不授权：

- Java 修改；
- test 修改；
- resources 修改；
- dashboard 修改；
- schema / config / pom 修改；
- service / controller / mapper / repository / scheduler 修改；
- runtime wiring；
- market data reads；
- external channel；
- Push send；
- order；
- execution；
- auto-trading。
