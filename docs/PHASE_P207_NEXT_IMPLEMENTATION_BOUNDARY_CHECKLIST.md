# P207 下一步实现边界检查清单

## 1. 下一步推荐

P208 可以考虑 pure DTO / enum / tests（纯数据传输对象 / 枚举 / 测试）最大安全包。

推荐名称：

- BACKEND-P208 Watchlist Scan Runtime DTO Skeleton
- BACKEND-P208 Watchlist Scan Result DTO Skeleton

## 2. P208 暂定允许文件

以下仅作为建议，不在 P207 实现：

- `src/main/java/org/example/trademodel/dto/watchlistscan/WatchlistRuntimeSnapshotDTO.java`
- `src/main/java/org/example/trademodel/dto/watchlistscan/WatchlistScanResultDTO.java`
- `src/main/java/org/example/trademodel/dto/watchlistscan/WatchlistScanStatusEnum.java`
- `src/test/java/org/example/trademodel/dto/watchlistscan/WatchlistScanResultDTOTest.java`
- `src/test/java/org/example/trademodel/dto/watchlistscan/WatchlistRuntimeSnapshotDTOTest.java`

## 3. P208 暂定测试要求

P208 如果进入 pure DTO / enum / tests（纯数据传输对象 / 枚举 / 测试），建议测试：

- DTO 默认安全值。
- fail-closed factory（失败关闭工厂）。
- non-watchlist blocked（非观察库阻断）。
- stale / missing data incomplete（过期 / 缺失数据不完整）。
- no opportunity push（不允许机会推送）。
- no readiness（不升级可执行就绪）。
- no entry / stop / TP / RR（不生成入场 / 止损 / 止盈 / 盈亏比）。
- no trading action（不创建交易动作）。

## 4. P208 仍禁止

P208 即使进入 pure DTO / enum / tests（纯数据传输对象 / 枚举 / 测试），仍禁止：

- 不接 MarketQuoteClient（行情客户端）。
- 不读 runtime / live / external data（运行时 / 实时 / 外部数据）。
- 不接 service（服务）。
- 不接 mapper（映射器）。
- 不接 controller（控制器）。
- 不改 scheduler（定时器）。
- 不改 dashboard（首页）。
- 不接 DB / API（数据库 / 接口）。
- 不创建真实扫描。
- 不实现 ScanScore（扫描分数）。
- 不创建 Candidate Attention（候选关注）。
- 不创建 Promote To Home（提升到首页观察）。
- 不创建 opportunity push execution（机会推送执行）。
- 不生成真实点位。
- 不升级 readiness（可执行就绪）。
- 不接 order / execution / auto-trading（下单 / 执行 / 自动交易）。

## 5. 结论

P207 本身不授权真实扫描。

P207 只为 P208 pure DTO skeleton（纯数据传输对象骨架）做边界准备。
