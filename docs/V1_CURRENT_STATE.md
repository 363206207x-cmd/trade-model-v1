# V1 Current State（V1 当前状态）

本文件用于记录 Trade Model V1 当前阶段。以后每次 PR（合并请求）合并后，必须优先更新本文件或在下一次 progress index（进度索引）刷新中同步更新。

## 1. 当前本地 / 远端主线

当前 main（主分支）基准：

```text
550f743 BACKEND-P203 Low-Frequency Scan Scheduler Authorization Gate (#523)
```

说明：本文件创建于 WORKFLOW-P1 分支，主线基准为 P203 合并后状态。

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

当前已创建但尚未完成的功能 PR：

```text
PR #525：BACKEND-P204 Low-Frequency Scan Scheduler Minimal Skeleton（低频扫描定时器最小骨架）
Branch：p204
Issue：#524
风险档位：B 档偏低风险
状态：Draft PR（草稿合并请求）
```

P204 预期只允许新增 disabled-by-default（默认关闭）、dry-run（空跑）、no-op（不执行实际动作）的 scheduler skeleton（定时器骨架）和测试。

P204 禁止：

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
#524：BACKEND-P204 Low-Frequency Scan Scheduler Minimal Skeleton
#526：WORKFLOW-P1 固化 V1 Operator Workflow Contract
```

注意：若本文件合并后，#526 会关闭。

## 6. 下一步推荐

当前优先级：

```text
先完成 WORKFLOW-P1：固化工作流契约文件。
然后回到 P204：审查 / 推进 Low-Frequency Scan Scheduler Minimal Skeleton。
```

P204 属于 B 档偏低风险，因为它会新增 Java scheduler skeleton（定时器骨架），但默认关闭，不接行情，不扫真实资产，不交易。

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
