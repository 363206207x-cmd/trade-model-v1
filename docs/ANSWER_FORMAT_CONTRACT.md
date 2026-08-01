# Answer Format Contract

Every status, progress, task-handoff, and PR-review answer must use the fields below.

Default workflow is GPT + Codex + GitHub-native.
（默认工作流是 GPT + Codex + GitHub 原生。）

Terminal scripts are fallback only except local main sync after merge.
（终端脚本除合并后同步 main 外，只作为兜底。）

GitHub auth and GPT / Codex / local `gh` handoff must follow `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`.
（GitHub 认证与 GPT / Codex / 本地 `gh` 交接必须遵守 `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`。）

Use both English and Chinese labels. Do not answer only in English or only in Chinese.

## Product-First Start Contract / 产品优先启动契约

Before editing, every task response or working record must include:

```text
PRODUCT_SOURCE_GATE_STATUS:
PASS / BLOCKED

PRODUCT_SOURCES_READ:
- <registered source_id and real path>

PRODUCT_CONTRACT_MAPPING:
- product module
- applicable source chapter
- required business semantics
- forbidden boundary changes

DESIGN_INTERACTION_MAPPING:
- page / component
- module order
- click / linkage
- detail entry
- Loading / Empty / Error / Partial / Missing

DATA_SOURCE_MAPPING:
- field
- source domain
- API / Service / provider
- cadence and cache
- null / error behavior
- public / private scope

CURRENT_IMPLEMENTATION_GAP:
- product requirement
- current implementation
- gap
- bounded part allowed in this task

STOP_CONDITIONS:
- <task-specific hard stops>
```

At task end, every product-affecting answer must additionally include:

```text
PRODUCT_ALIGNMENT_STATUS:
PASS / PARTIAL / BLOCKED

DESIGN_ALIGNMENT_STATUS:
PASS / PARTIAL / BLOCKED / NOT_APPLICABLE

SEMANTIC_ALIGNMENT_STATUS:
PASS / BLOCKED

DATA_SOURCE_ALIGNMENT_STATUS:
PASS / PARTIAL / BLOCKED

REAL_SCENARIO_STATUS:
PASS / NOT_RUN / BLOCKED

DEVIATIONS:
- <difference from the registered product sources, or NONE>
```

Maven PASS, Workflow PASS, Governance PASS, test count, an open PR, or a merged technical slice cannot by itself mark a product module complete.

## Fixed Codex Output Contract / Codex 固定输出契约

All Codex final outputs must include the fixed fields below.
Every field name must keep both English and Chinese labels.
The first two fields must always be:

```text
WHAT_THIS_STEP_DOES（这一步在做什么）:
CURRENT_PROGRESS（当前进度）:
CURRENT_PHASE（当前阶段）:
CURRENT_BLOCK（当前模块）:
CURRENT_BRANCH（当前分支）:
CURRENT_PR（当前 PR）:
MERGED_MAIN_STATUS（合并主线状态）:
EFFECTIVE_STATUS（生效状态）:
NEXT_ALLOWED_ACTION（下一允许动作）:
NEXT_BLOCKED_ACTION（下一禁止动作）:
WHY_BLOCKED_OR_ALLOWED（为什么允许或阻塞）:
FILES_CHANGED（变更文件）:
CHECKS（检查）:
RISK_LEVEL（风险等级）:
OVERREACH_STATUS（越界状态）:
```

Required bilingual technical terms:

- merged main（已合并主线）
- clean/synced main（干净且已同步主线）
- open PR（未合并 PR）
- Draft PR（草稿 PR）
- A-risk（低风险）
- B-risk（中风险）
- effective（已生效）
- blocked（阻塞）
- allowed（允许）
- worktree（工作区）
- gate（门禁）
- UserPosition（用户手动持仓）
- ExecutionPlan（执行计划）
- Source Gate（来源门禁）
- AccountRisk（账户风险）
- PositionMonitor（持仓监控）
- Review（复盘）

Rules:

1. `WHAT_THIS_STEP_DOES（这一步在做什么）` and `CURRENT_PROGRESS（当前进度）` must appear first.
2. `CURRENT_PROGRESS（当前进度）` must state the completed P stage, whether the current PR is merged main（已合并主线）, whether the current phase is effective（已生效）, and whether the next phase is allowed（允许）.
3. `NEXT_ALLOWED_ACTION（下一允许动作）` must only name the next action allowed by the contract and runtime gate（门禁）.
4. `NEXT_BLOCKED_ACTION（下一禁止动作）` must explicitly name work that must not be started.
5. If there is an open PR（未合并 PR） or PENDING_MERGED_MAIN（等待合并主线）, the answer must say that open PR does not count as complete and only merged main（已合并主线） can become effective（已生效）.
6. Codex must not output only technical logs; it must include user-readable phase progress and gate（门禁） status.

```text
Current Mainline（当前主线）:
Current Block（当前模块）:
Current Level（当前层级）:
Current Phase Status（当前阶段状态）:
Existing Module Maturity（现有模块成熟度）:
Production Deployment Readiness（生产部署就绪度）:
Contract Sync（契约同步状态）:
Done Criteria（完成标准）:
Current PR（当前 PR）:
Can Merge?（能否合并）:
Next Step（下一步）:
Remaining Steps（剩余步骤）:
Do Not Do（禁止事项）:
```

## Rules

- `Current PR（当前 PR）` must say open / merged / none.
- `Can Merge?（能否合并）` must mention risk level and approval rule.
- For A-risk Auto Merge Rule（A-risk 自动合并规则） answers, include whether the target PR is the current package PR, whether unrelated Draft PRs only block the next business phase, and whether PR #1004 was untouched.
- `Next Step（下一步）` must not skip an open required PR, unsynced main, dirty worktree, Draft PR, failed CI, or unresolved merge conflict.
- `Do Not Do（禁止事项）` must include no auto-trading when trading paths are discussed.
- Open PR / branch / Issue does not count as done.
- Merged `main` is the only completed state.
- When current package is merged, main is synced, worktree is clean, CI passed, and no blocker exists, do not ask whether to continue; generate the next maximum safe Codex task prompt.


## Contract-First Fields

- `Current Phase Status（当前阶段状态）` must come from `docs/DELIVERY_PROGRESS_MATRIX.md`.
- `Existing Module Maturity（现有模块成熟度）` must come from `docs/DELIVERY_PROGRESS_MATRIX.md`.
- `Production Deployment Readiness（生产部署就绪度）` must come from `docs/PROJECT_CURRENT_STATE.md` / compatibility mirror.
- `Contract Sync（契约同步状态）` must be based on agreement between contract, matrix, current state, and derived compatibility files.

Legacy review-only slice count must not be used as project delivery completion.
