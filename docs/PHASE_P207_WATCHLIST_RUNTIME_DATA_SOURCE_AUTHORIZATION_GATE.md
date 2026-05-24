# P207 观察库运行时数据源授权门

## 1. 阶段定位

P207 是 Watchlist runtime data source（观察库运行时数据源）的 authorization gate（授权门）。

P207 只授权未来可能创建只读 DTO（数据传输对象）/ contract skeleton（契约骨架），用于承载观察库低频扫描前的安全状态。

P207 本身不实现运行时代码，不读取数据，不接行情，不改变 scheduler behavior（定时器行为）。

## 2. 未来 P208 / P209 可考虑内容

未来 P208 / P209 可以考虑 pure DTO / enum / test（纯数据传输对象 / 枚举 / 测试）的 WatchlistRuntimeSnapshot skeleton（观察库运行时快照骨架）。

未来只读字段可以承载：

- symbol。
- watchlistMember。
- watchlistSource。
- dataQualityStatus。
- staleStatus。
- missingFields。
- blockingReasons。
- manualReviewRequired。
- notTradeInstruction。

未来 skeleton（骨架）可以考虑 fail-closed factory / builder（失败关闭工厂 / 构造器）。

未来 skeleton 仍必须 no runtime read（不读取运行时数据）。

## 3. 不允许内容

P207 不允许：

- 不接 MarketQuoteClient（行情客户端）。
- 不读 live / runtime / external data（实时 / 运行时 / 外部数据）。
- 不接 mapper / DB / API（映射器 / 数据库 / 接口）。
- 不改 service / scheduler behavior（服务 / 定时器行为）。
- 不创建真实扫描。
- 不创建 Candidate Attention（候选关注）。
- 不创建 Promote To Home（提升到首页观察）。
- 不创建 Opportunity Push execution（机会推送执行）。
- 不生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不升级 readiness（可执行就绪）。
- 不接 order / execution / auto-trading（下单 / 执行 / 自动交易）。

## 4. Fail-Closed 规则

未来 WatchlistRuntimeSnapshot skeleton（观察库运行时快照骨架）如果进入 P208 / P209，必须保持以下失败关闭规则：

- 非观察库资产：`BLOCKED_NOT_WATCHLIST` / fail-closed（失败关闭）。
- watchlist membership unknown（观察库成员关系未知）：`INCOMPLETE` 或 `BLOCKED`。
- stale data（过期数据）：`REVIEW_ONLY` / `INCOMPLETE`。
- partial data（部分数据）：不得生成 ScanScore（扫描分数）。
- unknown source（未知来源）：不得进入 Candidate Attention（候选关注）。
- risk state unknown（风险状态未知）：不得 Promote To Home（提升到首页观察）。

## 5. 结论

P207 只允许下一步进入 pure DTO / enum / tests（纯数据传输对象 / 枚举 / 测试）的 skeleton（骨架）。

P207 不允许直接进入 MarketQuoteClient real scan implementation（行情客户端真实扫描实现）。
