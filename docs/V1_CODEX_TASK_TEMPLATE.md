# V1 Codex Task Template（V1 Codex 任务模板）

本文件固定 Codex 每轮任务格式。以后不要每次重新发明任务包，只填变量。

For terminal workflows, prefer bash scripts/v1.sh as the single entry point.  
（终端工作流优先使用 bash scripts/v1.sh 作为单一入口。）

For terminal workflows, prefer `bash scripts/v1-auto.sh` as the default non-interactive entry point.
（终端工作流默认优先使用 `bash scripts/v1-auto.sh` 作为非交互入口。）

Keep `bash scripts/v1.sh` only as a fallback menu.
（`bash scripts/v1.sh` 只作为备用菜单。）

## 1. 固定开头

```text
继续 Trade Model V1 当前分支任务：<TASK_TITLE>。

你必须只完成一个明确交付包：<DELIVERABLE_SCOPE>。

任务模式：<TASK_MODE>

当前 GitHub 信息：
- Issue: #<ISSUE_NUMBER>
- PR: #<PR_NUMBER>
- Branch: <BRANCH_NAME>
- Base main commit: <BASE_COMMIT>
- 当前 placeholder: <PLACEHOLDER_PATH>
```

`<TASK_MODE>` 可为 `MINIMAL_DELIVERABLE`（单点交付模式）或 `MAX_SAFE_PACK`（最大安全任务包）。默认优先使用 `MAX_SAFE_PACK`。`MINIMAL_DELIVERABLE` 只在任务确实需要单点收口、风险边界不清，或 Authorization Gate（授权门）前需要暂停时使用。`MAX_SAFE_PACK` 不能跨越 A/B/C 授权边界。

## 2. 执行前命令

状态检查优先使用：

```bash
bash scripts/v1-status.sh
```

```bash
cd /Users/xuchao/Documents/trade-model-v1

git fetch origin
git switch <BRANCH_NAME>
git pull origin <BRANCH_NAME>
git status
```

## 3. 本轮目标

```text
本轮目标：
1. 删除 placeholder：
   <PLACEHOLDER_PATH>

2. 新增或修改：
   <ALLOWED_FILE_1>
   <ALLOWED_FILE_2>
   <ALLOWED_FILE_3>

本轮最大安全任务包文件组：
   <GROUPED_ALLOWED_FILES>
```

## 4. 必须读取文件

```text
必须读取：
- docs/SESSION_BOOTSTRAP.md
- docs/ACTIVE_MAINLINE_STATUS.yml
- docs/ANSWER_FORMAT_CONTRACT.md
- docs/V1_OPERATOR_WORKFLOW_CONTRACT.md
- docs/V1_CURRENT_STATE.md
- docs/V1_CODEX_TASK_TEMPLATE.md
- docs/V1_PR_REVIEW_CHECKLIST.md
- docs/PROJECT_PROGRESS_INDEX.md
- <PHASE_PREVIOUS_DOCS>
```

Open PR 不算完成。branch 不算完成。Issue 不算完成。Codex 输出不算完成。merged main 才算完成。

## 5. 允许修改文件

```text
只允许修改：
- <ALLOWED_FILE_1>
- <ALLOWED_FILE_2>
- <ALLOWED_FILE_3>
- <GROUPED_ALLOWED_FILES>
```

`<GROUPED_ALLOWED_FILES>` 表示同一风险档位、同一模块 / 同一业务轨道、同一验证方式且不会跨越授权门的一组允许文件。

## 6. 禁止修改文件

```text
默认不允许修改：
- src/main/resources/application.yml
- src/main/resources/schema.sql
- src/main/resources/templates/dashboard.html，除非明确授权
- controller / endpoint / API（控制器 / 接口）
- service（服务），除非明确授权
- mapper（数据库映射）
- config（配置）
- PushRecheckScheduler / PushRecheckService / PushSnapshotService
- PositionSyncScheduler / PositionSyncService
- MarketDataScheduler / RealMarketDataFetcherService
- MarketQuoteClient / BinanceMarketQuoteClient
```

## 7. 强制禁止事项

```text
强制禁止：
- 不接 order API（下单接口）
- 不接 execution API（执行接口）
- 不自动交易
- 不自动平仓
- 不自动反手
- 不自动修改止损
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）
- 不升级 readiness（可执行就绪）
- 不创建 opportunity push execution（机会推送执行）
- 不读取 runtime / live / external data（运行时 / 实时 / 外部数据），除非明确授权
- 不接 MarketQuoteClient（行情客户端），除非明确授权
- 不把 Display Slots（首页展示位）当作 Watchlist Pool（观察库池）
- 不恢复默认六币固定推送
- 不让非观察库资产进入机会推送候选
```

## 8. 测试命令

按任务类型选择。

### docs-only（只改文档）

```bash
git diff --name-status main...HEAD
git diff --check
git status
```

### docs-only max safe pack（只改文档的最大安全任务包）

```bash
<GROUPED_VALIDATION_COMMANDS>
git diff --name-status main...HEAD
git diff --check
git status
```

`<GROUPED_VALIDATION_COMMANDS>` 表示本轮统一验证命令。docs-only 最大安全包仍只跑 docs-only 验证。Java / dashboard / schema / API 不得混入 docs-only 包。

### Java 小改

```bash
./mvnw -q -Dtest=<TARGET_TEST> test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
git diff --check
git diff --name-status main...HEAD
git status
```

### dashboard.html（首页页面）小改

```bash
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
git diff --check
git diff --name-status main...HEAD
git status
```

## 9. 提交命令模板

```bash
git add <ALLOWED_FILE_1>
git add <ALLOWED_FILE_2>
git add -u <PLACEHOLDER_PATH>

git status
git commit -m "<COMMIT_MESSAGE>"
git push origin <BRANCH_NAME>
```

提交命令和用户需要复制的命令必须尽量保持完整代码块，不要让用户分多次复制。

## 10. Codex 最终输出格式

Codex 完成后必须输出：

```text
1. 改动文件清单
2. 是否只修改授权文件并删除 placeholder
3. 是否误改 Java / test / dashboard / schema / config
4. 是否接 API / MarketQuoteClient / scheduler / order / execution / auto-trading
5. 是否生成真实 entry / stop / TP / RR
6. 是否升级 readiness
7. 测试结果
8. git diff --check 结果
9. 当前分支和 commit hash
10. 明确说明本轮没有做哪些禁止事项
```

Codex 最终输出必须逐项列出每个变更文件。

Codex 最终输出必须逐项确认每条边界没有越界，特别是 Java / test / dashboard / schema / config / API / MarketQuoteClient / scheduler / order / execution / auto-trading。

最大安全任务包完成后，也必须说明是否只修改了 `<GROUPED_ALLOWED_FILES>` 中授权文件。

## 11. 风险档位变量

任务包必须标明风险档位：

```text
风险档位：A / B / C
是否需要用户业务授权：是 / 否
```

A 档通常无需用户业务确认。B 档需要用户一句业务确认。C 档必须暂停等用户明确授权。

## 12. P291C Workflow Contract Requirements

每个 Codex 任务必须声明：

```text
Current Mainline（当前主线）:
Current Block（当前模块）:
Current Level（当前层级）:
Done Criteria（完成标准）:
Current PR（当前 PR）:
Can Merge?（能否合并）:
Next Step（下一步）:
Remaining Steps（剩余步骤）:
Do Not Do（禁止事项）:
```

每个任务必须运行适用的 workflow contract check：

```bash
bash scripts/check-workflow-contract.sh
```

Codex 完成后优先运行：

```bash
bash scripts/v1-safe-check.sh
```

如果任务修改 Java 或 test 路径，PR 模板和状态文件必须包含 capability level 字段。

## 12. P291A 后新增必填项

每个任务必须补充以下内容。

### 12.1 Capability Level Uplift

必须写明本轮提升哪个 capability level：

```text
Capability before:
Capability after:
Business-chain step:
Why this is not a low-value repeat:
```

允许的层级：

- `0 NOT_STARTED`
- `1 DOCS_ONLY_GATE`
- `2 SKELETON`
- `3 TARGETED_TEST`
- `4 TEST_ONLY_WIRING`
- `5 REVIEW_ONLY_RUNTIME`
- `6 PRODUCTION_WIRING`
- `7 PRODUCTION_READY`

### 12.2 Allowed Review-Only Outputs

每个任务必须列出本轮允许或保持允许的 review-only outputs。

如果本轮没有任何 allowed review-only output，必须解释为什么仍然值得做。

可选输出包括：

- entry zone proposal
- stop zone proposal
- TP proposal
- RR estimate
- position size suggestion
- leverage cap suggestion
- invalidation condition
- reduce position
- tighten stop
- move stop
- partial take-profit
- wait for trigger
- plan invalidated
- manual review required
- internal push preview
- risk downgraded candidate
- confused with recovery condition

### 12.3 Production Wiring Declaration

每个任务必须明确：

```text
This is production wiring: yes / no
If yes, explicit authorization source:
If no, what remains blocked:
```

除非用户明确授权，否则不能把 skeleton、targeted test、test-only wiring、review-only runtime 写成 production wiring。

### 12.4 Low-Value Repeat Check

提交前必须回答：

- 是否只是 closure-only？
- 是否只是重复 blocked list？
- 是否把 docs-only 当成 production complete？
- 是否把 skeleton 当成 production wiring？
- 是否把 review-only 写成 no output？
- 是否至少推进一个业务链路节点？

如果答案显示低价值重复，必须合并包、改包或暂停。

## 13. P291D Workflow Command Automation

- 新窗口优先运行 `bash scripts/v1-session-bootstrap.sh`
- 状态检查优先运行 `bash scripts/v1-status.sh`
- 审 PR 优先运行 `bash scripts/v1-pr-review-input.sh <PR_NUMBER>`
- 合并同步优先运行 `bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SUBJECT>"`
- Codex 完成后优先运行 `bash scripts/v1-safe-check.sh`
