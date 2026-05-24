# V1 Current State（V1 当前状态）

本文件用于记录 Trade Model V1 当前阶段。以后每次 PR（合并请求）合并后，必须优先更新本文件或在下一次 progress index（进度索引）刷新中同步更新。

## 1. 当前本地 / 远端主线

当前 main（主分支）基准：

```text
a955449 BACKEND-P205 Max Safe Docs Pack After P204 Scheduler Skeleton (#529)
```

说明：WORKFLOW-P1 已合并，P204 已合并，P205 已完成并合并。当前主线基准为 P205 合并后状态。

## 2. 当前已完成主线

近期已完成：

```text
P197：Watchlist Low-Frequency Scan / Opportunity Promote Scope Audit（观察库低频扫描 / 机会提升范围审计）
P198：Watchlist Low-Frequency Scan / Opportunity Promote Authorization Gate（观察库低频扫描 / 机会提升授权门）
P199：Watchlist Low-Frequency Scan / Opportunity Promote Minimal Wiring（观察库低频扫描 / 机会提升最小接线，docs-only）
P200：Watchlist Low-Frequency Scan / Opportunity Promote Closure（观察库低频扫描 / 机会提升收口）
P201：Project Progress Index Refresh After Watchlist Scan Promote Semantics（观察库扫描提升语义后项目总进度索引刷新）
P202：Low-Frequency Scan Scheduler Scope Audit（低频扫描定时器范围审计）
P203：Low-Frequency Scan Scheduler Authorization Gate（低频扫描定时器授权门）
P204：Low-Frequency Scan Scheduler Minimal Skeleton（低频扫描定时器最小骨架）
P205：Max Safe Docs Pack After P204 Scheduler Skeleton（P204 定时器骨架后的最大安全文档包）
```

## 3. 当前项目真实状态

当前 Trade Model V1 已经完成大量 read-only（只读）、review-only（只允许复核）、fail-closed（失败关闭）基础。

当前已经完成：

- SourceTrace（证据来源追踪）只读展示。
- BoundaryCandidate（边界候选交易计划）只读候选展示。
- ExecutionPlan（执行计划）只允许复核展示。
- Risk Action Guard（风险动作保护器）只读风险展示。
- Position Monitor（持仓监控）强反转 / 移动止损只读展示。
- Dashboard Risk Reminder（首页风险提醒）只读展示。
- Watchlist Low-Frequency Scan / Opportunity Promote（观察库低频扫描 / 机会提升）语义边界。
- Low-Frequency Scan Scheduler（低频扫描定时器）范围审计和授权门。
- Low-Frequency Scan Scheduler（低频扫描定时器）disabled-by-default skeleton（默认关闭骨架）。

当前仍未完成：

- 真实低频扫描。
- Watchlist runtime data source（观察库运行时数据源）。
- MarketQuoteClient scan integration（行情客户端扫描接入）。
- ScanScore（扫描分数）。
- Candidate Attention（候选关注）。
- Promote To Home（提升到首页观察）运行时逻辑。
- Opportunity Push execution（机会推送执行）。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- ExecutionPlan readiness（执行计划可执行就绪）。
- order API（下单接口）。
- execution API（执行接口）。
- auto-trading（自动交易）。

## 4. 当前 open PR（未合并请求）

当前已创建但尚未完成的文档 PR：

```text
PR #531：BACKEND-P206 Low-Frequency Scan Runtime Contract Audit Pack（低频扫描运行时契约审计包）
Branch：p206
Issue：#530
风险档位：A 档 docs-only（只改文档）
状态：Draft PR（草稿合并请求）
```

P206 只允许完成最大安全运行时契约审计包：新增低频扫描运行时契约、观察库运行时数据源、扫描结果契约和扫描分数规则定义审计文档，并同步更新 `V1_CURRENT_STATE.md` 与 `PROJECT_PROGRESS_INDEX.md`。

P206 禁止：

- 接 MarketQuoteClient（行情客户端）。
- 读取 runtime / live / external data（运行时 / 实时 / 外部数据）。
- 扫描真实资产。
- 生成 ScanScore（扫描分数）。
- 生成 Candidate Attention（候选关注）。
- 生成 Promote To Home（提升到首页观察）。
- 创建 opportunity push execution（机会推送执行）。
- 生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 升级 readiness（可执行就绪）。
- 自动交易。

## 5. 当前 open Issue（未关闭问题单）

```text
#530：BACKEND-P206 Low-Frequency Scan Runtime Contract Audit Pack
```

P204、P205 和 WORKFLOW-P1 已合并，不再作为当前 open PR（未合并请求）处理。

## 6. 下一步推荐

当前优先级：

```text
完成 P206 最大安全运行时契约审计包。
```

P206 属于 A 档 docs-only（只改文档），只补齐 Low-Frequency Scan（低频扫描）真实实现前的 runtime contract audit（运行时契约审计）。本轮不写 Java，不改测试，不改 dashboard，不接 API，不接 MarketQuoteClient（行情客户端），不读取运行时数据，不创建真实扫描。

## 7. 当前禁止越界

除非后续 Issue（问题单）和 Authorization Gate（授权门）明确允许，否则禁止：

- 自动下单。
- 自动平仓。
- 自动反手。
- 自动修改止损。
- 生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 升级 ExecutionPlan readiness（执行计划可执行就绪）。
- 接 order API（下单接口）。
- 接 execution API（执行接口）。
- 接 auto-trading（自动交易）。
- 把 Display Slots（首页展示位）当作 Watchlist Pool（观察库池）。
- 默认六币固定推送。
- 非观察库资产进入机会推送候选。
- 在踩踏状态推送机会、反手或新开仓。
- 把短线插针当作趋势反转。

## 8. 合并后同步命令

每次 PR（合并请求）合并后，本地同步命令固定为：

```bash
cd /Users/xuchao/Documents/trade-model-v1 && git switch main && git pull origin main && git status && git log --oneline -5
```
