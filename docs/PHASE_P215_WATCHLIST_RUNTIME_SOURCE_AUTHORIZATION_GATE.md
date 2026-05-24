# P215 Watchlist Runtime Source Authorization Gate

## 1. 阶段定位

P215 是 Watchlist runtime source authorization gate（观察库运行时数据源授权门）。

P215 只决定未来是否可以进入 DTO skeleton（数据对象骨架）。

P215 不实现 runtime source（运行时数据源）。

P215 不读取 DB / API / external data（数据库 / 接口 / 外部数据）。

P215 不接 MarketQuoteClient（行情客户端）。

P215 不启用 scheduler（定时器）。

P215 不创建 scan loop（扫描循环）。

## 2. 未来 P216 可考虑内容

未来 P216 只能在最大安全边界内考虑：

- 仅 pure DTO / enum / tests（纯数据对象 / 枚举 / 测试）。
- 可定义 `WatchlistRuntimeSourceDTO`。
- 可定义 `WatchlistRuntimeSourceStatusEnum`。
- 可定义 `SourceTypeEnum`。
- 可定义 freshness / stale / missing / fail-closed factory（新鲜度 / 过期 / 缺失 / 失败关闭工厂）。
- 只允许承载字段，不允许读取数据。

这些内容不能接入 DB（数据库）、行情、scheduler（定时器）、service wiring（服务接线）、API（接口）或 dashboard（首页）。

## 3. P216 暂定允许字段

以下字段仅作为 P216 DTO skeleton（数据对象骨架）方案：

- `symbol`
- `watchlistMember`
- `watchlistSource`
- `sourceType`
- `sourceRef`
- `sourceUpdatedAt`
- `receivedAt`
- `freshnessStatus`
- `staleStatus`
- `dataQualityStatus`
- `missingFields`
- `staleFields`
- `blockingReasons`
- `manualReviewRequired`
- `notTradeInstruction`

字段只能表达来源状态、阻断原因和人工复核边界，不得被解释成运行时读取、扫描结果、推送许可或交易信号。

## 4. P216 暂定 enum

以下 enum（枚举）仅作为 P216 方案：

SourceTypeEnum:

- `WATCHLIST_CONFIG`
- `DB_WATCHLIST_READ`
- `CACHE_SNAPSHOT`
- `MARKET_QUOTE_CLIENT`
- `SCHEDULER_TRIGGER`
- `MANUAL_REVIEW_INPUT`
- `UNKNOWN`

FreshnessStatusEnum:

- `FRESH`
- `STALE`
- `EXPIRED`
- `UNKNOWN`
- `NOT_AVAILABLE`

RuntimeSourceStatusEnum:

- `AVAILABLE_REVIEW_ONLY`
- `BLOCKED_NOT_WATCHLIST`
- `INCOMPLETE`
- `STALE_REVIEW_ONLY`
- `SOURCE_UNAVAILABLE`
- `NOT_IMPLEMENTED`

`DB_WATCHLIST_READ`、`MARKET_QUOTE_CLIENT` 和 `SCHEDULER_TRIGGER` 只是文档层候选值，不代表 P215 或 P216 可以读取 DB、接行情客户端或启用定时器。

## 5. P216 仍禁止

即使 P216 进入 DTO skeleton，也仍然禁止：

- 不读 DB（数据库）。
- 不读 runtime（运行时数据）。
- 不接 MarketQuoteClient（行情客户端）。
- 不接 scheduler（定时器）。
- 不接 mapper / service / controller / API（映射器 / 服务 / 控制器 / 接口）。
- 不创建 scan loop（扫描循环）。
- 不创建 real scan（真实扫描）。
- 不计算 ScanScore（扫描分数）。
- 不创建 push / readiness / trading action（推送 / 可执行就绪 / 交易动作）。

## 6. 结论

P215 只授权未来可能做 pure DTO / enum / tests skeleton（纯数据对象 / 枚举 / 测试骨架）。

P215 不授权 P216 直接实现 runtime source（运行时数据源）。
