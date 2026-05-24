# P215 Watchlist Runtime Source DTO Skeleton Plan

## 1. 阶段定位

本文只规划 `WatchlistRuntimeSourceDTO` skeleton（观察库运行时数据源数据对象骨架）。

P215 不实现 Java。

P215 不创建 DTO（数据对象）、enum（枚举）或 test（测试）文件。

P215 不读取 DB / runtime / live / external data（数据库 / 运行时 / 实时 / 外部数据）。

## 2. P216 建议文件

以下文件仅作为 P216 方案，不在 P215 创建：

- `src/main/java/org/example/trademodel/dto/watchlistsource/WatchlistRuntimeSourceDTO.java`
- `src/main/java/org/example/trademodel/dto/watchlistsource/WatchlistRuntimeSourceStatusEnum.java`
- `src/main/java/org/example/trademodel/dto/watchlistsource/WatchlistRuntimeSourceTypeEnum.java`
- `src/main/java/org/example/trademodel/dto/watchlistsource/WatchlistRuntimeFreshnessStatusEnum.java`
- `src/test/java/org/example/trademodel/dto/watchlistsource/WatchlistRuntimeSourceDTOTest.java`

这些文件如果未来创建，也只能是 pure DTO / enum / tests（纯数据对象 / 枚举 / 测试），不能接入生产链路。

## 3. 必须测试的安全行为

以下测试要求仅作为 P216 方案：

- non-watchlist blocked（非观察库资产阻断）。
- unknown membership incomplete（成员关系未知时不完整）。
- missing source incomplete（来源缺失时不完整）。
- stale source review-only / incomplete（来源过期时只允许复核 / 不完整）。
- source unavailable incomplete（来源不可用时不完整）。
- `manualReviewRequired=true`。
- `notTradeInstruction=true`。
- no push（不推送）。
- no readiness（不升级可执行就绪）。
- no entry / stop / TP / RR（不生成入场 / 止损 / 止盈 / 盈亏比）。
- no trading action（不创建交易动作）。
- defensive copy for `missingFields` / `staleFields` / `blockingReasons`（缺失字段 / 过期字段 / 阻断原因防御性复制）。

## 4. 禁止

P215 和未来 P216 DTO skeleton 均不得越过以下边界：

- 不接 MarketQuoteClient（行情客户端）。
- 不接 DB（数据库）。
- 不读 runtime（运行时数据）。
- 不接 scheduler（定时器）。
- 不接 service / mapper / controller（服务 / 映射器 / 控制器）。
- 不改 dashboard（首页）。
- 不创建 API response（接口响应）。
- 不实现 freshness calculation（新鲜度计算）。
- 不实现 observability logging（可观测性日志）。
- 不创建 scan loop（扫描循环）。

## 5. 结论

P216 如果做，也只能是 DTO skeleton（数据对象骨架）。

不能把 DTO skeleton 当作 runtime source implementation（运行时数据源实现）。
