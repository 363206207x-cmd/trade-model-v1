# V1 Codex Task Template（V1 Codex 任务模板）

本文件固定 Codex 每轮任务格式。以后不要每次重新发明任务包，只填变量。

## 1. 固定开头

```text
继续 Trade Model V1 当前分支任务：<TASK_TITLE>。

你必须只完成一个最小交付物：<MINIMAL_DELIVERABLE>。

当前 GitHub 信息：
- Issue: #<ISSUE_NUMBER>
- PR: #<PR_NUMBER>
- Branch: <BRANCH_NAME>
- Base main commit: <BASE_COMMIT>
- 当前 placeholder: <PLACEHOLDER_PATH>
```

## 2. 执行前命令

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
```

## 4. 必须读取文件

```text
必须读取：
- docs/V1_OPERATOR_WORKFLOW_CONTRACT.md
- docs/V1_CURRENT_STATE.md
- docs/V1_CODEX_TASK_TEMPLATE.md
- docs/V1_PR_REVIEW_CHECKLIST.md
- docs/PROJECT_PROGRESS_INDEX.md
- <PHASE_PREVIOUS_DOCS>
```

## 5. 允许修改文件

```text
只允许修改：
- <ALLOWED_FILE_1>
- <ALLOWED_FILE_2>
- <ALLOWED_FILE_3>
```

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

## 11. 风险档位变量

任务包必须标明风险档位：

```text
风险档位：A / B / C
是否需要用户业务授权：是 / 否
```

A 档通常无需用户业务确认。B 档需要用户一句业务确认。C 档必须暂停等用户明确授权。
