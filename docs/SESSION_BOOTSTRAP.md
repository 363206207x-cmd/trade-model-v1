# Product-First Session Bootstrap

Every new task starts in this exact order:

1. **Repository Identity** — confirm canonical repository, branch, exact Head, local/origin relationship, index, worktree, merge/rebase/cherry-pick state, and relevant PR state.
2. **Product Source Gate** — run `bash scripts/product-source-gate.sh`; editing is forbidden when it returns `BLOCKED`.
3. **Product Sources Read** — read `docs/PRODUCT_SOURCE_OF_TRUTH.md` and all `required_product_sources` for the task.
4. **Product Contract Mapping** — map module, source chapters, required meanings, identities, state boundaries, privacy, and forbidden reinterpretations.
5. **Design / Interaction Mapping** — map page, Figma/interaction source, module order, clicks, linked refresh, detail entry, and Loading/Empty/Error/Partial/Missing.
6. **Data Source Mapping** — map each affected field to domain, service/API/provider, cadence, cache, nullable/error behavior, and public/private scope.
7. **Current Implementation Gap** — state product requirement, current behavior, exact gap, and the bounded part authorized for this task.
8. **Scope and Stop Conditions** — state allowed/blocked scope, real-scenario requirement, hard boundaries, and stop conditions.
9. **Editing** — begin only after the preceding steps pass.
10. **Validation** — run product gate, task-specific checks, tests, failure scenarios, diff/scope checks, and real scenario when applicable.
11. **Product Alignment Report** — report product/design/semantic/data/real-scenario alignment and deviations; tests alone never prove completion.

Product sources are the highest business authority. Delivery contracts, current-state files, Workflow, Governance, and tests are read after the product gate as delivery controls and implementation evidence. They cannot redefine the product.

## Task-Mode Gate

`bash scripts/v1-state.sh` has a persisted P0-to-P1A transition and two deliberately separate continuation decisions. `current_task_mode` remains `PRODUCT_FOUNDATION_REMEDIATION` while the P0 package is unmerged; `authorized_next_task_mode` is `READ_ONLY_PRODUCT_AUDIT`. Only P0 effective merged main, local/origin main match, merged-main validation, and Product Source Gate `PASS` derive the effective task mode as P1A. No YAML rewrite is required after those runtime facts become true.

- `PRODUCT_AUDIT_ALLOWED` applies only after the effective task mode becomes `READ_ONLY_PRODUCT_AUDIT` and the fixed `read_only_product_audit_scope_contract` remains locked. It requires Product Source Gate `PASS`, a clean worktree, clean/synced `main`, no current business-package PR, no active/conflicting PR, and a complete machine-readable audit scope. A configured Draft `PAUSED_TECHNICAL_DEBT` PR is tolerated only when its actual changed files are available and `PAUSED_PR_SCOPE_RELATION=UNRELATED` after exact-path, directory-prefix, module, and source-domain checks. `OVERLAPPING` blocks; `UNKNOWN` fails closed and requires human decision. Number, branch, Head, title, Draft state, or a documentation label alone never proves unrelated scope.
- `NEXT_BUSINESS_PHASE_ALLOWED` and `CAN_START_NEXT_BUSINESS_PHASE` remain the strict implementation/merge/deployment gates. Paused or active open PRs continue to block them according to the current phase rules.

A read-only product audit may inspect product sources, code, APIs, tests, runtime, network payloads, screenshots, and Figma. It may not change code or tests, create a business implementation PR, touch the paused PR, transition Ready, merge, deploy, or begin implementation. An active/conflicting PR, dirty worktree, failed Product Source Gate, missing clean/synced main, or attempted editable scope makes `PRODUCT_AUDIT_ALLOWED=NO`.

P1A may start only after the P0 baseline is effective and validated on clean/synced merged main. P1A has `repository_edits_allowed=false`, `implementation_allowed=false`, and `implementation_pr_allowed=false`. P1B remains blocked until the P1A decision and bounded implementation authorization are independently reviewed and effective on merged main; completing P1A does not mark Home implementation complete.

---

# Contract-First Delivery Compatibility

After the Product Source Gate and product mappings above, read these delivery-control files in this order:

1. `docs/PROJECT_DELIVERY_CONTRACT.md`
2. `docs/PROJECT_CURRENT_STATE.md`
3. `docs/DELIVERY_PROGRESS_MATRIX.md`
4. `docs/CODEX_TASK_TEMPLATE.md`

Then read compatibility and historical evidence as needed:

- `docs/ACTIVE_MAINLINE_STATUS.yml` is `DERIVED_ONLY` and cannot override the contract/matrix/current state.
- `docs/CODEX_NEXT_TASK.yml` is `DERIVED_ONLY` and cannot choose the next business phase by itself.
- Legacy V1 docs are historical asset and audit evidence only.
- Review-only slice count is not a delivery completion standard.

Only merged `main` counts as completed. Open Issue, branch, Draft PR, open PR, CI-green unmerged PR, local commit, Codex output, or chat history does not count as completion.

---

# Session Bootstrap

Use this file first in every new window.

Default workflow is GPT + Codex + GitHub-native.
（默认工作流是 GPT + Codex + GitHub 原生。）

Terminal scripts are fallback only except local main sync after merge.
（终端脚本除合并后同步 main 外，只作为兜底。）

GitHub auth and GPT / Codex / local `gh` handoff must follow `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`.
（GitHub 认证与 GPT / Codex / 本地 `gh` 交接必须遵守 `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`。）

Fixed local fallback commands must follow `docs/WORKFLOW_COMMAND_AUTOMATION.md`.
（固定本地兜底命令必须遵守 `docs/WORKFLOW_COMMAND_AUTOMATION.md`。）

After #830, duplicate skeleton packages must follow `docs/V1_DUPLICATE_SKELETON_FREEZE_RULE.md`.
（#830 之后，重复骨架包必须遵守 `docs/V1_DUPLICATE_SKELETON_FREEZE_RULE.md`。）

Codex must output the result requested by the current package and stop; PR number is required only when PR creation is assigned to Codex.
（Codex 必须输出当前包要求的结果并停止；只有 PR 创建分配给 Codex 时才必须输出 PR 编号。）

Fallback bootstrap command:

```bash
bash scripts/v1-session-bootstrap.sh
```

Preferred human operator command:

```bash
bash scripts/v1-go.sh
```

This is the default one-command entry. It checks state, starts the existing operator path, copies the next Codex task to the macOS clipboard when Codex CLI cannot start, packages dirty work, reads PR numbers automatically, and routes A/B PR flow without manual long-command copy/paste.

Codex final answers must follow the Fixed Codex Output Contract（Codex 固定输出契约） in `docs/ANSWER_FORMAT_CONTRACT.md`.
Every final answer must begin with `WHAT_THIS_STEP_DOES（这一步在做什么）` and `CURRENT_PROGRESS（当前进度）`, then state `NEXT_ALLOWED_ACTION（下一允许动作）`, `NEXT_BLOCKED_ACTION（下一禁止动作）`, `RISK_LEVEL（风险等级）`, and `OVERREACH_STATUS（越界状态）`.
English technical terms must include Chinese explanations such as merged main（已合并主线）, clean/synced main（干净且已同步主线）, effective（已生效）, blocked（阻塞）, and allowed（允许）.

For reviewed B-risk PRs:

```bash
bash scripts/v1-go.sh --confirm-reviewed <PR_NUMBER>
```

Read-only status:

```bash
bash scripts/v1-go.sh --status
```

Underlying operator command:

```bash
bash scripts/v1-operator.sh
```

This is the preferred one-command terminal entry. It checks state, creates or switches the task branch, starts Codex or prints the full task, packages dirty work, opens PRs, and delegates merge flow to fixed scripts.

For reviewed B-risk PRs:

```bash
bash scripts/v1-operator.sh --confirm-reviewed <PR_NUMBER>
```

Older status/task summary command:

```bash
bash scripts/v1-auto.sh next
```

`v1-auto.sh` is a Chinese workflow operator. It summarizes state, progress, blockers, and the next Codex task while still delegating to the fixed workflow scripts.

Baseline sync packages are no longer a normal workflow step. If clean / synced `main` has no open PR and the actual HEAD is ahead of `docs/ACTIVE_MAINLINE_STATUS.yml` or `docs/CODEX_NEXT_TASK.yml`, use actual HEAD as the Effective execution baseline（实际执行基线）. The next business package should update source-of-truth docs within its own scoped changes.

baseline sync 小包不再作为常规流程。若 clean / synced `main` 没有 open PR，且 actual HEAD 领先事实源文件，使用 actual HEAD 作为实际执行基线；下一业务包在自身范围内顺手更新事实源。

One-command Codex runner:

```bash
bash scripts/v1-codex-run-next.sh
```

This starts from clean/synced `main`, generates the next Codex task through `v1-auto.sh next`, and tries to launch Codex CLI. It does not stage, commit, push, create PRs, or merge.

If Codex shell cannot confirm Open PR because local `gh` is unavailable, but GPT connector or the user's terminal has already confirmed Open PR none, the allowed handoff form is:

```bash
bash scripts/v1-codex-run-next.sh --open-pr-none-confirmed
```

This only bypasses Codex GitHub status unknown. It does not bypass non-main branch, dirty worktree, explicit open PR, failed Main Sync, or other blockers.

If Codex CLI fails after task generation because of local session permission, readonly database, or `codex exec` failure, `v1-codex-run-next.sh` prints the full task text directly. Copy the printed task into Codex; do not run extra business steps.

如果 Codex CLI 因本地 session 权限、readonly database 或 `codex exec` 失败而无法启动，`v1-codex-run-next.sh` 会直接打印完整任务全文。复制打印出的任务给 Codex，不要额外执行业务步骤。

One-command PR completion helper:

```bash
bash scripts/v1-pr-complete.sh <PR_NUMBER> A "<SUBJECT>"
bash scripts/v1-pr-complete.sh <PR_NUMBER> B "<SUBJECT>" --confirm-reviewed
```

This helper always checks through `v1-auto.sh check-pr` and merges only through `v1-merge-sync.sh`.

A-risk Auto Merge Rule（A-risk 自动合并规则） allows Codex to complete docs / contract / workflow packages by running PR check, CI wait, merge, main sync, and effective-state verification when the target PR is the current package PR, the PR is not Draft, Maven / workflow / Codex task validation passed, PR checks passed, and the changed files contain only docs / workflow scripts / contract files. Java, tests, schema, dashboard, pom, runtime config, business logic, order, execution, and auto-trading remain forbidden.

PR #1004 is an unrelated Draft PR for this P0-0 package. It must not be modified, merged, closed, reviewed, or targeted by the completion helper. It must not block PR #1005 merge, but while open it still blocks P0-1.

B-risk checks allow negative safety assertions such as `.doesNotExist()`, `does not expose`, `notTradingSignal`, `notCandidateSignal`, `notDecisionGeneration`, `notPointSignal`, `notExecutable`, `externalRefreshTriggered=false`, `displaySlotsAreCandidatePool=false`, and `failClosed`. Positive forbidden additions still stop the flow.

B-risk 检查允许 `.doesNotExist()`、`does not expose`、`notTradingSignal`、`notCandidateSignal`、`notDecisionGeneration`、`notPointSignal`、`notExecutable`、`externalRefreshTriggered=false`、`displaySlotsAreCandidatePool=false` 和 `failClosed` 等负向安全断言。正向禁用语义仍会停止流程。

Dirty-work package helper:

```bash
bash scripts/v1-package-dirty-work.sh
```

Use it only when Codex wrote files but did not successfully create the task branch / commit / PR. It reads `docs/CODEX_NEXT_TASK.yml`, stages only files allowed by the declared risk, commits, pushes, and calls the fixed PR command. It stops for open PR, GitHub PR status unknown, C-risk, forbidden staged paths, or disallowed file paths.

脏工作区打包入口只用于 Codex 已写文件但未成功创建分支 / commit / PR 的情况。它读取 `docs/CODEX_NEXT_TASK.yml`，只 stage 当前 risk 允许的文件，commit、push 并调用固定 PR 命令。遇到 open PR、GitHub PR 状态未知、C-risk、已 stage 禁止路径或不允许的文件路径时会停止。

1. Read `docs/ACTIVE_MAINLINE_STATUS.yml`.
2. Read `docs/V1_CAPABILITY_MATRIX.md`.
3. Read `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`.
4. Read `docs/V1_DUPLICATE_SKELETON_FREEZE_RULE.md`.
5. Read `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`.
6. Read `docs/WORKFLOW_COMMAND_AUTOMATION.md`.
7. Run `git branch --show-current`, `git status --short`, and `git log --oneline -5`.
8. Never use chat memory as progress.
9. Reply using `docs/ANSWER_FORMAT_CONTRACT.md`.
10. Do not continue to next package unless current PR is merged, main is synced, and worktree is clean.
11. Open PR / branch / Issue does not count as done.
12. Do not continue P359 or start P360 by default.
13. Do not default back to a historical track. The current active block comes from `docs/ACTIVE_MAINLINE_STATUS.yml`.
14. For editable implementation, merge, or deployment, continue only when `bash scripts/v1-state.sh` or accepted handoff evidence confirms the strict phase gate. For an effective `READ_ONLY_PRODUCT_AUDIT`, use `PRODUCT_AUDIT_ALLOWED`; only a paused technical-debt PR classified `UNRELATED` from actual changed-file/module/source-domain evidence may coexist. `OVERLAPPING` and `UNKNOWN` remain blocked or require human decision, and all other audit conditions remain fail closed.

If Codex shell prints `OPEN_PRS: GH_NOT_AVAILABLE`, treat it as Codex GitHub status unknown. It is not, by itself, proof that the project has an open PR or an unsynced main. GPT connector evidence or the user's local terminal `gh` output may be accepted as handoff evidence when it explicitly confirms open PR none, main sync, and clean worktree.

For one-command execution, use `bash scripts/v1-codex-run-next.sh --open-pr-none-confirmed` only after that handoff evidence exists.

## Workflow Command Shortcuts

- Default workflow: GPT decides the next pack, Codex executes scoped file changes / checks / commit / push, and PR creation / merge follows `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`.
- Fallback new window command: `bash scripts/v1-session-bootstrap.sh`
- Fixed status check: `bash scripts/v1-state.sh`
- Chinese operator entry: `bash scripts/v1-auto.sh next`
- One-command Codex runner: `bash scripts/v1-codex-run-next.sh`
- One-command Codex runner with explicit Open PR none handoff: `bash scripts/v1-codex-run-next.sh --open-pr-none-confirmed`
- PR completion helper: `bash scripts/v1-pr-complete.sh <PR_NUMBER> <A|B|C> "<SUBJECT>" [--confirm-reviewed]`
- Dirty-work package helper: `bash scripts/v1-package-dirty-work.sh`
- Fixed PR creation: `bash scripts/v1-open-pr.sh <branch> "<title>" <risk> [--body-file <file>] [--draft|--ready]`
- Fallback PR review input: `bash scripts/v1-pr-review-input.sh <PR_NUMBER>`
- Local merge sync after approval: `bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SUBJECT>" --risk <risk> [--confirm]`
- Fallback Codex completion safe check: `bash scripts/v1-safe-check.sh`

Token leakage remains a hard stop: never paste or repeat GitHub tokens in chat or logs.
