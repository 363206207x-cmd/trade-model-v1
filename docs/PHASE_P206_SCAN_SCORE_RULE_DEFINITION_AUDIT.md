# P206 扫描分数规则定义审计

## 1. ScanScore 定位

ScanScore（扫描分数）是未来 Low-Frequency Scan（低频扫描）排序 / 关注度辅助分，不是交易评分。

- ScanScore 不等于综合决策评分。
- ScanScore 不等于开仓信号。
- ScanScore 不等于 ExecutionPlan（执行计划）。
- ScanScore 不等于 Readiness（可执行就绪）。
- ScanScore 不等于 Opportunity Push（机会推送）。
- ScanScore 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- ScanScore 不创建交易动作。

P206 只做规则定义审计，不实现 ScanScore（扫描分数）。

## 2. 未来可考虑的证据族

未来如果另开授权实现 ScanScore，可以考虑这些证据族，但 P206 只列文档候选：

- trend alignment：趋势一致性。
- volume abnormality：成交量异常。
- volatility expansion：波动扩张。
- liquidity quality：流动性质量。
- funding / leverage risk：资金费率 / 杠杆风险。
- event window：事件窗口。
- wick / stampede risk：插针 / 踩踏风险。
- multi-timeframe consistency：多周期一致性。
- data quality：数据质量。

这些证据族不能在 P206 里接入数据源，也不能在 P206 里计算分数。

## 3. 必须阻断 Score 的条件

以下条件出现时，未来也必须阻断 ScanScore（扫描分数）生成：

- 非观察库资产。
- watchlist membership unknown（观察库成员关系未知）。
- stale market data（行情数据过期）。
- missing core market fields（缺失核心行情字段）。
- data quality below threshold（数据质量低于阈值）。
- stampede state（踩踏状态）。
- wick-only short-term risk without confirmation（仅插针短期风险且未确认）。
- source trace incomplete（证据来源追踪不完整）。
- conflicting timeframe evidence unresolved（多周期证据冲突未解决）。

阻断后只能进入 INCOMPLETE（信息不完整）或 REVIEW_ONLY（只允许复核），不能进入 Candidate Attention（候选关注）或 Promote To Home（提升到首页观察）执行。

## 4. 输出边界

- Score missing（分数缺失）时必须 INCOMPLETE（信息不完整）或 REVIEW_ONLY（只允许复核）。
- Score 高不等于交易。
- Score 高不等于 Opportunity Push execution（机会推送执行）。
- Score 高不等于 Promote To Home（提升到首页观察）自动执行。
- Score 高不生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- Score 高不升级 Readiness（可执行就绪）。
- Score 高不创建 trading action（交易动作）。
- Score 高不连接 order API（下单接口）。
- Score 高不连接 execution API（执行接口）。
- Score 高不连接 auto-trading（自动交易）。

