# P213 Runtime Source Authorization Gate

## 1. 阶段定位

P213 只定义未来 runtime source（运行时数据源）的 authorization gate（授权门）。

P213 不实现 runtime source（运行时数据源）。

P213 不写 Java。

P213 不接行情。

## 2. Runtime Source 的含义

未来凡是以下任意一种，都属于 runtime source（运行时数据源）：

- real data source（真实数据源）。
- live data（实时数据）。
- external data（外部数据）。
- DB-backed watchlist read（数据库观察库读取）。
- MarketQuoteClient（行情客户端）。
- BinanceMarketQuoteClient（币安行情客户端）。
- scheduler-triggered scan（定时器触发扫描）。
- scan loop（扫描循环）。
- production service wiring（生产服务接线）。

## 3. P213 不授权 Runtime Source

P213 明确不授权：

- P213 不授权读取运行时数据。
- P213 不授权读取行情。
- P213 不授权读取 DB watchlist（数据库观察库）。
- P213 不授权 MarketQuoteClient（行情客户端）。
- P213 不授权 scheduler activation（定时器激活）。
- P213 不授权 scan loop（扫描循环）。
- P213 不授权真实低频扫描。

## 4. 未来 Runtime Source 前置定义

未来如果要做 runtime source（运行时数据源），必须先定义：

- freshness（新鲜度）。
- stale data（过期数据）。
- missing data（缺失数据）。
- partial evidence（部分证据）。
- fail-closed（失败关闭）。
- no-score（无分数）。
- no-push（无推送）。
- no-readiness（无可执行就绪）。
- no-trading（无交易动作）。
- observability（可观测性）。
- `manualReviewRequired=true`。
- `notTradeInstruction=true`。

这些定义必须先于真实数据读取、行情接入、扫描循环和生产服务接线。

## 5. 未来 Runtime Source 仍禁止

即使未来另开授权门进入 runtime source（运行时数据源），仍禁止：

- 不生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不升级 readiness（可执行就绪）。
- 不创建 push execution（推送执行）。
- 不创建 trading action（交易动作）。
- 不接 order / execution / auto-trading（下单 / 执行 / 自动交易）。

## 6. 结论

P213 只是 runtime source authorization gate（运行时数据源授权门）。

P213 不解除任何 runtime / market / scheduler / scan loop（运行时 / 行情 / 定时器 / 扫描循环）禁令。
